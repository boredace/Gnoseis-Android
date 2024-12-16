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

package org.gnoseis.ui.settings

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupSettingsPageRoute(
        val dummy: Boolean? = false
)

private const val TAG = "BACKUP"

@Composable
fun BackupSettingsPage(
    pageViewModel: BackupSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    onImportClicked: () -> Unit,
    ) {

    val exportSuccess = pageViewModel.exportuccess.value

    val context = LocalContext.current
    val activity = context as? Activity

    BackupSettingsScaffold(
        onNavMenuClick = onNavMenuClick,
        onExportClicked = {
            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format((Date()))
            pageViewModel.backupDatabase("/storage/emulated/0/Download/gnoseis_data_${dateStamp}.backup")
        },
        onImportClicked = onImportClicked,
        exportSuccess = exportSuccess,
        onExportCompleteConfirm = {
            Log.i(TAG, "Closing app")
            activity?.finish()
        }

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScaffold(
    onNavMenuClick: () -> Unit,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    exportSuccess: Boolean,
    onExportCompleteConfirm: () -> Unit,

    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Import / Export Database") },
                navigationIcon = {
                    IconButton(onClick = { onNavMenuClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {},
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                BackupSettingsBody(
                    onExportClicked = onExportClicked,
                    onImportClicked = onImportClicked,
                    exportSuccess = exportSuccess,
                    onExportCompleteConfirm =  onExportCompleteConfirm,
                )
            }
        }
    )
}


@Composable
fun BackupSettingsBody(
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    exportSuccess: Boolean,
    onExportCompleteConfirm: () -> Unit,

    ) {

    if(exportSuccess) {
        BackupCompleteDialog( onConfirm = onExportCompleteConfirm )
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Export Database", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Pressing Export will create a copy of the current SQLite application database in your Documents folder. Currently backup file is not encrypted.")
        }

        Row(
            modifier = Modifier
                .padding(bottom = 12.dp, top = 0.dp)
        ) {
            Text(text = "Once exported application will need to be restarted.")
        }

        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onExportClicked() }
            ) { Text(text = "Export") }
        }


        Spacer(modifier = Modifier.padding(12.dp))

        /*HorizontalDivider(
            modifier = Modifier.padding(12.dp)
        )*/

        Row(
            modifier = Modifier
                .padding(
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {
            Text(text = "Import Database", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "You can import existing backup file to replace database currently used by the application.")
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImportClicked() }
            ) { Text(text = "Import") }
        }
    }
}


@Composable
fun BackupCompleteDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = {
            Column(){
                Row(){
                    Text(text = "Database file exported successfully to your Downloads " +
                            "folder. Application needs to be restarted.")
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(text="Exit the Application")
            }
        },
        title = { Text(text="Export Successful") },
        icon =  { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) },
    )

}



@Preview
@Composable
fun BackupSettingsPagePreview(
) {
    BackupSettingsScaffold(
        onNavMenuClick = {},
        onExportClicked = {},
        onImportClicked = {},
        onExportCompleteConfirm = {},
        exportSuccess = false
    )
}