package com.example.carrotpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.carrotpdf.ui.CarrotPdfApp
import com.example.carrotpdf.ui.theme.CarrotpdfTheme

class MainActivity : ComponentActivity() {
    private var externalPdfUri by mutableStateOf<Uri?>(null)
    private var externalImageUris by mutableStateOf<List<Uri>>(emptyList())
    private var externalOpenRequestId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            CarrotpdfTheme {
                CarrotPdfApp(
                    externalPdfUri = externalPdfUri,
                    externalImageUris = externalImageUris,
                    externalOpenRequestId = externalOpenRequestId
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val request = extractExternalOpenRequest(intent)

        externalPdfUri = request.pdfUri
        externalImageUris = request.imageUris

        if (request.pdfUri != null || request.imageUris.isNotEmpty()) {
            externalOpenRequestId += 1
            persistReadPermissionsIfPossible(intent, request.allUris)
        }
    }

    private fun extractExternalOpenRequest(intent: Intent?): ExternalOpenRequest {
        if (intent == null) {
            return ExternalOpenRequest()
        }

        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return ExternalOpenRequest()
                val type = intent.type ?: contentResolver.getType(uri).orEmpty()

                when {
                    isPdfUri(uri, type) -> ExternalOpenRequest(pdfUri = uri)
                    isImageUri(uri, type) -> ExternalOpenRequest(imageUris = listOf(uri))
                    else -> ExternalOpenRequest()
                }
            }

            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?: return ExternalOpenRequest()
                val type = intent.type ?: contentResolver.getType(uri).orEmpty()

                when {
                    isPdfUri(uri, type) -> ExternalOpenRequest(pdfUri = uri)
                    isImageUri(uri, type) -> ExternalOpenRequest(imageUris = listOf(uri))
                    else -> ExternalOpenRequest()
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.filter { uri ->
                        val type = contentResolver.getType(uri).orEmpty()
                        isImageUri(uri, type)
                    }
                    .orEmpty()

                if (uris.isNotEmpty()) {
                    ExternalOpenRequest(imageUris = uris)
                } else {
                    ExternalOpenRequest()
                }
            }

            else -> ExternalOpenRequest()
        }
    }

    private fun isPdfUri(uri: Uri, type: String): Boolean {
        return type == "application/pdf" ||
            uri.toString().endsWith(".pdf", ignoreCase = true)
    }

    private fun isImageUri(uri: Uri, type: String): Boolean {
        val value = uri.toString()
        return type.startsWith("image/") ||
            value.endsWith(".jpg", ignoreCase = true) ||
            value.endsWith(".jpeg", ignoreCase = true) ||
            value.endsWith(".png", ignoreCase = true) ||
            value.endsWith(".webp", ignoreCase = true) ||
            value.endsWith(".gif", ignoreCase = true) ||
            value.endsWith(".bmp", ignoreCase = true)
    }

    private fun persistReadPermissionsIfPossible(intent: Intent?, uris: List<Uri>) {
        if (intent == null || uris.isEmpty()) {
            return
        }

        val hasReadPermission = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0

        if (!hasReadPermission) {
            return
        }

        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers grant temporary access only.
            }
        }
    }

    private data class ExternalOpenRequest(
        val pdfUri: Uri? = null,
        val imageUris: List<Uri> = emptyList()
    ) {
        val allUris: List<Uri>
            get() = listOfNotNull(pdfUri) + imageUris
    }
}
