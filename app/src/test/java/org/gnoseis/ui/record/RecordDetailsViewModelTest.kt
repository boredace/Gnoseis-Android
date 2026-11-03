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

package org.gnoseis.ui.record

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.testutil.stamped
import org.gnoseis.testutil.waitFor
import org.gnoseis.ui.contact.ContactDetailsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #11: unlinking from a Details page, through the view model every Details page shares. */
@RunWith(AndroidJUnit4::class)
class RecordDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private val contact = Contact(nameLast = "Doe", nameFirst = "Jane").stamped()
    private val org = Organization(organizationName = "Acme").stamped()
    private val note = Note(title = "Call").stamped()
    private val noteLink = link(note.id, RecordType.Note, contact.id, RecordType.Contact)

    private suspend fun addLinked() {
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, org.id, RecordType.Organization))
        c.linkedRecordRepository.addLinkedRecord(noteLink)
    }

    private fun detailsVm() = mainDispatcherRule.viewModel {
        ContactDetailsViewModel(SavedStateHandle(mapOf("contactId" to contact.id)), c.contactRepository, c.linkedRecordRepository, c.settingsRepository)
    }

    @Test
    fun unlink_removesTheRecordFromItsList_andItsTab_andUndoLinksItAgain() = runTest {
        addLinked()
        val vm = detailsVm()
        var removed: LinkedRecord? = null

        vm.contactDetailsPageState.test {
            var state = awaitItem()
            while (state.linkedRecordTabs.size < 2) state = awaitItem()

            vm.unlink(note.id) { removed = it }
            while (state.linkedRecordTabs.size > 1) state = awaitItem()
            assertEquals(listOf(RecordType.Organization.recordTypeId), state.linkedRecordTabs.map { it.recordTypeId })
            cancelAndIgnoreRemainingEvents()
        }
        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedNotes.isNotEmpty() || linked.linkedOrganizations.isEmpty()) linked = awaitItem()
            assertEquals(listOf(org), linked.linkedOrganizations)
            cancelAndIgnoreRemainingEvents()
        }
        waitFor { removed != null }
        assertNotNull(c.settingsRepository.getLastDeletionAtStream().first())

        vm.undoUnlink(removed!!)

        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedNotes.isEmpty()) linked = awaitItem()
            assertEquals(listOf(note), linked.linkedNotes)
            cancelAndIgnoreRemainingEvents()
        }
        val restored = c.linkedRecordRepository.getLinkBetween(contact.id, note.id)!!
        assertEquals(noteLink.record1Id, restored.record1Id) // same way round as before
        assertEquals(noteLink.record2Id, restored.record2Id)
        assertNotEquals(noteLink.id, restored.id)
    }

    @Test
    fun linkedRecords_nameOf_isTheNameItsRowShows() {
        val linked = LinkedRecords(linkedContacts = listOf(contact), linkedNotes = listOf(note, Note(id = "untitled", textContents = "text only")))

        assertEquals("Doe, Jane", linked.nameOf(contact.id))
        assertEquals("Call", linked.nameOf(note.id))
        assertEquals("text only", linked.nameOf("untitled"))
        assertEquals(null, linked.nameOf(org.id))
    }
}
