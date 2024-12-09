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

package org.gnoseis.ui.category

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.constants.TITLE_LENGTH_CATEGORY
import org.gnoseis.data.entity.category.Category
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.theme.GnoseisTheme

object CategoryEditDestination : NavigationDestination {
    override val route = "category_edit"
    override val titleRes = -9
    const val categoryIdArg = "categoryId"
    val routeWithArgs = "${CategoryEditDestination.route}/{$categoryIdArg}"
}
const val TAG = "category_edit"

@Composable
fun CategoryEditPage(
    pageViewModel: CategoryEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,

    ) {
    val pageState by pageViewModel.categoryEditPageState.collectAsState()
    val coroutineScope = rememberCoroutineScope()


    CategoryEditScaffold(
        pageState = pageState,
        onNavMenuclick = navMenuClick,
        onSave = {
            coroutineScope.launch {
                val addedCategoryId = pageViewModel.onSave()
                if(addedCategoryId != null) {
                    navigateToCategoryDetailsPage(addedCategoryId)
                } else {
                    navMenuClick()
                }
            }
         },
        onCategoryNameChanged = { pageViewModel.updateCategoryName(it) },
        onCategoryDescriptionChanged = {pageViewModel.updateCategoryDescription(it) }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScaffold(
    pageState: CategoryEditViewModel.CategoryEditState,
    onNavMenuclick: () -> Unit,
    onSave: () -> Unit,
    onCategoryNameChanged: (String) -> Unit,
    onCategoryDescriptionChanged: (String) -> Unit


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
                title = { Text(text="New Category") },
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
                CategoryEditBody(
                    pageState = pageState,
                    onCategoryNameChanged = onCategoryNameChanged,
                    onCategoryDescriptionChanged = onCategoryDescriptionChanged
                )
            }
        }
    )
}

@Composable
fun CategoryEditBody(
    pageState: CategoryEditViewModel.CategoryEditState,
    onCategoryNameChanged: (String) -> Unit,
    onCategoryDescriptionChanged: (String) -> Unit
){
    Column(
       modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
    ){

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = {Text(text="Category name")},
            value = pageState.editCategoryName?:"",
            onValueChange = {
                if(it.length <= TITLE_LENGTH_CATEGORY) {
                    onCategoryNameChanged(it)
                }
            },
            singleLine = true,

        )
        Spacer(modifier = Modifier.height(10.dp))

        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            label = {Text(text="Category description")},
            value = pageState.editCategoryDescription?:"",
            onValueChange = {
                onCategoryDescriptionChanged(it)
            },
            singleLine = false,
            minLines = 10)
    }
}



@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoryEditPagePreview()
{
    GnoseisTheme {
        Surface {
            CategoryEditScaffold(
                pageState = CategoryEditViewModel.CategoryEditState(
                    isValid = false,
                    category = Category(
                        ownerDbId = "aaa",
                        categoryName = "New category name"
                    ),
                ),
                onNavMenuclick = {},
                onSave = {},
                onCategoryNameChanged = {},
                onCategoryDescriptionChanged = {},
            )
        }
    }
}