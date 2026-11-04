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


package org.gnoseis.ui.search

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.gnoseis.data.entity.links.LinkedRecordTypeCount
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.enums.pluralName
import org.gnoseis.data.enums.singularName
import org.gnoseis.ui.components.CreateFromSearch
import org.gnoseis.ui.components.CreateFromSearchOffer
import org.gnoseis.ui.components.RecordTypeIcon
import org.gnoseis.ui.components.creatableRecordTypes
import org.gnoseis.ui.components.recordTypeColors
import org.gnoseis.ui.theme.GnoseisTheme

/** The shortest query search searches for. */
const val SEARCH_MIN_QUERY_LENGTH = 3

/** Whether [query] is long enough to search for. */
fun isSearchQuery(query: String) = query.trim().length >= SEARCH_MIN_QUERY_LENGTH

/** Everything the search component shows. */
data class SearchUiState(
    val query: String = "",
    val placeholder: String = "Search all records",
    /** The type chips offered, in [searchTypeOrder]. */
    val chipTypes: List<RecordType> = searchTypeOrder,
    /** The selected type chips; none means All. */
    val selectedChips: Set<RecordType> = emptySet(),
    /** The type of the list page search was opened from, if any. */
    val scope: RecordType? = null,
    /** The results for [query], after the chips. */
    val results: SearchResultsView = SearchResultsView(),
    /** The links by type of the best match. */
    val bestMatchLinks: List<LinkedRecordTypeCount> = emptyList(),
    /** The most recently changed records, shown before a query is typed. */
    val recent: List<SearchResult> = emptyList(),
    val createOffer: CreateFromSearchOffer = CreateFromSearchOffer.None,
    /** The types create from search offers, in button order. */
    val createTypes: List<RecordType> = creatableRecordTypes,
) {
    val isSearching: Boolean get() = isSearchQuery(query)
}

/** On Link Records: which results are selected and which are linked already. */
data class SearchSelection(
    val selectedIds: Set<String> = emptySet(),
    val linkedIds: Set<String> = emptySet(),
)

/** What the search field starts with: Back (the Search page) or a search icon (Link Records). */
enum class SearchFieldLeading { Back, Search }

/**
 * Search: the field, the type chips and the results (or, before a query is typed, the
 * recently changed records). With [selection], each result has a check (Link Records).
 * [header] is shown above the field.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordSearch(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onChipClick: (RecordType?) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onCreate: (RecordType) -> Unit,
    modifier: Modifier = Modifier,
    leading: SearchFieldLeading = SearchFieldLeading.Search,
    onBack: () -> Unit = {},
    selection: SearchSelection? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    now: Long = remember { System.currentTimeMillis() },
    header: @Composable () -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (state.query.isEmpty()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    // A new query starts at the top of its results; coming back to the same one keeps the place
    var shownQuery by remember { mutableStateOf(state.query) }
    LaunchedEffect(state.query) {
        if (state.query != shownQuery) {
            shownQuery = state.query
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        header()
        SearchField(
            query = state.query,
            placeholder = state.placeholder,
            leading = leading,
            onBack = onBack,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
        )
        SearchChips(
            types = state.chipTypes,
            selected = state.selectedChips,
            counts = if (state.isSearching) state.results.counts else null,
            onChipClick = onChipClick,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (!state.isSearching) {
                recentlyChanged(state.recent, now, selection, onResultClick)
            } else {
                searchResults(state, selection, onResultClick, onChipClick, onCreate)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.searchResults(
    state: SearchUiState,
    selection: SearchSelection?,
    onResultClick: (SearchResult) -> Unit,
    onChipClick: (RecordType?) -> Unit,
    onCreate: (RecordType) -> Unit,
) {
    val view = state.results
    val createBlock: LazyListScope.() -> Unit = {
        item(key = "create") {
            CreateFromSearch(
                query = state.query,
                offer = state.createOffer,
                types = state.createTypes,
                onCreate = onCreate,
            )
        }
    }
    if (state.createOffer == CreateFromSearchOffer.NoResults) {
        createBlock()
        return
    }
    view.bestMatch?.let { best ->
        item(key = "best") {
            BestMatchCard(
                result = best,
                query = state.query,
                links = state.bestMatchLinks,
                selected = selection?.let { best.recordId in it.selectedIds },
                onClick = { onResultClick(best) },
            )
        }
    }
    view.groups.forEach { group ->
        stickyHeader(key = "header-${group.type}") {
            GroupHeader(group.type, group.results.size)
        }
        items(group.results, key = { it.recordId }) { result ->
            ResultRow(
                result = result,
                query = state.query,
                context = searchContextLine(result),
                selection = selection,
                onClick = { onResultClick(result) },
            )
        }
    }
    if (view.isEmpty && view.hiddenTotal > 0) {
        item(key = "no-match") {
            Text(
                text = "No ${selectedNames(state.selectedChips)} match “${state.query.trim()}”.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (state.scope != null && view.hiddenTotal > 0) {
        item(key = "more") {
            MoreInOtherTypesCard(
                hidden = view.hidden,
                onShowAll = { onChipClick(null) },
            )
        }
    }
    if (state.createOffer == CreateFromSearchOffer.AfterResults) createBlock()
}

/** "contacts", "notes or contacts", "notes, contacts or items". */
internal fun selectedNames(types: Set<RecordType>): String {
    val names = searchTypeOrder.filter { it in types }.map { pluralName(it).lowercase() }
    return when (names.size) {
        0 -> "records"
        1 -> names.single()
        else -> names.dropLast(1).joinToString(", ") + " or " + names.last()
    }
}

