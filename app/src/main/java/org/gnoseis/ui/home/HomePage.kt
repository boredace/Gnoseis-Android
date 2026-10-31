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

package org.gnoseis.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.entity.home.DayCount
import org.gnoseis.data.entity.home.LinkEnds
import org.gnoseis.data.entity.home.LinkedRecordCount
import org.gnoseis.data.entity.home.NoteSummary
import org.gnoseis.data.entity.home.RecordCounts
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.icons.CategoryIcon
import org.gnoseis.ui.icons.ContactIcon
import org.gnoseis.ui.icons.ItemIcon
import org.gnoseis.ui.icons.NoteIcon
import org.gnoseis.ui.icons.OrganizationIcon
import org.gnoseis.ui.theme.CategoryColorsDark
import org.gnoseis.ui.theme.CategoryColorsLight
import org.gnoseis.ui.theme.ContactColorsDark
import org.gnoseis.ui.theme.ContactColorsLight
import org.gnoseis.ui.theme.GnoseisTheme
import org.gnoseis.ui.theme.ItemColorsDark
import org.gnoseis.ui.theme.ItemColorsLight
import org.gnoseis.ui.theme.OrganizationColorsDark
import org.gnoseis.ui.theme.OrganizationColorsLight
import org.gnoseis.ui.theme.RecordTypeColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
object HomeRoute

/** The record types shown as Most linked bubbles, in filter chip order. */
private val BUBBLE_TYPES = listOf(RecordType.Contact, RecordType.Organization, RecordType.Category, RecordType.Item)
private val CLUSTER_HEIGHT = 360.dp

@Composable
fun HomePage(
    pageViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToSettingsPage: () -> Unit,
    navigateToSearchPage: () -> Unit,
    navigateToRecordDetailsPage: (RecordType, String) -> Unit,
    navigateToNewRecordPage: (RecordType) -> Unit,
    navigateToRecordListPage: (RecordType) -> Unit,
    navigateToLinkRecordsPage: (noteId: String) -> Unit,
    navigateToBackupSettingsPage: () -> Unit,
) {
    val pageState by pageViewModel.homePageState.collectAsState()
    HomePageScaffold(
        pageState = pageState,
        navMenuClick = navMenuClick,
        navigateToSettingsPage = navigateToSettingsPage,
        navigateToSearchPage = navigateToSearchPage,
        navigateToRecordDetailsPage = navigateToRecordDetailsPage,
        navigateToNewRecordPage = navigateToNewRecordPage,
        navigateToRecordListPage = navigateToRecordListPage,
        navigateToLinkRecordsPage = navigateToLinkRecordsPage,
        navigateToBackupSettingsPage = navigateToBackupSettingsPage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScaffold(
    pageState: HomePageState,
    navMenuClick: () -> Unit = {},
    navigateToSettingsPage: () -> Unit = {},
    navigateToSearchPage: () -> Unit = {},
    navigateToRecordDetailsPage: (RecordType, String) -> Unit = { _, _ -> },
    navigateToNewRecordPage: (RecordType) -> Unit = {},
    navigateToRecordListPage: (RecordType) -> Unit = {},
    navigateToLinkRecordsPage: (String) -> Unit = {},
    navigateToBackupSettingsPage: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(onClick = navMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Drawer Menu.")
                    }
                },
                actions = {
                    IconButton(onClick = navigateToSettingsPage) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DateAndSearch(pageState.today, navigateToSearchPage)
            MostLinked(pageState, navigateToRecordDetailsPage)
            AddNew(navigateToNewRecordPage)
            RecentNotes(
                notes = pageState.recentNotes,
                isLoaded = pageState.isLoaded,
                navigateToNoteDetailsPage = { navigateToRecordDetailsPage(RecordType.Note, it) },
                navigateToNoteListPage = { navigateToRecordListPage(RecordType.Note) },
            )
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            RecordCountTiles(pageState.counts, navigateToRecordListPage)
            NotesChart(pageState.notesPerDay)
            if (pageState.unlinkedNoteCount > 0) {
                UnlinkedNotes(pageState.unlinkedNotes, pageState.unlinkedNoteCount, navigateToLinkRecordsPage)
            }
            if (pageState.isLoaded) {
                BackupCard(pageState.backup, pageState.today, navigateToBackupSettingsPage)
            }
        }
    }
}

