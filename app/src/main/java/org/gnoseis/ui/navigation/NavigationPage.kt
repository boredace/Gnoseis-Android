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
import org.gnoseis.data.enums.CategoryEditPageMode
import org.gnoseis.data.enums.ContactEditPageMode
import org.gnoseis.data.enums.ItemEditPageMode
import org.gnoseis.data.enums.NoteEditPageMode
import org.gnoseis.data.enums.OrganizationEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.TestPage
import org.gnoseis.ui.TestPageDestination
import org.gnoseis.ui.category.CategoryDetailsPage
import org.gnoseis.ui.category.CategoryDetailsRoute
import org.gnoseis.ui.category.CategoryEditPage
import org.gnoseis.ui.category.CategoryEditRoute
import org.gnoseis.ui.category.CategoryListPage
import org.gnoseis.ui.category.CategoryListPageDestination
import org.gnoseis.ui.contact.ContactDetailsPage
import org.gnoseis.ui.contact.ContactDetailsRoute
import org.gnoseis.ui.contact.ContactEditPage
import org.gnoseis.ui.contact.ContactEditRoute
import org.gnoseis.ui.contact.ContactListPage
import org.gnoseis.ui.contact.ContactListPageDestination
import org.gnoseis.ui.item.ItemDetailsPage
import org.gnoseis.ui.item.ItemDetailsRoute
import org.gnoseis.ui.item.ItemEditPage
import org.gnoseis.ui.item.ItemEditRoute
import org.gnoseis.ui.item.ItemListPage
import org.gnoseis.ui.item.ItemListPageDestination
import org.gnoseis.ui.link.LinkRecordsDestination
import org.gnoseis.ui.link.LinkRecordsPage
import org.gnoseis.ui.note.NoteDetailsPage
import org.gnoseis.ui.note.NoteDetailsRoute
import org.gnoseis.ui.note.NoteEditPage
import org.gnoseis.ui.note.NoteEditRoute
import org.gnoseis.ui.note.NoteListPage
import org.gnoseis.ui.note.NoteListPageDestination
import org.gnoseis.ui.organization.OrganizationDetailsPage
import org.gnoseis.ui.organization.OrganizationDetailsPageDestination
import org.gnoseis.ui.organization.OrganizationEditPage
import org.gnoseis.ui.organization.OrganizationEditRoute
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

                // #############################################################
                // ###################    CATEGORY    ##########################
                // #############################################################

                composable(
                    route = CategoryListPageDestination.route
                ) {
                    CategoryListPage(
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToCategoryEditPage = { navController.navigate(CategoryEditRoute(it, CategoryEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

               composable<CategoryDetailsRoute> {
                   val args = it.toRoute<CategoryDetailsRoute>()
                    CategoryDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToOrganizationDetailsPage = { navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/4") },
                        navigateToLinkNewNotePage = { navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, RecordType.Category, it) )},
                        navigateToLinkNewContactPage = { navController.navigate(ContactEditRoute(null, ContactEditPageMode.NEWLINK, RecordType.Category, it) )},
                        navigateToLinkNewItemPage = { navController.navigate(ItemEditRoute(null, ItemEditPageMode.NEWLINK, RecordType.Category, it) )},
                        navigateToLinkNewOrganizationPage = { navController.navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEWLINK, RecordType.Category, it) )},
                        navigateToCategoryEditPage = { navController.navigate(CategoryEditRoute(it, CategoryEditPageMode.EDIT)) },
                        )
                }

                composable<CategoryEditRoute> {
                    val args = it.toRoute<CategoryEditRoute>()
                    CategoryEditPage(
                        pageMode = args.pageMode,
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = {
                            navController.popBackStack()
                            navController.navigate(CategoryDetailsRoute(it))
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
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToContactEditPage = { navController.navigate(ContactEditRoute(it, ContactEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

               composable<ContactDetailsRoute> {
                    val args = it.toRoute<ContactDetailsRoute>()
                    ContactDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToOrganizationDetailsPage = {navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/2") },
                        navigateToLinkNewNotePage = { navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, RecordType.Contact, it) )},
                        navigateToLinkNewCategoryPage = { navController.navigate(CategoryEditRoute(null, CategoryEditPageMode.NEWLINK, RecordType.Contact, it) )},
                        navigateToLinkNewItemPage = { navController.navigate(ItemEditRoute(null, ItemEditPageMode.NEWLINK, RecordType.Contact, it) )},
                        navigateToLinkNewOrganizationPage = { navController.navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEWLINK, RecordType.Contact, it) )},
                        navigateToContactEditPage = { navController.navigate(ContactEditRoute(it, ContactEditPageMode.EDIT))},
                        )
                }

                composable<ContactEditRoute> {
                    val args = it.toRoute<ContactEditRoute>()
                    ContactEditPage(
                        pageMode = args.pageMode,
                        navMenuClick = { navController.popBackStack() },
                        navigateToContactDetailsPage = {
                            navController.popBackStack()
                            navController.navigate(ContactDetailsRoute(it))
                        }
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
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToItemEditPage = { navController.navigate(ItemEditRoute(it, ItemEditPageMode.NEW)) },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                composable<ItemDetailsRoute> {
                    val args = it.toRoute<ItemDetailsRoute>()
                    ItemDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToOrganizationDetailsPage = {navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/5") },
                        navigateToLinkNewNotePage = { navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, RecordType.Item, it) )},
                        navigateToLinkNewCategoryPage = { navController.navigate(CategoryEditRoute(null, CategoryEditPageMode.NEWLINK, RecordType.Item, it) )},
                        navigateToLinkNewContactPage = { navController.navigate(ContactEditRoute(null, ContactEditPageMode.NEWLINK, RecordType.Item, it) )},
                        navigateToLinkNewOrganizationPage = { navController.navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEWLINK, RecordType.Item, it) )},
                        navigateToItemEditPage = { navController.navigate(ItemEditRoute(it, ItemEditPageMode.EDIT)) },
                    )
                }

                composable<ItemEditRoute> {
                    val args = it.toRoute<ItemEditRoute>()
                    ItemEditPage(
                        pageMode = args.pageMode,
                        navMenuClick = { navController.popBackStack() },
                        navigateToItemDetailsPage = {
                            navController.popBackStack()
                            navController.navigate(ItemDetailsRoute(it))
                        }
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
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToNoteEditPage = {navController.navigate(NoteEditRoute(it, NoteEditPageMode.NEW))},
                        navigateToSearchPage = {navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                composable<NoteDetailsRoute> {
                    val args = it.toRoute<NoteDetailsRoute>()
                    NoteDetailsPage(
                        navMenuClick = { navController.popBackStack() },
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToOrganizationDetailsPage = {navController.navigate("${OrganizationDetailsPageDestination.route}/$it") },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/2") },
                        navigateToLinkNewContactPage = { navController.navigate(ContactEditRoute(null, ContactEditPageMode.NEWLINK, RecordType.Note, it) )},
                        navigateToLinkNewCategoryPage = { navController.navigate(CategoryEditRoute(null, CategoryEditPageMode.NEWLINK, RecordType.Note, it) )},
                        navigateToLinkNewItemPage = { navController.navigate(ItemEditRoute(null, ItemEditPageMode.NEWLINK, RecordType.Note, it) )},
                        navigateToLinkNewOrganizationPage = { navController.navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEWLINK, RecordType.Note, it) )},
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
                            navController.navigate(NoteDetailsRoute(it))
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
                        navigateToOrganizationEditPage = { navController.navigate(OrganizationEditRoute(it, OrganizationEditPageMode.NEW)) },
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
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToLinkRecordsPage = { navController.navigate("${LinkRecordsDestination.route}/$it/3") },
                        navigateToLinkNewNotePage = { navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, RecordType.Organization, it) )},
                        navigateToLinkNewCategoryPage = { navController.navigate(CategoryEditRoute(null, CategoryEditPageMode.NEWLINK, RecordType.Organization, it) )},
                        navigateToLinkNewContactPage = { navController.navigate(ContactEditRoute(null, ContactEditPageMode.NEWLINK, RecordType.Organization, it) )},
                        navigateToLinkNewItemPage = { navController.navigate(ItemEditRoute(null, ItemEditPageMode.NEWLINK, RecordType.Organization, it))},
                        navigateToOrganizationEditPage = { navController.navigate(OrganizationEditRoute(it, OrganizationEditPageMode.EDIT)) },
                    )
                }

                composable<OrganizationEditRoute> {
                    val args = it.toRoute<OrganizationEditRoute>()
                    OrganizationEditPage(
                        pageMode = args.pageMode,
                        navMenuClick = { navController.popBackStack() },
                        navigateToOrganizationDetailsPage = {
                            navController.popBackStack()
                            navController.navigate("${OrganizationDetailsPageDestination.route}/$it")
                        }
                    )
                }



                // ---------------------------------------------------------------------------------


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
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
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
                        onImportClicked = {}

                        )
                }
            }
        }
    }
}

