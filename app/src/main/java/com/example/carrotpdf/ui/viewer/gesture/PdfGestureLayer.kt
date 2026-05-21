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
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import kotlin.math.abs

@Composable
fun PdfGestureLayer(
    viewerState: PdfViewerState,
    onTransformEnded: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val currentOnTransformEnded by rememberUpdatedState(onTransformEnded)

    Box(
        modifier = modifier.pointerInput(viewerState.documentId) {
            detectViewerTransformGestures(
                viewerState = viewerState,
                onTransformEnded = currentOnTransformEnded
            )
        },
        content = content
    )
}

private suspend fun PointerInputScope.detectViewerTransformGestures(
    viewerState: PdfViewerState,
    onTransformEnded: () -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var isTransforming = false
        var isPanning = false
        var hasMeaningfulZoom = false

        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }

            if (pressedPointers > 1) {
                val zoomChange = event.calculateZoom()

                if (!isTransforming) {
                    isTransforming = true
                    viewerState.beginTransientTransform()
                }

                if (abs(zoomChange - 1f) > MIN_ZOOM_DELTA) {
                    hasMeaningfulZoom = true
                }

                viewerState.updateTransientTransform(
                    zoomChange = zoomChange,
                    pan = event.calculatePan(),
                    centroid = event.calculateCentroid()
                )

                event.changes.forEach { pointerChange ->
                    pointerChange.consume()
                }
            } else if (pressedPointers == 1 && viewerState.canPanContent()) {
                val pan = event.calculatePan()

                if (pan.getDistance() > MIN_PAN_DELTA) {
                    if (!isPanning) {
                        isPanning = true
                        viewerState.beginPan()
                    }

                    viewerState.updatePan(delta = pan)
                }
            }
        } while (event.changes.any { it.pressed })

        if (isTransforming) {
            if (hasMeaningfulZoom) {
                viewerState.endTransientTransform()
                onTransformEnded()
            } else {
                viewerState.cancelTransientTransform()
            }
        } else if (isPanning) {
            viewerState.endPan()
        }
    }
}

private const val MIN_ZOOM_DELTA = 0.003f
private const val MIN_PAN_DELTA = 0.5f
