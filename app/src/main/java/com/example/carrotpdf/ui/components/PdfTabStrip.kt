package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PdfTabStrip(
    tabs: List<PdfTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenPdf: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(CarrotColors.Background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId

            Row(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(32.dp)
                    .background(
                        color = if (isActive) CarrotColors.Accent else CarrotColors.Surface,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectTab(tab.id) }
                    .padding(start = 10.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.title,
                    color = if (isActive) CarrotColors.Background else CarrotColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(122.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    modifier = Modifier.clickable {
                        onCloseTab(tab.id)
                    },
                    text = "×",
                    color = if (isActive) CarrotColors.Background else CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(
            modifier = Modifier
                .height(32.dp)
                .background(
                    color = CarrotColors.Surface,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onOpenPdf() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+",
                color = CarrotColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}