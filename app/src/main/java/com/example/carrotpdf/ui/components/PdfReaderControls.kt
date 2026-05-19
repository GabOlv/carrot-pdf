package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun PdfReaderControls(
    activeTab: PdfTab?,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onResetZoom: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CarrotColors.Background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = activeTab?.title ?: "No document opened",
            color = CarrotColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        if (activeTab != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareButton(
                    text = "‹",
                    enabled = activeTab.currentPageIndex > 0,
                    onClick = onPreviousPage
                )

                Text(
                    text = "${activeTab.currentPageIndex + 1} / ${activeTab.pageCount.coerceAtLeast(1)}",
                    color = CarrotColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                SquareButton(
                    text = "›",
                    enabled = activeTab.pageCount == 0 || activeTab.currentPageIndex < activeTab.pageCount - 1,
                    onClick = onNextPage
                )

                Spacer(modifier = Modifier.width(8.dp))

                SquareButton(
                    text = "−",
                    enabled = activeTab.zoom > 0.7f,
                    onClick = onZoomOut
                )

                Text(
                    text = "${(activeTab.zoom * 100).toInt()}%",
                    color = CarrotColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                SquareButton(
                    text = "+",
                    enabled = activeTab.zoom < 3f,
                    onClick = onZoomIn
                )

                SquareButton(
                    text = "1×",
                    enabled = activeTab.zoom != 1f,
                    onClick = onResetZoom
                )
            }
        }
    }
}

@Composable
private fun SquareButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(34.dp)
            .width(42.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CarrotColors.SurfaceAlt,
            contentColor = CarrotColors.TextPrimary,
            disabledContainerColor = CarrotColors.Surface,
            disabledContentColor = CarrotColors.TextMuted
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Text(text)
    }
}