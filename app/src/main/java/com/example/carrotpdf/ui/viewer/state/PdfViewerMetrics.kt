package com.example.carrotpdf.ui.viewer.state

import androidx.compose.runtime.Immutable

@Immutable
data class PdfViewerMetrics(
    val pageCount: Int,
    val currentPageIndex: Int,
    val zoom: Float,
    val visiblePageRange: IntRange? = null
) {
    val hasPages: Boolean
        get() = pageCount > 0
}
