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

package org.gnoseis.ui.record

import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.text.normalizeLineBreaks

/*
 * A record's multi-line text as pages and list rows show it (#67): the stored text with every
 * line break as a line feed. Text saved by another client can hold CR or CRLF line breaks, and
 * a lone CR isn't drawn as a line break. The stored text itself is left as it is.
 */

val Note.displayText: String?
    get() = textContents?.let(::normalizeLineBreaks)

val Contact.displayComments: String?
    get() = comments?.let(::normalizeLineBreaks)

val Organization.displayComments: String?
    get() = comments?.let(::normalizeLineBreaks)

val Category.displayComments: String?
    get() = comments?.let(::normalizeLineBreaks)

val Item.displayComments: String?
    get() = comments?.let(::normalizeLineBreaks)
