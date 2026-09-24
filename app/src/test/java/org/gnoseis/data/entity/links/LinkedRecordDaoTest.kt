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

package org.gnoseis.data.entity.links

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkedRecordDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.linkedRecordDao()

    private val category = Category(categoryName = "cat")
    private val contact = Contact(nameLast = "Doe")
    private val item = Item(itemName = "item")
    private val note = Note(title = "note")
    private val org = Organization(organizationName = "org")

    @After
    fun tearDown() = db.close()

    private suspend fun insertAllRecords() {
        db.categoryDao().addCategory(category)
        db.contactDao().addContact(contact)
        db.itemDao().addItem(item)
        db.noteDao().addNote(note)
        db.organizationDao().addOrganization(org)
    }

    /**
     * Links every pair of record types exactly once. Directions alternate so that each
     * query is exercised with the source record on both the record1 and record2 side.
     */
    private suspend fun linkEveryPair() {
        val records = listOf(
            category.id to RecordType.Category,
            contact.id to RecordType.Contact,
            item.id to RecordType.Item,
            note.id to RecordType.Note,
            org.id to RecordType.Organization,
        )
        var flip = false
        for (i in records.indices) for (j in i + 1 until records.size) {
            val (a, aType) = records[i]
            val (b, bType) = records[j]
            dao.addLinkedRecord(if (flip) link(b, bType, a, aType) else link(a, aType, b, bType))
            flip = !flip
        }
    }

    @Test
    fun everyLinkedListQuery_findsTheLinkedRecord_inBothDirections() = runTest {
        insertAllRecords()
        linkEveryPair()

        // Categories
        assertEquals(listOf(category), dao.getLinkedCategoryListByContactId(contact.id).first())
        assertEquals(listOf(category), dao.getLinkedCategoryListByItemId(item.id).first())
        assertEquals(listOf(category), dao.getLinkedCategoryListByNoteId(note.id).first())
        assertEquals(listOf(category), dao.getLinkedCategoryListByOrganizationId(org.id).first())
        // Contacts
        assertEquals(listOf(contact), dao.getLinkedContactListByCategoryId(category.id).first())
        assertEquals(listOf(contact), dao.getLinkedContactListByItemId(item.id).first())
        assertEquals(listOf(contact), dao.getLinkedContactListByNoteId(note.id).first())
        assertEquals(listOf(contact), dao.getLinkedContactListByOrganizationId(org.id).first())
        // Items
        assertEquals(listOf(item), dao.getLinkedItemListByCategoryId(category.id).first())
        assertEquals(listOf(item), dao.getLinkedItemListByContactId(contact.id).first())
        assertEquals(listOf(item), dao.getLinkedItemListByNoteId(note.id).first())
        assertEquals(listOf(item), dao.getLinkedItemListByOrganizationId(org.id).first())
        // Notes
        assertEquals(listOf(note), dao.getLinkedNoteListByCategoryId(category.id).first())
        assertEquals(listOf(note), dao.getLinkedNoteListByContactId(contact.id).first())
        assertEquals(listOf(note), dao.getLinkedNoteListByItemId(item.id).first())
        assertEquals(listOf(note), dao.getLinkedNoteListByOrganizationId(org.id).first())
        // Organizations
        assertEquals(listOf(org), dao.getLinkedOrganizationListByCategoryId(category.id).first())
        assertEquals(listOf(org), dao.getLinkedOrganizationListByContactId(contact.id).first())
        assertEquals(listOf(org), dao.getLinkedOrganizationListByItemId(item.id).first())
        assertEquals(listOf(org), dao.getLinkedOrganizationListByNoteId(note.id).first())
    }

    @Test
    fun linkedRecordTypes_countsPerType_sortedByCountDesc() = runTest {
        insertAllRecords()
        val note2 = Note(title = "note2")
        db.noteDao().addNote(note2)
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(note2.id, RecordType.Note, contact.id, RecordType.Contact))
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, org.id, RecordType.Organization))

        val types = dao.getLinkedRecordTypesForRecordIdStream(contact.id).first()

        assertEquals(
            listOf(
                LinkedRecordTypeCount(RecordType.Note.recordTypeId, 2),
                LinkedRecordTypeCount(RecordType.Organization.recordTypeId, 1),
            ),
            types,
        )
    }

    @Test
    fun recordIdsLinkedToRecordId_andDeleteLinks() = runTest {
        dao.addLinkedRecord(link("a", RecordType.Note, "b", RecordType.Contact))
        dao.addLinkedRecord(link("c", RecordType.Item, "a", RecordType.Note))
        dao.addLinkedRecord(link("b", RecordType.Contact, "c", RecordType.Item))

        assertEquals(setOf("b", "c"), dao.getRecordIdsLinkedToRecordId("a").first().toSet())

        assertEquals(2, dao.deleteLinksToRecordID("a"))
        assertEquals(emptyList<String>(), dao.getRecordIdsLinkedToRecordId("a").first())
        assertEquals(listOf("c"), dao.getRecordIdsLinkedToRecordId("b").first())
        assertEquals(0, dao.deleteLinksToRecordID("a"))
    }

    @Test
    fun duplicateLinks_areDedupedInLists_butDoubleCountedInTabCounts() = runTest {
        insertAllRecords()
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))

        assertEquals(listOf(note), dao.getLinkedNoteListByContactId(contact.id).first())
        // Documented inconsistency: the tab badge says 2 while the tab lists 1 note.
        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Note.recordTypeId, 2)),
            dao.getLinkedRecordTypesForRecordIdStream(contact.id).first(),
        )
    }

    @Test
    fun addLinkedRecordIfAbsent_insertsNewLink_butSkipsExistingPairInEitherDirection() = runTest {
        suspend fun addIfAbsent(l: LinkedRecord) = dao.addLinkedRecordIfAbsent(
            l.id, l.ownerDbId, l.record1Id, l.record2Id, l.record1TypeId, l.record2TypeId,
        )
        val first = link("a", RecordType.Note, "b", RecordType.Contact)

        addIfAbsent(first)
        addIfAbsent(link("a", RecordType.Note, "b", RecordType.Contact))
        addIfAbsent(link("b", RecordType.Contact, "a", RecordType.Note))
        addIfAbsent(link("a", RecordType.Note, "c", RecordType.Item))

        assertEquals(listOf("b", "c"), dao.getRecordIdsLinkedToRecordId("a").first().sorted())
        assertEquals(listOf("a"), dao.getRecordIdsLinkedToRecordId("b").first())
        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Contact.recordTypeId, 1), LinkedRecordTypeCount(RecordType.Item.recordTypeId, 1)),
            dao.getLinkedRecordTypesForRecordIdStream("a").first().sortedBy { it.recordTypeId },
        )
    }

    @Test
    fun deletingRecordDirectly_leavesOrphanLinks() = runTest {
        // There are no foreign keys: only DeleteRecordUseCase cleans up links.
        insertAllRecords()
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))

        db.noteDao().deleteNoteById(note.id)

        assertEquals(emptyList<Note>(), dao.getLinkedNoteListByContactId(contact.id).first())
        assertEquals(listOf(note.id), dao.getRecordIdsLinkedToRecordId(contact.id).first())
        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Note.recordTypeId, 1)),
            dao.getLinkedRecordTypesForRecordIdStream(contact.id).first(),
        )
    }

    @Test
    fun linkedListFlow_emitsWhenLinkAdded_andWhenLinkedRecordChanges() = runTest {
        insertAllRecords()
        dao.getLinkedNoteListByContactId(contact.id).test {
            assertEquals(emptyList<Note>(), awaitItem())
            dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
            assertEquals(listOf(note), awaitItem())
            db.noteDao().updateNote(note.copy(title = "renamed"))
            assertEquals(listOf(note.copy(title = "renamed")), awaitItem())
            dao.deleteLinksToRecordID(note.id)
            assertEquals(emptyList<Note>(), awaitItem())
        }
    }
}
