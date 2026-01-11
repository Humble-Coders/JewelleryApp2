package com.humblecoders.jewelleryapp.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

object ImageLoaderConfig {
    
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory cache configuration - increase size for smoother scrolling
            .memoryCache {
                MemoryCache.Builder(context)
                    // Use 25% of available memory (increased from default 15-20%)
                    .maxSizePercent(0.25)
                    // Keep strong references to prevent GC
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Disk cache configuration
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    // Larger disk cache - 250MB
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            // Enable memory and disk caching
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Network cache policy
            .networkCachePolicy(CachePolicy.ENABLED)
            // Reduced crossfade duration for faster image appearance (150ms instead of 300ms)
            .crossfade(true)
            .crossfade(150)
            // Respect cache headers
            .respectCacheHeaders(false)
            .build()
    }
}

