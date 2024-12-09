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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.note.Note
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteList(
    notes : List<Note> = emptyList(),
    navigateToNoteDetailsPage: (String) -> Unit,
    showHeaders: Boolean = true
)
{

    LazyColumn(
        modifier = Modifier.padding(start = 15.dp, end = 15.dp)//.scrollable(ScrollableState)
    ){
        if (showHeaders) {
            val noteGroups = notes.sortedByDescending { it.createDate }.groupBy { it.createDate }
            noteGroups.forEach { (header, notes) ->
                stickyHeader {
                    NoteListHeader(
                        headerText = header
                    )
                }
                items(notes.sortedByDescending { it.createDateTime }) { note ->
                    NoteListItem(
                        id = note.id,
                        title = note.title ?: "",
                        textContent = note.textContents ?: "",
                        onClick = { navigateToNoteDetailsPage(note.id) }

                    )
                }
            }
        } else {
            items(notes) { note ->
                NoteListItem(
                    id = note.id,
                    title = note.title ?: "",
                    textContent = note.textContents ?: "",
                    onClick = { navigateToNoteDetailsPage(note.id) }

                )
            }
        }
    }
}

@Composable
fun NoteListHeader(
    headerText: Long
){
    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(headerText), ZoneId.of("GMT"))
//    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(headerText), ZoneId.systemDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box() {
            Text(
                text = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
                )
        }
        Box() {
            Text(text = zonedDateTime.format(DateTimeFormatter.ofPattern("E")),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}


@Composable
fun NoteListItem(
    id: String,
    title: String ="",
    textContent: String = "",
    onClick: (String) -> Unit
){
//    val anotatedString =  getAnnotatedString(title, textContent)

//    Spacer(modifier = Modifier.height(5.dp))
    Card(
        onClick = { onClick(id) }
    ){
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp)

        ){
            if(title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }
            if(textContent.length > 0) {
                Text(
                    text = textContent,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }


/*            Text(text=
            AnnotatedString(
                text = anotatedString.consolidatedString,
                spanStyles = listOf(
                    AnnotatedString.Range(
                        SpanStyle(
                            fontWeight = FontWeight.Bold
                        ),
                        0,
                        anotatedString.boldUntil)
                )
            ),
                maxLines = 2,
            )*/
        }
    }
}

fun getAnnotatedString(title: String?="", textContent: String?="") : NoteListViewModel.ListEntryAnnotatedString {
    var _string: String = ""
    var _endBold: Int = 0
    if(!title.isNullOrEmpty()) {
        _string = title
        _endBold = title.length
        if(!textContent.isNullOrEmpty()) _string = _string + " "
    }
    if(!textContent.isNullOrEmpty()) {
        _string = _string + textContent
        if(_endBold == 0) _endBold = 40
    }
    return NoteListViewModel.ListEntryAnnotatedString(
        boldUntil = _endBold,
        consolidatedString = _string
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotePagePreviewInNoteList(){
    NotePagePreview()
}