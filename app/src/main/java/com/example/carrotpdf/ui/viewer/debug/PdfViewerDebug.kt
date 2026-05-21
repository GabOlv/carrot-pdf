package com.example.carrotpdf.ui.viewer.debug

import android.util.Log

object PdfViewerDebug {
    const val ENABLED = false

    private const val TAG = "CarrotPdfViewer"

    fun log(message: () -> String) {
        if (ENABLED) {
            Log.d(TAG, message())
        }
    }
}
