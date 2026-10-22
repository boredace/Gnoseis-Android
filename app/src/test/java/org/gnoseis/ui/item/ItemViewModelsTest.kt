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

package org.gnoseis.ui.item

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.ItemEditPageMode
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
class ItemViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val c = TestAppContainer()

    @After
    fun tearDown() = c.close()

    private fun editVm(itemId: String?, mode: ItemEditPageMode, linkFromType: RecordType? = null, linkFromId: String? = null) =
        ItemEditViewModel(
            SavedStateHandle(mapOf("itemId" to itemId, "pageMode" to mode, "linkFromType" to linkFromType, "linkFromId" to linkFromId)),
            c.itemRepository,
            c.linkedRecordRepository,
        )

    @Test
    fun list_reflectsRepository() = runTest {
        val vm = ItemListViewModel(c.itemRepository)
        vm.itemListPageState.test {
            assertEquals(emptyList<Item>(), awaitItem().items)
            val item = Item(itemName = "Laptop")
            c.itemRepository.addItem(item)
            assertEquals(listOf(item), awaitItem().items)
        }
    }

    @Test
    fun details_loadsLinkedRecords_andDeleteCleansUp() = runTest {
        val item = Item(itemName = "Laptop")
        val cat = Category(categoryName = "Hardware")
        val note = Note(title = "warranty")
        c.itemRepository.addItem(item)
        c.categoryRepository.addCategory(cat)
        c.noteRepository.addNote(note)
        c.linkedRecordRepository.addLinkedRecord(link(item.id, RecordType.Item, cat.id, RecordType.Category))
        c.linkedRecordRepository.addLinkedRecord(link(note.id, RecordType.Note, item.id, RecordType.Item))
        val vm = ItemDetailsViewModel(SavedStateHandle(mapOf("itemId" to item.id)), c.itemRepository, c.linkedRecordRepository)

        vm.linkedRecords.test {
            var linked = awaitItem()
            while (linked.linkedCategories.isEmpty() || linked.linkedNotes.isEmpty()) linked = awaitItem()
            assertEquals(listOf(cat), linked.linkedCategories)
            assertEquals(listOf(note), linked.linkedNotes)
        }
        vm.itemDetailsPageState.test {
            var state = awaitItem()
            while (state.item != item) state = awaitItem()
            vm.deleteItem()
            while (state.item != null) state = awaitItem()
        }
        assertTrue(c.linkedRecordRepository.getRecordIdsLinkedToRecordId(cat.id).first().isEmpty())
    }

    @Test
    fun edit_validationIsDebounced_andBlankIsInvalid() = runTest {
        val vm = editVm(null, ItemEditPageMode.NEW)
        vm.onEvent(ItemEditPageEvent.ItemNameChanged("Laptop"))
        assertFalse(vm.editState.value.isValid)
        advanceTimeBy(501); runCurrent()
        assertTrue(vm.editState.value.isValid)

        vm.onEvent(ItemEditPageEvent.ItemNameChanged(""))
        advanceTimeBy(501); runCurrent()
        assertFalse(vm.editState.value.isValid)
    }

    @Test
    fun edit_save_whenInvalid_doesNothing() = runTest {
        assertNull(editVm(null, ItemEditPageMode.NEW).onSave())
        assertTrue(c.itemRepository.getItemsStream().first().isEmpty())
    }

    @Test
    fun edit_newLinkMode_savesAndLinks() = runTest {
        val cat = Category(categoryName = "Hardware")
        c.categoryRepository.addCategory(cat)
        val vm = editVm(null, ItemEditPageMode.NEWLINK, RecordType.Category, cat.id)
        vm.onEvent(ItemEditPageEvent.ItemNameChanged("Laptop"))
        vm.onEvent(ItemEditPageEvent.ItemDescriptionChanged("16GB"))
        vm.onEvent(ItemEditPageEvent.Save)
        advanceTimeBy(501); runCurrent()

        val id = vm.onSave()!!

        val saved = c.itemRepository.getItem(id)
        assertEquals("Laptop", saved.itemName)
        assertEquals("16GB", saved.comments)
        assertEquals(listOf(saved), c.linkedRecordRepository.getLinkedItemListByCategoryId(cat.id).first())
    }

    @Test
    fun edit_editMode_loadsAndUpdates() = runTest {
        val item = Item(itemName = "Old", comments = "c")
        c.itemRepository.addItem(item)
        val vm = editVm(item.id, ItemEditPageMode.EDIT)

        waitFor { !vm.pageState.value.isNew }
        assertEquals("Old", vm.editState.value.editItemName)
        assertEquals("c", vm.editState.value.editItemDescription)

        vm.onEvent(ItemEditPageEvent.ItemNameChanged("New"))
        advanceTimeBy(501); runCurrent()
        assertNull(vm.onSave())

        assertEquals(item.copy(itemName = "New"), c.itemRepository.getItem(item.id))
    }
}