// ---------- Date and search ----------

@Composable
private fun DateAndSearch(today: LocalDate, navigateToSearchPage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = today.format(DateTimeFormatter.ofPattern("E")),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Surface(
            onClick = navigateToSearchPage,
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Search all records",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- Most linked ----------

@Composable
private fun MostLinked(
    pageState: HomePageState,
    navigateToRecordDetailsPage: (RecordType, String) -> Unit,
) {
    var filterTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "Most linked", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Bigger bubble, more records link to it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (pageState.isLoaded && pageState.mostLinked.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CLUSTER_HEIGHT)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Link records to see them here",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filterTypeId == null,
                    onClick = { filterTypeId = null },
                    label = { Text("All") },
                )
                BUBBLE_TYPES.forEach { type ->
                    val colors = recordTypeColors(type)
                    FilterChip(
                        selected = filterTypeId == type.recordTypeId,
                        onClick = { filterTypeId = type.recordTypeId },
                        label = { Text(pluralName(type)) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.outline)
                            )
                        },
                    )
                }
            }
            BubbleCluster(
                records = pageState.mostLinked,
                links = pageState.mostLinkedLinks,
                filterTypeId = filterTypeId,
                onClick = { navigateToRecordDetailsPage(RecordType.from(it.recordTypeId), it.id) },
            )
        }
    }
}

@Composable
private fun BubbleCluster(
    records: List<LinkedRecordCount>,
    links: List<LinkEnds>,
    filterTypeId: Int?,
    onClick: (LinkedRecordCount) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(CLUSTER_HEIGHT)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        val width = maxWidth.value
        val height = maxHeight.value
        val linkCounts = records.map { it.linkCount }
        val bubbles = remember(linkCounts, width, height) { BubbleLayout.layout(linkCounts, width, height) }
        val centres = records.indices.associate { records[it].id to bubbles[it] }
        val lineColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(modifier = Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            links.forEach { link ->
                val a = centres[link.record1Id] ?: return@forEach
                val b = centres[link.record2Id] ?: return@forEach
                drawLine(
                    color = lineColor,
                    start = Offset(a.x.dp.toPx(), a.y.dp.toPx()),
                    end = Offset(b.x.dp.toPx(), b.y.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dash,
                )
            }
        }
        records.forEachIndexed { index, record ->
            val bubble = bubbles[index]
            val visible = filterTypeId == null || filterTypeId == record.recordTypeId
            Bubble(
                record = record,
                type = RecordType.from(record.recordTypeId),
                diameter = bubble.diameter.dp,
                modifier = Modifier
                    .offset((bubble.x - bubble.diameter / 2).dp, (bubble.y - bubble.diameter / 2).dp)
                    .alpha(if (visible) 1f else 0.2f),
                onClick = { onClick(record) },
            )
        }
    }
}

