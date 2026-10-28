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

package org.gnoseis.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
/**
 * #45: record type colours for the Home page's Most linked bubbles: an outline (also the icon)
 * on a lighter fill in the light theme, and a lighter outline on a darker fill in the dark one,
 * so the types differ in lightness as well as hue.
 */
data class RecordTypeColors(val outline: Color, val fill: Color)

val ContactColorsLight = RecordTypeColors(outline = Color(0xFF4A5C92), fill = Color(0xFFDBE1FF))
val OrganizationColorsLight = RecordTypeColors(outline = Color(0xFF8A5A00), fill = Color(0xFFFFDDB3))
val CategoryColorsLight = RecordTypeColors(outline = Color(0xFF3A6A3E), fill = Color(0xFFCDEBC6))
val ItemColorsLight = RecordTypeColors(outline = Color(0xFF7B4E91), fill = Color(0xFFF3DAFF))

val ContactColorsDark = RecordTypeColors(outline = Color(0xFFB4C5FF), fill = Color(0xFF31447A))
val OrganizationColorsDark = RecordTypeColors(outline = Color(0xFFFFB951), fill = Color(0xFF633F00))
val CategoryColorsDark = RecordTypeColors(outline = Color(0xFFA0D49A), fill = Color(0xFF225128))
val ItemColorsDark = RecordTypeColors(outline = Color(0xFFE7B4FF), fill = Color(0xFF613777))
