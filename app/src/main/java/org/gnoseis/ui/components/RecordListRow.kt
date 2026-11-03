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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/**
 * A long press action on the rows of a record list (#11). Long press calls [onLongClick], which
 * should open [menu]; [menu] is drawn with each row, so a DropdownMenu in it is anchored to the
 * row. TalkBack users get [onAction] as the row's custom action "[label] <record name>".
 */
class RecordRowAction(
    val label: String,
    val onLongClick: (id: String) -> Unit,
    val onAction: (id: String) -> Unit,
    val menu: @Composable (id: String) -> Unit,
)

/**
 * A record list row: a card that opens the record on tap. With [rowAction], also long press
 * (with haptic feedback), its menu and its TalkBack action; without it, a plain clickable card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordListRow(
    id: String,
    name: String,
    onClick: () -> Unit,
    rowAction: RecordRowAction?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (rowAction == null) {
        Card(onClick = onClick, modifier = modifier, content = content)
    } else {
        val haptics = LocalHapticFeedback.current
        Box {
            Card(
                modifier = modifier
                    .clip(CardDefaults.shape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            rowAction.onLongClick(id)
                        },
                    )
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction("${rowAction.label} $name") {
                                rowAction.onAction(id)
                                true
                            }
                        )
                    },
                content = content,
            )
            rowAction.menu(id)
        }
    }
}
