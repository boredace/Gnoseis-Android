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

package org.gnoseis.ui.organization

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
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class OrganizationListRoute(
    val dummy: Boolean? = false
)

@Composable
fun OrganizationListPage(
    pageViewModel: OrganizationListViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToOrganizationEditPage: (String?) -> Unit,
    navigateToSearchPage: () -> Unit,
    navMenuClick: () -> Unit
){
    val pageState by pageViewModel.organizationListPageState.collectAsState()

    OrganizationPageScaffold(
        pageState = pageState,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
        navigateToOrganizationEditPage = navigateToOrganizationEditPage,
        navigateToSearchPage = navigateToSearchPage,
        onNavMenuclick = navMenuClick,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationPageScaffold(
    pageState: OrganizationListViewModel.OrganizationListPageState,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToOrganizationEditPage: (String?) -> Unit,
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
                title = { Text("Organizations") },
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
                onClick = { navigateToOrganizationEditPage(null) }
            ) {
                Icon(Icons.Filled.Add, "Add Organization")
                Spacer(modifier = Modifier.width(15.dp))
                Text(text = "Add Organization")
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                OrganizationPageBody(
                    pageState = pageState,
                    navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage
                )
            }
        }
    )
}


@Composable
fun OrganizationPageBody(
    pageState: OrganizationListViewModel.OrganizationListPageState,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    ){

    OrganizationList(
        organizations = pageState.organizations,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage
    )
}






@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OrganizationPagePreview(){
    GnoseisTheme {
        Surface {
            val organizations: List<Organization> = listOf(
                Organization(
                    id = "organization1",
                    ownerDbId = "11",
                    organizationName = "Company 1"
                ),
                Organization(id = "organization2", ownerDbId = "11", organizationName = "Org X"),
            )
            OrganizationPageScaffold(
                pageState = OrganizationListViewModel.OrganizationListPageState(
                    organizations,
                ),
                navigateToOrganizationDetailsPage = {},
                navigateToOrganizationEditPage = {},
                navigateToSearchPage = { },
            ) {

            }
        }
    }
}
