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

import androidx.navigation.NavDestination.Companion.hasRoute
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.TestPage
import org.gnoseis.ui.TestPageDestination
import org.gnoseis.ui.category.CategoryListPage
import org.gnoseis.ui.category.CategoryListRoute
import org.gnoseis.ui.contact.ContactListPage
import org.gnoseis.ui.contact.ContactListRoute
import org.gnoseis.ui.home.HomePage
import org.gnoseis.ui.home.HomeRoute
import org.gnoseis.ui.item.ItemListPage
import org.gnoseis.ui.item.ItemListRoute
import org.gnoseis.ui.link.LinkRecordsRoute
import org.gnoseis.ui.link.LinkRecordsPage
import org.gnoseis.ui.note.NoteListPage
import org.gnoseis.ui.note.NoteListRoute
import org.gnoseis.ui.organization.OrganizationListPage
import org.gnoseis.ui.organization.OrganizationListRoute
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordDetailsRoute
import org.gnoseis.ui.record.RecordEditNavigation
import org.gnoseis.ui.record.RecordEditRoute
import org.gnoseis.ui.search.SearchPage
import org.gnoseis.ui.search.SearchRoute
import org.gnoseis.ui.settings.BackupSettingsPage
import org.gnoseis.ui.settings.BackupSettingsPageRoute
import org.gnoseis.ui.settings.SettingsPage
import org.gnoseis.ui.settings.SettingsPageRoute

/**
 * #6: key in a Details page's back stack entry, set by an Edit page in NEWLINK mode to the
 * record type it just linked, so the Details page can open that type's tab.
 */
private const val LINKED_NEW_RECORD_TYPE_ID = "linkedNewRecordTypeId"

private fun NavController.popBackStackAfterLinkingNew(recordType: RecordType) {
    // #10: a record created from the Link Records page goes back past it, to the source's page
    if (previousBackStackEntry?.destination?.hasRoute<LinkRecordsRoute>() == true) popBackStack()
    previousBackStackEntry?.savedStateHandle?.set(LINKED_NEW_RECORD_TYPE_ID, recordType.recordTypeId)
    popBackStack()
}

/** Opens the Details page of the record [id] of [type]. */
private fun NavController.navigateToRecordDetails(type: RecordType, id: String) {
    navigate(RecordDetailsRoute(type, id))
}

/** Opens the Edit page of the record [id] of [type]. */
private fun NavController.navigateToRecordEdit(type: RecordType, id: String) {
    navigate(RecordEditRoute(type, id, RecordEditPageMode.EDIT))
}

/**
 * Opens the Edit page for a new record of [type], to be linked to [sourceId] of [sourceType].
 * With [prefillName], the record's name starts as that (#10).
 */
