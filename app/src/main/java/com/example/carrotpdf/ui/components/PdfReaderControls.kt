package com.example.carrotpdf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.viewer.state.PdfViewerState

@Composable
fun PdfReaderControls(
    activeTab: PdfTab?,
    viewerState: PdfViewerState?,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onEditClick: () -> Unit,
    onSearchClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    val hasDocument = activeTab != null && viewerState != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(CarrotColors.Background),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FooterIconButton(
            icon = FooterIcon.Bookmark,
            isAccent = isBookmarked,
            onClick = onBookmarkClick
        )

        FooterIconButton(
            icon = FooterIcon.Categories,
            onClick = onCategoriesClick
        )

        FooterIconButton(
            icon = FooterIcon.Draw,
            onClick = onEditClick
        )

        FooterIconButton(
            icon = FooterIcon.Search,
            onClick = onSearchClick
        )

        FooterIconButton(
            icon = FooterIcon.Config,
            onClick = onConfigClick
        )
    }
}

@Composable
private fun FooterIconButton(
    icon: FooterIcon,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (isAccent) CarrotColors.Accent else CarrotColors.TextSecondary

    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(24.dp)
        ) {
            val stroke = 2.dp.toPx()

            when (icon) {
                FooterIcon.Config -> {
                    listOf(6.dp, 12.dp, 18.dp).forEach { y ->
                        drawCircle(
                            color = color,
                            radius = 1.8.dp.toPx(),
                            center = Offset(12.dp.toPx(), y.toPx())
                        )
                    }
                }

                FooterIcon.Categories -> {
                    drawLine(color, Offset(5.dp.toPx(), 6.dp.toPx()), Offset(11.dp.toPx(), 4.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(11.dp.toPx(), 4.dp.toPx()), Offset(19.dp.toPx(), 6.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(5.dp.toPx(), 6.dp.toPx()), Offset(5.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(19.dp.toPx(), 6.dp.toPx()), Offset(19.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(5.dp.toPx(), 20.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(19.dp.toPx(), 20.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke, StrokeCap.Round)
                    drawLine(color, Offset(12.dp.toPx(), 5.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), 1.4.dp.toPx(), StrokeCap.Round)
                }

                FooterIcon.Draw -> {
                    drawLine(
                        color = color,
                        start = Offset(6.dp.toPx(), 18.dp.toPx()),
                        end = Offset(17.dp.toPx(), 7.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(15.dp.toPx(), 5.dp.toPx()),
                        end = Offset(19.dp.toPx(), 9.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(5.dp.toPx(), 20.dp.toPx()),
                        end = Offset(10.dp.toPx(), 19.dp.toPx()),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                FooterIcon.Bookmark -> {
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(7.dp.toPx(), 4.dp.toPx())
                            lineTo(17.dp.toPx(), 4.dp.toPx())
                            lineTo(17.dp.toPx(), 20.dp.toPx())
                            lineTo(12.dp.toPx(), 16.dp.toPx())
                            lineTo(7.dp.toPx(), 20.dp.toPx())
                            close()
                        },
                        color = color,
                        style = Stroke(width = stroke)
                    )
                }

                FooterIcon.Search -> {
                    drawCircle(
                        color = color,
                        radius = 7.dp.toPx(),
                        center = Offset(10.dp.toPx(), 10.dp.toPx()),
                        style = Stroke(width = stroke)
                    )
                    drawLine(
                        color = color,
                        start = Offset(15.5.dp.toPx(), 15.5.dp.toPx()),
                        end = Offset(21.dp.toPx(), 21.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private enum class FooterIcon {
    Bookmark,
    Categories,
    Draw,
    Search,
    Config
}
