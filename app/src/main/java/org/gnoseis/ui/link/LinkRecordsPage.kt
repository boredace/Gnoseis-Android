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

package org.gnoseis.ui.link

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.search.SearchResultsList

object LinkRecordsDestination : NavigationDestination {
    override val route = "link_records"
    override val titleRes = -9
    const val sourceRecordIdArg = "sourceRecordId"
    const val sourceRecordTypeIdArg = "sourceRecordTypeId"
    val routeWithArgs = "$route/{$sourceRecordIdArg}/{$sourceRecordTypeIdArg}"
}

@Composable
fun LinkRecordsPage(
    pageViewModel: LinkRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,

    ) {
    val results by pageViewModel.searchResults.collectAsState()
    val sourceRecordId by pageViewModel.sourceRecordId.collectAsState()
    val sourceRecordTypeId by pageViewModel.sourceRecordTypeId.collectAsState()
    val selectedCount by pageViewModel.selectedCount.asIntState()
    val coroutineScope = rememberCoroutineScope()

//    LaunchedEffect(true) {
//        pageViewModel.updateCurrentQuery("walk")
//    }

    LinkRecordsScaffold(
        results = results,
        onSearchBoxValueChanged = {
            Log.i(LinkRecordsDestination.route,"onSearchBoxValueChanged value = ${it}")
            pageViewModel.updateCurrentQuery(it)
        },
        onNavMenuClick = onNavMenuClick,
        onLinkSelectedResults = {
            coroutineScope.launch {
                pageViewModel.linkSelectedResults()
                onNavMenuClick()
            }
        },
        onResultClick = {pageViewModel.toggleSelectedSearchResult(it)},
        selectedCount = selectedCount
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkRecordsScaffold(
    results: List<SearchResult>,
    onNavMenuClick: () -> Unit,
    onLinkSelectedResults: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onSearchBoxValueChanged: (String) -> Unit,
    selectedCount: Int,


){
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text="Link Records")},
                navigationIcon = {
                    IconButton(onClick = { onNavMenuClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
//                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLinkSelectedResults() },
                        enabled = selectedCount > 0

                    ) {
                        Text(text = "Link")
                    }
                }
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                LinkRecordsBody(
                    results = results,
                    onSearchBoxValueChanged = onSearchBoxValueChanged,
                    onResultClick = onResultClick,
                    selectedCount = selectedCount
                )
            }

        }
    )
}

@Composable
fun LinkRecordsBody(
    results: List<SearchResult>,
    onSearchBoxValueChanged: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    selectedCount: Int


){
    var searchBoxValue = MutableStateFlow("")

    val showKeyboard = remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // LaunchedEffect prevents endless focus request
    LaunchedEffect(focusRequester) {
        if (showKeyboard.equals(true)) {
            focusRequester.requestFocus()
            delay(100) // Make sure you have delay here
            keyboard?.show()
        }
    }

    Column(
        modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
    ){
        var textTitle by remember { mutableStateOf("") }

        Text(text="Results: ${results.size}  Selected $selectedCount")
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = {Text(text="Search or Create Records")},
            value = textTitle,
            onValueChange = {
                    textTitle = it
                    searchBoxValue.value = textTitle
                    onSearchBoxValueChanged(it)
            },
            singleLine = true,
            )

        SearchResultsList(
            results = results,
            onResultClick = onResultClick,
//            onIncrementSelectedCount = {selectedCount++}

        )
    }
}





@Preview
@Composable
fun LinkdRecordsPreview()
{
    val results : List<SearchResult> = listOf(
        SearchResult(recordTypeId = 1, recordTitle = "This is my note I would like to link"),
        SearchResult(recordTypeId = 1, recordTitle = "Meeting with Jason smith"),
        SearchResult(recordTypeId = 2, recordTitle = "Smith, Jason"),

    )
    LinkRecordsScaffold(
        results = results,
        onNavMenuClick = {},
        onLinkSelectedResults = {},
        onResultClick = {},
        onSearchBoxValueChanged = {},
        selectedCount = 0)
}