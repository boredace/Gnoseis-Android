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
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program. If not, see
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Backup/restore against the real file database returned by GnoseisDatabase.getDatabase().
 */
@RunWith(AndroidJUnit4::class)
class DatabaseRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = DatabaseRepository(context)
    private val backup = File(context.cacheDir, "gnoseis_test.backup")

    @Before
    fun setUp() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        backup.delete()
        // Let the asynchronous seed-note insert (#20) finish first, so these tests only
        // exercise backup/restore and are not disturbed by that race.
        runBlocking {
            val dao = db().noteDao()
            waitFor { runBlocking { dao.getNotes().first().any { it.ownerDbId == SEED_OWNER } } }
        }
    }

    @After
    fun tearDown() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
        backup.delete()
    }

    private fun db() = GnoseisDatabase.getDatabase(context)

    /** Titles of user notes, ignoring the seed note inserted by the onCreate callback. */
    private fun List<Note>.userTitles() = filter { it.ownerDbId != SEED_OWNER }.mapNotNull { it.title }.toSet()

    private suspend fun GnoseisDatabase.userTitles() = noteDao().getNotes().first().userTitles()

    private fun titlesInFile(file: File): Set<String> =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            raw.rawQuery("SELECT title FROM Note WHERE ownerDbId != ?", arrayOf(SEED_OWNER)).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
        }

    // ---------- Export ----------

    @Test
    fun export_writesACompleteCopyOfTheDatabase() = runTest {
        db().noteDao().addNote(Note(title = "A"))

        assertTrue(repository.exportDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A"), titlesInFile(backup))
    }

    @Test
    fun export_toUnwritablePath_returnsFalse() = runTest {
        db().noteDao().addNote(Note(title = "A"))
        val path = File(context.cacheDir, "missing-dir/x.backup")

        assertFalse(repository.exportDatabase(Uri.fromFile(path)))
    }

    /** #14: export closes the singleton database that the repositories keep using. */
    @Test
    fun afterExport_existingDaoStillReadsAndWrites() = runTest {
        val noteDao = db().noteDao() // AppDataContainer holds DAOs from this instance
        noteDao.addNote(Note(title = "A"))

        repository.exportDatabase(Uri.fromFile(backup))
        noteDao.addNote(Note(title = "B"))

        assertEquals(setOf("A", "B"), noteDao.getNotes().first().userTitles())
    }

    /** #14: a list screen observing notes during a backup should keep receiving updates. */
    @Test
    fun afterExport_activeObserverStillReceivesUpdates() = runTest {
        val noteDao = db().noteDao()
        noteDao.addNote(Note(title = "A"))

        noteDao.getNotes().test {
            assertEquals(setOf("A"), awaitItem().userTitles())
            repository.exportDatabase(Uri.fromFile(backup))
            noteDao.addNote(Note(title = "B"))
            var titles = awaitItem().userTitles()
            while (titles != setOf("A", "B")) titles = awaitItem().userTitles()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** #32: exporting over an existing, longer file leaves only the new backup. */
    @Test
    fun export_overExistingFile_replacesItCompletely() = runTest {
        backup.writeBytes(ByteArray(10_000_000) { 1 })
        db().noteDao().addNote(Note(title = "A"))

        assertTrue(repository.exportDatabase(Uri.fromFile(backup)))

        assertTrue(backup.length() < 10_000_000)
        assertEquals(setOf("A"), titlesInFile(backup))
    }

    @Test
    fun deleteLeftoverTempFiles_removesOnlyBackupTempCopies() {
        val leftovers = listOf("gnoseis_export123.db", "gnoseis_import456.db", "gnoseis_import456.db-journal")
            .map { File(context.cacheDir, it).apply { writeText("data") } }
        val other = File(context.cacheDir, "unrelated.tmp").apply { writeText("keep") }

        repository.deleteLeftoverTempFiles()

        assertTrue(leftovers.none { it.exists() })
        assertTrue(other.exists())
        other.delete()
    }

    // ---------- Import ----------

    /** Backup contains note A; the live database then gets note B. */
    private suspend fun backupWithAThenAddB() {
        db().noteDao().addNote(Note(title = "A"))
        assertTrue(repository.exportDatabase(Uri.fromFile(backup)))
        db().noteDao().addNote(Note(title = "B"))
        assertEquals(setOf("A", "B"), db().userTitles())
    }

    @Test
    fun import_restoresBackup_afterAppRestart() = runTest {
        backupWithAThenAddB()

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))
        GnoseisDatabase.resetInstanceForTesting() // simulate process restart

        assertEquals(setOf("A"), db().userTitles())
    }

    /** #15: import does not reset GnoseisDatabase.Instance. */
    @Test
    fun import_restoresBackup_withoutRestart() = runTest {
        backupWithAThenAddB()

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A"), db().userTitles())
    }

    /** #14 + #15: import also closes the singleton, so the next save after a restore fails. */
    @Test
    fun afterImport_existingDaoCanWrite() = runTest {
        backupWithAThenAddB()
        val noteDao = db().noteDao()

        repository.importDatabase(Uri.fromFile(backup))
        noteDao.addNote(Note(title = "C"))

        assertEquals(setOf("A", "C"), noteDao.getNotes().first().userTitles())
    }

    /** #15: screens observing data should show the restored data. */
    @Test
    fun import_activeObserverSeesRestoredData() = runTest {
        backupWithAThenAddB()
        val noteDao = db().noteDao()

        noteDao.getNotes().test {
            var titles = awaitItem().userTitles()
            while (titles != setOf("A", "B")) titles = awaitItem().userTitles()
            repository.importDatabase(Uri.fromFile(backup))
            titles = awaitItem().userTitles()
            while (titles != setOf("A")) titles = awaitItem().userTitles()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** #15: data saved after a restore is kept after the app restarts. */
    @Test
    fun afterImport_savedDataSurvivesRestart() = runTest {
        backupWithAThenAddB()

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))
        db().noteDao().addNote(Note(title = "C"))
        GnoseisDatabase.resetInstanceForTesting() // simulate process restart

        assertEquals(setOf("A", "C"), db().userTitles())
    }

    /** #16: a backup from another database version is rejected, not loaded. */
    @Test
    fun import_backupFromOtherSchemaVersion_isRejected_andDataKept() = runTest {
        backupWithAThenAddB()
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 99 }

        assertFalse(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A", "B"), db().userTitles())
    }

    /** Turns the exported [backup] into the backup the app made at database version 1. */
    private fun downgradeBackupToVersion1(addLinks: SQLiteDatabase.() -> Unit = {}) =
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
            // v1 and v2 have the same schema and identity hash; only MIGRATION_1_2 differs
            raw.addLinks()
            raw.version = 1
        }

    private fun SQLiteDatabase.link(id: String, record1Id: String, record2Id: String) = execSQL(
        "INSERT INTO LinkedRecord (id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId) " +
                "VALUES (?, 'db1', ?, ?, 2, 1)",
        arrayOf(id, record1Id, record2Id),
    )

    private fun liveLinkIds(): List<String> =
        db().openHelper.writableDatabase.query("SELECT id FROM LinkedRecord ORDER BY id").use {
            buildList { while (it.moveToNext()) add(it.getString(0)) }
        }

    /** #31: a backup made at version 1 is migrated, and its duplicate links removed (#23). */
    @Test
    fun import_backupFromOlderVersion_isMigrated_andRestored() = runTest {
        backupWithAThenAddB()
        downgradeBackupToVersion1 {
            link("first", "contact", "note")
            link("reversed", "note", "contact")
        }

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A"), db().userTitles())
        assertEquals(listOf("first"), liveLinkIds())
        assertEquals(2, db().openHelper.writableDatabase.version)
    }

    /** #31: only the temporary copy is migrated, never the file the user picked. */
    @Test
    fun import_backupFromOlderVersion_leavesSelectedFileUnchanged() = runTest {
        backupWithAThenAddB()
        downgradeBackupToVersion1 { link("first", "contact", "note"); link("same", "contact", "note") }
        val before = backup.readBytes()

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))

        assertTrue(before.contentEquals(backup.readBytes()))
    }

    /** #31: a backup from a newer version than the app is still refused. */
    @Test
    fun import_backupFromNewerVersion_isRejected_andDataKept() = runTest {
        backupWithAThenAddB()
        val newer = db().openHelper.writableDatabase.version + 1
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = newer }

        assertFalse(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A", "B"), db().userTitles())
    }

    /**
     * A backup written by 0.2.3 and earlier: the app closed its version 1 database and copied
     * the raw file, which is still marked as a write-ahead-log database.
     */
    private fun writeOldStyleBackup(vararg titles: String) {
        val old = File(context.cacheDir, "old_app/gnoseis_data").apply { parentFile!!.mkdirs() }
        Room.databaseBuilder(context, GnoseisDatabase::class.java, old.absolutePath)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
            .apply {
                runBlocking { titles.forEach { noteDao().addNote(Note(title = it)) } }
                openHelper.writableDatabase.version = 1
                close()
            }
        old.copyTo(backup, overwrite = true)
        old.parentFile!!.deleteRecursively()
        assertEquals("backup is in WAL format", 2, backup.readBytes()[18].toInt())
    }

    /** #31: a backup exported by the old app is migrated and restored. */
    @Test
    fun import_backupExportedByOldApp_isRestored() = runTest {
        db().noteDao().addNote(Note(title = "B"))
        writeOldStyleBackup("A")

        assertTrue(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A"), db().userTitles())
    }

    /** #31: an SQLite file that Room did not create is not "migrated" into an empty database. */
    @Test
    fun import_sqliteFileFromAnotherApp_isRejected_andDataKept() = runTest {
        db().noteDao().addNote(Note(title = "A"))
        SQLiteDatabase.openOrCreateDatabase(backup, null).use {
            it.execSQL("CREATE TABLE Other (x TEXT)")
            it.version = 1
        }

        assertFalse(repository.importDatabase(Uri.fromFile(backup)))

        assertEquals(setOf("A"), db().userTitles())
    }

    @Test
    fun import_fileThatIsNotADatabase_isRejected_andDataKept() = runTest {
        db().noteDao().addNote(Note(title = "A"))
        backup.writeText("this is not an sqlite database")

        val result = runCatching { repository.importDatabase(Uri.fromFile(backup)) }
        GnoseisDatabase.resetInstanceForTesting()
        val titles = runCatching { db().userTitles() }

        assertEquals(false, result.getOrNull())
        assertEquals(setOf("A"), titles.getOrNull())
    }

    @Test
    fun import_missingFile_returnsFalse_insteadOfThrowing() = runTest {
        db().noteDao().addNote(Note(title = "A"))
        val missing = Uri.fromFile(File(context.cacheDir, "does-not-exist.backup"))

        val result = runCatching { repository.importDatabase(missing) }

        assertEquals(Result.success(false), result)
    }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
    }
}
