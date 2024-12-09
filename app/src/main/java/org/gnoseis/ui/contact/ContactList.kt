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
import org.gnoseis.data.entity.contact.Contact


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactList(
    contacts: List<Contact> = emptyList(),
    navigateToContactDetailsPage: (String) -> Unit,
    showHeaders: Boolean = true
) {
    LazyColumn(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)//.scrollable(ScrollableState)
    ){
        if(showHeaders) {
            val headerGroups = contacts.groupBy { it.nameLast.substring(0, 1).uppercase() }
            headerGroups.forEach { (header, contacts) ->
                stickyHeader {
                    ContactListHeader(
                        headerText = header
                    )
                }
                items(contacts) { contact ->
                    ContactListItem(
                        id = contact.id,
                        nameLast = contact.nameLast ?: "",
                        nameFirst = contact.nameFirst ?: "",
                        onClick = { navigateToContactDetailsPage(contact.id) }

                    )
                }
            }
        } else {
            items(contacts) { contact ->
                ContactListItem(
                    id = contact.id,
                    nameLast = contact.nameLast ?: "",
                    nameFirst = contact.nameFirst ?: "",
                    onClick = { navigateToContactDetailsPage(contact.id) }
                )
            }
        }
    }
}


@Composable
fun ContactListHeader(
    headerText: String
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box() {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun ContactListItem(
    id: String,
    nameLast: String ="",
    nameFirst: String ="",
    onClick: (String) -> Unit
){

    Card(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth(),
        onClick = { onClick(id) }
    ){
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(start=10.dp, end=10.dp, top=10.dp, bottom = 10.dp)
        ){
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                text="${nameLast}, ${nameFirst}",
                style = MaterialTheme.typography.bodyLarge,
                )
        }
    }
}


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ContactPagePreviewInNoteList(){
    ContactPagePreview()
}
