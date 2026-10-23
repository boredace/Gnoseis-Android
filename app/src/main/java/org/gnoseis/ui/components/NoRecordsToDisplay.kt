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
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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

package org.gnoseis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon

@Composable
fun NoRecordsToDisplay(
    recordType: RecordType,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var text: String = ""
            Row() {
                val size: Dp = 72.dp
                when(recordType) {
                    RecordType.Item -> {
                        ItemIcon(
                            width = size,
                            height = size,
                        )
                        text = "No Items to display"
                    }
                    RecordType.Category -> {
                        CategoryIcon(
                            width = size,
                            height = size,
                        )
                        text = "No Categories to display"
                    }
                    RecordType.Contact -> {
                        ContactIcon(
                            width = size,
                            height = size,
                        )
                        text = "No Contacts to display"
                    }
                    RecordType.Organization -> {
                        OrganizationIcon(
                            width = size,
                            height = size,
                        )
                        text = "No Organizations to display"
                    }
                    RecordType.Note -> {
                        NoteIcon(
                            width = size,
                            height = size,
                        )
                        text = "No Notes to display"
                    }
                    else -> {}
                }
            }
            Row {
                Text(text = text)
            }
        }
    }
}
