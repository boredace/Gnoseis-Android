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

package org.gnoseis.ui.organization

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.OrganizationEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OrganizationViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun editVm(id: String?, mode: OrganizationEditPageMode, linkFromType: RecordType? = null, linkFromId: String? = null) =
        OrganizationEditViewModel(
            SavedStateHandle(mapOf("organizationId" to id, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId)),
            c.organizationRepository,
            c.linkedRecordRepository,
        )

    @Test
    fun list_reflectsRepository() = runTest {
        val vm = OrganizationListViewModel(c.organizationRepository)
        vm.organizationListPageState.test {
            assertEquals(emptyList<Organization>(), awaitItem().organizations)
            val org = Organization(organizationName = "Acme")
            c.organizationRepository.addOrganization(org)
            assertEquals(listOf(org), awaitItem().organizations)
        }
    }

    @Test
    fun details_loadsLinkedNotesAndContacts_andDeleteCleansUp() = runTest {
        val org = Organization(organizationName = "Acme")
        val contact = Contact(nameLast = "Doe")
        val note = Note(title = "meeting")
        c.organizationRepository.addOrganization(org)
        c.contactRepository.addContact(contact)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, org.id, RecordType.Organization))
        val vm = OrganizationDetailsViewModel(SavedStateHandle(mapOf("organizationId" to org.id)), c.organizationRepository, c.linkedRecordRepository)

        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedContacts.isEmpty() || linked.linkedNotes.isEmpty()) linked = awaitItem()
            assertEquals(listOf(contact), linked.linkedContacts)
            assertEquals(listOf(note), linked.linkedNotes)
        }
        vm.organizationDetailsPageState.test {
            var state = awaitItem()
            while (state.organization != org) state = awaitItem()
            vm.deleteOrganization()
            while (state.organization != null) state = awaitItem()
        }
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
    }

    @Test
    fun edit_validationIsDebounced_andBlankIsInvalid() = runTest {
        val vm = editVm(null, OrganizationEditPageMode.NEW)
        vm.onEvent(OrganizationEditPageEvent.OrganizationNameChanged("Acme"))
        assertFalse(vm.editState.value.isValid)
        advanceTimeBy(501); runCurrent()
        assertTrue(vm.editState.value.isValid)

        vm.onEvent(OrganizationEditPageEvent.OrganizationNameChanged(""))
        advanceTimeBy(501); runCurrent()
        assertFalse(vm.editState.value.isValid)
    }

    @Test
    fun edit_save_whenInvalid_doesNothing() = runTest {
        assertNull(editVm(null, OrganizationEditPageMode.NEW).onSave())
        assertTrue(c.organizationRepository.getOrganizationsStream().first().isEmpty())
    }

    @Test
    fun edit_newLinkMode_savesAndLinks() = runTest {
        val contact = Contact(nameLast = "Doe")
        c.contactRepository.addContact(contact)
        val vm = editVm(null, OrganizationEditPageMode.NEWLINK, RecordType.Contact, contact.id)
        vm.onEvent(OrganizationEditPageEvent.OrganizationNameChanged("Acme"))
        vm.onEvent(OrganizationEditPageEvent.OrganizationDescriptionChanged("desc"))
        vm.onEvent(OrganizationEditPageEvent.Save)
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        val saved = c.organizationRepository.getOrganization(id)
        assertEquals("Acme", saved.organizationName)
        assertEquals("desc", saved.comments)
        assertEquals(listOf(saved), c.linkedRecordRepository.getLinkedOrganizationListByContactId(contact.id).first())
    }

    @Test
    fun edit_editMode_loadsAndUpdates() = runTest {
        val org = Organization(organizationName = "Old", comments = "c")
        c.organizationRepository.addOrganization(org)
        val vm = editVm(org.id, OrganizationEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Old", vm.editState.value.editOrganizationName)

        vm.onEvent(OrganizationEditPageEvent.OrganizationNameChanged("New"))
        advanceTimeBy(501); runCurrent()
        assertNull(vm.onSave())

        assertEquals(org.copy(organizationName = "New"), c.organizationRepository.getOrganization(org.id))
    }
}
