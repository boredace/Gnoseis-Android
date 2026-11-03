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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.repository.SearchRepository
import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.CreateFromSearchState
import org.gnoseis.ui.components.SEARCH_SETTLE_MILLIS
import org.gnoseis.ui.components.createFromSearchOffer
import org.gnoseis.ui.components.searchResultContains

class SearchPageViewModel(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository
) : ViewModel(){
    var currentQuery = MutableStateFlow("")


    private fun List<SearchResult>.matching(query: String) =
        filter { searchResultContains(it, query) }

    @OptIn(FlowPreview::class)
    var searchResults : StateFlow<List<SearchResult>> =
        searchRepository.getSearchResultsForQuery("%${currentQuery.value}%")
            .debounce(1000L)
            .combine(currentQuery) { list, filter -> list.matching(filter) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /**
     * #10: what to offer to create, once the query has settled. Only a query long enough to
     * search ([MIN_QUERY_LENGTH]) gets an offer.
     */
    @OptIn(FlowPreview::class)
    val createFromSearch: StateFlow<CreateFromSearchState> =
        searchRepository.getSearchResultsForQuery("%%")
            .combine(currentQuery.debounce(SEARCH_SETTLE_MILLIS)) { list, query ->
                CreateFromSearchState(
                    query = query,
                    offer = if (query.trim().length < MIN_QUERY_LENGTH) CreateFromSearchOffer.None
                        else createFromSearchOffer(query, list.matching(query)),
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                CreateFromSearchState()
            )
    public fun updateCurrentQuery(newQuery: String) {
        currentQuery.value = newQuery
    }

    companion object {
        /** The shortest query the Search page searches for. */
        const val MIN_QUERY_LENGTH = 3
    }

}

