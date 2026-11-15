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

package org.gnoseis.data.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The two stored note times (#68), both UTC epoch ms: [createDateTime] is the moment of the
 * note, [createDate] the moment its local day started, in the time zone it was saved in.
 */
data class StoredNoteTimes(val createDateTime: Long, val createDate: Long)

/**
 * The note time rule (#68). Note times are stored in UTC and shown in the device's time zone,
 * which callers pass in (`ZoneId.systemDefault()` in the app), so tests can fix it.
 *
 * Local to UTC: a local date and time becomes an instant through [LocalDateTime.atZone]. In a
 * daylight-saving gap it moves later by the gap; in an overlap it is the earlier moment. A day
 * starts at [LocalDate.atStartOfDay], the end of the gap when midnight falls in one.
 *
 * Nothing reads `createDate` to show, group or sort notes; it is written so the column keeps a
 * plain meaning for anyone reading the database.
 */
object NoteTimes {
    /** The columns for a note at local [day] and [time] in [zone]. */
    fun toStored(day: LocalDate, time: LocalTime, zone: ZoneId): StoredNoteTimes =
        toStored(LocalDateTime.of(day, time), zone)

    private fun toStored(local: LocalDateTime, zone: ZoneId): StoredNoteTimes {
        val instant = local.atZone(zone).toInstant()
        return of(instant, zone)
    }

    /** The columns for a new note whose day and time nobody changed: the moment of saving. */
    fun forNow(now: Instant, zone: ZoneId): StoredNoteTimes = of(now, zone)

    private fun of(instant: Instant, zone: ZoneId) = StoredNoteTimes(
        createDateTime = instant.toEpochMilli(),
        createDate = instant.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli(),
    )

    /**
     * The columns for a note stored before schema version 4, which kept the local clock time
     * the note was written as if it were UTC. That time is read back as UTC and converted in
     * [zone]. The old `createDate` isn't used; a note without a time of day becomes local midnight.
     */
    fun upgradeFromV3(oldCreateDateTime: Long, zone: ZoneId): StoredNoteTimes =
        toStored(LocalDateTime.ofInstant(Instant.ofEpochMilli(oldCreateDateTime), ZoneOffset.UTC), zone)

    /** The local day and time a note shows in [zone]. */
    fun shown(createDateTime: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(createDateTime), zone)

    /** The local day a note is grouped and counted by in [zone]. */
    fun shownDay(createDateTime: Long, zone: ZoneId): LocalDate = shown(createDateTime, zone).toLocalDate()

    /** The UTC instant, in epoch ms, at which local [day] starts in [zone]. */
    fun startOfDay(day: LocalDate, zone: ZoneId): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * The columns to save for an existing note. With neither [pickedDay] nor [pickedTime], the
     * [stored] columns exactly as they are, whatever zone the note was saved in. Otherwise the
     * picked day (or the shown one) at the picked time (or the shown one), converted in [zone].
     */
    fun edited(stored: StoredNoteTimes, pickedDay: LocalDate?, pickedTime: LocalTime?, zone: ZoneId): StoredNoteTimes {
        if (pickedDay == null && pickedTime == null) return stored
        val shown = shown(stored.createDateTime, zone)
        return toStored(pickedDay ?: shown.toLocalDate(), pickedTime ?: shown.toLocalTime(), zone)
    }
}
