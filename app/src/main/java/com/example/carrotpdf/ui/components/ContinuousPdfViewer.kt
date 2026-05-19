package com.example.carrotpdf.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext

@Composable
fun ContinuousPdfViewer(
    tabId: String,
    uri: Uri,
    pageCount: Int,
    currentPageIndex: Int,
    zoom: Float,
    scrollTargetPage: Int?,
    onCurrentPageChange: (Int) -> Unit,
    onScrollTargetConsumed: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val horizontalScrollState = rememberScrollState()

    val screenWidth = configuration.screenWidthDp.dp
    val pageWidth = (screenWidth - 24.dp) * zoom
    val viewerWidth = if (pageWidth > screenWidth) pageWidth + 24.dp else screenWidth

    key(tabId) {
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = currentPageIndex.coerceIn(
                0,
                (pageCount - 1).coerceAtLeast(0)
            )
        )

        val pageCache = remember(tabId, uri) {
            PdfPageCache(maxPages = 5)
        }

        DisposableEffect(tabId, uri) {
            onDispose {
                pageCache.clear()
            }
        }

        LaunchedEffect(scrollTargetPage) {
            val target = scrollTargetPage

            if (target != null && target in 0 until pageCount) {
                listState.animateScrollToItem(target)
                onScrollTargetConsumed()
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { visiblePageIndex ->
                    onCurrentPageChange(visiblePageIndex)
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CarrotColors.PdfCanvas)
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(viewerWidth)
                    .fillMaxHeight(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 14.dp
                )
            ) {
                items(
                    count = pageCount,
                    key = { pageIndex -> "$uri-$pageIndex" }
                ) { pageIndex ->
                    PdfPageItem(
                        context = context,
                        uri = uri,
                        pageIndex = pageIndex,
                        pageWidth = pageWidth,
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
    pageCache: PdfPageCache
) {
    var bitmap by remember(uri, pageIndex) {
        mutableStateOf<Bitmap?>(pageCache.get(pageIndex))
    }

    var failed by remember(uri, pageIndex) {
        mutableStateOf(false)
    }

    LaunchedEffect(uri, pageIndex) {
        val cachedBitmap = pageCache.get(pageIndex)

        if (cachedBitmap != null) {
            bitmap = cachedBitmap
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
            .height(pageWidth * 1.414f),
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
                PageMessage(
                    text = "Could not render page ${pageIndex + 1}"
                )
            }

            bitmap == null -> {
                PageMessage(
                    text = "Rendering page ${pageIndex + 1}..."
                )
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