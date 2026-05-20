package com.example.carrotpdf.ui.viewer.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

class PdfDocumentSession(
    private val context: Context,
    private val uri: Uri
) {
    private val renderMutex = Mutex()

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    suspend fun renderPage(key: PdfRenderKey): Bitmap? {
        return renderMutex.withLock {
            renderPageLocked(key)
        }
    }

    fun close() {
        renderer?.close()
        fileDescriptor?.close()

        renderer = null
        fileDescriptor = null
    }

    private fun renderPageLocked(key: PdfRenderKey): Bitmap? {
        var page: PdfRenderer.Page? = null

        return try {
            val pdfRenderer = openRenderer() ?: return null

            if (pdfRenderer.pageCount == 0) {
                return null
            }

            val safePageIndex = key.pageIndex.coerceIn(0, pdfRenderer.pageCount - 1)
            page = pdfRenderer.openPage(safePageIndex)

            val renderScale = renderScaleFor(key.scaleBucketPercent)
            val bitmap = createBitmap(
                width = (page.width * renderScale).roundToInt().coerceAtLeast(1),
                height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
            )

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            bitmap
        } catch (exception: Exception) {
            exception.printStackTrace()
            null
        } finally {
            page?.close()
        }
    }

    private fun openRenderer(): PdfRenderer? {
        val activeRenderer = renderer

        if (activeRenderer != null) {
            return activeRenderer
        }

        val activeFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return null

        fileDescriptor = activeFileDescriptor
        renderer = PdfRenderer(activeFileDescriptor)

        return renderer
    }

    private fun renderScaleFor(scaleBucketPercent: Int): Float {
        val zoomScale = scaleBucketPercent / 100f
        return (BASE_RENDER_SCALE * zoomScale).coerceIn(
            MIN_RENDER_SCALE,
            MAX_RENDER_SCALE
        )
    }

    private companion object {
        const val BASE_RENDER_SCALE = 2f
        const val MIN_RENDER_SCALE = 1f
        const val MAX_RENDER_SCALE = 5f
    }
}
