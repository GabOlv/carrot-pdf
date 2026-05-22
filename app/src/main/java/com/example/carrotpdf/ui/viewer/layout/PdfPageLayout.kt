package com.example.carrotpdf.ui.viewer.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PdfPageLayout(
    val viewportWidth: Dp,
    val pageWidth: Dp,
    val contentWidth: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val pageSpacing: Dp
)

@Composable
fun rememberPdfPageLayout(
    viewportWidth: Dp
): PdfPageLayout {
    return remember(viewportWidth) {
        val minimumHorizontalPadding = 12.dp
        val verticalPadding = 14.dp
        val pageSpacing = 14.dp
        val availableWidth = (viewportWidth - (minimumHorizontalPadding * 2))
            .coerceAtLeast(1.dp)
        val pageWidth = if (viewportWidth >= TABLET_WIDTH_THRESHOLD) {
            availableWidth.coerceAtMost(TABLET_READABLE_PAGE_WIDTH)
        } else {
            availableWidth
        }
        val horizontalPadding = ((viewportWidth - pageWidth) / 2)
            .coerceAtLeast(minimumHorizontalPadding)

        PdfPageLayout(
            viewportWidth = viewportWidth,
            pageWidth = pageWidth,
            contentWidth = viewportWidth,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            pageSpacing = pageSpacing
        )
    }
}

private val TABLET_WIDTH_THRESHOLD = 600.dp
private val TABLET_READABLE_PAGE_WIDTH = 600.dp
