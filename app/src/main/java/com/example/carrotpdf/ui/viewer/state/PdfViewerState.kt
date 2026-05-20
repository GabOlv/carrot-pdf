package com.example.carrotpdf.ui.viewer.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class PdfViewerState(
    val documentId: String,
    pageCount: Int,
    initialPageIndex: Int = 0,
    initialZoom: Float = PdfViewportState.DEFAULT_ZOOM,
    val viewportState: PdfViewportState = PdfViewportState(initialZoom)
) {
    var pageCount by mutableIntStateOf(pageCount.coerceAtLeast(0))
        private set

    var currentPageIndex by mutableIntStateOf(coercePageIndex(initialPageIndex))
        private set

    var scrollTargetPage by mutableStateOf<Int?>(null)
        private set

    var interactionMode by mutableStateOf(PdfInteractionMode.Idle)
        private set

    var visiblePages by mutableStateOf(PdfVisiblePages.Empty)
        private set

    val zoom: Float
        get() = viewportState.committedZoom

    val metrics: PdfViewerMetrics
        get() = PdfViewerMetrics(
            pageCount = pageCount,
            currentPageIndex = currentPageIndex,
            zoom = viewportState.displayZoom,
            visiblePageRange = visiblePages.visibleRange
        )

    fun updatePageCount(pageCount: Int) {
        this.pageCount = pageCount.coerceAtLeast(0)
        currentPageIndex = coercePageIndex(currentPageIndex)
    }

    fun updateCurrentPageIndex(pageIndex: Int): Boolean {
        val nextPageIndex = coercePageIndex(pageIndex)

        if (nextPageIndex == currentPageIndex) {
            return false
        }

        currentPageIndex = nextPageIndex
        return true
    }

    fun updateVisiblePages(visiblePages: PdfVisiblePages) {
        this.visiblePages = visiblePages
    }

    fun setZoom(zoom: Float): Float {
        return viewportState.setCommittedZoom(zoom)
    }

    fun advanceZoomPreset(): Float {
        return viewportState.advanceZoomPreset()
    }

    fun beginTransientTransform() {
        viewportState.beginTransientZoom()
        interactionMode = PdfInteractionMode.Zooming
    }

    fun updateTransientTransform(
        zoomChange: Float,
        pan: Offset,
        centroid: Offset
    ) {
        viewportState.updateTransientTransform(
            zoomChange = zoomChange,
            pan = pan,
            centroid = centroid
        )
    }

    fun commitTransientTransform(): Float {
        val zoom = viewportState.commitTransientZoom()
        interactionMode = PdfInteractionMode.Idle
        return zoom
    }

    fun requestScrollToPage(pageIndex: Int) {
        val targetPage = coercePageIndex(pageIndex)
        scrollTargetPage = targetPage
        interactionMode = PdfInteractionMode.ProgrammaticScroll
    }

    fun consumeScrollTarget() {
        scrollTargetPage = null

        if (interactionMode == PdfInteractionMode.ProgrammaticScroll) {
            interactionMode = PdfInteractionMode.Idle
        }
    }

    fun updateInteractionMode(mode: PdfInteractionMode) {
        interactionMode = mode
    }

    private fun coercePageIndex(pageIndex: Int): Int {
        val lastPageIndex = (pageCount - 1).coerceAtLeast(0)
        return pageIndex.coerceIn(0, lastPageIndex)
    }
}

@Composable
fun rememberPdfViewerState(
    documentId: String,
    pageCount: Int,
    initialPageIndex: Int = 0,
    initialZoom: Float = PdfViewportState.DEFAULT_ZOOM
): PdfViewerState {
    val state = remember(documentId) {
        PdfViewerState(
            documentId = documentId,
            pageCount = pageCount,
            initialPageIndex = initialPageIndex,
            initialZoom = initialZoom
        )
    }

    LaunchedEffect(pageCount) {
        state.updatePageCount(pageCount)
    }

    LaunchedEffect(initialZoom) {
        state.setZoom(initialZoom)
    }

    return state
}
