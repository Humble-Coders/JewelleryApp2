package com.example.jewelleryapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class JewelryApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FCMHelper.initializeFCM()

        // Any other app initialization code
    }

    override fun newImageLoader(): ImageLoader {
        // Create an optimized OkHttpClient for Coil
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Increased timeout for slower connections
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Retry automatically on connection issues
            .build()

        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.25) // Use 25% of available disk space
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this) // Pass 'this' as context to MemoryCache.Builder
                    .maxSizePercent(0.25) // Use 25% of app memory
                    .build()
            }
            .okHttpClient(okHttpClient) // Use the optimized HTTP client
            .fetcherDispatcher(Dispatchers.IO) // Use IO dispatcher for fetching
            .decoderDispatcher(Dispatchers.Default) // Use Default dispatcher for decoding
            .callFactory(okHttpClient) // Use same client for all calls
            .crossfade(true) // Enable crossfade animation between images
            .respectCacheHeaders(false) // Ignore cache headers from network
            .allowRgb565(true) // Use smaller image format when possible for better performance
            .build()
    }
}