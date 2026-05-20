package com.example.carrotpdf.ui.viewer.render

import android.graphics.Bitmap

sealed interface PdfPageRenderState {
    data object NotRequested : PdfPageRenderState
    data object Loading : PdfPageRenderState
    data class Ready(val bitmap: Bitmap) : PdfPageRenderState
    data object Failed : PdfPageRenderState
}
