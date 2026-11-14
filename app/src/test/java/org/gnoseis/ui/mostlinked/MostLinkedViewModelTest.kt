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

package org.gnoseis.ui.mostlinked

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MostLinkedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()
    private val db get() = c.database

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private fun vm(routeType: RecordType? = null) = mainDispatcherRule.viewModel {
        MostLinkedViewModel(
            SavedStateHandle(routeType?.let { mapOf("recordTypeId" to it.recordTypeId) } ?: emptyMap()),
            c.homeRepository,
        )
    }

    /** Waits until the page state matches [condition] and returns it. */
    private suspend fun MostLinkedViewModel.awaitState(condition: (MostLinkedPageState) -> Boolean): MostLinkedPageState {
        var result: MostLinkedPageState? = null
        pageState.test {
            var state = awaitItem()
            while (!(state.isLoaded && condition(state))) state = awaitItem()
            result = state
            cancelAndIgnoreRemainingEvents()
        }
        return result!!
    }

    private suspend fun note(id: String) = db.noteDao().addNote(Note(id = id))

    private suspend fun addLink(id1: String, type1: RecordType, id2: String, type2: RecordType) =
        db.linkedRecordDao().addLinkedRecord(link(id1, type1, id2, type2))

    /** A contact with two links and an organization with one. */
    private suspend fun contactAndOrganization() {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))
        db.organizationDao().addOrganization(Organization(id = "o", organizationName = "Acme"))
        note("n1"); note("n2")
        addLink("c", RecordType.Contact, "n1", RecordType.Note)
        addLink("c", RecordType.Contact, "n2", RecordType.Note)
        addLink("o", RecordType.Organization, "n1", RecordType.Note)
    }

    /** #52: starts on the route's type and lists only that type. */
    @Test
    fun startsOnTheRoutesType() = runTest {
        contactAndOrganization()

        val state = vm(RecordType.Organization).awaitState { it.records.isNotEmpty() }

        assertEquals(RecordType.Organization.recordTypeId, state.typeId)
        assertEquals(listOf("o"), state.records.map { it.id })
    }

    /** #52: without a type in the route, lists every type, most linked first. */
    @Test
    fun withoutRouteType_listsAllTypes() = runTest {
        contactAndOrganization()

        val state = vm().awaitState { it.records.size == 2 }

        assertEquals(null, state.typeId)
        assertEquals(listOf("c", "o"), state.records.map { it.id })
        assertEquals(listOf(2, 1), state.records.map { it.linkCount })
    }

    /** #52: changing the type reloads the list, and back to all types. */
    @Test
    fun changingTheType_reloadsTheList() = runTest {
        contactAndOrganization()
        val vm = vm()
        vm.awaitState { it.records.size == 2 }

        vm.setTypeId(RecordType.Contact.recordTypeId)
        val contacts = vm.awaitState { it.typeId == RecordType.Contact.recordTypeId }
        assertEquals(listOf("c"), contacts.records.map { it.id })

        vm.setTypeId(RecordType.Organization.recordTypeId)
        val orgs = vm.awaitState { it.typeId == RecordType.Organization.recordTypeId }
        assertEquals(listOf("o"), orgs.records.map { it.id })

        vm.setTypeId(null)
        assertEquals(listOf("c", "o"), vm.awaitState { it.typeId == null }.records.map { it.id })
    }

    /** #52: the list and the type picker update live when a link is added or removed. */
    @Test
    fun updatesLive_afterLinkAddedAndRemoved() = runTest {
        contactAndOrganization()
        val vm = vm()
        vm.awaitState { it.records.size == 2 }

        // The organization gains two links and moves to the top.
        note("n3")
        addLink("o", RecordType.Organization, "n2", RecordType.Note)
        addLink("o", RecordType.Organization, "n3", RecordType.Note)
        assertEquals(listOf("o", "c"), vm.awaitState { it.records.firstOrNull()?.id == "o" }.records.map { it.id })

        // The contact loses both its links and drops off the list and the picker.
        db.linkedRecordDao().deleteLinkBetween("c", "n1")
        db.linkedRecordDao().deleteLinkBetween("c", "n2")
        val state = vm.awaitState { it.records.size == 1 }
        assertEquals(listOf("o"), state.records.map { it.id })
        assertEquals(listOf(RecordType.Organization.recordTypeId), state.types.map { it.recordTypeId })
    }

    /** #52: nothing linked at all. */
    @Test
    fun nothingLinked_isNothingLinked() = runTest {
        db.contactDao().addContact(Contact(id = "c", nameLast = "Doe"))

        val state = vm().awaitState { true }

        assertEquals(emptyList<Any>(), state.records)
        assertEquals(emptyList<Any>(), state.types)
        assertTrue(state.isNothingLinked)
        assertFalse(state.isPickedTypeEmpty)
    }

    /** #52: the picked type has no linked records, but other types do. */
    @Test
    fun pickedTypeWithoutLinks_isPickedTypeEmpty() = runTest {
        contactAndOrganization()

        val state = vm(RecordType.Item).awaitState { it.types.isNotEmpty() }

        assertEquals(emptyList<Any>(), state.records)
        assertTrue(state.isPickedTypeEmpty)
        assertFalse(state.isNothingLinked)
    }

    /** #52: the picked type's last link is removed while the page is open. */
    @Test
    fun pickedTypeLosesItsLastLink_becomesPickedTypeEmpty() = runTest {
        contactAndOrganization()
        val vm = vm(RecordType.Organization)
        vm.awaitState { it.records.isNotEmpty() }

        db.linkedRecordDao().deleteLinkBetween("o", "n1")

        val state = vm.awaitState { it.records.isEmpty() }
        assertEquals(RecordType.Organization.recordTypeId, state.typeId)
        assertTrue(state.isPickedTypeEmpty)
    }
}
