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

    val displayZoom: Float
        get() = (committedZoom * transientScale).coerceIn(MIN_ZOOM, MAX_ZOOM)

    val isTransformActive: Boolean
        get() = transientScale != 1f || panOffset != Offset.Zero

    fun updateViewportSize(size: IntSize) {
        viewportSize = size
    }

    fun setCommittedZoom(zoom: Float): Float {
        committedZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        transientScale = 1f
        return committedZoom
    }

    fun advanceZoomPreset(): Float {
        return setCommittedZoom(nextZoomPreset(committedZoom))
    }

    fun beginTransientZoom() {
        transientScale = 1f
    }

    fun updateTransientTransform(
        scale: Float,
        pan: Offset
    ) {
        transientScale = scale.coerceAtLeast(0.01f)
        panOffset += pan
    }

    fun commitTransientZoom(): Float {
        return setCommittedZoom(displayZoom)
    }

    fun resetPan() {
        panOffset = Offset.Zero
    }

    private fun nextZoomPreset(currentZoom: Float): Float {
        return when {
            currentZoom < 1.0f -> 1.0f
            currentZoom < 1.25f -> 1.25f
            currentZoom < 1.5f -> 1.5f
            currentZoom < MAX_ZOOM -> MAX_ZOOM
            else -> MIN_ZOOM
        }
    }

    companion object {
        const val MIN_ZOOM = 0.85f
        const val DEFAULT_ZOOM = 1f
        const val MAX_ZOOM = 2f
    }
}
