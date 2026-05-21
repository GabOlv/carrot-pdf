package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.R
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun PdfTabStrip(
    tabs: List<PdfTab>,
    activeTabId: String?,
    isBookmarked: (String) -> Boolean,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTab: (String, Int) -> Unit,
    onOpenPdf: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .background(CarrotColors.Background)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { visibleIndex, tab ->
            val isActive = tab.id == activeTabId
            val isPinned = isBookmarked(tab.id)
            var dragAmount by remember(tab.id) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight()
                    .background(CarrotColors.Surface)
                    .border(
                        width = if (isPinned) 1.dp else 0.dp,
                        color = if (isPinned) CarrotColors.Accent else CarrotColors.Surface
                    )
                    .pointerInput(tab.id, visibleIndex) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragAmount = 0f
                            },
                            onHorizontalDrag = { _, dragDelta ->
                                dragAmount += dragDelta
                            },
                            onDragEnd = {
                                when {
                                    dragAmount > TAB_REORDER_THRESHOLD_PX -> {
                                        onMoveTab(tab.id, visibleIndex - 1)
                                    }

                                    dragAmount < -TAB_REORDER_THRESHOLD_PX -> {
                                        onMoveTab(tab.id, visibleIndex + 1)
                                    }
                                }

                                dragAmount = 0f
                            },
                            onDragCancel = {
                                dragAmount = 0f
                            }
                        )
                    }
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

                    if (isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_carrot_pin),
                            contentDescription = "Bookmarked",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        modifier = Modifier.clickable {
                            onCloseTab(tab.id)
                        }.size(18.dp),
                        painter = painterResource(R.drawable.ic_tab_close),
                        contentDescription = "Close tab",
                        tint = CarrotColors.TextMuted
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

private const val TAB_REORDER_THRESHOLD_PX = 48f
