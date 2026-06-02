package com.example.carrotpdf.pdf

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.carrotpdf.workspace.InkPoint
import com.example.carrotpdf.workspace.InkTool
import com.example.carrotpdf.workspace.PageInkStroke
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class AnnotatedPdfExport(
    val uri: Uri,
    val title: String
)

fun createAnnotatedPdfExport(
    context: Context,
    sourceUri: Uri,
    title: String,
    pageInkStrokes: List<PageInkStroke>
): Result<AnnotatedPdfExport> = runCatching {
    require(pageInkStrokes.isNotEmpty()) { "No PDF annotations to export." }

    PDFBoxResourceLoader.init(context.applicationContext)

    val outputDirectory = File(context.cacheDir, ANNOTATED_PDF_DIRECTORY).apply {
        mkdirs()
    }
    cleanupOldAnnotatedPdfs(outputDirectory)

    val outputTitle = title.annotatedCacheFileName()
    val outputFile = File(outputDirectory, outputTitle)
    val strokesByPage = pageInkStrokes
        .filter { stroke -> stroke.tool == InkTool.Pen && stroke.points.isNotEmpty() }
        .groupBy { stroke -> stroke.pageIndex }

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        PDDocument.load(input).use { document ->
            strokesByPage.forEach { (pageIndex, strokes) ->
                if (pageIndex in 0 until document.numberOfPages) {
                    document.getPage(pageIndex).appendInkStrokes(
                        document = document,
                        strokes = strokes
                    )
                }
            }

            FileOutputStream(outputFile).use { output ->
                document.save(output)
            }
        }
    } ?: error("Could not open source PDF.")

    AnnotatedPdfExport(
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        ),
        title = title.ensurePdfFileName()
    )
}

private fun PDPage.appendInkStrokes(
    document: PDDocument,
    strokes: List<PageInkStroke>
) {
    if (rotation != 0) {
        return
    }

    val pageBox = cropBox ?: mediaBox ?: return

    if (pageBox.width <= 0f || pageBox.height <= 0f) {
        return
    }

    val contentStream = PDPageContentStream(
        document,
        this,
        AppendMode.APPEND,
        true,
        true
    )

    try {
        strokes.forEach { stroke ->
            contentStream.drawInkStroke(
                stroke = stroke,
                pageBox = pageBox
            )
        }
    } finally {
        contentStream.close()
    }
}

private fun PDPageContentStream.drawInkStroke(
    stroke: PageInkStroke,
    pageBox: PDRectangle
) {
    val points = stroke.points
        .mapNotNull { point -> point.toPdfPoint(pageBox) }

    if (points.isEmpty()) {
        return
    }

    val color = stroke.color.toPdfRgb()
    val strokeWidth = (stroke.width * min(pageBox.width, pageBox.height))
        .coerceIn(MIN_INK_STROKE_WIDTH_PT, MAX_INK_STROKE_WIDTH_PT)

    setStrokingColor(
        color.red / 255f,
        color.green / 255f,
        color.blue / 255f
    )
    setLineWidth(strokeWidth)
    setLineCapStyle(PDF_ROUND_LINE_CAP)
    setLineJoinStyle(PDF_ROUND_LINE_JOIN)

    if (points.size == 1) {
        val point = points.first()
        moveTo(point.x - strokeWidth / 2f, point.y)
        lineTo(point.x + strokeWidth / 2f, point.y)
        stroke()
        return
    }

    val first = points.first()
    moveTo(first.x, first.y)

    points.drop(1).forEach { point ->
        lineTo(point.x, point.y)
    }

    stroke()
}

private fun InkPoint.toPdfPoint(
    pageBox: PDRectangle
): PdfPoint? {
    if (
        !x.isFinite() ||
        !y.isFinite() ||
        x < 0f ||
        x > 1f ||
        y < 0f ||
        y > 1f
    ) {
        return null
    }

    return PdfPoint(
        x = pageBox.lowerLeftX + x * pageBox.width,
        y = pageBox.lowerLeftY + (1f - y) * pageBox.height
    )
}

private fun Long.toPdfRgb(): PdfRgb {
    return PdfRgb(
        red = ((this ushr 16) and 0xFF).toInt(),
        green = ((this ushr 8) and 0xFF).toInt(),
        blue = (this and 0xFF).toInt()
    )
}

private fun String.ensurePdfFileName(): String {
    return if (endsWith(".pdf", ignoreCase = true)) {
        this
    } else {
        "$this.pdf"
    }
}

private fun String.annotatedCacheFileName(): String {
    val stem = substringBeforeLast(".")
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-', '.', '_')
        .take(48)
        .ifBlank { "carrot-pdf" }

    return "$stem-annotated-${System.currentTimeMillis()}.pdf"
}

private fun cleanupOldAnnotatedPdfs(directory: File) {
    val now = System.currentTimeMillis()

    directory.listFiles()
        ?.filter { file ->
            file.isFile &&
                file.extension.equals("pdf", ignoreCase = true) &&
                now - file.lastModified() > 7L * 24L * 60L * 60L * 1000L
        }
        ?.forEach { file ->
            runCatching { file.delete() }
        }
}

private data class PdfPoint(
    val x: Float,
    val y: Float
)

private data class PdfRgb(
    val red: Int,
    val green: Int,
    val blue: Int
)

private const val ANNOTATED_PDF_DIRECTORY = "generated_pdfs"
private const val MIN_INK_STROKE_WIDTH_PT = 0.75f
private const val MAX_INK_STROKE_WIDTH_PT = 18f
private const val PDF_ROUND_LINE_CAP = 1
private const val PDF_ROUND_LINE_JOIN = 1
