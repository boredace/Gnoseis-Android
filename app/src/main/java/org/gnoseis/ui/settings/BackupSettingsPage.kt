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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.graphics.Color
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


@Composable
fun BackupSettingsPage(
    pageViewModel: BackupSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    onImportClicked: () -> Unit,
    ) {



    val exportSuccess = pageViewModel.exportuccess.value
    val importSuccess = pageViewModel.importuccess.value
    val exportFailed = pageViewModel.exportFailed.value
    val importFailed = pageViewModel.importFailed.value

    // #32: the user chooses the folder and file name of the backup
    val exportLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        // uri is null when the user closes the file picker without choosing a location
        uri?.let { pageViewModel.exportDatabase(it) }
    }

    // Create a file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // uri is null when the user closes the file picker without choosing a file
        uri?.let { pageViewModel.importDatabase(it) }
    }

    BackupSettingsScaffold(
        onNavMenuClick = onNavMenuClick,
        onExportClicked = {
            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format((Date()))
            exportLocationLauncher.launch("gnoseis_data_${dateStamp}.backup")
        },
        onImportClicked = {
            filePickerLauncher.launch(arrayOf("*/*")) // Use a MIME type like "image/*" for specific files
        },
        exportSuccess = exportSuccess,
        importSuccess = importSuccess,
        exportFailed = exportFailed,
        importFailed = importFailed,
        onResultConfirm = { pageViewModel.dismissResult() },

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScaffold(
    onNavMenuClick: () -> Unit,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    exportSuccess: Boolean,
    importSuccess: Boolean,
    exportFailed: Boolean,
    importFailed: Boolean,
    onResultConfirm: () -> Unit,

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
                    importSuccess = importSuccess,
                    exportFailed = exportFailed,
                    importFailed = importFailed,
                    onResultConfirm = onResultConfirm,
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
    importSuccess: Boolean,
    exportFailed: Boolean,
    importFailed: Boolean,
    onResultConfirm: () -> Unit,

    ) {

    if(exportSuccess) {
        ExportCompleteDialog( onConfirm = onResultConfirm )
    }
    if(importSuccess) {
        ImportCompleteDialog( onConfirm = onResultConfirm )
    }
    if(exportFailed) {
        BackupFailedDialog(
            title = "Export Failed",
            message = "The backup file could not be written to the selected location.",
            onConfirm = onResultConfirm,
        )
    }
    if(importFailed) {
        BackupFailedDialog(
            title = "Import Failed",
            message = "The selected file could not be read, is not a Gnoseis backup, or comes " +
                    "from a newer version of the app. Your existing data was not changed.",
            onConfirm = onResultConfirm,
        )
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
            Text(text = "Pressing Export will let you choose where to save a copy of the current SQLite application database. Currently backup file is not encrypted, so anyone with access to that location can read your data.")
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

        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Warning: All existing data in your application will be replaced with the data from imported file. Ensure you have exported existing database using the Export function above before proceeding.", color = Color.Red)
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
fun ExportCompleteDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = {
            Column(){
                Row(){
                    Text(text = "Database file exported successfully to the selected location.")
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(text="OK")
            }
        },
        title = { Text(text="Export Successful") },
        icon =  { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) },
    )

}


@Composable
fun ImportCompleteDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = {
            Column(){
                Row(){
                    Text(text = "Database file imported successfully and replaced the old data.")
                }
            }
        },
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(text="OK")
            }
        },
        title = { Text(text="Import Successful") },
        icon =  { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) },
    )

}


@Composable
fun BackupFailedDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = { Text(text = message) },
        onDismissRequest = { onConfirm() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(text="OK")
            }
        },
        title = { Text(text = title) },
        icon =  { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
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
        onResultConfirm = {},
        exportSuccess = false,
        importSuccess = false,
        exportFailed = false,
        importFailed = false,
    )
}