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

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.note.Note
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val repository = SettingsRepository(db, now = { 100 })

    @After
    fun tearDown() = db.close()

    @Test
    fun recordingDeletion_savesLastDeletionAt_andReturnsTheResult() = runTest {
        assertEquals("done", repository.recordingDeletion { "done" })

        assertEquals(100L, repository.getLastDeletionAtStream().first())
        assertNull(repository.getLastBackupAtStream().first())
    }

    /** #51: the time and the deletion are saved together or not at all. */
    @Test
    fun recordingDeletion_whenTheDeleteFails_savesNeitherTheDeleteNorTheTime() = runTest {
        val note = Note(title = "n")
        db.noteDao().addNote(note)

        val failure = runCatching {
            repository.recordingDeletion {
                db.noteDao().deleteNoteById(note.id)
                error("delete failed")
            }
        }

        assertTrue(failure.isFailure)
        assertEquals(listOf(note), db.noteDao().getNotes().first())
        assertNull(repository.getLastDeletionAtStream().first())
    }

    /** #11: a delete that found nothing to delete doesn't count as a deletion. */
    @Test
    fun recordingDeletion_whenNothingWasDeleted_doesNotSaveTheTime() = runTest {
        assertEquals(0, repository.recordingDeletion(delete = { 0 }, deleted = { it > 0 }))
        assertNull(repository.getLastDeletionAtStream().first())

        assertEquals(1, repository.recordingDeletion(delete = { 1 }, deleted = { it > 0 }))
        assertEquals(100L, repository.getLastDeletionAtStream().first())
    }
}
