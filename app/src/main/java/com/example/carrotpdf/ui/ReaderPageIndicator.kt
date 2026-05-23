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
fun BoxScope.DrivePageIndicator(
    currentPage: Int,
    pageCount: Int,
    isScrollInProgress: Boolean,
    scrollProgress: Float,
    onScrollToProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var thumbCenterY by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage, pageCount, isScrollInProgress, isDragging, scrollProgress) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
            return@LaunchedEffect
        }

        isVisible = true
        delay(PAGE_INDICATOR_VISIBLE_MS)
        isVisible = false
    }

    AnimatedVisibility(
        visible = isVisible || isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .padding(top = 24.dp, end = 10.dp, bottom = 24.dp)
        ) {
            val heightPx = with(density) { maxHeight.toPx() }

            val handleVisualWidth = 7.dp
            val handleVisualHeight = 44.dp
            val handleHitWidth = 34.dp
            val handleHalfHeightPx = with(density) { handleVisualHeight.toPx() / 2f }
            val travelHeightPx = (heightPx - (handleHalfHeightPx * 2f))
                .coerceAtLeast(1f)

            val targetCenterY = (
                    handleHalfHeightPx +
                            (travelHeightPx * scrollProgress.coerceIn(0f, 1f))
                    ).coerceIn(
                    handleHalfHeightPx,
                    heightPx - handleHalfHeightPx
                )

            LaunchedEffect(targetCenterY, isDragging) {
                if (!isDragging) {
                    thumbCenterY = targetCenterY
                }
            }

            fun scrollToThumbPosition(centerY: Float) {
                val clampedY = centerY.coerceIn(
                    handleHalfHeightPx,
                    heightPx - handleHalfHeightPx
                )

                thumbCenterY = clampedY

                val targetProgress = if (heightPx <= 0f) {
                    0f
                } else {
                    ((clampedY - handleHalfHeightPx) / travelHeightPx)
                        .coerceIn(0f, 1f)
                }

                onScrollToProgress(targetProgress)
            }

            // Gesture layer restricted to the current scrollbar thumb only.
            // This avoids jumping the document when the user taps elsewhere
            // along the page indicator's vertical space.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(handleHitWidth)
                    .fillMaxHeight()
                    .pointerInput(heightPx) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                val downY = down.position.y
                                val isOnThumb = downY in
                                    (thumbCenterY - handleHalfHeightPx)..(thumbCenterY + handleHalfHeightPx)

                                if (!isOnThumb) {
                                    continue
                                }

                                isDragging = true
                                down.consume()
                                val grabOffsetY = downY - thumbCenterY

                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)

                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            scrollToThumbPosition(change.position.y - grabOffsetY)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { change -> change.pressed })

                                isDragging = false
                            }
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        y = with(density) {
                            (thumbCenterY - handleHalfHeightPx).toDp()
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentPage / $pageCount",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            color = Color(0xDD1D2025),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(width = handleVisualWidth, height = handleVisualHeight)
                        .background(
                            color = CarrotColors.Accent,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        }
    }
}

