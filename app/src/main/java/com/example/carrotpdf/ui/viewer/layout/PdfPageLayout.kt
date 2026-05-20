package com.example.carrotpdf.ui.viewer.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.viewer.state.PdfViewportState

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
    viewportWidth: Dp,
    viewportState: PdfViewportState
): PdfPageLayout {
    val committedZoom = viewportState.committedZoom

    return remember(viewportWidth, committedZoom) {
        val horizontalPadding = 12.dp
        val verticalPadding = 14.dp
        val pageSpacing = 14.dp
        val basePageWidth = viewportWidth - (horizontalPadding * 2)
        val pageWidth = basePageWidth * committedZoom
        val contentWidth = if (pageWidth > viewportWidth) {
            pageWidth + (horizontalPadding * 2)
        } else {
            viewportWidth
        }

        PdfPageLayout(
            viewportWidth = viewportWidth,
            pageWidth = pageWidth,
            contentWidth = contentWidth,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            pageSpacing = pageSpacing
        )
    }
}
