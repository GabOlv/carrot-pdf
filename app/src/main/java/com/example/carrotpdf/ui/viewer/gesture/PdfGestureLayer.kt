package com.example.carrotpdf.ui.viewer.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
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
        awaitFirstDown(requireUnconsumed = true)

        var isTransforming = false
        var isPanning = false
        var hasMeaningfulZoom = false

        do {
            val event = awaitPointerEvent()
            val activeChanges = event.changes.filter { change ->
                change.pressed && !change.isConsumed
            }
            val pressedPointers = activeChanges.size

            if (pressedPointers > 1) {
                val zoomChange = activeChanges.calculateUnconsumedZoom()

                if (!isTransforming) {
                    isTransforming = true
                    viewerState.beginTransientTransform()
                }

                if (abs(zoomChange - 1f) > MIN_ZOOM_DELTA) {
                    hasMeaningfulZoom = true
                }

                viewerState.updateTransientTransform(
                    zoomChange = zoomChange,
                    pan = activeChanges.calculateUnconsumedPan(),
                    centroid = activeChanges.calculateCurrentCentroid()
                )

                activeChanges.forEach { pointerChange ->
                    pointerChange.consume()
                }
            } else if (pressedPointers == 1 && viewerState.canPanContent()) {
                val pan = activeChanges.calculateUnconsumedPan()

                if (pan.getDistance() > MIN_PAN_DELTA) {
                    if (!isPanning) {
                        isPanning = true
                        viewerState.beginPan()
                    }

                    viewerState.updatePan(delta = pan)
                    activeChanges.forEach { pointerChange ->
                        pointerChange.consume()
                    }
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

private fun List<PointerInputChange>.calculateCurrentCentroid(): Offset {
    if (isEmpty()) {
        return Offset.Zero
    }

    val sum = fold(Offset.Zero) { acc, change ->
        acc + change.position
    }

    return sum / size.toFloat()
}

private fun List<PointerInputChange>.calculatePreviousCentroid(): Offset {
    if (isEmpty()) {
        return Offset.Zero
    }

    val sum = fold(Offset.Zero) { acc, change ->
        acc + change.previousPosition
    }

    return sum / size.toFloat()
}

private fun List<PointerInputChange>.calculateUnconsumedPan(): Offset {
    return calculateCurrentCentroid() - calculatePreviousCentroid()
}

private fun List<PointerInputChange>.calculateUnconsumedZoom(): Float {
    if (size < 2) {
        return 1f
    }

    val currentCentroid = calculateCurrentCentroid()
    val previousCentroid = calculatePreviousCentroid()
    val currentDistance = averageDistanceTo(currentCentroid) { change -> change.position }
    val previousDistance = averageDistanceTo(previousCentroid) { change -> change.previousPosition }

    if (previousDistance <= 0f) {
        return 1f
    }

    return currentDistance / previousDistance
}

private inline fun List<PointerInputChange>.averageDistanceTo(
    centroid: Offset,
    position: (PointerInputChange) -> Offset
): Float {
    return fold(0f) { acc, change ->
        acc + (position(change) - centroid).getDistance()
    } / size.toFloat()
}

private const val MIN_ZOOM_DELTA = 0.003f
private const val MIN_PAN_DELTA = 0.5f
