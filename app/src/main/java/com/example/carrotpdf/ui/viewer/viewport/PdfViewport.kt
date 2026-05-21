package com.example.carrotpdf.ui.viewer.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.viewer.gesture.PdfGestureLayer
import com.example.carrotpdf.ui.viewer.layout.PdfPageLayout
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.PdfViewportState

@Composable
fun PdfViewport(
    viewerState: PdfViewerState,
    viewportState: PdfViewportState,
    pageLayout: PdfPageLayout,
    onTransformEnded: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(CarrotColors.PdfCanvas)
            .onSizeChanged(viewportState::updateViewportSize)
    ) {
        PdfGestureLayer(
            viewerState = viewerState,
            onTransformEnded = onTransformEnded,
            modifier = Modifier.matchParentSize(),
            content = content
        )
    }
}
