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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.icons.EmailIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.icons.PhoneIcon
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.RecordDetailsHeader
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordDetailsScaffold
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class ContactDetailsRoute(
    val contactId: String
)

@Composable
fun ContactDetailsPage(
    pageViewModel: ContactDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordDetailsNavigation,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.contactDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()
    val contact = pageState.contact ?: Contact()

    RecordDetailsScaffold(
        recordType = RecordType.Contact,
        recordId = contact.id,
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Item, RecordType.Category, RecordType.Organization),
        navigation = navigation,
        onDelete = { pageViewModel.deleteContact() },
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
        header = { RecordDetailsHeader(title = "${contact.nameLast}, ${contact.nameFirst}") },
    ) {
        ContactDetailsTab(contact)
    }
}

@Composable
fun ContactDetailsTab(
    contact: Contact = Contact()
) {
    Column(modifier = Modifier.detailsTabPadding()) {
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
            RecordDetailsEmpty(text = "This Contact has no details")
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
            val contact = Contact(ownerDbId = "xxx", nameLast = "Shmoebargansky", nameFirst = "Joseph Jr.")
            RecordDetailsScaffold(
                recordType = RecordType.Contact,
                recordId = contact.id,
                linkedRecordTabs = listOf(LinkedRecordTypeCount(recordTypeId = RecordType.Note.recordTypeId, count = 3)),
                linkedRecords = LinkedRecords(),
                newLinkedRecordTypes = listOf(RecordType.Note, RecordType.Item, RecordType.Category, RecordType.Organization),
                navigation = RecordDetailsNavigation({}, { _, _ -> }, {}, {}, { _, _ -> }),
                onDelete = {},
                linkedNewRecordTypeId = null,
                onLinkedNewRecordTypeShown = {},
                header = { RecordDetailsHeader(title = "${contact.nameLast}, ${contact.nameFirst}") },
            ) {
                ContactDetailsTab(contact)
            }
        }
    }
}
