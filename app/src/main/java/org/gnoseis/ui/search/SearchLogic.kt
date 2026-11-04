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

import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.components.creatableRecordTypes
import org.gnoseis.ui.components.firstLastTitle
import org.gnoseis.ui.components.matchesSearchExactly
import org.gnoseis.ui.components.normalized
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** The record types search shows, in the order of its chips and result groups. */
val searchTypeOrder: List<RecordType> = creatableRecordTypes

/** How well a result's title matches a query, best first. */
enum class MatchTier {
    /** The title is the query. */
    Exact,
    /** The title starts with the query. */
    StartsWith,
    /** A word in the title starts with the query (a contact: in either name order). */
    WordStart,
    /** The title contains the query somewhere (a contact: in either name order). */
    Contains,
}

private fun startsAtWord(text: String, query: String): Boolean {
    var index = text.indexOf(query)
    while (index >= 0) {
        if (index == 0 || !text[index - 1].isLetterOrDigit()) return true
        index = text.indexOf(query, index + 1)
    }
    return false
}

/** How [result] matches [query], ignoring case and spaces; null if it doesn't. */
fun matchTier(result: SearchResult, query: String): MatchTier? {
    val q = normalized(query)
    if (q.isEmpty()) return null
    val title = normalized(result.recordTitle)
    val forms = listOfNotNull(title, firstLastTitle(result)?.let(::normalized))
    return when {
        matchesSearchExactly(result, query) -> MatchTier.Exact
        title.startsWith(q) -> MatchTier.StartsWith
        forms.any { startsAtWord(it, q) } -> MatchTier.WordStart
        forms.any { it.contains(q) } -> MatchTier.Contains
        else -> null
    }
}

/**
 * The order of results for [query]: by [MatchTier], then more links first, then title A–Z
 * ignoring case. Results that don't match come last.
 */
fun searchResultOrder(query: String): Comparator<SearchResult> =
    compareBy<SearchResult> { matchTier(it, query)?.ordinal ?: MatchTier.entries.size }
        .thenByDescending { it.linkCount }
        .thenBy { it.recordTitle.lowercase() }
        .thenBy { it.recordId }

/** [results] ranked for [query]. */
fun rankSearchResults(results: List<SearchResult>, query: String): List<SearchResult> =
    results.sortedWith(searchResultOrder(query))

/** The type of the record. */
val SearchResult.recordType: RecordType get() = RecordType.from(recordTypeId)

// ********** Filter chips **********

/**
 * The chips' selection after tapping [type]: a type chip is added or removed; All (null)
 * clears them. No type chip selected means All.
 */
fun toggleSearchChip(selected: Set<RecordType>, type: RecordType?): Set<RecordType> = when {
    type == null -> emptySet()
    type in selected -> selected - type
    else -> selected + type
}

/** Whether results of [type] show with the chips at [selected]. */
fun chipsShow(selected: Set<RecordType>, type: RecordType) = selected.isEmpty() || type in selected

/** The type chips a search offers: every type but [excluded] (on Link Records, the source's). */
fun searchChipTypes(excluded: RecordType? = null): List<RecordType> = searchTypeOrder - setOfNotNull(excluded)

/** The chips a search opened with [scope] starts with: that type's chip, or All. */
fun startingChips(scope: RecordType?): Set<RecordType> = setOfNotNull(scope?.takeIf { it in searchTypeOrder })

// ********** Results as the page shows them **********

/** A type's results under its header. */
data class SearchGroup(val type: RecordType, val results: List<SearchResult>)

/** What a search shows for a query, after the chips. */
data class SearchResultsView(
    /** All matches per type, whatever the chips show. */
    val counts: Map<RecordType, Int> = emptyMap(),
    /** The best visible result, shown first and not repeated in its group. */
    val bestMatch: SearchResult? = null,
    /** The other visible results, one group per type in [searchTypeOrder]. */
    val groups: List<SearchGroup> = emptyList(),
    /** Matches of the types the chips hide, per type in [searchTypeOrder]. */
    val hidden: Map<RecordType, Int> = emptyMap(),
) {
    val total: Int get() = counts.values.sum()
    val isEmpty: Boolean get() = bestMatch == null && groups.isEmpty()
    val hiddenTotal: Int get() = hidden.values.sum()
}

