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
import kotlinx.coroutines.flow.first
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
import org.gnoseis.testutil.knownBug
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun tearDown() = c.close()

    private val note = Note(title = "Source note")
    private val otherNote = Note(title = "Other note")
    private val contact = Contact(nameLast = "Doe", nameFirst = "Jane")
    private val org = Organization(organizationName = "Acme")
    private val item = Item(itemName = "Laptop")
    private val category = Category(categoryName = "Work")

    private suspend fun seed() {
        c.noteRepository.addNote(note)
        c.noteRepository.addNote(otherNote)
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.itemRepository.addItem(item)
        c.categoryRepository.addCategory(category)
    }

    private fun vm(sourceId: String = note.id, sourceType: RecordType = RecordType.Note) = LinkRecordsViewModel(
        SavedStateHandle(mapOf("sourceRecordId" to sourceId, "sourceRecordTypeId" to sourceType.recordTypeId)),
        c.linkedRecordRepository,
        c.searchRepository,
    )

    private suspend fun ReceiveTurbine<List<SearchResult>>.awaitResults(predicate: (List<SearchResult>) -> Boolean): List<SearchResult> {
        var results = awaitItem()
        while (!predicate(results)) results = awaitItem()
        return results
    }

    @Test
    fun results_excludeSourceRecord_andRecordsOfTheSameType() = runTest {
        seed()
        vm().searchResults.test {
            val results = awaitResults { it.isNotEmpty() }
            assertEquals(setOf(contact.id, org.id, item.id, category.id), results.map { it.recordId }.toSet())
        }
    }

    @Test
    fun results_areFilteredByQuery_caseInsensitively() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            awaitResults { it.isNotEmpty() }
            vm.updateCurrentQuery("ACM")
            assertEquals(listOf(org.id), awaitResults { it.size == 1 }.map { it.recordId })
        }
    }

    @Test
    fun alreadyLinkedResults_areFlagged() = runTest {
        seed()
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
        val vm = vm()
        vm.searchResults.test {
            awaitResults { it.isNotEmpty() }
            // A query change re-evaluates results against the (by now loaded) linked ids
            vm.updateCurrentQuery("Acme")
            assertEquals(true, awaitResults { it.size == 1 }.single().isLinked)
            vm.updateCurrentQuery("Work")
            // Unlinked results carry null (Room ignores the Kotlin default), which the UI treats as false
            assertNotEquals(true, awaitResults { it.singleOrNull()?.recordId == category.id }.single().isLinked)
        }
    }

    @Test
    fun selection_countsAndIsClearedByNewQuery() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            val results = awaitResults { it.isNotEmpty() }
            vm.toggleSelectedSearchResult(results.first { it.recordId == org.id })
            vm.toggleSelectedSearchResult(results.first { it.recordId == item.id })
            assertEquals(2, vm.selectedCount.intValue)

            vm.toggleSelectedSearchResult(results.first { it.recordId == item.id })
            assertEquals(1, vm.selectedCount.intValue)

            vm.updateCurrentQuery("x")
            assertEquals(0, vm.selectedCount.intValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun linkSelectedResults_createsLinksFromSource() = runTest {
        seed()
        val vm = vm()
        vm.searchResults.test {
            val results = awaitResults { it.isNotEmpty() }
            vm.toggleSelectedSearchResult(results.first { it.recordId == org.id })
            vm.toggleSelectedSearchResult(results.first { it.recordId == item.id })
            vm.linkSelectedResults()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(org), c.linkedRecordRepository.getLinkedOrganizationListByNoteId(note.id).first())
        assertEquals(listOf(item), c.linkedRecordRepository.getLinkedItemListByNoteId(note.id).first())
        assertEquals(
            setOf(RecordType.Organization.recordTypeId, RecordType.Item.recordTypeId),
            c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(note.id).first().map { it.recordTypeId }.toSet(),
        )
    }

    @Test
    fun linkingAnAlreadyLinkedRecord_doesNotCreateDuplicateLink() = runTest {
        knownBug(
            "#13 - linking a record that is already linked creates a duplicate link, and the tab badge then shows 2. LinkRecordsViewModel.linkSelectedResults() does not skip results with isLinked == true. Fix: skip them.",
        ) {
            seed()
            c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
            val vm = vm()
            vm.searchResults.test {
                val results = awaitResults { it.isNotEmpty() }
                vm.toggleSelectedSearchResult(results.first { it.recordId == org.id })
                vm.linkSelectedResults()
                cancelAndIgnoreRemainingEvents()
            }

            val counts = c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(note.id).first()
            assertEquals(1, counts.single { it.recordTypeId == RecordType.Organization.recordTypeId }.count)
        }
    }

    @Test
    fun contactWithoutFirstName_appearsInResults() = runTest {
        knownBug(
            "Bug 6 - the Link Records page crashes when any contact has no first name (the SearchDao title is NULL, see SearchDaoTest). Fix: use COALESCE(', ' || NameFirst, '') in SearchDao.",
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
