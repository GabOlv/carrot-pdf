package com.example.carrotpdf.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.printPdf
import com.example.carrotpdf.pdf.searchPdfText
import com.example.carrotpdf.pdf.sharePdf
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CarrotPdfApp() {
    CarrotDesignTheme(darkTheme = true) {
        CarrotPdfContent()
    }
}

@Composable
private fun CarrotPdfContent() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val tabs = remember { mutableStateListOf<PdfTab>() }
    val searchResults = remember { mutableStateListOf<PdfSearchResult>() }

    var activeTabId by remember { mutableStateOf<String?>(null) }
    var isChromeVisible by remember { mutableStateOf(true) }
    var isTabSwitcherOpen by remember { mutableStateOf(false) }
    var isOverflowOpen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var activeSearchResultIndex by remember { mutableIntStateOf(-1) }
    var isLoadingDocument by remember { mutableStateOf(false) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeViewerState = rememberActiveViewerState(activeTab)

    ImmersiveSystemBars(isChromeVisible)

    fun hideChromeForReading() {
        if (!isSearchVisible) {
            isChromeVisible = false
        }
    }

    fun closeSearch() {
        isSearchVisible = false
        searchQuery = ""
        searchResults.clear()
        activeSearchResultIndex = -1
        isSearching = false
    }

    fun openTab(uri: Uri, title: String) {
        val existingTab = tabs.firstOrNull { it.uri == uri }

        if (existingTab != null) {
            activeTabId = existingTab.id
        } else {
            val tab = PdfTab(uri = uri, title = title)
            tabs.add(tab)
            activeTabId = tab.id
        }

        closeSearch()
        isChromeVisible = true
    }

    fun closeTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }

        if (index < 0) {
            return
        }

        tabs.removeAt(index)

        if (activeTabId == tabId) {
            activeTabId = tabs.getOrNull(index)?.id
                ?: tabs.getOrNull(index - 1)?.id
        }

        if (tabs.isEmpty()) {
            closeSearch()
            isChromeVisible = true
        }
    }

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
                    // Some providers grant only temporary access.
                }

                openTab(
                    uri = uri,
                    title = getPdfTitle(context, uri)
                )
            }
        }
    )
    val openPdf = {
        pdfPicker.launch(arrayOf("application/pdf"))
    }

    LaunchedEffect(activeTabId) {
        closeSearch()

        val tab = activeTab ?: return@LaunchedEffect

        if (tab.pageCount == 0) {
            isLoadingDocument = true

            val pageCount = withContext(Dispatchers.IO) {
                getPdfPageCount(context, tab.uri)
            }

            isLoadingDocument = false

            if (pageCount > 0) {
                updateActiveTab(tabs, tab.id) { currentTab ->
                    currentTab.copy(pageCount = pageCount)
                }
            }
        }
    }

    LaunchedEffect(isSearchVisible, searchQuery, activeTab?.id) {
        val tab = activeTab
        val query = searchQuery.trim()

        if (!isSearchVisible || tab == null || query.isBlank()) {
            searchResults.clear()
            activeSearchResultIndex = -1
            isSearching = false
            return@LaunchedEffect
        }

        delay(SEARCH_DEBOUNCE_MS)
        isSearching = true

        val results = withContext(Dispatchers.IO) {
            searchPdfText(
                context = context.applicationContext,
                uri = tab.uri,
                query = query
            )
        }

        searchResults.clear()
        searchResults.addAll(results)
        activeSearchResultIndex = if (results.isEmpty()) -1 else 0
        isSearching = false

        results.firstOrNull()?.let { result ->
            activeViewerState?.requestScrollToPage(result.pageIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CarrotColors.PdfCanvas
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CarrotColors.PdfCanvas)
        ) {
            ReaderStage(
                activeTab = activeTab,
                viewerState = activeViewerState,
                isLoadingDocument = isLoadingDocument,
                searchResults = searchResults,
                activeSearchResultIndex = activeSearchResultIndex,
                onOpenPdf = openPdf,
                onToggleChrome = {
                    isChromeVisible = !isChromeVisible
                    if (!isChromeVisible) {
                        closeSearch()
                    }
                },
                onRevealChrome = {
                    isChromeVisible = true
                },
                onUserInteraction = ::hideChromeForReading,
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
                }
            )

            AnimatedVisibility(
                visible = isChromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    title = activeTab?.title ?: "Carrot PDF",
                    tabCount = tabs.size,
                    onBack = {
                        activity?.finish()
                    },
                    onSearch = {
                        if (activeTab != null) {
                            isSearchVisible = true
                            isChromeVisible = true
                        }
                    },
                    onTabs = {
                        isTabSwitcherOpen = true
                    },
                    onMenu = {
                        isOverflowOpen = true
                    }
                )
            }

            AnimatedVisibility(
                visible = isChromeVisible && isSearchVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TOP_BAR_HEIGHT, start = 12.dp, end = 12.dp)
            ) {
                ReaderSearchOverlay(
                    query = searchQuery,
                    isSearching = isSearching,
                    resultCount = searchResults.size,
                    activeResultIndex = activeSearchResultIndex,
                    onQueryChange = { query ->
                        searchQuery = query
                    },
                    onClose = ::closeSearch,
                    onPrevious = {
                        if (searchResults.isNotEmpty()) {
                            activeSearchResultIndex = if (activeSearchResultIndex <= 0) {
                                searchResults.lastIndex
                            } else {
                                activeSearchResultIndex - 1
                            }

                            searchResults.getOrNull(activeSearchResultIndex)?.let { result ->
                                activeViewerState?.requestScrollToPage(result.pageIndex)
                            }
                        }
                    },
                    onNext = {
                        if (searchResults.isNotEmpty()) {
                            activeSearchResultIndex = if (activeSearchResultIndex >= searchResults.lastIndex) {
                                0
                            } else {
                                activeSearchResultIndex + 1
                            }

                            searchResults.getOrNull(activeSearchResultIndex)?.let { result ->
                                activeViewerState?.requestScrollToPage(result.pageIndex)
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = isChromeVisible && activeTab != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp)
            ) {
                FloatingAnnotationButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Annotations will come later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            if (isTabSwitcherOpen) {
                TabSwitcherDialog(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onSelect = { tabId ->
                        activeTabId = tabId
                        isTabSwitcherOpen = false
                        isChromeVisible = true
                    },
                    onCloseTab = { tabId ->
                        closeTab(tabId)
                    },
                    onOpenPdf = {
                        isTabSwitcherOpen = false
                        openPdf()
                    },
                    onDismiss = {
                        isTabSwitcherOpen = false
                    }
                )
            }

            if (isOverflowOpen) {
                ReaderMenuDialog(
                    hasDocument = activeTab != null,
                    onOpenPdf = {
                        isOverflowOpen = false
                        openPdf()
                    },
                    onSharePdf = {
                        val tab = activeTab

                        if (tab != null) {
                            sharePdf(context, tab.uri, tab.title)
                        }
                    },
                    onDownloadPdf = {
                        val tab = activeTab

                        if (tab != null) {
                            val success = downloadPdf(context, tab.uri, tab.title)
                            Toast.makeText(
                                context,
                                if (success) "PDF downloaded" else "Could not download PDF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onPrintPdf = {
                        val tab = activeTab

                        if (tab != null) {
                            printPdf(context, tab.uri, tab.title)
                        }
                    },
                    onDismiss = {
                        isOverflowOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReaderStage(
    activeTab: PdfTab?,
    viewerState: PdfViewerState?,
    isLoadingDocument: Boolean,
    searchResults: List<PdfSearchResult>,
    activeSearchResultIndex: Int,
    onOpenPdf: () -> Unit,
    onToggleChrome: () -> Unit,
    onRevealChrome: () -> Unit,
    onUserInteraction: () -> Unit,
    onCurrentPageChange: (Int) -> Unit,
    onZoomCommitted: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CarrotColors.PdfCanvas)
            .pointerInput(activeTab?.id) {
                detectTapGestures(
                    onTap = {
                        onToggleChrome()
                    }
                )
            },
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
                    onZoomCommitted = onZoomCommitted,
                    searchResults = searchResults,
                    activeSearchResultIndex = activeSearchResultIndex,
                    onUserInteraction = onUserInteraction
                )
            }
        }

        EdgeRevealZones(
            onRevealChrome = onRevealChrome
        )
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    tabCount: Int,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF20B0D10),
                        Color(0xDD10141A)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonCanvas(
                contentDescription = "Back",
                onClick = onBack
            ) {
                drawLine(
                    color = Color.White,
                    start = Offset(17.dp.toPx(), 5.dp.toPx()),
                    end = Offset(8.dp.toPx(), 12.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(8.dp.toPx(), 12.dp.toPx()),
                    end = Offset(17.dp.toPx(), 19.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            IconButtonCanvas(
                contentDescription = "Search",
                onClick = onSearch
            ) {
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawLine(
                    color = Color.White,
                    start = Offset(15.5.dp.toPx(), 15.5.dp.toPx()),
                    end = Offset(21.dp.toPx(), 21.dp.toPx()),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            IconButtonCanvas(
                contentDescription = "Tabs",
                onClick = onTabs
            ) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(5.dp.toPx(), 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(13.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(8.dp.toPx(), 3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(13.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = Color(0xFF33383F),
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.coerceAtMost(99).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButtonCanvas(
                contentDescription = "Menu",
                onClick = onMenu
            ) {
                listOf(6.dp, 12.dp, 18.dp).forEach { y ->
                    drawCircle(
                        color = Color.White,
                        radius = 1.8.dp.toPx(),
                        center = Offset(12.dp.toPx(), y.toPx())
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .height(3.dp)
                .width(96.dp)
                .background(CarrotColors.Accent)
        )
    }
}

@Composable
private fun ReaderSearchOverlay(
    query: String,
    isSearching: Boolean,
    resultCount: Int,
    activeResultIndex: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xF20F1217),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onClose) {
            Text(
                text = "<",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = {
                Text("Search")
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = when {
                isSearching -> "..."
                resultCount == 0 -> "0"
                else -> "${activeResultIndex + 1} of $resultCount"
            },
            color = CarrotColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        TextButton(
            enabled = resultCount > 0,
            onClick = onPrevious
        ) {
            Text(
                text = "<",
                color = if (resultCount > 0) Color.White else CarrotColors.TextMuted,
                style = MaterialTheme.typography.titleLarge
            )
        }

        TextButton(
            enabled = resultCount > 0,
            onClick = onNext
        ) {
            Text(
                text = ">",
                color = if (resultCount > 0) Color.White else CarrotColors.TextMuted,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun TabSwitcherDialog(
    tabs: List<PdfTab>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Open PDFs",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tabs.isEmpty()) {
                    Text(
                        text = "No PDFs are open.",
                        color = CarrotColors.TextMuted
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(
                        items = tabs,
                        key = { tab -> tab.id }
                    ) { tab ->
                        TabSwitcherRow(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onSelect = {
                                onSelect(tab.id)
                            },
                            onClose = {
                                onCloseTab(tab.id)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenPdf) {
                Text(
                    text = "Open PDF",
                    color = CarrotColors.Accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = CarrotColors.TextSecondary
                )
            }
        },
        containerColor = CarrotColors.Surface
    )
}

@Composable
private fun TabSwitcherRow(
    tab: PdfTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CarrotColors.AccentSoft else CarrotColors.SurfaceAlt
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tab.title,
                color = if (isActive) CarrotColors.Accent else CarrotColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onClose) {
                Text(
                    text = "Close",
                    color = CarrotColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun ReaderMenuDialog(
    hasDocument: Boolean,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PDF options",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuAction("Open PDF", onOpenPdf)
                MenuAction("Share", onSharePdf, enabled = hasDocument)
                MenuAction("Download", onDownloadPdf, enabled = hasDocument)
                MenuAction("Print", onPrintPdf, enabled = hasDocument)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
private fun MenuAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Text(
        text = text,
        color = if (enabled) CarrotColors.TextPrimary else CarrotColors.TextMuted,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CarrotColors.SurfaceAlt,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(14.dp)
    )
}

@Composable
private fun FloatingAnnotationButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF7A1A),
                        Color(0xFFFF5A10)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(34.dp)) {
            drawLine(
                color = Color.White,
                start = Offset(9.dp.toPx(), 24.dp.toPx()),
                end = Offset(24.dp.toPx(), 9.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(22.dp.toPx(), 7.dp.toPx()),
                end = Offset(27.dp.toPx(), 12.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF72D86A),
                start = Offset(24.dp.toPx(), 7.dp.toPx()),
                end = Offset(29.dp.toPx(), 3.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF72D86A),
                start = Offset(27.dp.toPx(), 9.dp.toPx()),
                end = Offset(31.dp.toPx(), 7.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun IconButtonCanvas(
    contentDescription: String,
    onClick: () -> Unit,
    drawIcon: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(24.dp),
            contentDescription = contentDescription,
            onDraw = drawIcon
        )
    }
}

@Composable
private fun BoxScope.EdgeRevealZones(
    onRevealChrome: () -> Unit
) {
    var topDrag by remember { mutableStateOf(0f) }
    var bottomDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .align(Alignment.TopCenter)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        topDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        topDrag += dragAmount

                        if (topDrag > EDGE_REVEAL_DISTANCE_PX) {
                            change.consume()
                            onRevealChrome()
                        }
                    },
                    onDragEnd = {
                        topDrag = 0f
                    },
                    onDragCancel = {
                        topDrag = 0f
                    }
                )
            }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .align(Alignment.BottomCenter)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        bottomDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        bottomDrag += dragAmount

                        if (bottomDrag < -EDGE_REVEAL_DISTANCE_PX) {
                            change.consume()
                            onRevealChrome()
                        }
                    },
                    onDragEnd = {
                        bottomDrag = 0f
                    },
                    onDragCancel = {
                        bottomDrag = 0f
                    }
                )
            }
    )
}

@Composable
private fun ImmersiveSystemBars(
    isChromeVisible: Boolean
) {
    val view = LocalView.current
    val activity = view.context.findActivity()

    DisposableEffect(activity, isChromeVisible) {
        val window = activity?.window

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = WindowInsetsControllerCompat(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (isChromeVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
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
    context: Context,
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

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private val TOP_BAR_HEIGHT = 76.dp
private const val SEARCH_DEBOUNCE_MS = 320L
private const val EDGE_REVEAL_DISTANCE_PX = 18f