@Composable
private fun Bubble(
    record: LinkedRecordCount,
    type: RecordType,
    diameter: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = recordTypeColors(type)
    val large = diameter >= 100.dp
    Column(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(colors.fill)
            .border(2.dp, colors.outline, CircleShape)
            .clickable(onClickLabel = "Open ${singularName(type).lowercase()}", onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        RecordTypeIcon(type, ColorFilter.tint(colors.outline), if (large) 22.dp else 16.dp)
        Text(
            text = record.name,
            fontSize = if (large) 15.sp else if (diameter >= 80.dp) 12.sp else 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = if (large) 17.sp else 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = linkCountText(record.linkCount),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ---------- Add new ----------

@Composable
private fun AddNew(navigateToNewRecordPage: (RecordType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Add new")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                RecordType.Note to "Note",
                RecordType.Contact to "Contact",
                RecordType.Organization to "Org",
                RecordType.Category to "Category",
                RecordType.Item to "Item",
            ).forEach { (type, label) ->
                val container =
                    if (type == RecordType.Note) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                val content =
                    if (type == RecordType.Note) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                Surface(
                    onClick = { navigateToNewRecordPage(type) },
                    shape = RoundedCornerShape(16.dp),
                    color = container,
                    contentColor = content,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        RecordTypeIcon(type, ColorFilter.tint(content), 22.dp)
                        Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ---------- Recent notes ----------

@Composable
private fun RecentNotes(
    notes: List<NoteSummary>,
    isLoaded: Boolean,
    navigateToNoteDetailsPage: (String) -> Unit,
    navigateToNoteListPage: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Recent notes")
            TextButton(onClick = navigateToNoteListPage) { Text("See all") }
        }
        if (isLoaded && notes.isEmpty()) {
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notes.forEach { note ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navigateToNoteDetailsPage(note.id) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = noteDateText(note.createDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = linkCountText(note.linkCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!note.title.isNullOrBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                firstLine(note.textContents)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ---------- Overview ----------

@Composable
private fun RecordCountTiles(counts: RecordCounts, navigateToRecordListPage: (RecordType) -> Unit) {
    val tiles = listOf(
        Triple(RecordType.Contact, counts.contacts, "Contacts"),
        Triple(RecordType.Organization, counts.organizations, "Organizations"),
        Triple(RecordType.Note, counts.notes, "Notes"),
        Triple(RecordType.Category, counts.categories, "Categories"),
        Triple(RecordType.Item, counts.items, "Items"),
        Triple(RecordType.LinkedRecord, counts.links, "Links"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, count, label) ->
                    val content: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium)
                                RecordTypeIcon(type, ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant), 20.dp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val tileModifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                    // Links have no list page, so that tile isn't clickable
                    if (type == RecordType.LinkedRecord) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = tileModifier,
                            content = content,
                        )
                    } else {
                        Surface(
                            onClick = { navigateToRecordListPage(type) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = tileModifier,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesChart(notesPerDay: List<DayCount>) {
    val total = notesPerDay.sumOf { it.count }
    val max = notesPerDay.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = "Notes, last ${HomeViewModel.CHART_DAYS} days", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (total == 1) "1 note" else "$total notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                notesPerDay.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (day.count == 0) 3.dp else (76.dp * day.count / max).coerceAtLeast(8.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(
                                if (day.count == 0) MaterialTheme.colorScheme.outlineVariant
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
            if (notesPerDay.isNotEmpty()) {
                val format = DateTimeFormatter.ofPattern("MMM d")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(
                        LocalDate.ofEpochDay(notesPerDay.first().day).format(format),
                        LocalDate.ofEpochDay(notesPerDay[notesPerDay.size / 2].day).format(format),
                        "Today",
                    ).forEach {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlinkedNotes(
    notes: List<NoteSummary>,
    total: Int,
    navigateToLinkRecordsPage: (String) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (total == 1) "1 note isn't linked" else "$total notes aren't linked",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Link them to the people and organizations they're about.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            notes.forEach { note ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = noteName(note),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { navigateToLinkRecordsPage(note.id) }) { Text("Link") }
                }
            }
        }
    }
}

@Composable
private fun BackupCard(backup: BackupState, today: LocalDate, navigateToBackupSettingsPage: () -> Unit) {
    val (title, text) = backupCardText(backup, today, ZoneId.systemDefault()) ?: return
    val never = backup == BackupState.NeverBackedUp
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (never) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (never) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Backup, contentDescription = null)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = navigateToBackupSettingsPage,
                modifier = Modifier.align(Alignment.End),
            ) { Text("Back up now") }
        }
    }
}

// ---------- Helpers ----------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecordTypeIcon(type: RecordType, colorFilter: ColorFilter, size: Dp) {
    when (type) {
        RecordType.Contact -> ContactIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Organization -> OrganizationIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Category -> CategoryIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Item -> ItemIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.Note -> NoteIcon(colorFilter = colorFilter, width = size, height = size)
        RecordType.LinkedRecord -> Icon(
            Icons.Filled.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun recordTypeColors(type: RecordType): RecordTypeColors {
    val dark = isSystemInDarkTheme()
    return when (type) {
        RecordType.Contact -> if (dark) ContactColorsDark else ContactColorsLight
        RecordType.Organization -> if (dark) OrganizationColorsDark else OrganizationColorsLight
        RecordType.Category -> if (dark) CategoryColorsDark else CategoryColorsLight
        else -> if (dark) ItemColorsDark else ItemColorsLight
    }
}

private fun pluralName(type: RecordType) = when (type) {
    RecordType.Contact -> "Contacts"
    RecordType.Organization -> "Organizations"
    RecordType.Category -> "Categories"
    RecordType.Item -> "Items"
    RecordType.Note -> "Notes"
    RecordType.LinkedRecord -> "Links"
}

private fun singularName(type: RecordType) = when (type) {
    RecordType.Category -> "Category"
    else -> pluralName(type).removeSuffix("s")
}

internal fun linkCountText(count: Int) = when (count) {
    0 -> "No links"
    1 -> "1 link"
    else -> "$count links"
}

/** A note date as the note list shows it: the stored day, read in UTC. */
private fun noteDateText(createDate: Long): String =
    Instant.ofEpochMilli(createDate).atZone(ZoneId.of("GMT"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd · E"))

private fun firstLine(text: String?): String? =
    text?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }

private fun noteName(note: NoteSummary): String =
    note.title?.takeIf { it.isNotBlank() } ?: firstLine(note.textContents) ?: "Untitled note"

/**
 * Title and text of the backup card, or null when it's hidden. The backup time is a system
 * time, so its date is shown in [zone], the device's time zone.
 */
internal fun backupCardText(backup: BackupState, today: LocalDate, zone: ZoneId): Pair<String, String>? = when (backup) {
    BackupState.Hidden -> null
    BackupState.NeverBackedUp -> "Not backed up" to
            "Your database isn't backed up. It's stored only in this app on this device, and " +
            "you'll lose it if you uninstall the app or lose the phone. Back it up to keep it safe."
    is BackupState.ChangedSince -> {
        val date = Instant.ofEpochMilli(backup.lastBackupAt).atZone(zone).toLocalDate()
        val title = when (val days = ChronoUnit.DAYS.between(date, today)) {
            0L -> "Last backup today"
            1L -> "Last backup yesterday"
            else -> "Last backup $days days ago"
        }
        val changes = when (backup.changeCount) {
            0 -> null
            1 -> "1 record changed since"
            else -> "${backup.changeCount} records changed since"
        }
        val detail = when {
            changes != null && backup.deletedSince -> "$changes, and some records were deleted."
            changes != null -> "$changes."
            else -> "Some records were deleted since."
        }
        title to "${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} · $detail"
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomePagePreview() {
    GnoseisTheme {
        Surface {
            val today = LocalDate.of(2026, 10, 25)
            HomePageScaffold(
                pageState = HomePageState(
                    today = today,
                    counts = RecordCounts(contacts = 48, organizations = 17, notes = 132, categories = 9, items = 23, links = 214),
                    mostLinked = listOf(
                        LinkedRecordCount("a", "Clients", RecordType.Category.recordTypeId, 21),
                        LinkedRecordCount("b", "Maya Ortiz", RecordType.Contact.recordTypeId, 16),
                        LinkedRecordCount("c", "Northwind", RecordType.Organization.recordTypeId, 14),
                        LinkedRecordCount("d", "Harbor & Pine", RecordType.Organization.recordTypeId, 11),
                        LinkedRecordCount("e", "Daniel Park", RecordType.Contact.recordTypeId, 9),
                        LinkedRecordCount("f", "Warehouse audit", RecordType.Item.recordTypeId, 7),
                    ),
                    mostLinkedLinks = listOf(LinkEnds("a", "b"), LinkEnds("a", "c"), LinkEnds("b", "e")),
                    recentNotes = listOf(
                        NoteSummary("n1", "Call with Maya about Phase 2", "Maya confirmed the revised timeline.", 1_792_713_600_000, 2),
                        NoteSummary("n2", "Ideas for the clinic website", "A simpler booking page.", 1_792_627_200_000, 0),
                    ),
                    notesPerDay = (0 until HomeViewModel.CHART_DAYS).map {
                        DayCount(today.toEpochDay() - HomeViewModel.CHART_DAYS + 1 + it, it % 3)
                    },
                    unlinkedNotes = listOf(NoteSummary("n2", "Ideas for the clinic website", null, 0, 0)),
                    unlinkedNoteCount = 1,
                    backup = BackupState.ChangedSince(1_791_849_600_000, 18, deletedSince = false),
                    isLoaded = true,
                ),
            )
        }
    }
}
