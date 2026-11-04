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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.constants.TITLE_LENGTH_CATEGORY
import org.gnoseis.data.constants.TITLE_LENGTH_CONTACT
import org.gnoseis.data.constants.TITLE_LENGTH_NOTE
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.stamped
import org.gnoseis.testutil.waitFor
import org.gnoseis.ui.category.CategoryEditViewModel
import org.gnoseis.ui.contact.ContactEditViewModel
import org.gnoseis.ui.item.ItemEditViewModel
import org.gnoseis.ui.note.NoteEditViewModel
import org.gnoseis.ui.organization.OrganizationEditViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #10: a new record's name, prefilled from the search it's created from. */
@RunWith(AndroidJUnit4::class)
class RecordPrefillTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private fun route(idKey: String, id: String?, mode: Any, prefill: String?, linkFromType: RecordType? = null, linkFromId: String? = null) =
        SavedStateHandle(mapOf(idKey to id, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId, "prefillName" to prefill))

    private fun noteVm(mode: RecordEditPageMode, prefill: String?, id: String? = null) = mainDispatcherRule.viewModel {
        NoteEditViewModel(route("noteId", id, mode, prefill), c.noteRepository, c.linkedRecordRepository, c.getLinkSourceUseCase())
    }
    private fun contactVm(mode: RecordEditPageMode, prefill: String?) = mainDispatcherRule.viewModel {
        ContactEditViewModel(route("contactId", null, mode, prefill), c.contactRepository, c.linkedRecordRepository, c.getLinkSourceUseCase())
    }
    private fun orgVm(mode: RecordEditPageMode, prefill: String?) = mainDispatcherRule.viewModel {
        OrganizationEditViewModel(route("organizationId", null, mode, prefill), c.organizationRepository, c.linkedRecordRepository, c.getLinkSourceUseCase())
    }
    private fun itemVm(mode: RecordEditPageMode, prefill: String?) = mainDispatcherRule.viewModel {
        ItemEditViewModel(route("itemId", null, mode, prefill), c.itemRepository, c.linkedRecordRepository, c.getLinkSourceUseCase())
    }
    private fun categoryVm(mode: RecordEditPageMode, prefill: String?, id: String? = null, linkFromType: RecordType? = null, linkFromId: String? = null) =
        mainDispatcherRule.viewModel {
            CategoryEditViewModel(route("categoryId", id, mode, prefill, linkFromType, linkFromId), c.categoryRepository, c.linkedRecordRepository, c.getLinkSourceUseCase())
        }

    // ---------- Mapping ----------

    @Test
    fun prefillName_trimsAndCutsToTheLimit() {
        assertEquals("Acme", prefillName("  Acme \n", TITLE_LENGTH_CATEGORY))
        assertEquals("a".repeat(TITLE_LENGTH_NOTE), prefillName("a".repeat(TITLE_LENGTH_NOTE + 5), TITLE_LENGTH_NOTE))
        assertNull(prefillName("   ", TITLE_LENGTH_CATEGORY))
        assertNull(prefillName(null, TITLE_LENGTH_CATEGORY))
    }

    @Test
    fun prefillContactName_oneWordIsTheLastName_moreWordsSplitOffTheLast() {
        assertEquals(ContactNamePrefill(nameFirst = null, nameLast = "Doe"), prefillContactName(" Doe "))
        assertEquals(ContactNamePrefill(nameFirst = "Jane", nameLast = "Doe"), prefillContactName("Jane Doe"))
        assertEquals(ContactNamePrefill(nameFirst = "Mary Jane", nameLast = "Watson"), prefillContactName("Mary   Jane Watson"))
        assertEquals("b".repeat(TITLE_LENGTH_CONTACT), prefillContactName("a " + "b".repeat(TITLE_LENGTH_CONTACT + 3))!!.nameLast)
        assertNull(prefillContactName(" "))
        assertNull(prefillContactName(null))
    }

    // ---------- Edit pages ----------

    @Test
    fun newRecords_startWithTheNameFieldFilled_andValid() {
        val note = noteVm(RecordEditPageMode.NEW, " Call Bob ")
        assertEquals("Call Bob", note.editState.value.editTitle)
        assertTrue(note.editState.value.isValid)
        assertTrue(note.pageState.value.namePrefilled)

        val contact = contactVm(RecordEditPageMode.NEWLINK, "Jane Doe")
        assertEquals("Jane", contact.editState.value.nameFirst)
        assertEquals("Doe", contact.editState.value.nameLast)
        assertTrue(contact.editState.value.isValid)

        val org = orgVm(RecordEditPageMode.NEW, "Acme")
        assertEquals("Acme", org.editState.value.editOrganizationName)
        assertTrue(org.editState.value.isValid)

        val item = itemVm(RecordEditPageMode.NEWLINK, "Laptop")
        assertEquals("Laptop", item.editState.value.editItemName)
        assertTrue(item.editState.value.isValid)

        val category = categoryVm(RecordEditPageMode.NEW, "x".repeat(TITLE_LENGTH_CATEGORY + 1))
        assertEquals("x".repeat(TITLE_LENGTH_CATEGORY), category.editState.value.editCategoryName)
        assertTrue(category.editState.value.isValid)
    }

    @Test
    fun withoutAPrefill_newRecordsStartEmpty() {
        val category = categoryVm(RecordEditPageMode.NEW, null)
        assertNull(category.editState.value.editCategoryName)
        assertFalse(category.editState.value.isValid)
        assertFalse(category.pageState.value.namePrefilled)
    }

    @Test
    fun editMode_ignoresThePrefill() = runTest {
        val note = Note(title = "Old").stamped()
        c.noteRepository.addNote(note)
        val vm = noteVm(RecordEditPageMode.EDIT, "New", note.id)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Old", vm.editState.value.editTitle)
        assertFalse(vm.pageState.value.namePrefilled)
    }

    @Test
    fun newLinkSave_withAPrefilledName_createsTheRecordAndTheLink() = runTest {
        val note = Note(title = "n").stamped()
        c.noteRepository.addNote(note)
        val vm = categoryVm(RecordEditPageMode.NEWLINK, "Work", linkFromType = RecordType.Note, linkFromId = note.id)

        val id = vm.onSave()!!

        assertEquals("Work", c.categoryRepository.getCategory(id).categoryName)
        assertEquals(listOf(id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(note.id).first())
    }
}
