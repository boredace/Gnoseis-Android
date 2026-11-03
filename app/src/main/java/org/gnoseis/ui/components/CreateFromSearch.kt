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

package org.gnoseis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.singularName

/** Where a search offers to create a record from its query (#10). */
enum class CreateFromSearchOffer {
    /** Nothing to offer: the query is too short, or a result matches it exactly. */
    None,
    /** No results: the offer replaces the list. */
    NoResults,
    /** Results, but none matches exactly: the offer is the list's last item. */
    AfterResults,
}

/** The record types a search can create, in the order their buttons show. */
val creatableRecordTypes = listOf(
    RecordType.Note,
    RecordType.Contact,
    RecordType.Organization,
    RecordType.Category,
    RecordType.Item,
)

private val spaces = Regex("\\s+")

private fun normalized(text: String) = text.trim().replace(spaces, " ").lowercase()

/** A contact's title shows as "Last, First"; this is it as "First Last". Null for other records. */
private fun firstLastTitle(result: SearchResult): String? {
    val title = result.recordTitle
    return if (result.recordTypeId == RecordType.Contact.recordTypeId && ", " in title) {
        "${title.substringAfter(", ")} ${title.substringBefore(", ")}"
    } else null
}

/**
 * Whether a result's [title] contains the [query], ignoring case, the spaces around the query
 * and runs of spaces, so a result that [matchesSearchExactly] is always found.
 */
fun searchTitleContains(title: String, query: String): Boolean = normalized(title).contains(normalized(query))

/** Whether [result] is found by the [query]; a contact also as "First Last". */
fun searchResultContains(result: SearchResult, query: String): Boolean =
    searchTitleContains(result.recordTitle, query) || firstLastTitle(result)?.let { searchTitleContains(it, query) } == true

/**
 * Whether a result's [title] is the [query]: the same once trimmed, with runs of spaces as one
 * and ignoring case. A title that only contains the query doesn't match.
 */
fun matchesSearchExactly(title: String?, query: String): Boolean =
    title != null && normalized(query).isNotEmpty() && normalized(title) == normalized(query)

/** Whether [result] is the record the [query] names; a contact also as "First Last". */
fun matchesSearchExactly(result: SearchResult, query: String): Boolean =
    matchesSearchExactly(result.recordTitle, query) || matchesSearchExactly(firstLastTitle(result), query)

/** What to offer for a settled [query] with these [results]. */
fun createFromSearchOffer(query: String, results: List<SearchResult>): CreateFromSearchOffer = when {
    query.isBlank() -> CreateFromSearchOffer.None
    results.isEmpty() -> CreateFromSearchOffer.NoResults
    results.any { matchesSearchExactly(it, query) } -> CreateFromSearchOffer.None
    else -> CreateFromSearchOffer.AfterResults
}

/** How long a query stays unchanged before a search offers to create a record from it. */
const val SEARCH_SETTLE_MILLIS = 500L

/** What a search offers for its settled [query]. */
data class CreateFromSearchState(
    val query: String = "",
    val offer: CreateFromSearchOffer = CreateFromSearchOffer.None,
)

/**
 * Offers to create a record of each of [types] named [query]: in place of the results when
 * [offer] is [CreateFromSearchOffer.NoResults], after them when it's
 * [CreateFromSearchOffer.AfterResults]. Shows nothing for [CreateFromSearchOffer.None].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFromSearch(
    query: String,
    offer: CreateFromSearchOffer,
    types: List<RecordType>,
    onCreate: (RecordType) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (offer != CreateFromSearchOffer.None) {
        val name = query.trim()
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (offer == CreateFromSearchOffer.NoResults) "No matches for “$name”" else "Not what you're looking for?",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                text = "Create “$name” as:",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                types.forEach { type ->
                    OutlinedButton(onClick = { onCreate(type) }) {
                        RecordTypeIcon(
                            type = type,
                            colorFilter = ColorFilter.tint(recordTypeColors(type).outline),
                            size = 18.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = singularName(type))
                    }
                }
            }
        }
    }
}
