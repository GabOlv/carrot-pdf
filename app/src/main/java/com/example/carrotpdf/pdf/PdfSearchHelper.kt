package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor

data class PdfSearchResult(
    val pageIndex: Int,
    val snippet: String
)

fun searchPdfText(
    context: Context,
    uri: Uri,
    query: String
): List<PdfSearchResult> {
    val normalizedQuery = query.trim()

    if (normalizedQuery.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return emptyList()
    }

    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()

    return fileDescriptor.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            buildList {
                for (pageIndex in 0 until renderer.pageCount) {
                    val pageText = extractPageText(renderer, pageIndex)
                    val matchIndex = pageText.indexOf(normalizedQuery, ignoreCase = true)

                    if (matchIndex >= 0) {
                        add(
                            PdfSearchResult(
                                pageIndex = pageIndex,
                                snippet = pageText.snippetAround(matchIndex, normalizedQuery.length)
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun extractPageText(
    renderer: PdfRenderer,
    pageIndex: Int
): String {
    var page: PdfRenderer.Page? = null

    return try {
        page = renderer.openPage(pageIndex)
        page.getTextContents()
            .joinToString(separator = "\n") { content -> content.text }
    } catch (_: Exception) {
        ""
    } finally {
        page?.close()
    }
}

private fun String.snippetAround(
    matchIndex: Int,
    matchLength: Int
): String {
    val start = (matchIndex - 48).coerceAtLeast(0)
    val end = (matchIndex + matchLength + 72).coerceAtMost(length)
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < length) "..." else ""

    return prefix + substring(start, end).replace(Regex("\\s+"), " ") + suffix
}
