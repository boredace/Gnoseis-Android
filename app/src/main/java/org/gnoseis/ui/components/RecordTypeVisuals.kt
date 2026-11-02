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

package org.gnoseis.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.theme.CategoryColorsDark
import org.gnoseis.ui.theme.CategoryColorsLight
import org.gnoseis.ui.theme.ContactColorsDark
import org.gnoseis.ui.theme.ContactColorsLight
import org.gnoseis.ui.theme.ItemColorsDark
import org.gnoseis.ui.theme.ItemColorsLight
import org.gnoseis.ui.theme.NoteColorsDark
import org.gnoseis.ui.theme.NoteColorsLight
import org.gnoseis.ui.theme.OrganizationColorsDark
import org.gnoseis.ui.theme.OrganizationColorsLight
import org.gnoseis.ui.theme.RecordTypeColors

/** A record type's icon, tinted with [colorFilter]. */
@Composable
fun RecordTypeIcon(type: RecordType, colorFilter: ColorFilter, size: Dp) {
    when (type) {
        RecordType.Contact -> ContactIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Organization -> OrganizationIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Category -> CategoryIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Item -> ItemIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Note -> NoteIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.LinkedRecord -> Icon(
            Icons.Filled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
fun recordTypeColors(type: RecordType): RecordTypeColors {
    val dark = isSystemInDarkTheme()
    return when (type) {
        RecordType.Contact -> if (dark) ContactColorsDark else ContactColorsLight
        RecordType.Organization -> if (dark) OrganizationColorsDark else OrganizationColorsLight
        RecordType.Category -> if (dark) CategoryColorsDark else CategoryColorsLight
        RecordType.Note -> if (dark) NoteColorsDark else NoteColorsLight
        else -> if (dark) ItemColorsDark else ItemColorsLight
    }
}
