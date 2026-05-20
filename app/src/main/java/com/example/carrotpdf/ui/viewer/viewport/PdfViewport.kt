package com.example.carrotpdf.ui.viewer.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
    onZoomCommitted: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val horizontalScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .background(CarrotColors.PdfCanvas)
            .onSizeChanged(viewportState::updateViewportSize)
            .horizontalScroll(horizontalScrollState)
    ) {
        PdfGestureLayer(
            viewerState = viewerState,
            onZoomCommitted = onZoomCommitted,
            modifier = Modifier
                .width(pageLayout.contentWidth)
                .graphicsLayer {
                    val transientScale = viewportState.transientScale
                    scaleX = transientScale
                    scaleY = transientScale
                    translationX = viewportState.panOffset.x
                    translationY = viewportState.panOffset.y
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
            content = content
        )
    }
}
