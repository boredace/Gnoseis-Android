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

package org.gnoseis.ui.contact

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.constants.TITLE_LENGTH_CONTACT
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.ContactEditPageMode
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

@RunWith(AndroidJUnit4::class)
class ContactViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun editVm(
        contactId: String?,
        mode: ContactEditPageMode,
        linkFromType: RecordType? = null,
        linkFromId: String? = null,
    ) = ContactEditViewModel(
        SavedStateHandle(
            mapOf("contactId" to contactId, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId)
        ),
        c.contactRepository,
        c.linkedRecordRepository,
    )

    // ---------- List ----------

    @Test
    fun list_reflectsRepository() = runTest {
        val vm = ContactListViewModel(c.contactRepository)
        vm.contactListPageState.test {
            assertEquals(emptyList<Contact>(), awaitItem().contacts)
            val contact = Contact(nameLast = "Doe")
            c.contactRepository.addContact(contact)
            assertEquals(listOf(contact), awaitItem().contacts)
        }
    }

    // ---------- Details ----------

    @Test
    fun details_loadsContactAndLinkedRecords_andDeleteCleansUp() = runTest {
        val contact = Contact(nameLast = "Doe")
        val org = Organization(organizationName = "Acme")
        val item = Item(itemName = "Laptop")
        c.contactRepository.addContact(contact)
        c.organizationRepository.addOrganization(org)
        c.itemRepository.addItem(item)
        c.linkedRecordRepository.addLinkedRecord(link(org.id, RecordType.Organization, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(contact.id, RecordType.Contact, item.id, RecordType.Item))
        val vm = ContactDetailsViewModel(SavedStateHandle(mapOf("contactId" to contact.id)), c.contactRepository, c.linkedRecordRepository)

        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedOrganizations.isEmpty() || linked.linkedItems.isEmpty()) linked = awaitItem()
            assertEquals(listOf(org), linked.linkedOrganizations)
            assertEquals(listOf(item), linked.linkedItems)
        }
        vm.contactDetailsPageState.test {
            var state = awaitItem()
            while (state.contact != contact) state = awaitItem()
            assertEquals(2, state.linkedRecordTabs.size)
            vm.deleteContact()
            while (state.contact != null) state = awaitItem()
        }
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(org.id).first().isEmpty())
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(item.id).first().isEmpty())
    }

    // ---------- Edit: validation ----------

    @Test
    fun edit_lastNameValidation_lengthBounds() {
        val vm = editVm(null, ContactEditPageMode.NEW)
        fun validFor(name: String): Boolean {
            vm.onEvent(ContactEditPageEvent.NameLastChanged(name))
            return vm.editState.value.isValid
        }

        assertFalse(validFor(""))
        assertTrue(validFor("D"))
        assertTrue(validFor("x".repeat(TITLE_LENGTH_CONTACT)))
        assertFalse(validFor("x".repeat(TITLE_LENGTH_CONTACT + 1)))
        // Whitespace-only last name is accepted (documented)
        assertTrue(validFor("   "))
    }

    @Test
    fun edit_otherFieldsDoNotAffectValidity() {
        val vm = editVm(null, ContactEditPageMode.NEW)
        vm.onEvent(ContactEditPageEvent.NameFirstChanged("Jane"))
        vm.onEvent(ContactEditPageEvent.CompanyChanged("Acme"))
        assertFalse(vm.editState.value.isValid)
    }

    @Test
    fun edit_phoneMainIsValidated_butDoesNotBlockSave() = runTest {
        val vm = editVm(null, ContactEditPageMode.NEW)
        vm.onEvent(ContactEditPageEvent.NameLastChanged("Doe"))
        vm.onEvent(ContactEditPageEvent.PhoneMainChanged("not a phone"))

        assertEquals(false, vm.editState.value.phoneMainValidationResult?.successful)
        assertTrue(vm.editState.value.isValid)
        val id = vm.onSave()!!
        assertEquals("not a phone", c.contactRepository.getContact(id).phoneMain)
    }

    @Test
    fun edit_clearingOptionalPhoneMain_reportsBlankError() {
        val vm = editVm(null, ContactEditPageMode.NEW)
        vm.onEvent(ContactEditPageEvent.PhoneMainChanged("5551234"))
        assertEquals(true, vm.editState.value.phoneMainValidationResult?.successful)

        vm.onEvent(ContactEditPageEvent.PhoneMainChanged(""))

        // Documented: an optional field shows "can't be blank" once cleared.
        assertEquals("The phone number can't be blank", vm.editState.value.phoneMainValidationResult?.errorMessage)
    }

    @Test
    fun edit_emailIsNotValidated() {
        val vm = editVm(null, ContactEditPageMode.NEW)
        vm.onEvent(ContactEditPageEvent.EmailMainChanged("not an email"))
        assertEquals("not an email", vm.editState.value.emailMain)
    }

    // ---------- Edit: save ----------

    @Test
    fun edit_save_whenInvalid_doesNothing() = runTest {
        assertNull(editVm(null, ContactEditPageMode.NEW).onSave())
        assertTrue(c.contactRepository.getContactsStream().first().isEmpty())
    }

    @Test
    fun edit_newMode_saveStoresEveryField() = runTest {
        val vm = editVm(null, ContactEditPageMode.NEW)
        listOf(
            ContactEditPageEvent.NameLastChanged("Doe"),
            ContactEditPageEvent.NameFirstChanged("Jane"),
            ContactEditPageEvent.JobTitleChanged("CTO"),
            ContactEditPageEvent.CompanyChanged("Acme"),
            ContactEditPageEvent.PhoneMainChanged("1111"),
            ContactEditPageEvent.PhoneMobileChanged("2222"),
            ContactEditPageEvent.PhoneHomeChanged("3333"),
            ContactEditPageEvent.PhoneWorkChanged("4444"),
            ContactEditPageEvent.EmailMainChanged("a@x.io"),
            ContactEditPageEvent.EmailMobileChanged("b@x.io"),
            ContactEditPageEvent.EmailHomeChanged("c@x.io"),
            ContactEditPageEvent.EmailWorkChanged("d@x.io"),
            ContactEditPageEvent.CommentsChanged("hi"),
            ContactEditPageEvent.Save,
        ).forEach(vm::onEvent)

        val id = vm.onSave()!!

        assertEquals(
            Contact(
                id = id, ownerDbId = "db1", nameLast = "Doe", nameFirst = "Jane", jobTitle = "CTO", company = "Acme",
                phoneMain = "1111", phoneMobile = "2222", phoneHome = "3333", phoneWork = "4444",
                emailMain = "a@x.io", emailMobile = "b@x.io", emailHome = "c@x.io", emailWork = "d@x.io",
                comments = "hi",
            ),
            c.contactRepository.getContact(id),
        )
    }

    @Test
    fun edit_newLinkMode_linksToSource() = runTest {
        val org = Organization(organizationName = "Acme")
        c.organizationRepository.addOrganization(org)
        val vm = editVm(null, ContactEditPageMode.NEWLINK, RecordType.Organization, org.id)
        vm.onEvent(ContactEditPageEvent.NameLastChanged("Doe"))

        val id = vm.onSave()!!

        assertEquals(listOf(id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(org.id).first())
    }

    @Test
    fun edit_editMode_loadsAndUpdates() = runTest {
        val contact = Contact(nameLast = "Doe", nameFirst = "Jane", emailMain = "a@x.io")
        c.contactRepository.addContact(contact)
        val vm = editVm(contact.id, ContactEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Doe", vm.editState.value.nameLast)
        assertEquals("Jane", vm.editState.value.nameFirst)
        assertEquals("a@x.io", vm.editState.value.emailMain)
        assertTrue(vm.editState.value.isValid)

        vm.onEvent(ContactEditPageEvent.NameFirstChanged("John"))
        assertNull(vm.onSave())

        assertEquals(contact.copy(nameFirst = "John"), c.contactRepository.getContact(contact.id))
    }
}
