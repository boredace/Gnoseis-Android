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

import org.gnoseis.ui.components.RecordRowAction
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.enums.IconTextPosition
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.category.CategoryList
import org.gnoseis.ui.components.RecordTypeIcon
import org.gnoseis.ui.components.recordTypeColors
import org.gnoseis.ui.contact.ContactList
import org.gnoseis.ui.icons.QuestionIcon
import org.gnoseis.ui.item.ItemList
import org.gnoseis.ui.note.NoteList
import org.gnoseis.ui.organization.OrganizationList

/** How a Details page labels its tabs. */
enum class LinkedRecordsTabStyle {
    /** The label in the tab's text slot, under the icon; the Details tab has a neutral icon. */
    LabelBelow,

    /**
     * The label drawn with the icon (beside it, or under it once there are more than three
     * linked types); the Details tab shows the record's own type icon.
     */
    LabelWithIcon,
}

/**
 * A Details page's [header], its tab row (the Details tab, then one tab per linked record type,
 * in [linkedRecordTabs] order) and the selected tab's content: [detailsTab], or the list of
 * linked records of that type. [detailsTab] gets the whole page area under the tab row and does
 * its own padding, scrolling and empty state. With [onUnlink], a long press on a linked record
 * offers to unlink it (#11).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkedRecordsSection(
    recordType: RecordType,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    linkedRecords: LinkedRecords,
    onOpenRecord: (RecordType, String) -> Unit,
    linkedNewRecordTypeId: Int?,
    onLinkedNewRecordTypeShown: () -> Unit,
    tabStyle: LinkedRecordsTabStyle,
    header: @Composable () -> Unit,
    onUnlink: ((linkedRecordId: String) -> Unit)? = null,
    detailsTab: @Composable () -> Unit,
) {
    var selectedTabRecordTypeId by rememberSelectedLinkedRecordType(linkedNewRecordTypeId, onLinkedNewRecordTypeShown)
    val selectedTabIndex = tabIndexOf(linkedRecordTabs, selectedTabRecordTypeId)
    val pagerState = rememberPagerState { linkedRecordTabs.size + 1 }

    Column {
        Row(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 15.dp, end = 15.dp, bottom = 5.dp)
            ) {
                header()
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinkedRecordsTabRow(
                        recordType = recordType,
                        linkedRecordTabs = linkedRecordTabs,
                        selectedTabIndex = selectedTabIndex,
                        onSelect = { selectedTabRecordTypeId = it },
                        tabStyle = tabStyle,
                    )
                }
            }
        }
        Box {
            HorizontalPager(
                state = pagerState,
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTabIndex == 0) {
                        detailsTab()
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(start = 15.dp, end = 15.dp, bottom = 5.dp)
                        ) {
                            Spacer(modifier = Modifier.height(15.dp))
                            LinkedRecordList(
                                recordTypeId = linkedRecordTabs[selectedTabIndex - 1].recordTypeId,
                                linkedRecords = linkedRecords,
                                onOpenRecord = onOpenRecord,
                                rowAction = onUnlink?.let { rememberUnlinkRowAction(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Padding of a Details tab's content, matching the linked record lists beside it. Each type's
 * Details tab applies it itself.
 */
fun Modifier.detailsTabPadding(): Modifier =
    padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 5.dp)

@Composable
private fun LinkedRecordsTabRow(
    recordType: RecordType,
    linkedRecordTabs: List<LinkedRecordTypeCount>,
    selectedTabIndex: Int,
    onSelect: (Int?) -> Unit,
    tabStyle: LinkedRecordsTabStyle,
) {
    val labelBelow = tabStyle == LinkedRecordsTabStyle.LabelBelow
    val textPosition =
        if (linkedRecordTabs.size > 3) IconTextPosition.Bottom
        else IconTextPosition.Right
    TabRow(
        selectedTabIndex = selectedTabIndex
    ) {
        Tab(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceContainer),
            selected = selectedTabIndex == 0,
            onClick = { onSelect(null) },
            text = if (labelBelow) ({ Text(text = "Details") }) else null,
            icon = {
                if (labelBelow) {
                    Icon(
                        imageVector = Icons.Outlined.Notes,
                        contentDescription = "Details"
                    )
                } else {
                    TypeIcon(recordType, "Details", textPosition)
                }
            }
        )
        linkedRecordTabs.forEachIndexed { index, tab ->
            val count = "(${tab.count})"
            val type = RecordType.entries.firstOrNull { it.recordTypeId == tab.recordTypeId }
            Tab(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceContainer),
                selected = selectedTabIndex == index + 1,
                onClick = { onSelect(tab.recordTypeId) },
                text = if (labelBelow) ({ Text(text = count) }) else null,
                icon = {
                    if (type == null) {
                        QuestionIcon()
                    } else if (labelBelow) {
                        TypeIcon(type, null, null)
                    } else {
                        TypeIcon(type, count, textPosition)
                    }
                }
            )
        }
    }
}

