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

package org.gnoseis.data.entity.search

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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.searchDao()

    @After
    fun tearDown() = db.close()

    private suspend fun search(query: String) = dao.getSearchResultsForQuery(query).first()

    private suspend fun seed() {
        db.noteDao().addNote(Note(id = "n", title = "Meeting notes", textContents = "secret body"))
        db.contactDao().addContact(Contact(id = "c", nameLast = "Meeting", nameFirst = "Mary"))
        db.organizationDao().addOrganization(Organization(id = "o", organizationName = "Meetup Org"))
        db.categoryDao().addCategory(Category(id = "cat", categoryName = "Meetings"))
        db.itemDao().addItem(Item(id = "i", itemName = "Projector"))
    }

    @Test
    fun matchesAcrossAllRecordTypes_withTypeIdsAndTitles() = runTest {
        seed()

        val results = search("%meet%").map { Triple(it.recordId, it.recordTypeId, it.recordTitle) }.toSet()

        assertEquals(
            setOf(
                Triple("n", RecordType.Note.recordTypeId, "Meeting notes"),
                Triple("c", RecordType.Contact.recordTypeId, "Meeting, Mary"),
                Triple("o", RecordType.Organization.recordTypeId, "Meetup Org"),
                Triple("cat", RecordType.Category.recordTypeId, "Meetings"),
            ),
            results,
        )
    }

    @Test
    fun isCaseInsensitive_forAscii() = runTest {
        seed()
        assertEquals(listOf("i"), search("%PROJ%").map { it.recordId })
    }

    @Test
    fun isCaseInsensitive_forNonAsciiToo() = runTest {
        // Stock SQLite only folds ASCII, but Android's SQLite build (used by Robolectric's
        // native mode) also folds non-ASCII letters. To be confirmed on a device in Phase 4.
        db.itemDao().addItem(Item(id = "u", itemName = "Ärger"))
        assertEquals(listOf("u"), search("%Ärg%").map { it.recordId })
        assertEquals(listOf("u"), search("%ärg%").map { it.recordId })
    }

    @Test
    fun partialMatch_anywhereInField() = runTest {
        seed()
        assertEquals(listOf("i"), search("%ject%").map { it.recordId })
        assertEquals(emptyList<SearchResult>(), search("ject"))
    }

    @Test
    fun contactMatchesOnFirstName() = runTest {
        seed()
        assertEquals(listOf("c"), search("%mary%").map { it.recordId })
    }

    @Test
    fun noteTextContents_isNotSearched() = runTest {
        seed()
        assertEquals(emptyList<SearchResult>(), search("%secret%"))
    }

    @Test
    fun emptyQuery_returnsEverything_exceptNotesWithoutTitle() = runTest {
        seed()
        db.noteDao().addNote(Note(id = "untitled", title = null, textContents = "only body"))

        val ids = search("%%").map { it.recordId }.toSet()

        assertEquals(setOf("n", "c", "o", "cat", "i"), ids)
    }

    @Test
    fun noMatches_returnsEmptyList() = runTest {
        seed()
        assertEquals(emptyList<SearchResult>(), search("%zzz%"))
    }

    @Test
    fun contactWithoutFirstName_canBeSearched() = runTest {
        // A contact only needs a last name to be valid (ContactEditViewModel).
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe", nameFirst = null))

        val results = search("%doe%")

        assertEquals(listOf("c"), results.map { it.recordId })
        assertEquals("Doe", results.single().recordTitle)
    }

    @Test
    fun resultsFlow_emitsWhenMatchingRecordAdded() = runTest {
        dao.getSearchResultsForQuery("%lap%").test {
            assertEquals(emptyList<SearchResult>(), awaitItem())
            db.itemDao().addItem(Item(id = "i", itemName = "Laptop"))
            assertEquals(listOf("i"), awaitItem().map { it.recordId })
        }
    }
}
