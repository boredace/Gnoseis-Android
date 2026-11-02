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
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.TestAppContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GetLinkSourceUseCaseTest {
    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun useCase() = GetLinkSourceUseCase(
        categoryRepository = c.categoryRepository,
        contactRepository = c.contactRepository,
        itemRepository = c.itemRepository,
        noteRepository = c.noteRepository,
        organizationRepository = c.organizationRepository,
    )

    @Test
    fun contactWithBothNames_isFirstThenLast() = runTest {
        val contact = Contact(nameFirst = "Anna", nameLast = "Kowalski")
        c.contactRepository.addContact(contact)

        assertEquals(LinkSource(RecordType.Contact, "Anna Kowalski"), useCase()(RecordType.Contact, contact.id))
    }

    @Test
    fun contactWithOnlyLastName_hasNoLeadingSpace() = runTest {
        val contact = Contact(nameFirst = null, nameLast = "Kowalski")
        c.contactRepository.addContact(contact)

        assertEquals(LinkSource(RecordType.Contact, "Kowalski"), useCase()(RecordType.Contact, contact.id))
    }

    @Test
    fun organizationCategoryAndItem_useTheirNames() = runTest {
        val org = Organization(organizationName = "Northwind Traders")
        val category = Category(categoryName = "Suppliers")
        val item = Item(itemName = "Invoice 42")
        c.organizationRepository.addOrganization(org)
        c.categoryRepository.addCategory(category)
        c.itemRepository.addItem(item)

        assertEquals(LinkSource(RecordType.Organization, "Northwind Traders"), useCase()(RecordType.Organization, org.id))
        assertEquals(LinkSource(RecordType.Category, "Suppliers"), useCase()(RecordType.Category, category.id))
        assertEquals(LinkSource(RecordType.Item, "Invoice 42"), useCase()(RecordType.Item, item.id))
    }

    @Test
    fun noteWithTitle_usesTitle() = runTest {
        val note = Note(title = "Meeting notes", createDate = NOTE_DATE)
        c.noteRepository.addNote(note)

        assertEquals(LinkSource(RecordType.Note, "Meeting notes"), useCase()(RecordType.Note, note.id))
    }

    @Test
    fun noteWithoutTitle_usesItsDate() = runTest {
        val untitled = Note(title = null, textContents = "text", createDate = NOTE_DATE)
        val blank = Note(title = "  ", createDate = NOTE_DATE)
        c.noteRepository.addNote(untitled)
        c.noteRepository.addNote(blank)

        assertEquals(LinkSource(RecordType.Note, "2026-10-28"), useCase()(RecordType.Note, untitled.id))
        assertEquals(LinkSource(RecordType.Note, "2026-10-28"), useCase()(RecordType.Note, blank.id))
    }

    @Test
    fun blankName_fallsBackToTypeName() = runTest {
        val contact = Contact(nameFirst = " ", nameLast = "")
        val org = Organization(organizationName = "")
        val category = Category(categoryName = " ")
        val item = Item(itemName = "")
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.categoryRepository.addCategory(category)
        c.itemRepository.addItem(item)

        assertEquals(LinkSource(RecordType.Contact, "Contact"), useCase()(RecordType.Contact, contact.id))
        assertEquals(LinkSource(RecordType.Organization, "Organization"), useCase()(RecordType.Organization, org.id))
        assertEquals(LinkSource(RecordType.Category, "Category"), useCase()(RecordType.Category, category.id))
        assertEquals(LinkSource(RecordType.Item, "Item"), useCase()(RecordType.Item, item.id))
    }

    @Test
    fun missingRecord_isNull() = runTest {
        listOf(
            RecordType.Contact, RecordType.Organization, RecordType.Category, RecordType.Item, RecordType.Note,
        ).forEach { assertNull(it.name, useCase()(it, "missing-id")) }
    }

    private companion object {
        /** 2026-10-28 at UTC midnight, the day the note list shows. */
        const val NOTE_DATE = 1_793_145_600_000L
    }
}