private fun NavController.navigateToNewLinkedRecord(type: RecordType, sourceType: RecordType, sourceId: String, prefillName: String? = null) {
    navigate(RecordEditRoute(type, null, RecordEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
}

/** Opens the Edit page for a new record of [type]; with [prefillName] (#10), named that. */
private fun NavController.navigateToNewRecord(type: RecordType, prefillName: String? = null) {
    navigate(RecordEditRoute(type, null, RecordEditPageMode.NEW, prefillName = prefillName))
}

/** Where the Details page of a record of [type] can go. */
private fun NavController.recordDetailsNavigation(type: RecordType) = RecordDetailsNavigation(
    back = { popBackStack() },
    openRecord = { linkedType, id -> navigateToRecordDetails(linkedType, id) },
    edit = { navigateToRecordEdit(type, it) },
    linkExisting = { navigate(LinkRecordsRoute(it, type)) },
    newLinked = { newType, sourceId -> navigateToNewLinkedRecord(newType, type, sourceId) },
)

/** Where the Edit page of a record of [type] can go. */
private fun NavController.recordEditNavigation(type: RecordType) = RecordEditNavigation(
    back = { popBackStack() },
    openCreated = { id ->
        popBackStack()
        navigateToRecordDetails(type, id)
    },
    backAfterLinkingNew = { popBackStackAfterLinkingNew(type) },
)

@Composable
fun NavigationPage(
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    fun navMenuClick() {
        if(drawerState.isClosed){
            coroutineScope.launch {
                drawerState.open()
            }
        } else {
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
            NavHost(navController = navController, startDestination = HomeRoute) {

                // #############################################################
                // ######################    HOME    ###########################
                // #############################################################

                composable<HomeRoute> {
                    HomePage(
                        navMenuClick = { navMenuClick() },
                        navigateToSettingsPage = { navController.navigate(SettingsPageRoute()) },
                        navigateToSearchPage = { navController.navigate(SearchRoute()) },
                        navigateToRecordDetailsPage = { type, id -> navController.navigateToRecordDetails(type, id) },
                        navigateToNewRecordPage = { type -> navController.navigateToNewRecord(type) },
                        navigateToRecordListPage = { type ->
                            when (type) {
                                RecordType.Category -> navController.navigate(CategoryListRoute())
                                RecordType.Contact -> navController.navigate(ContactListRoute())
                                RecordType.Item -> navController.navigate(ItemListRoute())
                                RecordType.Note -> navController.navigate(NoteListRoute)
                                RecordType.Organization -> navController.navigate(OrganizationListRoute())
                                RecordType.LinkedRecord -> Unit
                            }
                        },
                        navigateToLinkRecordsPage = { navController.navigate(LinkRecordsRoute(it, RecordType.Note)) },
                        navigateToBackupSettingsPage = { navController.navigate(BackupSettingsPageRoute()) },
                    )
                }

                // #############################################################
                // ####################  CATEGORY   ############################
                // #############################################################

                composable<CategoryListRoute> {
                    CategoryListPage(
                        navigateToCategoryDetailsPage = { navController.navigateToRecordDetails(RecordType.Category, it) },
                        navigateToCategoryEditPage = { navController.navigate(RecordEditRoute(RecordType.Category, it, RecordEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchRoute(RecordType.Category.recordTypeId)) },
                        navMenuClick = { navMenuClick() }
                    )
                }


                // #############################################################
                // ##################  CONTACT   ###############################
                // #############################################################

                composable<ContactListRoute> {
                    val args = it.toRoute<ContactListRoute>()
                    ContactListPage(
                        navigateToContactDetailsPage = { navController.navigateToRecordDetails(RecordType.Contact, it) },
                        navigateToContactEditPage = { navController.navigate(RecordEditRoute(RecordType.Contact, it, RecordEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchRoute(RecordType.Contact.recordTypeId)) },
                        navMenuClick = { navMenuClick() }
                    )
                }


                // #############################################################
                // #####################    ITEM    ############################
                // #############################################################

                composable<ItemListRoute> {
                    val args = it.toRoute<ItemListRoute>()
                    ItemListPage(
                        navigateToItemDetailsPage = { navController.navigateToRecordDetails(RecordType.Item, it) },
                        navigateToItemEditPage = { navController.navigate(RecordEditRoute(RecordType.Item, it, RecordEditPageMode.NEW)) },
                        navigateToSearchPage = { navController.navigate(SearchRoute(RecordType.Item.recordTypeId)) },
                        navMenuClick = { navMenuClick() }
                    )
                }


                // #############################################################
                // #####################    NOTE    ############################
                // #############################################################

                composable<NoteListRoute>() {
                    NoteListPage(
                        navigateToNoteDetailsPage = { navController.navigateToRecordDetails(RecordType.Note, it) },
                        navigateToNoteEditPage = {navController.navigate(RecordEditRoute(RecordType.Note, it, RecordEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchRoute(RecordType.Note.recordTypeId)) },
                        navMenuClick = { navMenuClick() }
                    )
                }


                // #############################################################
                // ###################      ORGANIZATION      ##################
                // #############################################################

                composable<OrganizationListRoute> {
                    val args = it.toRoute<OrganizationListRoute>()
                    OrganizationListPage(
                        navigateToOrganizationDetailsPage = { navController.navigateToRecordDetails(RecordType.Organization, it) },
                        navigateToOrganizationEditPage = { navController.navigate(RecordEditRoute(RecordType.Organization, it, RecordEditPageMode.NEW)) },
                        navigateToSearchPage = { navController.navigate(SearchRoute(RecordType.Organization.recordTypeId)) },
                        navMenuClick = { navMenuClick() }
                    )
                }




                // #############################################################
                // ###############  RECORD DETAILS AND EDIT  ###################
                // #############################################################

                composable<RecordDetailsRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<RecordDetailsRoute>()
                    val page = recordDetailsPage(route.recordType)
                    if (page == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        val linkedNewRecordTypeId by backStackEntry.savedStateHandle
                            .getStateFlow<Int?>(LINKED_NEW_RECORD_TYPE_ID, null).collectAsState()
                        page(navController.recordDetailsNavigation(route.recordType), linkedNewRecordTypeId) {
                            backStackEntry.savedStateHandle[LINKED_NEW_RECORD_TYPE_ID] = null
                        }
                    }
                }

                composable<RecordEditRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<RecordEditRoute>()
                    val page = recordEditPage(route.recordType)
                    if (page == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        page(route.pageMode, navController.recordEditNavigation(route.recordType))
                    }
                }

                // ---------------------------------------------------------------------------------


                //
                // ********** LINK RECORD PAGE **********
                //
                composable<LinkRecordsRoute> {
                    val route = it.toRoute<LinkRecordsRoute>()
                    LinkRecordsPage(
                        onNavMenuClick = { navController.popBackStack()},
                        navigateToNewLinkedRecordPage = { type, name ->
                            navController.navigateToNewLinkedRecord(type, route.sourceRecordType, route.sourceRecordId, name)
                        },
                    )
                }




                //
                // ********** SEARCH PAGE **********
                //
                composable<SearchRoute> {
                    SearchPage(
                        onBack = { navController.popBackStack() },
                        navigateToRecordDetailsPage = { type, id -> navController.navigateToRecordDetails(type, id) },
                        navigateToNewRecordPage = { type, name -> navController.navigateToNewRecord(type, name) },
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

