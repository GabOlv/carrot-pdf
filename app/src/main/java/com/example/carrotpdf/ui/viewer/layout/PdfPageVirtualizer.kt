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

@Stable
class PdfPageVirtualizerState(
    private val overscanPages: Int
) {
    var visiblePages by mutableStateOf(PdfVisiblePages.Empty)
        private set

    fun updateVisibleIndexes(
        visibleIndexes: List<Int>,
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
                activeRange = activeFirstPage..activeLastPage
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
            listState.layoutInfo.visibleItemsInfo
                .map { item -> item.index }
                .filter { pageIndex -> pageIndex in 0 until pageCount }
        }
            .distinctUntilChanged()
            .collect { visibleIndexes ->
                val visiblePages = virtualizerState.updateVisibleIndexes(
                    visibleIndexes = visibleIndexes,
                    pageCount = pageCount
                )

                onVisiblePagesChange(visiblePages)
            }
    }

    return virtualizerState
}
