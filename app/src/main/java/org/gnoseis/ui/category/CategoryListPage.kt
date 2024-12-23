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

package org.gnoseis.ui.category

import android.content.res.Configuration
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
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.category.Category
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class CategoryListRoute(
    val dummy: Boolean? = false
)

@Composable
fun CategoryListPage(
    pageViewModel: CategoryListViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToCategoryEditPage: (String?) -> Unit,
    navigateToSearchPage: () -> Unit,
    navMenuClick: () -> Unit
){
    val pageState by pageViewModel.categoryListPageState.collectAsState()

    CategoryListPageScaffold(
        pageState = pageState,
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
        navigateToCategoryEditPage = navigateToCategoryEditPage,
        navigateToSearchPage = navigateToSearchPage,
        onNavMenuclick = navMenuClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListPageScaffold(
    pageState: CategoryListViewModel.CategoryListPageState,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToCategoryEditPage: (String?) -> Unit,
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
                title = { Text("Categories") },
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
                    IconButton(onClick ={ /*TODO */}) {
                        Icon(Icons.Filled.FilterList, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigateToCategoryEditPage(null) }
            ) {
                Icon(Icons.Filled.Add, "Add Category")
                Spacer(modifier = Modifier.width(15.dp))
                Text(text = "Add Category")
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                CategoryPageBody(
                    pageState = pageState,
                    navigateToCategoryDetailsPage = navigateToCategoryDetailsPage
                )
            }
        }
    )
}

@Composable
fun CategoryPageBody(
    pageState: CategoryListViewModel.CategoryListPageState,
    navigateToCategoryDetailsPage: (String) -> Unit
){
    CategoryList(
        categories = pageState.categories,
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage
    )
}


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ItemListPagePreview(){
    GnoseisTheme {
        Surface {
            val items : List<Category> = listOf(
                Category(id="guid1",  categoryName = "Personal", ownerDbId = "11"),
                Category(id="guid2",  categoryName = "Health", ownerDbId = "11"),
                Category(id="guid2a",  categoryName = "High Priced", ownerDbId = "11"),
                Category(id="guid2b",  categoryName = "Humourous Stories", ownerDbId = "11"),
                Category(id="guid3",  categoryName = "Work", ownerDbId = "11"),
                Category(id="guid4",  categoryName = "Important", ownerDbId = "11"),
            )
            CategoryListPageScaffold(
                pageState = CategoryListViewModel.CategoryListPageState(
                    items,
                ),
                navigateToCategoryDetailsPage = {},
                navigateToCategoryEditPage = {},
                navigateToSearchPage = {}
            ) {

            }
        }
    }

}