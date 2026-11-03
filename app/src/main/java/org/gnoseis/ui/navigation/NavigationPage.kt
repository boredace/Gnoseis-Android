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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import org.gnoseis.ui.category.CategoryListRoute
import org.gnoseis.ui.contact.ContactDetailsPage
import org.gnoseis.ui.contact.ContactDetailsRoute
import org.gnoseis.ui.contact.ContactEditPage
import org.gnoseis.ui.contact.ContactEditRoute
import org.gnoseis.ui.contact.ContactListPage
import org.gnoseis.ui.contact.ContactListRoute
import org.gnoseis.ui.home.HomePage
import org.gnoseis.ui.home.HomeRoute
import org.gnoseis.ui.item.ItemDetailsPage
import org.gnoseis.ui.item.ItemDetailsRoute
import org.gnoseis.ui.item.ItemEditPage
import org.gnoseis.ui.item.ItemEditRoute
import org.gnoseis.ui.item.ItemListPage
import org.gnoseis.ui.item.ItemListRoute
import org.gnoseis.ui.link.LinkRecordsRoute
import org.gnoseis.ui.link.LinkRecordsPage
import org.gnoseis.ui.note.NoteDetailsPage
import org.gnoseis.ui.note.NoteDetailsRoute
import org.gnoseis.ui.note.NoteEditPage
import org.gnoseis.ui.note.NoteEditRoute
import org.gnoseis.ui.note.NoteListPage
import org.gnoseis.ui.note.NoteListRoute
import org.gnoseis.ui.organization.OrganizationDetailsPage
import org.gnoseis.ui.organization.OrganizationDetailsRoute
import org.gnoseis.ui.organization.OrganizationEditPage
import org.gnoseis.ui.organization.OrganizationEditRoute
import org.gnoseis.ui.organization.OrganizationListPage
import org.gnoseis.ui.organization.OrganizationListRoute
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordEditNavigation
import org.gnoseis.ui.search.SearchPage
import org.gnoseis.ui.search.SearchPageDestination
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
    previousBackStackEntry?.savedStateHandle?.set(LINKED_NEW_RECORD_TYPE_ID, recordType.recordTypeId)
    popBackStack()
}

/** Opens the Details page of the record [id] of [type]. */
private fun NavController.navigateToRecordDetails(type: RecordType, id: String) {
    when (type) {
        RecordType.Category -> navigate(CategoryDetailsRoute(id))
        RecordType.Contact -> navigate(ContactDetailsRoute(id))
        RecordType.Item -> navigate(ItemDetailsRoute(id))
        RecordType.Note -> navigate(NoteDetailsRoute(id))
        RecordType.Organization -> navigate(OrganizationDetailsRoute(id))
        RecordType.LinkedRecord -> Unit
    }
}

/** Opens the Edit page of the record [id] of [type]. */
private fun NavController.navigateToRecordEdit(type: RecordType, id: String) {
    when (type) {
        RecordType.Category -> navigate(CategoryEditRoute(id, CategoryEditPageMode.EDIT))
        RecordType.Contact -> navigate(ContactEditRoute(id, ContactEditPageMode.EDIT))
        RecordType.Item -> navigate(ItemEditRoute(id, ItemEditPageMode.EDIT))
        RecordType.Note -> navigate(NoteEditRoute(id, NoteEditPageMode.EDIT))
        RecordType.Organization -> navigate(OrganizationEditRoute(id, OrganizationEditPageMode.EDIT))
        RecordType.LinkedRecord -> Unit
    }
}

/**
 * Opens the Edit page for a new record of [type], to be linked to [sourceId] of [sourceType].
 * With [prefillName], the record's name starts as that (#10).
 */
