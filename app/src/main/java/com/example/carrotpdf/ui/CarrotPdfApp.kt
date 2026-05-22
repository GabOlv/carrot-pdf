package com.example.carrotpdf.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.model.PdfTabPersistence
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.getPdfPageSizes
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
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun CarrotPdfApp(
    externalPdfUri: Uri? = null
) {
    CarrotDesignTheme(darkTheme = true) {
        CarrotPdfContent(externalPdfUri = externalPdfUri)
    }
}

@Composable
private fun CarrotPdfContent(
    externalPdfUri: Uri?
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
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
    var hasRestoredPersistedTabs by remember { mutableStateOf(false) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeViewerState = rememberActiveViewerState(activeTab)

    ImmersiveSystemBars(isChromeVisible)

    fun closeSearch() {
        isSearchVisible = false
        searchQuery = ""
        searchResults.clear()
        activeSearchResultIndex = -1
        isSearching = false
    }

    fun hideChromeForReading() {
        if (!isSearchVisible) {
            isChromeVisible = false
        }
    }

    fun persistTabsNow() {
        PdfTabPersistence.save(
            context = context.applicationContext,
            tabs = tabs.toList(),
            activeTabId = activeTabId
        )
    }

    fun openTab(uri: Uri, title: String) {
        val existingTab = tabs.firstOrNull { it.uri == uri }

        if (existingTab != null) {
            activeTabId = existingTab.id
            updateActiveTab(tabs, existingTab.id) { tab ->
                tab.copy(
                    currentPageIndex = 0,
                    zoom = 1f
                )
            }
        } else {
            val tab = PdfTab(uri = uri, title = title)
            tabs.add(tab)
            activeTabId = tab.id
        }

        closeSearch()
        isChromeVisible = true
        persistTabsNow()
    }

    fun openExternalPdf(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers grant temporary access only.
        }

        openTab(
            uri = uri,
            title = getPdfTitle(context, uri)
        )
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

        persistTabsNow()
    }

    fun moveTab(tabId: String, direction: Int) {
        val fromIndex = tabs.indexOfFirst { it.id == tabId }

        if (fromIndex < 0) {
            return
        }

        val toIndex = (fromIndex + direction).coerceIn(0, tabs.lastIndex)

        if (fromIndex == toIndex) {
            return
        }

        val tab = tabs.removeAt(fromIndex)
        tabs.add(toIndex, tab)
        persistTabsNow()
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

    BackHandler(enabled = isSearchVisible) {
        closeSearch()
        isChromeVisible = true
    }

    DisposableEffect(activity, tabs, activeTabId) {
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle

        if (lifecycle == null) {
            onDispose {
                persistTabsNow()
            }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    persistTabsNow()
                }
            }

            lifecycle.addObserver(observer)

            onDispose {
                persistTabsNow()
                lifecycle.removeObserver(observer)
            }
        }
    }

    LaunchedEffect(Unit) {
        val restoredTabs = withContext(Dispatchers.IO) {
            PdfTabPersistence.restore(context.applicationContext)
        }

        if (tabs.isEmpty() && restoredTabs.tabs.isNotEmpty()) {
            tabs.addAll(restoredTabs.tabs)
            activeTabId = restoredTabs.activeTabId
        }

        hasRestoredPersistedTabs = true
    }

    LaunchedEffect(externalPdfUri, hasRestoredPersistedTabs) {
        if (hasRestoredPersistedTabs && externalPdfUri != null) {
            openExternalPdf(externalPdfUri)
        }
    }

    LaunchedEffect(activeTabId) {
        closeSearch()

        val tab = activeTab ?: return@LaunchedEffect

        if (tab.pageCount == 0 || tab.pageSizes.isEmpty()) {
            isLoadingDocument = true

            val pageSizes = withContext(Dispatchers.IO) {
                getPdfPageSizes(context, tab.uri)
            }
            val pageCount = pageSizes.size.takeIf { it > 0 }
                ?: withContext(Dispatchers.IO) {
                    getPdfPageCount(context, tab.uri)
                }

            isLoadingDocument = false

            if (pageCount > 0) {
                updateActiveTab(tabs, tab.id) { currentTab ->
                    currentTab.copy(
                        pageCount = pageCount,
                        pageSizes = pageSizes
                    )
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
                pageSizes = activeTab?.pageSizes.orEmpty(),
                pageIndicatorContent = { currentPage, pageCount, isScrollInProgress, scrollProgress, onScrollToProgress ->
                    DrivePageIndicator(
                        currentPage = currentPage,
                        pageCount = pageCount,
                        isScrollInProgress = isScrollInProgress,
                        scrollProgress = scrollProgress,
                        onScrollToProgress = onScrollToProgress,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                },
                onOpenPdf = openPdf,
                onToggleChrome = {
                    if (!isSearchVisible) {
                        isChromeVisible = !isChromeVisible
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
                    persistTabsNow()
                },
                onZoomCommitted = { zoom ->
                    updateActiveTab(tabs, activeTabId) { tab ->
                        if (tab.zoom == zoom) {
                            tab
                        } else {
                            tab.copy(zoom = zoom)
                        }
                    }
                    persistTabsNow()
                }
            )

            AnimatedVisibility(
                visible = isChromeVisible && !isSearchVisible,
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
                visible = isSearchVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        start = 10.dp,
                        top = statusBarTop + 8.dp,
                        end = 10.dp,
                        bottom = 8.dp
                    )
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
                visible = (isChromeVisible || isSearchVisible) && activeTab != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 18.dp,
                        bottom = navigationBarBottom + 18.dp
                    )
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
                    onMoveTab = { tabId, direction ->
                        moveTab(tabId, direction)
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
                ReaderMenuPopup(
                    hasDocument = activeTab != null,
                    onOpenPdf = {
                        isOverflowOpen = false
                        openPdf()
                    },
                    onGoToPage = {
                        isOverflowOpen = false
                        Toast.makeText(
                            context,
                            "Go to page will come next.",
                            Toast.LENGTH_SHORT
                        ).show()
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
    pageSizes: List<PdfPageSize>,
    pageIndicatorContent: @Composable BoxScope.(
        currentPage: Int,
        pageCount: Int,
        isScrollInProgress: Boolean,
        scrollProgress: Float,
        onScrollToProgress: (Float) -> Unit
    ) -> Unit,
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
                    pageSizes = pageSizes,
                    onUserInteraction = onUserInteraction,
                    pageIndicatorContent = { currentPage, pageCount, isScrollInProgress, scrollProgress, onScrollToProgress ->
                        pageIndicatorContent(
                            currentPage,
                            pageCount,
                            isScrollInProgress,
                            scrollProgress,
                            onScrollToProgress
                        )
                    }
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
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT + statusBarTop)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF70B0D10),
                        Color(0xEE11151A)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = 14.dp,
                    top = statusBarTop,
                    end = 10.dp
                ),
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
                style = MaterialTheme.typography.titleMedium,
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
                drawBookTabsIcon()
            }

            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 16.dp)
                    .offset(x = (-10).dp, y = 8.dp)
                    .background(
                        color = Color(0xEE2B3037),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.coerceAtMost(99).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
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
                .width(86.dp)
                .background(CarrotColors.Accent)
        )
    }
}

