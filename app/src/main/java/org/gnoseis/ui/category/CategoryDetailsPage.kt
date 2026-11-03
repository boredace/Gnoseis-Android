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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.LinkedRecordsTabStyle
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordDetailsScaffold
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class CategoryDetailsRoute(
    val categoryId: String
)

@Composable
fun CategoryDetailsPage(
    pageViewModel: CategoryDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordDetailsNavigation,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.categoryDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()
    val category = pageState.category ?: Category()

    RecordDetailsScaffold(
        recordType = RecordType.Category,
        recordId = category.id,
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Item, RecordType.Contact, RecordType.Organization),
        navigation = navigation,
        onDelete = { pageViewModel.deleteCategory() },
        onUnlink = pageViewModel::unlink,
        onUndoUnlink = pageViewModel::undoUnlink,
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        title = category.categoryName,
        tabStyle = LinkedRecordsTabStyle.LabelWithIcon,
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CategoryDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            val category = Category(ownerDbId = "xxx", categoryName = "Clients")
            RecordDetailsScaffold(
                recordType = RecordType.Category,
                recordId = category.id,
                linkedRecordTabs = listOf(LinkedRecordTypeCount(recordTypeId = RecordType.Note.recordTypeId, count = 3)),
                linkedRecords = LinkedRecords(),
                newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Item, RecordType.Contact, RecordType.Organization),
                navigation = RecordDetailsNavigation({}, { _, _ -> }, {}, {}, { _, _ -> }),
                onDelete = {},
                linkedNewRecordTypeId = null,
                onLinkedNewRecordTypeShown = {},
                title = category.categoryName,
                tabStyle = LinkedRecordsTabStyle.LabelWithIcon,
            ) {
                CategoryDetailsTab(category)
            }
        }
    }
}
