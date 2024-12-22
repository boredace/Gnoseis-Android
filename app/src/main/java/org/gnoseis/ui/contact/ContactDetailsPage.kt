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

package org.gnoseis.ui.contact

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.category.CategoryList
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.components.ExpandableActionButton
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.DeleteIcon
import org.gnoseis.ui.icons.EditIcon
import org.gnoseis.ui.icons.EmailIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.icons.PhoneIcon
import org.gnoseis.ui.icons.QuestionIcon
import org.gnoseis.ui.item.ItemList
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.note.NoteList
import org.gnoseis.ui.organization.OrganizationList
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class ContactDetailsRoute(
    val contactId: String
)

@Composable
fun ContactDetailsPage(
    pageViewModel: ContactDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToContactEditPage: (String) -> Unit,

) {
    val pageState by pageViewModel.contactDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()

    ContactDetailsScaffold(
        contact = pageState.contact?: Contact(),
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        onDeleteRecord = {
            pageViewModel.deleteContact()
            navMenuClick() },
        onNavMenuclick = navMenuClick,
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
        navigateToItemDetailsPage = navigateToItemDetailsPage,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToLinkNewNotePage = navigateToLinkNewNotePage,
        navigateToLinkNewCategoryPage = navigateToLinkNewCategoryPage,
        navigateToLinkNewItemPage = navigateToLinkNewItemPage,
        navigateToLinkNewOrganizationPage = navigateToLinkNewOrganizationPage,
        navigateToContactEditPage = navigateToContactEditPage,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScaffold(
    contact: Contact,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  ContactDetailsViewModel.ContactLinkedRecords,
    onDeleteRecord: () -> Unit,
    onNavMenuclick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToContactEditPage: (String) -> Unit,
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
                title = { Text(text="Contact") },
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
                        onClick = {
                            showDeleteAlertDialog = true
                        }
                    ) {
                        DeleteIcon(colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
                    }
                    IconButton(
                        onClick = { navigateToContactEditPage(contact.id) }
                    ) {
                        EditIcon(colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface))
                    }
                }
            )
        },
        floatingActionButton = {
            Column() {
                ExpandableActionButton(
                    isExpanded = fabExpanded,
                    fabIcon = Icons.Filled.AddLink,
                    fabText = "Link Existing",
                    onFabClick = {
                        if (fabExpanded) {
                            fabExpanded = false
                            navigateToLinkRecordsPage(contact.id)
                        } else {
                            fabExpanded = true
                        }
                    },
                    fab1Icon = ImageVector.vectorResource(R.drawable.outline_description_24),
                    fab1Text = "New Note",
                    onFab1Click = { navigateToLinkNewNotePage(contact.id) },
                    fab2Icon = ImageVector.vectorResource(R.drawable.outline_deployed_code_24),
                    fab2Text = "New Item",
                    onFab2Click = { navigateToLinkNewItemPage(contact.id) },
                    fab3Icon = ImageVector.vectorResource(R.drawable.outline_label_24),
                    fab3Text = "New Category",
                    onFab3Click = { navigateToLinkNewCategoryPage(contact.id) },
                    fab4Icon = ImageVector.vectorResource(R.drawable.baseline_business_24),
                    fab4Text = "New Organization",
                    onFab4Click = { navigateToLinkNewOrganizationPage(contact.id) },
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                ContactDetailsBody(
                    contact = contact,
                    linkedRecordTabs = linkedRecordTabs,
                    linkedRecords = linkedRecords,
                    showDeleteAlertDialog = showDeleteAlertDialog,
                    onDeleteAlertDialogConfirm = {
                        onDeleteRecord()
                        showDeleteAlertDialog = false
                    },
                    onDeleteAlertDialogDismiss = { showDeleteAlertDialog = false },
                    navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
                    navigateToItemDetailsPage = navigateToItemDetailsPage,
                    navigateToNoteDetailsPage = navigateToNoteDetailsPage,
                    navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailsBody(
    contact: Contact,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  ContactDetailsViewModel.ContactLinkedRecords,
    showDeleteAlertDialog: Boolean,
    onDeleteAlertDialogDismiss: () -> Unit,
    onDeleteAlertDialogConfirm: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
) {

    if(showDeleteAlertDialog) {
        DeleteRecordAlertDialog(
            recordTypeName = "Contact",
            onDismiss = onDeleteAlertDialogDismiss,
            onConfirm = onDeleteAlertDialogConfirm,
            linkedRecordCount =
                linkedRecords.linkedCategories.count() +
                linkedRecords.linkedItems.count() +
                linkedRecords.linkedNotes.count() +
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
                ContactHeader(
                    title = "${contact.nameLast}, ${contact.nameFirst}",
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
                            0 ->  ContactDetailsTabContent(contact = contact)
                            else ->
                            {
                                val selectedRecordTypeId = linkedRecordTabs[selectedTabIndex-1].recordTypeId
                                when(selectedRecordTypeId) {
                                    1-> {
                                        NoteList(
                                            notes = linkedRecords.linkedNotes,
                                            navigateToNoteDetailsPage = navigateToNoteDetailsPage)
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
                                    5 -> {
                                        ItemList(
                                            items = linkedRecords.linkedItems,
                                            navigateToItemDetailsPage = navigateToItemDetailsPage,
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


            /*Row(){
                RelatedItemTag(
                    title = "Personal",
                    textColour = Color.White,
                    backgroundColour = Color.Red
                )
                RelatedItemTag(
                    title = "Personal",
                    textColour = Color.White,
                    backgroundColour = Color.Black
                )
            }*/

        }



    }

}

@Composable
fun ContactHeader(
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
fun ContactDetailsTabContent(
    contact: Contact = Contact()
) {
    Column {
        var displayedSections = mutableStateOf(0)

        if (contact.jobTitle != null || contact.company != null) {
            displayedSections.value++
            Column() {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactDetailsFieldHeder(text = "Company", icon = OrganizationIcon())
                }
                if (contact.company != null) {
                    ContactDetailsField(contact.company, "Company")
                }
                if (contact.jobTitle != null) {
                    ContactDetailsField(contact.jobTitle, "Job Title")
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (contact.phoneHome != null ||
            contact.phoneMain != null ||
            contact.phoneMobile != null ||
            contact.phoneWork != null
        ) {
            displayedSections.value++
            Column() {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactDetailsFieldHeder(text = "Phone", icon = PhoneIcon())
                }
                if (contact.phoneMain != null) {
                    ContactDetailsField(contact.phoneMain, "Main")
                }
                if (contact.phoneWork != null) {
                    ContactDetailsField(contact.phoneWork, "Work")
                }
                if (contact.phoneHome != null) {
                    ContactDetailsField(contact.phoneHome, "Home")
                }
                if (contact.phoneMobile != null) {
                    ContactDetailsField(contact.phoneMobile, "Mobile")
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (contact.emailHome != null ||
            contact.emailMain != null ||
            contact.emailMobile != null ||
            contact.emailWork != null
        ) {
            displayedSections.value++
            Column() {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactDetailsFieldHeder(text = "Email", icon = EmailIcon())
                }
                if (contact.emailMain != null) {
                    ContactDetailsField(contact.emailMain, "Main")
                }
                if (contact.emailWork != null) {
                    ContactDetailsField(contact.emailWork, "Work")
                }
                if (contact.emailHome != null) {
                    ContactDetailsField(contact.emailHome, "Home")
                }
                if (contact.emailMobile != null) {
                    ContactDetailsField(contact.emailMobile, "Mobile")
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (contact.comments != null) {
            displayedSections.value++
            if(displayedSections.value > 1) {
                HorizontalDivider(Modifier.padding(top =10.dp, bottom = 10.dp))
//                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = contact.comments,
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        if (displayedSections.value == 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ){
                Text(text="This Contact has no details")
            }
        }
    }
}

@Composable
fun ContactDetailsFieldHeder(
    text : String,
    icon : Unit
){
    Box(
        contentAlignment = Alignment.Center
    ) {
        icon
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary
    )
}
@Composable
fun ContactDetailsField(
    text : String?,
    title: String
){
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(35.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = text ?: "",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ContactDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            ContactDetailsScaffold(
                contact = Contact(
                    ownerDbId = "xxx",
                    nameLast = "Shmoebargansky",
                    nameFirst = "Joseph Jr.",
//                    company = "Anaconda Corporation Ltd.",
//                    jobTitle = "Executive Director",
//                    phoneMain = "+1-647-919-9999",
//                    phoneWork = "800-CALL-CIA",
//                    phoneHome = "505-123-3333",
//                    phoneMobile = "202-843-8800",
//                    emailMain = "joe@shome.com",
//                    emailHome = "joe3333@hotmail.com",
//                    emailMobile = "wtfismobileemail@wtf.com",
//                    emailWork = "joseph.shmoebarganofsky@anaconda.ca",
//                    comments = "This guy is an incredible salesperson, he was able to sell" +
//                            " widget to a prospect who didnt need any widgets. As a matter of " +
//                            "fact didnt even know what widgets are..."
                ),
                linkedRecords = ContactDetailsViewModel.ContactLinkedRecords(),
                onNavMenuclick = {},
                onDeleteRecord = {},
                navigateToContactEditPage = {},
                navigateToNoteDetailsPage = {},
                navigateToCategoryDetailsPage = {},
                navigateToItemDetailsPage = {},
                navigateToOrganizationDetailsPage = {},
                navigateToLinkRecordsPage = {},
                navigateToLinkNewNotePage = {},
                navigateToLinkNewItemPage = {},
                navigateToLinkNewOrganizationPage = {},
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