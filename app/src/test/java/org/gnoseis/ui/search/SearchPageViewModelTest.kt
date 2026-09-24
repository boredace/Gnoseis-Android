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

package org.gnoseis.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.knownBug
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchPageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun vm() = SearchPageViewModel(SavedStateHandle(), c.searchRepository)

    private suspend fun ReceiveTurbine<List<SearchResult>>.awaitResults(predicate: (List<SearchResult>) -> Boolean): List<SearchResult> {
        var results = awaitItem()
        while (!predicate(results)) results = awaitItem()
        return results
    }

    private val note = Note(title = "Budget meeting")
    private val contact = Contact(nameLast = "Meyer", nameFirst = "Ann")
    private val item = Item(itemName = "Projector")
    private val category = Category(categoryName = "Finance")

    private suspend fun seed() {
        c.noteRepository.addNote(note)
        c.contactRepository.addContact(contact)
        c.itemRepository.addItem(item)
        c.categoryRepository.addCategory(category)
    }

    @Test
    fun initialState_isEmpty() = runTest {
        vm().searchResults.test {
            assertEquals(emptyList<SearchResult>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyQuery_showsAllRecords() = runTest {
        seed()
        vm().searchResults.test {
            val results = awaitResults { it.isNotEmpty() }
            assertEquals(setOf(note.id, contact.id, item.id, category.id), results.map { it.recordId }.toSet())
        }
    }

    @Test
    fun query_filtersByTitle_caseInsensitive_partial() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            awaitResults { it.isNotEmpty() }
            vm.updateCurrentQuery("MEE")
            assertEquals(listOf(note.id), awaitResults { it.size == 1 }.map { it.recordId })
            vm.updateCurrentQuery("zzz")
            awaitResults { it.isEmpty() }
        }
    }

    @Test
    fun query_matchesContactDisplayTitle() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            awaitResults { it.isNotEmpty() }
            vm.updateCurrentQuery("meyer, a")
            assertEquals(listOf(contact.id), awaitResults { it.size == 1 }.map { it.recordId })
        }
    }

    @Test
    fun contactWithoutFirstName_appearsInResults() = runTest {
        knownBug(
            "Bug 6 - the Search page crashes when any contact has no first name (the SearchDao title is NULL, see SearchDaoTest). Fix: use COALESCE(', ' || NameFirst, '') in SearchDao.",
        ) {
            seed()
            val lastNameOnly = Contact(nameLast = "Solo", nameFirst = null)
            c.contactRepository.addContact(lastNameOnly)

            vm().searchResults.test {
                val results = awaitResults { it.isNotEmpty() }
                assertTrue(results.any { it.recordId == lastNameOnly.id })
            }
        }
    }
}
