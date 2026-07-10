package com.example.carrotpdf.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePdfNamingTest {
    @Test
    fun `first generated image PDF starts at zero`() {
        val index = nextImagePdfIndex(
            existingTitles = emptyList(),
            lastReservedIndex = -1
        )

        assertEquals(0, index)
    }

    @Test
    fun `next index follows greatest existing or reserved number`() {
        val index = nextImagePdfIndex(
            existingTitles = listOf("image-pdf-2.pdf", "notes.pdf", "IMAGE-PDF-7"),
            lastReservedIndex = 4
        )

        assertEquals(8, index)
    }
}
