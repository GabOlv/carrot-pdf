package com.example.carrotpdf.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InkPointerPolicyTest {
    @Test
    fun `touch contact is rejected while stylus is active`() {
        assertTrue(
            shouldRejectContactForPalmRejection(
                stylusActive = true,
                contactIsStylus = false
            )
        )
    }

    @Test
    fun `finger drawing remains available without stylus`() {
        assertFalse(
            shouldRejectContactForPalmRejection(
                stylusActive = false,
                contactIsStylus = false
            )
        )
    }
}
