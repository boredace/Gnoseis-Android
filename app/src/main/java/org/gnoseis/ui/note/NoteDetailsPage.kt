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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.record.LinkedRecords
import org.gnoseis.ui.record.RecordDetailsEmpty
import org.gnoseis.ui.record.RecordDetailsNavigation
import org.gnoseis.ui.record.RecordDetailsScaffold
import org.gnoseis.ui.record.detailsTabPadding
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class NoteDetailsRoute(
    val noteId: String
)

@Composable
fun NoteDetailsPage(
    pageViewModel: NoteDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordDetailsNavigation,
    linkedNewRecordTypeId: Int? = null,
    onLinkedNewRecordTypeShown: () -> Unit = {},
) {
    val pageState by pageViewModel.noteDetailsPageState.collectAsState()
    val linkedRecords by pageViewModel.linkedRecords.collectAsState()
    val note = pageState.note ?: Note()

    RecordDetailsScaffold(
        recordType = RecordType.Note,
        recordId = note.id,
        linkedRecordTabs = pageState.linkedRecordTabs,
        linkedRecords = linkedRecords,
        newLinkedRecordTypes = listOf(RecordType.Contact, RecordType.Item, RecordType.Category, RecordType.Organization),
        navigation = navigation,
        onDelete = { pageViewModel.deleteNote() },
        onUnlink = pageViewModel::unlink,
        onUndoUnlink = pageViewModel::undoUnlink,
        linkedNewRecordTypeId = linkedNewRecordTypeId,
        onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoteDetailsPagePreview() {
    GnoseisTheme {
        Surface {
            val note = Note(ownerDbId = "xxx", title = "Welcome to the Jungle", textContents = "This will be amazing adventure, are you ready?")
            RecordDetailsScaffold(
                recordType = RecordType.Note,
                recordId = note.id,
                linkedRecordTabs = listOf(LinkedRecordTypeCount(recordTypeId = RecordType.Contact.recordTypeId, count = 3)),
                linkedRecords = LinkedRecords(),
                newLinkedRecordTypes = listOf(RecordType.Contact, RecordType.Item, RecordType.Category, RecordType.Organization),
                navigation = RecordDetailsNavigation({}, { _, _ -> }, {}, {}, { _, _ -> }),
                onDelete = {},
                linkedNewRecordTypeId = null,
                onLinkedNewRecordTypeShown = {},
                header = { NoteHeader(title = note.title ?: "", createdDateTime = note.createDateTime) },
            ) {
                NoteDetailsTab(note)
            }
        }
    }
}
