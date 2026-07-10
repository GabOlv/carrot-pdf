package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class EditedPdf(
    val uri: Uri,
    val title: String,
    val pageCount: Int
)

fun appendImagesToPdf(
    context: Context,
    sourceUri: Uri,
    imageUris: List<Uri>,
    title: String
): Result<EditedPdf> = editPdfDocument(
    context = context,
    sourceUri = sourceUri,
    title = title
) { document ->
    require(imageUris.isNotEmpty()) { "No images selected." }

    imageUris.forEach { imageUri ->
        val bitmap = decodeImageForPdf(context, imageUri)

        try {
            document.appendImagePage(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}

fun removePagesFromPdf(
    context: Context,
    sourceUri: Uri,
    pageIndices: Set<Int>,
    title: String
): Result<EditedPdf> = editPdfDocument(
    context = context,
    sourceUri = sourceUri,
    title = title
) { document ->
    val validIndices = pageIndices
        .filter { index -> index in 0 until document.numberOfPages }
        .toSortedSet()

    require(validIndices.isNotEmpty()) { "No valid pages selected." }
    require(validIndices.size < document.numberOfPages) { "A PDF must keep at least one page." }

    validIndices.sortedDescending().forEach(document::removePage)
}

internal fun remapPageIndexAfterRemoval(
    pageIndex: Int,
    removedPageIndices: Set<Int>
): Int? {
    if (pageIndex in removedPageIndices) {
        return null
    }

    return pageIndex - removedPageIndices.count { removedIndex -> removedIndex < pageIndex }
}

private fun editPdfDocument(
    context: Context,
    sourceUri: Uri,
    title: String,
    edit: (PDDocument) -> Unit
): Result<EditedPdf> = runCatching {
    PDFBoxResourceLoader.init(context.applicationContext)

    val outputDirectory = File(context.cacheDir, EDITED_PDF_DIRECTORY).apply {
        mkdirs()
    }
    val safeStem = title.substringBeforeLast(".")
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-', '.', '_')
        .take(48)
        .ifBlank { "carrot-pdf" }
    val outputFile = File(
        outputDirectory,
        "$safeStem-edited-${System.currentTimeMillis()}.pdf"
    )

    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        PDDocument.load(input).use { document ->
            edit(document)

            FileOutputStream(outputFile).use { output ->
                document.save(output)
            }

            EditedPdf(
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                ),
                title = title.ensurePdfTitle(),
                pageCount = document.numberOfPages
            )
        }
    } ?: error("Could not open source PDF.")
}

private fun PDDocument.appendImagePage(bitmap: Bitmap) {
    val landscape = bitmap.width > bitmap.height
    val pageRectangle = if (landscape) {
        PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width)
    } else {
        PDRectangle.A4
    }
    val page = PDPage(pageRectangle)
    addPage(page)

    val image = LosslessFactory.createFromImage(this, bitmap)
    val scale = min(
        pageRectangle.width / bitmap.width.toFloat(),
        pageRectangle.height / bitmap.height.toFloat()
    )
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    val x = (pageRectangle.width - width) / 2f
    val y = (pageRectangle.height - height) / 2f

    PDPageContentStream(this, page).use { content ->
        content.drawImage(image, x, y, width, height)
    }
}

private fun String.ensurePdfTitle(): String {
    return if (endsWith(".pdf", ignoreCase = true)) this else "$this.pdf"
}

private const val EDITED_PDF_DIRECTORY = "generated_pdfs"
