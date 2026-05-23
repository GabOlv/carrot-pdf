package com.example.carrotpdf.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.model.PdfTabPersistence
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.pdf.createPdfFromImages
import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.getPdfPageSizes
import com.example.carrotpdf.pdf.printPdf
import com.example.carrotpdf.pdf.searchPdfText
import com.example.carrotpdf.pdf.sharePdf
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun TabSwitcherDialog(
    tabs: List<PdfTab>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onMoveTab: (String, Int) -> Unit,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var closeCandidate by remember { mutableStateOf<PdfTab?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetWidth = (maxWidth * 0.86f)
                .coerceIn(360.dp, 620.dp)

            Column(
                modifier = Modifier
                    .width(sheetWidth)
                    .padding(horizontal = 12.dp, vertical = 18.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .background(
                        color = Color(0xF31A1D22),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable { }
                    .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Abas Abertas",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Editar",
                        color = CarrotColors.Accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tabs.isEmpty()) {
                    Text(
                        text = "Nenhum PDF estÃ¡ aberto.",
                        color = CarrotColors.TextMuted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(
                        items = tabs,
                        key = { tab -> tab.id }
                    ) { tab ->
                        TabSwitcherRow(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onSelect = {
                                onSelect(tab.id)
                            },
                            onClose = {
                                closeCandidate = tab
                            },
                            onMove = { direction ->
                                onMoveTab(tab.id, direction)
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(54.dp)
                        .border(
                            width = 1.dp,
                            color = CarrotColors.Accent.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenPdf() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+",
                            color = CarrotColors.Accent,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Abrir Novo PDF",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    closeCandidate?.let { tab ->
        AlertDialog(
            onDismissRequest = {
                closeCandidate = null
            },
            title = {
                Text(
                    text = "Fechar a Aba?",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Fechar ${tab.title}?",
                    color = CarrotColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeCandidate = null
                        onCloseTab(tab.id)
                    }
                ) {
                    Text(
                        text = "Sim",
                        color = CarrotColors.Accent
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        closeCandidate = null
                    }
                ) {
                    Text(
                        text = "NÃ£o",
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            },
            containerColor = Color(0xF51A1D22),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TabSwitcherRow(
    tab: PdfTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: (Int) -> Unit
) {
    val density = LocalDensity.current
    val reorderThresholdPx = with(density) { 96.dp.toPx() }
    var accumulatedDrag by remember(tab.id) { mutableFloatStateOf(0f) }
    var visualDragOffset by remember(tab.id) { mutableFloatStateOf(0f) }
    var isReordering by remember(tab.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = with(density) { visualDragOffset.toDp() })
                .scale(if (isReordering) 1.025f else 1f)
                .shadow(
                    elevation = if (isReordering) 12.dp else 0.dp,
                    shape = RoundedCornerShape(10.dp)
                )
                .background(
                    color = if (isReordering) Color(0xFF242A31) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) CarrotColors.Accent else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfFileGlyph()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tab.title,
                color = CarrotColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            IconButtonCanvas(
                contentDescription = "Fechar a Aba  ",
                onClick = onClose
            ) {
                drawLine(
                    color = Color.White.copy(alpha = 0.58f),
                    start = Offset(8.dp.toPx(), 8.dp.toPx()),
                    end = Offset(16.dp.toPx(), 16.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.58f),
                    start = Offset(16.dp.toPx(), 8.dp.toPx()),
                    end = Offset(8.dp.toPx(), 16.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Canvas(
                modifier = Modifier
                    .size(width = 40.dp, height = 44.dp)
                    .pointerInput(tab.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                down.consume()
                                var lastY = down.position.y
                                var wasReleased = false

                                val releasedBeforeHold = withTimeoutOrNull(TAB_REORDER_HOLD_MS) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: return@withTimeoutOrNull true

                                        if (change.changedToUpIgnoreConsumed()) {
                                            return@withTimeoutOrNull true
                                        }

                                        lastY = change.position.y
                                        change.consume()
                                    }
                                } == true

                                if (releasedBeforeHold) {
                                    continue
                                }

                                isReordering = true
                                accumulatedDrag = 0f
                                visualDragOffset = 0f

                                while (!wasReleased) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break

                                    if (change.changedToUpIgnoreConsumed()) {
                                        wasReleased = true
                                        break
                                    }

                                    val deltaY = change.positionChange().y
                                    lastY = change.position.y

                                    change.consume()
                                    accumulatedDrag += deltaY
                                    visualDragOffset += deltaY

                                    if (accumulatedDrag > reorderThresholdPx) {
                                        onMove(1)
                                        accumulatedDrag = 0f
                                        visualDragOffset -= reorderThresholdPx
                                    } else if (accumulatedDrag < -reorderThresholdPx) {
                                        onMove(-1)
                                        accumulatedDrag = 0f
                                        visualDragOffset += reorderThresholdPx
                                    }
                                }

                                isReordering = false
                                accumulatedDrag = 0f
                                visualDragOffset = 0f
                            }
                        }
                    }
            ) {
                val handleColor = if (isReordering) {
                    Color(0xFFFF7A1A)
                } else {
                    Color.White.copy(alpha = 0.38f)
                }

                listOf(10.dp, 18.dp, 26.dp).forEach { y ->
                    drawCircle(
                        color = handleColor,
                        radius = 1.35.dp.toPx(),
                        center = Offset(16.dp.toPx(), (y + 4.dp).toPx())
                    )
                    drawCircle(
                        color = handleColor,
                        radius = 1.35.dp.toPx(),
                        center = Offset(24.dp.toPx(), (y + 4.dp).toPx())
                    )
                }
            }
        }
    }
}

