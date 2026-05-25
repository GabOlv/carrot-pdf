package com.example.carrotpdf.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.pdf.PdfLinkRegion
import com.example.carrotpdf.pdf.PdfLinkTarget
import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.pdf.PdfTextSelection
import com.example.carrotpdf.pdf.PdfTextSelectionHandle
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.ui.viewer.debug.PdfViewerDebug
import com.example.carrotpdf.ui.viewer.layout.buildPdfDocumentGeometry
import com.example.carrotpdf.ui.viewer.layout.rememberPdfPageLayout
import com.example.carrotpdf.ui.viewer.layout.visiblePagesForViewport
import com.example.carrotpdf.ui.viewer.render.PdfPageRenderState
import com.example.carrotpdf.ui.viewer.render.PdfRenderScheduler
import com.example.carrotpdf.ui.viewer.render.PdfRenderSchedulerState
import com.example.carrotpdf.ui.viewer.render.buildRenderKey
import com.example.carrotpdf.ui.viewer.render.rememberPdfRenderScheduler
import com.example.carrotpdf.ui.viewer.state.PdfInteractionMode
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.viewport.PdfViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
fun ContinuousPdfViewer(
    uri: Uri,
    viewerState: PdfViewerState,
    onCurrentPageChange: (Int) -> Unit,
    onZoomCommitted: (Float) -> Unit,
    searchResults: List<PdfSearchResult> = emptyList(),
    activeSearchResultIndex: Int = -1,
    linkRegions: List<PdfLinkRegion> = emptyList(),
    selectedTextSelection: PdfTextSelection? = null,
    pageSizes: List<PdfPageSize> = emptyList(),
    onLinkTap: (PdfLinkRegion) -> Unit = {},
    onTextLongPress: (pageIndex: Int, normalizedX: Float, normalizedY: Float) -> Unit = { _, _, _ -> },
    onTextSelectionHandleDrag: (
        handle: PdfTextSelectionHandle,
        pageIndex: Int,
        normalizedX: Float,
        normalizedY: Float
    ) -> Unit = { _, _, _, _ -> },
    onUserInteraction: () -> Unit = {},
    pageIndicatorContent: @Composable BoxScope.(
        currentPage: Int,
        pageCount: Int,
        isScrollInProgress: Boolean,
        scrollProgress: Float,
        onScrollToProgress: (Float) -> Unit
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = configuration.screenWidthDp.dp
    val pageLayout = rememberPdfPageLayout(
        viewportWidth = screenWidth
    )
    val documentGeometry = remember(
        viewerState.documentId,
        viewerState.pageCount,
        pageSizes,
        pageLayout,
        density
    ) {
        buildPdfDocumentGeometry(
            pageCount = viewerState.pageCount,
            pageSizes = pageSizes,
            contentWidthPx = with(density) { pageLayout.contentWidth.toPx() },
            pageWidthPx = with(density) { pageLayout.pageWidth.toPx() },
            horizontalPaddingPx = with(density) { pageLayout.horizontalPadding.toPx() },
            verticalPaddingPx = with(density) { pageLayout.verticalPadding.toPx() },
            pageSpacingPx = with(density) { pageLayout.pageSpacing.toPx() }
        )
    }

    LaunchedEffect(documentGeometry) {
        val firstFrame = documentGeometry.pageFrames.firstOrNull()
        val lastFrame = documentGeometry.pageFrames.lastOrNull()

        PdfViewerDebug.log {
            "documentGeometry doc=${viewerState.documentId} pages=${documentGeometry.pageCount} " +
                "contentWidth=${documentGeometry.contentWidthPx} pageWidth=${documentGeometry.pageWidthPx} " +
                "paddingH=${documentGeometry.horizontalPaddingPx} paddingV=${documentGeometry.verticalPaddingPx} " +
                "spacing=${documentGeometry.pageSpacingPx} totalHeight=${documentGeometry.totalHeightPx} " +
                "first=${firstFrame?.let { "#${it.pageIndex + 1}:${it.topPx}-${it.bottomPx} h=${it.heightPx}" }} " +
                "last=${lastFrame?.let { "#${it.pageIndex + 1}:${it.topPx}-${it.bottomPx} h=${it.heightPx}" }}"
        }
    }

    key(viewerState.documentId) {
        var hasInitializedScroll by remember { mutableStateOf(false) }
        var isManualScrollInProgress by remember { mutableStateOf(false) }
        var renderRefinementRequests by remember { mutableIntStateOf(0) }

        val renderSchedulerState = rememberPdfRenderScheduler(
            context = context,
            uri = uri,
            documentId = viewerState.documentId
        )

        LaunchedEffect(documentGeometry) {
            viewerState.viewportState.updateContentSize(
                IntSize(
                    width = documentGeometry.contentWidthPx.roundToInt().coerceAtLeast(0),
                    height = documentGeometry.totalHeightPx.roundToInt().coerceAtLeast(0)
                )
            )
        }

        LaunchedEffect(documentGeometry, viewerState.documentId) {
            if (!hasInitializedScroll) {
                val initialPageIndex = viewerState.currentPageIndex.coerceIn(
                    0,
                    (viewerState.pageCount - 1).coerceAtLeast(0)
                )
                val initialTopPx = documentGeometry.pageFrames
                    .getOrNull(initialPageIndex)
                    ?.topPx
                    ?.coerceIn(0f, documentGeometry.totalHeightPx)
                    ?: 0f
                viewerState.viewportState.setViewportOrigin(
                    leftPx = 0f,
                    topPx = initialTopPx
                )
                hasInitializedScroll = true
            }
        }

        val viewportState = viewerState.viewportState
        val scale = viewportState.visualScale.coerceAtLeast(0.001f)
        val viewportSize = viewportState.viewportSize
        val viewportHeightUnscaled = if (viewportSize.height > 0) {
            viewportSize.height / scale
        } else {
            0f
        }
        val viewportWidthUnscaled = if (viewportSize.width > 0) {
            viewportSize.width / scale
        } else {
            0f
        }
        val maxViewportTopPx = (documentGeometry.totalHeightPx - viewportHeightUnscaled)
            .coerceAtLeast(0f)
        val viewportOrigin = viewportState.viewportOrigin()
        val geometryViewportTop = viewportOrigin.y
            .coerceIn(0f, maxViewportTopPx)
        val geometryViewportBottom = geometryViewportTop + viewportHeightUnscaled
        val geometryViewportLeft = viewportOrigin.x
        val geometryViewportRight = geometryViewportLeft + viewportWidthUnscaled
        val geometryVisiblePages = remember(
            documentGeometry,
            geometryViewportTop,
            geometryViewportBottom
        ) {
            documentGeometry.visiblePagesForViewport(
                viewportTopPx = geometryViewportTop,
                viewportBottomPx = geometryViewportBottom
            )
        }
        val pagesToCompose = geometryVisiblePages.activeRange
            ?: documentGeometry.pageFrames
                .firstOrNull()
                ?.let { 0..0 }
            ?: IntRange.EMPTY
        val scrollProgress = if (maxViewportTopPx > 0f) {
            (geometryViewportTop / maxViewportTopPx).coerceIn(0f, 1f)
        } else {
            0f
        }
        val scrollableState = rememberScrollableState { delta ->
            val consumed = viewerState.viewportState.scrollVerticallyBy(delta)

            if (consumed != 0f) {
                isManualScrollInProgress = true
                onUserInteraction()
            }

            consumed
        }

        LaunchedEffect(isManualScrollInProgress, viewportState.panOffset) {
            if (isManualScrollInProgress) {
                delay(MANUAL_SCROLL_IDLE_DEBOUNCE_MS)
                isManualScrollInProgress = false
            }
        }

        LaunchedEffect(geometryVisiblePages, viewerState.interactionMode) {
            viewerState.updateVisiblePages(geometryVisiblePages)

            val firstVisiblePage = geometryVisiblePages.firstVisiblePage

            if (
                viewerState.canUpdateCurrentPageFromScroll &&
                firstVisiblePage != null &&
                viewerState.updateCurrentPageIndex(firstVisiblePage)
            ) {
                onCurrentPageChange(firstVisiblePage)
            }
        }

        LaunchedEffect(
            viewerState.documentId,
            documentGeometry,
            viewportState.visualScale,
            viewportState.panOffset,
            viewportState.viewportSize
        ) {
            snapshotFlow {
                val viewportContentSize = viewerState.viewportState.contentSize
                val visibleComparisons = pagesToCompose.joinToString(
                    separator = ";",
                    limit = 6
                ) { pageIndex ->
                    val frame = documentGeometry.pageFrames.getOrNull(pageIndex)

                    "#${pageIndex + 1}:geomTop=${frame?.topPx},geomHeight=${frame?.heightPx}"
                }
                val scaledGeometryHeight = documentGeometry.totalHeightPx * viewerState.viewportState.visualScale
                val scaledViewportContentHeight = viewportContentSize.height * viewerState.viewportState.visualScale

                "geometryCompare doc=${viewerState.documentId} " +
                    "geometryTotal=${documentGeometry.totalHeightPx} scaledGeometryTotal=$scaledGeometryHeight " +
                    "viewportContent=${viewportContentSize.width}x${viewportContentSize.height} scaledViewportContentHeight=$scaledViewportContentHeight " +
                    "viewportTop=$geometryViewportTop viewportBottom=$geometryViewportBottom " +
                    "visibleCompare=[$visibleComparisons]"
            }
                .distinctUntilChanged()
                .collect { message ->
                    PdfViewerDebug.log { message }
                }
        }

        LaunchedEffect(
            viewerState.documentId,
            documentGeometry,
            viewportState.visualScale,
            viewportState.panOffset,
            viewportState.viewportSize
        ) {
            snapshotFlow {
                "geometryVisible doc=${viewerState.documentId} " +
                    "origin=$viewportOrigin scale=$scale pan=${viewportState.panOffset} " +
                    "viewportPx=${viewportSize.width}x${viewportSize.height} " +
                    "geometryViewportX=$geometryViewportLeft-$geometryViewportRight " +
                    "geometryViewportY=$geometryViewportTop-$geometryViewportBottom " +
                    "geometryVisible=${geometryVisiblePages.visibleRange} active=${geometryVisiblePages.activeRange} primary=${geometryVisiblePages.primaryVisiblePage} " +
                    "composed=$pagesToCompose"
            }
                .distinctUntilChanged()
                .collect { message ->
                    PdfViewerDebug.log { message }
                }
        }

        PdfRenderScheduler(
            schedulerState = renderSchedulerState,
            documentId = viewerState.documentId,
            visiblePages = geometryVisiblePages,
            renderQualityScale = viewerState.renderQualityScale,
            isEnabled = viewerState.canRunRenderScheduler,
            onRenderQualityDisplayed = viewerState::markRenderQualityDisplayed
        )

        LaunchedEffect(viewerState.interactionMode) {
            if (
                viewerState.interactionMode != PdfInteractionMode.Idle &&
                viewerState.interactionMode != PdfInteractionMode.ProgrammaticScroll
            ) {
                onUserInteraction()
            }
        }

        LaunchedEffect(viewerState.scrollTarget) {
            val target = viewerState.scrollTarget

            if (target != null && target.pageIndex in 0 until viewerState.pageCount) {
                val targetFrame = documentGeometry.pageFrames.getOrNull(target.pageIndex)
                val targetTopPx = if (targetFrame != null && target.normalizedY != null) {
                    targetFrame.topPx +
                        (targetFrame.heightPx * target.normalizedY) -
                        (viewportHeightUnscaled * SEARCH_TARGET_VERTICAL_ANCHOR)
                } else {
                    targetFrame?.topPx ?: geometryViewportTop
                }.coerceIn(0f, maxViewportTopPx)

                val targetLeftPx = if (targetFrame != null && target.normalizedX != null) {
                    targetFrame.leftPx +
                        (targetFrame.widthPx * target.normalizedX) -
                        (viewportWidthUnscaled / 2f)
                } else {
                    geometryViewportLeft
                }

                viewerState.viewportState.setViewportOrigin(
                    leftPx = targetLeftPx,
                    topPx = targetTopPx
                )
                viewerState.consumeScrollTarget()
            }
        }

        LaunchedEffect(renderRefinementRequests) {
            if (renderRefinementRequests == 0) {
                return@LaunchedEffect
            }

            delay(RENDER_REFINEMENT_DEBOUNCE_MS)
            viewerState.refineRenderQualityIfNeeded()
            viewerState.finishSettling()
        }

        PdfViewport(
            viewerState = viewerState,
            viewportState = viewerState.viewportState,
            pageLayout = pageLayout,
            onTransformEnded = {
                onZoomCommitted(viewerState.zoom)
                renderRefinementRequests += 1
            },
            modifier = Modifier
                .fillMaxSize()
                .background(CarrotColors.PdfCanvas)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollable(
                        state = scrollableState,
                        orientation = Orientation.Vertical
                    )
            ) {
                pagesToCompose.forEach { pageIndex ->
                    val frame = documentGeometry.pageFrames.getOrNull(pageIndex)
                        ?: return@forEach
                    val pageX = ((frame.leftPx - geometryViewportLeft) * scale).roundToInt()
                    val pageY = ((frame.topPx - geometryViewportTop) * scale).roundToInt()

                    key("$uri-$pageIndex") {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = pageX,
                                        y = pageY
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                        ) {
                            val pageWidth = with(density) { frame.widthPx.toDp() }

                            PdfPageItem(
                                renderSchedulerState = renderSchedulerState,
                                documentId = viewerState.documentId,
                                pageIndex = pageIndex,
                                pageWidth = pageWidth,
                                pageAspectRatio = pageSizes.getOrNull(pageIndex)?.aspectRatio,
                                renderQualityScale = viewerState.renderQualityScale,
                                searchResults = searchResults.searchResultsForPage(pageIndex),
                                activeSearchResult = searchResults.getOrNull(activeSearchResultIndex),
                                linkRegions = linkRegions.linkRegionsForPage(pageIndex),
                                selectedTextSelection = selectedTextSelection
                                    ?.takeIf { selection -> selection.hasSelectionOnPage(pageIndex) },
                                onLinkTap = onLinkTap,
                                onTextLongPress = onTextLongPress,
                                onTextSelectionHandleDrag = onTextSelectionHandleDrag
                            )
                        }
                    }
                }
            }

            pageIndicatorContent(
                viewerState.currentPageIndex + 1,
                viewerState.pageCount.coerceAtLeast(1),
                isManualScrollInProgress || viewerState.interactionMode != PdfInteractionMode.Idle,
                scrollProgress
                ) { targetProgress ->
                    val targetTopPx = (targetProgress.coerceIn(0f, 1f) * maxViewportTopPx)
                        .coerceIn(0f, maxViewportTopPx)
                    val currentLeftPx = viewerState.viewportState.viewportOrigin().x

                    viewerState.viewportState.setViewportOrigin(
                        leftPx = currentLeftPx,
                        topPx = targetTopPx
                    )
                }
        }
    }
}

