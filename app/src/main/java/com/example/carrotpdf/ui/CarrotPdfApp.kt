package com.example.carrotpdf.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.R
import com.example.carrotpdf.data.CarrotLibraryStore
import com.example.carrotpdf.model.PdfBookmark
import com.example.carrotpdf.model.PdfCategory
import com.example.carrotpdf.model.PdfTab
import com.example.carrotpdf.pdf.downloadPdf
import com.example.carrotpdf.pdf.getPdfPageCount
import com.example.carrotpdf.pdf.printPdf
import com.example.carrotpdf.pdf.PdfSearchResult
import com.example.carrotpdf.pdf.searchPdfText
import com.example.carrotpdf.pdf.sharePdf
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
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val libraryStore = remember(context) {
        CarrotLibraryStore(context.applicationContext)
    }
    val initialLibrary = remember(libraryStore) {
        libraryStore.load()
    }

    val tabs = remember {
        mutableStateListOf<PdfTab>().apply {
            initialLibrary.bookmarks.forEach { bookmark ->
                add(
                    PdfTab(
                        id = bookmark.tabId,
                        uri = Uri.parse(bookmark.uri),
                        title = bookmark.title
                    )
                )
            }
        }
    }
    val categories = remember {
        mutableStateListOf<PdfCategory>().apply {
            addAll(initialLibrary.categories)
        }
    }
    val bookmarkedTabCategories = remember {
        mutableStateMapOf<String, String>().apply {
            initialLibrary.bookmarks.forEach { bookmark ->
                put(bookmark.tabId, bookmark.categoryId)
            }
        }
    }
    val pinnedTabOrder = remember {
        mutableStateListOf<String>().apply {
            addAll(initialLibrary.bookmarks.map { bookmark -> bookmark.tabId })
        }
    }

    var activeTabId by remember { mutableStateOf(tabs.firstOrNull()?.id) }
    var isFullscreenReader by remember { mutableStateOf(false) }
    var isSettingsModalOpen by remember { mutableStateOf(false) }
    var isCategoriesModalOpen by remember { mutableStateOf(false) }
    var isSearchModalOpen by remember { mutableStateOf(false) }
    var categoryNameInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var closeBookmarkedTabRequest by remember { mutableStateOf<PdfTab?>(null) }
    val searchResults = remember { mutableStateListOf<PdfSearchResult>() }

    var isLoadingDocument by remember { mutableStateOf(false) }

    val activeTab = tabs.firstOrNull { it.id == activeTabId }
    val activeViewerState = rememberActiveViewerState(activeTab)
    val displayedTabs = buildDisplayedTabs(
        tabs = tabs,
        bookmarkedTabIds = bookmarkedTabCategories.keys,
        pinnedTabOrder = pinnedTabOrder
    )

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

    fun persistLibrary() {
        val bookmarks = pinnedTabOrder.mapNotNull { tabId ->
            val tab = tabs.firstOrNull { it.id == tabId } ?: return@mapNotNull null
            val categoryId = bookmarkedTabCategories[tabId] ?: return@mapNotNull null

            PdfBookmark(
                tabId = tab.id,
                uri = tab.uri.toString(),
                title = tab.title,
                categoryId = categoryId
            )
        }

        libraryStore.save(
            categories = categories,
            bookmarks = bookmarks
        )
    }

    val removeBookmark: (String) -> Unit = { tabId ->
        bookmarkedTabCategories.remove(tabId)
        pinnedTabOrder.remove(tabId)
        persistLibrary()
    }

    val closeTab: (String) -> Unit = { tabId ->
        val indexToRemove = tabs.indexOfFirst { it.id == tabId }

        if (indexToRemove >= 0) {
            removeBookmark(tabId)
            tabs.removeAt(indexToRemove)

            if (activeTabId == tabId) {
                activeTabId = tabs.getOrNull(indexToRemove)?.id
                    ?: tabs.getOrNull(indexToRemove - 1)?.id
            }
        }
    }

    val bookmarkActiveTab: (String) -> Unit = { categoryId ->
        val tab = activeTab

        if (tab != null) {
            bookmarkedTabCategories[tab.id] = categoryId

            if (tab.id !in pinnedTabOrder) {
                pinnedTabOrder.add(tab.id)
            }

            persistLibrary()
        }
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
                        tabs = displayedTabs,
                        activeTabId = activeTabId,
                        isBookmarked = { tabId ->
                            bookmarkedTabCategories.containsKey(tabId)
                        },
                        onSelectTab = { id ->
                            activeTabId = id
                        },
                        onCloseTab = { id ->
                            val tab = tabs.firstOrNull { it.id == id }

                            if (tab != null && bookmarkedTabCategories.containsKey(id)) {
                                closeBookmarkedTabRequest = tab
                            } else {
                                closeTab(id)
                            }
                        },
                        onMoveTab = { tabId, visibleIndex ->
                            moveDisplayedTab(
                                tabs = tabs,
                                pinnedTabOrder = pinnedTabOrder,
                                bookmarkedTabIds = bookmarkedTabCategories.keys,
                                displayedTabs = displayedTabs,
                                tabId = tabId,
                                targetVisibleIndex = visibleIndex
                            )
                            persistLibrary()
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
                        isBookmarked = activeTab?.id?.let { bookmarkedTabCategories.containsKey(it) } == true,
                        onBookmarkClick = {
                            val tab = activeTab

                            if (tab != null) {
                                if (bookmarkedTabCategories.containsKey(tab.id)) {
                                    closeBookmarkedTabRequest = tab
                                } else {
                                    isCategoriesModalOpen = true
                                }
                            }
                        },
                        onCategoriesClick = {
                            isCategoriesModalOpen = true
                        },
                        onEditClick = {},
                        onSearchClick = {
                            isSearchModalOpen = true
                        },
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
                    onOpenPdf = {
                        isSettingsModalOpen = false
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
                                if (success) "PDF baixado" else "Nao foi possivel baixar",
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
                        isSettingsModalOpen = false
                    }
                )
            }

            if (isCategoriesModalOpen) {
                CategoriesModal(
                    activeTab = activeTab,
                    categories = categories,
                    bookmarkedCategoryId = activeTab?.id?.let { bookmarkedTabCategories[it] },
                    categoryNameInput = categoryNameInput,
                    onCategoryNameChange = { categoryNameInput = it },
                    onCreateCategory = {
                        val name = categoryNameInput.trim()

                        if (name.isNotBlank()) {
                            val category = PdfCategory(name = name)
                            categories.add(category)
                            categoryNameInput = ""
                            bookmarkActiveTab(category.id)
                            persistLibrary()
                            isCategoriesModalOpen = false
                        }
                    },
                    onSelectCategory = { category ->
                        bookmarkActiveTab(category.id)
                        isCategoriesModalOpen = false
                    },
                    onDismiss = {
                        isCategoriesModalOpen = false
                    }
                )
            }

            if (isSearchModalOpen) {
                SearchModal(
                    activeTab = activeTab,
                    query = searchQuery,
                    isSearching = isSearching,
                    results = searchResults,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        val tab = activeTab ?: return@SearchModal

                        coroutineScope.launch {
                            isSearching = true
                            val results = withContext(Dispatchers.IO) {
                                searchPdfText(
                                    context = context.applicationContext,
                                    uri = tab.uri,
                                    query = searchQuery
                                )
                            }
                            searchResults.clear()
                            searchResults.addAll(results)
                            isSearching = false
                        }
                    },
                    onOpenResult = { result ->
                        activeViewerState?.requestScrollToPage(result.pageIndex)
                        isSearchModalOpen = false
                    },
                    onDismiss = {
                        isSearchModalOpen = false
                    }
                )
            }

            if (closeBookmarkedTabRequest != null) {
                ConfirmRemoveBookmarkDialog(
                    tab = closeBookmarkedTabRequest,
                    onConfirm = { tab ->
                        closeBookmarkedTabRequest = null
                        closeTab(tab.id)
                    },
                    onDismiss = {
                        closeBookmarkedTabRequest = null
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
                text = "Carrot PDF",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsActionCard(
                    iconResId = R.drawable.ic_action_open_pdf,
                    title = "Abrir PDF",
                    subtitle = "Escolher um arquivo local",
                    onClick = onOpenPdf
                )
                SettingsActionCard(
                    iconResId = R.drawable.ic_action_share,
                    title = "Enviar Arquivo",
                    subtitle = "Compartilhar uma copia",
                    onClick = onSharePdf
                )
                SettingsActionCard(
                    iconResId = R.drawable.ic_action_download,
                    title = "Baixar",
                    subtitle = "Salvar uma copia local",
                    onClick = onDownloadPdf
                )
                SettingsActionCard(
                    iconResId = R.drawable.ic_action_print,
                    title = "Imprimir",
                    subtitle = "Enviar para impressora",
                    onClick = onPrintPdf
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
private fun SettingsActionCard(
    iconResId: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = CarrotColors.Background
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = CarrotColors.AccentSoft,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = title,
                    tint = CarrotColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = CarrotColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CategoriesModal(
    activeTab: PdfTab?,
    categories: List<PdfCategory>,
    bookmarkedCategoryId: String?,
    categoryNameInput: String,
    onCategoryNameChange: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onSelectCategory: (PdfCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Categories",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = activeTab?.title ?: "Open a PDF before bookmarking.",
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                categories.forEach { category ->
                    val isSelected = category.id == bookmarkedCategoryId

                    CategoryRow(
                        category = category,
                        isSelected = isSelected,
                        isEnabled = activeTab != null,
                        onClick = {
                            onSelectCategory(category)
                        }
                    )
                }

                OutlinedTextField(
                    value = categoryNameInput,
                    onValueChange = onCategoryNameChange,
                    label = {
                        Text("New category")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onCreateCategory,
                    enabled = activeTab != null && categoryNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarrotColors.Accent,
                        contentColor = CarrotColors.Background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create and bookmark")
                }
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
private fun SearchModal(
    activeTab: PdfTab?,
    query: String,
    isSearching: Boolean,
    results: List<PdfSearchResult>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenResult: (PdfSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Search PDF",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = activeTab?.title ?: "Open a PDF before searching.",
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = {
                        Text("Search current PDF")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onSearch,
                    enabled = activeTab != null && query.isNotBlank() && !isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CarrotColors.Accent,
                        contentColor = CarrotColors.Background
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSearching) "Searching..." else "Search")
                }

                Text(
                    text = when {
                        isSearching -> "Scanning pages..."
                        results.isEmpty() -> "No results yet"
                        else -> "${results.size} result${if (results.size == 1) "" else "s"}"
                    },
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )

                results.take(6).forEach { result ->
                    SearchResultRow(
                        result = result,
                        onClick = {
                            onOpenResult(result)
                        }
                    )
                }
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
private fun SearchResultRow(
    result: PdfSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = CarrotColors.Background
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Page ${result.pageIndex + 1}",
                color = CarrotColors.Accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = result.snippet,
                color = CarrotColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: PdfCategory,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) CarrotColors.AccentSoft else CarrotColors.Background,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = isEnabled) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    color = if (isSelected) CarrotColors.Accent else CarrotColors.SurfaceAlt,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (category.id == PdfCategory.DEFAULT_ID) {
                        R.drawable.ic_action_default
                    } else {
                        R.drawable.ic_action_book
                    }
                ),
                contentDescription = category.name,
                tint = if (isSelected) CarrotColors.Background else CarrotColors.Accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.name,
            color = if (isSelected) CarrotColors.Accent else CarrotColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ConfirmRemoveBookmarkDialog(
    tab: PdfTab?,
    onConfirm: (PdfTab) -> Unit,
    onDismiss: () -> Unit
) {
    if (tab == null) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Remove bookmark?",
                color = CarrotColors.TextPrimary
            )
        },
        text = {
            Text(
                text = "${tab.title} will be removed from bookmarks and closed.",
                color = CarrotColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(tab)
                }
            ) {
                Text(
                    text = "Remove",
                    color = CarrotColors.Accent
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = CarrotColors.TextSecondary
                )
            }
        },
        containerColor = CarrotColors.Surface
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

private fun buildDisplayedTabs(
    tabs: List<PdfTab>,
    bookmarkedTabIds: Set<String>,
    pinnedTabOrder: List<String>
): List<PdfTab> {
    val pinnedTabs = pinnedTabOrder
        .mapNotNull { tabId -> tabs.firstOrNull { tab -> tab.id == tabId } }
        .filter { tab -> tab.id in bookmarkedTabIds }
    val pinnedIds = pinnedTabs.mapTo(mutableSetOf()) { tab -> tab.id }
    val regularTabs = tabs.filterNot { tab -> tab.id in pinnedIds }

    return pinnedTabs + regularTabs
}

private fun moveDisplayedTab(
    tabs: MutableList<PdfTab>,
    pinnedTabOrder: MutableList<String>,
    bookmarkedTabIds: Set<String>,
    displayedTabs: List<PdfTab>,
    tabId: String,
    targetVisibleIndex: Int
) {
    if (displayedTabs.isEmpty()) {
        return
    }

    val safeTarget = targetVisibleIndex.coerceIn(0, displayedTabs.lastIndex)

    if (tabId in bookmarkedTabIds) {
        val pinnedIds = displayedTabs
            .filter { tab -> tab.id in bookmarkedTabIds }
            .map { tab -> tab.id }
        val safePinnedTarget = safeTarget.coerceIn(0, (pinnedIds.size - 1).coerceAtLeast(0))

        pinnedTabOrder.remove(tabId)
        pinnedTabOrder.add(safePinnedTarget, tabId)
        return
    }

    val targetTab = displayedTabs[safeTarget]
    val fromIndex = tabs.indexOfFirst { tab -> tab.id == tabId }
    val toIndex = tabs.indexOfFirst { tab -> tab.id == targetTab.id }

    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
        return
    }

    val tab = tabs.removeAt(fromIndex)
    val adjustedTarget = if (fromIndex < toIndex) {
        toIndex - 1
    } else {
        toIndex
    }

    tabs.add(adjustedTarget.coerceIn(0, tabs.size), tab)
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

private const val ZOOM_CHIP_VISIBLE_MS = 1200L
