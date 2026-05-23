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
    private val pdfBoxIndexMutex = Mutex()
    private var pdfBoxIndexedPages: List<PdfBoxIndexedPage>? = null

    suspend fun warmUp() {
        if (!usesPlatformSearch()) {
            ensurePdfBoxIndexedPages()
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

        return ensurePdfBoxIndexedPages().flatMap { indexedPage ->
            indexedPage.findMatches(
                query = normalizedQuery
            )
        }
    }

    private suspend fun ensurePdfBoxIndexedPages(): List<PdfBoxIndexedPage> {
        pdfBoxIndexedPages?.let { cachedPages ->
            return cachedPages
        }

        return pdfBoxIndexMutex.withLock {
            pdfBoxIndexedPages?.let { cachedPages ->
                return@withLock cachedPages
            }

            extractPdfBoxIndexedPages(
                context = context,
                uri = uri
            ).also { indexedPages ->
                pdfBoxIndexedPages = indexedPages
            }
        }
    }
}

private data class PdfBoxIndexedPage(
    val pageIndex: Int,
    val text: String,
    val glyphs: List<PdfBoxGlyph>,
    val pageWidth: Float,
    val pageHeight: Float
)

private data class PdfBoxGlyph(
    val sourceIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
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
    return extractPdfBoxIndexedPages(
        context = context,
        uri = uri
    ).flatMap { indexedPage ->
        indexedPage.findMatches(
            query = query
        )
    }
}

private fun extractPdfBoxIndexedPages(
    context: Context,
    uri: Uri
): List<PdfBoxIndexedPage> {
    PDFBoxResourceLoader.init(context.applicationContext)

    val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    return inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            runCatching {
                PageTextCollector().extract(document)
            }.getOrElse {
                extractPdfBoxIndexedPagesOnePageAtATime(document)
            }
        }
    }
}

private fun extractPdfBoxIndexedPagesOnePageAtATime(
    document: PDDocument
): List<PdfBoxIndexedPage> {
    val stripper = PDFTextStripper()

    return buildList {
        for (pageIndex in 0 until document.numberOfPages) {
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            val page = document.getPage(pageIndex)
            val mediaBox = page.mediaBox

            add(
                PdfBoxIndexedPage(
                    pageIndex = pageIndex,
                    text = runCatching {
                        stripper.getText(document)
                    }.getOrDefault(""),
                    glyphs = emptyList(),
                    pageWidth = mediaBox.width,
                    pageHeight = mediaBox.height
                )
            )
        }
    }
}

private class PageTextCollector : PDFTextStripper() {
    private val pages = mutableListOf<PdfBoxIndexedPage>()
    private var currentPageIndex = -1
    private var currentPageWidth = 0f
    private var currentPageHeight = 0f
    private var currentPageText = StringBuilder()
    private var currentPageGlyphs = mutableListOf<PdfBoxGlyph>()

    fun extract(document: PDDocument): List<PdfBoxIndexedPage> {
        sortByPosition = true
        startPage = 1
        endPage = document.numberOfPages
        getText(document)
        return pages.toList()
    }

    override fun startPage(page: PDPage?) {
        currentPageIndex += 1
        currentPageText = StringBuilder()
        currentPageGlyphs = mutableListOf()
        currentPageWidth = page?.mediaBox?.width ?: 0f
        currentPageHeight = page?.mediaBox?.height ?: 0f
        super.startPage(page)
    }

    override fun endPage(page: PDPage?) {
        pages.add(
            PdfBoxIndexedPage(
                pageIndex = currentPageIndex,
                text = currentPageText.toString(),
                glyphs = currentPageGlyphs.toList(),
                pageWidth = currentPageWidth,
                pageHeight = currentPageHeight
            )
        )
        super.endPage(page)
    }

    override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?
    ) {
        if (!text.isNullOrEmpty()) {
            val textStartIndex = currentPageText.length
            currentPageText.append(text)
            appendGlyphs(
                text = text,
                textStartIndex = textStartIndex,
                textPositions = textPositions.orEmpty()
            )
        }

        super.writeString(text, textPositions)
    }

    override fun writeWordSeparator() {
        currentPageText.append(' ')
        super.writeWordSeparator()
    }

    override fun writeLineSeparator() {
        currentPageText.append('\n')
        super.writeLineSeparator()
    }

    private fun appendGlyphs(
        text: String,
        textStartIndex: Int,
        textPositions: List<TextPosition>
    ) {
        if (textPositions.isEmpty()) {
            return
        }

        if (textPositions.size == text.length) {
            textPositions.forEachIndexed { index, position ->
                position.toGlyph(sourceIndex = textStartIndex + index)?.let(currentPageGlyphs::add)
            }
            return
        }

        var sourceIndex = textStartIndex
        textPositions.forEach { position ->
            val unicode = position.unicode.orEmpty()
            val glyphLength = unicode.length.coerceAtLeast(1)
            val right = sourceIndex + glyphLength

            if (right <= textStartIndex + text.length) {
                position.toGlyph(sourceIndex = sourceIndex)?.let(currentPageGlyphs::add)
            }

            sourceIndex = right
        }
    }
}

private fun TextPosition.toGlyph(
    sourceIndex: Int
): PdfBoxGlyph? {
    if (dir != 0f || pageWidth <= 0f || pageHeight <= 0f) {
        return null
    }

    val left = xDirAdj
    val top = yDirAdj
    val right = left + widthDirAdj
    val bottom = top + heightDir

    if (
        !left.isFinite() ||
        !top.isFinite() ||
        !right.isFinite() ||
        !bottom.isFinite() ||
        right <= left ||
        bottom <= top
    ) {
        return null
    }

    return PdfBoxGlyph(
        sourceIndex = sourceIndex,
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

private fun PdfBoxIndexedPage.findMatches(
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

private fun PdfBoxIndexedPage.boundsForMatch(
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
        .sortedWith(compareBy<PdfBoxGlyph> { it.top }.thenBy { it.left })
        .groupIntoVisualLines()
        .mapNotNull { lineGlyphs ->
            lineGlyphs.toSearchBounds(
                pageWidth = pageWidth,
                pageHeight = pageHeight
            )
        }
}

private fun List<PdfBoxGlyph>.groupIntoVisualLines(): List<List<PdfBoxGlyph>> {
    return fold(mutableListOf<MutableList<PdfBoxGlyph>>()) { lines, glyph ->
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

private fun List<PdfBoxGlyph>.toSearchBounds(
    pageWidth: Float,
    pageHeight: Float
): PdfSearchBounds? {
    if (isEmpty()) {
        return null
    }

    val left = minOf { it.left }.coerceIn(0f, pageWidth)
    val top = minOf { it.top }.coerceIn(0f, pageHeight)
    val right = maxOf { it.right }.coerceIn(0f, pageWidth)
    val bottom = maxOf { it.bottom }.coerceIn(0f, pageHeight)

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

private fun PdfBoxGlyph.hasReliableBounds(
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

private val PdfBoxGlyph.centerY: Float
    get() = (top + bottom) / 2f

private val PdfBoxGlyph.height: Float
    get() = bottom - top

private const val MAX_MATCH_RECT_WIDTH_RATIO = 0.95f
private const val MAX_MATCH_RECT_HEIGHT_RATIO = 0.12f

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