@Composable
private fun PdfPageItem(
    renderSchedulerState: PdfRenderSchedulerState,
    documentId: String,
    pageIndex: Int,
    pageWidth: androidx.compose.ui.unit.Dp,
    pageAspectRatio: Float?,
    renderQualityScale: Float,
    searchResults: List<PdfSearchResult>,
    activeSearchResult: PdfSearchResult?,
    linkRegions: List<PdfLinkRegion>,
    selectedTextSelection: PdfTextSelection?,
    onLinkTap: (PdfLinkRegion) -> Unit,
    onTextLongPress: (pageIndex: Int, normalizedX: Float, normalizedY: Float) -> Unit,
    onTextSelectionHandleDrag: (
        handle: PdfTextSelectionHandle,
        pageIndex: Int,
        normalizedX: Float,
        normalizedY: Float
    ) -> Unit
) {
    val renderKey = remember(
        documentId,
        pageIndex,
        renderQualityScale
    ) {
        buildRenderKey(
            documentId = documentId,
            pageIndex = pageIndex,
            renderQualityScale = renderQualityScale
        )
    }

    val renderState = renderSchedulerState.stateFor(renderKey)
    var displayedBitmap by remember(documentId, pageIndex) {
        mutableStateOf<Bitmap?>(null)
    }
    val density = LocalDensity.current

    val readyBitmap = (renderState as? PdfPageRenderState.Ready)?.bitmap

    LaunchedEffect(readyBitmap) {
        if (readyBitmap != null && !readyBitmap.isRecycled) {
            PdfViewerDebug.log {
                "renderBitmap doc=$documentId page=${pageIndex + 1} bitmap=${readyBitmap.width}x${readyBitmap.height} quality=$renderQualityScale"
            }
            displayedBitmap = readyBitmap
        }
    }

    val bitmapToShow = when {
        readyBitmap != null && !readyBitmap.isRecycled -> readyBitmap
        displayedBitmap != null && displayedBitmap?.isRecycled == false -> displayedBitmap
        else -> null
    }

    val resolvedPageAspectRatio = pageAspectRatio ?: if (bitmapToShow != null) {
        bitmapToShow.height.toFloat() / bitmapToShow.width.toFloat()
    } else {
        DEFAULT_PAGE_ASPECT_RATIO
    }

    val pageHeight = pageWidth * resolvedPageAspectRatio

    LaunchedEffect(
        documentId,
        pageIndex,
        pageWidth,
        pageHeight,
        pageAspectRatio,
        resolvedPageAspectRatio
    ) {
        PdfViewerDebug.log {
            val pageWidthPx = with(density) { pageWidth.roundToPx() }
            val pageHeightPx = with(density) { pageHeight.roundToPx() }

            "pageMeasure doc=$documentId page=${pageIndex + 1} " +
                "widthDp=$pageWidth heightDp=$pageHeight widthPx=$pageWidthPx heightPx=$pageHeightPx " +
                "sourceAspect=$pageAspectRatio resolvedAspect=$resolvedPageAspectRatio"
        }
    }

    Card(
        modifier = Modifier
            .width(pageWidth)
            .height(pageHeight),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        when {
            bitmapToShow != null -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        bitmap = bitmapToShow.asImageBitmap(),
                        contentDescription = "PDF page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    SearchHighlightOverlay(
                        results = searchResults,
                        activeSearchResult = activeSearchResult,
                        modifier = Modifier.fillMaxSize()
                    )

                    TextSelectionOverlay(
                        pageIndex = pageIndex,
                        selection = selectedTextSelection,
                        modifier = Modifier.fillMaxSize()
                    )

                    PdfPageInteractionOverlay(
                        pageIndex = pageIndex,
                        linkRegions = linkRegions,
                        selectedTextSelection = selectedTextSelection,
                        onLinkTap = onLinkTap,
                        onTextLongPress = onTextLongPress,
                        onTextSelectionHandleDrag = onTextSelectionHandleDrag,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            renderState == PdfPageRenderState.Failed -> {
                PageMessage("Could not render page ${pageIndex + 1}")
            }

            renderState == PdfPageRenderState.Loading -> {
                PageMessage("Rendering page ${pageIndex + 1}...")
            }

            else -> {
                PageMessage("Page ${pageIndex + 1}")
            }
        }
    }
}

