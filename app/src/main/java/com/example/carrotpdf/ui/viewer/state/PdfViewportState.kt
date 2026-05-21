package com.example.carrotpdf.ui.viewer.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

@Stable
class PdfViewportState(
    initialZoom: Float = DEFAULT_ZOOM
) {
    var visualScale by mutableFloatStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    var panOffset by mutableStateOf(Offset.Zero)
        private set

    var renderQualityScale by mutableFloatStateOf(renderQualityFor(visualScale))
        private set

    var lastRenderedQualityScale by mutableFloatStateOf(renderQualityScale)
        private set

    var viewportSize by mutableStateOf(IntSize.Zero)
        private set

    var contentSize by mutableStateOf(IntSize.Zero)
        private set

    val displayZoom: Float
        get() = visualScale

    val isTransformActive: Boolean
        get() = visualScale != DEFAULT_ZOOM || panOffset != Offset.Zero

    val canPanContent: Boolean
        get() = visualScale > DEFAULT_ZOOM

    fun updateViewportSize(size: IntSize) {
        viewportSize = size
        panOffset = coercePanOffset(panOffset, visualScale)
    }

    fun updateContentSize(size: IntSize) {
        contentSize = size
        panOffset = coercePanOffset(panOffset, visualScale)
    }

    fun setVisualScale(zoom: Float): Float {
        visualScale = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        panOffset = coercePanOffset(panOffset, visualScale)
        return visualScale
    }

    fun advanceZoomPreset(): Float {
        return setVisualScale(nextZoomPreset(visualScale))
    }

    fun beginTransform() {
        panOffset = coercePanOffset(panOffset, visualScale)
    }

    fun updateTransform(
        zoomChange: Float,
        pan: Offset,
        centroid: Offset
    ) {
        val oldScale = visualScale
        val newScale = (oldScale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)

        if (abs(newScale - oldScale) < 0.0001f && pan == Offset.Zero) {
            return
        }

        val contentX = (centroid.x - panOffset.x) / oldScale
        val contentY = (centroid.y - panOffset.y) / oldScale

        visualScale = newScale
        panOffset = coercePanOffset(
            offset = Offset(
                x = centroid.x - (contentX * newScale) + pan.x,
                y = centroid.y - (contentY * newScale) + pan.y
            ),
            scale = newScale
        )
    }

    fun panBy(delta: Offset): Boolean {
        val previousOffset = panOffset
        panOffset = coercePanOffset(
            offset = panOffset + delta,
            scale = visualScale
        )
        return panOffset != previousOffset
    }

    fun endTransform(): Float {
        panOffset = coercePanOffset(panOffset, visualScale)
        return visualScale
    }

    fun cancelTransform(): Float {
        panOffset = coercePanOffset(panOffset, visualScale)
        return visualScale
    }

    fun desiredRenderQualityScale(): Float {
        return renderQualityFor(visualScale)
    }

    fun refineRenderQualityIfNeeded(): Boolean {
        val targetQualityScale = desiredRenderQualityScale()

        if (abs(targetQualityScale - lastRenderedQualityScale) < RENDER_REFINEMENT_THRESHOLD) {
            return false
        }

        renderQualityScale = targetQualityScale
        return true
    }

    fun markRenderQualityDisplayed(qualityScale: Float = renderQualityScale) {
        lastRenderedQualityScale = qualityScale.coerceIn(
            MIN_RENDER_QUALITY_SCALE,
            MAX_RENDER_QUALITY_SCALE
        )
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

        if (
            viewportWidth <= 0f ||
            viewportHeight <= 0f ||
            contentSize.width <= 0 ||
            contentSize.height <= 0
        ) {
            return offset
        }

        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale

        val minY = if (scaledContentHeight > viewportHeight) {
            viewportHeight - scaledContentHeight
        } else {
            0f
        }

        val coercedX = if (scaledContentWidth > viewportWidth) {
            offset.x.coerceIn(viewportWidth - scaledContentWidth, 0f)
        } else {
            (viewportWidth - scaledContentWidth) / 2f
        }

        return Offset(
            x = coercedX,
            y = offset.y.coerceIn(minY, 0f)
        )
    }

    private fun renderQualityFor(scale: Float): Float {
        return scale.coerceIn(
            MIN_RENDER_QUALITY_SCALE,
            MAX_RENDER_QUALITY_SCALE
        )
    }

    companion object {
        const val MIN_ZOOM = 0.85f
        const val DEFAULT_ZOOM = 1f
        const val MAX_ZOOM = 4f
        const val MIN_RENDER_QUALITY_SCALE = 1f
        const val MAX_RENDER_QUALITY_SCALE = 2f

        private const val RENDER_REFINEMENT_THRESHOLD = 0.18f
    }
}
