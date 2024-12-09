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

package org.gnoseis.ui.item

import android.annotation.SuppressLint
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
import org.gnoseis.data.constants.TITLE_LENGTH_ITEM
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.theme.GnoseisTheme

object ItemEditDestination : NavigationDestination {
    override val route = "item_edit"
    override val titleRes = -9
    const val itemIdArg = "itemId"
    val routeWithArgs = "${route}/{$itemIdArg}"
}
@Composable
fun ItemEditPage(
    pageViewModel: ItemEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,

    ) {
    val coroutineScope = rememberCoroutineScope()
    val pageState = pageViewModel.itemEditPageState

    ItemEditScaffold(
        pageState = pageState,
        onEvent = { pageViewModel.onEvent(it) },
        onSave = {
            coroutineScope.launch {
                val addedItemId = pageViewModel.onSave()
                if(addedItemId != null) {
                    navigateToItemDetailsPage(addedItemId)
                } else {
                    navMenuClick()
                }
            }
        },
        onNavMenuclick = navMenuClick,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScaffold(
    pageState: ItemEditViewModel.ItemEditPageState,
    onEvent: (ItemEditPageEvent) -> Unit,
    onSave: () -> Unit,
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
                title = { Text(text="New Item") },
                navigationIcon = {
                    IconButton(onClick = { onNavMenuclick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
//                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSave() },
                        enabled = pageState.isValid

                    ) {
                        when {
                            pageState.isNew -> Text(text = "Add")
                            !pageState.isNew -> Text(text= "Save")
                        }
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
                ItemEditBody(
                    pageState = pageState,
                    onEvent = onEvent,
                )
            }
        }
    )
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun ItemEditBody(
    pageState: ItemEditViewModel.ItemEditPageState,
    onEvent: (ItemEditPageEvent) -> Unit,
){
    Column(
       modifier = Modifier
           .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
           .verticalScroll(rememberScrollState())
    ){

        ItemTextField(
            label = "Item Name",
            value = pageState.editItemName?:"" ,
            onValueChanged = {
                if(it.length <= TITLE_LENGTH_ITEM)
                    onEvent(ItemEditPageEvent.ItemNameChanged(it))},
            type = TextFieldType.WORD)

        Spacer(modifier = Modifier.height(10.dp))

         OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            label = {Text(text="Item Description")},
            value = pageState.editItemDescription?:"",
            onValueChange = {
                onEvent(ItemEditPageEvent.ItemDescriptionChanged(it))
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
fun ItemTextField(
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
            ItemEditScaffold(
                pageState = ItemEditViewModel.ItemEditPageState(
                    isValid = false,
                    editItemName = "Personal"
                ),
                onEvent = {},
                onSave = {},
                onNavMenuclick = {},
            )
        }
    }
}