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
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.CreateFromSearchState
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
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private fun vm(scope: RecordType? = null) = mainDispatcherRule.viewModel {
        SearchPageViewModel(
            SavedStateHandle(scope?.let { mapOf("scopeTypeId" to it.recordTypeId) } ?: emptyMap()),
            c.searchRepository,
            c.linkedRecordRepository,
        )
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        var value = awaitItem()
        while (!predicate(value)) value = awaitItem()
        return value
    }

    private val note = Note(title = "Budget meeting")
    private val contact = Contact(nameLast = "Meyer", nameFirst = "Ann")
    private val item = Item(itemName = "Projector")
    private val category = Category(categoryName = "Finance")
    private val org = Organization(organizationName = "Meetup Org")

    private suspend fun seed() {
        c.noteRepository.addNote(note)
        c.contactRepository.addContact(contact)
        c.itemRepository.addItem(item)
        c.categoryRepository.addCategory(category)
        c.organizationRepository.addOrganization(org)
    }

    private fun ids(results: List<SearchResult>) = results.map { it.recordId }.toSet()

    @Test
    fun initialState_isEmpty() = runTest {
        vm().searchResults.test {
            assertEquals(emptyList<SearchResult>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noResults_beforeThreeCharacters() = runTest {
        seed()
        val vm = vm()
        vm.uiState.test {
            vm.updateCurrentQuery("me")
            val state = awaitUntil { it.query == "me" && it.recent.isNotEmpty() }
            assertTrue(state.results.isEmpty)
            assertTrue(!state.isSearching)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun query_filtersByTitle_caseInsensitive_partial() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.updateCurrentQuery("MEE")
            assertEquals(setOf(note.id, org.id), ids(awaitUntil { it.size == 2 }))
            vm.updateCurrentQuery("zzz")
            awaitUntil { it.isEmpty() }
        }
    }

    @Test
    fun query_matchesContactInBothNameOrders() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.updateCurrentQuery("meyer, a")
            assertEquals(setOf(contact.id), ids(awaitUntil { it.size == 1 }))
            vm.updateCurrentQuery("zzz")
            awaitUntil { it.isEmpty() }
            vm.updateCurrentQuery("ann mey")
            assertEquals(setOf(contact.id), ids(awaitUntil { it.size == 1 }))
        }
    }

    @Test
    fun contactWithoutFirstName_appearsInResults() = runTest {
        seed()
        val lastNameOnly = Contact(nameLast = "Solo", nameFirst = null)
        c.contactRepository.addContact(lastNameOnly)
        val vm = vm()
        vm.searchResults.test {
            vm.updateCurrentQuery("solo")
            assertTrue(awaitUntil { it.isNotEmpty() }.any { it.recordId == lastNameOnly.id })
        }
    }

    @Test
    fun results_bestMatchFirst_thenGroupsInTypeOrder() = runTest {
        seed()
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, item.id, RecordType.Item))
        val vm = vm()
        vm.uiState.test {
            vm.updateCurrentQuery("meet")
            val state = awaitUntil { it.results.total == 2 && it.bestMatchLinks.isNotEmpty() }
            // The organization name starts with the query; the note only has a word that does
            assertEquals(org.id, state.results.bestMatch?.recordId)
            assertEquals(listOf(RecordType.Note), state.results.groups.map { it.type })
            assertEquals(1, state.bestMatchLinks.single { it.recordTypeId == RecordType.Item.recordTypeId }.count)
            assertEquals(1, state.results.counts[RecordType.Organization])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun chips_filterTheResultsFound() = runTest {
        seed()
        val vm = vm()
        vm.uiState.test {
            vm.updateCurrentQuery("meet")
            awaitUntil { it.results.total == 2 }
            vm.toggleChip(RecordType.Note)
            val notesOnly = awaitUntil { it.selectedChips == setOf(RecordType.Note) }
            assertEquals(note.id, notesOnly.results.bestMatch?.recordId)
            assertTrue(notesOnly.results.groups.isEmpty())
            assertEquals(2, notesOnly.results.total) // the chips count all matches
            vm.toggleChip(null)
            awaitUntil { it.selectedChips.isEmpty() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fromHome_allTypes() {
        val vm = vm()
        assertEquals(null, vm.scope)
        assertEquals(emptySet<RecordType>(), vm.uiState.value.selectedChips)
        assertEquals("Search all records", vm.uiState.value.placeholder)
        assertEquals(searchTypeOrder, vm.uiState.value.createTypes)
    }

    @Test
    fun fromAListPage_startsWithItsType_andCountsTheOthersAsHidden() = runTest {
        seed()
        val vm = vm(RecordType.Contact)
        assertEquals(setOf(RecordType.Contact), vm.uiState.value.selectedChips)
        assertEquals("Search contacts", vm.uiState.value.placeholder)
        assertEquals(RecordType.Contact, vm.uiState.value.createTypes.first())
        vm.uiState.test {
            vm.updateCurrentQuery("meet")
            val state = awaitUntil { it.results.total == 2 }
            assertTrue(state.results.isEmpty)
            assertEquals(mapOf(RecordType.Note to 1, RecordType.Organization to 1), state.results.hidden)
            assertEquals(RecordType.Contact, state.scope)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentlyChanged_followsTheChips() = runTest {
        seed()
        val vm = vm(RecordType.Item)
        vm.uiState.test {
            assertEquals(listOf(item.id), awaitUntil { it.recent.isNotEmpty() }.recent.map { it.recordId })
            vm.toggleChip(null)
            assertEquals(5, awaitUntil { it.recent.size == 5 }.recent.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- create from search ----------

    private suspend fun SearchPageViewModel.settledOffer(query: String): CreateFromSearchOffer {
        updateCurrentQuery(query)
        var state = CreateFromSearchState()
        createFromSearch.test {
            state = awaitItem()
            while (state.query != query) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        return state.offer
    }

    @Test
    fun createOffer_onlyForASettledQueryOfThreeOrMoreCharacters_withNoResults() = runTest {
        seed()
        val vm = vm()
        assertEquals(CreateFromSearchOffer.None, vm.createFromSearch.value.offer)
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("zz"))
        assertEquals(CreateFromSearchOffer.NoResults, vm.settledOffer("zzz"))
    }

    @Test
    fun createOffer_afterResults_unlessOneMatchesExactly() = runTest {
        seed()
        val vm = vm()
        assertEquals(CreateFromSearchOffer.AfterResults, vm.settledOffer("budget"))
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer(" BUDGET  meeting "))
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("Meyer, Ann"))
    }

    @Test
    fun createOffer_exactMatchHiddenByTheChips_stillCounts() = runTest {
        seed()
        val vm = vm(RecordType.Contact)
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("projector"))
    }
}
