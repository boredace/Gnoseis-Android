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

package org.gnoseis.data.entity.note

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.noteDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun add_get_update_delete() = runTest {
        val note = Note(title = "t", textContents = "body", createDateTime = 1_000, createDate = 0)
        val rowId = dao.addNote(note)
        assertEquals(note, dao.getNote(note.id))
        assertEquals(note, dao.getNoteByRowId(rowId))

        dao.updateNote(note.copy(title = null, textContents = "changed"))
        assertEquals(note.copy(title = null, textContents = "changed"), dao.getNoteStream(note.id).first())

        dao.deleteNoteById(note.id)
        val deleted: Note? = dao.getNote(note.id)
        assertNull(deleted)
    }

    @Test
    fun list_isSortedNewestFirst() = runTest {
        val old = Note(title = "old", createDateTime = 1)
        val new = Note(title = "new", createDateTime = 3)
        val mid = Note(title = "mid", createDateTime = 2)
        listOf(old, new, mid).forEach { dao.addNote(it) }

        assertEquals(listOf(new, mid, old), dao.getNotes().first())
    }

    @Test
    fun defaultNote_hasCreateDateTruncatedToDay() {
        val note = Note()
        assertEquals(0L, note.createDate % 1_000)
        assert(note.createDate <= note.createDateTime)
    }

    @Test
    fun listFlow_emitsOnChanges() = runTest {
        val note = Note(title = "x")
        dao.getNotes().test {
            assertEquals(emptyList<Note>(), awaitItem())
            dao.addNote(note)
            assertEquals(listOf(note), awaitItem())
            dao.deleteNoteById(note.id)
            assertEquals(emptyList<Note>(), awaitItem())
        }
    }
}
