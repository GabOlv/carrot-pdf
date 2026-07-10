package com.example.carrotpdf.pdf

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun createCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAMERA_IMAGE_DIRECTORY).apply {
        mkdirs()
    }
    cleanupOldCameraImages(directory)

    val imageFile = File(
        directory,
        "camera-${System.currentTimeMillis()}.jpg"
    ).apply {
        createNewFile()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private fun cleanupOldCameraImages(directory: File) {
    val now = System.currentTimeMillis()

    directory.listFiles()
        ?.filter { file ->
            file.isFile && now - file.lastModified() > CAMERA_IMAGE_MAX_AGE_MS
        }
        ?.forEach { file ->
            runCatching { file.delete() }
        }
}

private const val CAMERA_IMAGE_DIRECTORY = "camera_images"
private const val CAMERA_IMAGE_MAX_AGE_MS = 24L * 60L * 60L * 1000L
