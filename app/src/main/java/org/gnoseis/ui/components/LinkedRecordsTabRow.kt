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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import org.gnoseis.data.entity.links.LinkedRecordTypeCount

class LinkedRecordsTabRow ()
{}

data class TabItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * The selected tab of a Details page, kept as a linked record type id (null = the Details tab)
 * rather than a tab index: tabs are sorted by count, so an index can end up on another type.
 * Saveable, so it survives opening a linked record and coming back.
 *
 * #6: when [linkedNewRecordTypeId] is set because a new record of that type was just linked,
 * its tab is selected and [onLinkedNewRecordTypeShown] is called, so this happens only once.
 */
@Composable
fun rememberSelectedLinkedRecordType(
    linkedNewRecordTypeId: Int?,
    onLinkedNewRecordTypeShown: () -> Unit,
): MutableState<Int?> {
    val selected = rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(linkedNewRecordTypeId) {
        if (linkedNewRecordTypeId != null) {
            selected.value = linkedNewRecordTypeId
            onLinkedNewRecordTypeShown()
        }
    }
    return selected
}

/**
 * Index in a Details page's tab row (Details tab first) of the tab for [selectedRecordTypeId].
 * The Details tab (0) if that type has no tab, e.g. while a just-linked record's tab loads.
 */
fun tabIndexOf(linkedRecordTabs: List<LinkedRecordTypeCount>, selectedRecordTypeId: Int?): Int =
    linkedRecordTabs.indexOfFirst { it.recordTypeId == selectedRecordTypeId } + 1

