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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SearchRepository
import org.gnoseis.domain.usecase.GetLinkSourceUseCase
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.ui.components.CreateFromSearchState
import org.gnoseis.ui.components.creatableRecordTypes
import org.gnoseis.ui.search.RecordSearchModel
import org.gnoseis.ui.search.SearchSelection
import org.gnoseis.ui.search.SearchUiState
import org.gnoseis.ui.search.recordType
import org.gnoseis.ui.search.searchChipTypes

class LinkRecordsViewModel(
    savedStateHandle: SavedStateHandle,
    private val linkedRecordRepository: LinkedRecordRepository,
    searchRepository: SearchRepository,
    getLinkSource: GetLinkSourceUseCase,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<LinkRecordsRoute>()

    val sourceRecordId: String = route.sourceRecordId
    val sourceRecordType: RecordType = route.sourceRecordType

    private val _source = MutableStateFlow<LinkSource?>(null)

    /** The record the selected records are linked to, for the "Linking to" card. */
    val source: StateFlow<LinkSource?> = _source.asStateFlow()

    init {
        viewModelScope.launch { _source.value = getLinkSource(sourceRecordType, sourceRecordId) }
    }

    /** The records linked to the source already. */
    private val linkedIds: StateFlow<Set<String>> =
        linkedRecordRepository.getRecordIdsLinkedToRecordId(sourceRecordId)
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The types a new record can be created as and linked: all but the source's. */
    val createFromSearchTypes: List<RecordType> = creatableRecordTypes - sourceRecordType

    /** The search: every type but the source's, without the source itself. */
    val search = RecordSearchModel(
        scope = viewModelScope,
        searchRepository = searchRepository,
        linkedRecordRepository = linkedRecordRepository,
        chipTypes = searchChipTypes(sourceRecordType),
        placeholder = "Search records to link",
        createTypes = createFromSearchTypes,
        excludeIds = setOf(sourceRecordId),
        linkedIds = linkedIds,
    )

    val uiState: StateFlow<SearchUiState> = search.uiState

    val currentQuery: MutableStateFlow<String> = search.query

    /** Every record that matches the query, whatever the chips show. */
    val searchResults: StateFlow<List<SearchResult>> = search.matches

    /** What to offer to create, once the query has settled. */
    val createFromSearch: StateFlow<CreateFromSearchState> = search.createFromSearch

    fun updateCurrentQuery(newQuery: String) = search.updateQuery(newQuery)

    fun toggleChip(type: RecordType?) = search.toggleChip(type)

    // ********** Selection **********

    private val _selected = MutableStateFlow<List<SearchResult>>(emptyList())

    /**
     * The selected records, in the order they were picked. Kept apart from the results, so
     * a new query, an erased query or other chips leave it as it is: only unchecking a record
     * or linking removes it.
     */
    val selected: StateFlow<List<SearchResult>> = _selected.asStateFlow()

    /** Which records show as checked, and which as linked already. */
    val selection: StateFlow<SearchSelection> =
        combine(_selected, linkedIds) { picked, linked -> SearchSelection(picked.map { it.recordId }.toSet(), linked) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SearchSelection())

    /** Checks [result], or unchecks it if it's checked. A record linked already can't be checked. */
    fun toggleSelectedSearchResult(result: SearchResult) {
        if (result.recordId in linkedIds.value) return
        _selected.update { picked ->
            if (picked.any { it.recordId == result.recordId }) picked.filterNot { it.recordId == result.recordId }
            else picked + result
        }
    }

    /** Unchecks the record [recordId]. */
    fun unselect(recordId: String) {
        _selected.update { picked -> picked.filterNot { it.recordId == recordId } }
    }

    /** The selected records, to link to the source as well when a record created from search is saved. */
    fun selectedForNewRecord(): List<Pair<String, RecordType>> =
        _selected.value.filter { it.recordId !in linkedIds.value }.map { it.recordId to it.recordType }

    private val linkMutex = Mutex()

    /** Links every selected record to the source, whether the current search shows it or not, then clears the selection. */
    suspend fun linkSelectedResults() {
        // Ignore a second tap on "Link" while the first one is still saving
        if (!linkMutex.tryLock()) return
        try {
            val picked = _selected.value
            linkedRecordRepository.linkRecordsTo(
                sourceRecordId,
                sourceRecordType,
                picked.map { it.recordId to it.recordType },
            )
            _selected.update { current -> current.filterNot { c -> picked.any { it.recordId == c.recordId } } }
        } finally {
            linkMutex.unlock()
        }
    }
}
