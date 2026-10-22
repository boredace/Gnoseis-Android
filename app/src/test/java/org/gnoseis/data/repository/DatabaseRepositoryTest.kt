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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.knownBug
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

        assertTrue(repository.exportDatabase(backup.path))

        assertEquals(setOf("A"), titlesInFile(backup))
    }

    @Test
    fun export_toUnwritablePath_returnsFalse() = runTest {
        db().noteDao().addNote(Note(title = "A"))
        val path = File(context.cacheDir, "missing-dir/x.backup").path

        assertFalse(repository.exportDatabase(path))
    }

    /** #14: export closes the singleton database that the repositories keep using. */
    @Test
    fun afterExport_existingDaoStillReadsAndWrites() = runTest {
        knownBug(
            issue = 14,
            "after a backup (export) the next save fails. DatabaseRepository.exportDatabase() closes the GnoseisDatabase singleton, but the app keeps using that closed instance. Fix: do not close the DB; checkpoint the WAL and copy the file.",
        ) {
            val noteDao = db().noteDao() // AppDataContainer holds DAOs from this instance
            noteDao.addNote(Note(title = "A"))

            repository.exportDatabase(backup.path)
            noteDao.addNote(Note(title = "B"))

            assertEquals(setOf("A", "B"), noteDao.getNotes().first().userTitles())
        }
    }

    /** #14: a list screen observing notes during a backup should keep receiving updates. */
    @Test
    fun afterExport_activeObserverStillReceivesUpdates() = runTest {
        knownBug(
            issue = 14,
            "after a backup (export) saving fails and open screens stop updating. exportDatabase() closes the singleton database the app keeps using. Fix: do not close the DB; checkpoint the WAL and copy the file.",
        ) {
            val noteDao = db().noteDao()
            noteDao.addNote(Note(title = "A"))

            noteDao.getNotes().test {
                assertEquals(setOf("A"), awaitItem().userTitles())
                repository.exportDatabase(backup.path)
                noteDao.addNote(Note(title = "B"))
                var titles = awaitItem().userTitles()
                while (titles != setOf("A", "B")) titles = awaitItem().userTitles()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------- Import ----------

    /**
     * Backup contains note A; the live database then gets note B. The process is "restarted"
     * after the export so that #14 does not interfere with the import tests.
     */
    private suspend fun backupWithAThenAddB() {
        db().noteDao().addNote(Note(title = "A"))
        assertTrue(repository.exportDatabase(backup.path))
        GnoseisDatabase.resetInstanceForTesting()
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
        knownBug(
            issue = 15,
            "after a restore (import) the next save fails. importDatabase() closes the singleton database and never resets GnoseisDatabase.Instance. Fix: reset the instance after copying and restart the app process.",
        ) {
            backupWithAThenAddB()
            val noteDao = db().noteDao()

            repository.importDatabase(Uri.fromFile(backup))
            noteDao.addNote(Note(title = "C"))

            assertEquals(setOf("A", "C"), noteDao.getNotes().first().userTitles())
        }
    }

    /** #15: screens observing data should show the restored data. */
    @Test
    fun import_activeObserverSeesRestoredData() = runTest {
        knownBug(
            issue = 15,
            "after a restore (import) open screens keep showing the old data. Room is not told the file was replaced, so its observers never refresh. Fix: reset the instance after copying and restart the app process.",
        ) {
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
    }

    /** #15: only the main file is replaced; a -wal/-shm pair left behind would be replayed. */
    @Test
    fun import_leavesNoStaleWalOrShmFiles() = runTest {
        backupWithAThenAddB()
        val dbFile = context.getDatabasePath(DB_NAME)
        val wal = File(dbFile.path + "-wal")
        val walBeforeImport = wal.exists() && wal.length() > 0

        repository.importDatabase(Uri.fromFile(backup))

        assertTrue("WAL should exist while the DB is open (sanity check)", walBeforeImport)
        assertFalse("stale -wal left next to restored DB", wal.exists() && wal.length() > 0)
        GnoseisDatabase.resetInstanceForTesting()
        assertEquals(setOf("A"), db().userTitles())
    }

    @Test
    fun import_fileThatIsNotADatabase_isRejected_andDataKept() = runTest {
        knownBug(
            issue = 16,
            "restore accepts any file. importDatabase() overwrites the user's database without checking the file, then reports success. Fix: open the file read-only first and check integrity_check and the schema version.",
        ) {
            db().noteDao().addNote(Note(title = "A"))
            backup.writeText("this is not an sqlite database")

            val result = runCatching { repository.importDatabase(Uri.fromFile(backup)) }
            GnoseisDatabase.resetInstanceForTesting()
            val titles = runCatching { db().userTitles() }

            assertEquals(false, result.getOrNull())
            assertEquals(setOf("A"), titles.getOrNull())
        }
    }

    @Test
    fun import_missingFile_returnsFalse_insteadOfThrowing() = runTest {
        knownBug(
            issue = 17,
            "restore crashes on an unreadable file. contentResolver.openInputStream() throws outside the try/catch in importDatabase(). Fix: catch the exception and return false.",
        ) {
            db().noteDao().addNote(Note(title = "A"))
            val missing = Uri.fromFile(File(context.cacheDir, "does-not-exist.backup"))

            val result = runCatching { repository.importDatabase(missing) }

            assertEquals(Result.success(false), result)
        }
    }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
    }
}
