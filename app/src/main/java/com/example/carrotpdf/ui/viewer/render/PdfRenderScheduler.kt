package com.example.carrotpdf.ui.viewer.render

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.example.carrotpdf.ui.viewer.state.PdfVisiblePages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Stable
class PdfRenderSchedulerState(
    private val context: Context,
    private val uri: Uri,
    private val cache: PdfRenderCache = PdfRenderCache(),
    private val documentSession: PdfDocumentSession = PdfDocumentSession(
        context = context,
        uri = uri
    )
) {
    private val pageStates = mutableStateMapOf<PdfRenderKey, PdfPageRenderState>()

    fun stateFor(key: PdfRenderKey): PdfPageRenderState {
        val cachedBitmap = cache.get(key)

        if (cachedBitmap != null) {
            return PdfPageRenderState.Ready(cachedBitmap)
        }

        val fallbackBitmap = cache.getClosestScale(key)
        val currentState = pageStates[key]

        if (fallbackBitmap != null) {
            return when (currentState) {
                PdfPageRenderState.Loading,
                PdfPageRenderState.Failed,
                PdfPageRenderState.NotRequested,
                null -> PdfPageRenderState.Ready(
                    bitmap = fallbackBitmap,
                    isFallback = true
                )

                is PdfPageRenderState.Ready -> currentState
            }
        }

        return currentState ?: PdfPageRenderState.NotRequested
    }

    suspend fun render(requests: List<PdfRenderRequest>) {
        requests.forEach { request ->
            coroutineContext.ensureActive()

            val key = request.key

            if (cache.get(key) != null) {
                pageStates.remove(key)
                return@forEach
            }

            if (pageStates[key] is PdfPageRenderState.Loading) {
                return@forEach
            }

            pageStates[key] = PdfPageRenderState.Loading

            var renderedBitmap: Bitmap? = null

            try {
                renderedBitmap = withContext(Dispatchers.IO) {
                    documentSession.renderPage(key)
                }

                coroutineContext.ensureActive()

                val bitmap = renderedBitmap

                if (bitmap == null) {
                    pageStates[key] = PdfPageRenderState.Failed
                } else {
                    cache.put(key, bitmap)
                    pageStates.remove(key)
                    renderedBitmap = null
                }
            } catch (exception: CancellationException) {
                if (pageStates[key] is PdfPageRenderState.Loading) {
                    pageStates.remove(key)
                }

                if (renderedBitmap != null && !renderedBitmap.isRecycled) {
                    renderedBitmap.recycle()
                }

                throw exception
            }
        }
    }

    fun clear() {
        pageStates.clear()
        cache.clear()
        documentSession.close()
    }
}

@Composable
fun rememberPdfRenderScheduler(
    context: Context,
    uri: Uri,
    documentId: String
): PdfRenderSchedulerState {
    val applicationContext = context.applicationContext

    return remember(documentId, uri) {
        PdfRenderSchedulerState(
            context = applicationContext,
            uri = uri
        )
    }
}

@Composable
fun PdfRenderScheduler(
    schedulerState: PdfRenderSchedulerState,
    documentId: String,
    visiblePages: PdfVisiblePages,
    zoom: Float
) {
    val scaleBucketPercent = pdfRenderScaleBucketPercent(zoom)

    DisposableEffect(schedulerState) {
        onDispose {
            schedulerState.clear()
        }
    }

    LaunchedEffect(
        schedulerState,
        documentId,
        visiblePages,
        scaleBucketPercent
    ) {
        schedulerState.render(
            buildRenderRequests(
                documentId = documentId,
                visiblePages = visiblePages,
                scaleBucketPercent = scaleBucketPercent
            )
        )
    }
}

fun buildRenderKey(
    documentId: String,
    pageIndex: Int,
    zoom: Float
): PdfRenderKey {
    return PdfRenderKey(
        documentId = documentId,
        pageIndex = pageIndex,
        scaleBucketPercent = pdfRenderScaleBucketPercent(zoom)
    )
}

private fun buildRenderRequests(
    documentId: String,
    visiblePages: PdfVisiblePages,
    scaleBucketPercent: Int
): List<PdfRenderRequest> {
    val activeRange = visiblePages.activeRange ?: return emptyList()
    val visibleRange = visiblePages.visibleRange

    return activeRange
        .map { pageIndex ->
            PdfRenderRequest(
                key = PdfRenderKey(
                    documentId = documentId,
                    pageIndex = pageIndex,
                    scaleBucketPercent = scaleBucketPercent
                ),
                priority = if (visibleRange?.contains(pageIndex) == true) {
                    PdfRenderPriority.Visible
                } else {
                    PdfRenderPriority.Nearby
                }
            )
        }
        .sortedWith(
            compareBy<PdfRenderRequest> { request -> request.priority.ordinal }
                .thenBy { request ->
                    val firstVisiblePage = visibleRange?.first ?: request.key.pageIndex
                    kotlin.math.abs(request.key.pageIndex - firstVisiblePage)
                }
        )
}
