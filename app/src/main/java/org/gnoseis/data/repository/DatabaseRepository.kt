/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024 Gnoseis.org
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation version 3 of the License.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program. If not, see
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
* 
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
* 
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnoseis.data.backup.BackupDecryptException
import org.gnoseis.data.backup.BackupFormat
import org.gnoseis.data.backup.BackupHeader
import org.gnoseis.data.backup.BackupKeys
import org.gnoseis.data.backup.BackupPassphrase
import org.gnoseis.data.backup.EncryptedBackup
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.backup.readFully
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.settings.SettingKey
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Backs up and restores the app database while it stays open, so the DAOs and screens that
 * hold the [GnoseisDatabase] instance keep working and no restart is needed.
 * [now] gives the system time (UTC epoch ms) saved as lastBackupAt (#51).
 * [backupKey] is this device's stored backup key; while it is set, backups are encrypted (#66).
 */
class DatabaseRepository (
    private val context: Context,
    private val now: () -> Long = System::currentTimeMillis,
    private val backupKey: StoredBackupKey = StoredBackupKey.forDevice(context),
){
    /**
     * Writes a self-contained copy of the database to [destination], a file the user chose with
     * the system file picker (#32). The copy is made in a single transaction, so it is consistent
     * even if the app saves something at the same time.
     *
     * #51: the copy holds lastBackupAt = the time of this export. The live database gets the
     * same time only once the copy is written, so a failed export changes nothing.
     *
     * #66: while backup encryption is on, the copy is encrypted straight into [destination]
     * with the stored key, unless [plain] asks for an unencrypted backup. The plain copy only
     * ever exists in the private cache folder. If the stored key can't be opened, nothing is
     * written rather than falling back to a plain file.
     */
    suspend fun exportDatabase(destination: Uri, plain: Boolean = false) : ExportResult = withContext(Dispatchers.IO) {
        val key = if (plain) null else when (val state = backupKey.state()) {
            StoredBackupKey.Off -> null
            StoredBackupKey.Unreadable -> return@withContext ExportResult.KeyUnreadable
            is StoredBackupKey.On -> state
        }
        val temp = File.createTempFile(EXPORT_TEMP_PREFIX, ".db", context.cacheDir)
        val lastBackup = SettingKey.LAST_BACKUP_AT.row(now())
        try {
            temp.delete() // SQLite creates the file itself
            val db = GnoseisDatabase.getDatabase(context)
            SQLiteDatabase.openOrCreateDatabase(temp, null).use { backup ->
                backup.disableWriteAheadLogging() // the backup must be a single self-contained file
                db.runInTransaction {
                    val live = db.openHelper.writableDatabase
                    copySchema(live, backup)
                    backup.beginTransaction()
                    try {
                        for (table in tableNames(live)) copyRows(live, table, backup)
                        backup.setTransactionSuccessful()
                    } finally {
                        backup.endTransaction()
                    }
                    backup.version = live.version
                }
                backup.insertWithOnConflict(
                    SETTINGS_TABLE,
                    null,
                    ContentValues().apply {
                        put("settingId", lastBackup.settingId)
                        put("settingName", lastBackup.settingName)
                        put("valueInt", lastBackup.valueInt)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            // "wt" truncates, so overwriting a longer file leaves no trailing bytes
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw FileNotFoundException("Cannot write to $destination")
            output.use {
                if (key == null) {
                    temp.inputStream().use { input -> input.copyTo(it) }
                } else {
                    EncryptedBackup.encrypt(temp, it, key.masterKey, key.kdfSalt, key.kdf)
                }
            }
            db.settingsDao().upsert(lastBackup)
            if (key == null) ExportResult.Plain else ExportResult.Encrypted
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult.Failed
        } finally {
            deleteDatabaseFiles(temp)
            key?.clear()
        }
    }

    /**
     * Replaces all data with the backup in [selectedFile]. A backup from an older database
     * version is migrated first, on a temporary copy. If the file cannot be read, is not a valid
     * Gnoseis backup, cannot be migrated, or comes from a newer version, nothing is changed and
     * the result says why. The selected file itself is never modified.
     *
     * #66: the first 8 bytes decide whether the file is encrypted, never its name. An encrypted
     * backup is decrypted into the private cache folder with the stored key if it fits, otherwise
     * with [passphrase] (NeedsPassphrase when there is none); then the same checks, migration and
     * copy run on it. The caller owns [passphrase] and clears it.
     */
    suspend fun importDatabase(selectedFile: Uri, passphrase: CharArray? = null) : ImportResult = withContext(Dispatchers.IO) {
        val temp = File.createTempFile(IMPORT_TEMP_PREFIX, ".db", context.cacheDir)
        var keepOffer: KeepOffer? = null
        try {
            val opened = try {
                context.contentResolver.openInputStream(selectedFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: return@withContext ImportResult.Failed
            var encrypted = false
            try {
                opened.buffered().use { input ->
                    input.mark(MAGIC_SIZE)
                    val start = ByteArray(MAGIC_SIZE).let { it.copyOf(input.readFully(it)) }
                    input.reset()
                    if (BackupFormat.hasMagic(start)) {
                        encrypted = true
                        when (val decryption = decryptInto(input, temp, passphrase)) {
                            is Decryption.Done -> keepOffer = decryption.keepOffer
                            is Decryption.Refused -> return@withContext decryption.result
                        }
                    } else {
                        temp.outputStream().use { input.copyTo(it) }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext ImportResult.Failed
            }

            val db = GnoseisDatabase.getDatabase(context)
            val liveVersion = db.openHelper.writableDatabase.version
            try {
                val backupVersion = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
                    if (!isIntactRoomDatabase(backup)) return@withContext ImportResult.NotABackup
                    backup.version
                }
                // #31: bring an older backup up to date with the migrations the app runs at startup.
                // Only the temporary copy is changed; a newer backup cannot be migrated down.
                if (backupVersion < 1) return@withContext ImportResult.NotABackup
                if (backupVersion > liveVersion) return@withContext ImportResult.NewerFormat
                if (backupVersion < liveVersion) migrateBackup(temp)
            } catch (e: Exception) {
                // Not an SQLite file, or no migration path
                e.printStackTrace()
                return@withContext ImportResult.NotABackup
            }

            SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
                if (!isCompatibleBackup(backup, db.openHelper.writableDatabase)) {
                    return@withContext ImportResult.NotABackup
                }
                // Room's invalidation tracker sees these writes, so open screens refresh.
                db.runInTransaction {
                    val live = db.openHelper.writableDatabase
                    live.execSQL("PRAGMA defer_foreign_keys = ON")
                    for (table in tableNames(live) - ROOM_MASTER_TABLE) {
                        live.execSQL("DELETE FROM `$table`")
                        copyRows(backup, table, live)
                    }
                }
            }
            ImportResult.Restored(encrypted, keepOffer).also { keepOffer = null }
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Failed
        } finally {
            keepOffer?.clear()
            deleteDatabaseFiles(temp)
        }
    }

    /**
     * Keeps the key from a passphrase restore on this device, so later backups are encrypted
     * with that passphrase without asking (#66). Clears [offer].
     */
    suspend fun keepEncrypting(offer: KeepOffer) = withContext(Dispatchers.IO) {
        try {
            backupKey.keep(offer.masterKey, offer.kdfSalt, offer.kdf)
        } finally {
            offer.clear()
        }
    }

    private sealed interface Decryption {
        class Done(val keepOffer: KeepOffer?) : Decryption
        class Refused(val result: ImportResult) : Decryption
    }

    /** Decrypts the encrypted backup in [input] into [temp], with the stored key or [passphrase]. */
    private fun decryptInto(input: InputStream, temp: File, passphrase: CharArray?): Decryption {
        val header = try {
            BackupHeader.read(input)
        } catch (e: BackupDecryptException) {
            return Decryption.Refused(e.toImportResult())
        }

        backupKey.matching(header)?.let { storedKey ->
            return try {
                EncryptedBackup.decrypt(input, header, storedKey, temp)
                Decryption.Done(keepOffer = null)
            } catch (e: BackupDecryptException) {
                Decryption.Refused(e.toImportResult())
            } finally {
                storedKey.fill(0)
            }
        }

        if (passphrase == null) return Decryption.Refused(ImportResult.NeedsPassphrase)
        val normalised = BackupPassphrase.normalise(passphrase) as? BackupPassphrase.Normalised
            ?: return Decryption.Refused(ImportResult.WrongPassphraseOrDamaged)
        val masterKey = try {
            BackupKeys.deriveMasterKey(normalised.bytes, header.kdfSalt, header.kdf)
        } catch (e: OutOfMemoryError) {
            // Settings within the format's limits can still be more than this device's heap
            return Decryption.Refused(ImportResult.WrongPassphraseOrDamaged)
        } finally {
            normalised.bytes.fill(0)
        }
        try {
            EncryptedBackup.decrypt(input, header, masterKey, temp)
        } catch (e: BackupDecryptException) {
            masterKey.fill(0)
            return Decryption.Refused(e.toImportResult())
        }
        // Offer to keep encrypting only where this device has no usable key of its own
        if (backupKey.state() is StoredBackupKey.On) {
            masterKey.fill(0)
            return Decryption.Done(keepOffer = null)
        }
        return Decryption.Done(KeepOffer(masterKey, header.kdfSalt, header.kdf))
    }

    private fun BackupDecryptException.toImportResult(): ImportResult = when (this) {
        is BackupDecryptException.NewerFormat -> ImportResult.NewerFormat
        is BackupDecryptException.NotEncrypted -> ImportResult.NotABackup
        else -> ImportResult.WrongPassphraseOrDamaged
    }

    /**
     * Deletes temporary copies left in the private cache folder when the app was stopped in the
     * middle of an export or import, so a full copy of the user's data doesn't stay behind.
     * Call only at startup, before any export or import can be running.
     */
    fun deleteLeftoverTempFiles() {
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith(EXPORT_TEMP_PREFIX) || it.name.startsWith(IMPORT_TEMP_PREFIX) }
            ?.forEach { it.delete() }
    }

    /** True if [backup] is an intact SQLite file that was created by Room. */
    private fun isIntactRoomDatabase(backup: SQLiteDatabase): Boolean {
        val intact = backup.rawQuery("PRAGMA integrity_check", null).use {
            it.moveToFirst() && it.getString(0) == "ok"
        }
        return intact && backup.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(ROOM_MASTER_TABLE)
        ).use { it.moveToFirst() }
    }

    /**
     * Runs [GnoseisDatabase.MIGRATIONS] on the backup copy in [file]. There is no destructive
     * fallback and no seed-note callback: if no migration path exists, or the migrated tables
     * don't match the app's entities, Room throws and the import is abandoned.
     */
    private fun migrateBackup(file: File) {
        // An absolute path as the name makes Room open that file instead of one in databases/
        Room.databaseBuilder(context, GnoseisDatabase::class.java, file.absolutePath)
            .addMigrations(*GnoseisDatabase.MIGRATIONS)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
            .run {
                try {
                    openHelper.writableDatabase // runs the migrations, then validates the schema
                } finally {
                    close()
                }
            }
    }

    /** True if [backup] has exactly the version and schema of [live]. */
    private fun isCompatibleBackup(backup: SQLiteDatabase, live: SupportSQLiteDatabase): Boolean {
        if (backup.version != live.version) return false
        val backupHash = backup.rawQuery("SELECT identity_hash FROM $ROOM_MASTER_TABLE", null).use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        val liveHash = live.query("SELECT identity_hash FROM $ROOM_MASTER_TABLE").use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        return backupHash != null && backupHash == liveHash
    }

    private fun copySchema(live: SupportSQLiteDatabase, backup: SQLiteDatabase) {
        live.query(
            "SELECT sql FROM sqlite_master WHERE sql IS NOT NULL AND type IN ('table', 'index') " +
                    "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' " +
                    "ORDER BY type = 'index'"
        ).use { while (it.moveToNext()) backup.execSQL(it.getString(0)) }
    }

    private fun tableNames(live: SupportSQLiteDatabase): List<String> =
        live.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'"
        ).use { buildList { while (it.moveToNext()) add(it.getString(0)) } }

    private fun copyRows(from: SupportSQLiteDatabase, table: String, to: SQLiteDatabase) =
        from.query("SELECT * FROM `$table`").use { rows ->
            while (rows.moveToNext()) to.insertOrThrow(table, null, rows.toContentValues())
        }

    private fun copyRows(from: SQLiteDatabase, table: String, to: SupportSQLiteDatabase) =
        from.rawQuery("SELECT * FROM `$table`", null).use { rows ->
            while (rows.moveToNext()) to.insert(table, SQLiteDatabase.CONFLICT_ABORT, rows.toContentValues())
        }

    private fun Cursor.toContentValues() = ContentValues().also { values ->
        for (i in 0 until columnCount) {
            val name = getColumnName(i)
            when (getType(i)) {
                Cursor.FIELD_TYPE_NULL -> values.putNull(name)
                Cursor.FIELD_TYPE_INTEGER -> values.put(name, getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> values.put(name, getDouble(i))
                Cursor.FIELD_TYPE_BLOB -> values.put(name, getBlob(i))
                else -> values.put(name, getString(i))
            }
        }
    }

    private fun deleteDatabaseFiles(file: File) {
        for (suffix in listOf("", "-journal", "-wal", "-shm")) File(file.path + suffix).delete()
    }

    private companion object {
        const val ROOM_MASTER_TABLE = "room_master_table"
        const val SETTINGS_TABLE = "Settings"
        const val EXPORT_TEMP_PREFIX = "gnoseis_export"
        const val IMPORT_TEMP_PREFIX = "gnoseis_import"
        const val MAGIC_SIZE = 8
    }
}
