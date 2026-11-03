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

package org.gnoseis.data.entity.home

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
class HomeDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.homeDao()

    @After
    fun tearDown() = db.close()

    private suspend fun addLink(id1: String, type1: RecordType, id2: String, type2: RecordType) =
        db.linkedRecordDao().addLinkedRecord(link(id1, type1, id2, type2))

    private suspend fun note(id: String, createDateTime: Long = 0, createDate: Long = 0, title: String = id) =
        db.noteDao().addNote(Note(id = id, title = title, createDateTime = createDateTime, createDate = createDate))

    @Test
    fun recordCounts_countEveryTable() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        db.organizationDao().addOrganization(Organization(id = "o", organizationName = "Acme"))
        db.categoryDao().addCategory(Category(id = "g", categoryName = "Work"))
        db.itemDao().addItem(Item(id = "i1", itemName = "A"))
        db.itemDao().addItem(Item(id = "i2", itemName = "B"))
        note("n")
        addLink("c", RecordType.Contact, "n", RecordType.Note)

        assertEquals(RecordCounts(contacts = 1, organizations = 1, notes = 1, categories = 1, items = 2, links = 1), dao.getRecordCounts().first())
    }

    @Test
    fun mostLinked_countsBothDirections_leavesOutNotes_andBreaksTiesByName() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameFirst = "Jane", nameLast = "Doe"))
        db.organizationDao().addOrganization(Organization(id = "o", organizationName = "acme"))
        db.categoryDao().addCategory(Category(id = "g", categoryName = "Work"))
        db.itemDao().addItem(Item(id = "i", itemName = "Laptop"))
        db.itemDao().addItem(Item(id = "unlinked", itemName = "Spare"))
        note("n1"); note("n2"); note("n3")
        // c: 3 links (as record1 and record2); o and g: 2 each (tie, "acme" before "Work"); i: 1
        addLink("c", RecordType.Contact, "n1", RecordType.Note)
        addLink("n2", RecordType.Note, "c", RecordType.Contact)
        addLink("o", RecordType.Organization, "c", RecordType.Contact)
        addLink("o", RecordType.Organization, "n3", RecordType.Note)
        addLink("g", RecordType.Category, "n1", RecordType.Note)
        addLink("i", RecordType.Item, "g", RecordType.Category)

        assertEquals(
            listOf(
                LinkedRecordCount("c", "Jane Doe", RecordType.Contact.recordTypeId, 3),
                LinkedRecordCount("o", "acme", RecordType.Organization.recordTypeId, 2),
                LinkedRecordCount("g", "Work", RecordType.Category.recordTypeId, 2),
                LinkedRecordCount("i", "Laptop", RecordType.Item.recordTypeId, 1),
            ),
            dao.getMostLinked(10, null).first(),
        )
        assertEquals(listOf("c", "o"), dao.getMostLinked(2, null).first().map { it.id })
    }

    @Test
    fun mostLinked_ofOneType_onlyHasThatType() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        db.categoryDao().addCategory(Category(id = "g1", categoryName = "Work"))
        db.categoryDao().addCategory(Category(id = "g2", categoryName = "Home"))
        db.categoryDao().addCategory(Category(id = "unlinked", categoryName = "Spare"))
        note("n1"); note("n2"); note("n3")
        addLink("c", RecordType.Contact, "n1", RecordType.Note)
        addLink("c", RecordType.Contact, "n2", RecordType.Note)
        addLink("c", RecordType.Contact, "n3", RecordType.Note)
        addLink("g1", RecordType.Category, "n1", RecordType.Note)
        addLink("g2", RecordType.Category, "n1", RecordType.Note)
        addLink("g2", RecordType.Category, "c", RecordType.Contact)

        assertEquals(listOf("g2", "g1"), dao.getMostLinked(10, RecordType.Category.recordTypeId).first().map { it.id })
        assertEquals(listOf("g2"), dao.getMostLinked(1, RecordType.Category.recordTypeId).first().map { it.id })
        assertEquals(emptyList<LinkedRecordCount>(), dao.getMostLinked(10, RecordType.Item.recordTypeId).first())
    }

    @Test
    fun mostLinked_contactWithoutFirstName_usesLastName() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        note("n")
        addLink("c", RecordType.Contact, "n", RecordType.Note)

        assertEquals("Doe", dao.getMostLinked(10, null).first().single().name)
    }

    @Test
    fun linksAmong_onlyLinksWithBothEndsInTheSet() = runTest {
        addLink("a", RecordType.Contact, "b", RecordType.Organization)
        addLink("c", RecordType.Category, "a", RecordType.Contact)
        addLink("a", RecordType.Contact, "outside", RecordType.Note)

        assertEquals(
            setOf(LinkEnds("a", "b"), LinkEnds("c", "a")),
            dao.getLinksAmong(listOf("a", "b", "c")).first().toSet(),
        )
    }

    @Test
    fun linksPerType_countsSameTypeLinksOnce_andLinksFromNotes_andLeavesOutTypesWithNone() = runTest {
        addLink("c1", RecordType.Contact, "c2", RecordType.Contact) // same type: counts once
        addLink("c1", RecordType.Contact, "n", RecordType.Note)
        addLink("n", RecordType.Note, "o", RecordType.Organization)
        addLink("o", RecordType.Organization, "g", RecordType.Category)
        addLink("n", RecordType.Note, "n2", RecordType.Note) // notes aren't a picker type

        assertEquals(
            listOf(
                TypeLinkCount(RecordType.Contact.recordTypeId, 2),
                TypeLinkCount(RecordType.Organization.recordTypeId, 2),
                TypeLinkCount(RecordType.Category.recordTypeId, 1),
            ),
            dao.getLinksPerType().first(),
        )
    }

    @Test
    fun recentNotes_newestFirst_withLinkCounts() = runTest {
        note("old", createDateTime = 1)
        note("mid", createDateTime = 2)
        note("new", createDateTime = 3)
        note("newest", createDateTime = 4)
        addLink("mid", RecordType.Note, "c", RecordType.Contact)
        addLink("o", RecordType.Organization, "mid", RecordType.Note)

        val recent = dao.getRecentNotes(3).first()

        assertEquals(listOf("newest", "new", "mid"), recent.map { it.id })
        assertEquals(listOf(0, 0, 2), recent.map { it.linkCount })
    }

    @Test
    fun notesPerDay_countsByNoteDate_withinTheRange() = runTest {
        val day = 86_400_000L
        note("before", createDate = 9 * day)
        note("a", createDate = 10 * day)
        note("b", createDate = 10 * day)
        note("c", createDate = 12 * day)
        note("after", createDate = 13 * day)

        assertEquals(
            listOf(DayCount(10, 2), DayCount(12, 1)),
            dao.getNotesPerDay(fromDay = 10, toDay = 12).first().sortedBy { it.day },
        )
    }

    @Test
    fun unlinkedNotes_newestFirst_andTheirTotal() = runTest {
        note("linked", createDateTime = 5)
        note("u1", createDateTime = 1)
        note("u2", createDateTime = 2)
        note("u3", createDateTime = 3)
        note("u4", createDateTime = 4)
        addLink("c", RecordType.Contact, "linked", RecordType.Note)

        assertEquals(listOf("u4", "u3", "u2"), dao.getUnlinkedNotes(3).first().map { it.id })
        assertEquals(4, dao.getUnlinkedNoteCount().first())
    }

    @Test
    fun changeCountSince_countsCreatedAndEditedRecordsAndLinks_butNotNullTimes() = runTest {
        db.noteDao().addNote(Note(id = "before", createdAt = 50, updatedAt = 50))
        db.noteDao().addNote(Note(id = "edited", createdAt = 50, updatedAt = 150))
        db.noteDao().addNote(Note(id = "legacy")) // NULL times, from before v3
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe", createdAt = 150, updatedAt = 150))
        db.organizationDao().addOrganization(Organization(id = "o", createdAt = 150, updatedAt = 150))
        db.categoryDao().addCategory(Category(id = "g", createdAt = 150, updatedAt = 150))
        db.itemDao().addItem(Item(id = "i", createdAt = 150, updatedAt = 150))
        db.linkedRecordDao().addLinkedRecord(link("c", RecordType.Contact, "o", RecordType.Organization).copy(createdAt = 150))
        db.linkedRecordDao().addLinkedRecord(link("c", RecordType.Contact, "g", RecordType.Category).copy(createdAt = 50))

        assertEquals(6, dao.getChangeCountSince(100).first())
        assertEquals(0, dao.getChangeCountSince(150).first())
    }

    @Test
    fun flows_updateWhenALinkIsAdded() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        note("n")

        dao.getMostLinked(10, null).test {
            assertEquals(emptyList<LinkedRecordCount>(), awaitItem())
            addLink("c", RecordType.Contact, "n", RecordType.Note)
            assertEquals(listOf("c"), awaitItem().map { it.id })
        }
    }

    /** #11: an unlinked pair stops counting towards Most linked. */
    @Test
    fun mostLinked_dropsAfterAnUnlink() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        db.organizationDao().addOrganization(Organization(id = "o", organizationName = "acme"))
        note("n1")
        addLink("c", RecordType.Contact, "n1", RecordType.Note)
        addLink("o", RecordType.Organization, "c", RecordType.Contact)

        db.linkedRecordDao().deleteLinkBetween("c", "o")

        assertEquals(
            listOf(LinkedRecordCount("c", "Doe", RecordType.Contact.recordTypeId, 1)),
            dao.getMostLinked(10, null).first(),
        )
    }
}
