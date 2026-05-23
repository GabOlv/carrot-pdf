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
fun PdfFileGlyph() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 30.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PDF",
            color = Color(0xFFFF3B30),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.13f))
    )
}

@Composable
fun MenuIconCanvas(
    icon: MenuIcon,
    color: Color
) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()

        when (icon) {
            MenuIcon.Folder -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(3.dp.toPx(), 7.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(
                    color = color,
                    start = Offset(4.dp.toPx(), 8.dp.toPx()),
                    end = Offset(9.dp.toPx(), 8.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            MenuIcon.PageNumber -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(5.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(color, Offset(9.dp.toPx(), 8.dp.toPx()), Offset(15.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 12.dp.toPx()), Offset(15.dp.toPx(), 12.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 16.dp.toPx()), Offset(13.dp.toPx(), 16.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Share -> {
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(7.dp.toPx(), 12.dp.toPx()), style = Stroke(width = stroke))
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(16.dp.toPx(), 7.dp.toPx()), style = Stroke(width = stroke))
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(16.dp.toPx(), 17.dp.toPx()), style = Stroke(width = stroke))
                drawLine(color, Offset(9.dp.toPx(), 11.dp.toPx()), Offset(14.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 13.dp.toPx()), Offset(14.dp.toPx(), 16.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Download -> {
                drawLine(color, Offset(12.dp.toPx(), 4.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(6.dp.toPx(), 20.dp.toPx()), Offset(18.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Print -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(6.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(4.dp.toPx(), 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(color, Offset(8.dp.toPx(), 18.dp.toPx()), Offset(16.dp.toPx(), 18.dp.toPx()), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun IconButtonCanvas(
    contentDescription: String,
    onClick: () -> Unit,
    drawIcon: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(24.dp),
            contentDescription = contentDescription,
            onDraw = drawIcon
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBookTabsIcon() {
    val stroke = 1.8.dp.toPx()
    val white = Color.White.copy(alpha = 0.92f)

    drawRoundRect(
        color = white,
        topLeft = Offset(4.dp.toPx(), 5.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 15.dp.toPx()),
        style = Stroke(width = stroke),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx())
    )
    drawLine(
        color = white,
        start = Offset(12.dp.toPx(), 5.dp.toPx()),
        end = Offset(12.dp.toPx(), 20.dp.toPx()),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = white.copy(alpha = 0.62f),
        start = Offset(7.dp.toPx(), 9.dp.toPx()),
        end = Offset(10.dp.toPx(), 9.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = white.copy(alpha = 0.62f),
        start = Offset(14.dp.toPx(), 9.dp.toPx()),
        end = Offset(17.dp.toPx(), 9.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

enum class MenuIcon {
    Folder,
    PageNumber,
    Share,
    Download,
    Print
}

