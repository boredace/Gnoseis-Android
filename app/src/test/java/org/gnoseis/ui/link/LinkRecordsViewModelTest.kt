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


package org.gnoseis.ui.link

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.testutil.stamped
import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.CreateFromSearchState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkRecordsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private val note = Note(title = "Source note").stamped()
    private val otherNote = Note(title = "Other note").stamped()
    private val contact = Contact(nameLast = "Doe", nameFirst = "Jane").stamped()
    private val org = Organization(organizationName = "Acme").stamped()
    private val apple = Organization(organizationName = "Apple Computers").stamped()
    private val pixel = Item(itemName = "Google Pixel 9").stamped()
    private val item = Item(itemName = "Laptop").stamped()
    private val category = Category(categoryName = "Work").stamped()

    private suspend fun seed() {
        c.noteRepository.addNote(note)
        c.noteRepository.addNote(otherNote)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.organizationRepository.addOrganization(apple)
        c.itemRepository.addItem(pixel)
        c.itemRepository.addItem(item)
        c.categoryRepository.addCategory(category)
    }

    private fun vm(sourceId: String = note.id, sourceType: RecordType = RecordType.Note) = mainDispatcherRule.viewModel {
        LinkRecordsViewModel(
            SavedStateHandle(mapOf("sourceRecordId" to sourceId, "sourceRecordType" to sourceType)),
            c.linkedRecordRepository,
            c.searchRepository,
            c.getLinkSourceUseCase(),
        )
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        var value = awaitItem()
        while (!predicate(value)) value = awaitItem()
        return value
    }

    /** Types [query] and waits for its results. */
    private suspend fun ReceiveTurbine<List<SearchResult>>.search(vm: LinkRecordsViewModel, query: String, until: (List<SearchResult>) -> Boolean) =
        run {
            vm.updateCurrentQuery(query)
            awaitUntil(until)
        }

    private fun ids(results: List<SearchResult>) = results.map { it.recordId }.toSet()

    private suspend fun linkedIds() = c.linkedRecordRepository.getRecordIdsLinkedToRecordId(note.id).first().toSet()

    @Test
    fun results_excludeSourceRecord_andRecordsOfTheSameType() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            assertEquals(setOf(category.id), ids(search(vm, "work") { it.isNotEmpty() }))
            // "note" matches both notes, but they're the source and its type
            assertEquals(emptySet<String>(), ids(search(vm, "note") { it.isEmpty() }))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun results_areFilteredByQuery_caseInsensitively() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            assertEquals(setOf(org.id), ids(search(vm, "ACM") { it.size == 1 }))
        }
    }

    @Test
    fun chips_offerOnlyLinkableTypes() {
        assertEquals(
            listOf(RecordType.Contact, RecordType.Organization, RecordType.Category, RecordType.Item),
            vm().uiState.value.chipTypes,
        )
        assertEquals("Search records to link", vm().uiState.value.placeholder)
    }

    @Test
    fun source_isShownByName() = runTest {
        seed()
        val vm = vm()
        vm.source.test {
            assertEquals(LinkSource(RecordType.Note, "Source note"), awaitUntil { it != null })
        }
    }

    @Test
    fun alreadyLinked_isMarked_neverBestMatch_andCanNotBeSelected() = runTest {
        seed()
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
        val vm = vm()
        vm.selection.test { awaitUntil { org.id in it.linkedIds }; cancelAndIgnoreRemainingEvents() }
        vm.uiState.test {
            vm.updateCurrentQuery("acme")
            val state = awaitUntil { it.results.total == 1 }
            assertEquals(null, state.results.bestMatch)
            assertEquals(listOf(org.id), state.results.groups.single().results.map { it.recordId })
            vm.toggleSelectedSearchResult(state.results.groups.single().results.single())
            assertTrue(vm.selected.value.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Select in one search, erase the query, select in a second search of another type, Link. */
    @Test
    fun selectionSticksAcrossSearches_andLinkLinksAll() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "apple") { it.size == 1 }.single())
            search(vm, "") { it.isEmpty() }
            vm.toggleSelectedSearchResult(search(vm, "google") { it.size == 1 }.single())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(apple.id, pixel.id), vm.selected.value.map { it.recordId })

        vm.linkSelectedResults()

        assertEquals(setOf(apple.id, pixel.id), linkedIds())
        assertTrue(vm.selected.value.isEmpty())
    }

    @Test
    fun selection_keptThroughEmptyAndShortQueries_clearAndChips() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "apple") { it.size == 1 }.single())
            search(vm, "ap") { it.isEmpty() }
            assertEquals(listOf(apple.id), vm.selected.value.map { it.recordId })
            vm.updateCurrentQuery("") // the clear button
            vm.toggleChip(RecordType.Item)
            vm.toggleChip(null)
            search(vm, "laptop") { it.size == 1 }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(apple.id), vm.selected.value.map { it.recordId })
        assertEquals(setOf(apple.id), vm.selection.value.selectedIds)
    }

    @Test
    fun selectedRecord_showsCheckedWhenItAppearsAgain() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "apple") { it.size == 1 }.single())
            search(vm, "google") { it.size == 1 }
            val again = search(vm, "computers") { it.size == 1 }.single()
            assertTrue(again.recordId in vm.selection.value.selectedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selection_keepsTheOrderPicked_andUncheckingRemoves() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "google") { it.size == 1 }.single())
            vm.toggleSelectedSearchResult(search(vm, "apple") { it.size == 1 }.single())
            vm.toggleSelectedSearchResult(search(vm, "jane") { it.size == 1 }.single())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(pixel.id, apple.id, contact.id), vm.selected.value.map { it.recordId })

        vm.unselect(apple.id) // unchecked in the selected records sheet
        assertEquals(listOf(pixel.id, contact.id), vm.selected.value.map { it.recordId })
    }

    @Test
    fun linkLinksEverySelectedRecord_visibleOrNot() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "acme") { it.size == 1 }.single())
            vm.toggleSelectedSearchResult(search(vm, "laptop") { it.size == 1 }.single())
            search(vm, "zzz") { it.isEmpty() }
            cancelAndIgnoreRemainingEvents()
        }
        vm.linkSelectedResults()

        assertEquals(listOf(org), c.linkedRecordRepository.getLinkedOrganizationsForRecordId(note.id).first())
        assertEquals(listOf(item), c.linkedRecordRepository.getLinkedItemsForRecordId(note.id).first())
    }

    @Test
    fun linkingAnAlreadyLinkedRecord_doesNotCreateDuplicateLink() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "acme") { it.size == 1 }.single())
            cancelAndIgnoreRemainingEvents()
        }
        // Linked elsewhere while it was selected
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
        vm.linkSelectedResults()

        val counts = c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(note.id).first()
        assertEquals(1, counts.single { it.recordTypeId == RecordType.Organization.recordTypeId }.count)
    }

    /** The user taps "Link" twice. */
    @Test
    fun tappingLinkTwice_createsOneLink_andClearsSelection() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "acme") { it.size == 1 }.single())
            cancelAndIgnoreRemainingEvents()
        }
        // Two taps racing each other, then a late third tap
        val first = async { vm.linkSelectedResults() }
        val second = async { vm.linkSelectedResults() }
        awaitAll(first, second)
        vm.linkSelectedResults()

        assertTrue(vm.selected.value.isEmpty())
        val counts = c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(note.id).first()
        assertEquals(1, counts.single { it.recordTypeId == RecordType.Organization.recordTypeId }.count)
    }

    @Test
    fun selectedForNewRecord_givesTheSelectionWithTypes() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            vm.toggleSelectedSearchResult(search(vm, "apple") { it.size == 1 }.single())
            vm.toggleSelectedSearchResult(search(vm, "google") { it.size == 1 }.single())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            listOf(apple.id to RecordType.Organization, pixel.id to RecordType.Item),
            vm.selectedForNewRecord(),
        )
    }

    @Test
    fun recentlyChanged_leavesOutTheSourceAndLinkedRecords() = runTest {
        seed()
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
        val vm = vm()
        vm.selection.test { awaitUntil { org.id in it.linkedIds }; cancelAndIgnoreRemainingEvents() }
        vm.uiState.test {
            val recent = awaitUntil { it.recent.size == 5 }.recent.map { it.recordId }
            assertTrue(org.id !in recent)
            assertTrue(recent.none { it == note.id || it == otherNote.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun contactWithoutFirstName_appearsInResults() = runTest {
        seed()
        val lastNameOnly = Contact(nameLast = "Solo", nameFirst = null).stamped()
        c.contactRepository.addContact(lastNameOnly)
        val vm = vm()
        vm.searchResults.test {
            assertTrue(search(vm, "solo") { it.isNotEmpty() }.any { it.recordId == lastNameOnly.id })
        }
    }

    // ---------- create from search ----------

    private suspend fun LinkRecordsViewModel.settledOffer(query: String): CreateFromSearchOffer {
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
    fun createOffer_noResultsAfterTheFilters() = runTest {
        seed()
        val vm = vm()
        // "Other note" is found by search, but it's the source's type, so it's left out
        assertEquals(CreateFromSearchOffer.NoResults, vm.settledOffer("other"))
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("  "))
    }

    @Test
    fun createOffer_afterResults_unlessOneMatchesExactly() = runTest {
        seed()
        val vm = vm()
        assertEquals(CreateFromSearchOffer.AfterResults, vm.settledOffer("acm"))
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("acme"))
        assertEquals(CreateFromSearchOffer.None, vm.settledOffer("Jane Doe"))
    }

    @Test
    fun createOffer_typesLeaveOutTheSourceType() {
        assertEquals(
            listOf(RecordType.Contact, RecordType.Organization, RecordType.Category, RecordType.Item),
            vm().createFromSearchTypes,
        )
        assertEquals(
            listOf(RecordType.Note, RecordType.Contact, RecordType.Organization, RecordType.Category),
            vm(item.id, RecordType.Item).createFromSearchTypes,
        )
    }
}
