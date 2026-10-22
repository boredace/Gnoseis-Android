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

package org.gnoseis.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.knownBug
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GnoseisDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = reset()

    @After
    fun tearDown() = reset()

    private fun reset() {
        GnoseisDatabase.resetInstanceForTesting()
        context.deleteDatabase(DB_NAME)
    }

    private fun seedNotes(notes: List<Note>) = notes.filter { it.ownerDbId == SEED_OWNER }

    @Test
    fun getDatabase_returnsSingleton() {
        assertSame(GnoseisDatabase.getDatabase(context), GnoseisDatabase.getDatabase(context))
    }

    @Test
    fun freshDatabase_eventuallyContainsExactlyOneSeedNote() = runTest {
        val dao = GnoseisDatabase.getDatabase(context).noteDao()
        dao.getNotes().first() // opens the DB and triggers onCreate

        waitFor { runBlocking { seedNotes(dao.getNotes().first()).isNotEmpty() } }

        val seed = seedNotes(dao.getNotes().first())
        assertEquals(1, seed.size)
        assertEquals("This is a default note", seed.single().title)
    }

    /**
     * #20: the seed note is inserted from an untracked CoroutineScope(Dispatchers.IO), so the
     * first read after creating the database may or may not include it. Repeats the scenario
     * to show the outcome is not deterministic.
     */
    @Test
    fun freshDatabase_firstRead_alwaysIncludesSeedNote() = runTest {
        knownBug(
            issue = 20,
            "the default 'seed' note appears at a random moment. The onCreate callback inserts it from an untracked CoroutineScope(Dispatchers.IO), racing with the first reads. Fix: insert it synchronously with the SupportSQLiteDatabase passed to onCreate.",
        ) {
            val attempts = 20
            var missing = 0
            repeat(attempts) {
                reset()
                val firstRead = GnoseisDatabase.getDatabase(context).noteDao().getNotes().first()
                if (seedNotes(firstRead).isEmpty()) missing++
            }
            assertEquals("first read was missing the seed note in $missing/$attempts runs", 0, missing)
        }
    }

    /**
     * #18: fallbackToDestructiveMigration() silently drops all tables whenever Room finds a
     * version it has no migration for. A database from another schema version (e.g. a backup
     * made by a newer app build, restored via importDatabase) is simulated by bumping
     * PRAGMA user_version. Acceptable outcomes: data kept, or opening fails loudly.
     */
    @Test
    fun openingDatabaseWithUnknownVersion_doesNotSilentlyWipeUserData() = runTest {
        knownBug(
            issue = 18,
            "any schema version mismatch silently deletes ALL user data. GnoseisDatabase uses fallbackToDestructiveMigration(). Fix: remove it, add real migrations and test them against app/schemas.",
        ) {
            val dao = GnoseisDatabase.getDatabase(context).noteDao()
            waitFor { runBlocking { seedNotes(dao.getNotes().first()).isNotEmpty() } } // avoid #20
            dao.addNote(Note(title = "precious"))
            GnoseisDatabase.resetInstanceForTesting()
            val path: File = context.getDatabasePath(DB_NAME)
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 2 }

            val titles = runCatching {
                GnoseisDatabase.getDatabase(context).noteDao().getNotes().first().mapNotNull { it.title }
            }

            assertTrue(
                "user data was silently wiped: $titles",
                titles.isFailure || "precious" in titles.getOrThrow(),
            )
        }
    }

    private companion object {
        const val DB_NAME = "gnoseis_data"
        const val SEED_OWNER = "yyy"
    }
}
