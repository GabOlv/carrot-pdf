package com.example.carrotpdf.model

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class PersistedPdfTabs(
    val tabs: List<PdfTab>,
    val activeTabId: String?
)

object PdfTabPersistence {
    private const val PREFS_NAME = "carrot_pdf_tabs"
    private const val KEY_TABS = "tabs"
    private const val KEY_ACTIVE_TAB_ID = "active_tab_id"

    fun restore(context: Context): PersistedPdfTabs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeTabId = prefs.getString(KEY_ACTIVE_TAB_ID, null)
        val rawTabs = prefs.getString(KEY_TABS, null).orEmpty()

        if (rawTabs.isBlank()) {
            return PersistedPdfTabs(emptyList(), activeTabId = null)
        }

        val tabs = runCatching {
            val array = JSONArray(rawTabs)
            buildList {
                repeat(array.length()) { index ->
                    val item = array.optJSONObject(index) ?: return@repeat
                    val id = item.optString("id").takeIf { it.isNotBlank() }
                        ?: return@repeat
                    val uri = item.optString("uri").takeIf { it.isNotBlank() }
                        ?.let(Uri::parse)
                        ?: return@repeat
                    val title = item.optString("title").takeIf { it.isNotBlank() }
                        ?: "Document.pdf"
                    val currentPageIndex = item.optInt("currentPageIndex", 0)
                        .coerceAtLeast(0)
                    val zoom = item.optDouble("zoom", 1.0)
                        .toFloat()
                        .takeIf { it.isFinite() && it > 0f }
                        ?: 1f
                    val viewportLeft = item.optDouble("viewportLeft", 0.0)
                        .toFloat()
                        .takeIf { it.isFinite() && it >= 0f }
                        ?: 0f
                    val viewportTop = item.optDouble("viewportTop", 0.0)
                        .toFloat()
                        .takeIf { it.isFinite() && it >= 0f }
                        ?: 0f
                    val isMissing = item.optBoolean("isMissing", false)

                    add(
                        PdfTab(
                            id = id,
                            uri = uri,
                            title = title,
                            currentPageIndex = currentPageIndex,
                            zoom = zoom,
                            viewportLeft = viewportLeft,
                            viewportTop = viewportTop,
                            isMissing = isMissing
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        return PersistedPdfTabs(
            tabs = tabs,
            activeTabId = activeTabId?.takeIf { id -> tabs.any { tab -> tab.id == id } }
                ?: tabs.firstOrNull()?.id
        )
    }

    fun save(
        context: Context,
        tabs: List<PdfTab>,
        activeTabId: String?
    ) {
        val array = JSONArray()

        tabs.forEach { tab ->
            array.put(
                JSONObject()
                    .put("id", tab.id)
                    .put("uri", tab.uri.toString())
                    .put("title", tab.title)
                    .put("currentPageIndex", tab.currentPageIndex)
                    .put("zoom", tab.zoom)
                    .put("viewportLeft", tab.viewportLeft)
                    .put("viewportTop", tab.viewportTop)
                    .put("isMissing", tab.isMissing)
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TABS, array.toString())
            .putString(KEY_ACTIVE_TAB_ID, activeTabId)
            .commit()
    }
}
