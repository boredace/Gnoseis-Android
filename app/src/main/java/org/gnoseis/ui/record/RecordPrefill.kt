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

package org.gnoseis.ui.record

import org.gnoseis.data.constants.TITLE_LENGTH_CONTACT

/**
 * #10: the name a new record starts with when it's created from a search: the trimmed
 * [query], cut to [maxLength]. Null when there's nothing to prefill.
 */
fun prefillName(query: String?, maxLength: Int): String? =
    query?.trim()?.take(maxLength)?.ifEmpty { null }

/** A new contact's names, prefilled from a search (#10). */
data class ContactNamePrefill(val nameFirst: String?, val nameLast: String)

/**
 * #10: one word is the last name (the required field); with more words, the last one is the
 * last name and the rest the first name. The last name is cut to its length limit.
 */
fun prefillContactName(query: String?): ContactNamePrefill? {
    val words = query?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    if (words.isEmpty()) return null
    return ContactNamePrefill(
        nameFirst = words.dropLast(1).joinToString(" ").ifEmpty { null },
        nameLast = words.last().take(TITLE_LENGTH_CONTACT),
    )
}
