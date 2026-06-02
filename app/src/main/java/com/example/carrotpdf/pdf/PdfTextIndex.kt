package com.example.carrotpdf.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.floor

internal data class PdfTextIndexedPage(
    val pageIndex: Int,
    val text: String,
    val glyphs: List<PdfTextGlyph>,
    val pageWidth: Float,
    val pageHeight: Float
)

internal data class PdfTextGlyph(
    val sourceIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class PdfTextSelection(
    val pageIndex: Int,
    val text: String,
    val startIndex: Int,
    val endExclusive: Int,
    val pageRanges: List<PdfTextSelectionRange>,
    val bounds: List<PdfTextBounds>
)

data class PdfTextSelectionRange(
    val pageIndex: Int,
    val startIndex: Int,
    val endExclusive: Int
)

enum class PdfTextSelectionHandle {
    Start,
    End
}

data class PdfTextBounds(
    val pageIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pageWidth: Float,
    val pageHeight: Float
)

internal class PdfTextIndexSession(
    private val context: Context,
    private val uri: Uri
) {
    private val mutex = Mutex()
    private var cachedPages: List<PdfTextIndexedPage>? = null

    suspend fun warmUp() {
        pages()
    }

    suspend fun wordAt(
        pageIndex: Int,
        normalizedX: Float,
        normalizedY: Float
    ): PdfTextSelection? {
        return pages()
            .getOrNull(pageIndex)
            ?.wordAt(
                normalizedX = normalizedX,
                normalizedY = normalizedY
            )
    }

    suspend fun adjustSelection(
        selection: PdfTextSelection,
        handle: PdfTextSelectionHandle,
        pageIndex: Int,
        normalizedX: Float,
        normalizedY: Float
    ): PdfTextSelection? {
        return pages().adjustSelection(
            selection = selection,
            handle = handle,
            pageIndex = pageIndex,
            normalizedX = normalizedX,
            normalizedY = normalizedY
        )
    }

    suspend fun pages(): List<PdfTextIndexedPage> {
        cachedPages?.let { pages ->
            return pages
        }

        return mutex.withLock {
            cachedPages?.let { pages ->
                return@withLock pages
            }

            extractPdfTextIndex(
                context = context,
                uri = uri
            ).also { pages ->
                cachedPages = pages
            }
        }
    }
}

private fun PdfTextIndexedPage.wordAt(
    normalizedX: Float,
    normalizedY: Float
): PdfTextSelection? {
    if (
        text.isBlank() ||
        glyphs.isEmpty() ||
        pageWidth <= 0f ||
        pageHeight <= 0f
    ) {
        return null
    }

    val pageX = normalizedX.coerceIn(0f, 1f) * pageWidth
    val pageY = normalizedY.coerceIn(0f, 1f) * pageHeight
    val glyph = glyphs
        .filter { glyph -> glyph.hasReliableBounds(pageWidth, pageHeight) }
        .minByOrNull { glyph -> glyph.hitDistanceSquared(pageX, pageY) }
        ?.takeIf { glyph ->
            glyph.isNear(
                x = pageX,
                y = pageY
            )
        } ?: return null

    val wordRange = text.wordRangeAround(glyph.sourceIndex) ?: return null
    return selectionForRange(
        start = wordRange.first,
        endExclusive = wordRange.last + 1
    )
}

private fun List<PdfTextIndexedPage>.adjustSelection(
    selection: PdfTextSelection,
    handle: PdfTextSelectionHandle,
    pageIndex: Int,
    normalizedX: Float,
    normalizedY: Float
): PdfTextSelection? {
    val targetPoint = resolvePagePoint(
        pageIndex = pageIndex,
        normalizedX = normalizedX,
        normalizedY = normalizedY
    ) ?: return selection
    val targetPage = getOrNull(targetPoint.pageIndex) ?: return selection

    val sourceIndex = targetPage.sourceIndexClosestTo(
        normalizedX = targetPoint.normalizedX,
        normalizedY = targetPoint.normalizedY
    ) ?: return selection

    val currentStart = selection.startEndpoint() ?: return selection
    val currentEnd = selection.endEndpoint() ?: return selection
    val targetEndpoint = PdfTextEndpoint(
        pageIndex = targetPage.pageIndex,
        index = sourceIndex
    )

    val rawStart = when (handle) {
        PdfTextSelectionHandle.Start -> targetEndpoint
        PdfTextSelectionHandle.End -> currentStart
    }
    val rawEnd = when (handle) {
        PdfTextSelectionHandle.Start -> currentEnd
        PdfTextSelectionHandle.End -> targetEndpoint.copy(index = sourceIndex + 1)
    }
    val (start, endExclusive) = normalizeSelectionEndpoints(
        start = rawStart,
        endExclusive = rawEnd
    )

    return selectionForEndpoints(
        start = start,
        endExclusive = endExclusive
    ) ?: selection
}

private fun PdfTextIndexedPage.selectionForRange(
    start: Int,
    endExclusive: Int
): PdfTextSelection? {
    if (
        text.isBlank() ||
        glyphs.isEmpty() ||
        pageWidth <= 0f ||
        pageHeight <= 0f ||
        start !in 0..text.length ||
        endExclusive !in 0..text.length ||
        endExclusive <= start
    ) {
        return null
    }

    val selectedText = text.substring(start, endExclusive).trim()

    if (selectedText.isBlank()) {
        return null
    }

    val bounds = boundsForRange(
        start = start,
        endExclusive = endExclusive
    )

    if (bounds.isEmpty()) {
        return null
    }

    val range = PdfTextSelectionRange(
        pageIndex = pageIndex,
        startIndex = start,
        endExclusive = endExclusive
    )

    return PdfTextSelection(
        pageIndex = pageIndex,
        text = selectedText,
        startIndex = start,
        endExclusive = endExclusive,
        pageRanges = listOf(range),
        bounds = bounds
    )
}

private fun List<PdfTextIndexedPage>.selectionForEndpoints(
    start: PdfTextEndpoint,
    endExclusive: PdfTextEndpoint
): PdfTextSelection? {
    if (isEmpty()) {
        return null
    }

    val normalizedStart = start.coerceToPages(this)
    val normalizedEnd = endExclusive.coerceToPages(this)
    val ranges = buildSelectionRanges(
        pages = this,
        start = normalizedStart,
        endExclusive = normalizedEnd
    )

    if (ranges.isEmpty()) {
        return null
    }

    val textParts = ranges.mapNotNull { range ->
        val page = getOrNull(range.pageIndex) ?: return@mapNotNull null

        if (
            range.startIndex !in 0..page.text.length ||
            range.endExclusive !in 0..page.text.length ||
            range.endExclusive <= range.startIndex
        ) {
            null
        } else {
            page.text.substring(range.startIndex, range.endExclusive)
        }
    }
    val selectedText = textParts.joinToString(separator = "\n").trim()

    if (selectedText.isBlank()) {
        return null
    }

    val bounds = ranges.flatMap { range ->
        getOrNull(range.pageIndex)
            ?.boundsForRange(
                start = range.startIndex,
                endExclusive = range.endExclusive
            )
            .orEmpty()
    }

    if (bounds.isEmpty()) {
        return null
    }

    val firstRange = ranges.first()
    val lastRange = ranges.last()

    return PdfTextSelection(
        pageIndex = firstRange.pageIndex,
        text = selectedText,
        startIndex = firstRange.startIndex,
        endExclusive = lastRange.endExclusive,
        pageRanges = ranges,
        bounds = bounds
    )
}

private data class PdfTextEndpoint(
    val pageIndex: Int,
    val index: Int
)

private data class PdfTextPagePoint(
    val pageIndex: Int,
    val normalizedX: Float,
    val normalizedY: Float
)

private fun PdfTextSelection.startEndpoint(): PdfTextEndpoint? {
    val firstRange = pageRanges.firstOrNull() ?: return null

    return PdfTextEndpoint(
        pageIndex = firstRange.pageIndex,
        index = firstRange.startIndex
    )
}

private fun PdfTextSelection.endEndpoint(): PdfTextEndpoint? {
    val lastRange = pageRanges.lastOrNull() ?: return null

    return PdfTextEndpoint(
        pageIndex = lastRange.pageIndex,
        index = lastRange.endExclusive
    )
}

private fun normalizeSelectionEndpoints(
    start: PdfTextEndpoint,
    endExclusive: PdfTextEndpoint
): Pair<PdfTextEndpoint, PdfTextEndpoint> {
    return if (start <= endExclusive) {
        start to endExclusive
    } else {
        endExclusive to start
    }
}

private operator fun PdfTextEndpoint.compareTo(
    other: PdfTextEndpoint
): Int {
    return when {
        pageIndex != other.pageIndex -> pageIndex.compareTo(other.pageIndex)
        else -> index.compareTo(other.index)
    }
}

private fun PdfTextEndpoint.coerceToPages(
    pages: List<PdfTextIndexedPage>
): PdfTextEndpoint {
    val page = pages
        .getOrNull(pageIndex.coerceIn(0, pages.lastIndex))
        ?: return this

    return PdfTextEndpoint(
        pageIndex = page.pageIndex,
        index = index.coerceIn(0, page.text.length)
    )
}

private fun buildSelectionRanges(
    pages: List<PdfTextIndexedPage>,
    start: PdfTextEndpoint,
    endExclusive: PdfTextEndpoint
): List<PdfTextSelectionRange> {
    if (start.pageIndex == endExclusive.pageIndex) {
        val page = pages.getOrNull(start.pageIndex) ?: return emptyList()
        val startIndex = start.index.coerceIn(0, page.text.length)
        val endIndex = endExclusive.index.coerceIn(0, page.text.length)

        return if (endIndex > startIndex) {
            listOf(
                PdfTextSelectionRange(
                    pageIndex = page.pageIndex,
                    startIndex = startIndex,
                    endExclusive = endIndex
                )
            )
        } else {
            emptyList()
        }
    }

    return buildList {
        for (pageIndex in start.pageIndex..endExclusive.pageIndex) {
            val page = pages.getOrNull(pageIndex) ?: continue
            val rangeStart = if (pageIndex == start.pageIndex) {
                start.index.coerceIn(0, page.text.length)
            } else {
                0
            }
            val rangeEnd = if (pageIndex == endExclusive.pageIndex) {
                endExclusive.index.coerceIn(0, page.text.length)
            } else {
                page.text.length
            }

            if (rangeEnd > rangeStart) {
                add(
                    PdfTextSelectionRange(
                        pageIndex = page.pageIndex,
                        startIndex = rangeStart,
                        endExclusive = rangeEnd
                    )
                )
            }
        }
    }
}

private fun List<PdfTextIndexedPage>.resolvePagePoint(
    pageIndex: Int,
    normalizedX: Float,
    normalizedY: Float
): PdfTextPagePoint? {
    if (isEmpty() || pageIndex !in indices) {
        return null
    }

    val pageOffset = when {
        normalizedY < 0f -> floor(normalizedY).toInt()
        normalizedY > 1f -> floor(normalizedY).toInt()
        else -> 0
    }
    val rawPageIndex = pageIndex + pageOffset
    val targetPageIndex = rawPageIndex.coerceIn(0, lastIndex)
    val localY = when {
        rawPageIndex < 0 -> 0f
        rawPageIndex > lastIndex -> 1f
        pageOffset != 0 -> normalizedY - pageOffset
        else -> normalizedY
    }

    return PdfTextPagePoint(
        pageIndex = targetPageIndex,
        normalizedX = normalizedX.coerceIn(0f, 1f),
        normalizedY = localY.coerceIn(0f, 1f)
    )
}

private fun PdfTextIndexedPage.sourceIndexClosestTo(
    normalizedX: Float,
    normalizedY: Float
): Int? {
    if (
        text.isBlank() ||
        glyphs.isEmpty() ||
        pageWidth <= 0f ||
        pageHeight <= 0f
    ) {
        return null
    }

    val pageX = normalizedX.coerceIn(0f, 1f) * pageWidth
    val pageY = normalizedY.coerceIn(0f, 1f) * pageHeight

    return glyphs
        .filter { glyph -> glyph.hasReliableBounds(pageWidth, pageHeight) }
        .minByOrNull { glyph -> glyph.hitDistanceSquared(pageX, pageY) }
        ?.sourceIndex
}

private fun PdfTextIndexedPage.boundsForRange(
    start: Int,
    endExclusive: Int
): List<PdfTextBounds> {
    val rangeGlyphs = glyphs
        .filter { glyph -> glyph.sourceIndex in start until endExclusive }
        .filter { glyph -> glyph.hasReliableBounds(pageWidth, pageHeight) }

    if (rangeGlyphs.isEmpty()) {
        return emptyList()
    }

    return rangeGlyphs
        .sortedWith(compareBy<PdfTextGlyph> { it.top }.thenBy { it.left })
        .groupIntoVisualLines()
        .mapNotNull { lineGlyphs ->
            lineGlyphs.toTextBounds(
                pageIndex = pageIndex,
                pageWidth = pageWidth,
                pageHeight = pageHeight
            )
        }
}

private fun String.wordRangeAround(
    sourceIndex: Int
): IntRange? {
    if (sourceIndex !in indices) {
        return null
    }

    var start = sourceIndex
    var end = sourceIndex

    if (!this[sourceIndex].isPdfWordCharacter()) {
        return null
    }

    while (start > 0 && this[start - 1].isPdfWordCharacter()) {
        start -= 1
    }

    while (end < lastIndex && this[end + 1].isPdfWordCharacter()) {
        end += 1
    }

    return start..end
}

private fun Char.isPdfWordCharacter(): Boolean {
    return isLetterOrDigit() || this == '_' || this == '-'
}

private fun List<PdfTextGlyph>.groupIntoVisualLines(): List<List<PdfTextGlyph>> {
    return fold(mutableListOf<MutableList<PdfTextGlyph>>()) { lines, glyph ->
        val line = lines.lastOrNull()
        val lineCenter = line?.map { it.centerY }?.average()?.toFloat()
        val tolerance = maxOf(2f, glyph.height * 0.65f)

        if (line != null && lineCenter != null && kotlin.math.abs(glyph.centerY - lineCenter) <= tolerance) {
            line.add(glyph)
        } else {
            lines.add(mutableListOf(glyph))
        }

        lines
    }
}

private fun List<PdfTextGlyph>.toTextBounds(
    pageIndex: Int,
    pageWidth: Float,
    pageHeight: Float
): PdfTextBounds? {
    if (isEmpty()) {
        return null
    }

    val rawLeft = minOf { it.left }
    val rawTop = minOf { it.top }
    val rawRight = maxOf { it.right }
    val rawBottom = maxOf { it.bottom }
    val rawHeight = rawBottom - rawTop
    val verticalShift = rawHeight * TEXT_SELECTION_UPSHIFT_RATIO
    val verticalInset = rawHeight * TEXT_SELECTION_VERTICAL_INSET_RATIO

    val left = rawLeft.coerceIn(0f, pageWidth)
    val top = (rawTop - verticalShift + verticalInset).coerceIn(0f, pageHeight)
    val right = rawRight.coerceIn(0f, pageWidth)
    val bottom = (rawBottom - verticalShift - verticalInset).coerceIn(0f, pageHeight)

    if (right <= left || bottom <= top) {
        return null
    }

    return PdfTextBounds(
        pageIndex = pageIndex,
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        pageWidth = pageWidth,
        pageHeight = pageHeight
    )
}

private fun PdfTextGlyph.hitDistanceSquared(
    x: Float,
    y: Float
): Float {
    val nearestX = x.coerceIn(left, right)
    val nearestY = y.coerceIn(top, bottom)
    val dx = x - nearestX
    val dy = y - nearestY

    return (dx * dx) + (dy * dy)
}

private fun PdfTextGlyph.isNear(
    x: Float,
    y: Float
): Boolean {
    val horizontalTolerance = maxOf(TEXT_HIT_MIN_TOLERANCE_PT, width * TEXT_HIT_WIDTH_TOLERANCE_RATIO)
    val verticalTolerance = maxOf(TEXT_HIT_MIN_TOLERANCE_PT, height * TEXT_HIT_HEIGHT_TOLERANCE_RATIO)

    return x >= left - horizontalTolerance &&
        x <= right + horizontalTolerance &&
        y >= top - verticalTolerance &&
        y <= bottom + verticalTolerance
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

private val PdfTextGlyph.width: Float
    get() = right - left

private val PdfTextGlyph.height: Float
    get() = bottom - top

private val PdfTextGlyph.centerY: Float
    get() = (top + bottom) / 2f

internal fun extractPdfTextIndex(
    context: Context,
    uri: Uri
): List<PdfTextIndexedPage> {
    PDFBoxResourceLoader.init(context.applicationContext)

    val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    return inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            runCatching {
                PdfTextIndexCollector().extract(document)
            }.getOrElse {
                extractPdfTextIndexOnePageAtATime(document)
            }
        }
    }
}

