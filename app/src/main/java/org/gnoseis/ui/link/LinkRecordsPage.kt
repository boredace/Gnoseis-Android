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

import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.CreateFromSearch
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.search.SearchResultsList

@Serializable
data class LinkRecordsRoute(
    val sourceRecordId: String,
    val sourceRecordType: RecordType,
)

@Composable
fun LinkRecordsPage(
    pageViewModel: LinkRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    navigateToNewLinkedRecordPage: (type: RecordType, name: String) -> Unit,
    ) {
    val results by pageViewModel.searchResults.collectAsState()
    val query by pageViewModel.currentQuery.collectAsState()
    val createFromSearch by pageViewModel.createFromSearch.collectAsState()
    val sourceRecordId by pageViewModel.sourceRecordId.collectAsState()
    val sourceRecordTypeId by pageViewModel.sourceRecordTypeId.collectAsState()
    val selectedCount by pageViewModel.selectedCount.asIntState()
    val coroutineScope = rememberCoroutineScope()

//    LaunchedEffect(true) {
//        pageViewModel.updateCurrentQuery("walk")
//    }

    LinkRecordsScaffold(
        results = results,
        query = query,
        // #10: only once the query has settled, so the offer doesn't flash while typing
        createFromSearchOffer = if (createFromSearch.query == query) createFromSearch.offer else CreateFromSearchOffer.None,
        createFromSearchTypes = pageViewModel.createFromSearchTypes,
        onCreateFromSearch = { navigateToNewLinkedRecordPage(it, query.trim()) },
        onSearchBoxValueChanged = {
            Log.i("LinkRecords","onSearchBoxValueChanged value = ${it}")
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
    query: String,
    createFromSearchOffer: CreateFromSearchOffer,
    createFromSearchTypes: List<RecordType>,
    onCreateFromSearch: (RecordType) -> Unit,
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
                    query = query,
                    createFromSearchOffer = createFromSearchOffer,
                    createFromSearchTypes = createFromSearchTypes,
                    onCreateFromSearch = onCreateFromSearch,
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
    query: String,
    createFromSearchOffer: CreateFromSearchOffer,
    createFromSearchTypes: List<RecordType>,
    onCreateFromSearch: (RecordType) -> Unit,
    onSearchBoxValueChanged: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    selectedCount: Int


){
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val createBlock = @Composable {
        CreateFromSearch(
            query = query,
            offer = createFromSearchOffer,
            types = createFromSearchTypes,
            onCreate = onCreateFromSearch,
        )
    }

    // focus the search box and open the keyboard when the page opens empty
    LaunchedEffect(Unit) {
        if (query.isEmpty()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
    ){

        Text(text="Results: ${results.size}  Selected $selectedCount")
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = {Text(text="Search or Create Records")},
            value = query,
            onValueChange = { onSearchBoxValueChanged(it) },
            singleLine = true,
            )

        if (createFromSearchOffer == CreateFromSearchOffer.NoResults) {
            createBlock()
        } else {
            SearchResultsList(
                results = results,
                onResultClick = onResultClick,
                footer = if (createFromSearchOffer == CreateFromSearchOffer.AfterResults) createBlock else null,
            )
        }
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
        query = "",
        createFromSearchOffer = CreateFromSearchOffer.None,
        createFromSearchTypes = emptyList(),
        onCreateFromSearch = {},
        onNavMenuClick = {},
        onLinkSelectedResults = {},
        onResultClick = {},
        onSearchBoxValueChanged = {},
        selectedCount = 0)
}