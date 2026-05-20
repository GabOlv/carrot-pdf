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
        val horizontalPadding = 12.dp
        val verticalPadding = 14.dp
        val pageSpacing = 14.dp
        val pageWidth = viewportWidth - (horizontalPadding * 2)

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
