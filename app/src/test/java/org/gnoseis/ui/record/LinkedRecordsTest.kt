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

package org.gnoseis.ui.record

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
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
import org.gnoseis.testutil.stamped
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkedRecordsTest {
    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private val note = Note(title = "Note").stamped()
    private val contact = Contact(nameLast = "Doe", nameFirst = "Jane").stamped()
    private val organization = Organization(organizationName = "Acme").stamped()
    private val category = Category(categoryName = "Clients").stamped()
    private val item = Item(itemName = "Laptop").stamped()

    private val records = listOf(
        RecordType.Note to note.id,
        RecordType.Contact to contact.id,
        RecordType.Organization to organization.id,
        RecordType.Category to category.id,
        RecordType.Item to item.id,
    )

    /** One record of each type, every pair linked, plus a second record of each type linked to nothing. */
    private suspend fun addAllLinked() {
        c.noteRepository.addNote(note)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(organization)
        c.categoryRepository.addCategory(category)
        c.itemRepository.addItem(item)
        c.noteRepository.addNote(Note(title = "Other").stamped())
        c.contactRepository.addContact(Contact(nameLast = "Other").stamped())
        c.organizationRepository.addOrganization(Organization(organizationName = "Other").stamped())
        c.categoryRepository.addCategory(Category(categoryName = "Other").stamped())
        c.itemRepository.addItem(Item(itemName = "Other").stamped())
        for (i in records.indices) for (j in i + 1 until records.size) {
            val (type1, id1) = records[i]
            val (type2, id2) = records[j]
            c.linkedRecordRepository.addLinkedRecord(link(id1, type1, id2, type2))
        }
    }

    private fun expected(except: RecordType) = LinkedRecords(
        linkedNotes = if (except == RecordType.Note) emptyList() else listOf(note),
        linkedContacts = if (except == RecordType.Contact) emptyList() else listOf(contact),
        linkedOrganizations = if (except == RecordType.Organization) emptyList() else listOf(organization),
        linkedCategories = if (except == RecordType.Category) emptyList() else listOf(category),
        linkedItems = if (except == RecordType.Item) emptyList() else listOf(item),
    )

    @Test
    fun eachRecordType_listsTheRecordsLinkedToIt_byType() = runTest {
        addAllLinked()

        for ((type, id) in records) {
            val linked = linkedRecordsFlow(c.linkedRecordRepository, id, type).first()
            assertEquals("linked to the $type", expected(except = type), linked)
            assertEquals(4, linked.count)
        }
    }

    @Test
    fun organization_listsItsOwnNotes_notAnItemsNotes() = runTest {
        val otherItem = Item(itemName = "Phone").stamped()
        val itemNote = Note(title = "item note").stamped()
        c.organizationRepository.addOrganization(organization)
        c.itemRepository.addItem(otherItem)
        c.noteRepository.addNote(note)
        c.noteRepository.addNote(itemNote)
        c.linkedRecordRepository.addLinkedRecord(link(organization.id, RecordType.Organization, note.id, RecordType.Note))
        c.linkedRecordRepository.addLinkedRecord(link(itemNote.id, RecordType.Note, otherItem.id, RecordType.Item))

        val linked = linkedRecordsFlow(c.linkedRecordRepository, organization.id, RecordType.Organization).first()

        assertEquals(listOf(note), linked.linkedNotes)
    }

    @Test
    fun updates_whenALinkIsAddedOrRemoved() = runTest {
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(organization)

        linkedRecordsFlow(c.linkedRecordRepository, contact.id, RecordType.Contact).test {
            assertEquals(LinkedRecords(), awaitItem())

            c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, organization.id, RecordType.Organization))
            var linked = awaitItem()
            while (linked.linkedOrganizations.isEmpty()) linked = awaitItem()
            assertEquals(listOf(organization), linked.linkedOrganizations)

            c.linkedRecordRepository.deleteLinksToRecordID(organization.id)
            while (linked.linkedOrganizations.isNotEmpty()) linked = awaitItem()
            assertEquals(LinkedRecords(), linked)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
