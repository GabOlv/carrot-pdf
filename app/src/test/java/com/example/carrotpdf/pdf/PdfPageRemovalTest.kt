package com.example.carrotpdf.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPageRemovalTest {
    @Test
    fun `removed page has no destination`() {
        assertNull(remapPageIndexAfterRemoval(2, setOf(0, 2)))
    }

    @Test
    fun `remaining page shifts by removals before it`() {
        assertEquals(3, remapPageIndexAfterRemoval(5, setOf(0, 3)))
        assertEquals(0, remapPageIndexAfterRemoval(0, setOf(2, 4)))
    }
}
