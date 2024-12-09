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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package org.gnoseis.ui.organization

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.constants.TITLE_LENGTH_ORGANIZATION
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.theme.GnoseisTheme

object OrganizationEditDestination : NavigationDestination {
    override val route = "organization_edit"
    override val titleRes = -9
    const val organizationIdArg = "organizationId"
    val routeWithArgs = "${route}/{$organizationIdArg}"
}
@Composable
fun OrganizationEditPage(
    pageViewModel: OrganizationEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    ) {
    val coroutineScope = rememberCoroutineScope()
    val pageState = pageViewModel.organizationEditPageState

    OrganizationEditScaffold(
        pageState = pageState,
        onEvent = {
            when (it) {
                is OrganizationEditPageEvent.Save -> {
                    coroutineScope.launch {
                        val addedOrganizationtId = pageViewModel.onSave()
                        if (addedOrganizationtId != null) {
                            navigateToOrganizationDetailsPage(addedOrganizationtId)
                        } else {
                            navMenuClick()
                        }
                    }
                }
                else -> {
                    pageViewModel.onEvent(it)
                }
            }
        },
        onNavMenuclick = navMenuClick,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationEditScaffold(
    pageState: OrganizationEditViewModel.OrganizationEditPageState,
    onEvent: (OrganizationEditPageEvent) -> Unit,
    onNavMenuclick: () -> Unit,


    ){
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            // TODO: Do something
            delay(1500)
            pullRefreshState.endRefresh()
        }
    }
    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text(text="New Organization") },
                navigationIcon = {
                    IconButton(onClick = { onNavMenuclick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
//                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onEvent(OrganizationEditPageEvent.Save)
                            onNavMenuclick()
                        },
                        enabled = pageState.isValid

                    ) {
                        Text(text = "Save")
                        }
                }
            )
        },
        floatingActionButton = {},
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                OrganizationEditBody(
                    pageState = pageState,
                    onEvent = onEvent,
                )
            }
        }
    )
}

@Composable
fun OrganizationEditBody(
    pageState: OrganizationEditViewModel.OrganizationEditPageState,
    onEvent: (OrganizationEditPageEvent) -> Unit,
){
    Column(
       modifier = Modifier
           .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
           .verticalScroll(rememberScrollState())
    ){

        OrganizationTextField(
            label = "Organization Name",
            value = pageState.organizationName?:"" ,
            onValueChanged = {
                if(it.length <= TITLE_LENGTH_ORGANIZATION)
                onEvent(OrganizationEditPageEvent.OrganizationNameChanged(it))},
            type = TextFieldType.WORD)

        Spacer(modifier = Modifier.height(10.dp))

         OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            label = {Text(text="Organization Description")},
            value = pageState.organizationDescription?:"",
            onValueChange = {
                onEvent(OrganizationEditPageEvent.OrganizationDescriptionChanged(it))
            },
            singleLine = false,
            minLines = 10
        )

    }
}

enum class TextFieldType {
    WORD,
    EMAIL,
    PHONE
}
@Composable
fun OrganizationTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    type: TextFieldType
){
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = {Text(text=label)},
        value = value,
        onValueChange = {onValueChanged(it) },
        singleLine = true,
        keyboardOptions =
        when (type) {
            TextFieldType.WORD -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text
                )
            }
            TextFieldType.EMAIL -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email
                )
            }
            TextFieldType.PHONE -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Phone
                )
            }
        },
//            isError = pageState.phoneMainValidationResult?.successful  != null
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ContactEditPagePreview()
{
    GnoseisTheme {
        Surface {
            OrganizationEditScaffold(
                pageState = OrganizationEditViewModel.OrganizationEditPageState(
                    isValid = false,
                    organizationName = "Personal"
                ),
                onEvent = {},
                onNavMenuclick = {},
            )
        }
    }
}