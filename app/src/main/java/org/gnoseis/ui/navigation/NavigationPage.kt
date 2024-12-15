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

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.gnoseis.data.enums.NoteEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.TestPage
import org.gnoseis.ui.TestPageDestination
import org.gnoseis.ui.category.CategoryDetailsPage
import org.gnoseis.ui.category.CategoryDetailsPageDestination
import org.gnoseis.ui.category.CategoryEditDestination
import org.gnoseis.ui.category.CategoryEditPage
import org.gnoseis.ui.category.CategoryListPage
import org.gnoseis.ui.category.CategoryListPageDestination
import org.gnoseis.ui.contact.ContactDetailsPage
import org.gnoseis.ui.contact.ContactDetailsPageDestination
import org.gnoseis.ui.contact.ContactEditDestination
import org.gnoseis.ui.contact.ContactEditPage
import org.gnoseis.ui.contact.ContactListPage
import org.gnoseis.ui.contact.ContactListPageDestination
import org.gnoseis.ui.item.ItemDetailsPage
import org.gnoseis.ui.item.ItemDetailsPageDestination
import org.gnoseis.ui.item.ItemEditDestination
import org.gnoseis.ui.item.ItemEditPage
import org.gnoseis.ui.item.ItemListPage
import org.gnoseis.ui.item.ItemListPageDestination
import org.gnoseis.ui.link.LinkRecordsDestination
import org.gnoseis.ui.link.LinkRecordsPage
import org.gnoseis.ui.note.NoteDetailsPage
import org.gnoseis.ui.note.NoteDetailsPageDestination
import org.gnoseis.ui.note.NoteEditPage
import org.gnoseis.ui.note.NoteEditRoute
import org.gnoseis.ui.note.NoteListPage
import org.gnoseis.ui.note.NoteListPageDestination
import org.gnoseis.ui.organization.OrganizationDetailsPage
import org.gnoseis.ui.organization.OrganizationDetailsPageDestination
import org.gnoseis.ui.organization.OrganizationEditDestination
import org.gnoseis.ui.organization.OrganizationEditPage
import org.gnoseis.ui.organization.OrganizationListPage
import org.gnoseis.ui.organization.OrganizationListPageDestination
import org.gnoseis.ui.search.SearchPage
import org.gnoseis.ui.search.SearchPageDestination
import org.gnoseis.ui.settings.BackupSettingsPage
import org.gnoseis.ui.settings.BackupSettingsPageRoute
import org.gnoseis.ui.settings.SettingsPage
import org.gnoseis.ui.settings.SettingsPageRoute

const val TAG = "navigation_page"

