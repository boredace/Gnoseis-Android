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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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


@Composable
fun BackupSettingsPage(
    pageViewModel: BackupSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    onImportClicked: () -> Unit,
    ) {



    val exportSuccess = pageViewModel.exportuccess.value
    val importSuccess = pageViewModel.importuccess.value

    val context = LocalContext.current
    val activity = context as? Activity

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Create a file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedFileUri = uri // Update the selected URI
        pageViewModel.importDatabase(selectedFileUri!!)
    }

    BackupSettingsScaffold(
        onNavMenuClick = onNavMenuClick,
        onExportClicked = {
            val dateStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format((Date()))
            pageViewModel.exportDatabase("/storage/emulated/0/Download/gnoseis_data_${dateStamp}.backup")
        },
        onImportClicked = {
            filePickerLauncher.launch(arrayOf("*/*")) // Use a MIME type like "image/*" for specific files
        },
        exportSuccess = exportSuccess,
        importSuccess = importSuccess,
        onExportCompleteConfirm = {
            activity?.finish()
        },
        onImportCompleteConfirm = {
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
    importSuccess: Boolean,
    onExportCompleteConfirm: () -> Unit,
    onImportCompleteConfirm: () -> Unit,

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
                    onExportCompleteConfirm = onExportCompleteConfirm,
                    onImportCompleteConfirm = onImportCompleteConfirm,
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
    onExportCompleteConfirm: () -> Unit,
    onImportCompleteConfirm: () -> Unit,

    ) {

    if(exportSuccess) {
        ExportCompleteDialog( onConfirm = onExportCompleteConfirm )
    }
    if(importSuccess) {
        ImportCompleteDialog( onConfirm = onImportCompleteConfirm )
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

        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Warning: All existing data in your application will be replaced with the data from imported file. Ensure you have exported existing database using the Export function above before proceeding.", color = Color.Red)
        }
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp, top = 0.dp)
        ) {
            Text(text = "Once imported application will need to be restarted.")
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


@Composable
fun ImportCompleteDialog(
    onConfirm: () -> Unit,
) {
    AlertDialog(
        text = {
            Column(){
                Row(){
                    Text(text = "Database file imported successfully and replaced the old database. " +
                            "Application needs to be restarted.")
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
        title = { Text(text="Import Successful") },
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
        onImportCompleteConfirm = {},
        exportSuccess = false,
        importSuccess = false,
    )
}