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

package org.gnoseis.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.gnoseis.ui.category.CategoryListRoute
import org.gnoseis.ui.contact.ContactListRoute
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.item.ItemListRoute
import org.gnoseis.ui.note.NoteListRoute
import org.gnoseis.ui.organization.OrganizationListRoute

@Composable
fun NavigationDrawerItems(
    navController: NavHostController,
    drawerState: DrawerState
) {

    var scope = rememberCoroutineScope()
    var currentBackStackEntryAsState = navController.currentBackStackEntryAsState()
    var destination = currentBackStackEntryAsState.value?.destination

    NavigationDrawerItem(
        icon = { ContactIcon() },
        label = { Text(text = "Contacts") },
        selected = destination?.route == ContactListRoute.toString(),
        onClick = {
            navController.navigate(ContactListRoute()) {
                this.launchSingleTop = true
                this.restoreState = true
            }
            scope.launch {
                drawerState.close()
            }

        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )

    NavigationDrawerItem(
        icon = { OrganizationIcon() },
        label = { Text(text = "Organizations") },
        selected = destination?.route == OrganizationListRoute.toString(),
        onClick = {
            navController.navigate(OrganizationListRoute()) {
                this.launchSingleTop = true
                this.restoreState = true
            }
            scope.launch {
                drawerState.close()
            }

        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )

    NavigationDrawerItem(
        icon = { NoteIcon() },
        label = { Text(text = "Notes") },
        selected = destination?.route == NoteListRoute.toString(),
        onClick = {
            navController.navigate(NoteListRoute) {
                this.launchSingleTop = true
                this.restoreState = true
            }
            scope.launch {
                drawerState.close()
            }

        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )

    NavigationDrawerItem(
        icon = { CategoryIcon() } ,
        label = { Text(text = "Categories") },
        selected = destination?.route == CategoryListRoute.toString(),
        onClick = {
            navController.navigate(CategoryListRoute()) {
                this.launchSingleTop = true
                this.restoreState = true
            }
            scope.launch {
                drawerState.close()
            }

        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )

    NavigationDrawerItem(
        icon = { ItemIcon() },
        label = { Text(text = "Items") },
        selected = destination?.route == ItemListRoute.toString(),
        onClick = {
            navController.navigate(ItemListRoute()) {
                this.launchSingleTop = true
                this.restoreState = true
            }
            scope.launch {
                drawerState.close()
            }

        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

