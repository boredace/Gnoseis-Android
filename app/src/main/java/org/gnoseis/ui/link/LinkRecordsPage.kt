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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.ui.search.RecordSearch
import org.gnoseis.ui.search.ResultRow
import org.gnoseis.ui.search.SearchFieldLeading
import org.gnoseis.ui.search.SearchScroll
import org.gnoseis.ui.search.SearchSelection
import org.gnoseis.ui.search.SearchUiState
import org.gnoseis.ui.search.TypeAvatar
import org.gnoseis.ui.search.recordType
import org.gnoseis.ui.search.searchChipTypes
import org.gnoseis.ui.search.searchContextLine
import org.gnoseis.ui.search.searchResultsView
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class LinkRecordsRoute(
    val sourceRecordId: String,
    val sourceRecordType: RecordType,
)

@Composable
fun LinkRecordsPage(
    pageViewModel: LinkRecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    navigateToNewLinkedRecordPage: (type: RecordType, name: String, alsoLink: List<Pair<String, RecordType>>) -> Unit,
) {
    val state by pageViewModel.uiState.collectAsState()
    val source by pageViewModel.source.collectAsState()
    val selected by pageViewModel.selected.collectAsState()
    val selection by pageViewModel.selection.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val search = pageViewModel.search

    // Kept in the view model, so coming back from a page this one opened shows the same place
    val listState = rememberLazyListState(search.scroll.index, search.scroll.offset)
    DisposableEffect(listState) {
        onDispose { search.scroll = SearchScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
    }

    LinkRecordsScaffold(
        state = state,
        source = source ?: LinkSource(pageViewModel.sourceRecordType, ""),
        selected = selected,
        selection = selection,
        onBack = onNavMenuClick,
        onQueryChange = pageViewModel::updateCurrentQuery,
        onChipClick = pageViewModel::toggleChip,
        onResultClick = pageViewModel::toggleSelectedSearchResult,
        onUnselect = pageViewModel::unselect,
        onCreate = { navigateToNewLinkedRecordPage(it, state.query.trim(), pageViewModel.selectedForNewRecord()) },
        onLink = {
            coroutineScope.launch {
                pageViewModel.linkSelectedResults()
                onNavMenuClick()
            }
        },
        listState = listState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkRecordsScaffold(
    state: SearchUiState,
    source: LinkSource,
    selected: List<SearchResult>,
    selection: SearchSelection,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onChipClick: (RecordType?) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onUnselect: (String) -> Unit,
    onCreate: (RecordType) -> Unit,
    onLink: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    var showSheet by remember { mutableStateOf(false) }
    LaunchedEffect(selected.isEmpty()) {
        if (selected.isEmpty()) showSheet = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Link records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                SelectionBar(selected = selected, onOpen = { showSheet = true }, onLink = onLink)
            }
        },
    ) { innerPadding ->
        RecordSearch(
            state = state,
            onQueryChange = onQueryChange,
            onChipClick = onChipClick,
            onResultClick = onResultClick,
            onCreate = onCreate,
            // Results stay above the keyboard
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            leading = SearchFieldLeading.Search,
            selection = selection,
            listState = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            header = { LinkingToCard(source) },
        )
    }

    if (showSheet && selected.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SelectedRecordsSheet(selected = selected, onUnselect = onUnselect, onLink = onLink)
        }
    }
}

/** The "Linking to" card: the record the selected records will be linked to. */
@Composable
private fun LinkingToCard(source: LinkSource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TypeAvatar(source.type, 28.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Linking to",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Overlapping avatars of the first five [selected] records, in the order picked. */
@Composable
private fun SelectedAvatars(selected: List<SearchResult>) {
    Box {
        selected.take(5).forEachIndexed { index, result ->
            TypeAvatar(
                type = result.recordType,
                size = 28.dp,
                modifier = Modifier.offset(x = (index * 18).dp),
            )
        }
        Spacer(Modifier.width((28 + 18 * (minOf(selected.size, 5) - 1)).dp))
    }
}

/** Shown while records are selected: their avatars and count, and Link. */
@Composable
private fun SelectionBar(selected: List<SearchResult>, onOpen: () -> Unit, onLink: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClickLabel = "Show selected records", onClick = onOpen)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectedAvatars(selected)
                Text(
                    text = "${selected.size} selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinkButton(selected.size, onLink)
        }
    }
}

@Composable
private fun LinkButton(count: Int, onLink: () -> Unit) {
    Button(onClick = onLink) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Link $count")
    }
}

/** Every selected record, also ones the current search doesn't show; unchecking one removes it. */
@Composable
private fun SelectedRecordsSheet(selected: List<SearchResult>, onUnselect: (String) -> Unit, onLink: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "${selected.size} selected",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val all = SearchSelection(selectedIds = selected.map { it.recordId }.toSet())
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(selected, key = { it.recordId }) { result ->
                ResultRow(
                    result = result,
                    query = "",
                    context = searchContextLine(result),
                    selection = all,
                    onClick = { onUnselect(result.recordId) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            LinkButton(selected.size, onLink)
        }
    }
}

// ********** Previews **********

@Preview(name = "Link records, light")
@Preview(name = "Link records, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LinkRecordsPreview() {
    val results = listOf(
        SearchResult("k1", RecordType.Category.recordTypeId, "Hardware suppliers", linkCount = 6),
        SearchResult("c1", RecordType.Contact.recordTypeId, "Harris, Owen", linkCount = 4, jobTitle = "Site lead", company = "Harbor & Pine"),
        SearchResult("c2", RecordType.Contact.recordTypeId, "Hart, Lena", linkCount = 2, company = "Northwind Traders"),
        SearchResult("o1", RecordType.Organization.recordTypeId, "Harbor & Pine", linkCount = 11),
        SearchResult("i1", RecordType.Item.recordTypeId, "Harness kit"),
    )
    val selected = listOf(results[1], SearchResult("i9", RecordType.Item.recordTypeId, "Google Pixel 9"))
    GnoseisTheme(dynamicColor = false) {
        LinkRecordsScaffold(
            state = SearchUiState(
                query = "har",
                placeholder = "Search records to link",
                chipTypes = searchChipTypes(RecordType.Note),
                results = searchResultsView(results, "har", emptySet(), canBeBest = { it.recordId != "o1" }),
            ),
            source = LinkSource(RecordType.Note, "Site visit, Harbor & Pine"),
            selected = selected,
            selection = SearchSelection(selectedIds = selected.map { it.recordId }.toSet(), linkedIds = setOf("o1")),
            onBack = {}, onQueryChange = {}, onChipClick = {}, onResultClick = {}, onUnselect = {}, onCreate = {}, onLink = {},
        )
    }
}

