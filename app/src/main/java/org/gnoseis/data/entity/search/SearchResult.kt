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


package org.gnoseis.data.entity.search

import java.util.UUID

/**
 * A record as search shows it: its type and title, how many records it's linked to, and the
 * stored fields its context line is made of ([noteDate] for a note; [jobTitle] and [company]
 * for a contact). [updatedAt] is the system time of its last change, null if never stamped.
 */
data class SearchResult (
    val recordId: String = UUID.randomUUID().toString(),
    val recordTypeId: Int,
    val recordTitle: String,
    val linkCount: Int = 0,
    val noteDate: Long? = null,
    val jobTitle: String? = null,
    val company: String? = null,
    val updatedAt: Long? = null,
)
