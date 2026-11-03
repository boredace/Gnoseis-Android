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

package org.gnoseis.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.links.LinkedRecordDao
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.testutil.TEST_NOW
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlinkRecordsUseCaseTest {
    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private val contact = Contact(nameLast = "Doe")
    private val note = Note(title = "n")
    private val stored = link(note.id, RecordType.Note, contact.id, RecordType.Contact)

    private suspend fun addLinked() {
        c.contactRepository.addContact(contact)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(stored)
    }

    private fun useCase(linkedRecordRepository: LinkedRecordRepository = c.linkedRecordRepository) =
        UnlinkRecordsUseCase(linkedRecordRepository, c.settingsRepository)

    @Test
    fun unlink_removesTheLink_returnsIt_andSavesLastDeletionAt() = runTest {
        addLinked()

        val removed = useCase()(contact.id, note.id)

        assertEquals(stored.record1Id, removed?.record1Id)
        assertEquals(stored.record2Id, removed?.record2Id)
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
        assertEquals(TEST_NOW, c.settingsRepository.getLastDeletionAtStream().first())
    }

    @Test
    fun unlink_whenNotLinked_changesNothing() = runTest {
        c.contactRepository.addContact(contact)
        c.noteRepository.addNote(note)

        assertNull(useCase()(contact.id, note.id))

        assertNull(c.settingsRepository.getLastDeletionAtStream().first())
    }

    /** G9: the unlink and lastDeletionAt are saved together or not at all. */
    @Test
    fun unlink_whenTheDeleteFails_savesNeitherTheUnlinkNorTheTime() = runTest {
        addLinked()
        val dao = c.database.linkedRecordDao()
        val failingDao = object : LinkedRecordDao by dao {
            override suspend fun deleteLinkBetween(recordIdA: String, recordIdB: String): Int {
                dao.deleteLinkBetween(recordIdA, recordIdB)
                error("unlink failed")
            }
        }

        val failure = runCatching { useCase(LinkedRecordRepository(failingDao))(contact.id, note.id) }

        assertTrue(failure.isFailure)
        assertEquals(listOf(note.id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first())
        assertNull(c.settingsRepository.getLastDeletionAtStream().first())
    }
}