@Composable
private fun SearchHighlightOverlay(
    results: List<PdfSearchResult>,
    activeSearchResult: PdfSearchResult?,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        return
    }

    Canvas(modifier = modifier) {
        results.forEach { result ->
            val isActive = result == activeSearchResult
            val color = if (isActive) {
                Color(0x66FF8A1F)
            } else {
                Color(0x55FFD84D)
            }

            result.bounds.forEach { bound ->
                if (bound.pageWidth > 0f && bound.pageHeight > 0f) {
                    val left = (bound.left / bound.pageWidth) * size.width
                    val top = (bound.top / bound.pageHeight) * size.height
                    val right = (bound.right / bound.pageWidth) * size.width
                    val bottom = (bound.bottom / bound.pageHeight) * size.height
                    val rect = expandHighlightRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        maxHeight = size.height
                    )

                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                        size = androidx.compose.ui.geometry.Size(
                            width = rect.width,
                            height = rect.height
                        ),
                        style = Fill
                    )
                }
            }
        }
    }
}

private fun List<PdfSearchResult>.searchResultsForPage(pageIndex: Int): List<PdfSearchResult> {
    return filter { it.pageIndex == pageIndex }
}

private fun expandHighlightRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    maxHeight: Float
): Rect {
    val width = (right - left).coerceAtLeast(2f)
    val height = (bottom - top).coerceAtLeast(2f)
    val verticalExpansion = height * HIGHLIGHT_VERTICAL_EXPANSION_RATIO
    val expandedTop = (top - verticalExpansion).coerceIn(0f, maxHeight)
    val expandedBottom = (bottom + verticalExpansion).coerceIn(0f, maxHeight)

    return Rect(
        left = left,
        top = expandedTop,
        right = left + width,
        bottom = expandedBottom.coerceAtLeast(expandedTop + 2f)
    )
}

