package com.example.carrotpdf.model

import android.net.Uri
import com.example.carrotpdf.pdf.PdfPageSize
import java.util.UUID

data class PdfTab(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val title: String,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val pageSizes: List<PdfPageSize> = emptyList(),
    val zoom: Float = 1f,
    val viewportLeft: Float = 0f,
    val viewportTop: Float = 0f
)
