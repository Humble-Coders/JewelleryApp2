package com.humblecoders.jewelleryapp.screen.homeScreen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.humblecoders.jewelleryapp.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class Testimonial(
    val name: String,
    val age: Int,
    val imageUrl: String, // Changed from imageRes: Int to imageUrl: String for Firebase URLs
    val text: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverFlowTestimonialCard(
    testimonial: Testimonial,
    pageOffset: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(260.dp)
            .height(360.dp)
            .graphicsLayer {
                // Calculate the absolute offset
                val absOffset = pageOffset.absoluteValue

                // Scale effect: center card is larger, side cards are clearly visible
                val scale = lerp(
                    start = 0.80f,
                    stop = 1f,
                    fraction = 1f - absOffset.coerceIn(0f, 1f)
                )
                scaleX = scale
                scaleY = scale

                // Minimal rotation for subtle 3D effect
                rotationY = pageOffset * 20f

                // Alpha/transparency effect - side cards fully visible
                alpha = lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - absOffset.coerceIn(0f, 1f)
                )

                // Minimal translation for subtle depth
                translationX = pageOffset * -30f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAF8F6)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image section with vintage photo border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF8F8F8))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(testimonial.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Customer photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.goldbracelet_homescreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Name and age
            Text(
                text = "${testimonial.name}, ${testimonial.age}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2D3748),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Testimonial text
            Text(
                text = testimonial.text,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerTestimonialsWithCurvedString(
    testimonials: List<Testimonial>,
    modifier: Modifier = Modifier,
    clipDrawableRes: Int = R.drawable.paper_clip // Keep for compatibility but not used
) {
    if (testimonials.isEmpty()) return

    // Create a large virtual list for infinite scrolling effect
    val virtualPageCount = Int.MAX_VALUE
    val initialPage = virtualPageCount / 2

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll effect - slow and smooth
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Wait 5 seconds
            coroutineScope.launch {
                val nextPage = pagerState.currentPage + 1
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F5F3))
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Customer Testimonials",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF896C6C),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Cover Flow Carousel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(260.dp),
                contentPadding = PaddingValues(horizontal = 50.dp),
                pageSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                // Map virtual page to actual testimonial
                val actualIndex = page % testimonials.size
                val testimonial = testimonials[actualIndex]

                // Calculate page offset for animations
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                CoverFlowTestimonialCard(
                    testimonial = testimonial,
                    pageOffset = pageOffset
                )
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(testimonials.size) { index ->
                val currentIndex = pagerState.currentPage % testimonials.size
                Box(
                    modifier = Modifier
                        .size(if (currentIndex == index) 10.dp else 8.dp)
                        .background(
                            color = if (currentIndex == index) Color(0xFF896C6C) else Color(0xFFD4B896),
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoverFlowTestimonialsPreview() {
    val sampleTestimonials = listOf(
        Testimonial(
            name = "Akanksha Khanna",
            age = 27,
            imageUrl = "",
            text = "Obsessed with my engagement ring, my husband chose perfectly and it's everything I wanted in a ring. Handcrafted with love!"
        ),
        Testimonial(
            name = "Nutan Mishra",
            age = 33,
            imageUrl = "",
            text = "I got a necklace for my baby boy from this brand and it's so beautiful! It gave me happiness and security knowing it's pure."
        ),
        Testimonial(
            name = "Sarah Johnson",
            age = 28,
            imageUrl = "",
            text = "Amazing quality and beautiful designs. The customer service was exceptional and I couldn't be happier!"
        ),
        Testimonial(
            name = "Priya Sharma",
            age = 25,
            imageUrl = "",
            text = "The jewelry is stunning and exactly what I was looking for. Fast delivery and beautiful packaging too!"
        )
    )

    MaterialTheme {
        CustomerTestimonialsWithCurvedString(
            testimonials = sampleTestimonials,
            clipDrawableRes = R.drawable.paper_clip
        )
    }
}