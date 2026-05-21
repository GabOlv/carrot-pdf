package com.example.carrotpdf.ui.viewer.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.example.carrotpdf.ui.viewer.state.PdfVisiblePages
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.max
import kotlin.math.min

@Stable
class PdfPageVirtualizerState(
    private val overscanPages: Int
) {
    var visiblePages by mutableStateOf(PdfVisiblePages.Empty)
        private set

    fun updateVisibleIndexes(
        visibleIndexes: List<Int>,
        primaryVisiblePage: Int?,
        pageCount: Int
    ): PdfVisiblePages {
        val firstVisiblePage = visibleIndexes.minOrNull()
        val lastVisiblePage = visibleIndexes.maxOrNull()

        visiblePages = if (
            firstVisiblePage == null ||
            lastVisiblePage == null ||
            pageCount <= 0
        ) {
            PdfVisiblePages.Empty
        } else {
            val activeFirstPage = (firstVisiblePage - overscanPages).coerceAtLeast(0)
            val activeLastPage = (lastVisiblePage + overscanPages).coerceAtMost(pageCount - 1)

            PdfVisiblePages(
                visibleRange = firstVisiblePage..lastVisiblePage,
                activeRange = activeFirstPage..activeLastPage,
                primaryVisiblePage = primaryVisiblePage
            )
        }

        return visiblePages
    }
}

@Composable
fun rememberPdfPageVirtualizer(
    listState: LazyListState,
    pageCount: Int,
    overscanPages: Int = 1,
    onVisiblePagesChange: (PdfVisiblePages) -> Unit
): PdfPageVirtualizerState {
    val virtualizerState = remember(overscanPages) {
        PdfPageVirtualizerState(
            overscanPages = overscanPages
        )
    }

    LaunchedEffect(
        listState,
        pageCount,
        virtualizerState
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val visibleItems = layoutInfo.visibleItemsInfo
                .filter { item -> item.index in 0 until pageCount }
            val primaryVisiblePage = visibleItems
                .maxByOrNull { item ->
                    val itemStart = item.offset
                    val itemEnd = item.offset + item.size
                    val visibleStart = max(itemStart, viewportStart)
                    val visibleEnd = min(itemEnd, viewportEnd)

                    (visibleEnd - visibleStart).coerceAtLeast(0)
                }
                ?.index

            visibleItems.map { item -> item.index } to primaryVisiblePage
        }
            .distinctUntilChanged()
            .collect { (visibleIndexes, primaryVisiblePage) ->
                val visiblePages = virtualizerState.updateVisibleIndexes(
                    visibleIndexes = visibleIndexes,
                    primaryVisiblePage = primaryVisiblePage,
                    pageCount = pageCount
                )

                onVisiblePagesChange(visiblePages)
            }
    }

    return virtualizerState
}
