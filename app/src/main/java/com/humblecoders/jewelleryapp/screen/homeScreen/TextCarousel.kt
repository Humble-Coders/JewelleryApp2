package com.humblecoders.jewelleryapp.screen.homeScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun JewelryTextCarousel(
    modifier: Modifier = Modifier
) {
    val jewelryTexts = listOf(
        "Elegant Jewels",
        "Timeless Beauty",
        "Precious Moments",
        "Sparkling Elegance",
        "Refined Luxury",
        "Classic Sophistication",
        "Radiant Glamour",
        "Exquisite Craftsmanship"
    )

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-rotate texts every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // 3 seconds
            currentIndex = (currentIndex + 1) % jewelryTexts.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                // Flip animation: slide out up, slide in from bottom
                ContentTransform(
                    targetContentEnter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    ) + fadeIn(animationSpec = tween(400)),
                    initialContentExit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(400)
                    ) + fadeOut(animationSpec = tween(400))
                )
            },
            label = "text_carousel"
        ) { index ->
            Text(
                text = jewelryTexts[index],
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF896C6C), // Theme color for elegance
                textAlign = TextAlign.Center,
                fontSize = 24.sp
            )
        }
    }
}