private fun PdfTextSelection.hasSelectionOnPage(
    pageIndex: Int
): Boolean {
    return pageRanges.any { range -> range.pageIndex == pageIndex } ||
        bounds.any { bound -> bound.pageIndex == pageIndex }
}

private fun PdfTextSelection.selectionRectsForCanvas(
    pageIndex: Int,
    width: Float,
    height: Float
): List<Rect> {
    if (width <= 0f || height <= 0f) {
        return emptyList()
    }

    return bounds
        .filter { bound -> bound.pageIndex == pageIndex }
        .mapNotNull { bound ->
            if (bound.pageWidth > 0f && bound.pageHeight > 0f) {
                val left = (bound.left / bound.pageWidth) * width
                val top = (bound.top / bound.pageHeight) * height
                val right = (bound.right / bound.pageWidth) * width
                val bottom = (bound.bottom / bound.pageHeight) * height

                expandHighlightRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    maxHeight = height
                )
            } else {
                null
            }
        }
}

private fun PdfTextSelection.hitTestSelectionHandle(
    pageIndex: Int,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    handleStemPx: Float,
    hitRadiusPx: Float
): PdfTextSelectionHandle? {
    val rects = selectionRectsForCanvas(
        pageIndex = pageIndex,
        width = width,
        height = height
    )
    val firstRange = pageRanges.firstOrNull()
    val lastRange = pageRanges.lastOrNull()
    val firstRect = rects.firstOrNull()
    val lastRect = rects.lastOrNull()

    val touch = androidx.compose.ui.geometry.Offset(x, y)
    val startDistance = if (firstRange?.pageIndex == pageIndex && firstRect != null) {
        val startCenter = androidx.compose.ui.geometry.Offset(
            x = firstRect.left,
            y = firstRect.bottom + handleStemPx
        )
        (touch - startCenter).getDistance()
    } else {
        Float.POSITIVE_INFINITY
    }
    val endDistance = if (lastRange?.pageIndex == pageIndex && lastRect != null) {
        val endCenter = androidx.compose.ui.geometry.Offset(
            x = lastRect.right,
            y = lastRect.bottom + handleStemPx
        )
        (touch - endCenter).getDistance()
    } else {
        Float.POSITIVE_INFINITY
    }

    return when {
        startDistance <= hitRadiusPx && startDistance <= endDistance -> PdfTextSelectionHandle.Start
        endDistance <= hitRadiusPx -> PdfTextSelectionHandle.End
        else -> null
    }
}

