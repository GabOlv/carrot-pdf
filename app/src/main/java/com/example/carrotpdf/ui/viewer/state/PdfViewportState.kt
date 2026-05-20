package com.example.carrotpdf.ui.viewer.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

@Stable
class PdfViewportState(
    initialZoom: Float = DEFAULT_ZOOM
) {
    var committedZoom by mutableFloatStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    var transientScale by mutableFloatStateOf(1f)
        private set

    var panOffset by mutableStateOf(Offset.Zero)
        private set

    var viewportSize by mutableStateOf(IntSize.Zero)
        private set

    var contentSize by mutableStateOf(IntSize.Zero)
        private set

    val displayZoom: Float
        get() = (committedZoom * transientScale).coerceIn(MIN_ZOOM, MAX_ZOOM)

    val isTransformActive: Boolean
        get() = transientScale != 1f || panOffset != Offset.Zero

    fun updateViewportSize(size: IntSize) {
        viewportSize = size
        panOffset = coercePanOffset(panOffset, transientScale)
    }

    fun updateContentSize(size: IntSize) {
        contentSize = size
        panOffset = coercePanOffset(panOffset, transientScale)
    }

    fun setCommittedZoom(zoom: Float): Float {
        committedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        transientScale = 1f
        panOffset = Offset.Zero
        return committedZoom
    }

    fun advanceZoomPreset(): Float {
        return setCommittedZoom(nextZoomPreset(committedZoom))
    }

    fun beginTransientZoom() {
        transientScale = 1f
        panOffset = coercePanOffset(panOffset, transientScale)
    }

    fun updateTransientTransform(
        zoomChange: Float,
        pan: Offset,
        centroid: Offset
    ) {
        val oldScale = transientScale
        val targetDisplayZoom = (committedZoom * oldScale * zoomChange)
            .coerceIn(MIN_ZOOM, MAX_ZOOM)
        val newScale = targetDisplayZoom / committedZoom
        val appliedZoomChange = newScale / oldScale

        transientScale = newScale
        panOffset = coercePanOffset(
            offset = (panOffset * appliedZoomChange) +
                (centroid * (1f - appliedZoomChange)) +
                pan,
            scale = newScale
        )
    }

    fun commitTransientZoom(): Float {
        committedZoom = displayZoom
        transientScale = 1f
        panOffset = coercePanOffset(panOffset, transientScale)
        return committedZoom
    }

    fun resetPan() {
        panOffset = Offset.Zero
    }

    private fun nextZoomPreset(currentZoom: Float): Float {
        return when {
            currentZoom < 1.0f -> 1.0f
            currentZoom < 1.25f -> 1.25f
            currentZoom < 1.5f -> 1.5f
            currentZoom < 2.0f -> 2.0f
            currentZoom < 3.0f -> 3.0f
            currentZoom < MAX_ZOOM -> MAX_ZOOM
            else -> MIN_ZOOM
        }
    }

    private fun coercePanOffset(
        offset: Offset,
        scale: Float
    ): Offset {
        val viewportWidth = viewportSize.width.toFloat()
        val viewportHeight = viewportSize.height.toFloat()
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale

        val minX = if (scaledContentWidth > viewportWidth) {
            viewportWidth - scaledContentWidth
        } else {
            0f
        }
        val minY = if (scaledContentHeight > viewportHeight) {
            viewportHeight - scaledContentHeight
        } else {
            0f
        }

        return Offset(
            x = offset.x.coerceIn(minX, 0f),
            y = offset.y.coerceIn(minY, 0f)
        )
    }

    companion object {
        const val MIN_ZOOM = 0.85f
        const val DEFAULT_ZOOM = 1f
        const val MAX_ZOOM = 4f
    }
}
