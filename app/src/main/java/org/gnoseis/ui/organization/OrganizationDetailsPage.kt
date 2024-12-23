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
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.R
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.category.CategoryList
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.components.ExpandableActionButton
import org.gnoseis.ui.contact.ContactList
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.DeleteIcon
import org.gnoseis.ui.icons.EditIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.icons.QuestionIcon
import org.gnoseis.ui.item.ItemList
import org.gnoseis.ui.note.NoteList
import org.gnoseis.ui.theme.GnoseisTheme


@Serializable
data class OrganizationDetailsRoute(
    val organizationId: String
)

@Composable
fun OrganizationDetailsPage(
    pageViewModel: OrganizationDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToOrganizationEditPage: (String) -> Unit,

) {
    val pageState by pageViewModel.organizationDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()

    OrganizationDetailsScaffold(
        organization = pageState.organization?: Organization(),
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        onDeleteRecord = {
            pageViewModel.deleteOrganization()
            navMenuClick() },
        onNavMenuclick = navMenuClick,
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
        navigateToContactDetailsPage = navigateToContactDetailsPage,
        navigateToItemDetailsPage = navigateToItemDetailsPage,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToLinkNewNotePage = navigateToLinkNewNotePage,
        navigateToLinkNewCategoryPage = navigateToLinkNewCategoryPage,
        navigateToLinkNewContactPage = navigateToLinkNewContactPage,
        navigateToLinkNewItemPage = navigateToLinkNewItemPage,
        navigateToOrganizationEditPage = navigateToOrganizationEditPage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationDetailsScaffold(
    organization: Organization,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  OrganizationDetailsViewModel.OrganizationLinkedRecords,
    onDeleteRecord: () -> Unit,
    onNavMenuclick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToOrganizationEditPage: (String) -> Unit,


    ){
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            // TODO: Do something
            delay(1500)
            pullRefreshState.endRefresh()
        }
    }
    var fabExpanded by remember { mutableStateOf(false)}
    var showDeleteAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {

            TopAppBar(
                modifier = Modifier
                    .alpha(if(fabExpanded) 0.1f else 1f),
                title = { Text(text="Organization") },
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
                        onClick = { navigateToOrganizationEditPage(organization.id) }
                    ) {
                        EditIcon(colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
                    }
                }
            )
        },
        floatingActionButton = {
            Column() {
                ExpandableActionButton(
                    isExpanded =  fabExpanded,
                    fabIcon = Icons.Filled.AddLink,
                    fabText = "Link Existing",
                    onFabClick = {
                        if (fabExpanded) {
                            fabExpanded = false
                            navigateToLinkRecordsPage(organization.id)
                        } else {
                            fabExpanded = true
                        }
                    },
                    fab1Icon = ImageVector.vectorResource(R.drawable.outline_description_24),
                    fab1Text = "New Note",
                    onFab1Click = { navigateToLinkNewNotePage(organization.id) },
                    fab2Icon = ImageVector.vectorResource(R.drawable.outline_people_24),
                    fab2Text = "New Contact",
                    onFab2Click = { navigateToLinkNewContactPage(organization.id) },
                    fab3Icon = ImageVector.vectorResource(R.drawable.outline_label_24),
                    fab3Text = "New Category",
                    onFab3Click = { navigateToLinkNewCategoryPage(organization.id) },
                    fab4Icon = ImageVector.vectorResource(R.drawable.outline_deployed_code_24),
                    fab4Text = "New Item",
                    onFab4Click = { navigateToLinkNewItemPage(organization.id) },
                )
                /*ExtendedFloatingActionButton(
                    onClick = { navigateToLinkRecordsPage(organization.id) }
                ) {
                    Icon(Icons.Filled.AddLink, "Link Records")
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(text = "Link Records")
                }*/
            }


        },
        content = { innerPadding ->

            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
//                    .alpha(if (fabExpanded) 0.1f else 1.0f)
//                    .clickable(
//                        enabled = if(fabExpanded) true else false,
//                        onClick = { fabExpanded = false}
//                    )

//                    .alpha(if(fabExpanded) 0.1f else 1f)
            ) {
                OrganizationDetailsBody(
                    organization = organization,
                    linkedRecordTabs = linkedRecordTabs,
                    linkedRecords = linkedRecords,
                    showDeleteAlertDialog = showDeleteAlertDialog,
                    onDeleteAlertDialogConfirm = {
                        onDeleteRecord()
                        showDeleteAlertDialog = false
                    },
                    onDeleteAlertDialogDismiss = { showDeleteAlertDialog = false },
                    navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
                    navigateToContactDetailsPage = navigateToContactDetailsPage,
                    navigateToItemDetailsPage = navigateToItemDetailsPage,
                    navigateToNoteDetailsPage = navigateToNoteDetailsPage
                )
            }
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize ()
                        .alpha(0.8f)
                        .background(Color.Black)
                        .clickable(
                            enabled = true,
                            onClick = { fabExpanded = false }
                        )
                ) {

                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrganizationDetailsBody(
    organization: Organization,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  OrganizationDetailsViewModel.OrganizationLinkedRecords,
    showDeleteAlertDialog: Boolean,
    onDeleteAlertDialogDismiss: () -> Unit,
    onDeleteAlertDialogConfirm: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
) {
    if(showDeleteAlertDialog) {
        DeleteRecordAlertDialog(
            recordTypeName = "Organization",
            onDismiss = onDeleteAlertDialogDismiss,
            onConfirm = onDeleteAlertDialogConfirm,
            linkedRecordCount =
                linkedRecords.linkedCategories.count() +
                linkedRecords.linkedItems.count() +
                linkedRecords.linkedNotes.count() +
                linkedRecords.linkedContacts.count()
        )
    }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
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
                OrganizationHeader(
                    title = "${organization.organizationName}",
                )
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    TabRow(
                        selectedTabIndex = selectedTabIndex
                    ) {
                        Tab(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.surfaceContainer),
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text(text = "Details") },
                            icon = {
                                Icon(
                                    imageVector =
                                    Icons.Outlined.Notes,
                                    contentDescription = "Details"
                                )
                            }
                        )
                        linkedRecordTabs.forEachIndexed { index, item ->
                            Tab(
                                modifier = Modifier
                                    .background(color = MaterialTheme.colorScheme.surfaceContainer),
                                selected = index == selectedTabIndex + 1,
                                onClick = { selectedTabIndex = index + 1 },
                                text = { Text(text = "(${item.count.toString()})") },
                                icon = {
                                    when (item.recordTypeId) {
                                        RecordType.Category.recordTypeId -> CategoryIcon()
                                        RecordType.Contact.recordTypeId -> ContactIcon()
                                        RecordType.Item.recordTypeId -> ItemIcon()
                                        RecordType.Note.recordTypeId -> NoteIcon()
                                        RecordType.Organization.recordTypeId -> OrganizationIcon()
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
            Box() {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                ) { index ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Log.i("TABS", "selectedTabIndex = ${selectedTabIndex}")
                        when (selectedTabIndex) {
                            0 -> {
                                if(!organization.comments.isNullOrEmpty()) {
                                    Box(modifier=Modifier.verticalScroll(rememberScrollState())){
                                        Text(text=organization.comments?:"")
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize(),
                                    ){
                                        Text(text="Category has no details")
                                    }
                                }
                            }
                            else -> {
                                val selectedRecordTypeId =
                                    linkedRecordTabs[selectedTabIndex - 1].recordTypeId
                                when (selectedRecordTypeId) {
                                    1 -> {
                                        NoteList(
                                            notes = linkedRecords.linkedNotes,
                                            navigateToNoteDetailsPage = navigateToNoteDetailsPage
                                        )
                                    }
                                    2 -> {
                                        ContactList(
                                            contacts = linkedRecords.linkedContacts,
                                            navigateToContactDetailsPage = navigateToContactDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    4 -> {
                                        CategoryList(
                                            categories = linkedRecords.linkedCategories,
                                            navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    5 -> {
                                        ItemList(
                                            items = linkedRecords.linkedItems,
                                            navigateToItemDetailsPage = navigateToItemDetailsPage,
                                            showHeaders = false
                                        )
                                    }
                                    else -> {
                                        Text(text = "Record type ID = ${selectedRecordTypeId}")
                                    }
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
fun OrganizationHeader(
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
fun OrganizationDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            OrganizationDetailsScaffold(
                organization = Organization(
                    ownerDbId = "xxx",
                    organizationName = "Happy Monkeys Inc."
                ),
                linkedRecords = OrganizationDetailsViewModel.OrganizationLinkedRecords(),
                onDeleteRecord = {},
                onNavMenuclick = {},
                navigateToCategoryDetailsPage = {},
                navigateToContactDetailsPage = {},
                navigateToItemDetailsPage = {},
                navigateToNoteDetailsPage = {},
                navigateToLinkRecordsPage = {},
                navigateToLinkNewNotePage = {},
                navigateToLinkNewContactPage = {},
                navigateToOrganizationEditPage = {},
                navigateToLinkNewItemPage = {},
                navigateToLinkNewCategoryPage = {},
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