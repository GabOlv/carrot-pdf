package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

data class PdfPageSize(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float
        get() = if (width > 0) {
            height.toFloat() / width.toFloat()
        } else {
            DEFAULT_PAGE_ASPECT_RATIO
        }
}

fun getPdfPageCount(
    context: Context,
    uri: Uri
): Int {
    var fileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null

    return try {
        fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return 0

        pdfRenderer = PdfRenderer(fileDescriptor)

        pdfRenderer.pageCount
    } catch (exception: Exception) {
        exception.printStackTrace()
        0
    } finally {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

fun getPdfPageSizes(
    context: Context,
    uri: Uri
): List<PdfPageSize> {
    var fileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null

    return try {
        fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return emptyList()

        pdfRenderer = PdfRenderer(fileDescriptor)

        buildList {
            for (pageIndex in 0 until pdfRenderer.pageCount) {
                var page: PdfRenderer.Page? = null

                try {
                    page = pdfRenderer.openPage(pageIndex)
                    add(
                        PdfPageSize(
                            width = page.width,
                            height = page.height
                        )
                    )
                } finally {
                    page?.close()
                }
            }
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
        emptyList()
    } finally {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}

private const val DEFAULT_PAGE_ASPECT_RATIO = 1.414f
