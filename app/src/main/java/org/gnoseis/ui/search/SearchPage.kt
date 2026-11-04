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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.enums.RecordType

/**
 * The Search page. [scopeTypeId] is the record type id of the list page it was opened from;
 * null (from Home) searches all types.
 */
@Serializable
data class SearchRoute(val scopeTypeId: Int? = null)

@Composable
fun SearchPage(
    pageViewModel: SearchPageViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onBack: () -> Unit,
    navigateToRecordDetailsPage: (RecordType, String) -> Unit,
    navigateToNewRecordPage: (type: RecordType, name: String) -> Unit,
) {
    val state by pageViewModel.uiState.collectAsState()
    val search = pageViewModel.search

    // Kept in the view model, so coming back from a record shows the same place in the list
    val listState = rememberLazyListState(search.scroll.index, search.scroll.offset)
    DisposableEffect(listState) {
        onDispose { search.scroll = SearchScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        RecordSearch(
            state = state,
            onQueryChange = pageViewModel::updateCurrentQuery,
            onChipClick = pageViewModel::toggleChip,
            onResultClick = { navigateToRecordDetailsPage(it.recordType, it.recordId) },
            onCreate = { navigateToNewRecordPage(it, state.query.trim()) },
            leading = SearchFieldLeading.Back,
            onBack = onBack,
            listState = listState,
            // The page draws behind the system bars: keep the field below the status bar and
            // the results above the navigation bar and the keyboard
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
