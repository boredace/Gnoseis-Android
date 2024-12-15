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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider

@Serializable
data class BackupSettingsPageRoute(
        val dummy: Boolean? = false
)


@Composable
fun BackupSettingsPage(
    pageViewModel: BackupSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    ) {
    BackupSettingsScaffold(
        onNavMenuClick = onNavMenuClick,
        onExportClicked = {
            pageViewModel.backupDatabase("/storage/emulated/0/Download/gnoseis_data.backup")

        },
        onImportClicked = onImportClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScaffold(
    onNavMenuClick: () -> Unit,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
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
                )
            }
        }
    )
}


@Composable
fun BackupSettingsBody(
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Export Database ")
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
            Text(text = "Import Database ")
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImportClicked() }
            ) { Text(text = "Import") }
        }
    }
}




@Preview
@Composable
fun BackupSettingsPagePreview(
) {
    BackupSettingsScaffold(
        onNavMenuClick = {},
        onExportClicked = {},
        onImportClicked = {},
    )
}