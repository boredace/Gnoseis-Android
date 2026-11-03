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

import org.gnoseis.ui.components.searchResultContains
import org.gnoseis.ui.components.creatableRecordTypes
import org.gnoseis.ui.components.createFromSearchOffer
import org.gnoseis.ui.components.SEARCH_SETTLE_MILLIS
import org.gnoseis.ui.components.CreateFromSearchState
import org.gnoseis.data.enums.RecordType
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
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

    private val route = savedStateHandle.toRoute<LinkRecordsRoute>()

    val sourceRecordId = MutableStateFlow(route.sourceRecordId)
    val sourceRecordTypeId = MutableStateFlow(route.sourceRecordType.recordTypeId)

    private var alreadyLinked : StateFlow<List<String>> =
        linkedRecordRepository.getRecordIdsLinkedToRecordId(sourceRecordId.value)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    private fun List<SearchResult>.matching(query: String) =
        filter { searchResultContains(it, query) }

    // Not the source record, nor any record of its type: those can't be linked to it
    private fun List<SearchResult>.linkable(source: String) =
        filterNot { it.recordId.contains(source) || it.recordTypeId == sourceRecordTypeId.value }

    @OptIn(FlowPreview::class)
    var searchResults : StateFlow<List<SearchResult>> =
        searchRepository.getSearchResultsForQuery("%${currentQuery.value}%")
            .debounce(1000L)
            .combine(currentQuery) { list, filter -> list.matching(filter) }
            .combine(sourceRecordId) { list, source -> list.linkable(source) }
            .combine(alreadyLinked) { searchResults, linkedIds ->
                // Combined (not read once) so the flags stay correct whichever loads first
                searchResults.onEach {
                    if (linkedIds.contains(it.recordId)) it.isLinked = true
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /** #10: the types a new record can be created as and linked: all but the source's. */
    val createFromSearchTypes: List<RecordType> = creatableRecordTypes - route.sourceRecordType

    /** #10: what to offer to create, once the query has settled. */
    @OptIn(FlowPreview::class)
    val createFromSearch: StateFlow<CreateFromSearchState> =
        searchRepository.getSearchResultsForQuery("%%")
            .combine(currentQuery.debounce(SEARCH_SETTLE_MILLIS)) { list, query ->
                CreateFromSearchState(query, createFromSearchOffer(query, list.matching(query).linkable(route.sourceRecordId)))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                CreateFromSearchState()
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

    private val linkMutex = Mutex()

    suspend fun linkSelectedResults() {
        // Ignore a second tap on "Link" while the first one is still saving
        if (!linkMutex.tryLock()) return
        try {
            searchResults.value
                .filter { it.isSelected == true && it.isLinked != true }
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
                    it.isSelected = false
                    it.isLinked = true
                }
            selectedCount.intValue = 0
        } finally {
            linkMutex.unlock()
        }
    }
}