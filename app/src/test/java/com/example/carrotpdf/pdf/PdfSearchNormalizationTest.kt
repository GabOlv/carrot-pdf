package com.example.carrotpdf.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSearchNormalizationTest {
    @Test
    fun `search ignores accents and case while preserving source range`() {
        val text = "Introdução à AÇÃO educativa"

        val ranges = findNormalizedPdfMatches(text, "acao")

        assertEquals(listOf(13..16), ranges)
        assertEquals("AÇÃO", text.substring(ranges.single().first, ranges.single().last + 1))
    }

    @Test
    fun `search collapses extracted line and word whitespace`() {
        val ranges = findNormalizedPdfMatches("alpha\n   beta", "alpha beta")

        assertTrue(ranges.isNotEmpty())
    }
}
