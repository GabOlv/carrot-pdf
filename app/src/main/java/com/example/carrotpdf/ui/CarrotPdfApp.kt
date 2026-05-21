package com.example.carrotpdf.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.components.PdfReaderControls
import com.example.carrotpdf.ui.components.PdfTabStrip
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CarrotPdfApp() {
    val systemDarkTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }

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

    val tabs = remember { mutableStateListOf<PdfTab>() }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var isFullscreenReader by remember { mutableStateOf(false) }
    var isSettingsModalOpen by remember { mutableStateOf(false) }

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
                isFullscreenReader = false
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CarrotColors.Background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isFullscreenReader) {
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
                }

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
                    },
                    onZoomCommitted = { zoom ->
                        updateActiveTab(tabs, activeTabId) { tab ->
                            if (tab.zoom == zoom) {
                                tab
                            } else {
                                tab.copy(zoom = zoom)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                if (!isFullscreenReader) {
                    PdfReaderControls(
                        activeTab = activeTab,
                        viewerState = activeViewerState,
                        onBookmarkClick = {},
                        onExpandClick = {
                            isFullscreenReader = true
                        },
                        onEditClick = {},
                        onSearchClick = {},
                        onConfigClick = {
                            isSettingsModalOpen = true
                        }
                    )
                }
            }

            if (isFullscreenReader) {
                Text(
                    text = "Exit",
                    color = CarrotColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp)
                        .background(
                            color = CarrotColors.Surface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            isFullscreenReader = false
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (isSettingsModalOpen) {
                ReaderSettingsModal(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onOpenPdf = {
                        isSettingsModalOpen = false
                        openPdf()
                    },
                    onDismiss = {
                        isSettingsModalOpen = false
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
    onCurrentPageChange: (Int) -> Unit,
    onZoomCommitted: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
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
                    onCurrentPageChange = onCurrentPageChange,
                    onZoomCommitted = onZoomCommitted
                )

                PdfPageChip(
                    currentPage = viewerState.currentPageIndex + 1,
                    pageCount = viewerState.pageCount.coerceAtLeast(1),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 18.dp)
                )

                ReaderZoomBubble(
                    zoom = viewerState.zoom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun PdfPageChip(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentPage, pageCount) {
        isVisible = true
        delay(PAGE_CHIP_VISIBLE_MS)
        isVisible = false
    }

    val chipAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.82f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "page-chip-alpha"
    )

    if (chipAlpha > 0.01f) {
        Text(
            text = "$currentPage / $pageCount",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
                .alpha(chipAlpha)
                .background(
                    color = CarrotColors.Surface,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ReaderZoomBubble(
    zoom: Float,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(zoom) {
        isVisible = true
        delay(ZOOM_CHIP_VISIBLE_MS)
        isVisible = false
    }

    val chipAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.82f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "zoom-chip-alpha"
    )

    if (chipAlpha > 0.01f) {
        Text(
            text = "${(zoom * 100).toInt()}%",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier
                .alpha(chipAlpha)
                .background(
                    color = CarrotColors.Surface,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReaderSettingsModal(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Carrot PDF",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column {
                SettingsModalItem(
                    text = "Open PDF",
                    onClick = onOpenPdf
                )
                SettingsModalItem(
                    text = "Recent Items",
                    onClick = {}
                )
                SettingsModalItem(
                    text = "Bookmarked",
                    onClick = {}
                )
                SettingsModalItem(
                    text = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                    onClick = onToggleTheme
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Close",
                    color = CarrotColors.Accent
                )
            }
        },
        containerColor = CarrotColors.Surface
    )
}

@Composable
private fun SettingsModalItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = CarrotColors.TextPrimary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
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

private const val PAGE_CHIP_VISIBLE_MS = 1200L
private const val ZOOM_CHIP_VISIBLE_MS = 1200L
