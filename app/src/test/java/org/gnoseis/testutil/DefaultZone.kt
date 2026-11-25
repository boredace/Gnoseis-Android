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

package org.gnoseis.testutil

import java.time.ZoneId
import java.util.TimeZone

/**
 * Runs [block] with the JVM's default time zone, which `ZoneId.systemDefault()` reads, set to
 * [zoneId] (#68), then puts the previous one back.
 */
inline fun <T> withDefaultZone(zoneId: String, block: () -> T): T {
    val previous = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zoneId)))
    try {
        return block()
    } finally {
        TimeZone.setDefault(previous)
    }
}
