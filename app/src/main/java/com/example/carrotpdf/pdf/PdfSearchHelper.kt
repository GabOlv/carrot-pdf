package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

class PdfSearchSession(
    private val context: Context,
    private val uri: Uri
) {
    private val pdfBoxIndexMutex = Mutex()
    private var pdfBoxPageTexts: List<String>? = null

    suspend fun warmUp() {
        if (!usesPlatformSearch()) {
            ensurePdfBoxPageTexts()
        }
    }

    suspend fun search(query: String): List<PdfSearchResult> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        if (usesPlatformSearch()) {
            return searchPdfTextWithPlatform(
                context = context,
                uri = uri,
                query = normalizedQuery
            )
        }

        return ensurePdfBoxPageTexts().flatMapIndexed { pageIndex, pageText ->
            pageText.findMatches(
                pageIndex = pageIndex,
                query = normalizedQuery
            )
        }
    }

    private suspend fun ensurePdfBoxPageTexts(): List<String> {
        pdfBoxPageTexts?.let { cachedTexts ->
            return cachedTexts
        }

        return pdfBoxIndexMutex.withLock {
            pdfBoxPageTexts?.let { cachedTexts ->
                return@withLock cachedTexts
            }

            extractPdfBoxPageTexts(
                context = context,
                uri = uri
            ).also { extractedTexts ->
                pdfBoxPageTexts = extractedTexts
            }
        }
    }
}

fun searchPdfText(
    context: Context,
    uri: Uri,
    query: String
): List<PdfSearchResult> {
    val normalizedQuery = query.trim()

    if (normalizedQuery.isBlank()) {
        return emptyList()
    }

    if (!usesPlatformSearch()) {
        return searchPdfTextWithPdfBox(
            context = context,
            uri = uri,
            query = normalizedQuery
        )
    }

    return searchPdfTextWithPlatform(
        context = context,
        uri = uri,
        query = normalizedQuery
    )
}

private fun usesPlatformSearch(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
}

private fun searchPdfTextWithPlatform(
    context: Context,
    uri: Uri,
    query: String
): List<PdfSearchResult> {
    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()

    return fileDescriptor.use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            buildList {
                for (pageIndex in 0 until renderer.pageCount) {
                    addAll(searchPage(renderer, pageIndex, query))
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
    return extractPdfBoxPageTexts(
        context = context,
        uri = uri
    ).flatMapIndexed { pageIndex, pageText ->
        pageText.findMatches(
            pageIndex = pageIndex,
            query = query
        )
    }
}

private fun extractPdfBoxPageTexts(
    context: Context,
    uri: Uri
): List<String> {
    PDFBoxResourceLoader.init(context.applicationContext)

    val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    return inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            runCatching {
                PageTextCollector().extract(document)
            }.getOrElse {
                extractPdfBoxPageTextsOnePageAtATime(document)
            }
        }
    }
}

private fun extractPdfBoxPageTextsOnePageAtATime(
    document: PDDocument
): List<String> {
    val stripper = PDFTextStripper()

    return buildList {
        for (pageIndex in 0 until document.numberOfPages) {
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1

            add(
                runCatching {
                    stripper.getText(document)
                }.getOrDefault("")
            )
        }
    }
}

private class PageTextCollector : PDFTextStripper() {
    private val pageTexts = mutableListOf<String>()
    private var currentPageText = StringBuilder()

    fun extract(document: PDDocument): List<String> {
        sortByPosition = true
        startPage = 1
        endPage = document.numberOfPages
        getText(document)
        return pageTexts.toList()
    }

    override fun startPage(page: PDPage?) {
        currentPageText = StringBuilder()
        super.startPage(page)
    }

    override fun endPage(page: PDPage?) {
        pageTexts.add(currentPageText.toString())
        super.endPage(page)
    }

    override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?
    ) {
        if (!text.isNullOrEmpty()) {
            currentPageText.append(text)
        }

        super.writeString(text, textPositions)
    }

    override fun writeLineSeparator() {
        currentPageText.append('\n')
        super.writeLineSeparator()
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
