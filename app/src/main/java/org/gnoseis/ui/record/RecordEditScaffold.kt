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

package org.gnoseis.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.singularName
import org.gnoseis.domain.usecase.LinkSource
import org.gnoseis.ui.components.LinkSourceCard

/** Where an Edit page can go. The same for every record type. */
class RecordEditNavigation(
    val back: () -> Unit,
    /** After a new record is saved: its Details page, in place of the Edit page. */
    val openCreated: (id: String) -> Unit,
    /** After a new linked record is saved: back to the source's Details page, on the new record's tab. */
    val backAfterLinkingNew: () -> Unit,
)

/** The Edit page title: "New <type>" while creating a record, "<type>" while editing one (#56). */
fun recordEditTitle(recordType: RecordType, isNew: Boolean): String =
    if (isNew) "New ${singularName(recordType)}" else singularName(recordType)

/**
 * The Edit page of a record of [recordType]: top bar with the title, back and the save button
 * (enabled by [saveEnabled]), the "Will be linked to" card for [linkSource], then [fields].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScaffold(
    recordType: RecordType,
    isNew: Boolean,
    linkSource: LinkSource?,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    fields: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = recordEditTitle(recordType, isNew)) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { onSave() },
                        enabled = saveEnabled
                    ) {
                        Text(text = if (isNew) "Add" else "Save")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                linkSource?.let { LinkSourceCard(it) }
                fields()
            }
        }
    )
}

/**
 * Saves the record with [save] (which returns the new record's id, or null after editing one)
 * and leaves the Edit page: back after editing; to the new record's Details page after creating
 * one; back to the source's Details page after creating one to link ([linksNew]).
 */
fun CoroutineScope.saveRecord(
    save: suspend () -> String?,
    linksNew: Boolean,
    navigation: RecordEditNavigation,
) {
    launch {
        val addedId = save()
        when {
            addedId == null -> navigation.back()
            linksNew -> navigation.backAfterLinkingNew()
            else -> navigation.openCreated(addedId)
        }
    }
}