private fun LazyListScope.recentlyChanged(
    recent: List<SearchResult>,
    now: Long,
    selection: SearchSelection?,
    onResultClick: (SearchResult) -> Unit,
) {
    if (recent.isEmpty()) return
    item(key = "recent-header") {
        Text(
            text = "RECENTLY CHANGED",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
    }
    items(recent, key = { "recent-${it.recordId}" }) { result ->
        ResultRow(
            result = result,
            query = "",
            context = result.updatedAt?.let { changedText(it, now) } ?: searchContextLine(result),
            selection = selection,
            onClick = { onResultClick(result) },
        )
    }
}

// ********** Field and chips **********

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    leading: SearchFieldLeading,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (leading) {
                SearchFieldLeading.Back -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                }
                SearchFieldLeading.Search -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = placeholder },
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = colors.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun SearchChips(
    types: List<RecordType>,
    selected: Set<RecordType>,
    counts: Map<RecordType, Int>?,
    onChipClick: (RecordType?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val allSelected = selected.isEmpty()
        FilterChip(
            selected = allSelected,
            onClick = { onChipClick(null) },
            label = { ChipLabel("All", counts?.let { c -> types.sumOf { c[it] ?: 0 } }) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (allSelected) colors.primary else colors.onSurfaceVariant,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.primaryContainer,
                selectedLabelColor = colors.onPrimaryContainer,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = allSelected,
                borderColor = colors.outlineVariant,
                selectedBorderColor = colors.primary,
                selectedBorderWidth = 1.dp,
            ),
        )
        types.forEach { type ->
            val typeColors = recordTypeColors(type)
            val isSelected = type in selected
            val count = counts?.get(type)
            FilterChip(
                selected = isSelected,
                onClick = { onChipClick(type) },
                modifier = Modifier.alpha(if (count == 0 && !isSelected) 0.5f else 1f),
                label = { ChipLabel(pluralName(type), count) },
                leadingIcon = { RecordTypeIcon(type, ColorFilter.tint(typeColors.outline), 18.dp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = typeColors.fill,
                    selectedLabelColor = colors.onSurface,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colors.outlineVariant,
                    selectedBorderColor = typeColors.outline,
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}

@Composable
private fun ChipLabel(name: String, count: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, maxLines = 1)
        if (count != null) {
            Text(text = "$count", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

// ********** Results **********

/** A record type's icon in a circle: [background], a [borderWidth] border in the type's outline. */
@Composable
internal fun TypeAvatar(
    type: RecordType,
    size: Dp,
    background: Color = recordTypeColors(type).fill,
    borderWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier,
) {
    val outline = recordTypeColors(type).outline
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(borderWidth, outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RecordTypeIcon(type, ColorFilter.tint(outline), size * 0.5f)
    }
}

/**
 * [text] with [ranges] in bold, underlined 2dp in [underline]. Underlines are drawn only on
 * the visible part of the text.
 */
@Composable
internal fun HighlightedText(
    text: String,
    ranges: List<IntRange>,
    underline: Color,
    style: TextStyle,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotated = remember(text, ranges) {
        buildAnnotatedString {
            append(text)
            ranges.forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1) }
        }
    }
    Text(
        text = annotated,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val result = layout ?: return@drawBehind
            val stroke = 2.dp.toPx()
            ranges.forEach { range -> drawUnderline(result, range, underline, stroke) }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUnderline(
    layout: TextLayoutResult,
    range: IntRange,
    color: Color,
    stroke: Float,
) {
    val textLength = layout.layoutInput.text.length
    if (range.first >= textLength) return
    val firstLine = layout.getLineForOffset(range.first)
    val lastLine = layout.getLineForOffset(minOf(range.last, textLength - 1))
    for (line in firstLine..lastLine) {
        if (line >= layout.lineCount) break
        val visibleEnd = layout.getLineEnd(line, visibleEnd = true)
        val start = maxOf(range.first, layout.getLineStart(line))
        val end = minOf(range.last + 1, visibleEnd)
        if (end <= start) continue
        val left = layout.getHorizontalPosition(start, usePrimaryDirection = true)
        val right = layout.getHorizontalPosition(end, usePrimaryDirection = true)
        val y = layout.getLineBaseline(line) + stroke * 1.5f
        drawLine(color, Offset(minOf(left, right), y), Offset(maxOf(left, right), y), stroke)
    }
}

/** The round check of a result on Link Records. */
@Composable
internal fun SelectionCheck(type: RecordType, selected: Boolean, border: Color = MaterialTheme.colorScheme.outline) {
    val typeColors = recordTypeColors(type)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(typeColors.outline)
                else Modifier.border(1.5.dp, border, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = typeColors.fill, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LinkedTag() {
    Row(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Linked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A result: its type avatar, title with the match highlighted and [context] line. With
 * [selection], a check at the end; a linked result is dimmed, tagged Linked and not tappable.
 */
@Composable
internal fun ResultRow(
    result: SearchResult,
    query: String,
    context: String,
    selection: SearchSelection?,
    onClick: () -> Unit,
    selectedOverride: Boolean? = null,
) {
    val type = result.recordType
    val typeColors = recordTypeColors(type)
    val linked = selection != null && result.recordId in selection.linkedIds
    val selected = selectedOverride ?: (selection != null && result.recordId in selection.selectedIds)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(shape)
            .then(if (selected) Modifier.background(typeColors.fill) else Modifier)
            .then(
                if (linked) Modifier
                else Modifier.clickable(role = if (selection != null) Role.Checkbox else Role.Button, onClick = onClick)
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .alpha(if (linked) 0.45f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TypeAvatar(type, 38.dp)
        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text = result.recordTitle,
                ranges = highlightRanges(result, query),
                underline = typeColors.outline,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = context,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selection != null) {
            if (linked) LinkedTag() else SelectionCheck(type, selected)
        }
    }
}

/** The best match: on its type's fill, with its links by type under it. */
@Composable
internal fun BestMatchCard(
    result: SearchResult,
    query: String,
    links: List<LinkedRecordTypeCount>,
    selected: Boolean?,
    onClick: () -> Unit,
) {
    val type = result.recordType
    val typeColors = recordTypeColors(type)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(typeColors.fill)
            .clickable(role = if (selected != null) Role.Checkbox else Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "BEST MATCH",
            style = MaterialTheme.typography.labelMedium,
            color = typeColors.outline,
            letterSpacing = 1.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TypeAvatar(type, 44.dp, background = MaterialTheme.colorScheme.surface)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HighlightedText(
                    text = result.recordTitle,
                    ranges = highlightRanges(result, query),
                    underline = typeColors.outline,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Text(
                    text = "${singularName(type)} · ${searchContextLine(result)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val byType = links.associate { RecordType.from(it.recordTypeId) to it.count }
                val shown = searchTypeOrder.filter { (byType[it] ?: 0) > 0 }
                if (shown.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        shown.forEach { linkedType ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = "${byType[linkedType]} ${pluralName(linkedType).lowercase()}"
                                },
                            ) {
                                RecordTypeIcon(linkedType, ColorFilter.tint(recordTypeColors(linkedType).outline), 18.dp)
                                Text(
                                    text = "${byType[linkedType]}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
            if (selected != null) SelectionCheck(type, selected, border = typeColors.outline)
        }
    }
}

@Composable
private fun GroupHeader(type: RecordType, count: Int) {
    val typeColors = recordTypeColors(type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            RecordTypeIcon(type, ColorFilter.tint(typeColors.outline), 20.dp)
            Text(
                text = pluralName(type).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = typeColors.outline,
                letterSpacing = 1.sp,
            )
            Text(
                text = "· $count",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(typeColors.fill)
        )
    }
}

@Composable
private fun MoreInOtherTypesCard(hidden: Map<RecordType, Int>, onShowAll: () -> Unit) {
    val dashColor = MaterialTheme.colorScheme.outline
    val total = hidden.values.sum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = size.copy(width = size.width - stroke, height = size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            searchTypeOrder.filter { it in hidden }.forEach { type ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(recordTypeColors(type).outline)
                )
            }
        }
        Text(
            text = "$total more in other types",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onShowAll) { Text("Show all types") }
    }
}

// ********** Previews **********

private val previewResults = listOf(
    SearchResult("o1", RecordType.Organization.recordTypeId, "Harbor & Pine", linkCount = 11),
    SearchResult("n1", RecordType.Note.recordTypeId, "Harbor & Pine renewal", linkCount = 3, noteDate = 1_760_832_000_000L),
    SearchResult("n2", RecordType.Note.recordTypeId, "Call with Owen Harris about the site", linkCount = 2, noteDate = 1_761_177_600_000L),
    SearchResult("c1", RecordType.Contact.recordTypeId, "Harris, Owen", linkCount = 4, jobTitle = "Site lead", company = "Harbor & Pine"),
    SearchResult("c2", RecordType.Contact.recordTypeId, "Hart, Lena", linkCount = 2, company = "Northwind Traders"),
    SearchResult("k1", RecordType.Category.recordTypeId, "Hardware suppliers", linkCount = 6),
    SearchResult("i1", RecordType.Item.recordTypeId, "Harness kit", linkCount = 0),
)

private fun previewState(selected: Set<RecordType> = emptySet(), scope: RecordType? = null) = SearchUiState(
    query = "har",
    placeholder = scope?.let { "Search ${pluralName(it).lowercase()}" } ?: "Search all records",
    selectedChips = selected,
    scope = scope,
    results = searchResultsView(previewResults, "har", selected),
    bestMatchLinks = listOf(
        LinkedRecordTypeCount(RecordType.Contact.recordTypeId, 5),
        LinkedRecordTypeCount(RecordType.Note.recordTypeId, 3),
        LinkedRecordTypeCount(RecordType.Item.recordTypeId, 2),
        LinkedRecordTypeCount(RecordType.Category.recordTypeId, 1),
    ),
    createOffer = CreateFromSearchOffer.AfterResults,
)

@Preview(name = "Results, light")
@Preview(name = "Results, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecordSearchResultsPreview() {
    GnoseisTheme(dynamicColor = false) {
        Surface {
            RecordSearch(
                state = previewState(),
                onQueryChange = {}, onChipClick = {}, onResultClick = {}, onCreate = {},
                leading = SearchFieldLeading.Back,
            )
        }
    }
}

@Preview(name = "Scoped, light")
@Preview(name = "Scoped, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecordSearchScopedPreview() {
    GnoseisTheme(dynamicColor = false) {
        Surface {
            RecordSearch(
                state = previewState(setOf(RecordType.Contact), RecordType.Contact),
                onQueryChange = {}, onChipClick = {}, onResultClick = {}, onCreate = {},
                leading = SearchFieldLeading.Back,
            )
        }
    }
}

@Preview(name = "Before typing, light")
@Preview(name = "Before typing, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecordSearchRecentPreview() {
    val now = 1_762_000_000_000L
    GnoseisTheme(dynamicColor = false) {
        Surface {
            RecordSearch(
                state = SearchUiState(recent = previewResults.take(5).mapIndexed { i, r -> r.copy(updatedAt = now - i * 26 * 3_600_000L) }),
                onQueryChange = {}, onChipClick = {}, onResultClick = {}, onCreate = {},
                leading = SearchFieldLeading.Back,
                now = now,
            )
        }
    }
}

@Preview(name = "Selection, light")
@Preview(name = "Selection, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecordSearchSelectionPreview() {
    GnoseisTheme(dynamicColor = false) {
        Surface {
            RecordSearch(
                state = previewState().copy(placeholder = "Search records to link"),
                onQueryChange = {}, onChipClick = {}, onResultClick = {}, onCreate = {},
                selection = SearchSelection(selectedIds = setOf("c1"), linkedIds = setOf("c2")),
            )
        }
    }
}
