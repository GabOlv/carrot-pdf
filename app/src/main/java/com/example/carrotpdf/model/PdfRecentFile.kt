package com.example.carrotpdf.model

data class PdfRecentFile(
    val uri: String,
    val title: String,
    val openedAtMillis: Long
)
