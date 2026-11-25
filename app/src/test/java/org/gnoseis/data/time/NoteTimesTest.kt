/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.data.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * #68: the shared conversion examples, both ways. Zones are set by id, never the machine's.
 */
class NoteTimesTest {
    /** One row of the examples: a note written at [local] in [zone]. */
    data class Example(
        val zone: String,
        val local: String,
        val v3CreateDateTime: Long,
        val v3CreateDate: Long,
        val v4CreateDateTime: Long,
        val v4CreateDate: Long,
        val shown: String,
    )

    private val examples = listOf(
        Example("UTC", "2026-11-14T14:30", 1794666600000, 1794614400000, 1794666600000, 1794614400000, "2026-11-14T14:30"),
        Example("America/Toronto", "2026-07-15T14:30", 1784125800000, 1784073600000, 1784140200000, 1784088000000, "2026-07-15T14:30"),
        Example("America/Toronto", "2026-01-15T14:30", 1768487400000, 1768435200000, 1768505400000, 1768453200000, "2026-01-15T14:30"),
        Example("America/Toronto", "2026-11-14T00:00", 1794614400000, 1794614400000, 1794632400000, 1794632400000, "2026-11-14T00:00"),
        Example("America/Toronto", "2026-03-08T02:30", 1772937000000, 1772928000000, 1772955000000, 1772946000000, "2026-03-08T03:30"),
        Example("America/Toronto", "2026-11-01T01:30", 1793496600000, 1793491200000, 1793511000000, 1793505600000, "2026-11-01T01:30"),
        Example("Asia/Tokyo", "2026-11-14T08:15", 1794644100000, 1794614400000, 1794611700000, 1794582000000, "2026-11-14T08:15"),
        Example("Asia/Kolkata", "2026-11-14T23:59", 1794700740000, 1794614400000, 1794680940000, 1794594600000, "2026-11-14T23:59"),
        Example("Europe/London", "2026-06-01T00:30", 1780273800000, 1780272000000, 1780270200000, 1780268400000, "2026-06-01T00:30"),
        Example("America/Santiago", "2026-09-06T00:00", 1788652800000, 1788652800000, 1788667200000, 1788667200000, "2026-09-06T01:00"),
        Example("America/Santiago", "2026-09-06T00:30", 1788654600000, 1788652800000, 1788669000000, 1788667200000, "2026-09-06T01:30"),
        Example("Australia/Lord_Howe", "2026-10-04T02:15", 1791080100000, 1791072000000, 1791042300000, 1791034200000, "2026-10-04T02:45"),
    )

    @Test
    fun upgradeFromV3_givesTheV4Columns_forEveryExample() = examples.forEach { e ->
        assertEquals(
            "${e.zone} ${e.local}",
            StoredNoteTimes(e.v4CreateDateTime, e.v4CreateDate),
            NoteTimes.upgradeFromV3(e.v3CreateDateTime, ZoneId.of(e.zone)),
        )
    }

    @Test
    fun toStored_fromTheLocalDayAndTime_givesTheV4Columns_forEveryExample() = examples.forEach { e ->
        val local = LocalDateTime.parse(e.local)
        assertEquals(
            "${e.zone} ${e.local}",
            StoredNoteTimes(e.v4CreateDateTime, e.v4CreateDate),
            NoteTimes.toStored(local.toLocalDate(), local.toLocalTime(), ZoneId.of(e.zone)),
        )
    }

    @Test
    fun shown_inTheSameZone_isTheShownColumn_forEveryExample() = examples.forEach { e ->
        assertEquals(
            "${e.zone} ${e.local}",
            LocalDateTime.parse(e.shown),
            NoteTimes.shown(e.v4CreateDateTime, ZoneId.of(e.zone)),
        )
    }

    @Test
    fun upgradeFromV3_ignoresTheOldCreateDate() {
        // Same createDateTime, whatever createDate held
        val zone = ZoneId.of("Asia/Tokyo")
        assertEquals(StoredNoteTimes(1794611700000, 1794582000000), NoteTimes.upgradeFromV3(1794644100000, zone))
    }

    @Test
    fun shown_tokyoNote_inThreeZones() {
        val tokyoNote = 1794611700000
        assertEquals(LocalDateTime.parse("2026-11-14T08:15"), NoteTimes.shown(tokyoNote, ZoneId.of("Asia/Tokyo")))
        assertEquals(LocalDateTime.parse("2026-11-13T18:15"), NoteTimes.shown(tokyoNote, ZoneId.of("America/Toronto")))
        assertEquals(LocalDateTime.parse("2026-11-14T04:45"), NoteTimes.shown(tokyoNote, ZoneId.of("Asia/Kolkata")))
    }

