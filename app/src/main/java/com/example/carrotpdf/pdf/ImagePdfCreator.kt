package com.example.carrotpdf.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private const val A4_PORTRAIT_WIDTH = 595
private const val A4_PORTRAIT_HEIGHT = 842
private const val MAX_DECODED_IMAGE_SIDE = 2800
private const val GENERATED_PDF_DIRECTORY = "generated_pdfs"

data class GeneratedImagePdf(
    val uri: Uri,
    val title: String
)

fun createPdfFromImages(
    context: Context,
    imageUris: List<Uri>
): Result<GeneratedImagePdf> = runCatching {
    require(imageUris.isNotEmpty()) { "No images selected." }

    val outputDirectory = File(context.cacheDir, GENERATED_PDF_DIRECTORY).apply {
        mkdirs()
    }
    cleanupOldGeneratedPdfs(outputDirectory)

    val title = buildGeneratedPdfTitle(imageUris.size)
    val outputFile = File(outputDirectory, title)
    val document = PdfDocument()

    try {
        imageUris.forEachIndexed { index, imageUri ->
            val bitmap = decodeImageForPdf(context, imageUri)
            try {
                val isLandscape = bitmap.width > bitmap.height
                val pageWidth = if (isLandscape) A4_PORTRAIT_HEIGHT else A4_PORTRAIT_WIDTH
                val pageHeight = if (isLandscape) A4_PORTRAIT_WIDTH else A4_PORTRAIT_HEIGHT
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    index + 1
                ).create()
                val page = document.startPage(pageInfo)

                val target = calculateFitCenterRect(
                    imageWidth = bitmap.width.toFloat(),
                    imageHeight = bitmap.height.toFloat(),
                    pageWidth = pageWidth.toFloat(),
                    pageHeight = pageHeight.toFloat()
                )

                page.canvas.drawBitmap(bitmap, null, target, null)
                document.finishPage(page)
            } finally {
                bitmap.recycle()
            }
        }

        FileOutputStream(outputFile).use { output ->
            document.writeTo(output)
        }
    } finally {
        document.close()
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )

    GeneratedImagePdf(
        uri = uri,
        title = title
    )
}

private fun decodeImageForPdf(
    context: Context,
    uri: Uri
): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeImageForPdfModern(context, uri)
    } else {
        decodeImageForPdfLegacy(context, uri)
    }
}

private fun decodeImageForPdfModern(
    context: Context,
    uri: Uri
): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)

    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width.coerceAtLeast(1)
        val height = info.size.height.coerceAtLeast(1)
        val scale = min(
            MAX_DECODED_IMAGE_SIDE.toFloat() / width.toFloat(),
            MAX_DECODED_IMAGE_SIDE.toFloat() / height.toFloat()
        )

        if (scale < 1f) {
            decoder.setTargetSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1)
            )
        }
    }
}

private fun decodeImageForPdfLegacy(
    context: Context,
    uri: Uri
): Bitmap {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, boundsOptions)
    }

    val sampleSize = calculateSampleSize(
        width = boundsOptions.outWidth.coerceAtLeast(1),
        height = boundsOptions.outHeight.coerceAtLeast(1),
        maxSide = MAX_DECODED_IMAGE_SIDE
    )
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }

    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
    } ?: error("Could not decode image.")
}

private fun calculateSampleSize(
    width: Int,
    height: Int,
    maxSide: Int
): Int {
    var sampleSize = 1
    var sampledWidth = width
    var sampledHeight = height

    while (sampledWidth > maxSide || sampledHeight > maxSide) {
        sampleSize *= 2
        sampledWidth /= 2
        sampledHeight /= 2
    }

    return sampleSize.coerceAtLeast(1)
}

private fun calculateFitCenterRect(
    imageWidth: Float,
    imageHeight: Float,
    pageWidth: Float,
    pageHeight: Float
): RectF {
    val scale = min(pageWidth / imageWidth, pageHeight / imageHeight)
    val drawWidth = imageWidth * scale
    val drawHeight = imageHeight * scale
    val left = (pageWidth - drawWidth) / 2f
    val top = (pageHeight - drawHeight) / 2f

    return RectF(
        left,
        top,
        left + drawWidth,
        top + drawHeight
    )
}

private fun buildGeneratedPdfTitle(imageCount: Int): String {
    val suffix = System.currentTimeMillis()
    return if (imageCount == 1) {
        "imagem-$suffix.pdf"
    } else {
        "imagens-$suffix.pdf"
    }
}

private fun cleanupOldGeneratedPdfs(directory: File) {
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
