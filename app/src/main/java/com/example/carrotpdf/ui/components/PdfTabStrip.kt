package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun PdfTabStrip(
    tabs: List<PdfTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenPdf: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .background(CarrotColors.Background)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId

            Box(
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight()
                    .background(CarrotColors.Surface)
                    .clickable { onSelectTab(tab.id) }
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.title,
                        color = if (isActive) CarrotColors.Accent else CarrotColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        modifier = Modifier.clickable {
                            onCloseTab(tab.id)
                        },
                        text = "x",
                        color = CarrotColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(2.dp)
                            .width(132.dp)
                            .background(CarrotColors.Accent)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(CarrotColors.SurfaceSoft)
            )
        }

        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 48.dp)
                .background(CarrotColors.Surface)
                .clickable { onOpenPdf() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = CarrotColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
