package com.example.carrotpdf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBars

@Composable
fun ReaderTextSelectionBar(
    selectedText: String,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT + statusBarTop)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF70B0D10),
                        Color(0xEE11151A)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = 14.dp,
                    top = statusBarTop,
                    end = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonCanvas(
                contentDescription = "Close text selection",
                onClick = onBack
            ) {
                drawLine(
                    color = Color.White,
                    start = Offset(17.dp.toPx(), 5.dp.toPx()),
                    end = Offset(8.dp.toPx(), 12.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(8.dp.toPx(), 12.dp.toPx()),
                    end = Offset(17.dp.toPx(), 19.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "Select text",
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            )

            Text(
                text = selectedText,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(0.85f)
                    .padding(horizontal = 8.dp)
            )

            IconButtonCanvas(
                contentDescription = "Copy selected text",
                onClick = onCopy
            ) {
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(8.dp.toPx(), 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.7f),
                    topLeft = Offset(5.dp.toPx(), 8.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8.dp.toPx())
                )
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
    }
}
