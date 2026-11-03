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

package org.gnoseis.ui.category

import android.content.res.Configuration
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.R
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.components.ExpandableActionButton
import org.gnoseis.ui.icons.DeleteIcon
import org.gnoseis.ui.icons.EditIcon
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.LinkedRecordsSection
import org.gnoseis.ui.record.LinkedRecordsTabStyle
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class CategoryDetailsRoute(
    val categoryId: String
)

@Composable
fun CategoryDetailsPage(
    pageViewModel: CategoryDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToCategoryEditPage: (String) -> Unit,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.categoryDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()

    CategoryDetailsScaffold(
        category = pageState.category?: Category(),
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        onDeleteRecord = {
             pageViewModel.deleteCategory()
            navMenuClick() },
        onNavMenuclick = navMenuClick,
        navigateToContactDetailsPage = navigateToContactDetailsPage,
        navigateToItemDetailsPage = navigateToItemDetailsPage,
        navigateToNoteDetailsPage = navigateToNoteDetailsPage,
        navigateToOrganizationDetailsPage = navigateToOrganizationDetailsPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToLinkNewNotePage = navigateToLinkNewNotePage,
        navigateToLinkNewContactPage = navigateToLinkNewContactPage,
        navigateToLinkNewItemPage = navigateToLinkNewItemPage,
        navigateToLinkNewOrganizationPage = navigateToLinkNewOrganizationPage,
        navigateToCategoryEditPage = navigateToCategoryEditPage,
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScaffold(
    category: Category,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  LinkedRecords,
    onDeleteRecord: () -> Unit,
    onNavMenuclick: () -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    navigateToLinkRecordsPage: (String) -> Unit,
    navigateToLinkNewNotePage: (String) -> Unit,
    navigateToLinkNewContactPage: (String) -> Unit,
    navigateToLinkNewItemPage: (String) -> Unit,
    navigateToLinkNewOrganizationPage: (String) -> Unit,
    navigateToCategoryEditPage: (String) -> Unit,
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
                title = { Text(text=category.categoryName) },
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
                        onClick = { navigateToCategoryEditPage(category.id) }
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
                            navigateToLinkRecordsPage(category.id)
                        } else {
                            fabExpanded = true
                        }
                    },
                    fab1Icon = ImageVector.vectorResource(R.drawable.outline_description_24),
                    fab1Text = "New Note",
                    onFab1Click = { navigateToLinkNewNotePage(category.id) },
                    fab2Icon = ImageVector.vectorResource(R.drawable.outline_deployed_code_24),
                    fab2Text = "New Item",
                    onFab2Click = { navigateToLinkNewItemPage(category.id) },
                    fab3Icon = ImageVector.vectorResource(R.drawable.outline_people_24),
                    fab3Text = "New Contact",
                    onFab3Click = { navigateToLinkNewContactPage(category.id) },
                    fab4Icon = ImageVector.vectorResource(R.drawable.baseline_business_24),
                    fab4Text = "New Organization",
                    onFab4Click = { navigateToLinkNewOrganizationPage(category.id) },
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                CategoryDetailsBody(
                    category = category,
                    linkedRecordTabs = linkedRecordTabs,
                    linkedRecords = linkedRecords,
                    showDeleteAlertDialog = showDeleteAlertDialog,
                    onDeleteAlertDialogConfirm = {
                        onDeleteRecord()
                        showDeleteAlertDialog = false
                    },
                    onDeleteAlertDialogDismiss = { showDeleteAlertDialog = false },
                    navigateToContactDetailsPage = navigateToContactDetailsPage,
                    navigateToItemDetailsPage = navigateToItemDetailsPage,
                    navigateToNoteDetailsPage = navigateToNoteDetailsPage,
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
                ) { }
            }
        }
    )
}

@Composable
fun CategoryDetailsBody(
    category: Category,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords:  LinkedRecords,
    showDeleteAlertDialog: Boolean,
    onDeleteAlertDialogDismiss: () -> Unit,
    onDeleteAlertDialogConfirm: () -> Unit,
    navigateToContactDetailsPage: (String) -> Unit,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToItemDetailsPage: (String) -> Unit,
    navigateToOrganizationDetailsPage: (String) -> Unit,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    if(showDeleteAlertDialog) {
        DeleteRecordAlertDialog(
            recordTypeName = "Category",
            onDismiss = onDeleteAlertDialogDismiss,
            onConfirm = onDeleteAlertDialogConfirm,
            linkedRecordCount =
            linkedRecords.linkedContacts.count() +
                    linkedRecords.linkedNotes.count() +
                    linkedRecords.linkedItems.count() +
                    linkedRecords.linkedOrganizations.count()
        )
    }
    LinkedRecordsSection(
        recordType = RecordType.Category,
        linkedRecordTabs = linkedRecordTabs,
        linkedRecords = linkedRecords,
        onOpenRecord = { type, id ->
            when (type) {
                RecordType.Contact -> navigateToContactDetailsPage(id)
                RecordType.Note -> navigateToNoteDetailsPage(id)
                RecordType.Item -> navigateToItemDetailsPage(id)
                RecordType.Organization -> navigateToOrganizationDetailsPage(id)
                else -> Unit
            }
        },
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        tabStyle = LinkedRecordsTabStyle.LabelWithIcon,
        header = {},
    ) {
        CategoryDetailsTab(category)
    }
}

/** A category's Details tab: its description, or a line saying it has none. */
@Composable
fun CategoryDetailsTab(category: Category) {
    if (!category.comments.isNullOrEmpty()) {
        Box(modifier = Modifier.detailsTabPadding().verticalScroll(rememberScrollState())) {
            Text(text = category.comments)
        }
    } else {
        RecordDetailsEmpty(text = "This Category has no details", modifier = Modifier.detailsTabPadding())
    }
}

@Composable
fun CategoryHeader(
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
fun CategoryDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            CategoryDetailsScaffold(
                category = Category(
                    ownerDbId = "xxx",
                    categoryName = "Important Long Category Name For Me",
                    comments = "All records linked with this category should be considered as most important"
                ),
                onNavMenuclick = {},
                onDeleteRecord = {},
                navigateToNoteDetailsPage = {},
                navigateToOrganizationDetailsPage = {},
                navigateToContactDetailsPage = {},
                navigateToItemDetailsPage = {},
                navigateToLinkRecordsPage = {},
                navigateToCategoryEditPage = {},
                linkedRecordTabs = listOf(
                    LinkedRecordTypeCount(
                        recordTypeId = 1,
                        count = 3
                    )
                ),
                linkedRecords = LinkedRecords(),
                navigateToLinkNewOrganizationPage = {},
                navigateToLinkNewItemPage = {},
                navigateToLinkNewContactPage = {},
                navigateToLinkNewNotePage = {},

            )
        }
    }
}