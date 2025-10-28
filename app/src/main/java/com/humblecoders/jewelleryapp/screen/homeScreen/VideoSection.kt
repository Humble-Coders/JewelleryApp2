package com.humblecoders.jewelleryapp.screen.homeScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.humblecoders.jewelleryapp.model.Video

@Composable
fun VideoSection(
    video: Video?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "#RadiantRomance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF896C6C), // Theme color for elegance
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isLoading) {
            // Show skeleton loading
            VideoSkeletonLoading(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            // Always show video player (will use test video if Firebase video is not available)
            VideoPlayerComposable(
                video = video,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
        }
    }
}
