package com.example.carrotpdf.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.pdf.PdfPageCache
import com.example.carrotpdf.pdf.renderPdfPage
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.viewer.layout.rememberPdfPageVirtualizer
import com.example.carrotpdf.ui.viewer.layout.rememberPdfPageLayout
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.viewport.PdfViewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        viewportWidth = screenWidth,
        viewportState = viewerState.viewportState
    )

    key(viewerState.documentId) {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = viewerState.currentPageIndex.coerceIn(
                0,
                (viewerState.pageCount - 1).coerceAtLeast(0)
            )
        )

        val virtualizerState = rememberPdfPageVirtualizer(
            listState = listState,
            pageCount = viewerState.pageCount,
            onVisiblePagesChange = { visiblePages ->
                viewerState.updateVisiblePages(visiblePages)

                val firstVisiblePage = visiblePages.firstVisiblePage

                if (
                    firstVisiblePage != null &&
                    viewerState.updateCurrentPageIndex(firstVisiblePage)
                ) {
                    onCurrentPageChange(firstVisiblePage)
                }
            }
        )

        val pageCache = remember(viewerState.documentId, uri) {
            PdfPageCache(maxPages = 5)
        }

        DisposableEffect(viewerState.documentId, uri) {
            onDispose {
                pageCache.clear()
            }
        }

        LaunchedEffect(viewerState.scrollTargetPage) {
            val target = viewerState.scrollTargetPage

            if (target != null && target in 0 until viewerState.pageCount) {
                listState.animateScrollToItem(target)
                viewerState.consumeScrollTarget()
            }
        }

        PdfViewport(
            viewerState = viewerState,
            viewportState = viewerState.viewportState,
            pageLayout = pageLayout,
            onZoomCommitted = onZoomCommitted,
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
                        context = context,
                        uri = uri,
                        pageIndex = pageIndex,
                        pageWidth = pageLayout.pageWidth,
                        pageHeight = pageLayout.pageHeight,
                        shouldRender = virtualizerState.visiblePages.isActive(pageIndex),
                        pageCache = pageCache
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    context: Context,
    uri: Uri,
    pageIndex: Int,
    pageWidth: androidx.compose.ui.unit.Dp,
    pageHeight: androidx.compose.ui.unit.Dp,
    shouldRender: Boolean,
    pageCache: PdfPageCache
) {
    var bitmap by remember(uri, pageIndex) {
        mutableStateOf<Bitmap?>(pageCache.get(pageIndex))
    }

    var failed by remember(uri, pageIndex) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        uri,
        pageIndex,
        shouldRender
    ) {
        val cachedBitmap = pageCache.get(pageIndex)

        if (cachedBitmap != null) {
            bitmap = cachedBitmap
            failed = false
            return@LaunchedEffect
        }

        if (!shouldRender) {
            failed = false
            return@LaunchedEffect
        }

        failed = false
        bitmap = null

        val result = withContext(Dispatchers.IO) {
            renderPdfPage(
                context = context,
                uri = uri,
                pageIndex = pageIndex
            )
        }

        if (result == null) {
            failed = true
        } else {
            pageCache.put(pageIndex, result.bitmap)
            bitmap = result.bitmap
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
            failed -> {
                PageMessage("Could not render page ${pageIndex + 1}")
            }

            bitmap == null && shouldRender -> {
                PageMessage("Rendering page ${pageIndex + 1}...")
            }

            bitmap == null -> {
                PageMessage("Page ${pageIndex + 1}")
            }

            else -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "PDF page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

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
