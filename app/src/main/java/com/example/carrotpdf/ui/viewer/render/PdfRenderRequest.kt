package com.example.carrotpdf.ui.viewer.render

enum class PdfRenderPriority {
    Visible,
    Nearby
}

data class PdfRenderRequest(
    val key: PdfRenderKey,
    val priority: PdfRenderPriority
)
