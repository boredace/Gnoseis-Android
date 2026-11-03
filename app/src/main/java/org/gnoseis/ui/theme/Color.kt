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
 * Colours for each record type: an outline (also the icon and legend dot) on a light fill in the
 * light theme, and a light outline on a dark fill in the dark one, so the types differ in
 * lightness as well as hue. Lightness also differs a little between types, to keep them apart
 * where hue alone can't, also with night light on. Every outline is at least 5:1 against its
 * fill. Fixed colours: they don't follow dynamic colour.
 */
data class RecordTypeColors(val outline: Color, val fill: Color)

val NoteColorsLight = RecordTypeColors(outline = Color(0xFF38641C), fill = Color(0xFFDBEFCB))
val ContactColorsLight = RecordTypeColors(outline = Color(0xFF7C560D), fill = Color(0xFFFFE7CA))
val OrganizationColorsLight = RecordTypeColors(outline = Color(0xFF96333F), fill = Color(0xFFFFCECF))
val CategoryColorsLight = RecordTypeColors(outline = Color(0xFF235198), fill = Color(0xFFD0D9FF))
val ItemColorsLight = RecordTypeColors(outline = Color(0xFF00646B), fill = Color(0xFFAFECF3))

val NoteColorsDark = RecordTypeColors(outline = Color(0xFFAACD90), fill = Color(0xFF324D21))
val ContactColorsDark = RecordTypeColors(outline = Color(0xFFE3BA82), fill = Color(0xFF553C12))
val OrganizationColorsDark = RecordTypeColors(outline = Color(0xFFFFAAAD), fill = Color(0xFF6B2D32))
val CategoryColorsDark = RecordTypeColors(outline = Color(0xFFACBFFF), fill = Color(0xFF293E6C))
val ItemColorsDark = RecordTypeColors(outline = Color(0xFF62DEEA), fill = Color(0xFF004349))
