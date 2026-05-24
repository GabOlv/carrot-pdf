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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
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
    pageSizes: List<PdfPageSize> = emptyList(),
    onLinkTap: (PdfLinkRegion) -> Unit = {},
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
                                onLinkTap = onLinkTap
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
    onLinkTap: (PdfLinkRegion) -> Unit
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

                    PdfLinkInteractionOverlay(
                        linkRegions = linkRegions,
                        onLinkTap = onLinkTap,
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
                Color(0x88FF8A1F)
            } else {
                Color(0x77FFD84D)
            }

            result.bounds.forEach { bound ->
                if (bound.pageWidth > 0f && bound.pageHeight > 0f) {
                    val left = (bound.left / bound.pageWidth) * size.width
                    val top = (bound.top / bound.pageHeight) * size.height
                    val right = (bound.right / bound.pageWidth) * size.width
                    val bottom = (bound.bottom / bound.pageHeight) * size.height

                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(
                            width = (right - left).coerceAtLeast(2f),
                            height = (bottom - top).coerceAtLeast(2f)
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

private fun List<PdfLinkRegion>.linkRegionsForPage(pageIndex: Int): List<PdfLinkRegion> {
    return filter { it.pageIndex == pageIndex }
}

@Composable
private fun PdfLinkInteractionOverlay(
    linkRegions: List<PdfLinkRegion>,
    onLinkTap: (PdfLinkRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    if (linkRegions.isEmpty()) {
        return
    }

    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    var selectedLink by remember(linkRegions) {
        mutableStateOf<PdfLinkRegion?>(null)
    }
    var selectedAtMillis by remember(linkRegions) {
        mutableStateOf(0L)
    }

    Box(
        modifier = modifier.pointerInput(linkRegions) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downLink = linkRegions.hitTestLink(
                    x = down.position.x,
                    y = down.position.y,
                    width = size.width.toFloat(),
                    height = size.height.toFloat()
                ) ?: return@awaitEachGesture

                val downPosition = down.position
                val downTime = down.uptimeMillis
                var hasMoved = false
                var hasMultiplePointers = false
                var wasTapLike = true
                var upLink: PdfLinkRegion? = null

                do {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { change -> change.pressed }

                    if (pressedChanges.size > 1) {
                        hasMultiplePointers = true
                    }

                    event.changes.firstOrNull { change -> change.id == down.id }?.let { change ->
                        if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
                            hasMoved = true
                        }

                        if (change.changedToUpIgnoreConsumed()) {
                            wasTapLike = change.uptimeMillis - downTime <= LINK_TAP_MAX_DURATION_MS
                            upLink = linkRegions.hitTestLink(
                                x = change.position.x,
                                y = change.position.y,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )

                            if (
                                !hasMoved &&
                                !hasMultiplePointers &&
                                wasTapLike &&
                                upLink != null
                            ) {
                                down.consume()
                                change.consume()
                            }
                        }
                    }
                } while (event.changes.any { change -> change.pressed })

                if (!hasMoved && !hasMultiplePointers && wasTapLike) {
                    val tappedLink = upLink

                    if (tappedLink != null && tappedLink.target == downLink.target) {
                        val selected = selectedLink
                        val isSameSelection = selected != null && selected.target == tappedLink.target
                        val isWithinDoubleTapWindow = downTime - selectedAtMillis <= LINK_DOUBLE_TAP_WINDOW_MS

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
        link.target is PdfLinkTarget.ExternalUri &&
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
