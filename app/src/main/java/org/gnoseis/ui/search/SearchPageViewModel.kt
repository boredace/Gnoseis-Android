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


package org.gnoseis.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SearchRepository
import org.gnoseis.ui.components.CreateFromSearchState

class SearchPageViewModel(
    savedStateHandle: SavedStateHandle,
    searchRepository: SearchRepository,
    linkedRecordRepository: LinkedRecordRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<SearchRoute>()

    /** The type of the list page search was opened from; null from Home. */
    val scope: RecordType? = route.scopeTypeId?.let { id -> RecordType.entries.firstOrNull { it.recordTypeId == id } }

    /** The search itself; kept here, so it's still there on coming back from a page this one opened. */
    val search = RecordSearchModel(
        scope = viewModelScope,
        searchRepository = searchRepository,
        linkedRecordRepository = linkedRecordRepository,
        startScope = scope,
    )

    val uiState: StateFlow<SearchUiState> = search.uiState

    val currentQuery: MutableStateFlow<String> = search.query

    /** Every record that matches the query, whatever the chips show. */
    val searchResults: StateFlow<List<SearchResult>> = search.matches

    /** What to offer to create, once the query has settled. */
    val createFromSearch: StateFlow<CreateFromSearchState> = search.createFromSearch

    fun updateCurrentQuery(newQuery: String) = search.updateQuery(newQuery)

    fun toggleChip(type: RecordType?) = search.toggleChip(type)

    companion object {
        /** The shortest query the Search page searches for. */
        const val MIN_QUERY_LENGTH = SEARCH_MIN_QUERY_LENGTH
    }
}
