package org.gnoseis.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.gnoseis.data.entity.search.SearchResult
import org.gnoseis.data.enums.RecordType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultsList(
    results: List<SearchResult> = emptyList(),
    onResultClick: (SearchResult) -> Unit,
//    onIncrementSelectedCount: () -> Unit
) {

    LazyColumn(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp)//.scrollable(ScrollableState)
    ){
        val headerGroups = results.groupBy { RecordType.from(it.recordTypeId) }
        headerGroups.forEach { (header, headerItems) ->
            stickyHeader {
                Text(
                    text = header.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    )
            }
            items(headerItems) { item ->
                var backgroundColor : Color = MaterialTheme.colorScheme.surface // Color.Transparent
                var alphaValue: Float = 1f
                if (item.isSelected == true) {
                    backgroundColor = MaterialTheme.colorScheme.errorContainer
                } else if (item.isLinked == true) {
                    alphaValue = 0.4f
                }

                val titleText : String
                if(item.isLinked == true) titleText = "${item.recordTitle} [Already Linked]"
                else titleText = item.recordTitle
                Card(
                    onClick = {if(item.isLinked != true) { onResultClick(item) }}
                ){
                    Column(
                        modifier = Modifier
                            .background(backgroundColor)
                            .alpha(alphaValue),
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }

        }
    }
}

@Preview
@Composable
fun SearchResultsListPreview() {
    SearchResultsList(
        results = listOf(
            SearchResult(
                recordId = "id1",
                recordTypeId = 1,
                recordTitle = "Meeting in Las Vegas",
                isLinked = false,
                isSelected = false
            ),
            SearchResult(
                recordId = "id2",
                recordTypeId = 1,
                recordTitle = "Bought new Model X today",
                isLinked = false,
                isSelected = true
            ),
            SearchResult(
                recordId = "id3",
                recordTypeId = 1,
                recordTitle = "This one is linked",
                isLinked = true,
                isSelected = false
            )
        ),
        onResultClick = {}
    )
}