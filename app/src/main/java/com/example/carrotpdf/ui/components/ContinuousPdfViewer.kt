package com.example.carrotpdf.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.viewer.layout.rememberPdfPageLayout
import com.example.carrotpdf.ui.viewer.layout.rememberPdfPageVirtualizer
import com.example.carrotpdf.ui.viewer.render.PdfPageRenderState
import com.example.carrotpdf.ui.viewer.render.PdfRenderScheduler
import com.example.carrotpdf.ui.viewer.render.PdfRenderSchedulerState
import com.example.carrotpdf.ui.viewer.render.buildRenderKey
import com.example.carrotpdf.ui.viewer.render.rememberPdfRenderScheduler
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.viewport.PdfViewport
import kotlinx.coroutines.delay

@Composable
fun ContinuousPdfViewer(
    uri: Uri,
    viewerState: PdfViewerState,
    onCurrentPageChange: (Int) -> Unit,
    onZoomCommitted: (Float) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp
    val pageLayout = rememberPdfPageLayout(
        viewportWidth = screenWidth
    )

    key(viewerState.documentId) {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = viewerState.currentPageIndex.coerceIn(
                0,
                (viewerState.pageCount - 1).coerceAtLeast(0)
            )
        )

        var renderRefinementRequests by remember { mutableIntStateOf(0) }

        val virtualizerState = rememberPdfPageVirtualizer(
            listState = listState,
            pageCount = viewerState.pageCount,
            onVisiblePagesChange = { visiblePages ->
                viewerState.updateVisiblePages(visiblePages)

                val firstVisiblePage = visiblePages.firstVisiblePage

                if (
                    viewerState.canUpdateCurrentPageFromScroll &&
                    firstVisiblePage != null &&
                    viewerState.updateCurrentPageIndex(firstVisiblePage)
                ) {
                    onCurrentPageChange(firstVisiblePage)
                }
            }
        )

        val renderSchedulerState = rememberPdfRenderScheduler(
            context = context,
            uri = uri,
            documentId = viewerState.documentId
        )

        PdfRenderScheduler(
            schedulerState = renderSchedulerState,
            documentId = viewerState.documentId,
            visiblePages = virtualizerState.visiblePages,
            renderQualityScale = viewerState.renderQualityScale,
            onRenderQualityDisplayed = viewerState::markRenderQualityDisplayed
        )

        LaunchedEffect(viewerState.scrollTargetPage) {
            val target = viewerState.scrollTargetPage

            if (target != null && target in 0 until viewerState.pageCount) {
                listState.animateScrollToItem(target)
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
            LazyColumn(
                modifier = Modifier
                    .width(pageLayout.contentWidth)
                    .fillMaxHeight(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(pageLayout.pageSpacing),
                contentPadding = PaddingValues(
                    horizontal = pageLayout.horizontalPadding,
                    vertical = pageLayout.verticalPadding
                )
            ) {
                items(
                    count = viewerState.pageCount,
                    key = { pageIndex -> "$uri-$pageIndex" }
                ) { pageIndex ->
                    PdfPageItem(
                        renderSchedulerState = renderSchedulerState,
                        documentId = viewerState.documentId,
                        pageIndex = pageIndex,
                        pageWidth = pageLayout.pageWidth,
                        renderQualityScale = viewerState.renderQualityScale
                    )
                }
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
    renderQualityScale: Float
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

    val readyBitmap = (renderState as? PdfPageRenderState.Ready)?.bitmap

    LaunchedEffect(readyBitmap) {
        if (readyBitmap != null && !readyBitmap.isRecycled) {
            displayedBitmap = readyBitmap
        }
    }

    val bitmapToShow = when {
        readyBitmap != null && !readyBitmap.isRecycled -> readyBitmap
        displayedBitmap != null && displayedBitmap?.isRecycled == false -> displayedBitmap
        else -> null
    }

    val pageAspectRatio = if (bitmapToShow != null) {
        bitmapToShow.height.toFloat() / bitmapToShow.width.toFloat()
    } else {
        DEFAULT_PAGE_ASPECT_RATIO
    }

    val pageHeight = pageWidth * pageAspectRatio

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
                Image(
                    bitmap = bitmapToShow.asImageBitmap(),
                    contentDescription = "PDF page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
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

private const val DEFAULT_PAGE_ASPECT_RATIO = 1.414f
private const val RENDER_REFINEMENT_DEBOUNCE_MS = 420L

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
