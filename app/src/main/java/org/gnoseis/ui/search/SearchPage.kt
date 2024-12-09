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

package org.gnoseis.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.navigation.NavigationDestination


object SearchPageDestination : NavigationDestination {
    override val route = "search_page"
    override val titleRes = -9
}

@Composable
fun SearchPage(
    pageViewModel: SearchPageViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    ) {
    val results by pageViewModel.searchResults.collectAsState()

    var textTitle by remember { mutableStateOf("") }
    var searchBoxValue = MutableStateFlow("")

    Column(
        modifier = Modifier.padding(10.dp)
    ) {

        Text(text = "Results: ${results.size}")
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            //            .focusRequester(focusRequester),
            label = { Text(text = "Search All Records") },
            value = textTitle,
            onValueChange = {
                textTitle = it
                searchBoxValue.value = textTitle
                if(textTitle.length >= 3)
                    pageViewModel.updateCurrentQuery(it)
            },
            singleLine = true,
        )

        if(textTitle.length >= 3) {
            SearchResultsList(
                results = results,
                onResultClick = {
                    when (it.recordTypeId) {
                        RecordType.Category.recordTypeId -> navigateToCategoryDetailsPage(it.recordId)
                        RecordType.Contact.recordTypeId -> navigateToContactDetailsPage(it.recordId)
                        RecordType.Item.recordTypeId -> navigateToItemDetailsPage(it.recordId)
                        RecordType.Note.recordTypeId -> navigateToNoteDetailsPage(it.recordId)
                        RecordType.Organization.recordTypeId -> navigateToOrganizationDetailsPage(it.recordId)
                    }
                }
            )
        }
    }
}


@Preview
@Composable
fun SearchPagePreview(){
    SearchPage(
        navigateToCategoryDetailsPage = {},
        navigateToContactDetailsPage = {},
        navigateToItemDetailsPage = {},
        navigateToNoteDetailsPage = {},
        navigateToOrganizationDetailsPage = {},
    )
}