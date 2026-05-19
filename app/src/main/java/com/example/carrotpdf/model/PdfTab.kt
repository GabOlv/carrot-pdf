package com.example.carrotpdf.model

import android.net.Uri
import java.util.UUID

data class PdfTab(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val title: String,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val zoom: Float = 1f
)