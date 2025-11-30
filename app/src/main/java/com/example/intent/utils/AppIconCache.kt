package com.example.intent.utils

import android.graphics.drawable.Drawable
import android.util.LruCache

object AppIconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of the available memory for this memory cache.
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Drawable>(cacheSize) {
        override fun sizeOf(key: String, drawable: Drawable): Int {
            // The cache size will be measured in kilobytes rather than
            // number of items.
            // Estimate size: width * height * 4 bytes / 1024
            val bitmapWidth = drawable.intrinsicWidth
            val bitmapHeight = drawable.intrinsicHeight
            return if (bitmapWidth > 0 && bitmapHeight > 0) {
                (bitmapWidth * bitmapHeight * 4) / 1024
            } else {
                1 // Fallback for unknown size
            }
        }
    }

    fun getIcon(packageName: String): Drawable? {
        return memoryCache.get(packageName)
    }

    fun putIcon(packageName: String, drawable: Drawable) {
        if (getIcon(packageName) == null) {
            memoryCache.put(packageName, drawable)
        }
    }
}
