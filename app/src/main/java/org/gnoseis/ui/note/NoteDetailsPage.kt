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

package org.gnoseis.ui.note

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.R
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.components.ExpandableActionButton
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.LinkedRecordsSection
import org.gnoseis.ui.record.LinkedRecordsTabStyle
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


@Serializable
data class NoteDetailsRoute(
    val noteId: String
)

@Composable
fun NoteDetailsPage(
    pageViewModel: NoteDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToNoteEditPage: (String) -> Unit,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.noteDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()

    NoteDetailsScaffold(
        note = pageState.note?: Note(),
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        onDeleteRecord = {
            pageViewModel.deleteNote()
           navMenuClick() },
        onNavMenuclick = navMenuClick,
        navigateToCategoryDetailsPage = navigateToCategoryDetailsPage,
        navigateToContactDetailsPage = navigateToContactDetailsPage,
        navigateToItemDetailsPage = navigateToItemDetailsPage,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToLinkNewContactPage = navigateToLinkNewContactPage,
        navigateToLinkNewCategoryPage = navigateToLinkNewCategoryPage,
        navigateToLinkNewItemPage = navigateToLinkNewItemPage,
        navigateToLinkNewOrganizationPage = navigateToLinkNewOrganizationPage,
        navigateToNoteEditPage = navigateToNoteEditPage,
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsScaffold(
    note: Note,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  LinkedRecords,
    onDeleteRecord: () -> Unit,
    onNavMenuclick: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewCategoryPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToNoteEditPage: (String) -> Unit,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
){
    var fabExpanded by remember { mutableStateOf(false)}
    var showDeleteAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .alpha(if(fabExpanded) 0.1f else 1f),
                title = { Text(text="Note") },
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
                        Image(
                            painter = painterResource(id = R.drawable.outline_delete_24),
                            contentDescription = "Delete Note",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    IconButton(
                        onClick = { navigateToNoteEditPage(note.id) }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.outline_edit_24),
                            contentDescription = "Edit Note",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
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
                            navigateToLinkRecordsPage(note.id)
                        } else {
                            fabExpanded = true
                        }
                    },
                    fab1Icon = ImageVector.vectorResource(R.drawable.outline_people_24),
                    fab1Text = "New Contact",
                    onFab1Click = { navigateToLinkNewContactPage(note.id) },
                    fab2Icon = ImageVector.vectorResource(R.drawable.outline_deployed_code_24),
                    fab2Text = "New Item",
                    onFab2Click = { navigateToLinkNewItemPage(note.id) },
                    fab3Icon = ImageVector.vectorResource(R.drawable.outline_label_24),
                    fab3Text = "New Category",
                    onFab3Click = { navigateToLinkNewCategoryPage(note.id) },
                    fab4Icon = ImageVector.vectorResource(R.drawable.baseline_business_24),
                    fab4Text = "New Organization",
                    onFab4Click = { navigateToLinkNewOrganizationPage(note.id) },
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                NoteDetailsBody(
                    note = note,
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
                    navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
                    linkedNewRecordTypeId = linkedNewRecordTypeId,
                    onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
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
fun NoteDetailsBody(
    note: Note,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  LinkedRecords,
    showDeleteAlertDialog: Boolean,
    onDeleteAlertDialogDismiss: () -> Unit,
    onDeleteAlertDialogConfirm: () -> Unit,
    navigateToCategoryDetailsPage: (String) -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {

    if(showDeleteAlertDialog) {
        DeleteRecordAlertDialog(
            recordTypeName = "Note",
            onDismiss = onDeleteAlertDialogDismiss,
            onConfirm = onDeleteAlertDialogConfirm,
            linkedRecordCount =
                linkedRecords.linkedContacts.count() +
                linkedRecords.linkedCategories.count() +
                linkedRecords.linkedItems.count() +
                linkedRecords.linkedOrganizations.count()
        )
    }
    LinkedRecordsSection(
        recordType = RecordType.Note,
        linkedRecordTabs = linkedRecordTabs,
        linkedRecords = linkedRecords,
        onOpenRecord = { type, id ->
            when (type) {
                RecordType.Category -> navigateToCategoryDetailsPage(id)
                RecordType.Contact -> navigateToContactDetailsPage(id)
                RecordType.Item -> navigateToItemDetailsPage(id)
                RecordType.Organization -> navigateToOrganizationDetailsPage(id)
                else -> Unit
            }
        },
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        tabStyle = LinkedRecordsTabStyle.LabelBelow,
        header = { NoteHeader(title = note.title ?: "", createdDateTime = note.createDateTime) },
    ) {
        NoteDetailsTab(note)
    }
}

/** A note's Details tab: its text, or a line saying it has none. */
@Composable
fun NoteDetailsTab(note: Note) {
    if (!note.textContents.isNullOrEmpty()) {
        Box(modifier = Modifier.detailsTabPadding().verticalScroll(rememberScrollState())) {
            Text(text = note.textContents)
        }
    } else {
        RecordDetailsEmpty(text = "This Note has no content", modifier = Modifier.detailsTabPadding())
    }
}

@Composable
fun NoteHeader(
    title: String,
    createdDateTime: Long
){
//    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(createdDateTime), ZoneId.systemDefault())
    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(createdDateTime), ZoneId.of("GMT"))

    if(!title.isNullOrEmpty()) {
        Text(
            maxLines = 2,
            style = MaterialTheme.typography.headlineSmall,
            overflow = TextOverflow.Ellipsis,
            text = title
        )
        Spacer(modifier = Modifier.height(5.dp))
    }
    Row(
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
    }
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
fun NoteDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            NoteDetailsScaffold(
                note = Note(
                    ownerDbId = "xxx",
                    title = "Welcome to the Jungle, this is a very long text! And now I am going to make it even longer?",
                    textContents = "This will be amazing adventure, are you ready?"
                ),
                onDeleteRecord = {},
                onNavMenuclick = {},
                navigateToCategoryDetailsPage = {},
                navigateToItemDetailsPage = {},
                navigateToContactDetailsPage = {},
                navigateToOrganizationDetailsPage = {},
                navigateToLinkRecordsPage = {},
                navigateToNoteEditPage = {},
                linkedRecordTabs = listOf(
                    LinkedRecordTypeCount(
                        recordTypeId = 1,
                        count = 3
                    ),
                    LinkedRecordTypeCount(
                        recordTypeId = 3,
                        count = 17
                    ),
                    LinkedRecordTypeCount(
                        recordTypeId = 2,
                        count = 1
                    )
                ),
                linkedRecords = LinkedRecords(),
                navigateToLinkNewItemPage = {},
                navigateToLinkNewOrganizationPage = {},
                navigateToLinkNewCategoryPage = {},
                navigateToLinkNewContactPage = {},
            )
        }
    }
}