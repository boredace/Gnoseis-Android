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
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.inMemoryDatabase
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * #50: the repositories stamp createdAt/updatedAt. Inserts set both; an update moves only
 * updatedAt, and only when an editable field changed. Updates never write ownerDbId or
 * createdAt, whatever the passed record holds.
 */
@RunWith(AndroidJUnit4::class)
class RecordTimestampsTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private var clock = 100L
    private val now = { clock }

    private val notes = NoteRepository(db.noteDao(), now)
    private val contacts = ContactRepository(db.contactDao(), now)
    private val organizations = OrganizationRepository(db.organizationDao(), now)
    private val categories = CategoryRepository(db.categoryDao(), now)
    private val items = ItemRepository(db.itemDao(), now)
    private val links = LinkedRecordRepository(db.linkedRecordDao(), now)

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_stampsCreatedAndUpdated_withTheSameTime() = runTest {
        val note = Note(title = "n")
        val contact = Contact(nameLast = "Doe")
        val organization = Organization(organizationName = "Acme")
        val category = Category(categoryName = "Work")
        val item = Item(itemName = "Laptop")

        notes.addNote(note)
        contacts.addContact(contact)
        organizations.addOrganization(organization)
        categories.addCategory(category)
        items.addItem(item)

        assertEquals(note.copy(createdAt = 100, updatedAt = 100), notes.getNote(note.id))
        assertEquals(contact.copy(createdAt = 100, updatedAt = 100), contacts.getContact(contact.id))
        assertEquals(organization.copy(createdAt = 100, updatedAt = 100), organizations.getOrganization(organization.id))
        assertEquals(category.copy(createdAt = 100, updatedAt = 100), categories.getCategory(category.id))
        assertEquals(item.copy(createdAt = 100, updatedAt = 100), items.getItem(item.id))
    }

    @Test
    fun insert_replacesTimesThePassedRecordHolds() = runTest {
        val note = Note(title = "n", createdAt = 1, updatedAt = 2)

        notes.addNote(note)

        assertEquals(note.copy(createdAt = 100, updatedAt = 100), notes.getNote(note.id))
    }

    @Test
    fun addLinkedRecord_stampsCreatedAt() = runTest {
        links.addLinkedRecord(link("c", RecordType.Contact, "n", RecordType.Note).copy(id = "l"))

        val createdAt = db.openHelper.writableDatabase
            .query("SELECT createdAt FROM LinkedRecord WHERE id = 'l'")
            .use { it.moveToFirst(); it.getLong(0) }
        assertEquals(100L, createdAt)
    }

    @Test
    fun saveWithoutChanges_leavesUpdatedAtUnchanged() = runTest {
        val note = Note(title = "n", textContents = "t")
        val contact = Contact(nameLast = "Doe", emailMain = "a@x.io")
        val organization = Organization(organizationName = "Acme")
        val category = Category(categoryName = "Work")
        val item = Item(itemName = "Laptop", comments = "c")
        notes.addNote(note)
        contacts.addContact(contact)
        organizations.addOrganization(organization)
        categories.addCategory(category)
        items.addItem(item)
        clock = 200

        assertFalse(notes.updateNote(notes.getNote(note.id)))
        assertFalse(contacts.updateContact(contacts.getContact(contact.id)))
        assertFalse(organizations.updateOrganization(organizations.getOrganization(organization.id)))
        assertFalse(categories.updateCategory(categories.getCategory(category.id)))
        assertFalse(items.updateItem(items.getItem(item.id)))

        assertEquals(100L, notes.getNote(note.id).updatedAt)
        assertEquals(100L, contacts.getContact(contact.id).updatedAt)
        assertEquals(100L, organizations.getOrganization(organization.id).updatedAt)
        assertEquals(100L, categories.getCategory(category.id).updatedAt)
        assertEquals(100L, items.getItem(item.id).updatedAt)
    }

    @Test
    fun edit_movesOnlyUpdatedAt_andKeepsOwnerAndCreatedAt() = runTest {
        val note = Note(ownerDbId = "owner", title = "n")
        val contact = Contact(ownerDbId = "owner", nameLast = "Doe")
        val organization = Organization(ownerDbId = "owner", organizationName = "Acme")
        val category = Category(ownerDbId = "owner", categoryName = "Work")
        val item = Item(ownerDbId = "owner", itemName = "Laptop")
        notes.addNote(note)
        contacts.addContact(contact)
        organizations.addOrganization(organization)
        categories.addCategory(category)
        items.addItem(item)
        clock = 200

        // The passed records carry a different owner and createdAt; neither may be written.
        assertTrue(notes.updateNote(note.copy(ownerDbId = "other", createdAt = 1, title = "n2")))
        assertTrue(contacts.updateContact(contact.copy(ownerDbId = "other", createdAt = 1, phoneWork = "5")))
        assertTrue(organizations.updateOrganization(organization.copy(ownerDbId = "other", createdAt = 1, comments = "c")))
        assertTrue(categories.updateCategory(category.copy(ownerDbId = "other", createdAt = 1, categoryName = "Home")))
        assertTrue(items.updateItem(item.copy(ownerDbId = "other", createdAt = 1, itemName = "Desktop")))

        assertEquals(note.copy(title = "n2", createdAt = 100, updatedAt = 200), notes.getNote(note.id))
        assertEquals(contact.copy(phoneWork = "5", createdAt = 100, updatedAt = 200), contacts.getContact(contact.id))
        assertEquals(organization.copy(comments = "c", createdAt = 100, updatedAt = 200), organizations.getOrganization(organization.id))
        assertEquals(category.copy(categoryName = "Home", createdAt = 100, updatedAt = 200), categories.getCategory(category.id))
        assertEquals(item.copy(itemName = "Desktop", createdAt = 100, updatedAt = 200), items.getItem(item.id))
    }

    @Test
    fun edit_clearingAField_countsAsAChange() = runTest {
        val contact = Contact(nameLast = "Doe", nameFirst = "Jane")
        contacts.addContact(contact)
        clock = 200

        assertTrue(contacts.updateContact(contact.copy(nameFirst = null)))

        assertEquals(contact.copy(nameFirst = null, createdAt = 100, updatedAt = 200), contacts.getContact(contact.id))
    }

    @Test
    fun edit_noteDate_countsAsAChange() = runTest {
        val note = Note(title = "n", createDate = 0, createDateTime = 0)
        notes.addNote(note)
        clock = 200

        assertTrue(notes.updateNote(note.copy(createDate = 86_400_000, createDateTime = 86_400_000)))

        assertEquals(
            note.copy(createDate = 86_400_000, createDateTime = 86_400_000, createdAt = 100, updatedAt = 200),
            notes.getNote(note.id),
        )
    }

    @Test
    fun edit_rowWithNullTimestamps_setsOnlyUpdatedAt() = runTest {
        // A row from before v3 (or a v2 backup) keeps createdAt NULL after an edit.
        val item = Item(itemName = "Laptop")
        db.itemDao().addItem(item)
        clock = 200

        assertTrue(items.updateItem(item.copy(itemName = "Desktop")))

        assertEquals(item.copy(itemName = "Desktop", updatedAt = 200), items.getItem(item.id))
    }

    @Test
    fun update_neverWritesParentId() = runTest {
        val item = Item(itemName = "Laptop", parentId = "p")
        val category = Category(categoryName = "Work", parentId = "p")
        items.addItem(item)
        categories.addCategory(category)

        items.updateItem(item.copy(parentId = "q", itemName = "Desktop"))
        categories.updateCategory(category.copy(parentId = "q", categoryName = "Home"))

        assertEquals("p", items.getItem(item.id).parentId)
        assertEquals("p", categories.getCategory(category.id).parentId)
    }
}
