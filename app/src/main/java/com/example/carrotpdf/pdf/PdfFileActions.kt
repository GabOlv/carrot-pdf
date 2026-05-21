package com.example.carrotpdf.pdf

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.getSystemService
import java.io.FileOutputStream

fun sharePdf(
    context: Context,
    uri: Uri,
    title: String
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Enviar Arquivo"
        )
    )
}

fun printPdf(
    context: Context,
    uri: Uri,
    title: String
) {
    val printManager = context.getSystemService<PrintManager>() ?: return

    printManager.print(
        title,
        UriPrintDocumentAdapter(
            context = context,
            uri = uri,
            title = title
        ),
        PrintAttributes.Builder().build()
    )
}

fun downloadPdf(
    context: Context,
    uri: Uri,
    title: String
): Boolean {
    val resolver = context.contentResolver
    val fileName = title.ensurePdfFileName()

    return runCatching {
        val outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
        } else {
            return false
        }

        resolver.openInputStream(uri)?.use { input ->
            resolver.openOutputStream(outputUri)?.use { output ->
                input.copyTo(output)
            }
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(outputUri, values, null, null)
        }

        true
    }.getOrDefault(false)
}

fun displayNameForUri(
    resolver: ContentResolver,
    uri: Uri
): String? {
    val cursor = resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )

    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (displayNameIndex >= 0) {
                return it.getString(displayNameIndex)
            }
        }
    }

    return null
}

private class UriPrintDocumentAdapter(
    private val context: Context,
    private val uri: Uri,
    private val title: String
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(title.ensurePdfFileName())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build(),
            true
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Could not open PDF")
        }.onSuccess {
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }.onFailure {
            callback.onWriteFailed(it.message)
        }
    }
}

private fun String.ensurePdfFileName(): String {
    return if (endsWith(".pdf", ignoreCase = true)) {
        this
    } else {
        "$this.pdf"
    }
}
