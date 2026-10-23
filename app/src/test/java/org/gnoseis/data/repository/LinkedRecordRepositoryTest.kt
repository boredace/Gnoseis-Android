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
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The repositories are pass-through except for one oddity covered here:
 * getLinkedNoteListByOrganizationId delegates to the DAO's *ItemId* query.
 */
@RunWith(AndroidJUnit4::class)
class LinkedRecordRepositoryTest {
    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    @Test
    fun linkedNotesByOrganizationId_returnsNotesLinkedToThatOrganization() = runTest {
        val org = Organization(organizationName = "Acme")
        val item = Item(itemName = "Laptop")
        val orgNote = Note(title = "org note")
        val itemNote = Note(title = "item note")
        c.organizationRepository.addOrganization(org)
        c.itemRepository.addItem(item)
        c.noteRepository.addNote(orgNote)
        c.noteRepository.addNote(itemNote)
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, orgNote.id, RecordType.Note))
        c.linkedRecordRepository.addLinkedRecord(link(itemNote.id, RecordType.Note, item.id, RecordType.Item))

        assertEquals(listOf(orgNote), c.linkedRecordRepository.getLinkedNoteListByOrganizationId(org.id).first())
        assertEquals(listOf(itemNote), c.linkedRecordRepository.getLinkedNoteListByItemId(item.id).first())
    }

    @Test
    fun addLinkedRecord_ignoresDuplicates_inBothDirections() = runTest {
        val org = Organization(organizationName = "Acme")
        val note = Note(title = "n")
        c.organizationRepository.addOrganization(org)
        c.noteRepository.addNote(note)

        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, note.id, RecordType.Note))
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, note.id, RecordType.Note))
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))

        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Note.recordTypeId, 1)),
            c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(org.id).first(),
        )
    }
}
