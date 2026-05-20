package com.example.carrotpdf.ui.viewer.render

import kotlin.math.roundToInt

data class PdfRenderKey(
    val documentId: String,
    val pageIndex: Int,
    val scaleBucketPercent: Int
)

fun pdfRenderScaleBucketPercent(
    zoom: Float
): Int {
    return ((zoom * 100f) / SCALE_BUCKET_STEP_PERCENT)
        .roundToInt()
        .coerceAtLeast(1) * SCALE_BUCKET_STEP_PERCENT
}

private const val SCALE_BUCKET_STEP_PERCENT = 25
