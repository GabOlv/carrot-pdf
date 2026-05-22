package com.example.carrotpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.carrotpdf.ui.CarrotPdfApp
import com.example.carrotpdf.ui.theme.CarrotpdfTheme

class MainActivity : ComponentActivity() {
    private var externalPdfUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalPdfUri = extractPdfUri(intent)
        persistReadPermissionIfPossible(intent, externalPdfUri)

        setContent {
            CarrotpdfTheme {
                CarrotPdfApp(externalPdfUri = externalPdfUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalPdfUri = extractPdfUri(intent)
        persistReadPermissionIfPossible(intent, externalPdfUri)
    }

    private fun extractPdfUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) {
            return null
        }

        val uri = intent.data ?: return null
        val type = intent.type.orEmpty()

        return if (type == "application/pdf" || uri.toString().endsWith(".pdf", ignoreCase = true)) {
            uri
        } else {
            null
        }
    }

    private fun persistReadPermissionIfPossible(intent: Intent?, uri: Uri?) {
        if (intent == null || uri == null) {
            return
        }

        val hasReadPermission = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0

        if (!hasReadPermission) {
            return
        }

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