private fun List<PdfLinkRegion>.linkRegionsForPage(pageIndex: Int): List<PdfLinkRegion> {
    return filter { it.pageIndex == pageIndex }
}

@Composable
private fun TextSelectionOverlay(
    pageIndex: Int,
    selection: PdfTextSelection?,
    modifier: Modifier = Modifier
) {
    if (selection == null || selection.bounds.isEmpty()) {
        return
    }

    Canvas(modifier = modifier) {
        val selectionRects = selection.selectionRectsForCanvas(
            pageIndex = pageIndex,
            width = size.width,
            height = size.height
        )

        selectionRects.forEach { rect ->
            drawRect(
                color = TEXT_SELECTION_FILL,
                topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                size = androidx.compose.ui.geometry.Size(
                    width = rect.width,
                    height = rect.height
                ),
                style = Fill
            )
        }

        val firstRect = selectionRects.firstOrNull()
        val lastRect = selectionRects.lastOrNull()
        val drawsStartHandle = selection.pageRanges.firstOrNull()?.pageIndex == pageIndex
        val drawsEndHandle = selection.pageRanges.lastOrNull()?.pageIndex == pageIndex

        if (firstRect != null && lastRect != null) {
            val handleRadius = 6.dp.toPx()
            val handleStem = TEXT_SELECTION_HANDLE_STEM.toPx()
            val startHandleY = firstRect.bottom + handleStem
            val endHandleY = lastRect.bottom + handleStem

            if (drawsStartHandle) {
                drawLine(
                    color = TEXT_SELECTION_HANDLE,
                    start = androidx.compose.ui.geometry.Offset(firstRect.left, firstRect.center.y),
                    end = androidx.compose.ui.geometry.Offset(firstRect.left, startHandleY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = TEXT_SELECTION_HANDLE,
                    radius = handleRadius,
                    center = androidx.compose.ui.geometry.Offset(firstRect.left, startHandleY),
                    style = Fill
                )
            }

            if (drawsEndHandle) {
                drawLine(
                    color = TEXT_SELECTION_HANDLE,
                    start = androidx.compose.ui.geometry.Offset(lastRect.right, lastRect.center.y),
                    end = androidx.compose.ui.geometry.Offset(lastRect.right, endHandleY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = TEXT_SELECTION_HANDLE,
                    radius = handleRadius,
                    center = androidx.compose.ui.geometry.Offset(lastRect.right, endHandleY),
                    style = Fill
                )
            }
        }
    }
}

@Composable
private fun PdfPageInteractionOverlay(
    pageIndex: Int,
    linkRegions: List<PdfLinkRegion>,
    selectedTextSelection: PdfTextSelection?,
    onLinkTap: (PdfLinkRegion) -> Unit,
    onTextLongPress: (pageIndex: Int, normalizedX: Float, normalizedY: Float) -> Unit,
    onTextSelectionHandleDrag: (
        handle: PdfTextSelectionHandle,
        pageIndex: Int,
        normalizedX: Float,
        normalizedY: Float
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val handleHitRadiusPx = with(density) { TEXT_SELECTION_HANDLE_HIT_RADIUS.toPx() }
    val handleStemPx = with(density) { TEXT_SELECTION_HANDLE_STEM.toPx() }
    val currentSelection = rememberUpdatedState(selectedTextSelection)
    val currentHandleDrag = rememberUpdatedState(onTextSelectionHandleDrag)
    var selectedLink by remember(linkRegions) {
        mutableStateOf<PdfLinkRegion?>(null)
    }
    var selectedAtMillis by remember(linkRegions) {
        mutableStateOf(0L)
    }

    Box(
        modifier = modifier.pointerInput(pageIndex, linkRegions) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downHandle = currentSelection.value?.hitTestSelectionHandle(
                    pageIndex = pageIndex,
                    x = down.position.x,
                    y = down.position.y,
                    width = size.width.toFloat(),
                    height = size.height.toFloat(),
                    handleStemPx = handleStemPx,
                    hitRadiusPx = handleHitRadiusPx
                )

                if (downHandle != null) {
                    selectedLink = null
                    selectedAtMillis = 0L
                    down.consume()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    var isHandlePressed: Boolean
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { pointerChange -> pointerChange.id == down.id }

                        if (change != null) {
                            if (size.width > 0 && size.height > 0) {
                                currentHandleDrag.value(
                                    downHandle,
                                    pageIndex,
                                    (change.position.x / size.width.toFloat()).coerceIn(0f, 1f),
                                    change.position.y / size.height.toFloat()
                                )
                            }

                            change.consume()
                        }

                        isHandlePressed = event.changes.any { change -> change.pressed }
                    } while (isHandlePressed)

                    return@awaitEachGesture
                }

                val downLink = linkRegions.hitTestLink(
                    x = down.position.x,
                    y = down.position.y,
                    width = size.width.toFloat(),
                    height = size.height.toFloat()
                )

                val downPosition = down.position
                val downTime = down.uptimeMillis
                var lastPosition = down.position
                var upTime = downTime
                var hasMoved = false
                var hasMultiplePointers = false
                var wasTapLike = true
                var isPressed = true
                var upLink: PdfLinkRegion? = null

                val timedOutForLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { change -> change.pressed }

                        if (pressedChanges.size > 1) {
                            hasMultiplePointers = true
                            return@withTimeoutOrNull false
                        }

                        val change = event.changes.firstOrNull { pointerChange -> pointerChange.id == down.id }
                            ?: return@withTimeoutOrNull false

                        lastPosition = change.position

                        if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
                            hasMoved = true
                            return@withTimeoutOrNull false
                        }

                        if (change.changedToUpIgnoreConsumed()) {
                            isPressed = false
                            upTime = change.uptimeMillis
                            wasTapLike = upTime - downTime <= LINK_TAP_MAX_DURATION_MS
                            upLink = linkRegions.hitTestLink(
                                x = change.position.x,
                                y = change.position.y,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )

                            if (
                                downLink != null &&
                                upLink != null &&
                                upLink?.target == downLink.target &&
                                wasTapLike
                            ) {
                                down.consume()
                                change.consume()
                            }

                            return@withTimeoutOrNull false
                        }
                    }
                } == null

                if (timedOutForLongPress && isPressed && !hasMoved && !hasMultiplePointers) {
                    selectedLink = null
                    selectedAtMillis = 0L

                    if (size.width > 0 && size.height > 0) {
                        onTextLongPress(
                            pageIndex,
                            (lastPosition.x / size.width.toFloat()).coerceIn(0f, 1f),
                            (lastPosition.y / size.height.toFloat()).coerceIn(0f, 1f)
                        )
                    }

                    drainPressedPointers(consume = true)
                    return@awaitEachGesture
                }

                if (hasMoved || hasMultiplePointers) {
                    selectedLink = null
                    selectedAtMillis = 0L
                    drainPressedPointers(consume = false)
                    return@awaitEachGesture
                }

                if (!hasMoved && !hasMultiplePointers && wasTapLike) {
                    val tappedLink = upLink

                    if (downLink != null && tappedLink != null && tappedLink.target == downLink.target) {
                        val selected = selectedLink
                        val isSameSelection = selected != null && selected.target == tappedLink.target
                        val isWithinDoubleTapWindow = upTime - selectedAtMillis <= LINK_DOUBLE_TAP_WINDOW_MS

                        if (isSameSelection && isWithinDoubleTapWindow) {
                            selectedLink = null
                            selectedAtMillis = 0L
                            onLinkTap(tappedLink)
                        } else {
                            selectedLink = tappedLink
                            selectedAtMillis = downTime
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                } else if (hasMoved || hasMultiplePointers) {
                    selectedLink = null
                    selectedAtMillis = 0L
                }
            }
        }
    )
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.drainPressedPointers(
    consume: Boolean
) {
    do {
        val event = awaitPointerEvent()

        if (consume) {
            event.changes.forEach { change ->
                change.consume()
            }
        }
    } while (event.changes.any { change -> change.pressed })
}

