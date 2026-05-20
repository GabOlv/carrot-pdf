package com.example.carrotpdf.ui.viewer.render

import android.graphics.Bitmap

class PdfRenderCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES
) {
    private val cache = LinkedHashMap<PdfRenderKey, Bitmap>(maxEntries, 0.75f, true)

    fun get(key: PdfRenderKey): Bitmap? {
        return cache[key]?.takeIf { bitmap -> !bitmap.isRecycled }
    }

    fun put(
        key: PdfRenderKey,
        bitmap: Bitmap
    ) {
        val oldBitmap = cache.put(key, bitmap)

        if (oldBitmap != null && oldBitmap != bitmap && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }

        trim()
    }

    fun clear() {
        cache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }

        cache.clear()
    }

    private fun trim() {
        while (cache.size > maxEntries) {
            val oldestKey = cache.keys.first()
            val bitmap = cache.remove(oldestKey)

            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private companion object {
        const val DEFAULT_MAX_ENTRIES = 8
    }
}
