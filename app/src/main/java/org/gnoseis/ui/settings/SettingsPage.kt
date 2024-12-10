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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.serialization.Serializable
import org.gnoseis.R
import org.gnoseis.ui.components.ListItem
import org.gnoseis.ui.components.SimpleList
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class SettingsPageRoute(
    val dummy: Boolean? = false
)

@Composable
fun SettingsPage(
    onNavMenuclick: () -> Unit,
    listItemClicked: (Int) -> Unit,

){
    SettingsPageScaffold(
        onNavMenuClick = onNavMenuclick,
        listItemClicked = listItemClicked,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageScaffold(
    onNavMenuClick: () -> Unit,
    listItemClicked: (Int) -> Unit,
){
//    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text="Settings") },
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
//                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                SettingsPageBody(
                    listItemClicked = listItemClicked,
                )
            }
        }
    )
}




@Composable
fun SettingsPageBody(
    listItemClicked: (Int) -> Unit,
){
    Column(){
        SimpleList(
            listOf(
                ListItem(0, "Import / Export", ImageVector.vectorResource(R.drawable.outline_archive_24),),
            ),
            listItemClicked = { listItemClicked(it) }
        )
    }
}



@Preview
@Composable
fun SettingsPagePreview()
{
    GnoseisTheme {
        Surface {
            SettingsPageScaffold(
                onNavMenuClick = {},
                listItemClicked = {}
            )
        }
    }
}