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
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchRepositoryTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val repository = SearchRepository(db.searchDao())

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        db.noteDao().addNote(Note(id = "n1", title = "N1", updatedAt = 10L))
        db.noteDao().addNote(Note(id = "n2", title = "N2", updatedAt = 4L))
        db.contactDao().addContact(Contact(id = "c1", nameLast = "C1", updatedAt = 8L))
        db.contactDao().addContact(Contact(id = "c2", nameLast = "C2", updatedAt = null))
        db.organizationDao().addOrganization(Organization(id = "o1", organizationName = "O1", updatedAt = 9L))
        db.organizationDao().addOrganization(Organization(id = "o2", organizationName = "O2", updatedAt = 6L))
        db.itemDao().addItem(Item(id = "i1", itemName = "I1", updatedAt = 7L))
        db.itemDao().addItem(Item(id = "i2", itemName = "I2", updatedAt = 5L))
    }

    private suspend fun recent(types: Collection<RecordType>, exclude: Collection<String> = emptyList()) =
        repository.getRecentlyChanged(types, exclude).first().map { it.recordId }

    @Test
    fun mergesTypes_newestFirst_atMostFive() = runTest {
        seed()
        assertEquals(listOf("n1", "o1", "c1", "i1", "o2"), recent(RecordType.entries))
    }

    @Test
    fun onlyTheTypesAsked() = runTest {
        seed()
        assertEquals(listOf("c1", "i1", "i2"), recent(listOf(RecordType.Contact, RecordType.Item)))
    }

    @Test
    fun leavesOutExcludedIds() = runTest {
        seed()
        assertEquals(listOf("n1", "c1", "i1", "o2", "i2"), recent(RecordType.entries, exclude = listOf("o1")))
    }

    @Test
    fun noTypes_isEmpty() = runTest {
        seed()
        assertEquals(emptyList<String>(), recent(emptyList()))
    }
}
