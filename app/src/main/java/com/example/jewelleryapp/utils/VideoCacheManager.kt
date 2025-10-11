package com.example.jewelleryapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.database.StandaloneDatabaseProvider
import java.io.File

object VideoCacheManager {
    private const val TAG = "VideoCacheManager"
    private const val CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    private var cache: SimpleCache? = null
    private var preloadPlayer: ExoPlayer? = null
    
    fun initializeCache(context: Context) {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            cache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(CACHE_SIZE),
                StandaloneDatabaseProvider(context)
            )
            
            Log.d(TAG, "Video cache initialized with ${CACHE_SIZE / (1024 * 1024)}MB limit")
        }
    }
    
    fun preloadVideo(context: Context, videoUrl: String) {
        if (videoUrl.isEmpty()) return
        
        try {
            Log.d(TAG, "Starting video preload: $videoUrl")
            
            initializeCache(context)
            
            // Create cached data source
            val dataSourceFactory = CacheDataSource.Factory()
                .setCache(cache!!)
                .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Create preload player
            preloadPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(videoUrl))
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = false // Don't auto-play during preload
                    volume = 0f // Silent preload
                }
            
            Log.d(TAG, "Video preload started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading video", e)
        }
    }
    
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        initializeCache(context)
        return CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
    
    fun releasePreloadPlayer() {
        preloadPlayer?.release()
        preloadPlayer = null
        Log.d(TAG, "Preload player released")
    }
    
    fun clearCache() {
        cache?.let { cache ->
            try {
                cache.release()
                this.cache = null
                Log.d(TAG, "Video cache cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
            }
        }
    }
}
