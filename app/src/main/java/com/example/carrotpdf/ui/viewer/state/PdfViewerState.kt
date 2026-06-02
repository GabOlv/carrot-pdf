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

data class PdfPageScrollTarget(
    val pageIndex: Int,
    val normalizedX: Float? = null,
    val normalizedY: Float? = null
)

@Stable
class PdfViewerState(
    val documentId: String,
    pageCount: Int,
    initialPageIndex: Int = 0,
    initialZoom: Float = PdfViewportState.DEFAULT_ZOOM,
    val initialViewportLeftPx: Float = 0f,
    val initialViewportTopPx: Float = 0f,
    val viewportState: PdfViewportState = PdfViewportState(initialZoom)
) {
    var pageCount by mutableIntStateOf(pageCount.coerceAtLeast(0))
        private set

    var currentPageIndex by mutableIntStateOf(coercePageIndex(initialPageIndex))
        private set

    var scrollTarget by mutableStateOf<PdfPageScrollTarget?>(null)
        private set

    val scrollTargetPage: Int?
        get() = scrollTarget?.pageIndex

    var interactionMode by mutableStateOf(PdfInteractionMode.Idle)
        private set

    var visiblePages by mutableStateOf(PdfVisiblePages.Empty)
        private set

    val zoom: Float
        get() = viewportState.visualScale

    val renderQualityScale: Float
        get() = viewportState.renderQualityScale

    val metrics: PdfViewerMetrics
        get() = PdfViewerMetrics(
            pageCount = pageCount,
            currentPageIndex = currentPageIndex,
            zoom = viewportState.displayZoom,
            visiblePageRange = visiblePages.visibleRange
        )

    val canUpdateCurrentPageFromScroll: Boolean
        get() = interactionMode == PdfInteractionMode.Idle

    val canRunRenderScheduler: Boolean
        get() = interactionMode != PdfInteractionMode.Zooming &&
            interactionMode != PdfInteractionMode.Panning &&
            interactionMode != PdfInteractionMode.Settling

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
        interactionMode = PdfInteractionMode.Idle
        return viewportState.setVisualScale(zoom)
    }

    fun advanceZoomPreset(): Float {
        interactionMode = PdfInteractionMode.Idle
        return viewportState.advanceZoomPreset()
    }

    fun beginTransientTransform() {
        viewportState.beginTransform()
        interactionMode = PdfInteractionMode.Zooming
    }

    fun updateTransientTransform(
        zoomChange: Float,
        pan: Offset,
        centroid: Offset
    ) {
        viewportState.updateTransform(
            zoomChange = zoomChange,
            pan = pan,
            centroid = centroid
        )
    }

    fun canPanContent(): Boolean {
        return viewportState.canPanContent
    }

    fun beginPan() {
        interactionMode = PdfInteractionMode.Panning
    }

    fun updatePan(delta: Offset): Boolean {
        return viewportState.panBy(delta)
    }

    fun endPan() {
        if (interactionMode == PdfInteractionMode.Panning) {
            interactionMode = PdfInteractionMode.Idle
        }
    }

    fun beginSettlingTransform() {
        interactionMode = PdfInteractionMode.Settling
    }

    fun endTransientTransform(): Float {
        val zoom = viewportState.endTransform()
        interactionMode = PdfInteractionMode.Settling
        return zoom
    }

    fun cancelTransientTransform() {
        viewportState.cancelTransform()
        interactionMode = PdfInteractionMode.Idle
    }

    fun refineRenderQualityIfNeeded(): Boolean {
        return viewportState.refineRenderQualityIfNeeded()
    }

    fun markRenderQualityDisplayed() {
        viewportState.markRenderQualityDisplayed()
    }

    fun finishSettling() {
        if (interactionMode == PdfInteractionMode.Settling) {
            interactionMode = PdfInteractionMode.Idle
        }
    }

    fun requestScrollToPage(pageIndex: Int) {
        val targetPage = coercePageIndex(pageIndex)
        scrollTarget = PdfPageScrollTarget(pageIndex = targetPage)
        interactionMode = PdfInteractionMode.ProgrammaticScroll
    }

    fun requestScrollToPageLocation(
        pageIndex: Int,
        normalizedX: Float?,
        normalizedY: Float?
    ) {
        val targetPage = coercePageIndex(pageIndex)
        scrollTarget = PdfPageScrollTarget(
            pageIndex = targetPage,
            normalizedX = normalizedX?.coerceIn(0f, 1f),
            normalizedY = normalizedY?.coerceIn(0f, 1f)
        )
        interactionMode = PdfInteractionMode.ProgrammaticScroll
    }

    fun consumeScrollTarget() {
        scrollTarget = null

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
    initialZoom: Float = PdfViewportState.DEFAULT_ZOOM,
    initialViewportLeftPx: Float = 0f,
    initialViewportTopPx: Float = 0f
): PdfViewerState {
    val state = remember(documentId) {
        PdfViewerState(
            documentId = documentId,
            pageCount = pageCount,
            initialPageIndex = initialPageIndex,
            initialZoom = initialZoom,
            initialViewportLeftPx = initialViewportLeftPx,
            initialViewportTopPx = initialViewportTopPx
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
