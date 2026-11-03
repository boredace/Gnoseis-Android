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
    fun everyLinkedRecordsQuery_findsTheLinkedRecordOfItsType_fromEverySourceType() = runTest {
        insertAllRecords()
        linkEveryPair()

        val sources = listOf(
            category.id to RecordType.Category,
            contact.id to RecordType.Contact,
            item.id to RecordType.Item,
            note.id to RecordType.Note,
            org.id to RecordType.Organization,
        )
        for ((id, type) in sources) {
            // Each source is linked to one record of every other type: each query returns only
            // the record of its own type, and nothing for the source's own type.
            assertEquals(if (type == RecordType.Category) emptyList() else listOf(category), dao.getLinkedCategoriesForRecordId(id).first())
            assertEquals(if (type == RecordType.Contact) emptyList() else listOf(contact), dao.getLinkedContactsForRecordId(id).first())
            assertEquals(if (type == RecordType.Item) emptyList() else listOf(item), dao.getLinkedItemsForRecordId(id).first())
            assertEquals(if (type == RecordType.Note) emptyList() else listOf(note), dao.getLinkedNotesForRecordId(id).first())
            assertEquals(if (type == RecordType.Organization) emptyList() else listOf(org), dao.getLinkedOrganizationsForRecordId(id).first())
        }
    }

    @Test
    fun linkedRecordsQueries_findLinksStoredEitherWayRound() = runTest {
        insertAllRecords()
        // The note is record1 in its link to the contact and record2 in its link to the organization
        dao.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))
        dao.addLinkedRecord(link(org.id, RecordType.Organization, note.id, RecordType.Note))

        assertEquals(listOf(contact), dao.getLinkedContactsForRecordId(note.id).first())
        assertEquals(listOf(org), dao.getLinkedOrganizationsForRecordId(note.id).first())
        assertEquals(listOf(note), dao.getLinkedNotesForRecordId(contact.id).first())
        assertEquals(listOf(note), dao.getLinkedNotesForRecordId(org.id).first())
    }

    @Test
    fun linkedRecordsQueries_followTheLinksTypeColumns() = runTest {
        insertAllRecords()
        // A link that stores the note's end under the Item type isn't one of the contact's notes
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Item))

        assertEquals(emptyList<Note>(), dao.getLinkedNotesForRecordId(contact.id).first())
        assertEquals(emptyList<Item>(), dao.getLinkedItemsForRecordId(contact.id).first())
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
    fun duplicateLinks_areDedupedInLists_andInTabCounts() = runTest {
        // Raw inserts, as databases from before #13 can contain: same pair twice and reversed
        insertAllRecords()
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))

        assertEquals(listOf(note), dao.getLinkedNotesForRecordId(contact.id).first())
        // #23: the tab badge matches the one note the tab lists
        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Note.recordTypeId, 1)),
            dao.getLinkedRecordTypesForRecordIdStream(contact.id).first(),
        )
    }

    @Test
    fun addLinkedRecordIfAbsent_insertsNewLink_butSkipsExistingPairInEitherDirection() = runTest {
        suspend fun addIfAbsent(l: LinkedRecord) = dao.addLinkedRecordIfAbsent(
            l.id, l.ownerDbId, l.record1Id, l.record2Id, l.record1TypeId, l.record2TypeId, l.createdAt,
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

        assertEquals(emptyList<Note>(), dao.getLinkedNotesForRecordId(contact.id).first())
        assertEquals(listOf(note.id), dao.getRecordIdsLinkedToRecordId(contact.id).first())
        assertEquals(
            listOf(LinkedRecordTypeCount(RecordType.Note.recordTypeId, 1)),
            dao.getLinkedRecordTypesForRecordIdStream(contact.id).first(),
        )
    }

    @Test
    fun linkedListFlow_emitsWhenLinkAdded_andWhenLinkedRecordChanges() = runTest {
        insertAllRecords()
        dao.getLinkedNotesForRecordId(contact.id).test {
            assertEquals(emptyList<Note>(), awaitItem())
            dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
            assertEquals(listOf(note), awaitItem())
            db.noteDao().updateNote(note.id, "renamed", note.textContents, note.createDate, note.createDateTime, now = 5)
            assertEquals(listOf(note.copy(title = "renamed", updatedAt = 5)), awaitItem())
            dao.deleteLinksToRecordID(note.id)
            assertEquals(emptyList<Note>(), awaitItem())
        }
    }

    /** #11: unlinking removes the pair however it's stored, duplicates included, and nothing else. */
    @Test
    fun deleteLinkBetween_removesThePairInBothDirections_andItsDuplicates_only() = runTest {
        insertAllRecords()
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(note.id, RecordType.Note, contact.id, RecordType.Contact))
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, note.id, RecordType.Note))
        dao.addLinkedRecord(link(contact.id, RecordType.Contact, org.id, RecordType.Organization))
        dao.addLinkedRecord(link(note.id, RecordType.Note, item.id, RecordType.Item))

        assertEquals(3, dao.deleteLinkBetween(note.id, contact.id))

        assertEquals(listOf(org.id), dao.getRecordIdsLinkedToRecordId(contact.id).first())
        assertEquals(listOf(item.id), dao.getRecordIdsLinkedToRecordId(note.id).first())
        assertEquals(0, dao.deleteLinkBetween(contact.id, note.id))
    }

    @Test
    fun getLinkBetween_findsTheLinkEitherWayRound() = runTest {
        insertAllRecords()
        val stored = link(org.id, RecordType.Organization, item.id, RecordType.Item)
        dao.addLinkedRecord(stored)

        assertEquals(stored, dao.getLinkBetween(org.id, item.id))
        assertEquals(stored, dao.getLinkBetween(item.id, org.id))
        assertEquals(null, dao.getLinkBetween(org.id, note.id))
    }
}
