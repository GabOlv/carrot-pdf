package com.example.carrotpdf.ui.viewer.layout

import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.ui.viewer.state.PdfVisiblePages
import kotlin.math.max
import kotlin.math.min

data class PdfPageFrame(
    val pageIndex: Int,
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float
) {
    val rightPx: Float
        get() = leftPx + widthPx

    val bottomPx: Float
        get() = topPx + heightPx
}

data class PdfDocumentGeometry(
    val pageCount: Int,
    val contentWidthPx: Float,
    val pageWidthPx: Float,
    val horizontalPaddingPx: Float,
    val verticalPaddingPx: Float,
    val pageSpacingPx: Float,
    val totalHeightPx: Float,
    val pageFrames: List<PdfPageFrame>
)

fun PdfDocumentGeometry.lazyScrollTopFor(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Float {
    val firstFrame = pageFrames.getOrNull(firstVisibleItemIndex)
        ?: return 0f

    return (firstFrame.topPx - verticalPaddingPx + firstVisibleItemScrollOffset)
        .coerceIn(0f, totalHeightPx)
}

fun PdfDocumentGeometry.visiblePagesForViewport(
    viewportTopPx: Float,
    viewportBottomPx: Float,
    overscanPages: Int = 1
): PdfVisiblePages {
    if (pageFrames.isEmpty() || viewportBottomPx <= viewportTopPx) {
        return PdfVisiblePages.Empty
    }

    val visibleTopPx = viewportTopPx.coerceAtLeast(0f)
    val visibleBottomPx = viewportBottomPx.coerceAtMost(totalHeightPx)

    val visibleFrames = pageFrames.filter { frame ->
        frame.bottomPx > visibleTopPx && frame.topPx < visibleBottomPx
    }

    if (visibleFrames.isEmpty()) {
        return PdfVisiblePages.Empty
    }

    val primaryFrame = visibleFrames.maxByOrNull { frame ->
        val visibleTop = max(frame.topPx, visibleTopPx)
        val visibleBottom = min(frame.bottomPx, visibleBottomPx)

        (visibleBottom - visibleTop).coerceAtLeast(0f)
    }

    val firstVisiblePage = visibleFrames.first().pageIndex
    val lastVisiblePage = visibleFrames.last().pageIndex
    val activeFirstPage = (firstVisiblePage - overscanPages).coerceAtLeast(0)
    val activeLastPage = (lastVisiblePage + overscanPages).coerceAtMost(pageCount - 1)

    return PdfVisiblePages(
        visibleRange = firstVisiblePage..lastVisiblePage,
        activeRange = activeFirstPage..activeLastPage,
        primaryVisiblePage = primaryFrame?.pageIndex
    )
}

fun buildPdfDocumentGeometry(
    pageCount: Int,
    pageSizes: List<PdfPageSize>,
    contentWidthPx: Float,
    pageWidthPx: Float,
    horizontalPaddingPx: Float,
    verticalPaddingPx: Float,
    pageSpacingPx: Float
): PdfDocumentGeometry {
    if (pageCount <= 0 || pageWidthPx <= 0f) {
        return PdfDocumentGeometry(
            pageCount = pageCount.coerceAtLeast(0),
            contentWidthPx = contentWidthPx.coerceAtLeast(0f),
            pageWidthPx = pageWidthPx.coerceAtLeast(0f),
            horizontalPaddingPx = horizontalPaddingPx.coerceAtLeast(0f),
            verticalPaddingPx = verticalPaddingPx.coerceAtLeast(0f),
            pageSpacingPx = pageSpacingPx.coerceAtLeast(0f),
            totalHeightPx = 0f,
            pageFrames = emptyList()
        )
    }

    val frames = ArrayList<PdfPageFrame>(pageCount)
    var cursorY = verticalPaddingPx

    repeat(pageCount) { pageIndex ->
        val aspectRatio = pageSizes.getOrNull(pageIndex)?.aspectRatio
            ?: DEFAULT_PAGE_ASPECT_RATIO
        val pageHeightPx = pageWidthPx * aspectRatio

        frames += PdfPageFrame(
            pageIndex = pageIndex,
            leftPx = horizontalPaddingPx,
            topPx = cursorY,
            widthPx = pageWidthPx,
            heightPx = pageHeightPx
        )

        cursorY += pageHeightPx

        if (pageIndex < pageCount - 1) {
            cursorY += pageSpacingPx
        }
    }

    cursorY += verticalPaddingPx

    return PdfDocumentGeometry(
        pageCount = pageCount,
        contentWidthPx = contentWidthPx,
        pageWidthPx = pageWidthPx,
        horizontalPaddingPx = horizontalPaddingPx,
        verticalPaddingPx = verticalPaddingPx,
        pageSpacingPx = pageSpacingPx,
        totalHeightPx = cursorY,
        pageFrames = frames
    )
}

private const val DEFAULT_PAGE_ASPECT_RATIO = 1.414f