@Composable
private fun BoxScope.DrivePageIndicator(
    currentPage: Int,
    pageCount: Int,
    isScrollInProgress: Boolean,
    scrollProgress: Float,
    onScrollToProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var thumbCenterY by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage, pageCount, isScrollInProgress, isDragging, scrollProgress) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
            return@LaunchedEffect
        }

        isVisible = true
        delay(PAGE_INDICATOR_VISIBLE_MS)
        isVisible = false
    }

    AnimatedVisibility(
        visible = isVisible || isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp)
                .padding(top = 24.dp, end = 10.dp, bottom = 24.dp)
        ) {
            val heightPx = with(density) { maxHeight.toPx() }

            val handleVisualWidth = 7.dp
            val handleVisualHeight = 44.dp
            val handleHitWidth = 34.dp
            val handleHalfHeightPx = with(density) { handleVisualHeight.toPx() / 2f }
            val travelHeightPx = (heightPx - (handleHalfHeightPx * 2f))
                .coerceAtLeast(1f)

            val targetCenterY = (
                    handleHalfHeightPx +
                            (travelHeightPx * scrollProgress.coerceIn(0f, 1f))
                    ).coerceIn(
                    handleHalfHeightPx,
                    heightPx - handleHalfHeightPx
                )

            LaunchedEffect(targetCenterY, isDragging) {
                if (!isDragging) {
                    thumbCenterY = targetCenterY
                }
            }

            fun scrollToThumbPosition(centerY: Float) {
                val clampedY = centerY.coerceIn(
                    handleHalfHeightPx,
                    heightPx - handleHalfHeightPx
                )

                thumbCenterY = clampedY

                val targetProgress = if (heightPx <= 0f) {
                    0f
                } else {
                    ((clampedY - handleHalfHeightPx) / travelHeightPx)
                        .coerceIn(0f, 1f)
                }

                onScrollToProgress(targetProgress)
            }

            // Gesture layer restricted to the current scrollbar thumb only.
            // This avoids jumping the document when the user taps elsewhere
            // along the page indicator's vertical space.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(handleHitWidth)
                    .fillMaxHeight()
                    .pointerInput(heightPx, thumbCenterY) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                val downY = down.position.y
                                val isOnThumb = downY in
                                    (thumbCenterY - handleHalfHeightPx)..(thumbCenterY + handleHalfHeightPx)

                                if (!isOnThumb) {
                                    continue
                                }

                                isDragging = true
                                down.consume()
                                val grabOffsetY = downY - thumbCenterY

                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)

                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            scrollToThumbPosition(change.position.y - grabOffsetY)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { change -> change.pressed })

                                isDragging = false
                            }
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        y = with(density) {
                            (thumbCenterY - handleHalfHeightPx).toDp()
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentPage / $pageCount",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            color = Color(0xDD1D2025),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(width = handleVisualWidth, height = handleVisualHeight)
                        .background(
                            color = CarrotColors.Accent,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        }
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
    val searchText = when {
        isSearching -> "Searching"
        query.isBlank() -> ""
        resultCount == 0 -> "0"
        else -> "${activeResultIndex + 1} / $resultCount"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT)
            .background(
                color = Color(0xF70B0D10)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonCanvas(
            contentDescription = "Close search",
            onClick = onClose
        ) {
            drawLine(
                color = Color.White,
                start = Offset(16.dp.toPx(), 6.dp.toPx()),
                end = Offset(8.dp.toPx(), 12.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(8.dp.toPx(), 12.dp.toPx()),
                end = Offset(16.dp.toPx(), 18.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .background(
                    color = Color(0xFF252A31),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(start = 16.dp, end = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                ),
                cursorBrush = SolidColor(CarrotColors.Accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text(
                            text = "Search in PDF",
                            color = Color.White.copy(alpha = 0.42f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    innerTextField()
                }
            )
        }

        Text(
            text = searchText,
            color = Color.White.copy(alpha = if (searchText.isBlank()) 0f else 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        IconButtonCanvas(
            contentDescription = "Previous result",
            onClick = {
                if (resultCount > 0) {
                    onPrevious()
                }
            }
        ) {
            val color = if (resultCount > 0) Color.White else Color.White.copy(alpha = 0.24f)
            drawLine(
                color = color,
                start = Offset(15.dp.toPx(), 7.dp.toPx()),
                end = Offset(9.dp.toPx(), 12.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(9.dp.toPx(), 12.dp.toPx()),
                end = Offset(15.dp.toPx(), 17.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        IconButtonCanvas(
            contentDescription = "Next result",
            onClick = {
                if (resultCount > 0) {
                    onNext()
                }
            }
        ) {
            val color = if (resultCount > 0) Color.White else Color.White.copy(alpha = 0.24f)
            drawLine(
                color = color,
                start = Offset(9.dp.toPx(), 7.dp.toPx()),
                end = Offset(15.dp.toPx(), 12.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(15.dp.toPx(), 12.dp.toPx()),
                end = Offset(9.dp.toPx(), 17.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
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
    onMoveTab: (String, Int) -> Unit,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var closeCandidate by remember { mutableStateOf<PdfTab?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetWidth = (maxWidth * 0.86f)
                .coerceIn(360.dp, 620.dp)

            Column(
                modifier = Modifier
                    .width(sheetWidth)
                    .padding(horizontal = 12.dp, vertical = 18.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .background(
                        color = Color(0xF31A1D22),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable { }
                    .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open tabs",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Edit",
                        color = CarrotColors.Accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tabs.isEmpty()) {
                    Text(
                        text = "No PDFs are open.",
                        color = CarrotColors.TextMuted,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
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
                                closeCandidate = tab
                            },
                            onMove = { direction ->
                                onMoveTab(tab.id, direction)
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(54.dp)
                        .border(
                            width = 1.dp,
                            color = CarrotColors.Accent.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = Color.White.copy(alpha = 0.03f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenPdf() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+",
                            color = CarrotColors.Accent,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Open new PDF",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    closeCandidate?.let { tab ->
        AlertDialog(
            onDismissRequest = {
                closeCandidate = null
            },
            title = {
                Text(
                    text = "Close tab?",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Close ${tab.title}?",
                    color = CarrotColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeCandidate = null
                        onCloseTab(tab.id)
                    }
                ) {
                    Text(
                        text = "Close",
                        color = CarrotColors.Accent
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        closeCandidate = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            },
            containerColor = Color(0xF51A1D22),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun TabSwitcherRow(
    tab: PdfTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onMove: (Int) -> Unit
) {
    val density = LocalDensity.current
    val reorderThresholdPx = with(density) { 96.dp.toPx() }
    var accumulatedDrag by remember(tab.id) { mutableFloatStateOf(0f) }
    var visualDragOffset by remember(tab.id) { mutableFloatStateOf(0f) }
    var isReordering by remember(tab.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = with(density) { visualDragOffset.toDp() })
                .scale(if (isReordering) 1.025f else 1f)
                .shadow(
                    elevation = if (isReordering) 12.dp else 0.dp,
                    shape = RoundedCornerShape(10.dp)
                )
                .background(
                    color = if (isReordering) Color(0xFF242A31) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) CarrotColors.Accent else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PdfFileGlyph()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tab.title,
                color = CarrotColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            IconButtonCanvas(
                contentDescription = "Close tab",
                onClick = onClose
            ) {
                drawLine(
                    color = Color.White.copy(alpha = 0.58f),
                    start = Offset(8.dp.toPx(), 8.dp.toPx()),
                    end = Offset(16.dp.toPx(), 16.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.58f),
                    start = Offset(16.dp.toPx(), 8.dp.toPx()),
                    end = Offset(8.dp.toPx(), 16.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Canvas(
                modifier = Modifier
                    .size(width = 40.dp, height = 44.dp)
                    .pointerInput(tab.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                down.consume()
                                var lastY = down.position.y
                                var wasReleased = false

                                val releasedBeforeHold = withTimeoutOrNull(TAB_REORDER_HOLD_MS) {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: return@withTimeoutOrNull true

                                        if (change.changedToUpIgnoreConsumed()) {
                                            return@withTimeoutOrNull true
                                        }

                                        lastY = change.position.y
                                        change.consume()
                                    }
                                } == true

                                if (releasedBeforeHold) {
                                    continue
                                }

                                isReordering = true
                                accumulatedDrag = 0f
                                visualDragOffset = 0f

                                while (!wasReleased) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break

                                    if (change.changedToUpIgnoreConsumed()) {
                                        wasReleased = true
                                        break
                                    }

                                    val deltaY = change.positionChange().y
                                    lastY = change.position.y

                                    change.consume()
                                    accumulatedDrag += deltaY
                                    visualDragOffset += deltaY

                                    if (accumulatedDrag > reorderThresholdPx) {
                                        onMove(1)
                                        accumulatedDrag = 0f
                                        visualDragOffset -= reorderThresholdPx
                                    } else if (accumulatedDrag < -reorderThresholdPx) {
                                        onMove(-1)
                                        accumulatedDrag = 0f
                                        visualDragOffset += reorderThresholdPx
                                    }
                                }

                                isReordering = false
                                accumulatedDrag = 0f
                                visualDragOffset = 0f
                            }
                        }
                    }
            ) {
                val handleColor = if (isReordering) {
                    Color(0xFFFF7A1A)
                } else {
                    Color.White.copy(alpha = 0.38f)
                }

                listOf(10.dp, 18.dp, 26.dp).forEach { y ->
                    drawCircle(
                        color = handleColor,
                        radius = 1.35.dp.toPx(),
                        center = Offset(16.dp.toPx(), (y + 4.dp).toPx())
                    )
                    drawCircle(
                        color = handleColor,
                        radius = 1.35.dp.toPx(),
                        center = Offset(24.dp.toPx(), (y + 4.dp).toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderMenuPopup(
    hasDocument: Boolean,
    onOpenPdf: () -> Unit,
    onGoToPage: () -> Unit,
    onSharePdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .padding(top = statusBarTop + TOP_BAR_HEIGHT + 2.dp, end = 12.dp)
                .width(248.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    color = Color(0xF31B1F24),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(vertical = 10.dp)
        ) {
            MenuAction(
                text = "Open PDF",
                icon = MenuIcon.Folder,
                onClick = onOpenPdf
            )
            MenuAction(
                text = "Go to page",
                icon = MenuIcon.PageNumber,
                onClick = onGoToPage,
                enabled = hasDocument
            )
            MenuDivider()
            MenuAction(
                text = "Share",
                icon = MenuIcon.Share,
                onClick = onSharePdf,
                enabled = hasDocument
            )
            MenuAction(
                text = "Download",
                icon = MenuIcon.Download,
                onClick = onDownloadPdf,
                enabled = hasDocument
            )
            MenuAction(
                text = "Print",
                icon = MenuIcon.Print,
                onClick = onPrintPdf,
                enabled = hasDocument
            )
        }
    }
}

@Composable
private fun MenuAction(
    text: String,
    icon: MenuIcon,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuIconCanvas(
            icon = icon,
            color = if (enabled) Color.White else CarrotColors.TextMuted
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = if (enabled) Color.White else CarrotColors.TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun FloatingAnnotationButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(15.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDDFF7A1A),
                        Color(0xCCFF5A10)
                    )
                ),
                shape = RoundedCornerShape(15.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(27.dp)) {
            val body = Color.White.copy(alpha = 0.96f)
            val green = Color(0xFF78D66A)
            val stroke = 2.4.dp.toPx()

            drawLine(
                color = body,
                start = Offset(7.dp.toPx(), 21.dp.toPx()),
                end = Offset(18.dp.toPx(), 10.dp.toPx()),
                strokeWidth = 4.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF7A1A).copy(alpha = 0.92f),
                start = Offset(9.dp.toPx(), 19.dp.toPx()),
                end = Offset(15.dp.toPx(), 13.dp.toPx()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = body,
                start = Offset(17.dp.toPx(), 8.dp.toPx()),
                end = Offset(22.dp.toPx(), 13.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = green,
                start = Offset(19.dp.toPx(), 8.dp.toPx()),
                end = Offset(24.dp.toPx(), 4.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = green,
                start = Offset(21.dp.toPx(), 10.dp.toPx()),
                end = Offset(26.dp.toPx(), 8.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = green.copy(alpha = 0.9f),
                start = Offset(20.dp.toPx(), 8.dp.toPx()),
                end = Offset(22.dp.toPx(), 3.dp.toPx()),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PdfFileGlyph() {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 30.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PDF",
            color = Color(0xFFFF3B30),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 7.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.13f))
    )
}

@Composable
private fun MenuIconCanvas(
    icon: MenuIcon,
    color: Color
) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()

        when (icon) {
            MenuIcon.Folder -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(3.dp.toPx(), 7.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(18.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(
                    color = color,
                    start = Offset(4.dp.toPx(), 8.dp.toPx()),
                    end = Offset(9.dp.toPx(), 8.dp.toPx()),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            MenuIcon.PageNumber -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(5.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(color, Offset(9.dp.toPx(), 8.dp.toPx()), Offset(15.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 12.dp.toPx()), Offset(15.dp.toPx(), 12.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 16.dp.toPx()), Offset(13.dp.toPx(), 16.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Share -> {
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(7.dp.toPx(), 12.dp.toPx()), style = Stroke(width = stroke))
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(16.dp.toPx(), 7.dp.toPx()), style = Stroke(width = stroke))
                drawCircle(color = color, radius = 2.4.dp.toPx(), center = Offset(16.dp.toPx(), 17.dp.toPx()), style = Stroke(width = stroke))
                drawLine(color, Offset(9.dp.toPx(), 11.dp.toPx()), Offset(14.dp.toPx(), 8.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 13.dp.toPx()), Offset(14.dp.toPx(), 16.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Download -> {
                drawLine(color, Offset(12.dp.toPx(), 4.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 15.dp.toPx()), stroke, StrokeCap.Round)
                drawLine(color, Offset(6.dp.toPx(), 20.dp.toPx()), Offset(18.dp.toPx(), 20.dp.toPx()), stroke, StrokeCap.Round)
            }

            MenuIcon.Print -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(6.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(4.dp.toPx(), 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawLine(color, Offset(8.dp.toPx(), 18.dp.toPx()), Offset(16.dp.toPx(), 18.dp.toPx()), stroke, StrokeCap.Round)
            }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBookTabsIcon() {
    val stroke = 1.8.dp.toPx()
    val white = Color.White.copy(alpha = 0.92f)

    drawRoundRect(
        color = white,
        topLeft = Offset(4.dp.toPx(), 5.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 15.dp.toPx()),
        style = Stroke(width = stroke),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx())
    )
    drawLine(
        color = white,
        start = Offset(12.dp.toPx(), 5.dp.toPx()),
        end = Offset(12.dp.toPx(), 20.dp.toPx()),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = white.copy(alpha = 0.62f),
        start = Offset(7.dp.toPx(), 9.dp.toPx()),
        end = Offset(10.dp.toPx(), 9.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = white.copy(alpha = 0.62f),
        start = Offset(14.dp.toPx(), 9.dp.toPx()),
        end = Offset(17.dp.toPx(), 9.dp.toPx()),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private enum class MenuIcon {
    Folder,
    PageNumber,
    Share,
    Download,
    Print
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

    DisposableEffect(activity) {
        val window = activity?.window

        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(activity, isChromeVisible) {
        val window = activity?.window

        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)

            if (isChromeVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
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

private val TOP_BAR_HEIGHT = 56.dp
private const val SEARCH_DEBOUNCE_MS = 320L
private const val EDGE_REVEAL_DISTANCE_PX = 18f
private const val PAGE_INDICATOR_VISIBLE_MS = 1200L
private const val TAB_REORDER_HOLD_MS = 180L
