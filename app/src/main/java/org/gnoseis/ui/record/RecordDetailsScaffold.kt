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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.singularName
import org.gnoseis.ui.components.DeleteRecordAlertDialog
import org.gnoseis.ui.components.ExpandableActionButton
import org.gnoseis.ui.components.recordTypeColors
import org.gnoseis.ui.components.recordTypeIconRes
import org.gnoseis.ui.icons.DeleteIcon
import org.gnoseis.ui.icons.EditIcon

/** Where a Details page can go. The same for every record type. */
class RecordDetailsNavigation(
    val back: () -> Unit,
    /** Opens the Details page of a linked record. */
    val openRecord: (type: RecordType, id: String) -> Unit,
    /** Opens this record's Edit page. */
    val edit: (id: String) -> Unit,
    /** Opens the Link Records page for this record. */
    val linkExisting: (id: String) -> Unit,
    /** Opens the Edit page for a new record of [type], to be linked to the record [sourceId]. */
    val newLinked: (type: RecordType, sourceId: String) -> Unit,
)

/**
 * The Details page of a record of [recordType]: top bar (back, delete, edit), [header], the
 * linked records tabs with [detailsTab] as the first tab, and the FAB to link an existing record
 * or a new one of each of [newLinkedRecordTypes] (in that order, top to bottom; at most four).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailsScaffold(
    recordType: RecordType,
    recordId: String,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords: LinkedRecords,
    newLinkedRecordTypes: List<RecordType>,
    navigation: RecordDetailsNavigation,
    onDelete: () -> Unit,
    linkedNewRecordTypeId: Int?,
    onLinkedNewRecordTypeShown: () -> Unit,
    title: String = singularName(recordType),
    tabStyle: LinkedRecordsTabStyle = LinkedRecordsTabStyle.LabelBelow,
    header: @Composable () -> Unit = {},
    detailsTab: @Composable () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    var showDeleteAlertDialog by remember { mutableStateOf(false) }
    val typeName = singularName(recordType)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .alpha(if (fabExpanded) 0.1f else 1f),
                title = { Text(text = title) },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = { navigation.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteAlertDialog = true }
                    ) {
                        DeleteIcon(
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = "Delete $typeName",
                        )
                    }
                    IconButton(
                        onClick = { navigation.edit(recordId) }
                    ) {
                        EditIcon(
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = "Edit $typeName",
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val fabs = newLinkedRecordTypes.map { type ->
                Triple(
                    ImageVector.vectorResource(recordTypeIconRes(type)),
                    recordTypeColors(type).outline,
                    "New ${singularName(type)}",
                )
            }
            fun onNew(index: Int) {
                newLinkedRecordTypes.getOrNull(index)?.let { navigation.newLinked(it, recordId) }
            }
            Column {
                ExpandableActionButton(
                    isExpanded = fabExpanded,
                    fabIcon = Icons.Filled.AddLink,
                    fabText = "Link Existing",
                    onFabClick = {
                        if (fabExpanded) {
                            fabExpanded = false
                            navigation.linkExisting(recordId)
                        } else {
                            fabExpanded = true
                        }
                    },
                    fab1Icon = fabs[0].first,
                    fab1IconTint = fabs[0].second,
                    fab1Text = fabs[0].third,
                    onFab1Click = { onNew(0) },
                    fab2Icon = fabs.getOrNull(1)?.first,
                    fab2IconTint = fabs.getOrNull(1)?.second ?: Color.Unspecified,
                    fab2Text = fabs.getOrNull(1)?.third,
                    onFab2Click = { onNew(1) },
                    fab3Icon = fabs.getOrNull(2)?.first,
                    fab3IconTint = fabs.getOrNull(2)?.second ?: Color.Unspecified,
                    fab3Text = fabs.getOrNull(2)?.third,
                    onFab3Click = { onNew(2) },
                    fab4Icon = fabs.getOrNull(3)?.first,
                    fab4IconTint = fabs.getOrNull(3)?.second ?: Color.Unspecified,
                    fab4Text = fabs.getOrNull(3)?.third,
                    onFab4Click = { onNew(3) },
                )
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                if (showDeleteAlertDialog) {
                    DeleteRecordAlertDialog(
                        recordTypeName = typeName,
                        onDismiss = { showDeleteAlertDialog = false },
                        onConfirm = {
                            onDelete()
                            navigation.back()
                            showDeleteAlertDialog = false
                        },
                        linkedRecordCount = linkedRecords.count,
                    )
                }
                LinkedRecordsSection(
                    recordType = recordType,
                    linkedRecordTabs = linkedRecordTabs,
                    linkedRecords = linkedRecords,
                    onOpenRecord = navigation.openRecord,
                    linkedNewRecordTypeId = linkedNewRecordTypeId,
                    onLinkedNewRecordTypeShown = onLinkedNewRecordTypeShown,
                    tabStyle = tabStyle,
                    header = header,
                    detailsTab = detailsTab,
                )
            }
            if (fabExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.8f)
                        .background(Color.Black)
                        .clickable(
                            enabled = true,
                            onClick = { fabExpanded = false }
                        )
                )
            }
        }
    )
}

/** A Details page's header: the record's name, on up to two lines. */
@Composable
fun RecordDetailsHeader(title: String) {
    Text(
        maxLines = 2,
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        text = title
    )
    Spacer(modifier = Modifier.height(5.dp))
}

/** A line in the middle of an empty Details tab, for a type's Details tab to show. */
@Composable
fun RecordDetailsEmpty(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(text = text)
    }
}
