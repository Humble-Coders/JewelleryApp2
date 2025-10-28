package com.humblecoders.jewelleryapp.screen.homeScreen

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.humblecoders.jewelleryapp.utils.VideoCacheManager
import androidx.media3.ui.PlayerView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.humblecoders.jewelleryapp.model.Video

@Composable
fun VideoSkeletonLoading(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Color.Gray.copy(alpha = alpha),
                RoundedCornerShape(12.dp)
            )
    )
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComposable(
    video: Video?,
    modifier: Modifier = Modifier
) {
    // For testing purposes, use a sample video URL if no video is provided
    val testVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val videoUrl = video?.videoUrl?.takeIf { it.isNotEmpty() } ?: testVideoUrl
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Initialize ExoPlayer with caching
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            try {
                exoPlayer?.release()
                
                // Use cached data source from VideoCacheManager
                val dataSourceFactory = VideoCacheManager.getCacheDataSourceFactory(context)
                val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                
                val player = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build()
                    .apply {
                        val mediaItem = MediaItem.Builder()
                            .setUri(Uri.parse(videoUrl))
                            .build()
                        setMediaItem(mediaItem)
                        prepare()
                        
                        // Enable infinite looping
                        repeatMode = Player.REPEAT_MODE_ONE
                        
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                super.onPlaybackStateChanged(playbackState)
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        isLoading = false
                                        hasError = false
                                    }
                                    Player.STATE_BUFFERING -> {
                                        isLoading = true
                                    }
                                    Player.STATE_ENDED -> {
                                    }
                                    Player.STATE_IDLE -> {
                                        isLoading = false
                                    }
                                }
                            }
                            
                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                super.onPlayerError(error)
                                hasError = true
                                isLoading = false
                                Log.e("VideoPlayer", "Player error: ${error.message}", error)
                            }
                            
                            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                                super.onIsPlayingChanged(isPlayingNow)
                            }
                        })
                        
                            // Start muted
                            volume = 0f
                            // Auto-play the video
                            playWhenReady = true
                    }
                
                exoPlayer = player
                
                // Small delay to ensure player is ready
                delay(100)
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error initializing ExoPlayer", e)
                hasError = true
                isLoading = false
            }
        } else {
            hasError = true
            isLoading = false
        }
    }

    // Cleanup ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black) // Add black background to make sure the container is visible
        ) {
            if (videoUrl.isNotEmpty() && !hasError) {
                // Use PlayerView with proper configuration
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false // Hide default controls
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // Hide buffering indicator
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setKeepContentOnPlayerReset(true)
                            // Remove black borders by scaling video to fill
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            // Force redraw
                            invalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { playerView ->
                        playerView.player = exoPlayer
                        playerView.invalidate() // Force redraw
                    }
                )
            } else {
                // Show skeleton loading or error state
                if (isLoading) {
                    VideoSkeletonLoading()
                } else if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No video available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        }
    }
}
