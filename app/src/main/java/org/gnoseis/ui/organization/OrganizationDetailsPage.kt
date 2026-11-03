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
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.RecordDetailsHeader
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordDetailsScaffold
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class OrganizationDetailsRoute(
    val organizationId: String
)

@Composable
fun OrganizationDetailsPage(
    pageViewModel: OrganizationDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordDetailsNavigation,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.organizationDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()
    val organization = pageState.organization ?: Organization()

    RecordDetailsScaffold(
        recordType = RecordType.Organization,
        recordId = organization.id,
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Contact, RecordType.Category, RecordType.Item),
        navigation = navigation,
        onDelete = { pageViewModel.deleteOrganization() },
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        header = { RecordDetailsHeader(title = organization.organizationName) },
    ) {
        OrganizationDetailsTab(organization)
    }
}

/** An organization's Details tab: its comments, or a line saying it has none. */
@Composable
fun OrganizationDetailsTab(organization: Organization) {
    if (!organization.comments.isNullOrEmpty()) {
        Box(modifier = Modifier.detailsTabPadding().verticalScroll(rememberScrollState())) {
            Text(text = organization.comments)
        }
    } else {
        RecordDetailsEmpty(text = "This Organization has no details", modifier = Modifier.detailsTabPadding())
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OrganizationDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            val organization = Organization(ownerDbId = "xxx", organizationName = "Anaconda Corporation Ltd.")
            RecordDetailsScaffold(
                recordType = RecordType.Organization,
                recordId = organization.id,
                linkedRecordTabs = listOf(LinkedRecordTypeCount(recordTypeId = RecordType.Note.recordTypeId, count = 3)),
                linkedRecords = LinkedRecords(),
                newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Contact, RecordType.Category, RecordType.Item),
                navigation = RecordDetailsNavigation({}, { _, _ -> }, {}, {}, { _, _ -> }),
                onDelete = {},
                linkedNewRecordTypeId = null,
                onLinkedNewRecordTypeShown = {},
                header = { RecordDetailsHeader(title = organization.organizationName) },
            ) {
                OrganizationDetailsTab(organization)
            }
        }
    }
}
