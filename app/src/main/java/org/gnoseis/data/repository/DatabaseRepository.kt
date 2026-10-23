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
import org.gnoseis.data.database.GnoseisDatabase
import java.io.File
import java.io.FileNotFoundException

/**
 * Backs up and restores the app database while it stays open, so the DAOs and screens that
 * hold the [GnoseisDatabase] instance keep working and no restart is needed.
 */
class DatabaseRepository (
    private val context: Context,
){
    /**
     * Writes a self-contained copy of the database to [destination], a file the user chose with
     * the system file picker (#32). The copy is made in a single transaction, so it is consistent
     * even if the app saves something at the same time.
     */
    suspend fun exportDatabase(destination: Uri) : Boolean = withContext(Dispatchers.IO) {
        val temp = File.createTempFile(EXPORT_TEMP_PREFIX, ".db", context.cacheDir)
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
            }
            // "wt" truncates, so overwriting a longer file leaves no trailing bytes
            val output = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw FileNotFoundException("Cannot write to $destination")
            temp.inputStream().use { input -> output.use { input.copyTo(it) } }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            deleteDatabaseFiles(temp)
        }
    }

    /**
     * Replaces all data with the backup in [selectedFile]. A backup from an older database
     * version is migrated first, on a temporary copy. If the file cannot be read, is not a valid
     * Gnoseis backup, cannot be migrated, or comes from a newer database version, nothing is
     * changed and false is returned. The selected file itself is never modified.
     */
    suspend fun importDatabase(selectedFile: Uri) : Boolean = withContext(Dispatchers.IO) {
        val temp = File.createTempFile(IMPORT_TEMP_PREFIX, ".db", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(selectedFile)
                ?: return@withContext false
            input.use { source -> temp.outputStream().use { source.copyTo(it) } }

            val db = GnoseisDatabase.getDatabase(context)
            val liveVersion = db.openHelper.writableDatabase.version
            val backupVersion = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
                if (!isIntactRoomDatabase(backup)) return@withContext false
                backup.version
            }
            // #31: bring an older backup up to date with the migrations the app runs at startup.
            // Only the temporary copy is changed; a newer backup cannot be migrated down.
            if (backupVersion < 1 || backupVersion > liveVersion) return@withContext false
            if (backupVersion < liveVersion) migrateBackup(temp)

            SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
                if (!isCompatibleBackup(backup, db.openHelper.writableDatabase)) {
                    return@withContext false
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
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            deleteDatabaseFiles(temp)
        }
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
        const val EXPORT_TEMP_PREFIX = "gnoseis_export"
        const val IMPORT_TEMP_PREFIX = "gnoseis_import"
    }
}
