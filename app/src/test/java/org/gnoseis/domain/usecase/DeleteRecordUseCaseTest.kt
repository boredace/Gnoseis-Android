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
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeleteRecordUseCaseTest {
    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun useCase() = DeleteRecordUseCase(
        categoryRepository = c.categoryRepository,
        contactRepository = c.contactRepository,
        itemRepository = c.itemRepository,
        noteRepository = c.noteRepository,
        organizationRepository = c.organizationRepository,
        linkedRecordRepository = c.linkedRecordRepository,
    )

    @Test
    fun deletesContact_andLinksInBothDirections_butKeepsUnrelatedLinks() = runTest {
        val contact = Contact(nameLast = "Doe")
        val note = Note(title = "n")
        val org = Organization(organizationName = "o")
        val item = Item(itemName = "i")
        c.contactRepository.addContact(contact)
        c.noteRepository.addNote(note)
        c.organizationRepository.addOrganization(org)
        c.itemRepository.addItem(item)
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, item.id, RecordType.Item))

        useCase()(contact.id)

        assertTrue(c.contactRepository.getContactsStream().first().isEmpty())
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(note.id).first().isEmpty())
        assertEquals(listOf(item.id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(org.id).first())
        // Linked records themselves are not deleted
        assertNotNull(c.noteRepository.getNoteStream(note.id).first())
        assertNotNull(c.organizationRepository.getOrganizationStream(org.id).first())
    }

    @Test
    fun deletesEachRecordType() = runTest {
        val category = Category(categoryName = "cat")
        val contact = Contact(nameLast = "Doe")
        val item = Item(itemName = "item")
        val note = Note(title = "note")
        val org = Organization(organizationName = "org")
        c.categoryRepository.addCategory(category)
        c.contactRepository.addContact(contact)
        c.itemRepository.addItem(item)
        c.noteRepository.addNote(note)
        c.organizationRepository.addOrganization(org)

        listOf(category.id, contact.id, item.id, note.id, org.id).forEach { useCase()(it) }

        assertTrue(c.categoryRepository.getCategoriesStream().first().isEmpty())
        assertTrue(c.contactRepository.getContactsStream().first().isEmpty())
        assertTrue(c.itemRepository.getItemsStream().first().isEmpty())
        assertTrue(c.noteRepository.getNotesStream().first().isEmpty())
        assertTrue(c.organizationRepository.getOrganizationsStream().first().isEmpty())
    }

    @Test
    fun onlyConfiguredRepositoriesDeleteRecords_butLinksAreAlwaysRemoved() = runTest {
        val note = Note(title = "n")
        val item = Item(itemName = "i")
        c.noteRepository.addNote(note)
        c.itemRepository.addItem(item)
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, item.id, RecordType.Item))

        // Use case configured for contacts only, but given a note id
        DeleteRecordUseCase(
            contactRepository = c.contactRepository,
            linkedRecordRepository = c.linkedRecordRepository,
        )(note.id)

        assertNotNull(c.noteRepository.getNoteStream(note.id).first())
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(note.id).first().isEmpty())
    }

    @Test
    fun unknownId_isNoOp() = runTest {
        val note = Note(title = "n")
        c.noteRepository.addNote(note)

        useCase()("does-not-exist")

        assertEquals(1, c.noteRepository.getNotesStream().first().size)
        assertNull(c.categoryRepository.getCategoryStream("does-not-exist").first())
    }
}
