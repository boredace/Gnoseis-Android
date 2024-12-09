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

package org.gnoseis.ui.item

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.IconTextPosition
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.category.CategoryList
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.contact.ContactList
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.DeleteIcon
import org.gnoseis.ui.icons.EditIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.icons.QuestionIcon
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.note.NoteList
import org.gnoseis.ui.organization.OrganizationList
import org.gnoseis.ui.theme.GnoseisTheme


object ItemDetailsPageDestination : NavigationDestination {
    override val route = "item_details_page"
    override val titleRes = -9
    const val itemIdArg = "itemId"
    val routeWithArgs = "$route/{$itemIdArg}"
}


@Composable
fun ItemDetailsPage(
    pageViewModel: ItemDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToItemEditPage: (String) -> Unit,
) {
    val pageState by pageViewModel.itemDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()

    ItemDetailsScaffold(
        item = pageState.item?: Item(),
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        onNavMenuclick = navMenuClick,
        onDeleteRecord = {
            pageViewModel.deleteItem()
            navMenuClick() },
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
        navigateToContactDetailsPage = navigateToContactDetailsPage,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToItemEditPage = navigateToItemEditPage,
        )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScaffold(
    item: Item,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  ItemDetailsViewModel.ItemLinkedRecords,
    onDeleteRecord: () -> Unit,
    onNavMenuclick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToItemEditPage: (String) -> Unit,
    ){
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            // TODO: Do something
            delay(1500)
            pullRefreshState.endRefresh()
        }
    }
    var showDeleteAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text(text=item.itemName?:"") },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = { onNavMenuclick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteAlertDialog = true }
                    ) {
                        DeleteIcon(colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
                    }
                    IconButton(
                        onClick = { navigateToItemEditPage(item.id) }
                    ) {
                        EditIcon(colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigateToLinkRecordsPage(item.id) }
            ) {
                Icon(Icons.Filled.AddLink, "Link Records")
                Spacer(modifier = Modifier.width(15.dp))
                Text(text = "Link Records")
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                ItemDetailsBody(
                    item = item,
                    linkedRecordTabs = linkedRecordTabs,
                    linkedRecords = linkedRecords,
                    showDeleteAlertDialog = showDeleteAlertDialog,
                    onDeleteAlertDialogConfirm = {
                        onDeleteRecord()
                        showDeleteAlertDialog = false
                    },
                    onDeleteAlertDialogDismiss = { showDeleteAlertDialog = false },  navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
                    navigateToContactDetailsPage = navigateToContactDetailsPage,
                    navigateToNoteDetailsPage = navigateToNoteDetailsPage,
                    navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemDetailsBody(
    item: Item,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  ItemDetailsViewModel.ItemLinkedRecords,
    showDeleteAlertDialog: Boolean,
    onDeleteAlertDialogDismiss: () -> Unit,
    onDeleteAlertDialogConfirm: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit
) {
    if(showDeleteAlertDialog) {
        DeleteRecordAlertDialog(
            recordTypeName = "Category",
            onDismiss = onDeleteAlertDialogDismiss,
            onConfirm = onDeleteAlertDialogConfirm,
            linkedRecordCount =
                linkedRecords.linkedContacts.count() +
                linkedRecords.linkedNotes.count() +
                linkedRecords.linkedCategories.count() +
                linkedRecords.linkedOrganizations.count()
        )
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { linkedRecordTabs.size + 1 }

    Column() {
        Row(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 15.dp, end = 15.dp, bottom = 5.dp)
            ) {
                /*ItemHeader(
                    title = item.itemName?:"",
        //            createdDateTime = note.createDateTime
                )*/
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    TabRow(
                        selectedTabIndex = selectedTabIndex
                    ) {
                        val align =
                            if (linkedRecordTabs.size > 3) IconTextPosition.Bottom
                            else IconTextPosition.Right
                        Tab(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.surfaceContainer),
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            icon = {
                                ItemIcon("Details", align)
                            }
                        )
                        linkedRecordTabs.forEachIndexed { index, item ->
                            val text = "(${item.count})"
                            Tab(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surfaceContainer),
                                selected = index == selectedTabIndex + 1,
                                onClick = { selectedTabIndex = index + 1 },
                                icon = {
                                    when (item.recordTypeId) {
                                        RecordType.Category.recordTypeId -> CategoryIcon(text, align)
                                        RecordType.Contact.recordTypeId -> ContactIcon(text, align)
                                        RecordType.Item.recordTypeId -> ItemIcon(text, align)
                                        RecordType.Note.recordTypeId -> NoteIcon(text, align)
                                        RecordType.Organization.recordTypeId -> OrganizationIcon(text, align)
                                        else -> QuestionIcon()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 15.dp, end = 15.dp, bottom = 5.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))

            Box(){
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
//                        .fillMaxSize()
                ){index ->
                    Box(
//                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ){
                        Log.i("TABS", "selectedTabIndex = ${selectedTabIndex}")
                        when (selectedTabIndex) {
                            0 -> {
                                if(!item.comments.isNullOrEmpty()) {
                                    Box(modifier=Modifier.verticalScroll(rememberScrollState())){
                                        Text(text=item.comments?:"")
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize(),
                                    ){
                                        Text(text="This Item has no details")
                                    }
                                }
                            }
                            else ->
                            {
                                val selectedRecordTypeId = linkedRecordTabs[selectedTabIndex-1].recordTypeId
                                when(selectedRecordTypeId) {
                                    1-> {
                                        NoteList(
                                            notes = linkedRecords.linkedNotes,
                                            navigateToNoteDetailsPage = navigateToNoteDetailsPage)
                                    }
                                    2 -> {
                                        ContactList(
                                            contacts = linkedRecords.linkedContacts,
                                            navigateToContactDetailsPage = navigateToContactDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    3 -> {
                                        OrganizationList(
                                            organizations = linkedRecords.linkedOrganizations,
                                            navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    4-> {
                                        CategoryList(
                                            categories = linkedRecords.linkedCategories,
                                            navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    else -> { Text(text="Record type ID = ${selectedRecordTypeId}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemHeader(
    title: String,
//    createdDateTime: Long
){
//    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(createdDateTime), ZoneId.systemDefault())

    Text(
        maxLines = 2,
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        text= title
    )
    Spacer(modifier = Modifier.height(5.dp))
    /*Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        )
        Text(
            text = zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        )
    }*/
}


@Composable
fun RelatedItemTag(
    title: String,
    textColour: Color,
    backgroundColour: Color
){
    Box(
        modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.dp)
    ){
        ElevatedCard(modifier = Modifier
//        .background(backgroundColour)
            ,
            onClick = {},
            shape =  RoundedCornerShape(5.dp)
        ){
            Text(
                modifier = Modifier
                    .background(backgroundColour)
                    .padding(horizontal = 15.dp, vertical = 5.dp)
                ,
                color = textColour,
                text = "Personal")
        }
    }

}
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ItemDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            ItemDetailsScaffold(
                item = Item(
                    ownerDbId = "xxx",
                    itemName = "Tesla Model Y - AWD",
                    comments = "This is my newest Model Y entry"
                ),
                linkedRecords = ItemDetailsViewModel.ItemLinkedRecords(),
                onNavMenuclick = {},
                onDeleteRecord = {},
                navigateToNoteDetailsPage = {},
                navigateToOrganizationDetailsPage = {},
                navigateToContactDetailsPage = {},
                navigateToCategoryDetailsPage = {},
                navigateToLinkRecordsPage = {},
                navigateToItemEditPage = {},
                linkedRecordTabs = listOf(
                    LinkedRecordTypeCount(
                        recordTypeId = 1,
                        count = 3
                    )
                )
            )
        }
    }
}