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

internal class PdfTextIndexSession(
    private val context: Context,
    private val uri: Uri
) {
    private val mutex = Mutex()
    private var cachedPages: List<PdfTextIndexedPage>? = null

    suspend fun warmUp() {
        pages()
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
