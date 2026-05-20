package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

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
