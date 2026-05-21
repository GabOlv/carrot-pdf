package com.example.carrotpdf.model

import java.util.UUID

data class PdfCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String
) {
    companion object {
        const val DEFAULT_ID = "default"

        val Default = PdfCategory(
            id = DEFAULT_ID,
            name = "Default"
        )
    }
}
