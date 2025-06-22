package com.example.jewelleryapp.screen.productDetailScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.snapBackZoomable
import net.engawapg.lib.zoomable.zoomable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImageViewer(
    imageUrls: List<String>,
    currentIndex: Int,
    onImageChange: (Int) -> Unit,
    onFullScreenToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { imageUrls.size }
    )

    // Sync pager state with external currentIndex
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // Notify parent when page changes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            onImageChange(pagerState.currentPage)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val zoomState = rememberZoomState()

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls.getOrNull(page) ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = "Product Image ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .snapBackZoomable(zoomState)
                    .clickable { onFullScreenToggle() },
                contentScale = ContentScale.Fit
            )
        }

        // Navigation arrows (only show if multiple images)
        if (imageUrls.size > 1) {
            // Previous button
            IconButton(
                onClick = {
                    val newIndex = if (currentIndex == 0) imageUrls.size - 1 else currentIndex - 1
                    onImageChange(newIndex)
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .zIndex(1f)
            ) {
                Icon(
                    Icons.Default.ArrowBackIos,
                    contentDescription = "Previous Image",
                    tint = Color.White
                )
            }

            // Next button
            IconButton(
                onClick = {
                    val newIndex = (currentIndex + 1) % imageUrls.size
                    onImageChange(newIndex)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .zIndex(1f)
            ) {
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = "Next Image",
                    tint = Color.White
                )
            }
        }

        // Image indicators (dots)
        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageUrls.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            .clickable { onImageChange(index) }
                    )
                }
            }
        }

        // Fullscreen button
        IconButton(
            onClick = onFullScreenToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .zIndex(1f)
        ) {
            Icon(
                Icons.Default.Fullscreen,
                contentDescription = "Fullscreen",
                tint = Color.White
            )
        }

        // Image counter
        if (imageUrls.size > 1) {
            Text(
                text = "${currentIndex + 1}/${imageUrls.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageDialog(
    imageUrls: List<String>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onImageChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { imageUrls.size }
    )

    // Sync pager state with external currentIndex
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    // Notify parent when page changes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            onImageChange(pagerState.currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                .zIndex(2f)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val zoomState = rememberZoomState()

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls.getOrNull(page) ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = "Fullscreen Image ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .snapBackZoomable(zoomState),
                contentScale = ContentScale.Fit
            )
        }

        // Navigation controls (same as above but for fullscreen)
        if (imageUrls.size > 1) {
            // Previous button
            IconButton(
                onClick = {
                    val newIndex = if (currentIndex == 0) imageUrls.size - 1 else currentIndex - 1
                    onImageChange(newIndex)
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(24.dp)
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .zIndex(1f)
            ) {
                Icon(
                    Icons.Default.ArrowBackIos,
                    contentDescription = "Previous Image",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Next button
            IconButton(
                onClick = {
                    val newIndex = (currentIndex + 1) % imageUrls.size
                    onImageChange(newIndex)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .zIndex(1f)
            ) {
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = "Next Image",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Image indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                imageUrls.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                            .clickable { onImageChange(index) }
                    )
                }
            }
        }
    }
}