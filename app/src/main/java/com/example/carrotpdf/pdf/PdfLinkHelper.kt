package com.example.carrotpdf.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

data class PdfLinkRegion(
    val pageIndex: Int,
    val bounds: List<PdfLinkBounds>,
    val target: PdfLinkTarget
)

data class PdfLinkBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val pageWidth: Float,
    val pageHeight: Float
)

sealed class PdfLinkTarget {
    data class ExternalUri(val uri: String) : PdfLinkTarget()
}

class PdfLinkSession(
    private val context: Context,
    private val uri: Uri
) {
    private val mutex = Mutex()
    private var cachedLinks: List<PdfLinkRegion>? = null

    suspend fun links(): List<PdfLinkRegion> {
        cachedLinks?.let { links ->
            return links
        }

        return mutex.withLock {
            cachedLinks?.let { links ->
                return@withLock links
            }

            extractPdfLinks(
                context = context,
                uri = uri
            ).also { links ->
                cachedLinks = links
            }
        }
    }
}

fun extractPdfLinks(
    context: Context,
    uri: Uri
): List<PdfLinkRegion> {
    PDFBoxResourceLoader.init(context.applicationContext)

    val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

    return inputStream.use { stream ->
        PDDocument.load(stream).use { document ->
            buildList {
                for (pageIndex in 0 until document.numberOfPages) {
                    val page = document.getPage(pageIndex)

                    addAll(
                        page.externalUriLinkRegions(
                            pageIndex = pageIndex
                        )
                    )
                }
            }
        }
    }
}

private fun PDPage.externalUriLinkRegions(
    pageIndex: Int
): List<PdfLinkRegion> {
    if (rotation != 0) {
        return emptyList()
    }

    val pageBox = cropBox ?: mediaBox ?: return emptyList()

    if (pageBox.width <= 0f || pageBox.height <= 0f) {
        return emptyList()
    }

    return runCatching {
        annotations.mapNotNull { annotation ->
            val link = annotation as? PDAnnotationLink ?: return@mapNotNull null
            val action = link.action as? PDActionURI ?: return@mapNotNull null
            val safeUri = action.uri?.trim()?.takeIf(::isSafeExternalUri)
                ?: return@mapNotNull null
            val bounds = link.linkBounds(pageBox)

            if (bounds.isEmpty()) {
                null
            } else {
                PdfLinkRegion(
                    pageIndex = pageIndex,
                    bounds = bounds,
                    target = PdfLinkTarget.ExternalUri(safeUri)
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun PDAnnotationLink.linkBounds(
    pageBox: PDRectangle
): List<PdfLinkBounds> {
    val quadBounds = getQuadPoints()
        ?.takeIf { points -> points.size >= QUAD_POINT_SIZE && points.size % QUAD_POINT_SIZE == 0 }
        ?.toList()
        ?.chunked(QUAD_POINT_SIZE)
        ?.mapNotNull { points -> points.toLinkBounds(pageBox) }
        .orEmpty()

    if (quadBounds.isNotEmpty()) {
        return quadBounds
    }

    return listOfNotNull(rectangle?.toLinkBounds(pageBox))
}

private fun List<Float>.toLinkBounds(
    pageBox: PDRectangle
): PdfLinkBounds? {
    val xs = listOf(this[0], this[2], this[4], this[6])
    val ys = listOf(this[1], this[3], this[5], this[7])

    return rawPdfRectToLinkBounds(
        left = xs.minOrNull() ?: return null,
        bottom = ys.minOrNull() ?: return null,
        right = xs.maxOrNull() ?: return null,
        top = ys.maxOrNull() ?: return null,
        pageBox = pageBox
    )
}

private fun PDRectangle.toLinkBounds(
    pageBox: PDRectangle
): PdfLinkBounds? {
    return rawPdfRectToLinkBounds(
        left = lowerLeftX,
        bottom = lowerLeftY,
        right = upperRightX,
        top = upperRightY,
        pageBox = pageBox
    )
}

private fun rawPdfRectToLinkBounds(
    left: Float,
    bottom: Float,
    right: Float,
    top: Float,
    pageBox: PDRectangle
): PdfLinkBounds? {
    val pageWidth = pageBox.width
    val pageHeight = pageBox.height

    if (
        pageWidth <= 0f ||
        pageHeight <= 0f ||
        !left.isFinite() ||
        !bottom.isFinite() ||
        !right.isFinite() ||
        !top.isFinite() ||
        right <= left ||
        top <= bottom
    ) {
        return null
    }

    val normalizedLeft = left - pageBox.lowerLeftX
    val normalizedRight = right - pageBox.lowerLeftX
    val normalizedTop = pageHeight - (top - pageBox.lowerLeftY)
    val normalizedBottom = pageHeight - (bottom - pageBox.lowerLeftY)

    val coercedLeft = normalizedLeft.coerceIn(0f, pageWidth)
    val coercedTop = normalizedTop.coerceIn(0f, pageHeight)
    val coercedRight = normalizedRight.coerceIn(0f, pageWidth)
    val coercedBottom = normalizedBottom.coerceIn(0f, pageHeight)

    if (
        coercedRight <= coercedLeft ||
        coercedBottom <= coercedTop ||
        coercedRight - coercedLeft < MIN_LINK_SIZE_PT ||
        coercedBottom - coercedTop < MIN_LINK_SIZE_PT ||
        coercedRight - coercedLeft > pageWidth * MAX_LINK_WIDTH_RATIO ||
        coercedBottom - coercedTop > pageHeight * MAX_LINK_HEIGHT_RATIO
    ) {
        return null
    }

    return PdfLinkBounds(
        left = coercedLeft,
        top = coercedTop,
        right = coercedRight,
        bottom = coercedBottom,
        pageWidth = pageWidth,
        pageHeight = pageHeight
    )
}

private fun isSafeExternalUri(uri: String): Boolean {
    val scheme = uri.substringBefore(":", missingDelimiterValue = "")
        .lowercase(Locale.US)

    return scheme in SAFE_EXTERNAL_URI_SCHEMES
}

private const val QUAD_POINT_SIZE = 8
private const val MIN_LINK_SIZE_PT = 1f
private const val MAX_LINK_WIDTH_RATIO = 1.05f
private const val MAX_LINK_HEIGHT_RATIO = 0.5f

private val SAFE_EXTERNAL_URI_SCHEMES = setOf(
    "http",
    "https",
    "mailto",
    "tel"
)
