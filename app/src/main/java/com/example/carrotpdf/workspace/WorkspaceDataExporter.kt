package com.example.carrotpdf.workspace

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WorkspaceDataExportResult(
    val notesExported: Boolean,
    val canvasExported: Boolean
) {
    val success: Boolean
        get() = notesExported && canvasExported
}

fun exportWorkspaceData(
    context: Context,
    title: String,
    notesText: String,
    canvas: WorkspaceCanvas
): WorkspaceDataExportResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return WorkspaceDataExportResult(
            notesExported = false,
            canvasExported = false
        )
    }

    val stem = title.safeExportStem()
    val relativePath = "Download/CarrotPDF/export/$stem-${exportTimestamp()}"

    val notesExported = exportNotesPdf(
        context = context,
        fileName = "$stem.pdf",
        relativePath = relativePath,
        title = title,
        notesText = notesText
    )
    val canvasExported = exportCanvasPng(
        context = context,
        fileName = "$stem.png",
        relativePath = relativePath,
        canvas = canvas
    )

    return WorkspaceDataExportResult(
        notesExported = notesExported,
        canvasExported = canvasExported
    )
}

private fun exportNotesPdf(
    context: Context,
    fileName: String,
    relativePath: String,
    title: String,
    notesText: String
): Boolean {
    val outputUri = context.createDownloadUri(
        fileName = fileName,
        mimeType = "application/pdf",
        relativePath = relativePath
    ) ?: return false

    return runCatching {
        val document = PdfDocument()

        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(28, 31, 36)
                textSize = 14f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(18, 20, 24)
                textSize = 20f
                isFakeBoldText = true
            }

            val pageWidth = NOTES_PAGE_WIDTH
            val pageHeight = NOTES_PAGE_HEIGHT
            val margin = NOTES_PAGE_MARGIN
            val lineHeight = paint.textSize * 1.45f
            val textWidth = pageWidth - margin * 2f
            val lines = buildNotesLines(
                text = notesText.ifBlank { "No notes." },
                paint = paint,
                maxWidth = textWidth
            )

            var lineIndex = 0
            var pageNumber = 1

            while (lineIndex < lines.size || pageNumber == 1) {
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                val canvas = page.canvas
                var y = margin

                if (pageNumber == 1) {
                    canvas.drawText(
                        title.ellipsizeToWidth(
                            paint = titlePaint,
                            maxWidth = textWidth
                        ),
                        margin,
                        y,
                        titlePaint
                    )
                    y += 34f
                }

                while (lineIndex < lines.size && y + lineHeight < pageHeight - margin) {
                    canvas.drawText(lines[lineIndex], margin, y, paint)
                    y += lineHeight
                    lineIndex += 1
                }

                document.finishPage(page)
                pageNumber += 1
            }

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                document.writeTo(output)
            } ?: return false
        } finally {
            document.close()
        }

        context.markDownloadReady(outputUri)
        true
    }.getOrDefault(false)
}

private fun exportCanvasPng(
    context: Context,
    fileName: String,
    relativePath: String,
    canvas: WorkspaceCanvas
): Boolean {
    val outputUri = context.createDownloadUri(
        fileName = fileName,
        mimeType = "image/png",
        relativePath = relativePath
    ) ?: return false

    return runCatching {
        val scale = min(
            MAX_CANVAS_EXPORT_SIDE / max(canvas.width, canvas.height),
            1f
        )
        val bitmapWidth = ceil(canvas.width * scale).roundToInt().coerceAtLeast(1)
        val bitmapHeight = ceil(canvas.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

        try {
            val bitmapCanvas = Canvas(bitmap)
            bitmapCanvas.drawColor(Color.rgb(249, 248, 244))
            canvas.strokes.forEach { stroke ->
                bitmapCanvas.drawCanvasStroke(
                    stroke = stroke,
                    scale = scale
                )
            }

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: return false
        } finally {
            bitmap.recycle()
        }

        context.markDownloadReady(outputUri)
        true
    }.getOrDefault(false)
}

private fun Canvas.drawCanvasStroke(
    stroke: CanvasInkStroke,
    scale: Float
) {
    if (stroke.points.isEmpty()) {
        return
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stroke.color.toAndroidColor()
        style = Paint.Style.STROKE
        strokeWidth = (stroke.width * scale).coerceAtLeast(1f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(
            point.x * scale,
            point.y * scale,
            paint.strokeWidth / 2f,
            paint
        )
        return
    }

    val path = Path()
    val first = stroke.points.first()
    path.moveTo(first.x * scale, first.y * scale)

    stroke.points.drop(1).forEach { point ->
        path.lineTo(point.x * scale, point.y * scale)
    }

    drawPath(path, paint)
}

private fun buildNotesLines(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    return text
        .replace("\r\n", "\n")
        .split("\n")
        .flatMap { paragraph ->
            if (paragraph.isBlank()) {
                listOf("")
            } else {
                paragraph.wrapToWidth(
                    paint = paint,
                    maxWidth = maxWidth
                )
            }
        }
}

private fun String.wrapToWidth(
    paint: Paint,
    maxWidth: Float
): List<String> {
    val words = trim().split(Regex("\\s+"))
    val lines = mutableListOf<String>()
    var current = ""

    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"

        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotBlank()) {
                lines += current
            }
            current = word
        }
    }

    if (current.isNotBlank()) {
        lines += current
    }

    return lines.ifEmpty { listOf("") }
}

private fun String.ellipsizeToWidth(
    paint: Paint,
    maxWidth: Float
): String {
    if (paint.measureText(this) <= maxWidth) {
        return this
    }

    val suffix = "..."
    var end = length

    while (end > 0 && paint.measureText(substring(0, end) + suffix) > maxWidth) {
        end -= 1
    }

    return if (end > 0) {
        substring(0, end).trimEnd() + suffix
    } else {
        suffix
    }
}

private fun Context.createDownloadUri(
    fileName: String,
    mimeType: String,
    relativePath: String
): android.net.Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    return contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
}

private fun Context.markDownloadReady(uri: android.net.Uri) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
    }

    contentResolver.update(uri, values, null, null)
}

private fun Long.toAndroidColor(): Int {
    return Color.argb(
        ((this ushr 24) and 0xFF).toInt(),
        ((this ushr 16) and 0xFF).toInt(),
        ((this ushr 8) and 0xFF).toInt(),
        (this and 0xFF).toInt()
    )
}

private fun String.safeExportStem(): String {
    return substringBeforeLast(".")
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-', '.', '_')
        .take(48)
        .ifBlank { "carrot-pdf" }
}

private fun exportTimestamp(): String {
    return LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    )
}

private const val NOTES_PAGE_WIDTH = 595
private const val NOTES_PAGE_HEIGHT = 842
private const val NOTES_PAGE_MARGIN = 48f
private const val MAX_CANVAS_EXPORT_SIDE = 4096f