/** A record type's icon in its outline colour; [text] stays in the tab's own text colour. */
@Composable
private fun TypeIcon(type: RecordType, text: String?, textPosition: IconTextPosition?) {
    RecordTypeIcon(
        type = type,
        colorFilter = ColorFilter.tint(recordTypeColors(type).outline),
        size = 24.dp,
        text = text,
        textPosition = textPosition,
    )
}

@Composable
private fun LinkedRecordList(
    recordTypeId: Int,
    linkedRecords: LinkedRecords,
    onOpenRecord: (RecordType, String) -> Unit,
    rowAction: RecordRowAction?,
) {
    when (recordTypeId) {
        RecordType.Note.recordTypeId -> NoteList(
            notes = linkedRecords.linkedNotes,
            navigateToNoteDetailsPage = { onOpenRecord(RecordType.Note, it) },
            rowAction = rowAction,
        )
        RecordType.Contact.recordTypeId -> ContactList(
            contacts = linkedRecords.linkedContacts,
            navigateToContactDetailsPage = { onOpenRecord(RecordType.Contact, it) },
            showHeaders = false,
            rowAction = rowAction,
        )
        RecordType.Organization.recordTypeId -> OrganizationList(
            organizations = linkedRecords.linkedOrganizations,
            navigateToOrganizationDetailsPage = { onOpenRecord(RecordType.Organization, it) },
            showHeaders = false,
            rowAction = rowAction,
        )
        RecordType.Category.recordTypeId -> CategoryList(
            categories = linkedRecords.linkedCategories,
            navigateToCategoryDetailsPage = { onOpenRecord(RecordType.Category, it) },
            showHeaders = false,
            rowAction = rowAction,
        )
        RecordType.Item.recordTypeId -> ItemList(
            items = linkedRecords.linkedItems,
            navigateToItemDetailsPage = { onOpenRecord(RecordType.Item, it) },
            showHeaders = false,
            rowAction = rowAction,
        )
        else -> Text(text = "Record type ID = $recordTypeId")
    }
}

/** Long press on a linked record opens a menu on its row with Unlink; TalkBack gets "Unlink <name>". */
@Composable
private fun rememberUnlinkRowAction(onUnlink: (String) -> Unit): RecordRowAction {
    var menuRecordId by remember { mutableStateOf<String?>(null) }
    return RecordRowAction(
        label = "Unlink",
        onLongClick = { menuRecordId = it },
        onAction = onUnlink,
        menu = { id ->
            DropdownMenu(
                expanded = menuRecordId == id,
                onDismissRequest = { menuRecordId = null },
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Unlink") },
                    onClick = {
                        menuRecordId = null
                        onUnlink(id)
                    },
                )
            }
        },
    )
}

/**
 * The selected tab of a Details page, kept as a linked record type id (null = the Details tab)
 * rather than a tab index: tabs are sorted by count, so an index can end up on another type.
 * Saveable, so it survives opening a linked record and coming back.
 *
 * #6: when [linkedNewRecordTypeId] is set because a new record of that type was just linked,
 * its tab is selected and [onLinkedNewRecordTypeShown] is called, so this happens only once.
 */
@Composable
fun rememberSelectedLinkedRecordType(
    linkedNewRecordTypeId: Int?,
    onLinkedNewRecordTypeShown: () -> Unit,
): MutableState<Int?> {
    val selected = rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(linkedNewRecordTypeId) {
        if (linkedNewRecordTypeId != null) {
            selected.value = linkedNewRecordTypeId
            onLinkedNewRecordTypeShown()
        }
    }
    return selected
}

/**
 * Index in a Details page's tab row (Details tab first) of the tab for [selectedRecordTypeId].
 * The Details tab (0) if that type has no tab, e.g. while a just-linked record's tab loads.
 */
fun tabIndexOf(linkedRecordTabs: List<LinkedRecordTypeCount>, selectedRecordTypeId: Int?): Int =
    linkedRecordTabs.indexOfFirst { it.recordTypeId == selectedRecordTypeId } + 1