@Composable
fun NavigationPage(
//    mainViewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)

) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()


    fun navMenuClick() {
        if(drawerState.isClosed){
            coroutineScope.launch {
                drawerState.open()
            }
        }else{
            coroutineScope.launch {
                drawerState.close()
            }
        }
    }

    ModalNavigationDrawer(
        drawerContent = { DrawerContent(navController, drawerState) },
        drawerState = drawerState
    ) {
        Box(/*modifier = Modifier.padding(it)*/) {
            NavHost(navController = navController, startDestination = "note_page") {
                //
                // ********** CATEGORY LIST PAGE **********
                //
                composable(
                    route = CategoryListPageDestination.route
                ) {
                    CategoryListPage(
                        navigateToCategoryDetailsPage = { navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToCategoryEditPage = { navController.navigate("${CategoryEditDestination.route}/$it") },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                //
                // ********** CATEGORY DETAILS PAGE **********
                //
                composable(
                    route = CategoryDetailsPageDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(CategoryDetailsPageDestination.categoryIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    CategoryDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToItemDetailsPage = {navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToOrganizationDetailsPage = { navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/4") },
                        navigateToCategoryEditPage = { navController.navigate("${CategoryEditDestination.route}/$it")} ,
                        )
                }
                //
                // ********** CATEGORY EDIT PAGE **********
                //
                composable(
                    route = CategoryEditDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(CategoryEditDestination.categoryIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    CategoryEditPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${CategoryDetailsPageDestination.route}/$it")
                        }
                    )
                }



                // #############################################################
                // ##################  CONTACT   ###############################
                // #############################################################

                //
                // ********** CONTACT LIST PAGE **********
                //
                composable(
                    route = ContactListPageDestination.route
                ) {
                    ContactListPage(
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToContactEditPage = { navController.navigate("${ContactEditDestination.route}/new") },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                //
                // ********** CONTACT DETAILS PAGE **********
                //
                composable(
                    route = ContactDetailsPageDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(ContactDetailsPageDestination.contactIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    ContactDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = {navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToItemDetailsPage = {navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToOrganizationDetailsPage = {navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/2") },
                        navigateToContactEditPage = {navController.navigate("${ContactEditDestination.route}/${it}") },
                    )
                }

                //
                // ********** CONTACT EDIT PAGE **********
                //
                composable(
                    route = ContactEditDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(ContactEditDestination.contactIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    val contactId = it.arguments?.getString("contactId") ?: "new"  // -9 means add new note
                    ContactEditPage(
                        navMenuClick = { navController.popBackStack()},
                        navigateToContactDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${ContactDetailsPageDestination.route}/$it")
                        },
                    )
                }



                // #############################################################
                // #####################    ITEM    ############################
                // #############################################################

                //
                // ********** ITEM LIST PAGE **********
                //
                composable(
                    route = ItemListPageDestination.route
                ) {
                    ItemListPage(
                        navigateToItemDetailsPage = {navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToItemEditPage = {navController.navigate("${ItemEditDestination.route}/new") },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                //
                // ********** ITEM DETAILS PAGE **********
                //
                composable(
                    route = ItemDetailsPageDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(ItemDetailsPageDestination.itemIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    ItemDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = {navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToOrganizationDetailsPage = {navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/5") },
                        navigateToItemEditPage = {navController.navigate("${ItemEditDestination.route}/$it") },
                        )
                }

                //
                // ********** ITEM EDIT PAGE **********
                //
                composable(
                    route = ItemEditDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(ItemEditDestination.itemIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    ItemEditPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToItemDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${ItemDetailsPageDestination.route}/$it")
                        },
                    )
                }





                // #############################################################
                // #####################    NOTE    ############################
                // #############################################################

                //
                // ********** NOTE LIST PAGE **********
                //
                composable(
                    route = NoteListPageDestination.route
                ) {
                    NoteListPage(
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToNoteEditPage = {navController.navigate(NoteEditRoute(it, NoteEditPageMode.NEW))},
                        navigateToSearchPage = {navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                //
                // ********** NOTE DETAILS PAGE **********
                //
                composable(
                    route = NoteDetailsPageDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(NoteDetailsPageDestination.noteIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    NoteDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = { navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToItemDetailsPage = { navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToOrganizationDetailsPage = { navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/1") },
                        navigateToNoteEditPage = { navController.navigate(NoteEditRoute(it, NoteEditPageMode.EDIT))},


                        )
                }

                composable<NoteEditRoute> {
                    val args = it.toRoute<NoteEditRoute>()
                    NoteEditPage(
                        pageMode = args.pageMode,
                        navMenuClick = { navController.popBackStack() },
                        navigateToNoteDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${NoteDetailsPageDestination.route}/$it")
                        }
                    )
                }

                // #############################################################
                // ###################      ORGANIZATION      ##################
                // #############################################################

                //
                // ********** ORGANIZATION LIST PAGE **********
                //
                composable(
                    route = OrganizationListPageDestination.route
                ) {
                    OrganizationListPage(
                        navigateToOrganizationDetailsPage = { navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToOrganizationEditPage = {navController.navigate("${OrganizationEditDestination.route}/new") },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                //
                // ********** ORGANIZATION DETAILS PAGE **********
                //
                composable(
                    route = OrganizationDetailsPageDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(OrganizationDetailsPageDestination.organizationIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    OrganizationDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = {navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToItemDetailsPage = {navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/3") },
                        navigateToLinkNewNotePage = { navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, RecordType.Organization, it) )},
                        navigateToOrganizationEditPage = {navController.navigate("${OrganizationEditDestination.route}/$it") },
                    )
                }

                //
                // ********** ORGANIZATION EDIT PAGE **********
                //
                composable(
                    route = OrganizationEditDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(OrganizationEditDestination.organizationIdArg) {
                            type = NavType.StringType
                        }
                    )
                ){
                    OrganizationEditPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToOrganizationDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${OrganizationDetailsPageDestination.route}/$it")
                        }
                    )
                }







                //
                // ********** LINK RECORD PAGE **********
                //
                composable(
                    route = LinkRecordsDestination.routeWithArgs,
                    arguments = listOf(
                        navArgument(LinkRecordsDestination.sourceRecordIdArg) {
                            type = NavType.StringType },
                        navArgument(LinkRecordsDestination.sourceRecordTypeIdArg) {
                            type = NavType.IntType }
                    )
                ) {
                    LinkRecordsPage(
                        onNavMenuClick = { navController.popBackStack()}
                    )
                }




                //
                // ********** SEARCH PAGE **********
                //
                composable(
                    route = SearchPageDestination.route,

                ) {
                    SearchPage(
                        navigateToCategoryDetailsPage = {navController.navigate("${CategoryDetailsPageDestination.route}/$it") },
                        navigateToContactDetailsPage = { navController.navigate("${ContactDetailsPageDestination.route}/$it") },
                        navigateToItemDetailsPage = {navController.navigate("${ItemDetailsPageDestination.route}/$it") },
                        navigateToNoteDetailsPage = {navController.navigate("${NoteDetailsPageDestination.route}/$it") },
                        navigateToOrganizationDetailsPage = { navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                    )
                }




                composable(
                    route = TestPageDestination.route
                ){
                    TestPage(
//                        onNavMenuClick = { navController.popBackStack()}
                    )
                }


                //
                // ********** SETTINGS PAGE **********
                //

                composable<SettingsPageRoute> {
                    SettingsPage(
                        onNavMenuclick = { navController.popBackStack() },
                        listItemClicked = { menuIndex ->
                            Log.i("Settings", "Menu Index ${menuIndex}")

                            when(menuIndex) {
                                0 -> navController.navigate(BackupSettingsPageRoute())
                            }
                        }
                    )
                }

                composable<BackupSettingsPageRoute> {
                    BackupSettingsPage(
                        onNavMenuClick = { navController.popBackStack() },
                        onExportClicked = {},
                        onImportClicked = {}

                        )
                }
            }
        }
    }
}

