/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.ui.mostlinked

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.home.LinkedRecordCount
import org.gnoseis.data.entity.home.TypeLinkCount
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.pluralName
import org.gnoseis.data.enums.singularName
import org.gnoseis.ui.components.RecordListRow
import org.gnoseis.ui.components.RecordTypeIcon
import org.gnoseis.ui.components.RecordTypePicker
import org.gnoseis.ui.components.linkCountText
import org.gnoseis.ui.components.recordTypeColors
import org.gnoseis.ui.theme.GnoseisTheme

/**
 * The full Most linked list. [recordTypeId] is the type Home's Most linked was filtered to when
 * "See all" was tapped; null for all types.
 */
@Serializable
data class MostLinkedRoute(val recordTypeId: Int? = null)

@Composable
fun MostLinkedPage(
    pageViewModel: MostLinkedViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onBack: () -> Unit,
    navigateToRecordDetailsPage: (RecordType, String) -> Unit,
) {
    val pageState by pageViewModel.pageState.collectAsState()
    MostLinkedPageScaffold(
        pageState = pageState,
        onBack = onBack,
        setTypeId = pageViewModel::setTypeId,
        navigateToRecordDetailsPage = navigateToRecordDetailsPage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MostLinkedPageScaffold(
    pageState: MostLinkedPageState,
    onBack: () -> Unit = {},
    setTypeId: (Int?) -> Unit = {},
    navigateToRecordDetailsPage: (RecordType, String) -> Unit = { _, _ -> },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Most linked") },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (pageState.isLoaded && !pageState.isNothingLinked) {
                RecordTypePicker(
                    typeId = pageState.typeId,
                    types = pageState.types,
                    onTypeChange = setTypeId,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            when {
                !pageState.isLoaded -> Unit
                pageState.isNothingLinked -> EmptyList("Link records to see them here")
                pageState.isPickedTypeEmpty -> EmptyList(
                    text = "No linked ${pluralName(RecordType.from(pageState.typeId!!)).lowercase()} yet",
                    showAllTypes = { setTypeId(null) },
                )
                else -> MostLinkedList(
                    records = pageState.records,
                    showType = pageState.typeId == null,
                    onClick = { navigateToRecordDetailsPage(RecordType.from(it.recordTypeId), it.id) },
                )
            }
        }
    }
}

@Composable
private fun MostLinkedList(
    records: List<LinkedRecordCount>,
    showType: Boolean,
    onClick: (LinkedRecordCount) -> Unit,
) {
    // Saved with the back stack entry, so coming back from Details returns to the same place.
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(records, key = { it.id }) { record ->
            MostLinkedRow(record, showType, onClick = { onClick(record) })
        }
    }
}

/** A record's type icon in its type colour, its name (and type, in the All view), and its link count. No rank: ties would make it misleading. */
@Composable
private fun MostLinkedRow(
    record: LinkedRecordCount,
    showType: Boolean,
    onClick: () -> Unit,
) {
    val type = RecordType.from(record.recordTypeId)
    RecordListRow(
        id = record.id,
        name = record.name,
        onClick = onClick,
        rowAction = null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecordTypeIcon(type, ColorFilter.tint(recordTypeColors(type).outline), 24.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showType) {
                    Text(
                        text = singularName(type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = linkCountText(record.linkCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** The empty states, worded as on Home. [showAllTypes] adds the "Show all types" link. */
@Composable
private fun EmptyList(text: String, showAllTypes: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (showAllTypes != null) {
                TextButton(onClick = showAllTypes) { Text("Show all types") }
            }
        }
    }
}

// ---------- Previews ----------

private val previewTypes = listOf(
    TypeLinkCount(RecordType.Contact.recordTypeId, 41),
    TypeLinkCount(RecordType.Organization.recordTypeId, 27),
    TypeLinkCount(RecordType.Category.recordTypeId, 33),
    TypeLinkCount(RecordType.Item.recordTypeId, 12),
)

private val previewRecords = listOf(
    LinkedRecordCount("a", "Clients", RecordType.Category.recordTypeId, 21),
    LinkedRecordCount("b", "Maya Ortiz", RecordType.Contact.recordTypeId, 16),
    LinkedRecordCount("c", "Northwind", RecordType.Organization.recordTypeId, 14),
    LinkedRecordCount("d", "Harbor & Pine Landscape Architecture and Urban Design", RecordType.Organization.recordTypeId, 11),
    LinkedRecordCount("e", "Daniel Park", RecordType.Contact.recordTypeId, 9),
    LinkedRecordCount("f", "Warehouse audit", RecordType.Item.recordTypeId, 7),
    LinkedRecordCount("g", "Priya Nair", RecordType.Contact.recordTypeId, 1),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MostLinkedPageAllTypesPreview() {
    GnoseisTheme {
        Surface {
            MostLinkedPageScaffold(MostLinkedPageState(null, previewTypes, previewRecords, isLoaded = true))
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MostLinkedPageContactsPickedPreview() {
    GnoseisTheme {
        Surface {
            MostLinkedPageScaffold(
                MostLinkedPageState(
                    typeId = RecordType.Contact.recordTypeId,
                    types = previewTypes,
                    records = previewRecords.filter { it.recordTypeId == RecordType.Contact.recordTypeId },
                    isLoaded = true,
                )
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MostLinkedPagePickedTypeEmptyPreview() {
    GnoseisTheme {
        Surface {
            MostLinkedPageScaffold(
                MostLinkedPageState(RecordType.Item.recordTypeId, previewTypes.dropLast(1), emptyList(), isLoaded = true)
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MostLinkedPageNothingLinkedPreview() {
    GnoseisTheme {
        Surface {
            MostLinkedPageScaffold(MostLinkedPageState(isLoaded = true))
        }
    }
}