private fun NavController.navigateToNewLinkedRecord(type: RecordType, sourceType: RecordType, sourceId: String, prefillName: String? = null) {
    when (type) {
        RecordType.Category -> navigate(CategoryEditRoute(null, CategoryEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
        RecordType.Contact -> navigate(ContactEditRoute(null, ContactEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
        RecordType.Item -> navigate(ItemEditRoute(null, ItemEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
        RecordType.Note -> navigate(NoteEditRoute(null, NoteEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
        RecordType.Organization -> navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEWLINK, sourceType, sourceId, prefillName))
        RecordType.LinkedRecord -> Unit
    }
}

/** #10: opens the Edit page for a new record of [type] named [prefillName]. */
private fun NavController.navigateToNewRecord(type: RecordType, prefillName: String) {
    when (type) {
        RecordType.Category -> navigate(CategoryEditRoute(null, CategoryEditPageMode.NEW, prefillName = prefillName))
        RecordType.Contact -> navigate(ContactEditRoute(null, ContactEditPageMode.NEW, prefillName = prefillName))
        RecordType.Item -> navigate(ItemEditRoute(null, ItemEditPageMode.NEW, prefillName = prefillName))
        RecordType.Note -> navigate(NoteEditRoute(null, NoteEditPageMode.NEW, prefillName = prefillName))
        RecordType.Organization -> navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEW, prefillName = prefillName))
        RecordType.LinkedRecord -> Unit
    }
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

/**
 * The Details page destination for route [R], a record of [type]. [page] gets the page's
 * navigation and the #6 linked-new-record tab handling.
 */
private inline fun <reified R : Any> NavGraphBuilder.recordDetailsDestination(
    navController: NavController,
    type: RecordType,
    crossinline page: @Composable (
        navigation: RecordDetailsNavigation,
        linkedNewRecordTypeId: Int?,
        onLinkedNewRecordTypeShown: () -> Unit,
    ) -> Unit,
) {
    composable<R> { backStackEntry ->
        val linkedNewRecordTypeId by backStackEntry.savedStateHandle
            .getStateFlow<Int?>(LINKED_NEW_RECORD_TYPE_ID, null).collectAsState()
        page(navController.recordDetailsNavigation(type), linkedNewRecordTypeId) {
            backStackEntry.savedStateHandle[LINKED_NEW_RECORD_TYPE_ID] = null
        }
    }
}

/** The Edit page destination for route [R], a record of [type]. [page] gets the route and the page's navigation. */
private inline fun <reified R : Any> NavGraphBuilder.recordEditDestination(
    navController: NavController,
    type: RecordType,
    crossinline page: @Composable (route: R, navigation: RecordEditNavigation) -> Unit,
) {
    composable<R> { backStackEntry ->
        page(backStackEntry.toRoute<R>(), navController.recordEditNavigation(type))
    }
}

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
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navigateToRecordDetailsPage = { type, id -> navController.navigateToRecordDetails(type, id) },
                        navigateToNewRecordPage = { type ->
                            when (type) {
                                RecordType.Category -> navController.navigate(CategoryEditRoute(null, CategoryEditPageMode.NEW))
                                RecordType.Contact -> navController.navigate(ContactEditRoute(null, ContactEditPageMode.NEW))
                                RecordType.Item -> navController.navigate(ItemEditRoute(null, ItemEditPageMode.NEW))
                                RecordType.Note -> navController.navigate(NoteEditRoute(null, NoteEditPageMode.NEW))
                                RecordType.Organization -> navController.navigate(OrganizationEditRoute(null, OrganizationEditPageMode.NEW))
                                RecordType.LinkedRecord -> Unit
                            }
                        },
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
                        navigateToCategoryDetailsPage = { navController.navigate(CategoryDetailsRoute(it)) },
                        navigateToCategoryEditPage = { navController.navigate(CategoryEditRoute(it, CategoryEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                recordDetailsDestination<CategoryDetailsRoute>(navController, RecordType.Category) { navigation, linkedNewRecordTypeId, onLinkedNewRecordTypeShown ->
                    CategoryDetailsPage(
                        navigation = navigation,
                        linkedNewRecordTypeId = linkedNewRecordTypeId,
                        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    )
                }

                recordEditDestination<CategoryEditRoute>(navController, RecordType.Category) { route, navigation ->
                    CategoryEditPage(pageMode = route.pageMode, navigation = navigation)
                }

                // #############################################################
                // ##################  CONTACT   ###############################
                // #############################################################

                composable<ContactListRoute> {
                    val args = it.toRoute<ContactListRoute>()
                    ContactListPage(
                        navigateToContactDetailsPage = { navController.navigate(ContactDetailsRoute(it)) },
                        navigateToContactEditPage = { navController.navigate(ContactEditRoute(it, ContactEditPageMode.NEW))},
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                recordDetailsDestination<ContactDetailsRoute>(navController, RecordType.Contact) { navigation, linkedNewRecordTypeId, onLinkedNewRecordTypeShown ->
                    ContactDetailsPage(
                        navigation = navigation,
                        linkedNewRecordTypeId = linkedNewRecordTypeId,
                        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    )
                }

                recordEditDestination<ContactEditRoute>(navController, RecordType.Contact) { route, navigation ->
                    ContactEditPage(pageMode = route.pageMode, navigation = navigation)
                }

                // #############################################################
                // #####################    ITEM    ############################
                // #############################################################

                composable<ItemListRoute> {
                    val args = it.toRoute<ItemListRoute>()
                    ItemListPage(
                        navigateToItemDetailsPage = { navController.navigate(ItemDetailsRoute(it)) },
                        navigateToItemEditPage = { navController.navigate(ItemEditRoute(it, ItemEditPageMode.NEW)) },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                recordDetailsDestination<ItemDetailsRoute>(navController, RecordType.Item) { navigation, linkedNewRecordTypeId, onLinkedNewRecordTypeShown ->
                    ItemDetailsPage(
                        navigation = navigation,
                        linkedNewRecordTypeId = linkedNewRecordTypeId,
                        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    )
                }

                recordEditDestination<ItemEditRoute>(navController, RecordType.Item) { route, navigation ->
                    ItemEditPage(pageMode = route.pageMode, navigation = navigation)
                }

                // #############################################################
                // #####################    NOTE    ############################
                // #############################################################

                composable<NoteListRoute>() {
                    NoteListPage(
                        navigateToNoteDetailsPage = { navController.navigate(NoteDetailsRoute(it)) },
                        navigateToNoteEditPage = {navController.navigate(NoteEditRoute(it, NoteEditPageMode.NEW))},
                        navigateToSearchPage = {navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                recordDetailsDestination<NoteDetailsRoute>(navController, RecordType.Note) { navigation, linkedNewRecordTypeId, onLinkedNewRecordTypeShown ->
                    NoteDetailsPage(
                        navigation = navigation,
                        linkedNewRecordTypeId = linkedNewRecordTypeId,
                        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    )
                }

                recordEditDestination<NoteEditRoute>(navController, RecordType.Note) { route, navigation ->
                    NoteEditPage(pageMode = route.pageMode, navigation = navigation)
                }

                // #############################################################
                // ###################      ORGANIZATION      ##################
                // #############################################################

                composable<OrganizationListRoute> {
                    val args = it.toRoute<OrganizationListRoute>()
                    OrganizationListPage(
                        navigateToOrganizationDetailsPage = { navController.navigate(OrganizationDetailsRoute(it)) },
                        navigateToOrganizationEditPage = { navController.navigate(OrganizationEditRoute(it, OrganizationEditPageMode.NEW)) },
                        navigateToSearchPage = { navController.navigate(SearchPageDestination.route) },
                        navMenuClick = { navMenuClick() }
                    )
                }

                recordDetailsDestination<OrganizationDetailsRoute>(navController, RecordType.Organization) { navigation, linkedNewRecordTypeId, onLinkedNewRecordTypeShown ->
                    OrganizationDetailsPage(
                        navigation = navigation,
                        linkedNewRecordTypeId = linkedNewRecordTypeId,
                        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    )
                }

                recordEditDestination<OrganizationEditRoute>(navController, RecordType.Organization) { route, navigation ->
                    OrganizationEditPage(pageMode = route.pageMode, navigation = navigation)
                }



                // ---------------------------------------------------------------------------------


                //
                // ********** LINK RECORD PAGE **********
                //
                composable<LinkRecordsRoute> {
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
                        navigateToOrganizationDetailsPage = { navController.navigate(OrganizationDetailsRoute(it)) },
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