/**
 * Lays out [results] (already matched to [query]) for the chips at [selected]: counts per
 * type, the best match (the highest-ranked visible result that [canBeBest] accepts), and the
 * other visible results grouped by type, ranked within each group.
 */
fun searchResultsView(
    results: List<SearchResult>,
    query: String,
    selected: Set<RecordType>,
    canBeBest: (SearchResult) -> Boolean = { true },
): SearchResultsView {
    val ranked = rankSearchResults(results, query)
    val byType = ranked.groupBy { it.recordType }
    val best = ranked.firstOrNull { chipsShow(selected, it.recordType) && canBeBest(it) }
    return SearchResultsView(
        counts = searchTypeOrder.associateWith { byType[it]?.size ?: 0 },
        bestMatch = best,
        groups = searchTypeOrder
            .filter { chipsShow(selected, it) }
            .mapNotNull { type ->
                byType[type].orEmpty()
                    .filter { it.recordId != best?.recordId }
                    .takeIf { it.isNotEmpty() }
                    ?.let { SearchGroup(type, it) }
            },
        hidden = searchTypeOrder
            .filterNot { chipsShow(selected, it) }
            .mapNotNull { type -> byType[type]?.size?.let { type to it } }
            .toMap(),
    )
}

// ********** Match highlight **********

/**
 * Where [query] shows in [result]'s title: every occurrence of the trimmed query, ignoring
 * case and runs of spaces. When there's none (a contact found under the other name order),
 * every occurrence of each query word.
 */
fun highlightRanges(result: SearchResult, query: String): List<IntRange> {
    val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    val title = result.recordTitle
    val whole = Regex(words.joinToString("\\s+") { Regex.escape(it) }, RegexOption.IGNORE_CASE)
    val ranges = whole.findAll(title).map { it.range }.toList()
    if (ranges.isNotEmpty() || words.size == 1) return ranges
    val merged = mutableListOf<IntRange>()
    words
        .flatMap { word -> Regex(Regex.escape(word), RegexOption.IGNORE_CASE).findAll(title).map { it.range }.toList() }
        .sortedBy { it.first }
        .forEach { range ->
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                merged += range
            }
        }
    return merged
}

// ********** Context line **********

/** "No links", "1 link" or "N links". */
fun linksText(count: Int): String = when (count) {
    0 -> "No links"
    1 -> "1 link"
    else -> "$count links"
}

private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** A note's date as the Notes list shows it. */
fun noteDateText(noteDate: Long): String =
    Instant.ofEpochMilli(noteDate).atZone(ZoneId.of("GMT")).format(dateFormat)

/**
 * The second line of a result: a note's date, or a contact's job title and company (whichever
 * are set), then its links.
 */
fun searchContextLine(result: SearchResult): String {
    val details = when (result.recordType) {
        RecordType.Note -> result.noteDate?.let(::noteDateText)
        RecordType.Contact -> listOfNotNull(result.jobTitle, result.company)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
            .ifEmpty { null }
        else -> null
    }
    return listOfNotNull(details, linksText(result.linkCount)).joinToString(" · ")
}

/**
 * How long before [now] [time] was, in [zone]: "just now", "N minutes ago", "N hours ago" (the
 * same day), "yesterday", "N days ago", "last week", then the date.
 */
fun relativeTimeText(time: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val minutes = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(now))
    val day = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(day, today)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
        days <= 0 -> (minutes / 60).let { if (it == 1L) "1 hour ago" else "$it hours ago" }
        days == 1L -> "yesterday"
        days < 7 -> "$days days ago"
        days < 14 -> "last week"
        else -> day.format(dateFormat)
    }
}

/** The context line of a recently changed record. */
fun changedText(updatedAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    "Changed ${relativeTimeText(updatedAt, now, zone)}"
