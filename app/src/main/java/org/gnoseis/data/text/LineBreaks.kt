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

package org.gnoseis.data.text

/**
 * [text] with every line break as a line feed (#67): CRLF and a lone CR each become one LF.
 * Nothing else changes. Stored text can hold any of the three, since other clients have saved
 * CR and CRLF; pages show the text normalised, and saved text has LF only.
 */
fun normalizeLineBreaks(text: String): String =
    if (text.indexOf('\r') < 0) text else text.replace("\r\n", "\n").replace('\r', '\n')

/**
 * The text to save for an edited multi-line field (#67): [edited] with line feeds only. When
 * [edited] equals the [stored] text once both are normalised, the result is [stored] exactly as
 * it is, so saving a record without touching its text isn't a change, whatever line breaks the
 * stored text has. [stored] is null for a new record.
 */
fun lineBreaksToSave(edited: String?, stored: String?): String? {
    val normalized = edited?.let(::normalizeLineBreaks)
    return if (stored != null && normalized == normalizeLineBreaks(stored)) stored else normalized
}
