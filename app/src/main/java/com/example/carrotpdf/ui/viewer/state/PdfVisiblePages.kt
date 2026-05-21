package com.example.carrotpdf.ui.viewer.state

import androidx.compose.runtime.Immutable

@Immutable
data class PdfVisiblePages(
    val visibleRange: IntRange?,
    val activeRange: IntRange?,
    val primaryVisiblePage: Int? = visibleRange?.first
) {
    val firstVisiblePage: Int?
        get() = primaryVisiblePage ?: visibleRange?.first

    fun isActive(pageIndex: Int): Boolean {
        return activeRange?.contains(pageIndex) == true
    }

    companion object {
        val Empty = PdfVisiblePages(
            visibleRange = null,
            activeRange = null,
            primaryVisiblePage = null
        )
    }
}
