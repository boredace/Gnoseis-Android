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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.home.TypeLinkCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.pluralName

/**
 * The Most linked type picker, on Home and on the full Most linked list: a chip showing the
 * current type ([typeId], or null for all types) that opens a menu of the [types] that have links.
 */
@Composable
fun RecordTypePicker(
    typeId: Int?,
    types: List<TypeLinkCount>,
    onTypeChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    // The selection the menu shows. Set when it opens and left alone while it closes, so the menu
    // doesn't change (or widen for the check column) during its exit animation.
    var menuTypeId by remember { mutableStateOf(typeId) }
    val filterType = typeId?.let { RecordType.from(it) }
    val current = filterType?.let { pluralName(it) } ?: "All types"
    Box(modifier) {
        // Laid out at its visible height, so the header row doesn't get the 48 dp touch-target padding
        // and leave a gap above the subtitle. Taps near the chip still reach it: Compose extends the
        // touch area of small clickable elements to the minimum touch target size.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FilterChip(
                selected = filterType != null,
                onClick = {
                    menuTypeId = typeId
                    expanded = true
                },
                label = { Text(current) },
                leadingIcon = filterType?.let { type ->
                    { RecordTypeIcon(type, ColorFilter.tint(recordTypeColors(type).outline), FilterChipDefaults.IconSize) }
                },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                },
                modifier = Modifier.semantics { contentDescription = "Filter by record type, $current selected" },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All types") },
                onClick = {
                    expanded = false
                    onTypeChange(null)
                },
                leadingIcon = { Icon(Icons.Outlined.BubbleChart, contentDescription = null) },
                trailingIcon = if (menuTypeId == null) {
                    { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                } else null,
            )
            HorizontalDivider()
            types.forEach { typeCount ->
                val type = RecordType.from(typeCount.recordTypeId)
                DropdownMenuItem(
                    text = {
                        // Menu rows are at most 280 dp wide. The count is measured first and never
                        // wraps; at very large font sizes the name gets an ellipsis instead.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pluralName(type),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                            )
                            Text(
                                text = linkCountText(typeCount.linkCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onTypeChange(typeCount.recordTypeId)
                    },
                    leadingIcon = { RecordTypeIcon(type, ColorFilter.tint(recordTypeColors(type).outline), 24.dp) },
                    // The check column is only kept while a type is picked, so with All types picked
                    // the counts end at the menu's edge instead of before an empty column.
                    trailingIcon = if (menuTypeId != null) {
                        { CurrentCheck(menuTypeId == typeCount.recordTypeId) }
                    } else null,
                )
            }
        }
    }
}

/** The check on the picked type's row of the type picker menu; keeps its space on the other type rows so their counts line up. */
@Composable
private fun CurrentCheck(current: Boolean) {
    if (current) {
        Icon(Icons.Filled.Check, contentDescription = "Selected")
    } else {
        Spacer(Modifier.size(24.dp))
    }
}

/** A link count as Most linked shows it: "No links", "1 link", "N links". */
internal fun linkCountText(count: Int) = when (count) {
    0 -> "No links"
    1 -> "1 link"
    else -> "$count links"
}
