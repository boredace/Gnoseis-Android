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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import kotlinx.coroutines.launch
import org.gnoseis.ui.icons.SettingsIcon

@Composable
fun DrawerContent(navController: NavHostController, drawerState: DrawerState) {
    var scope = rememberCoroutineScope()
    var currentBackStackEntryAsState = navController.currentBackStackEntryAsState()
    var destination = currentBackStackEntryAsState.value?.destination

    ModalDrawerSheet {
        Spacer(Modifier.height(26.dp))
        Text(text = "Gnoesis v0.1", Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(18.dp))

        NavigationDrawerItems(navController, drawerState)

        Spacer(modifier = Modifier.weight(1f))
        /*Text(
            "Battery Watch Dog",
            fontSize = 16.dp,
            modifier = Modifier.padding(horizontal = 28.dp)
           )*/
        Spacer(modifier = Modifier.height(4.dp))


        Spacer(modifier = Modifier.height(4.dp))

        NavigationDrawerItem(
            label = { Text(text="Test") },
            selected = destination?.route == "test_page",
            onClick = {
                navController.navigate( "test_page", navOptions {
                this.launchSingleTop = true
                this.restoreState = true
            })
                scope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            icon = { SettingsIcon() },

            label = { Text(text = "Settings") },
            selected = destination?.route == "SettingPage",
            onClick = {
                navController.navigate("SettingPage", navOptions {
                    this.launchSingleTop = true
                    this.restoreState = true
                })
                scope.launch {
                    drawerState.close()
                }
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        /* Text(
             "Version 0.1", textAlign = TextAlign.Center,
             modifier = Modifier.width(150.dp)
         )*/
        Spacer(Modifier.height(26.dp))

    }
}
