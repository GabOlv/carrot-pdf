package com.example.carrotpdf.ui

import android.content.Intent
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.ui.components.AppTopBar
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.components.PdfReaderControls
import com.example.carrotpdf.ui.components.PdfTabStrip
import com.example.carrotpdf.ui.design.CarrotColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CarrotPdfApp() {
    val context = LocalContext.current

    val tabs = remember { mutableStateListOf<PdfTab>() }
    var activeTabId by remember { mutableStateOf<String?>(null) }

    var isLoadingDocument by remember { mutableStateOf(false) }
    var scrollTargetPage by remember { mutableStateOf<Int?>(null) }

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
                    // Some providers may not allow persistable permission.
                }

                val newTab = PdfTab(
                    uri = uri,
                    title = getPdfTitle(context, uri)
                )

                tabs.add(newTab)
                activeTabId = newTab.id
                scrollTargetPage = null
            }
        }
    )

    LaunchedEffect(activeTabId) {
        val tab = activeTab ?: return@LaunchedEffect

        if (tab.pageCount == 0) {
            isLoadingDocument = true

            val pageCount = withContext(Dispatchers.IO) {
                getPdfPageCount(context, tab.uri)
            }

            isLoadingDocument = false

            if (pageCount > 0) {
                updateActiveTab(tabs, tab.id) {
                    it.copy(pageCount = pageCount)
                }
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
                    scrollTargetPage = null
                },
                onCloseTab = { id ->
                    val indexToRemove = tabs.indexOfFirst { it.id == id }

                    if (indexToRemove >= 0) {
                        tabs.removeAt(indexToRemove)

                        if (activeTabId == id) {
                            activeTabId = tabs.getOrNull(indexToRemove)?.id
                                ?: tabs.getOrNull(indexToRemove - 1)?.id
                        }

                        scrollTargetPage = null
                    }
                },
                onOpenPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                }
            )

            PdfReaderControls(
                activeTab = activeTab,
                onPreviousPage = {
                    val tab = activeTab ?: return@PdfReaderControls
                    val targetPage = (tab.currentPageIndex - 1).coerceAtLeast(0)

                    updateActiveTab(tabs, tab.id) {
                        it.copy(currentPageIndex = targetPage)
                    }

                    scrollTargetPage = targetPage
                },
                onNextPage = {
                    val tab = activeTab ?: return@PdfReaderControls
                    val maxPageIndex = (tab.pageCount - 1).coerceAtLeast(0)
                    val targetPage = (tab.currentPageIndex + 1).coerceAtMost(maxPageIndex)

                    updateActiveTab(tabs, tab.id) {
                        it.copy(currentPageIndex = targetPage)
                    }

                    scrollTargetPage = targetPage
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
                isLoadingDocument = isLoadingDocument,
                scrollTargetPage = scrollTargetPage,
                onOpenPdf = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                onCurrentPageChange = { pageIndex ->
                    updateActiveTab(tabs, activeTabId) { tab ->
                        if (tab.currentPageIndex == pageIndex) {
                            tab
                        } else {
                            tab.copy(currentPageIndex = pageIndex)
                        }
                    }
                },
                onScrollTargetConsumed = {
                    scrollTargetPage = null
                }
            )
        }
    }
}

@Composable
private fun PdfContentArea(
    activeTab: PdfTab?,
    isLoadingDocument: Boolean,
    scrollTargetPage: Int?,
    onOpenPdf: () -> Unit,
    onCurrentPageChange: (Int) -> Unit,
    onScrollTargetConsumed: () -> Unit
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

            isLoadingDocument || activeTab.pageCount == 0 -> {
                Text(
                    text = "Loading PDF...",
                    color = CarrotColors.TextSecondary
                )
            }

            else -> {
                ContinuousPdfViewer(
                    tabId = activeTab.id,
                    uri = activeTab.uri,
                    pageCount = activeTab.pageCount,
                    currentPageIndex = activeTab.currentPageIndex,
                    zoom = activeTab.zoom,
                    scrollTargetPage = scrollTargetPage,
                    onCurrentPageChange = onCurrentPageChange,
                    onScrollTargetConsumed = onScrollTargetConsumed
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