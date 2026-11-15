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

package org.gnoseis.ui.note

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.gnoseis.testutil.withDefaultZone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** #68: the note edit page's date picker values and time text. */
@RunWith(AndroidJUnit4::class)
class NoteDateTimeTextTest {
    /** Material's DatePicker uses UTC midnight; the day must not shift east or west of UTC. */
    @Test
    fun datePicker_roundTripsTheLocalDay_inZonesEastAndWestOfUtc() {
        val day = LocalDate.parse("2026-11-14")
        for (zone in listOf("Asia/Tokyo", "America/Toronto", "Pacific/Kiritimati", "Pacific/Pago_Pago")) {
            withDefaultZone(zone) {
                assertEquals(zone, 1794614400000, datePickerMillis(day))
                assertEquals(zone, day, datePickerDay(datePickerMillis(day)))
            }
        }
    }

    @Test
    fun noteTimeText_followsThe12Or24HourSetting() {
        assertEquals("00:00", noteTimeText(LocalTime.MIDNIGHT, is24Hour = true, Locale.US))
        assertEquals("14:30", noteTimeText(LocalTime.parse("14:30"), is24Hour = true, Locale.US))
        assertEquals("12:00 AM", noteTimeText(LocalTime.MIDNIGHT, is24Hour = false, Locale.US).replace(' ', ' '))
        assertEquals("2:30 PM", noteTimeText(LocalTime.parse("14:30"), is24Hour = false, Locale.US).replace(' ', ' '))
    }
}
