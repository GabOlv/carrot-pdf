package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap

data class PdfRenderResult(
    val bitmap: Bitmap,
    val pageCount: Int
)

fun renderPdfPage(
    context: Context,
    uri: Uri,
    pageIndex: Int
): PdfRenderResult? {
    var fileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null

    return try {
        fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return null

        pdfRenderer = PdfRenderer(fileDescriptor)

        if (pdfRenderer.pageCount == 0) {
            return null
        }

        val safePageIndex = pageIndex.coerceIn(0, pdfRenderer.pageCount - 1)
        page = pdfRenderer.openPage(safePageIndex)

        val scale = 2
        val bitmap = createBitmap(page.width * scale, page.height * scale)

        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        PdfRenderResult(
            bitmap = bitmap,
            pageCount = pdfRenderer.pageCount
        )
    } catch (exception: Exception) {
        exception.printStackTrace()
        null
    } finally {
        page?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}