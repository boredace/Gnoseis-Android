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

package org.gnoseis.ui.note

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.note.Note
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.theme.GnoseisTheme

object NoteListPageDestination : NavigationDestination {
    override val route = "note_page"
    override val titleRes = -9
}


@Composable
fun NoteListPage(
    pageViewModel: NoteListViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToNoteEditPage: (String?) -> Unit,
    navigateToSearchPage: () -> Unit,
    navMenuClick: () -> Unit
){
    val pageState by pageViewModel.notePageState.collectAsState()

    NotePageScaffold(
        pageState = pageState,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage,
        navigateToNoteEditPage = navigateToNoteEditPage,
        navigateToSearchPage = navigateToSearchPage,
        onNavMenuclick = navMenuClick,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePageScaffold(
    pageState: NoteListViewModel.NotePageState,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToNoteEditPage: (String?) -> Unit,
    navigateToSearchPage: () -> Unit,
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
                title = { Text("Notes") },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = { onNavMenuclick() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Drawer Menu.")
                    }
                },
                actions = {
                    IconButton(onClick ={ navigateToSearchPage() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick ={ /*TODO */},
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigateToNoteEditPage(null) }
            ) {
                Icon(Icons.Filled.Add, "Add Note")
                Spacer(modifier = Modifier.width(15.dp))
                Text(text = "Add Note")
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                NotePageBody(
                    pageState = pageState,
                    navigateToNoteDetailsPage = navigateToNoteDetailsPage
                )
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotePageBody(
    pageState: NoteListViewModel.NotePageState,
    navigateToNoteDetailsPage: (String) -> Unit
){
    NoteList(
        notes = pageState.notes,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage
    )
}








@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotePagePreview(){
    GnoseisTheme {
        Surface {
            val notes : List<Note> = listOf(
                Note(id="guid1", title = "Welcome to the jungle", createDateTime = 1636906970123, createDate = 1636906970123, textContents = "Hello, this is letter from your long forgotten uncle Jack who used to live in New York City but now lives deep in the jungle of the Amazonian river", ownerDbId = "11"),
                Note(id="guid2", title = "Intensive brainstorming", createDateTime = 1636906970123, createDate = 1636906970123, textContents = "This company is known for intensive brainstomring sessions which lead to the development of new products at a very fast pace. Last time this happened they created new software solution for organizing information which revolutionalized the world.", ownerDbId = "11"),
                Note(id="guid3", title = "", createDateTime = 1636906970123, createDate = 1636906970123, textContents = "This company is known for intensive brainstomring sessions which lead to the development of new products at a very fast pace. Last time this happened they created new software solution for sending securely notes which revolutionalized the world.", ownerDbId = "11"),
                Note(id="guid4", title = "Yet another day in the paradise", createDateTime = 1669906970123, createDate = 1636976970123, textContents = "This company is known for intensive brainstomring sessions which lead to the development of new products at a very fast pace. Last time this happened they created new software solution for sending securely notes which revolutionalized the world.", ownerDbId = "11"),

                )
            NotePageScaffold(
                pageState = NoteListViewModel.NotePageState(
                    notes,
                ),
                navigateToNoteDetailsPage = {},
                navigateToNoteEditPage = {},
                navigateToSearchPage = {}
            ) {

            }
        }
    }

}

