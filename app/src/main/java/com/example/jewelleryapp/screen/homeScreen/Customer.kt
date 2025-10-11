package com.example.jewelleryapp.screen.homeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import com.example.jewelleryapp.R

data class Testimonial(
    val name: String,
    val age: Int,
    val imageRes: Int,
    val text: String,
    val rotation: Float = 0f,
    val clipPosition: Float = 0.5f, // Position along the string (0.0 to 1.0)
    val stringDepth: Float = 0.3f   // How much the string sags (0.1 to 0.5)
)

@Composable
fun CurvedStringBackground(
    testimonials: List<Testimonial>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val stringColor = Color(0xFF896C6C)
        val stringThickness = 2.dp.toPx()
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate positions for each testimonial
        val cardPositions = testimonials.mapIndexed { index, testimonial ->
            val progress = if (testimonials.size == 1) 0.5f else index.toFloat() / (testimonials.size - 1)
            progress * canvasWidth
        }

        // Draw curved string segments between cards
        if (testimonials.isNotEmpty()) {
            val path = Path()

            // Start from left edge - lower to match clips
            val startY = canvasHeight * 0.25f
            path.moveTo(0f, startY)

            // Create curves between each card position
            for (i in cardPositions.indices) {
                val currentX = cardPositions[i]
                val currentDepth = testimonials[i].stringDepth
                // String touches the clip position - lower to align with clips
                val sagY = canvasHeight * (0.5f + currentDepth * 0.3f)

                if (i == 0) {
                    // Curve from start to first card
                    val controlX = currentX * 0.5f
                    val controlY = (startY + sagY) * 0.5f
                    path.quadraticBezierTo(controlX, controlY, currentX, sagY)
                } else {
                    // Curve from previous card to current card
                    val prevX = cardPositions[i - 1]
                    val prevDepth = testimonials[i - 1].stringDepth
                    val prevSagY = canvasHeight * (0.4f + prevDepth * 0.4f)

                    val midX = (prevX + currentX) * 0.5f
                    val midY = (prevSagY + sagY) * 0.5f + canvasHeight * 0.08f

                    path.quadraticBezierTo(midX, midY, currentX, sagY)
                }

                if (i == cardPositions.lastIndex) {
                    // Curve from last card to end
                    val controlX = currentX + (canvasWidth - currentX) * 0.5f
                    val controlY = (sagY + startY) * 0.5f
                    path.quadraticBezierTo(controlX, controlY, canvasWidth, startY)
                }
            }

            // Draw the main string path
            drawPath(
                path = path,
                color = stringColor,
                style = Stroke(width = stringThickness, cap = StrokeCap.Round)
            )

            // Add string texture with thinner parallel lines
            val texturePath1 = Path()
            val texturePath2 = Path()

            // Recreate paths with slight offsets for texture
            texturePath1.moveTo(0f, startY - stringThickness * 0.3f)
            texturePath2.moveTo(0f, startY + stringThickness * 0.3f)

            for (i in cardPositions.indices) {
                val currentX = cardPositions[i]
                val currentDepth = testimonials[i].stringDepth
                val sagY = canvasHeight * (0.5f + currentDepth * 0.3f)

                if (i == 0) {
                    val controlX = currentX * 0.5f
                    val controlY = (startY + sagY) * 0.5f
                    texturePath1.quadraticBezierTo(controlX, controlY - stringThickness * 0.3f, currentX, sagY - stringThickness * 0.3f)
                    texturePath2.quadraticBezierTo(controlX, controlY + stringThickness * 0.3f, currentX, sagY + stringThickness * 0.3f)
                } else {
                    val prevX = cardPositions[i - 1]
                    val prevDepth = testimonials[i - 1].stringDepth
                    val prevSagY = canvasHeight * (0.4f + prevDepth * 0.4f)

                    val midX = (prevX + currentX) * 0.5f
                    val midY = (prevSagY + sagY) * 0.5f + canvasHeight * 0.08f

                    texturePath1.quadraticBezierTo(midX, midY - stringThickness * 0.3f, currentX, sagY - stringThickness * 0.3f)
                    texturePath2.quadraticBezierTo(midX, midY + stringThickness * 0.3f, currentX, sagY + stringThickness * 0.3f)
                }

                if (i == cardPositions.lastIndex) {
                    val controlX = currentX + (canvasWidth - currentX) * 0.5f
                    val controlY = (sagY + startY) * 0.5f
                    texturePath1.quadraticBezierTo(controlX, controlY - stringThickness * 0.3f, canvasWidth, startY - stringThickness * 0.3f)
                    texturePath2.quadraticBezierTo(controlX, controlY + stringThickness * 0.3f, canvasWidth, startY + stringThickness * 0.3f)
                }
            }

            drawPath(
                path = texturePath1,
                color = stringColor.copy(alpha = 0.6f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = texturePath2,
                color = stringColor.copy(alpha = 0.6f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HangingTestimonialCard(
    testimonial: Testimonial,
    modifier: Modifier = Modifier,
    clipDrawableRes: Int = R.drawable.paper_clip // Vector drawable resource
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .height(450.dp), // Ensure full height for the card and clip
        contentAlignment = Alignment.TopCenter
    ) {
        // Paper clip from vector drawable - positioned to touch the string
        Image(
            painter = painterResource(id = clipDrawableRes),
            contentDescription = "Paper clip",
            modifier = Modifier
                .size(32.dp, 48.dp)
                .offset(
                    x = ((testimonial.clipPosition - 0.5f) * 120).dp,
                    y = (50 + testimonial.stringDepth * 25).dp // Aligned with lower string position
                )
                .rotate(testimonial.rotation * 0.3f + (testimonial.clipPosition - 0.5f) * 15f),
            contentScale = ContentScale.Fit
        )

        // The hanging card - positioned lower to account for clip connection
        Card(
            modifier = Modifier
                .width(260.dp)
                .height(360.dp)
                .offset(y = (85 + testimonial.stringDepth * 40).dp) // Positioned relative to lower string
                .rotate(testimonial.rotation)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFAF8F6)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image section with vintage photo border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFF8F8F8))
                    ) {
                        Image(
                            painter = painterResource(id = testimonial.imageRes),
                            contentDescription = "Customer photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name and age
                Text(
                    text = "${testimonial.name}, ${testimonial.age}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D3748),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Testimonial text
                Text(
                    text = testimonial.text,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CustomerTestimonialsWithCurvedString(
    testimonials: List<Testimonial>,
    modifier: Modifier = Modifier,
    clipDrawableRes: Int = R.drawable.paper_clip // Add your paper clip vector drawable
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F5F3))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        // Header
//        Text(
//            text = "Customer Testimonials",
//            fontSize = 16.sp,
//            fontWeight = FontWeight.Medium,
//            color = Color(0xFFD4B896),
//            modifier = Modifier.padding(bottom = 8.dp)
//        )

        // Container for string and cards
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp) // Increased height to accommodate lower string and full cards
        ) {
            // Curved string background
            CurvedStringBackground(
                testimonials = testimonials,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-20).dp) // Move string slightly up to better align with clips
            )

            // Testimonial cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 0.dp)
            ) {
                itemsIndexed(testimonials) { index, testimonial ->
                    HangingTestimonialCard(
                        testimonial = testimonial,
                        clipDrawableRes = clipDrawableRes
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CurvedStringTestimonialsPreview() {
    val sampleTestimonials = listOf(
        Testimonial(
            name = "Akanksha Khanna",
            age = 27,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "Obsessed with my engagement ring, my husband chose perfectly and it's everything I wanted in a ring. Handcrafted with love!",
            rotation = -6f,
            clipPosition = 0.3f,
            stringDepth = 0.25f
        ),
        Testimonial(
            name = "Nutan Mishra",
            age = 33,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "I got a necklace for my baby boy from this brand and it's so beautiful! It gave me happiness and security knowing it's pure.",
            rotation = 4f,
            clipPosition = 0.7f,
            stringDepth = 0.35f
        ),
        Testimonial(
            name = "Sarah Johnson",
            age = 28,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "Amazing quality and beautiful designs. The customer service was exceptional and I couldn't be happier!",
            rotation = -2f,
            clipPosition = 0.4f,
            stringDepth = 0.2f
        ),
        Testimonial(
            name = "Priya Sharma",
            age = 25,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "The jewelry is stunning and exactly what I was looking for. Fast delivery and beautiful packaging too!",
            rotation = 5f,
            clipPosition = 0.6f,
            stringDepth = 0.4f
        )
    )

    MaterialTheme {
        CustomerTestimonialsWithCurvedString(
            testimonials = sampleTestimonials,
            clipDrawableRes = R.drawable.paper_clip
        )
    }
}