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

package org.gnoseis.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.CategoryEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.testutil.MainDispatcherRule
import org.gnoseis.testutil.TestAppContainer
import org.gnoseis.testutil.link
import org.gnoseis.testutil.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CategoryViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() {
        mainDispatcherRule.clearViewModels() // before the database closes (#25)
        c.close()
    }

    private fun editVm(
        categoryId: String?,
        mode: CategoryEditPageMode,
        linkFromType: RecordType? = null,
        linkFromId: String? = null,
    ) = mainDispatcherRule.viewModel {
        CategoryEditViewModel(
            SavedStateHandle(
                mapOf(
                    "categoryId" to categoryId,
                    "pageMode" to mode,
                    "linkFromType" to linkFromType,
                    "linkFromId" to linkFromId,
                )
            ),
            c.categoryRepository,
            c.linkedRecordRepository,
        )
    }

    // ---------- List ----------

    @Test
    fun list_initiallyEmpty_thenReflectsRepository() = runTest {
        val vm = mainDispatcherRule.viewModel { CategoryListViewModel(c.categoryRepository) }
        vm.categoryListPageState.test {
            assertEquals(emptyList<Category>(), awaitItem().categories)
            val cat = Category(categoryName = "Work")
            c.categoryRepository.addCategory(cat)
            assertEquals(listOf(cat), awaitItem().categories)
        }
    }

    // ---------- Details ----------

    @Test
    fun details_loadsCategoryLinkedRecordsAndTabs() = runTest {
        val cat = Category(categoryName = "Work")
        val contact = Contact(nameLast = "Doe")
        val note = Note(title = "n")
        c.categoryRepository.addCategory(cat)
        c.contactRepository.addContact(contact)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(link(cat.id, RecordType.Category, contact.id, RecordType.Contact))
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, cat.id, RecordType.Category))

        val vm = mainDispatcherRule.viewModel { CategoryDetailsViewModel(SavedStateHandle(mapOf("categoryId" to cat.id)), c.categoryRepository, c.linkedRecordRepository) }

        vm.categoryDetailsPageState.test {
            var state = awaitItem()
            while (state.category != cat) state = awaitItem()
            assertEquals(setOf(RecordType.Contact.recordTypeId, RecordType.Note.recordTypeId), state.linkedRecordTabs.map { it.recordTypeId }.toSet())
        }
        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedContacts.isEmpty() || linked.linkedNotes.isEmpty()) linked = awaitItem()
            assertEquals(listOf(contact), linked.linkedContacts)
            assertEquals(listOf(note), linked.linkedNotes)
            assertTrue(linked.linkedItems.isEmpty())
            assertTrue(linked.linkedOrganizations.isEmpty())
        }
    }

    @Test
    fun details_delete_removesCategoryAndLinks_andStateBecomesNull() = runTest {
        val cat = Category(categoryName = "Work")
        val contact = Contact(nameLast = "Doe")
        c.categoryRepository.addCategory(cat)
        c.contactRepository.addContact(contact)
        c.linkedRecordRepository.addLinkedRecord(link(cat.id, RecordType.Category, contact.id, RecordType.Contact))
        val vm = mainDispatcherRule.viewModel { CategoryDetailsViewModel(SavedStateHandle(mapOf("categoryId" to cat.id)), c.categoryRepository, c.linkedRecordRepository) }

        vm.categoryDetailsPageState.test {
            var state = awaitItem()
            while (state.category != cat) state = awaitItem()
            vm.deleteCategory()
            while (state.category != null) state = awaitItem()
        }
        assertTrue(c.categoryRepository.getCategoriesStream().first().isEmpty())
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(contact.id).first().isEmpty())
    }

    // ---------- Edit ----------

    @Test
    fun edit_newMode_initialState() {
        val vm = editVm(null, CategoryEditPageMode.NEW)

        assertTrue(vm.pageState.value.isNew)
        assertEquals(CategoryEditPageMode.NEW, vm.pageState.value.pageMode)
        assertFalse(vm.editState.value.isValid)
        assertNull(vm.editState.value.editCategoryName)
    }

    @Test
    fun edit_save_whenInvalid_doesNothing() = runTest {
        val vm = editVm(null, CategoryEditPageMode.NEW)

        assertNull(vm.onSave())
        assertTrue(c.categoryRepository.getCategoriesStream().first().isEmpty())
    }

    @Test
    fun edit_nameValidation_isDebounced500ms() = runTest {
        val vm = editVm(null, CategoryEditPageMode.NEW)

        vm.updateCategoryName("Work")
        assertEquals("Work", vm.editState.value.editCategoryName)
        assertFalse(vm.editState.value.isValid)

        advanceTimeBy(499); runCurrent()
        assertFalse(vm.editState.value.isValid)
        advanceTimeBy(2); runCurrent()
        assertTrue(vm.editState.value.isValid)

        vm.updateCategoryName("")
        advanceTimeBy(501); runCurrent()
        assertFalse(vm.editState.value.isValid)
    }

    @Test
    fun edit_descriptionChange_revalidatesName() = runTest {
        val vm = editVm(null, CategoryEditPageMode.NEW)
        vm.updateCategoryName("Work")
        vm.updateCategoryDescription("desc") // cancels the name debounce job
        advanceTimeBy(501); runCurrent()

        assertTrue(vm.editState.value.isValid)
        assertEquals("desc", vm.editState.value.editCategoryDescription)
    }

    @Test
    fun edit_newMode_saveInsertsCategory() = runTest {
        val vm = editVm(null, CategoryEditPageMode.NEW)
        vm.updateCategoryName("Work")
        vm.updateCategoryDescription("desc")
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()

        assertNotNull(id)
        val saved = c.categoryRepository.getCategory(id!!)
        assertEquals("Work", saved.categoryName)
        assertEquals("desc", saved.comments)
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(id).first().isEmpty())
    }

    @Test
    fun edit_newLinkMode_saveAlsoLinksToSourceRecord() = runTest {
        val note = Note(title = "n")
        c.noteRepository.addNote(note)
        val vm = editVm(null, CategoryEditPageMode.NEWLINK, RecordType.Note, note.id)
        vm.updateCategoryName("Linked")
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        assertEquals(listOf(id), c.linkedRecordRepository.getRecordIdsLinkedToRecordId(note.id).first())
        assertEquals(listOf(RecordType.Category.recordTypeId), c.linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(note.id).first().map { it.recordTypeId })
    }

    @Test
    fun edit_editMode_loadsExistingAndSaveUpdates() = runTest {
        val cat = Category(categoryName = "Old", comments = "c")
        c.categoryRepository.addCategory(cat)
        val vm = editVm(cat.id, CategoryEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals(cat, vm.pageState.value.category)
        assertEquals("Old", vm.editState.value.editCategoryName)
        assertEquals("c", vm.editState.value.editCategoryDescription)
        assertTrue(vm.editState.value.isValid)

        vm.updateCategoryName("New")
        advanceTimeBy(501); runCurrent()
        assertNull(vm.onSave())

        assertEquals(listOf(cat.copy(categoryName = "New")), c.categoryRepository.getCategoriesStream().first())
    }
}