    @Test
    fun forNow_isTheMoment_andTheStartOfItsLocalDay() {
        val now = Instant.ofEpochMilli(1794611700123) // 2026-11-14 08:15:00.123 in Tokyo
        assertEquals(StoredNoteTimes(1794611700123, 1794582000000), NoteTimes.forNow(now, ZoneId.of("Asia/Tokyo")))
        // The same moment is still 2026-11-13 in Toronto
        assertEquals(
            NoteTimes.startOfDay(LocalDate.parse("2026-11-13"), ZoneId.of("America/Toronto")),
            NoteTimes.forNow(now, ZoneId.of("America/Toronto")).createDate,
        )
    }

    @Test
    fun edited_nothingPicked_keepsBothColumnsExactly() {
        val stored = StoredNoteTimes(1794611700123, 1794582000000)
        assertSame(stored, NoteTimes.edited(stored, null, null, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun edited_nothingPicked_inAnotherZone_keepsBothColumnsExactly() {
        // Saved in Tokyo, opened and saved in Toronto
        val stored = StoredNoteTimes(1794611700000, 1794582000000)
        assertSame(stored, NoteTimes.edited(stored, null, null, ZoneId.of("America/Toronto")))
    }

    @Test
    fun edited_dayPicked_keepsTheShownTime() {
        val zone = ZoneId.of("America/Toronto")
        val stored = NoteTimes.toStored(LocalDate.parse("2026-07-15"), LocalTime.parse("14:30:12"), zone)
        val edited = NoteTimes.edited(stored, LocalDate.parse("2026-01-15"), null, zone)
        assertEquals(LocalDateTime.parse("2026-01-15T14:30:12"), NoteTimes.shown(edited.createDateTime, zone))
        assertEquals(NoteTimes.startOfDay(LocalDate.parse("2026-01-15"), zone), edited.createDate)
    }

    @Test
    fun edited_timePicked_keepsTheShownDay() {
        val zone = ZoneId.of("America/Toronto")
        val stored = NoteTimes.toStored(LocalDate.parse("2026-07-15"), LocalTime.parse("14:30"), zone)
        val edited = NoteTimes.edited(stored, null, LocalTime.parse("09:05"), zone)
        assertEquals(StoredNoteTimes(1784120700000, 1784088000000), edited)
    }

    @Test
    fun edited_tokyoNote_timePickedInToronto_keepsTheTorontoDay() {
        // Shown in Toronto on 2026-11-13 at 18:15
        val stored = StoredNoteTimes(1794611700000, 1794582000000)
        val zone = ZoneId.of("America/Toronto")
        val edited = NoteTimes.edited(stored, null, LocalTime.parse("20:00"), zone)
        assertEquals(LocalDateTime.parse("2026-11-13T20:00"), NoteTimes.shown(edited.createDateTime, zone))
        assertEquals(NoteTimes.startOfDay(LocalDate.parse("2026-11-13"), zone), edited.createDate)
    }

    @Test
    fun edited_timePickedInAGap_movesLaterByTheGap() {
        val zone = ZoneId.of("America/Toronto")
        val stored = NoteTimes.toStored(LocalDate.parse("2026-03-08"), LocalTime.parse("10:00"), zone)
        val edited = NoteTimes.edited(stored, null, LocalTime.parse("02:30"), zone)
        assertEquals(StoredNoteTimes(1772955000000, 1772946000000), edited)
    }

    @Test
    fun edited_dayPickedIntoAGap_movesLaterByTheGap() {
        val zone = ZoneId.of("America/Toronto")
        val stored = NoteTimes.toStored(LocalDate.parse("2026-03-01"), LocalTime.parse("02:30"), zone)
        val edited = NoteTimes.edited(stored, LocalDate.parse("2026-03-08"), null, zone)
        assertEquals(StoredNoteTimes(1772955000000, 1772946000000), edited)
    }

    @Test
    fun startOfDay_inAMidnightGap_isTheEndOfTheGap() {
        assertEquals(1788667200000, NoteTimes.startOfDay(LocalDate.parse("2026-09-06"), ZoneId.of("America/Santiago")))
    }
}
