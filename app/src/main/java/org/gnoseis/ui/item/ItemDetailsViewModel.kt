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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.ItemRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.linkedRecordsFlow
import org.gnoseis.data.repository.SettingsRepository
import org.gnoseis.domain.usecase.DeleteRecordUseCase

class ItemDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val itemRepository: ItemRepository,
    private var linkedRecordRepository: LinkedRecordRepository,
    settingsRepository: SettingsRepository,
) : ViewModel(){
    private val deleteRecord : DeleteRecordUseCase =
        DeleteRecordUseCase(
            itemRepository = itemRepository,
            linkedRecordRepository = linkedRecordRepository,
            settingsRepository = settingsRepository,
        )
    private var itemId : String = checkNotNull(savedStateHandle.toRoute<ItemDetailsRoute>().itemId)

    val linkedRecords: StateFlow<LinkedRecords> =
        linkedRecordsFlow(linkedRecordRepository, itemId, RecordType.Item).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            LinkedRecords()
        )

    val itemDetailsPageState: StateFlow<ItemDetailsPageState> =
        combine(
            itemRepository.getItemStream(itemId),
            linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(itemId)
        ){
            item, linkedRecordTypes ->
            ItemDetailsPageState(
                item = item,
                linkedRecordTabs = linkedRecordTypes
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            ItemDetailsPageState()
        )

    fun deleteItem() {
        viewModelScope.launch {
            deleteRecord(itemId)
        }
    }
    data class ItemDetailsPageState(
        val item: Item? = Item(ownerDbId = "dbx", itemName = "xxx"),
        val linkedRecordTabs: List<LinkedRecordTypeCount> = emptyList()
    )




    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}