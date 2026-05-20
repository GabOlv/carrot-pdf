package com.example.carrotpdf.ui.viewer.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.example.carrotpdf.ui.viewer.state.PdfInteractionMode
import com.example.carrotpdf.ui.viewer.state.PdfViewerState

@Composable
fun PdfGestureLayer(
    viewerState: PdfViewerState,
    onZoomCommitted: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val currentOnZoomCommitted by rememberUpdatedState(onZoomCommitted)

    Box(
        modifier = modifier.pointerInput(viewerState.documentId) {
            detectViewerTransformGestures(
                viewerState = viewerState,
                onZoomCommitted = currentOnZoomCommitted
            )
        },
        content = content
    )
}

private suspend fun PointerInputScope.detectViewerTransformGestures(
    viewerState: PdfViewerState,
    onZoomCommitted: (Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var isTransforming = false

        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }

            if (pressedPointers > 1) {
                if (!isTransforming) {
                    isTransforming = true
                    viewerState.beginTransientTransform()
                }

                viewerState.updateTransientTransform(
                    zoomChange = event.calculateZoom(),
                    pan = event.calculatePan(),
                    centroid = event.calculateCentroid()
                )

                event.changes.forEach { pointerChange ->
                    pointerChange.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        if (isTransforming) {
            val committedZoom = viewerState.commitTransientTransform()
            viewerState.updateInteractionMode(PdfInteractionMode.Idle)
            onZoomCommitted(committedZoom)
        }
    }
}
