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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.repository.ItemRepository

class ItemEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository

) : ViewModel(){
    private var itemId : String = checkNotNull(savedStateHandle[ItemEditDestination.itemIdArg])
//    private var isValid = MutableStateFlow(false)
//    private var isNew = MutableStateFlow(true)

    var itemEditPageState by mutableStateOf(ItemEditPageState())

    init {
        if (itemId != "new") {
            viewModelScope.launch {
                itemRepository.getItem(itemId).let {
                    itemEditPageState.isNew = false
                    itemEditPageState.isValid = true
                    itemEditPageState.editItemName = it.itemName
                    itemEditPageState.editItemDescription = it.comments
                    itemEditPageState.item = it
                }
            }
        }
    }
    fun onEvent(event: ItemEditPageEvent) {
        when (event) {
            is ItemEditPageEvent.ItemNameChanged -> {
                var _isValid = false
                if(event.itemName.length >= 1) {
                    _isValid = true
                }
                itemEditPageState = itemEditPageState.copy(editItemName = event.itemName, isValid = _isValid)
            }
            is ItemEditPageEvent.ItemDescriptionChanged -> {
                itemEditPageState = itemEditPageState.copy(editItemDescription = event.itemDescription)
            }
            is ItemEditPageEvent.Save -> { }
        }
    }


    suspend fun onSave(): String? {
        if(itemEditPageState.isValid && itemEditPageState.isNew) {
            val addedItemRowId = itemRepository.addItem(
                Item(
                    ownerDbId = "db1",
                    itemName = itemEditPageState.editItemName,
                    comments = itemEditPageState.editItemDescription
                )
            )
            return itemRepository.getItemByRowId(addedItemRowId).id
        } else if (itemEditPageState.isValid && !itemEditPageState.isNew) {
            itemRepository.updateItem(
                itemEditPageState.item!!.copy(
                    itemName = itemEditPageState.editItemName,
                    comments = itemEditPageState.editItemDescription
                )
            )
            return null
        } else {
            return null
        }
    }



    data class ItemEditPageState(
        var isValid: Boolean = false,
        var isNew: Boolean = true,
        var editItemName: String = "",
        var editItemDescription: String? = null,
        var item: Item? = Item()
    )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}