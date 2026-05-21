package com.example.carrotpdf.data

import android.content.Context
import com.example.carrotpdf.model.PdfBookmark
import com.example.carrotpdf.model.PdfCategory
import org.json.JSONArray
import org.json.JSONObject

data class CarrotLibrarySnapshot(
    val categories: List<PdfCategory>,
    val bookmarks: List<PdfBookmark>
)

class CarrotLibraryStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): CarrotLibrarySnapshot {
        val categories = loadCategories()
        val bookmarks = loadBookmarks()

        return CarrotLibrarySnapshot(
            categories = categories.ifEmpty { listOf(PdfCategory.Default) },
            bookmarks = bookmarks
        )
    }

    fun save(
        categories: List<PdfCategory>,
        bookmarks: List<PdfBookmark>
    ) {
        preferences.edit()
            .putString(KEY_CATEGORIES, categoriesToJson(categories).toString())
            .putString(KEY_BOOKMARKS, bookmarksToJson(bookmarks).toString())
            .apply()
    }

    private fun loadCategories(): List<PdfCategory> {
        val raw = preferences.getString(KEY_CATEGORIES, null) ?: return listOf(PdfCategory.Default)
        val json = runCatching { JSONArray(raw) }.getOrNull() ?: return listOf(PdfCategory.Default)
        val categories = buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue

                add(PdfCategory(id = id, name = name))
            }
        }

        return if (categories.any { it.id == PdfCategory.DEFAULT_ID }) {
            categories
        } else {
            listOf(PdfCategory.Default) + categories
        }
    }

    private fun loadBookmarks(): List<PdfBookmark> {
        val raw = preferences.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        val json = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()

        return buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val tabId = item.optString("tabId").takeIf { it.isNotBlank() } ?: continue
                val uri = item.optString("uri").takeIf { it.isNotBlank() } ?: continue
                val title = item.optString("title").takeIf { it.isNotBlank() } ?: continue
                val categoryId = item.optString("categoryId")
                    .takeIf { it.isNotBlank() }
                    ?: PdfCategory.DEFAULT_ID

                add(
                    PdfBookmark(
                        tabId = tabId,
                        uri = uri,
                        title = title,
                        categoryId = categoryId
                    )
                )
            }
        }
    }

    private fun categoriesToJson(categories: List<PdfCategory>): JSONArray {
        return JSONArray().apply {
            categories.forEach { category ->
                put(
                    JSONObject()
                        .put("id", category.id)
                        .put("name", category.name)
                )
            }
        }
    }

    private fun bookmarksToJson(bookmarks: List<PdfBookmark>): JSONArray {
        return JSONArray().apply {
            bookmarks.forEach { bookmark ->
                put(
                    JSONObject()
                        .put("tabId", bookmark.tabId)
                        .put("uri", bookmark.uri)
                        .put("title", bookmark.title)
                        .put("categoryId", bookmark.categoryId)
                )
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "carrot_library"
        const val KEY_CATEGORIES = "categories"
        const val KEY_BOOKMARKS = "bookmarks"
    }
}
