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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.ui.components.AppDrawer
import com.example.carrotpdf.ui.components.AppTopBar
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.components.PdfReaderControls
import com.example.carrotpdf.ui.components.PdfTabStrip
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CarrotPdfApp() {
    var isDarkTheme by remember { mutableStateOf(true) }

    CarrotDesignTheme(
        darkTheme = isDarkTheme
    ) {
        CarrotPdfContent(
            isDarkTheme = isDarkTheme,
            onToggleTheme = {
                isDarkTheme = !isDarkTheme
            }
        )
    }
}

@Composable
private fun CarrotPdfContent(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val tabs = remember { mutableStateListOf<PdfTab>() }
    var activeTabId by remember { mutableStateOf<String?>(null) }

    var isLoadingDocument by remember { mutableStateOf(false) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeViewerState = rememberActiveViewerState(activeTab)

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
            }
        }
    )

    val openPdf = {
        pdfPicker.launch(arrayOf("application/pdf"))
    }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onOpenPdf = {
                    coroutineScope.launch {
                        drawerState.close()
                    }

                    openPdf()
                }
            )
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CarrotColors.Background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AppTopBar(
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
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
                    onOpenPdf = openPdf
                )

                PdfReaderControls(
                    activeTab = activeTab,
                    viewerState = activeViewerState,
                    onZoomClick = {
                        if (activeViewerState != null) {
                            val nextZoom = activeViewerState.advanceZoomPreset()

                            updateActiveTab(tabs, activeTabId) { tab ->
                                tab.copy(zoom = nextZoom)
                            }
                        }
                    },
                    onLayoutClick = {
                        // Future: thumbnails, split view, page grid, or window view.
                    }
                )

                PdfContentArea(
                    activeTab = activeTab,
                    viewerState = activeViewerState,
                    isLoadingDocument = isLoadingDocument,
                    onOpenPdf = openPdf,
                    onCurrentPageChange = { pageIndex ->
                        updateActiveTab(tabs, activeTabId) { tab ->
                            if (tab.currentPageIndex == pageIndex) {
                                tab
                            } else {
                                tab.copy(currentPageIndex = pageIndex)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PdfContentArea(
    activeTab: PdfTab?,
    viewerState: PdfViewerState?,
    isLoadingDocument: Boolean,
    onOpenPdf: () -> Unit,
    onCurrentPageChange: (Int) -> Unit
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

            viewerState != null -> {
                ContinuousPdfViewer(
                    uri = activeTab.uri,
                    viewerState = viewerState,
                    onCurrentPageChange = onCurrentPageChange
                )
            }
        }
    }
}

@Composable
private fun rememberActiveViewerState(
    activeTab: PdfTab?
): PdfViewerState? {
    if (activeTab == null) {
        return null
    }

    return rememberPdfViewerState(
        documentId = activeTab.id,
        pageCount = activeTab.pageCount,
        initialPageIndex = activeTab.currentPageIndex,
        initialZoom = activeTab.zoom
    )
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
