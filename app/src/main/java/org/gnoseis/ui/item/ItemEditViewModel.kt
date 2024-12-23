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

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.enums.ItemEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.ItemRepository
import org.gnoseis.data.repository.LinkedRecordRepository

class ItemEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private val linkedRecordRepository: LinkedRecordRepository,

    ) : ViewModel(){
    private val itemId : String? = savedStateHandle.toRoute<ItemEditRoute>().itemId

    private val _editState = mutableStateOf(
        EditState()
    )
    val editState: State<EditState> = _editState

    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<ItemEditRoute>().pageMode),
            itemId =  null,
            linkFromType = savedStateHandle.toRoute<ItemEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<ItemEditRoute>().linkFromId,
            item = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        if (!itemId.isNullOrEmpty()) {
            viewModelScope.launch {
                itemRepository.getItem(itemId).let {

                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<ItemEditRoute>().pageMode),
                        itemId = it.id,
                        linkFromType = savedStateHandle.toRoute<ItemEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<ItemEditRoute>().linkFromId,
                        item = it
                    )

                    _editState.value = _editState.value.copy(
                        isValid = true,
                        editItemName = it.itemName,
                        editItemDescription = it.comments,
                    )
                }
            }
        }
    }

    fun onEvent(event: ItemEditPageEvent) {
        when (event) {
            is ItemEditPageEvent.ItemNameChanged -> {
                updateItemName(event.itemName)
            }
            is ItemEditPageEvent.ItemDescriptionChanged -> {
                updateItemDescription(event.itemDescription)
            }
            is ItemEditPageEvent.Save -> { }
        }
    }

    private var debounceJob: Job? = null

    private fun updateItemName(it: String) {
        // update editItemName only immediately
        _editState.value = _editState.value.copy(editItemName = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(it),
                editItemName = it,
            )
        }
    }

    private fun updateItemDescription(it: String) {
        // update editItemDescription only immediately
        _editState.value = _editState.value.copy(editItemDescription = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(_editState.value.editItemName),
                editItemDescription = it,
            )
        }
    }

    private fun validateInput(itemName: String?):Boolean {
        var result = false
        if(!itemName.isNullOrEmpty()) result = true
        return result
    }

        // TODO: Call onSave from Event above


    suspend fun onSave(): String? {
        if(
        // Add new Item
            _editState.value.isValid &&
            _pageState.value.isNew
        ) {
            val addedRecordRowId = itemRepository.addItem(
                Item(
                    ownerDbId = "db1",
                    itemName = _editState.value.editItemName!!, // assuming cannot be valid if null
                    comments = _editState.value.editItemDescription,
                )
            )
            val addedRecordId = itemRepository.getItemByRowId(addedRecordRowId).id
            if (
            // Also link to source record
                _pageState.value.pageMode == ItemEditPageMode.NEWLINK &&
                _pageState.value.linkFromId != null &&
                _pageState.value.linkFromType != null
            ) {
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = _pageState.value.linkFromId!!,
                        record2Id = addedRecordId,
                        record1TypeId = _pageState.value.linkFromType!!.recordTypeId,
                        record2TypeId = RecordType.Item.recordTypeId
                    )
                )
            }
            return addedRecordId

        } else if (
        // Update Existing Item
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            itemRepository.updateItem(_pageState.value.item!!.copy(
                itemName = _editState.value.editItemName!!,  // assuming cannot be valid if null
                comments = _editState.value.editItemDescription,
            ))
            return null
        } else {
            return null
        }
    }

    data class PageState(
        val isNew: Boolean = true,
        val pageMode: ItemEditPageMode? = null,
        val itemId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val item: Item? = Item(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val editItemName: String? = null,
        val editItemDescription: String? = null,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}