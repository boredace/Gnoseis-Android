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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeleteRecordAlertDialog(
    recordTypeName: String,
    linkedRecordCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        text = {
            Column(){
                Row(){
                    Text(text = "This $recordTypeName will be permanently deleted.")
                }
                if(linkedRecordCount == 1) {
                    Row(modifier = Modifier.padding(top = 10.dp)){
                        Text(text = "Link to $linkedRecordCount record linked to this $recordTypeName will be removed. Linked record itself will not be deleted.")
                    }
                }
                if(linkedRecordCount > 1) {
                    Row(modifier = Modifier.padding(top = 10.dp)){
                        Text(text = "Links to $linkedRecordCount records linked to this $recordTypeName will be removed. Linked records themselves will not be deleted.")
                    }
                }
            }
        },
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
            ){
                Text(text="Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(text="Delete")
            }
        },
        title = { Text(text="Delete this $recordTypeName") },
        icon =  { Icon(Icons.Outlined.Delete, contentDescription = null) },
//        iconContentColor = MaterialTheme.colorScheme.error
    )

}