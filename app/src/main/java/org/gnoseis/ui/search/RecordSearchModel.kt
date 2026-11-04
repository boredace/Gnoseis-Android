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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.pluralName
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.data.repository.SearchRepository
import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.CreateFromSearchState
import org.gnoseis.ui.components.SEARCH_SETTLE_MILLIS
import org.gnoseis.ui.components.createFromSearchOffer
import org.gnoseis.ui.components.createTypesWithFirst
import org.gnoseis.ui.components.searchResultContains

/** Where a search's list was scrolled to, so coming back to it shows the same place. */
data class SearchScroll(val index: Int = 0, val offset: Int = 0)

/**
 * The search logic shared by the Search page and Link Records, run in [scope] (the owning
 * view model's): the query, the chips, the results laid out for the page, the recently
 * changed records and what create from search offers.
 *
 * Results are the records of [chipTypes] other than [excludeIds]. [linkedIds] are records
 * already linked (Link Records): never the best match and not among the recently changed.
 * [startScope] is the type of the list page search was opened from.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RecordSearchModel(
    scope: CoroutineScope,
    searchRepository: SearchRepository,
    linkedRecordRepository: LinkedRecordRepository,
    val chipTypes: List<RecordType> = searchTypeOrder,
    private val startScope: RecordType? = null,
    private val placeholder: String = placeholderFor(startScope),
    createTypes: List<RecordType> = chipTypes,
    private val excludeIds: Set<String> = emptySet(),
    linkedIds: Flow<Set<String>> = flowOf(emptySet()),
) {
    val query = MutableStateFlow("")
    val chips = MutableStateFlow(startingChips(startScope).intersect(chipTypes.toSet()))

    /** Where the list was scrolled to. */
    var scroll = SearchScroll()

    /** The types create from search offers: the list page's type first. */
    val createTypes: List<RecordType> = createTypesWithFirst(createTypes, startScope)

    fun updateQuery(newQuery: String) {
        query.value = newQuery
    }

    /** A tap on a type chip, or on All (null). */
    fun toggleChip(type: RecordType?) {
        chips.value = toggleSearchChip(chips.value, type)
    }

    private val linked: StateFlow<Set<String>> =
        linkedIds.stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val allResults: Flow<List<SearchResult>> =
        searchRepository.getSearchResultsForQuery("%%").map { list ->
            list.filter { it.recordTypeId in chipTypes.map(RecordType::recordTypeId) && it.recordId !in excludeIds }
        }

    private fun List<SearchResult>.matching(query: String) =
        if (isSearchQuery(query)) filter { searchResultContains(it, query) } else emptyList()

    /** Every record that matches the query, whatever the chips show. */
    val matches: StateFlow<List<SearchResult>> =
        combine(allResults, query) { list, q -> list.matching(q) }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The chips and the results laid out for them, from one snapshot so they always agree. */
    private data class LaidOut(val chips: Set<RecordType>, val view: SearchResultsView)

    private val laidOut: StateFlow<LaidOut> =
        combine(matches, query, chips, linked) { list, q, selected, linkedIds ->
            LaidOut(selected, searchResultsView(list, q, selected, canBeBest = { it.recordId !in linkedIds }))
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), LaidOut(chips.value, SearchResultsView()))

    /** The results laid out for the page. */
    val results: StateFlow<SearchResultsView> =
        laidOut.map { it.view }.stateIn(scope, SharingStarted.WhileSubscribed(5000), SearchResultsView())

    private val bestMatchLinks =
        laidOut.map { it.view.bestMatch?.recordId }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else linkedRecordRepository.getLinkedRecordTypesForRecordIdStream(id)
            }

    /** The most recently changed records in the chips' scope, shown before typing. */
    val recent: StateFlow<List<SearchResult>> =
        combine(chips, linked) { selected, linkedIds -> selected to linkedIds }
            .flatMapLatest { (selected, linkedIds) ->
                searchRepository.getRecentlyChanged(
                    types = chipTypes.filter { chipsShow(selected, it) },
                    excludeIds = excludeIds + linkedIds,
                )
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * What to offer to create, once the query has settled. A query too short to search for gets
     * no offer. Whether a result matches exactly is decided on all results, not only the ones
     * the chips show.
     */
    val createFromSearch: StateFlow<CreateFromSearchState> =
        combine(allResults, query.debounce(SEARCH_SETTLE_MILLIS)) { list, q ->
            CreateFromSearchState(
                query = q,
                offer = if (!isSearchQuery(q)) CreateFromSearchOffer.None else createFromSearchOffer(q, list.matching(q)),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), CreateFromSearchState())

    private data class Inputs(
        val query: String,
        val chips: Set<RecordType>,
        val results: SearchResultsView,
        val recent: List<SearchResult>,
    )

    /** Everything the search component shows. */
    val uiState: StateFlow<SearchUiState> =
        combine(
            combine(query, laidOut, recent) { q, l, rc -> Inputs(q, l.chips, l.view, rc) },
            bestMatchLinks,
            createFromSearch,
        ) { inputs, links, create ->
            SearchUiState(
                query = inputs.query,
                placeholder = placeholder,
                chipTypes = chipTypes,
                selectedChips = inputs.chips,
                scope = startScope,
                results = inputs.results,
                bestMatchLinks = links,
                recent = inputs.recent,
                // Only once the query has settled, so the offer doesn't flash while typing
                createOffer = if (create.query == inputs.query) create.offer else CreateFromSearchOffer.None,
                createTypes = createTypes,
            )
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(5000),
            SearchUiState(placeholder = placeholder, chipTypes = chipTypes, selectedChips = chips.value, scope = startScope, createTypes = this.createTypes),
        )

    companion object {
        /** The field's placeholder: "Search all records", or "Search contacts" from a list page. */
        fun placeholderFor(scope: RecordType?): String =
            scope?.takeIf { it in searchTypeOrder }?.let { "Search ${pluralName(it).lowercase()}" }
                ?: "Search all records"
    }
}
