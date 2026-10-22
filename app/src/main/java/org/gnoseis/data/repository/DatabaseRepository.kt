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
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnoseis.data.database.GnoseisDatabase
import java.io.File
import java.io.FileOutputStream

/**
 * Backs up and restores the app database while it stays open, so the DAOs and screens that
 * hold the [GnoseisDatabase] instance keep working and no restart is needed.
 */
class DatabaseRepository (
    private val context: Context,
){
    /**
     * Writes a self-contained copy of the database to [destinationPath]. The copy is made in a
     * single transaction, so it is consistent even if the app saves something at the same time.
     */
    suspend fun exportDatabase(destinationPath: String) : Boolean = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("gnoseis_export", ".db", context.cacheDir)
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
            // Plain streams, not File.copyTo, which would create missing folders
            temp.inputStream().use { input -> FileOutputStream(destinationPath).use { input.copyTo(it) } }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            deleteDatabaseFiles(temp)
        }
    }

    /**
     * Replaces all data with the backup in [selectedFile]. The file is checked first; if it
     * cannot be read, is not a valid Gnoseis backup, or comes from a different database
     * version, nothing is changed and false is returned.
     */
    suspend fun importDatabase(selectedFile: Uri) : Boolean = withContext(Dispatchers.IO) {
        val temp = File.createTempFile("gnoseis_import", ".db", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(selectedFile)
                ?: return@withContext false
            input.use { source -> temp.outputStream().use { source.copyTo(it) } }

            val db = GnoseisDatabase.getDatabase(context)
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

    /** True if [backup] is an intact SQLite file with exactly the schema of [live]. */
    private fun isCompatibleBackup(backup: SQLiteDatabase, live: SupportSQLiteDatabase): Boolean {
        val intact = backup.rawQuery("PRAGMA integrity_check", null).use {
            it.moveToFirst() && it.getString(0) == "ok"
        }
        if (!intact || backup.version != live.version) return false
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
    }
}
