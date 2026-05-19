package com.example.carrotpdf.pdf

import android.graphics.Bitmap

class PdfPageCache(
    private val maxPages: Int = 5
) {
    private val cache = LinkedHashMap<Int, Bitmap>(maxPages, 0.75f, true)

    fun get(pageIndex: Int): Bitmap? {
        return cache[pageIndex]?.takeIf { !it.isRecycled }
    }

    fun put(pageIndex: Int, bitmap: Bitmap) {
        val oldBitmap = cache.put(pageIndex, bitmap)

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
        while (cache.size > maxPages) {
            val oldestKey = cache.keys.first()
            val bitmap = cache.remove(oldestKey)

            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}