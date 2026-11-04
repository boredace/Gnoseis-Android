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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SearchLogicTest {
    private fun r(
        id: String,
        title: String,
        type: RecordType = RecordType.Item,
        links: Int = 0,
        noteDate: Long? = null,
        jobTitle: String? = null,
        company: String? = null,
    ) = SearchResult(
        recordId = id,
        recordTypeId = type.recordTypeId,
        recordTitle = title,
        linkCount = links,
        noteDate = noteDate,
        jobTitle = jobTitle,
        company = company,
    )

    private fun ids(results: List<SearchResult>) = results.map { it.recordId }

    // ********** Ranking **********

    @Test
    fun ranking_tierOfEachMatch() {
        assertEquals(MatchTier.Exact, matchTier(r("i", "Harbor"), "harbor"))
        assertEquals(MatchTier.StartsWith, matchTier(r("i", "Harbor Lights"), "harb"))
        assertEquals(MatchTier.WordStart, matchTier(r("i", "The Harbour Inn"), "harb"))
        assertEquals(MatchTier.Contains, matchTier(r("i", "Starharbor"), "harb"))
        assertNull(matchTier(r("i", "Pine"), "harb"))
    }

    @Test
    fun ranking_eachTier() {
        val results = listOf(
            r("contains", "Seaharbor"),
            r("word", "Old Harbor Road"),
            r("starts", "Harbor Road"),
            r("exact", "harbor"),
        )
        assertEquals(listOf("exact", "starts", "word", "contains"), ids(rankSearchResults(results, "Harbor")))
    }

    @Test
    fun ranking_tiesByLinksThenTitle() {
        val results = listOf(
            r("b", "harbor b", links = 1),
            r("a", "Harbor a", links = 1),
            r("c", "harbor c", links = 4),
        )
        assertEquals(listOf("c", "a", "b"), ids(rankSearchResults(results, "harbor")))
    }

    @Test
    fun ranking_contactFirstLastIsAWordStart() {
        val contact = r("c", "Hartley, Ann", RecordType.Contact)
        assertEquals(MatchTier.WordStart, matchTier(contact, "ann hart"))
        assertEquals(MatchTier.WordStart, matchTier(contact, "ann"))
        assertEquals(MatchTier.StartsWith, matchTier(contact, "hart"))
        assertEquals(MatchTier.Exact, matchTier(contact, "Ann Hartley"))
        assertNull(matchTier(r("i", "Hartley, Ann", RecordType.Item), "ann hart"))
    }

    @Test
    fun ranking_ignoresCaseAndSpaces() {
        assertEquals(MatchTier.Exact, matchTier(r("i", "Harbor  Pine"), "  harbor pine "))
        assertEquals(MatchTier.StartsWith, matchTier(r("i", "HARBOR Pine"), " harbor  p"))
    }

    // ********** Best match and grouping **********

    private val mixed = listOf(
        r("n1", "Harbor notes", RecordType.Note),
        r("n2", "Old harbor", RecordType.Note),
        r("c1", "Harbor, Jim", RecordType.Contact, links = 2),
        r("o1", "Harbor", RecordType.Organization),
        r("i1", "Harboring", RecordType.Item),
    )

    @Test
    fun bestMatch_isTheHighestRankedVisible() {
        val view = searchResultsView(mixed, "harbor", emptySet())
        assertEquals("o1", view.bestMatch?.recordId)
    }

    @Test
    fun bestMatch_respectsTheChips() {
        val view = searchResultsView(mixed, "harbor", setOf(RecordType.Note))
        assertEquals("n1", view.bestMatch?.recordId)
    }

    @Test
    fun bestMatch_skipsWhatCanNotBeBest() {
        val view = searchResultsView(mixed, "harbor", emptySet(), canBeBest = { it.recordId != "o1" })
        assertEquals("c1", view.bestMatch?.recordId)
        val none = searchResultsView(mixed, "harbor", emptySet(), canBeBest = { false })
        assertNull(none.bestMatch)
        assertEquals(5, none.groups.sumOf { it.results.size })
    }

    @Test
    fun bestMatch_noneWithoutVisibleResults() {
        assertNull(searchResultsView(mixed, "harbor", setOf(RecordType.Category)).bestMatch)
        assertNull(searchResultsView(emptyList(), "harbor", emptySet()).bestMatch)
        assertTrue(searchResultsView(emptyList(), "harbor", emptySet()).isEmpty)
    }

    @Test
    fun grouping_fixedOrder_bestNotRepeated_countsAsSpecified() {
        val view = searchResultsView(mixed.reversed(), "harbor", emptySet())
        assertEquals(
            listOf(RecordType.Note, RecordType.Contact, RecordType.Item),
            view.groups.map { it.type },
        )
        assertTrue(view.groups.none { g -> g.results.any { it.recordId == "o1" } })
        assertEquals(listOf("n1", "n2"), ids(view.groups[0].results))
        // the chip counts include the best match; the group rows don't
        assertEquals(1, view.counts[RecordType.Organization])
        assertEquals(2, view.counts[RecordType.Note])
        assertEquals(0, view.counts[RecordType.Category])
        assertEquals(5, view.total)
    }

    @Test
    fun grouping_hiddenTypesAreCountedForTheMoreCard() {
        val view = searchResultsView(mixed, "harbor", setOf(RecordType.Contact))
        assertEquals("c1", view.bestMatch?.recordId)
        assertTrue(view.groups.isEmpty())
        assertEquals(
            mapOf(RecordType.Note to 2, RecordType.Organization to 1, RecordType.Item to 1),
            view.hidden,
        )
        assertEquals(4, view.hiddenTotal)
        assertTrue(searchResultsView(mixed, "harbor", emptySet()).hidden.isEmpty())
    }

    // ********** Chips **********

    @Test
    fun chips_multiSelect_andAllClears() {
        var chips = emptySet<RecordType>()
        chips = toggleSearchChip(chips, RecordType.Note)
        chips = toggleSearchChip(chips, RecordType.Item)
        assertEquals(setOf(RecordType.Note, RecordType.Item), chips)
        chips = toggleSearchChip(chips, RecordType.Note)
        assertEquals(setOf(RecordType.Item), chips)
        assertEquals(emptySet<RecordType>(), toggleSearchChip(chips, null))
        assertTrue(chipsShow(emptySet(), RecordType.Category))
        assertTrue(chipsShow(chips, RecordType.Item))
        assertTrue(!chipsShow(chips, RecordType.Note))
    }

    @Test
    fun chips_startingScope() {
        assertEquals(emptySet<RecordType>(), startingChips(null))
        assertEquals(setOf(RecordType.Contact), startingChips(RecordType.Contact))
        assertEquals(emptySet<RecordType>(), startingChips(RecordType.LinkedRecord))
    }

    @Test
    fun chips_linkRecordsOffersOnlyLinkableTypes() {
        assertEquals(
            listOf(RecordType.Contact, RecordType.Organization, RecordType.Category, RecordType.Item),
            searchChipTypes(RecordType.Note),
        )
        assertEquals(searchTypeOrder, searchChipTypes())
    }

    // ********** Context line **********

    @Test
    fun contextLine_perType() {
        val day = LocalDateTime.of(2026, 11, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("2026-11-02 · 1 link", searchContextLine(r("n", "N", RecordType.Note, links = 1, noteDate = day)))
        assertEquals("Buyer, Acme · 4 links", searchContextLine(r("c", "C", RecordType.Contact, links = 4, jobTitle = "Buyer", company = "Acme")))
        assertEquals("Buyer · No links", searchContextLine(r("c", "C", RecordType.Contact, jobTitle = "Buyer")))
        assertEquals("Acme · No links", searchContextLine(r("c", "C", RecordType.Contact, jobTitle = " ", company = "Acme")))
        assertEquals("No links", searchContextLine(r("c", "C", RecordType.Contact)))
        assertEquals("2 links", searchContextLine(r("o", "O", RecordType.Organization, links = 2)))
        assertEquals("No links", searchContextLine(r("k", "K", RecordType.Category)))
        assertEquals("1 link", searchContextLine(r("i", "I", RecordType.Item, links = 1)))
    }

    @Test
    fun linksText_wording() {
        assertEquals("No links", linksText(0))
        assertEquals("1 link", linksText(1))
        assertEquals("12 links", linksText(12))
    }

    @Test
    fun relativeTime_wording() {
        val zone = ZoneId.of("UTC")
        fun at(day: Int, hour: Int, minute: Int = 0) =
            LocalDateTime.of(2026, 11, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()
        val now = at(20, 15)
        assertEquals("just now", relativeTimeText(now, now, zone))
        assertEquals("1 minute ago", relativeTimeText(at(20, 14, 59), now, zone))
        assertEquals("1 hour ago", relativeTimeText(at(20, 14), now, zone))
        assertEquals("yesterday", relativeTimeText(at(19, 23), now, zone))
        assertEquals("4 days ago", relativeTimeText(at(16, 9), now, zone))
        assertEquals("last week", relativeTimeText(at(10, 9), now, zone))
        assertEquals("2026-11-01", relativeTimeText(at(1, 9), now, zone))
        assertEquals("Changed yesterday", changedText(at(19, 8), now, zone))
    }

    // ********** Highlight **********

    @Test
    fun highlight_everyOccurrence() {
        assertEquals(listOf(0..2, 8..10), highlightRanges(r("i", "Har and Har"), " har"))
        assertEquals(listOf(4..13), highlightRanges(r("i", "Old harbor  pine"), "harbor pi"))
        assertEquals(emptyList<IntRange>(), highlightRanges(r("i", "Pine"), "harb"))
    }

    @Test
    fun highlight_wordByWordForTheOtherNameOrder() {
        // "Hartley, Ann" found by "ann hart"
        assertEquals(listOf(0..3, 9..11), highlightRanges(r("c", "Hartley, Ann", RecordType.Contact), "ann hart"))
    }
}
