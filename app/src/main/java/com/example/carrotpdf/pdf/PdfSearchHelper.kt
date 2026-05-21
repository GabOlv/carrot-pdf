package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class PdfSearchResult(
    val pageIndex: Int,
    val snippet: String,
    val bounds: List<PdfSearchBounds> = emptyList()
)

data class PdfSearchBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pageWidth: Float,
    val pageHeight: Float
)

fun searchPdfText(
    context: Context,
    uri: Uri,
    query: String
): List<PdfSearchResult> {
    val normalizedQuery = query.trim()

    if (normalizedQuery.isBlank()) {
        return emptyList()
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return searchPdfTextWithPdfBox(
            context = context,
            uri = uri,
            query = normalizedQuery
        )
    }

    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()

    return fileDescriptor.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            buildList {
                for (pageIndex in 0 until renderer.pageCount) {
                    addAll(searchPage(renderer, pageIndex, normalizedQuery))
                }
            }
        }
    }
}

private fun searchPdfTextWithPdfBox(
    context: Context,
    uri: Uri,
    query: String
): List<PdfSearchResult> {
    PDFBoxResourceLoader.init(context.applicationContext)

    val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    return inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            val stripper = PDFTextStripper()

            buildList {
                for (pageIndex in 0 until document.numberOfPages) {
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1

                    val pageText = runCatching {
                        stripper.getText(document)
                    }.getOrDefault("")

                    addAll(
                        pageText.findMatches(
                            pageIndex = pageIndex,
                            query = query
                        )
                    )
                }
            }
        }
    }
}

private fun searchPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    query: String
): List<PdfSearchResult> {
    var page: PdfRenderer.Page? = null

    return try {
        page = renderer.openPage(pageIndex)
        val text = page.getTextContents()
            .joinToString(separator = "\n") { content -> content.text }
        val textMatchIndex = text.indexOf(query, ignoreCase = true)
        val matches = page.searchText(query)

        matches.map { match ->
            PdfSearchResult(
                pageIndex = pageIndex,
                snippet = if (textMatchIndex >= 0) {
                    text.snippetAround(textMatchIndex, query.length)
                } else {
                    "Match on page ${pageIndex + 1}"
                },
                bounds = match.bounds.toSearchBounds(
                    pageWidth = page.width.toFloat(),
                    pageHeight = page.height.toFloat()
                )
            )
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        page?.close()
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

private fun List<RectF>.toSearchBounds(
    pageWidth: Float,
    pageHeight: Float
): List<PdfSearchBounds> {
    return map { rect ->
        PdfSearchBounds(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            pageWidth = pageWidth,
            pageHeight = pageHeight
        )
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

private fun String.findMatches(
    pageIndex: Int,
    query: String
): List<PdfSearchResult> {
    val results = mutableListOf<PdfSearchResult>()
    var searchFrom = 0

    while (searchFrom < length) {
        val matchIndex = indexOf(query, startIndex = searchFrom, ignoreCase = true)

        if (matchIndex < 0) {
            break
        }

        results.add(
            PdfSearchResult(
                pageIndex = pageIndex,
                snippet = snippetAround(matchIndex, query.length)
            )
        )

        searchFrom = matchIndex + query.length.coerceAtLeast(1)
    }

    return results
}
