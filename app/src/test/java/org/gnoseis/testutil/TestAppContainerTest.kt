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

package org.gnoseis.testutil

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.note.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestAppContainerTest {
    private val container = TestAppContainer()

    @After
    fun tearDown() = container.close()

    @Test
    fun inMemoryDatabase_startsEmpty_withoutSeedNote() = runTest {
        assertTrue(container.noteRepository.getNotesStream().first().isEmpty())
    }

    @Test
    fun repositories_shareTheSameDatabase() = runTest {
        val rowId = container.noteRepository.addNote(Note(title = "hello"))
        val note = container.noteRepository.getNoteByRowId(rowId)

        val results = container.searchRepository.getSearchResultsForQuery("%hello%").first()

        assertEquals(listOf(note.id), results.map { it.recordId })
    }
}
