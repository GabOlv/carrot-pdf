package com.example.carrotpdf.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.carrotpdf.pdf.PdfLinkRegion
import com.example.carrotpdf.pdf.PdfLinkSession
import com.example.carrotpdf.pdf.PdfLinkTarget
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.pdf.PdfSearchSession
import com.example.carrotpdf.pdf.createPdfFromImages
import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.getPdfPageSizes
import com.example.carrotpdf.pdf.printPdf
import com.example.carrotpdf.pdf.sharePdf
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun CarrotPdfApp(
    externalPdfUri: Uri? = null,
    externalImageUris: List<Uri> = emptyList(),
    externalOpenRequestId: Int = 0
) {
    CarrotDesignTheme(darkTheme = true) {
        CarrotPdfContent(
            externalPdfUri = externalPdfUri,
            externalImageUris = externalImageUris,
            externalOpenRequestId = externalOpenRequestId
        )
    }
}

@Composable
private fun CarrotPdfContent(
    externalPdfUri: Uri?,
    externalImageUris: List<Uri>,
    externalOpenRequestId: Int
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val tabs = remember { mutableStateListOf<PdfTab>() }
    val searchResults = remember { mutableStateListOf<PdfSearchResult>() }
    val linkRegions = remember { mutableStateListOf<PdfLinkRegion>() }

    var activeTabId by remember { mutableStateOf<String?>(null) }
    var isChromeVisible by remember { mutableStateOf(true) }
    var isTabSwitcherOpen by remember { mutableStateOf(false) }
    var isOverflowOpen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var activeSearchResultIndex by remember { mutableIntStateOf(-1) }
    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingImagePdf by remember { mutableStateOf(false) }
    var hasRestoredPersistedTabs by remember { mutableStateOf(false) }
    var selectedExternalLink by remember { mutableStateOf<String?>(null) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeViewerState = rememberActiveViewerState(activeTab)
    val activeSearchSession = remember(activeTab?.id) {
        activeTab?.let { tab ->
            PdfSearchSession(
                context = context.applicationContext,
                uri = tab.uri
            )
        }
    }
    val activeLinkSession = remember(activeTab?.id) {
        activeTab?.let { tab ->
            PdfLinkSession(
                context = context.applicationContext,
                uri = tab.uri
            )
        }
    }

    ImmersiveSystemBars(isChromeVisible)

    fun closeSearch() {
        isSearchVisible = false
        searchQuery = ""
        searchResults.clear()
        activeSearchResultIndex = -1
        isSearching = false
        selectedExternalLink = null
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

    fun openImagesAsPdf(imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            return
        }

        coroutineScope.launch {
            isCreatingImagePdf = true

            val result = try {
                withContext(Dispatchers.IO) {
                    createPdfFromImages(
                        context = context.applicationContext,
                        imageUris = imageUris
                    )
                }
            } finally {
                isCreatingImagePdf = false
            }

            result
                .onSuccess { generatedPdf ->
                    openTab(
                        uri = generatedPdf.uri,
                        title = generatedPdf.title
                    )
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        "Não foi possível criar o PDF das imagens.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
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

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Some providers grant only temporary access.
                    }
                }

                openImagesAsPdf(uris)
            }
        }
    )
    val openImages = {
        imagePicker.launch(arrayOf("image/*"))
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

    LaunchedEffect(externalOpenRequestId, hasRestoredPersistedTabs) {
        if (!hasRestoredPersistedTabs || externalOpenRequestId == 0) {
            return@LaunchedEffect
        }

        when {
            externalImageUris.isNotEmpty() -> openImagesAsPdf(externalImageUris)
            externalPdfUri != null -> openExternalPdf(externalPdfUri)
        }
    }

    LaunchedEffect(activeTabId) {
        closeSearch()
        linkRegions.clear()
        selectedExternalLink = null

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

    LaunchedEffect(activeTab?.id, activeLinkSession) {
        linkRegions.clear()
        selectedExternalLink = null

        val session = activeLinkSession ?: return@LaunchedEffect
        val links = withContext(Dispatchers.IO) {
            runCatching {
                session.links()
            }.getOrDefault(emptyList())
        }

        linkRegions.clear()
        linkRegions.addAll(links)
    }

    LaunchedEffect(isSearchVisible, activeSearchSession) {
        if (isSearchVisible && activeSearchSession != null) {
            withContext(Dispatchers.IO) {
                activeSearchSession.warmUp()
            }
        }
    }

    LaunchedEffect(isSearchVisible, searchQuery, activeTab?.id, activeSearchSession) {
        val tab = activeTab
        val query = searchQuery.trim()

        if (!isSearchVisible || tab == null || activeSearchSession == null || query.isBlank()) {
            searchResults.clear()
            activeSearchResultIndex = -1
            isSearching = false
            return@LaunchedEffect
        }

        delay(SEARCH_DEBOUNCE_MS)
        isSearching = true

        val results = withContext(Dispatchers.IO) {
            activeSearchSession.search(query)
        }

        searchResults.clear()
        searchResults.addAll(results)
        activeSearchResultIndex = if (results.isEmpty()) -1 else 0
        isSearching = false

        results.firstOrNull()?.let { result ->
            activeViewerState?.requestScrollToSearchResult(result)
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
                linkRegions = linkRegions,
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
                onLinkTap = { link ->
                    when (val target = link.target) {
                        is PdfLinkTarget.ExternalUri -> {
                            selectedExternalLink = target.uri
                            isChromeVisible = true
                            isOverflowOpen = false
                            isTabSwitcherOpen = false
                        }

                        is PdfLinkTarget.PageDestination -> {
                            selectedExternalLink = null
                            isOverflowOpen = false
                            isTabSwitcherOpen = false
                            activeViewerState?.requestScrollToPageLocation(
                                pageIndex = target.pageIndex,
                                normalizedX = target.normalizedX,
                                normalizedY = target.normalizedY
                            )
                        }

                        PdfLinkTarget.Unsupported -> {
                            Toast.makeText(
                                context,
                                "Unsupported PDF link.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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
                                activeViewerState?.requestScrollToSearchResult(result)
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
                                activeViewerState?.requestScrollToSearchResult(result)
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

            if (isCreatingImagePdf) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Criando PDF...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                    onOpenImages = {
                        isOverflowOpen = false
                        openImages()
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

            selectedExternalLink?.let { link ->
                ExternalLinkDialog(
                    uri = link,
                    onCopy = {
                        copyTextToClipboard(
                            context = context,
                            label = "PDF link",
                            text = link
                        )
                        Toast.makeText(
                            context,
                            "Link copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onOpen = {
                        selectedExternalLink = null
                        openExternalUri(
                            context = context,
                            uri = link
                        )
                    },
                    onDismiss = {
                        selectedExternalLink = null
                    }
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

private fun PdfViewerState.requestScrollToSearchResult(
    result: PdfSearchResult
) {
    val firstBound = result.bounds.firstOrNull()

    if (firstBound == null || firstBound.pageWidth <= 0f || firstBound.pageHeight <= 0f) {
        requestScrollToPage(result.pageIndex)
        return
    }

    requestScrollToPageLocation(
        pageIndex = result.pageIndex,
        normalizedX = ((firstBound.left + firstBound.right) / 2f) / firstBound.pageWidth,
        normalizedY = ((firstBound.top + firstBound.bottom) / 2f) / firstBound.pageHeight
    )
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

tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun ExternalLinkDialog(
    uri: String,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("PDF link")
        },
        text = {
            Text(
                text = uri,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Text("Open")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCopy) {
                    Text("Copy")
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun copyTextToClipboard(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return

    clipboard.setPrimaryClip(
        ClipData.newPlainText(label, text)
    )
}

private fun openExternalUri(
    context: Context,
    uri: String
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No app can open this link.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

val TOP_BAR_HEIGHT = 56.dp
const val SEARCH_DEBOUNCE_MS = 320L
const val EDGE_REVEAL_DISTANCE_PX = 18f
const val PAGE_INDICATOR_VISIBLE_MS = 1200L
const val TAB_REORDER_HOLD_MS = 180L

