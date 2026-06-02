package com.example.carrotpdf.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Rect
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
import androidx.core.view.drawToBitmap
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
import com.example.carrotpdf.pdf.PdfTextIndexSession
import com.example.carrotpdf.pdf.PdfTextSelection
import com.example.carrotpdf.pdf.PdfTextSelectionHandle
import com.example.carrotpdf.pdf.createAnnotatedPdfExport
import com.example.carrotpdf.pdf.createPdfFromImages
import com.example.carrotpdf.pdf.PdfPageSize
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.getPdfPageSizes
import com.example.carrotpdf.pdf.printPdf
import com.example.carrotpdf.pdf.saveScreenshot
import com.example.carrotpdf.pdf.sharePdf
import com.example.carrotpdf.ui.components.ContinuousPdfViewer
import com.example.carrotpdf.ui.components.DrawTarget
import com.example.carrotpdf.ui.components.EmptyState
import com.example.carrotpdf.ui.components.NotesWorkspaceSheet
import com.example.carrotpdf.ui.components.WorkspaceDrawTool
import com.example.carrotpdf.ui.components.WorkspaceMode
import com.example.carrotpdf.ui.components.WorkspaceSheetLayout
import com.example.carrotpdf.ui.design.CarrotColors
import com.example.carrotpdf.ui.design.CarrotDesignTheme
import com.example.carrotpdf.ui.viewer.state.PdfViewerState
import com.example.carrotpdf.ui.viewer.state.rememberPdfViewerState
import com.example.carrotpdf.workspace.CanvasInkStroke
import com.example.carrotpdf.workspace.InkPoint
import com.example.carrotpdf.workspace.InkTool
import com.example.carrotpdf.workspace.PageInkStroke
import com.example.carrotpdf.workspace.WorkspaceCanvas
import com.example.carrotpdf.workspace.WorkspaceRepository
import com.example.carrotpdf.workspace.exportWorkspaceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

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
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
    val workspaceRepository = remember(context) {
        WorkspaceRepository(context.applicationContext)
    }
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
    var isExportingAnnotatedPdf by remember { mutableStateOf(false) }
    var isExportingWorkspaceData by remember { mutableStateOf(false) }
    var hasRestoredPersistedTabs by remember { mutableStateOf(false) }
    var selectedExternalLink by remember { mutableStateOf<String?>(null) }
    var selectedTextSelection by remember { mutableStateOf<PdfTextSelection?>(null) }
    var textSelectionDragGeneration by remember { mutableIntStateOf(0) }
    var isCapturingScreenshot by remember { mutableStateOf(false) }
    var currentPaperBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    var reimportingTabId by remember { mutableStateOf<String?>(null) }
    var isWorkspaceOpen by remember { mutableStateOf(false) }
    var isWorkspaceExpanded by remember { mutableStateOf(false) }
    var workspaceMode by remember { mutableStateOf(WorkspaceMode.Notes) }
    var workspaceDrawTarget by remember { mutableStateOf(DrawTarget.Canvas) }
    var workspaceDrawTool by remember { mutableStateOf(WorkspaceDrawTool.Pen) }
    var workspaceInkColor by remember { mutableStateOf(DEFAULT_WORKSPACE_INK_COLOR) }
    var workspaceStrokeWidth by remember { mutableFloatStateOf(DEFAULT_WORKSPACE_STROKE_WIDTH) }
    var workspaceNotesText by remember { mutableStateOf("") }
    var workspaceCanvasStrokes by remember { mutableStateOf<List<CanvasInkStroke>>(emptyList()) }
    var workspacePageInkStrokes by remember { mutableStateOf<List<PageInkStroke>>(emptyList()) }
    var loadedWorkspaceTabId by remember { mutableStateOf<String?>(null) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val isPdfInkActive = isWorkspaceOpen &&
        workspaceMode == WorkspaceMode.Draw &&
        workspaceDrawTarget == DrawTarget.Pdf &&
        workspaceDrawTool != WorkspaceDrawTool.Move &&
        activeTab != null &&
        !activeTab.isMissing
    val activePdfInkTool = if (workspaceDrawTool == WorkspaceDrawTool.Eraser) {
        InkTool.Eraser
    } else {
        InkTool.Pen
    }
    val activePdfInkWidth = workspaceStrokeWidth.toPdfInkWidth()
    val activeViewerState = rememberActiveViewerState(activeTab)
    val activeSearchSession = remember(activeTab?.id, activeTab?.uri, activeTab?.isMissing) {
        activeTab?.takeUnless { it.isMissing }?.let { tab ->
            PdfSearchSession(
                context = context.applicationContext,
                uri = tab.uri
            )
        }
    }
    val activeLinkSession = remember(activeTab?.id, activeTab?.uri, activeTab?.isMissing) {
        activeTab?.takeUnless { it.isMissing }?.let { tab ->
            PdfLinkSession(
                context = context.applicationContext,
                uri = tab.uri
            )
        }
    }
    val activeTextIndexSession = remember(activeTab?.id, activeTab?.uri, activeTab?.isMissing) {
        activeTab?.takeUnless { it.isMissing }?.let { tab ->
            PdfTextIndexSession(
                context = context.applicationContext,
                uri = tab.uri
            )
        }
    }

    ImmersiveSystemBars(
        isChromeVisible = isChromeVisible || selectedTextSelection != null
    )

    fun closeSearch() {
        isSearchVisible = false
        searchQuery = ""
        searchResults.clear()
        activeSearchResultIndex = -1
        isSearching = false
        selectedExternalLink = null
        selectedTextSelection = null
    }

    fun hideChromeForReading() {
        selectedExternalLink = null

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
                if (tab.isMissing) {
                    tab.copy(
                        title = title,
                        isMissing = false,
                        pageCount = 0,
                        pageSizes = emptyList()
                    )
                } else {
                    tab
                }
            }
        } else {
            val tab = PdfTab(uri = uri, title = title)
            tabs.add(tab)
            activeTabId = tab.id
        }

        tabs.firstOrNull { tab -> tab.id == activeTabId }?.let { tab ->
            coroutineScope.launch(Dispatchers.IO) {
                workspaceRepository.loadOrCreate(tab)
            }
        }
        closeSearch()
        isChromeVisible = true
        persistTabsNow()
    }

    fun reimportTab(
        tabId: String,
        uri: Uri,
        title: String
    ) {
        updateActiveTab(tabs, tabId) { tab ->
            tab.copy(
                uri = uri,
                title = title,
                pageCount = 0,
                pageSizes = emptyList(),
                currentPageIndex = 0,
                zoom = 1f,
                viewportLeft = 0f,
                viewportTop = 0f,
                isMissing = false
            )
        }

        activeTabId = tabId
        tabs.firstOrNull { tab -> tab.id == tabId }?.let { tab ->
            coroutineScope.launch(Dispatchers.IO) {
                workspaceRepository.loadOrCreate(tab)
            }
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

    suspend fun exportablePdfUri(tab: PdfTab): Uri? {
        val pageInk = workspacePageInkStrokes

        if (pageInk.isEmpty()) {
            return tab.uri
        }

        isExportingAnnotatedPdf = true

        return try {
            withContext(Dispatchers.IO) {
                createAnnotatedPdfExport(
                    context = context.applicationContext,
                    sourceUri = tab.uri,
                    title = tab.title,
                    pageInkStrokes = pageInk
                ).getOrThrow().uri
            }
        } catch (_: Throwable) {
            Toast.makeText(
                context,
                "Could not export annotated PDF.",
                Toast.LENGTH_SHORT
            ).show()
            null
        } finally {
            isExportingAnnotatedPdf = false
        }
    }

    suspend fun exportCurrentWorkspaceData(tab: PdfTab): Boolean {
        isExportingWorkspaceData = true

        return try {
            withContext(Dispatchers.IO) {
                exportWorkspaceData(
                    context = context.applicationContext,
                    title = tab.title,
                    notesText = workspaceNotesText,
                    canvas = WorkspaceCanvas(
                        strokes = workspaceCanvasStrokes
                    )
                ).success
            }
        } finally {
            isExportingWorkspaceData = false
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

                val title = getPdfTitle(context, uri)
                val targetTabId = reimportingTabId

                if (targetTabId != null && tabs.any { tab -> tab.id == targetTabId }) {
                    reimportTab(
                        tabId = targetTabId,
                        uri = uri,
                        title = title
                    )
                } else {
                    openTab(
                        uri = uri,
                        title = title
                    )
                }
            }

            reimportingTabId = null
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

    BackHandler(enabled = selectedTextSelection != null) {
        selectedTextSelection = null
    }

    BackHandler(enabled = isWorkspaceOpen) {
        isWorkspaceOpen = false
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
        val restoredTabsWithMissingState = withContext(Dispatchers.IO) {
            restoredTabs.tabs.map { tab ->
                tab.copy(
                    isMissing = tab.isMissing || !canReadPdfUri(context.applicationContext, tab.uri)
                )
            }
        }
        val restoredActiveTabId = restoredTabs.activeTabId
            ?.takeIf { id -> restoredTabsWithMissingState.any { tab -> tab.id == id } }
            ?: restoredTabsWithMissingState.firstOrNull()?.id

        if (tabs.isEmpty() && restoredTabsWithMissingState.isNotEmpty()) {
            tabs.addAll(restoredTabsWithMissingState)
            activeTabId = restoredActiveTabId
        }

        withContext(Dispatchers.IO) {
            workspaceRepository.ensureWorkspaces(restoredTabsWithMissingState)
        }

        if (restoredTabsWithMissingState != restoredTabs.tabs) {
            PdfTabPersistence.save(
                context = context.applicationContext,
                tabs = restoredTabsWithMissingState,
                activeTabId = restoredActiveTabId
            )
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
        selectedTextSelection = null
        currentPaperBoundsInWindow = null
        loadedWorkspaceTabId = null
        workspaceNotesText = ""
        workspaceCanvasStrokes = emptyList()
        workspacePageInkStrokes = emptyList()

        val tab = activeTab ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val workspace = workspaceRepository.loadOrCreate(tab)
            withContext(Dispatchers.Main) {
                workspaceNotesText = workspace.notes.text
                workspaceCanvasStrokes = workspace.canvas.strokes
                workspacePageInkStrokes = workspace.pageInk
                loadedWorkspaceTabId = tab.id
            }
        }

        if (tab.isMissing) {
            isLoadingDocument = false
            return@LaunchedEffect
        }

        if (tab.pageCount == 0 || tab.pageSizes.isEmpty()) {
            isLoadingDocument = true

            val loadedDocument = withContext(Dispatchers.IO) {
                runCatching {
                    withTimeoutOrNull(DOCUMENT_LOAD_TIMEOUT_MS) {
                        val pageSizes = getPdfPageSizes(context, tab.uri)
                        val pageCount = pageSizes.size.takeIf { it > 0 }
                            ?: getPdfPageCount(context, tab.uri)

                        pageCount to pageSizes
                    } ?: error("PDF load timed out")
                }
            }

            isLoadingDocument = false

            loadedDocument
                .onSuccess { (pageCount, pageSizes) ->
                    if (pageCount <= 0) {
                        updateActiveTab(tabs, tab.id) { currentTab ->
                            currentTab.copy(isMissing = true)
                        }
                        persistTabsNow()
                        Toast.makeText(
                            context,
                            "Could not find this PDF.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@onSuccess
                    }

                    updateActiveTab(tabs, tab.id) { currentTab ->
                        currentTab.copy(
                            pageCount = pageCount,
                            pageSizes = pageSizes
                        )
                    }
                    persistTabsNow()
                }
                .onFailure {
                    updateActiveTab(tabs, tab.id) { currentTab ->
                        currentTab.copy(isMissing = true)
                    }
                    persistTabsNow()
                    Toast.makeText(
                        context,
                        "Could not find this PDF.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    LaunchedEffect(activeTab?.id, loadedWorkspaceTabId, workspaceNotesText) {
        val tab = activeTab ?: return@LaunchedEffect

        if (loadedWorkspaceTabId != tab.id) {
            return@LaunchedEffect
        }

        delay(WORKSPACE_SAVE_DEBOUNCE_MS)

        withContext(Dispatchers.IO) {
            workspaceRepository.updateNotes(
                tab = tab,
                text = workspaceNotesText
            )
        }
    }

    LaunchedEffect(activeTab?.id, loadedWorkspaceTabId, workspaceCanvasStrokes) {
        val tab = activeTab ?: return@LaunchedEffect

        if (loadedWorkspaceTabId != tab.id) {
            return@LaunchedEffect
        }

        delay(WORKSPACE_SAVE_DEBOUNCE_MS)

        withContext(Dispatchers.IO) {
            workspaceRepository.updateCanvasStrokes(
                tab = tab,
                strokes = workspaceCanvasStrokes
            )
        }
    }

    LaunchedEffect(activeTab?.id, loadedWorkspaceTabId, workspacePageInkStrokes) {
        val tab = activeTab ?: return@LaunchedEffect

        if (loadedWorkspaceTabId != tab.id) {
            return@LaunchedEffect
        }

        delay(WORKSPACE_SAVE_DEBOUNCE_MS)

        withContext(Dispatchers.IO) {
            workspaceRepository.updatePageInkStrokes(
                tab = tab,
                strokes = workspacePageInkStrokes
            )
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(CarrotColors.PdfCanvas)
        ) {
            val isTabletWorkspace = maxWidth >= TABLET_WORKSPACE_BREAKPOINT

            ReaderStage(
                activeTab = activeTab,
                viewerState = activeViewerState,
                isLoadingDocument = isLoadingDocument,
                searchResults = searchResults,
                activeSearchResultIndex = activeSearchResultIndex,
                linkRegions = linkRegions,
                selectedTextSelection = selectedTextSelection,
                suppressPageOverlays = isCapturingScreenshot,
                pageSizes = activeTab?.pageSizes.orEmpty(),
                pageInkStrokes = workspacePageInkStrokes,
                isPdfInkActive = isPdfInkActive,
                pdfInkTool = activePdfInkTool,
                pdfInkColor = workspaceInkColor,
                pdfInkWidth = activePdfInkWidth,
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
                onReimportMissingPdf = { tab ->
                    reimportingTabId = tab.id
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                onRemoveMissingPdf = { tab ->
                    closeTab(tab.id)
                },
                onToggleChrome = {
                    if (selectedTextSelection != null) {
                        selectedTextSelection = null
                    } else if (!isSearchVisible) {
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
                            selectedTextSelection = null
                            isChromeVisible = true
                            isOverflowOpen = false
                            isTabSwitcherOpen = false
                        }

                        is PdfLinkTarget.PageDestination -> {
                            selectedExternalLink = null
                            selectedTextSelection = null
                            isOverflowOpen = false
                            isTabSwitcherOpen = false
                            activeViewerState?.requestScrollToPageLocation(
                                pageIndex = target.pageIndex,
                                normalizedX = target.normalizedX,
                                normalizedY = target.normalizedY
                            )
                        }

                        PdfLinkTarget.Unsupported -> {
                            selectedTextSelection = null
                            Toast.makeText(
                                context,
                                "Unsupported PDF link.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onPdfInkStroke = { stroke ->
                    selectedExternalLink = null
                    selectedTextSelection = null
                    workspacePageInkStrokes = workspacePageInkStrokes + stroke
                },
                onPdfInkErase = { pageIndex, points ->
                    selectedExternalLink = null
                    selectedTextSelection = null
                    workspacePageInkStrokes = workspacePageInkStrokes.erasePageInkStrokes(
                        pageIndex = pageIndex,
                        erasePoints = points
                    )
                },
                onTextLongPress = { pageIndex, normalizedX, normalizedY ->
                    val session = activeTextIndexSession ?: return@ReaderStage
                    val tabId = activeTabId

                    selectedExternalLink = null
                    coroutineScope.launch {
                        val selection = withContext(Dispatchers.IO) {
                            runCatching {
                                session.wordAt(
                                    pageIndex = pageIndex,
                                    normalizedX = normalizedX,
                                    normalizedY = normalizedY
                                )
                            }.getOrNull()
                        }

                        if (activeTabId == tabId && selection != null) {
                            closeSearch()
                            selectedTextSelection = selection
                            isChromeVisible = true
                            isOverflowOpen = false
                            isTabSwitcherOpen = false
                        }
                    }
                },
                onTextSelectionHandleDrag = { handle, pageIndex, normalizedX, normalizedY ->
                    val session = activeTextIndexSession ?: return@ReaderStage
                    val selection = selectedTextSelection
                        ?.takeIf { currentSelection ->
                            currentSelection.pageRanges.any { range -> range.pageIndex == pageIndex }
                        }
                        ?: return@ReaderStage
                    val tabId = activeTabId
                    val requestId = ++textSelectionDragGeneration

                    coroutineScope.launch {
                        val adjustedSelection = withContext(Dispatchers.IO) {
                            runCatching {
                                session.adjustSelection(
                                    selection = selection,
                                    handle = handle,
                                    pageIndex = pageIndex,
                                    normalizedX = normalizedX,
                                    normalizedY = normalizedY
                                )
                            }.getOrNull()
                        }

                        if (
                            activeTabId == tabId &&
                            requestId == textSelectionDragGeneration &&
                            adjustedSelection != null
                        ) {
                            selectedTextSelection = adjustedSelection
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
                },
                onViewportOriginChange = { leftPx, topPx ->
                    updateActiveTab(tabs, activeTabId) { tab ->
                        if (
                            abs(tab.viewportLeft - leftPx) < 0.5f &&
                            abs(tab.viewportTop - topPx) < 0.5f
                        ) {
                            tab
                        } else {
                            tab.copy(
                                viewportLeft = leftPx.coerceAtLeast(0f),
                                viewportTop = topPx.coerceAtLeast(0f)
                            )
                        }
                    }
                },
                onCurrentPageBoundsChange = { bounds ->
                    currentPaperBoundsInWindow = bounds
                }
            )

            if (!isCapturingScreenshot) {
                AnimatedVisibility(
                    visible = isChromeVisible && !isSearchVisible && selectedTextSelection == null,
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
                            if (activeTab != null && !activeTab.isMissing) {
                                selectedTextSelection = null
                                selectedExternalLink = null
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
            }

            if (!isCapturingScreenshot) {
                AnimatedVisibility(
                    visible = isSearchVisible && selectedTextSelection == null,
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
            }

            selectedTextSelection?.let { selection ->
                if (!isCapturingScreenshot) {
                    ReaderTextSelectionBar(
                        selectedText = selection.text,
                        onBack = {
                            selectedTextSelection = null
                        },
                        onCopy = {
                            copyTextToClipboard(
                                context = context,
                                label = "PDF text",
                                text = selection.text
                            )
                            Toast.makeText(
                                context,
                                "Text copied",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedTextSelection = null
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            if (!isCapturingScreenshot) {
                AnimatedVisibility(
                    visible = (isChromeVisible || isSearchVisible) &&
                        activeTab != null &&
                        !activeTab.isMissing &&
                        selectedTextSelection == null,
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
                            isWorkspaceOpen = true
                            workspaceMode = WorkspaceMode.Draw
                            workspaceDrawTarget = DrawTarget.Pdf
                            workspaceDrawTool = WorkspaceDrawTool.Pen
                            selectedExternalLink = null
                            selectedTextSelection = null
                            isChromeVisible = true
                        }
                    )
                }
            }

            if (!isCapturingScreenshot) {
                AnimatedVisibility(
                    visible = (isChromeVisible || isSearchVisible) &&
                        activeTab != null &&
                        !activeTab.isMissing &&
                        selectedTextSelection == null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 24.dp,
                            bottom = navigationBarBottom + 18.dp
                        )
                ) {
                    FloatingScreenshotButton(
                        onClick = {
                            val tab = activeTab

                            if (tab != null) {
                                coroutineScope.launch {
                                    val paperBounds = currentPaperBoundsInWindow

                                    if (paperBounds == null) {
                                        Toast.makeText(
                                            context,
                                            "Could not capture page.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }

                                    isCapturingScreenshot = true
                                    isChromeVisible = false
                                    isOverflowOpen = false
                                    isTabSwitcherOpen = false
                                    selectedExternalLink = null
                                    delay(SCREENSHOT_CAPTURE_DELAY_MS)

                                    val bitmap = runCatching {
                                        view.drawToBitmap()
                                    }.getOrNull()
                                    val croppedBitmap = bitmap?.let { source ->
                                        cropBitmapToWindowBounds(
                                            source = source,
                                            view = view,
                                            boundsInWindow = paperBounds
                                        )
                                    }
                                    val saved = croppedBitmap != null && withContext(Dispatchers.IO) {
                                        saveScreenshot(
                                            context = context.applicationContext,
                                            bitmap = croppedBitmap,
                                            title = tab.title
                                        )
                                    }

                                    isCapturingScreenshot = false

                                    Toast.makeText(
                                        context,
                                        if (saved) "Screenshot saved" else "Could not save screenshot",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
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

            if (
                isWorkspaceOpen &&
                activeTab != null &&
                !isCapturingScreenshot
            ) {
                val mobileWorkspaceHeight = if (isWorkspaceExpanded) {
                    (maxHeight - statusBarTop - TOP_BAR_HEIGHT - navigationBarBottom - 12.dp)
                        .coerceAtLeast(WORKSPACE_DEFAULT_HEIGHT)
                } else if (workspaceMode == WorkspaceMode.Draw && workspaceDrawTarget == DrawTarget.Pdf) {
                    WORKSPACE_PDF_DRAW_HEIGHT
                } else {
                    WORKSPACE_DEFAULT_HEIGHT
                }

                NotesWorkspaceSheet(
                    notesText = workspaceNotesText,
                    onNotesChange = { text ->
                        workspaceNotesText = text
                    },
                    selectedMode = workspaceMode,
                    onModeChange = { mode ->
                        workspaceMode = mode
                        if (mode == WorkspaceMode.Draw && workspaceDrawTarget == DrawTarget.Pdf) {
                            selectedExternalLink = null
                            selectedTextSelection = null
                        }
                    },
                    selectedDrawTarget = workspaceDrawTarget,
                    onDrawTargetChange = { target ->
                        workspaceDrawTarget = target
                        if (target == DrawTarget.Pdf) {
                            selectedExternalLink = null
                            selectedTextSelection = null
                        }
                    },
                    selectedDrawTool = workspaceDrawTool,
                    onDrawToolChange = { tool ->
                        workspaceDrawTool = tool
                    },
                    selectedInkColor = workspaceInkColor,
                    onInkColorChange = { color ->
                        workspaceInkColor = color
                    },
                    selectedStrokeWidth = workspaceStrokeWidth,
                    onStrokeWidthChange = { width ->
                        workspaceStrokeWidth = width
                    },
                    canvasStrokes = workspaceCanvasStrokes,
                    onCanvasStrokesChange = { strokes ->
                        workspaceCanvasStrokes = strokes
                    },
                    pageInkStrokeCount = workspacePageInkStrokes.size,
                    onUndoPageInk = {
                        if (workspacePageInkStrokes.isNotEmpty()) {
                            workspacePageInkStrokes = workspacePageInkStrokes.dropLast(1)
                        }
                    },
                    onCollapse = {
                        isWorkspaceOpen = false
                    },
                    isExpanded = isWorkspaceExpanded,
                    onExpandedChange = { expanded ->
                        isWorkspaceExpanded = expanded
                    },
                    layout = if (isTabletWorkspace) {
                        WorkspaceSheetLayout.Side
                    } else {
                        WorkspaceSheetLayout.Bottom
                    },
                    modifier = if (isTabletWorkspace) {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(400.dp)
                            .padding(
                                top = statusBarTop + TOP_BAR_HEIGHT + 8.dp,
                                end = 14.dp,
                                bottom = navigationBarBottom + 14.dp
                            )
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .height(mobileWorkspaceHeight)
                            .padding(bottom = navigationBarBottom)
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
                    hasDocument = activeTab != null && !activeTab.isMissing,
                    onOpenPdf = {
                        isOverflowOpen = false
                        openPdf()
                    },
                    onOpenImages = {
                        isOverflowOpen = false
                        openImages()
                    },
                    onExportData = {
                        val tab = activeTab

                        if (tab != null) {
                            isOverflowOpen = false
                            coroutineScope.launch {
                                val success = exportCurrentWorkspaceData(tab)
                                Toast.makeText(
                                    context,
                                    if (success) {
                                        "PDF data exported"
                                    } else {
                                        "Could not export PDF data"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onSharePdf = {
                        val tab = activeTab

                        if (tab != null) {
                            isOverflowOpen = false
                            coroutineScope.launch {
                                exportablePdfUri(tab)?.let { exportUri ->
                                    sharePdf(context, exportUri, tab.title)
                                }
                            }
                        }
                    },
                    onDownloadPdf = {
                        val tab = activeTab

                        if (tab != null) {
                            isOverflowOpen = false
                            coroutineScope.launch {
                                val exportUri = exportablePdfUri(tab) ?: return@launch
                                val success = withContext(Dispatchers.IO) {
                                    downloadPdf(context, exportUri, tab.title)
                                }
                                Toast.makeText(
                                    context,
                                    if (success) "PDF downloaded" else "Could not download PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onPrintPdf = {
                        val tab = activeTab

                        if (tab != null) {
                            isOverflowOpen = false
                            coroutineScope.launch {
                                exportablePdfUri(tab)?.let { exportUri ->
                                    printPdf(context, exportUri, tab.title)
                                }
                            }
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

            if (isExportingAnnotatedPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preparing PDF...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isExportingWorkspaceData) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exporting PDF data...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

        }
    }
}

@Composable
private fun rememberActiveViewerState(
    activeTab: PdfTab?
): PdfViewerState? {
    if (activeTab == null || activeTab.isMissing) {
        return null
    }

    return rememberPdfViewerState(
        documentId = activeTab.id,
        pageCount = activeTab.pageCount,
        initialPageIndex = activeTab.currentPageIndex,
        initialZoom = activeTab.zoom,
        initialViewportLeftPx = activeTab.viewportLeft,
        initialViewportTopPx = activeTab.viewportTop
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

private fun List<PageInkStroke>.erasePageInkStrokes(
    pageIndex: Int,
    erasePoints: List<InkPoint>
): List<PageInkStroke> {
    if (erasePoints.isEmpty()) {
        return this
    }

    return filterNot { stroke ->
        stroke.pageIndex == pageIndex &&
            stroke.points.isNearPageInkPath(
                testPoints = erasePoints,
                radius = max(PAGE_INK_ERASER_RADIUS, stroke.width * 1.8f)
            )
    }
}

private fun List<InkPoint>.isNearPageInkPath(
    testPoints: List<InkPoint>,
    radius: Float
): Boolean {
    if (isEmpty() || testPoints.isEmpty()) {
        return false
    }

    val radiusSquared = radius * radius

    return testPoints.any { testPoint ->
        any { point ->
            point.distanceSquaredTo(testPoint) <= radiusSquared
        } || zipWithNext().any { (start, end) ->
            testPoint.distanceSquaredToSegment(start, end) <= radiusSquared
        }
    }
}

private fun InkPoint.distanceSquaredTo(other: InkPoint): Float {
    val dx = x - other.x
    val dy = y - other.y

    return dx * dx + dy * dy
}

private fun InkPoint.distanceSquaredToSegment(
    start: InkPoint,
    end: InkPoint
): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy

    if (lengthSquared <= 0.000001f) {
        return distanceSquaredTo(start)
    }

    val t = (((x - start.x) * dx + (y - start.y) * dy) / lengthSquared)
        .coerceIn(0f, 1f)
    val projection = InkPoint(
        x = start.x + t * dx,
        y = start.y + t * dy
    )

    return distanceSquaredTo(projection)
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

private fun canReadPdfUri(
    context: Context,
    uri: Uri
): Boolean {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
            true
        } ?: false
    }.getOrDefault(false)
}

private fun cropBitmapToWindowBounds(
    source: Bitmap,
    view: android.view.View,
    boundsInWindow: Rect?
): Bitmap? {
    if (boundsInWindow == null || source.width <= 0 || source.height <= 0) {
        return null
    }

    val viewLocation = IntArray(2)
    view.getLocationInWindow(viewLocation)

    val left = floor(boundsInWindow.left - viewLocation[0]).toInt().coerceIn(0, source.width)
    val top = floor(boundsInWindow.top - viewLocation[1]).toInt().coerceIn(0, source.height)
    val right = ceil(boundsInWindow.right - viewLocation[0]).toInt().coerceIn(0, source.width)
    val bottom = ceil(boundsInWindow.bottom - viewLocation[1]).toInt().coerceIn(0, source.height)
    val width = max(0, right - left)
    val height = max(0, bottom - top)

    if (width <= 1 || height <= 1) {
        return null
    }

    val croppedWidth = min(width, source.width - left)
    val croppedHeight = min(height, source.height - top)

    return Bitmap.createBitmap(
        source,
        left,
        top,
        croppedWidth,
        croppedHeight
    )
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
const val DOCUMENT_LOAD_TIMEOUT_MS = 12_000L
const val SCREENSHOT_CAPTURE_DELAY_MS = 120L
const val WORKSPACE_SAVE_DEBOUNCE_MS = 450L
const val DEFAULT_WORKSPACE_INK_COLOR = 0xFFFF7A1AL
const val DEFAULT_WORKSPACE_STROKE_WIDTH = 34f
const val PDF_INK_WIDTH_DENOMINATOR = 7000f
val WORKSPACE_DEFAULT_HEIGHT = 326.dp
val WORKSPACE_PDF_DRAW_HEIGHT = 226.dp
val TABLET_WORKSPACE_BREAKPOINT = 700.dp
const val PAGE_INK_ERASER_RADIUS = 0.022f

private fun Float.toPdfInkWidth(): Float {
    return (this / PDF_INK_WIDTH_DENOMINATOR).coerceIn(0.0025f, 0.009f)
}