private fun List<PdfLinkRegion>.hitTestLink(
    x: Float,
    y: Float,
    width: Float,
    height: Float
): PdfLinkRegion? {
    if (width <= 0f || height <= 0f) {
        return null
    }

    return firstOrNull { link ->
        link.target != PdfLinkTarget.Unsupported &&
            link.bounds.any { bound ->
                if (bound.pageWidth <= 0f || bound.pageHeight <= 0f) {
                    false
                } else {
                    val left = (bound.left / bound.pageWidth) * width
                    val top = (bound.top / bound.pageHeight) * height
                    val right = (bound.right / bound.pageWidth) * width
                    val bottom = (bound.bottom / bound.pageHeight) * height

                    x in left..right && y in top..bottom
                }
            }
    }
}

private const val DEFAULT_PAGE_ASPECT_RATIO = 1.414f
private const val RENDER_REFINEMENT_DEBOUNCE_MS = 420L
private const val SEARCH_TARGET_VERTICAL_ANCHOR = 0.35f
private const val MANUAL_SCROLL_IDLE_DEBOUNCE_MS = 180L
private const val LINK_TAP_MAX_DURATION_MS = 700L
private const val LINK_DOUBLE_TAP_WINDOW_MS = 650L
private const val HIGHLIGHT_VERTICAL_EXPANSION_RATIO = 0.18f
private val TEXT_SELECTION_HANDLE_STEM = 8.dp
private val TEXT_SELECTION_HANDLE_HIT_RADIUS = 28.dp
private val TEXT_SELECTION_FILL = Color(0x4435B9F5)
private val TEXT_SELECTION_HANDLE = Color(0xCC35B9F5)

@Composable
private fun PageMessage(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.DarkGray
        )
    }
}