private fun extractPdfTextIndexOnePageAtATime(
    document: PDDocument
): List<PdfTextIndexedPage> {
    val stripper = PDFTextStripper()

    return buildList {
        for (pageIndex in 0 until document.numberOfPages) {
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            val page = document.getPage(pageIndex)
            val mediaBox = page.mediaBox

            add(
                PdfTextIndexedPage(
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

private class PdfTextIndexCollector : PDFTextStripper() {
    private val pages = mutableListOf<PdfTextIndexedPage>()
    private var currentPageIndex = -1
    private var currentPageWidth = 0f
    private var currentPageHeight = 0f
    private var currentPageText = StringBuilder()
    private var currentPageGlyphs = mutableListOf<PdfTextGlyph>()

    fun extract(document: PDDocument): List<PdfTextIndexedPage> {
        sortByPosition = false
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
            PdfTextIndexedPage(
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

        textPositions
            .firstOrNull { position -> position.pageWidth > 0f && position.pageHeight > 0f }
            ?.let { position ->
                currentPageWidth = position.pageWidth
                currentPageHeight = position.pageHeight
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
): PdfTextGlyph? {
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

    return PdfTextGlyph(
        sourceIndex = sourceIndex,
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )
}

private const val TEXT_HIT_MIN_TOLERANCE_PT = 3f
private const val TEXT_HIT_WIDTH_TOLERANCE_RATIO = 0.65f
private const val TEXT_HIT_HEIGHT_TOLERANCE_RATIO = 0.85f
private const val TEXT_SELECTION_UPSHIFT_RATIO = 0.92f
private const val TEXT_SELECTION_VERTICAL_INSET_RATIO = 0.04f
