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
fun FloatingAnnotationButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(15.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDDFF7A1A),
                        Color(0xCCFF5A10)
                    )
                ),
                shape = RoundedCornerShape(15.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(27.dp)) {
            val body = Color.White.copy(alpha = 0.96f)
            val green = Color(0xFF78D66A)
            val stroke = 2.4.dp.toPx()

            drawLine(
                color = body,
                start = Offset(7.dp.toPx(), 21.dp.toPx()),
                end = Offset(18.dp.toPx(), 10.dp.toPx()),
                strokeWidth = 4.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF7A1A).copy(alpha = 0.92f),
                start = Offset(9.dp.toPx(), 19.dp.toPx()),
                end = Offset(15.dp.toPx(), 13.dp.toPx()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = body,
                start = Offset(17.dp.toPx(), 8.dp.toPx()),
                end = Offset(22.dp.toPx(), 13.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = green,
                start = Offset(19.dp.toPx(), 8.dp.toPx()),
                end = Offset(24.dp.toPx(), 4.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = green,
                start = Offset(21.dp.toPx(), 10.dp.toPx()),
                end = Offset(26.dp.toPx(), 8.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = green.copy(alpha = 0.9f),
                start = Offset(20.dp.toPx(), 8.dp.toPx()),
                end = Offset(22.dp.toPx(), 3.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FloatingPrintButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                color = Color(0xCC1C2229),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(25.dp)) {
            val icon = Color.White.copy(alpha = 0.92f)
            val accent = Color(0xFFFF7A1A)
            val stroke = 2.dp.toPx()

            drawRoundRect(
                color = icon,
                topLeft = Offset(7.dp.toPx(), 3.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(11.dp.toPx(), 7.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = icon,
                topLeft = Offset(4.dp.toPx(), 9.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(17.dp.toPx(), 9.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = icon,
                topLeft = Offset(7.dp.toPx(), 15.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(11.dp.toPx(), 7.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2.dp.toPx()),
                style = Stroke(width = stroke)
            )
            drawCircle(
                color = accent,
                radius = 1.3.dp.toPx(),
                center = Offset(18.dp.toPx(), 12.dp.toPx())
            )
        }
    }
}

