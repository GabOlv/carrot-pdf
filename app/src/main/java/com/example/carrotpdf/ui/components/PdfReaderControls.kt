package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.viewer.state.PdfViewerState

@Composable
fun PdfReaderControls(
    activeTab: PdfTab?,
    viewerState: PdfViewerState?,
    onZoomClick: () -> Unit,
    onLayoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(CarrotColors.Background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        if (activeTab != null && viewerState != null) {
            ReaderInfoBox(
                text = "${viewerState.currentPageIndex + 1} / ${viewerState.pageCount.coerceAtLeast(1)}"
            )

            Spacer(modifier = Modifier.weight(1f))

            ReaderInfoBox(
                text = "${(viewerState.zoom * 100).toInt()}%",
                onClick = onZoomClick
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                modifier = Modifier
                    .background(
                        color = CarrotColors.SurfaceAlt,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onLayoutClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                text = "▦",
                color = CarrotColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ReaderInfoBox(
    text: String,
    onClick: (() -> Unit)? = null
) {
    Text(
        modifier = Modifier
            .background(
                color = CarrotColors.Surface,
                shape = RoundedCornerShape(6.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        text = text,
        color = CarrotColors.TextPrimary,
        style = MaterialTheme.typography.bodyMedium
    )
}
