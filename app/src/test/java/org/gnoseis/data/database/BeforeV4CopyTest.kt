/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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

package org.gnoseis.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.note.Note
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.time.LocalDateTime

/** #68: the copy of the live database kept before it is upgraded to version 4. */
@RunWith(AndroidJUnit4::class)
class BeforeV4CopyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val live: File get() = context.getDatabasePath(GnoseisDatabase.DATABASE_NAME)
    private val now = LocalDateTime.parse("2026-11-14T09:05:07")

    @Before
    fun setUp() = reset()

    @After
    fun tearDown() = reset()

    private fun reset() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(GnoseisDatabase.DATABASE_NAME)
        copies().forEach { it.delete() }
    }

    private fun copies(): List<File> =
        live.parentFile?.listFiles { f -> f.name.startsWith("${GnoseisDatabase.DATABASE_NAME}.before-v4-") }?.toList().orEmpty()

    /** A live database with a user note, closed, then marked as version [version]. */
    private suspend fun liveDatabaseAt(version: Int) {
        GnoseisDatabase.getDatabase(context).noteDao().addNote(Note(title = "precious"))
        GnoseisDatabase.resetInstanceForTesting()
        SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = version }
    }

    @Test
    fun v3Database_isCopiedWithItsOriginalBytes_beforeItIsUpgraded() = runTest {
        liveDatabaseAt(3)
        val before = live.readBytes()

        val db = GnoseisDatabase.getDatabase(context)
        // Made before Room opens, so before the migration
        val copy = copies().single { !it.name.endsWith("-wal") && !it.name.endsWith("-shm") }
        assertArrayEquals(before, copy.readBytes())
        assertTrue(copy.name.matches(Regex("""gnoseis_data\.before-v4-\d{4}-\d\d-\d\d_\d\d-\d\d-\d\d""")))

        // Then the upgrade runs; the copy stays as it was, at version 3, with the user's note
        assertTrue(db.noteDao().getNotes().first().any { it.title == "precious" })
        assertEquals(4, db.openHelper.writableDatabase.version)
        GnoseisDatabase.resetInstanceForTesting()
        SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(3, it.version) }
    }

    @Test
    fun v4Database_isNotCopied() = runTest {
        liveDatabaseAt(4)

        GnoseisDatabase.getDatabase(context).noteDao().getNotes().first()

        assertEquals(emptyList<File>(), copies())
    }

    @Test
    fun newDatabase_isNotCopied() = runTest {
        GnoseisDatabase.getDatabase(context).noteDao().getNotes().first()

        assertEquals(emptyList<File>(), copies())
    }

    @Test
    fun keep_copiesTheShmFile_andNamesTheCopyByTheTime() = runTest {
        liveDatabaseAt(3)
        val shm = File(live.path + "-shm").apply { writeBytes(byteArrayOf(4, 5)) }
        try {
            val before = live.readBytes()
            val copy = BeforeV4Copy.keep(live, now)!!

            assertEquals("gnoseis_data.before-v4-2026-11-14_09-05-07", copy.name)
            assertArrayEquals(before, copy.readBytes())
            assertArrayEquals(byteArrayOf(4, 5), File(copy.path + "-shm").readBytes())
            // The live files are only read
            assertArrayEquals(before, live.readBytes())
            assertArrayEquals(byteArrayOf(4, 5), shm.readBytes())
        } finally {
            shm.delete()
        }
    }

    /**
     * A version change still in the -wal file: the header says 3 but the database is at 4.
     * The copy is checked, found to be at 4, and dropped; the live files are untouched.
     */
    @Test
    fun keep_whenTheWalAlreadyHoldsVersion4_makesNoCopy() = runTest {
        liveDatabaseAt(3)
        // Raise the version to 4 inside the WAL and keep it there by not letting SQLite checkpoint
        val raw = SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READWRITE)
        raw.rawQuery("PRAGMA journal_mode = WAL", null).use { it.moveToFirst() }
        raw.rawQuery("PRAGMA wal_autocheckpoint = 0", null).use { it.moveToFirst() }
        raw.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        val savedMain = live.readBytes()
        raw.version = 4
        val wal = File(live.path + "-wal")
        val main = live.readBytes()
        val walBytes = wal.readBytes()
        try {
            assertTrue("the version change should still be in the WAL", walBytes.isNotEmpty() && main.contentEquals(savedMain))
            assertNull(BeforeV4Copy.keep(live, now))
            assertEquals(emptyList<File>(), copies())
            assertArrayEquals(main, live.readBytes())
            assertArrayEquals(walBytes, wal.readBytes())
        } finally {
            raw.close()
        }
    }

    @Test
    fun keep_whenTheCopyFails_throws_andLeavesNoPartialCopy() = runTest {
        liveDatabaseAt(3)
        val wal = File(live.path + "-wal").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        // Something is already where the -wal copy goes
        val blocker = File(live.parentFile, "gnoseis_data.before-v4-2026-11-14_09-05-07-wal").apply { writeBytes(byteArrayOf(9)) }
        try {
            BeforeV4Copy.keep(live, now)
            fail("expected the copy to fail")
        } catch (expected: IOException) {
        } finally {
            wal.delete()
        }

        assertEquals(listOf(blocker.name), copies().map { it.name })
        // The live database is untouched, still version 3
        SQLiteDatabase.openDatabase(live.path, null, SQLiteDatabase.OPEN_READONLY).use { assertEquals(3, it.version) }
    }

    @Test
    fun keep_withNoDatabase_doesNothing() {
        assertNull(BeforeV4Copy.keep(live, now))
    }
}
