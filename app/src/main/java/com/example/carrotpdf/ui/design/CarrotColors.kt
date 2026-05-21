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
    background = Color(0xFF0D1116),
    surface = Color(0xFF151A20),
    surfaceAlt = Color(0xFF1E252D),
    surfaceSoft = Color(0xFF2A323B),
    textPrimary = Color(0xFFF5F3EF),
    textSecondary = Color(0xFFC8CDD4),
    textMuted = Color(0xFF808993),
    accent = Color(0xFFFF7A1A),
    accentSoft = Color(0x33FF7A1A),
    pdfCanvas = Color(0xFF11161C),
    pageShadow = Color(0x66000000)
)

private val LightCarrotPalette = CarrotPalette(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF2F3F5),
    surfaceSoft = Color(0xFFE4E6EA),
    textPrimary = Color(0xFF171A1F),
    textSecondary = Color(0xFF4E5661),
    textMuted = Color(0xFF8C949F),
    accent = Color(0xFFFF7A1A),
    accentSoft = Color(0x22FF7A1A),
    pdfCanvas = Color(0xFFF1F2F4),
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
