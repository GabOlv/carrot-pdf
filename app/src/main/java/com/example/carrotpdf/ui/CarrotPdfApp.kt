package com.example.carrotpdf.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.renderPdfPage
import com.example.carrotpdf.ui.components.AppTopBar
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.components.PdfReaderControls
import com.example.carrotpdf.ui.components.PdfTabStrip
import com.example.carrotpdf.ui.components.ZoomablePdfPage
import com.example.carrotpdf.ui.design.CarrotColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CarrotPdfApp() {
    val context = LocalContext.current

    val tabs = remember { mutableStateListOf<PdfTab>() }
    var activeTabId by remember { mutableStateOf<String?>(null) }

    var renderedPage by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some providers may not allow persistable permission. Safe to ignore for now.
                }

                val newTab = PdfTab(
                    uri = uri,
                    title = getPdfTitle(context, uri)
                )

                tabs.add(newTab)
                activeTabId = newTab.id
            }
        }
    )

    LaunchedEffect(activeTabId, activeTab?.currentPageIndex) {
        renderedPage = null

        val tab = activeTab ?: return@LaunchedEffect

        isRendering = true

        val result = withContext(Dispatchers.IO) {
            renderPdfPage(
                context = context,
                uri = tab.uri,
                pageIndex = tab.currentPageIndex
            )
        }

        renderedPage = result?.bitmap
        isRendering = false

        if (result != null && tab.pageCount != result.pageCount) {
            updateActiveTab(tabs, tab.id) {
                it.copy(pageCount = result.pageCount)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CarrotColors.Background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppTopBar(
                onOpenPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                }
            )

            PdfTabStrip(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = { id ->
                    activeTabId = id
                },
                onCloseTab = { id ->
                    val indexToRemove = tabs.indexOfFirst { it.id == id }

                    if (indexToRemove >= 0) {
                        tabs.removeAt(indexToRemove)

                        if (activeTabId == id) {
                            activeTabId = tabs.getOrNull(indexToRemove)?.id
                                ?: tabs.getOrNull(indexToRemove - 1)?.id
                        }
                    }
                },
                onOpenPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                }
            )

            PdfReaderControls(
                activeTab = activeTab,
                onPreviousPage = {
                    updateActiveTab(tabs, activeTabId) { tab ->
                        tab.copy(
                            currentPageIndex = (tab.currentPageIndex - 1).coerceAtLeast(0)
                        )
                    }
                },
                onNextPage = {
                    updateActiveTab(tabs, activeTabId) { tab ->
                        val maxPageIndex = (tab.pageCount - 1).coerceAtLeast(0)

                        tab.copy(
                            currentPageIndex = (tab.currentPageIndex + 1).coerceAtMost(maxPageIndex)
                        )
                    }
                },
                onZoomOut = {
                    updateActiveTab(tabs, activeTabId) { tab ->
                        tab.copy(zoom = (tab.zoom - 0.1f).coerceAtLeast(0.7f))
                    }
                },
                onZoomIn = {
                    updateActiveTab(tabs, activeTabId) { tab ->
                        tab.copy(zoom = (tab.zoom + 0.1f).coerceAtMost(3f))
                    }
                },
                onResetZoom = {
                    updateActiveTab(tabs, activeTabId) { tab ->
                        tab.copy(zoom = 1f)
                    }
                }
            )

            PdfContentArea(
                activeTab = activeTab,
                renderedPage = renderedPage,
                isRendering = isRendering,
                onOpenPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                onZoomChange = { newZoom ->
                    updateActiveTab(tabs, activeTabId) { tab ->
                        tab.copy(zoom = newZoom)
                    }
                }
            )
        }
    }
}

@Composable
private fun PdfContentArea(
    activeTab: PdfTab?,
    renderedPage: Bitmap?,
    isRendering: Boolean,
    onOpenPdf: () -> Unit,
    onZoomChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarrotColors.PdfCanvas),
        contentAlignment = Alignment.Center
    ) {
        when {
            activeTab == null -> {
                EmptyState(onOpenPdf = onOpenPdf)
            }

            isRendering || renderedPage == null -> {
                Text(
                    text = "Rendering...",
                    color = CarrotColors.TextSecondary
                )
            }

            else -> {
                ZoomablePdfPage(
                    bitmap = renderedPage,
                    zoom = activeTab.zoom,
                    onZoomChange = onZoomChange
                )
            }
        }
    }
}

private fun updateActiveTab(
    tabs: MutableList<PdfTab>,
    activeTabId: String?,
    transform: (PdfTab) -> PdfTab
) {
    val index = tabs.indexOfFirst { it.id == activeTabId }

    if (index >= 0) {
        tabs[index] = transform(tabs[index])
    }
}

private fun getPdfTitle(
    context: android.content.Context,
    uri: Uri
): String {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )

    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (displayNameIndex >= 0) {
                val displayName = it.getString(displayNameIndex)

                if (!displayName.isNullOrBlank()) {
                    return displayName
                }
            }
        }
    }

    return uri.lastPathSegment
        ?.substringAfterLast("/")
        ?.substringAfterLast(":")
        ?.takeIf { it.isNotBlank() }
        ?: "Document.pdf"
}