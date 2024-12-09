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

package org.gnoseis.ui.link

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SearchRepository

class LinkRecordsViewModel(
    savedStateHandle: SavedStateHandle,
    private val linkedRecordRepository: LinkedRecordRepository,
    private val searchRepository: SearchRepository
) : ViewModel(){
    var currentQuery = MutableStateFlow("")

    private val _sourceRecordId : String = checkNotNull(savedStateHandle[LinkRecordsDestination.sourceRecordIdArg])
    private val _sourceRecordTypeId : Int = checkNotNull(savedStateHandle[LinkRecordsDestination.sourceRecordTypeIdArg])

    val sourceRecordId = MutableStateFlow(_sourceRecordId)
    val sourceRecordTypeId = MutableStateFlow(_sourceRecordTypeId)

    private var alreadyLinked : StateFlow<List<String>> =
        linkedRecordRepository.getRecordIdsLinkedToRecordId(sourceRecordId.value)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    @OptIn(FlowPreview::class)
    var searchResults : StateFlow<List<SearchResult>> =
        searchRepository.getSearchResultsForQuery("%${currentQuery.value}%")
            .debounce(1000L)
            .combine(currentQuery) { list, filter ->
                list.filter { it.recordTitle.contains(filter, ignoreCase = true) }
            }
            .combine(sourceRecordId) {list, source ->
                list.filterNot { it.recordId.contains(source) || it.recordTypeId == sourceRecordTypeId.value}
            }
            .map {searchResults ->
                searchResults.map {
                   if (alreadyLinked.value.any { existing -> existing.equals(it.recordId) }) {
                       it.isLinked = true
                   }
                   it
                }
                searchResults
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    var selectedCount = mutableIntStateOf(0)

    fun updateCurrentQuery(newQuery: String) {
        currentQuery.value = newQuery
        for (result in searchResults.value) {
            result.isSelected = false
        }
        selectedCount.intValue =  searchResults.value.count { it.isSelected == true}
    }

    fun toggleSelectedSearchResult(selectedResult: SearchResult) {
        searchResults.value.find { it.recordId == selectedResult.recordId }?.isSelected =
            if (selectedResult.isSelected != true) {
                 true
            } else
            {  false }
        selectedCount.intValue =  searchResults.value.count { it.isSelected == true}
    }

    suspend fun linkSelectedResults() {
        searchResults.value
            .filter { it.isSelected == true}
            .forEach {
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = sourceRecordId.value,
                        record2Id = it.recordId,
                        record1TypeId = sourceRecordTypeId.value,
                        record2TypeId = it.recordTypeId
                    )
                )
            }
    }
}