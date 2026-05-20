package com.example.carrotpdf.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CarrotPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val pdfCanvas: Color,
    val pageShadow: Color
)

private val DarkCarrotPalette = CarrotPalette(
    background = Color(0xFF10151B),
    surface = Color(0xFF171D25),
    surfaceAlt = Color(0xFF202832),
    surfaceSoft = Color(0xFF26303B),
    textPrimary = Color(0xFFF4F1EA),
    textSecondary = Color(0xFFBAC1CC),
    textMuted = Color(0xFF79828E),
    accent = Color(0xFFFF9F43),
    accentSoft = Color(0x33FF9F43),
    pdfCanvas = Color(0xFF121820),
    pageShadow = Color(0x66000000)
)

private val LightCarrotPalette = CarrotPalette(
    background = Color(0xFFF6F3EF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEDE9E3),
    surfaceSoft = Color(0xFFE4DED6),
    textPrimary = Color(0xFF202124),
    textSecondary = Color(0xFF5F6368),
    textMuted = Color(0xFF8A8F98),
    accent = Color(0xFFE87922),
    accentSoft = Color(0x22E87922),
    pdfCanvas = Color(0xFFEAE6DF),
    pageShadow = Color(0x22000000)
)

private val LocalCarrotPalette = staticCompositionLocalOf {
    DarkCarrotPalette
}

object CarrotColors {
    val Background: Color
        @Composable get() = LocalCarrotPalette.current.background

    val Surface: Color
        @Composable get() = LocalCarrotPalette.current.surface

    val SurfaceAlt: Color
        @Composable get() = LocalCarrotPalette.current.surfaceAlt

    val SurfaceSoft: Color
        @Composable get() = LocalCarrotPalette.current.surfaceSoft

    val TextPrimary: Color
        @Composable get() = LocalCarrotPalette.current.textPrimary

    val TextSecondary: Color
        @Composable get() = LocalCarrotPalette.current.textSecondary

    val TextMuted: Color
        @Composable get() = LocalCarrotPalette.current.textMuted

    val Accent: Color
        @Composable get() = LocalCarrotPalette.current.accent

    val AccentSoft: Color
        @Composable get() = LocalCarrotPalette.current.accentSoft

    val PdfCanvas: Color
        @Composable get() = LocalCarrotPalette.current.pdfCanvas

    val PageShadow: Color
        @Composable get() = LocalCarrotPalette.current.pageShadow
}

@Composable
fun CarrotDesignTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCarrotPalette provides if (darkTheme) DarkCarrotPalette else LightCarrotPalette,
        content = content
    )
}