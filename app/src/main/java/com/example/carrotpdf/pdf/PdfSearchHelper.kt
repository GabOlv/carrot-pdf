package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import kotlin.math.abs
import kotlin.math.max

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
    private val textIndexSession = PdfTextIndexSession(
        context = context,
        uri = uri
    )

    suspend fun warmUp() {
        if (!usesPlatformSearch()) {
            textIndexSession.warmUp()
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

        return textIndexSession.pages().flatMap { indexedPage ->
            indexedPage.findMatches(
                query = normalizedQuery
            )
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
    return extractPdfTextIndex(
        context = context,
        uri = uri
    ).flatMap { indexedPage ->
        indexedPage.findMatches(
            query = query
        )
    }
}

private fun PdfTextIndexedPage.findMatches(
    query: String
): List<PdfSearchResult> {
    val results = mutableListOf<PdfSearchResult>()
    var searchFrom = 0

    while (searchFrom < text.length) {
        val matchIndex = text.indexOf(query, startIndex = searchFrom, ignoreCase = true)

        if (matchIndex < 0) {
            break
        }

        results.add(
            PdfSearchResult(
                pageIndex = pageIndex,
                snippet = text.snippetAround(matchIndex, query.length),
                bounds = boundsForMatch(
                    matchStart = matchIndex,
                    matchEnd = matchIndex + query.length
                )
            )
        )

        searchFrom = matchIndex + query.length.coerceAtLeast(1)
    }

    return results
}

private fun PdfTextIndexedPage.boundsForMatch(
    matchStart: Int,
    matchEnd: Int
): List<PdfSearchBounds> {
    if (pageWidth <= 0f || pageHeight <= 0f || glyphs.isEmpty()) {
        return emptyList()
    }

    val matchedGlyphs = glyphs
        .filter { glyph -> glyph.sourceIndex in matchStart until matchEnd }
        .filter { glyph -> glyph.hasReliableBounds(pageWidth, pageHeight) }

    if (matchedGlyphs.isEmpty()) {
        return emptyList()
    }

    return matchedGlyphs
        .sortedWith(compareBy<PdfTextGlyph> { it.top }.thenBy { it.left })
        .groupIntoVisualLines()
        .mapNotNull { lineGlyphs ->
            lineGlyphs.toSearchBounds(
                pageWidth = pageWidth,
                pageHeight = pageHeight
            )
    }
}

private fun List<PdfTextGlyph>.groupIntoVisualLines(): List<List<PdfTextGlyph>> {
    return fold(mutableListOf<MutableList<PdfTextGlyph>>()) { lines, glyph ->
        val line = lines.lastOrNull()
        val lineCenter = line?.map { it.centerY }?.average()?.toFloat()
        val tolerance = max(2f, glyph.height * 0.65f)

        if (line != null && lineCenter != null && abs(glyph.centerY - lineCenter) <= tolerance) {
            line.add(glyph)
        } else {
            lines.add(mutableListOf(glyph))
        }

        lines
    }
}

private fun List<PdfTextGlyph>.toSearchBounds(
    pageWidth: Float,
    pageHeight: Float
): PdfSearchBounds? {
    if (isEmpty()) {
        return null
    }

    val rawLeft = minOf { it.left }
    val rawTop = minOf { it.top }
    val rawRight = maxOf { it.right }
    val rawBottom = maxOf { it.bottom }
    val rawHeight = rawBottom - rawTop
    val verticalShift = rawHeight * PDFBOX_HIGHLIGHT_UPSHIFT_RATIO
    val verticalInset = rawHeight * PDFBOX_HIGHLIGHT_VERTICAL_INSET_RATIO

    val left = rawLeft.coerceIn(0f, pageWidth)
    val top = (rawTop - verticalShift + verticalInset).coerceIn(0f, pageHeight)
    val right = rawRight.coerceIn(0f, pageWidth)
    val bottom = (rawBottom - verticalShift - verticalInset).coerceIn(0f, pageHeight)

    if (
        right <= left ||
        bottom <= top ||
        right - left > pageWidth * MAX_MATCH_RECT_WIDTH_RATIO ||
        bottom - top > pageHeight * MAX_MATCH_RECT_HEIGHT_RATIO
    ) {
        return null
    }

    return PdfSearchBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        pageWidth = pageWidth,
        pageHeight = pageHeight
    )
}

private fun PdfTextGlyph.hasReliableBounds(
    pageWidth: Float,
    pageHeight: Float
): Boolean {
    return left.isFinite() &&
        top.isFinite() &&
        right.isFinite() &&
        bottom.isFinite() &&
        right > left &&
        bottom > top &&
        left >= -pageWidth * 0.05f &&
        right <= pageWidth * 1.05f &&
        top >= -pageHeight * 0.05f &&
        bottom <= pageHeight * 1.05f
}

private val PdfTextGlyph.centerY: Float
    get() = (top + bottom) / 2f

private val PdfTextGlyph.height: Float
    get() = bottom - top

private const val MAX_MATCH_RECT_WIDTH_RATIO = 0.95f
private const val MAX_MATCH_RECT_HEIGHT_RATIO = 0.12f
private const val PDFBOX_HIGHLIGHT_UPSHIFT_RATIO = 0.92f
private const val PDFBOX_HIGHLIGHT_VERTICAL_INSET_RATIO = 0.06f

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
