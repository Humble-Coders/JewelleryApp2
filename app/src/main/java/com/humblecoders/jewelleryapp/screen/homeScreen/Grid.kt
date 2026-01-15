package com.humblecoders.jewelleryapp.screen.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.humblecoders.jewelleryapp.R

data class JewelryItem(
    val imageUrl: String, // Changed from imageRes: Int to imageUrl: String for Firebase URLs
    val isLarge: Boolean = false,
    val productId: String? = null
)

@Composable
fun JewelryGridItem(
    item: JewelryItem,
    onProductClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context=LocalContext.current
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .then(
                if (item.productId != null) {
                    Modifier.clickable { 
                        item.productId?.let { productId ->
                            onProductClick?.invoke(productId)
                        }
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val gridImageRequest = remember(item.imageUrl) {
                ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .size(600, 600) // Optimize size
                    .allowHardware(true)
                    .memoryCacheKey("grid_${item.imageUrl}")
                    .diskCacheKey("grid_${item.imageUrl}")
                    .build()
            }
            AsyncImage(
                model = gridImageRequest,
                contentDescription = "Jewelry item",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.goldbracelet_homescreen)
            )
        }
    }
}

@Composable
fun ExactPatternJewelryGrid(
    items: List<JewelryItem>,
    modifier: Modifier = Modifier,
    onProductClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F6F4))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial heading
        HeadingWithDividers(
            text = "Editorial",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF896C6C),
            modifier = Modifier.fillMaxWidth()
        )
        // Calculate how many complete patterns we can create (each pattern uses 10 items)
        val patternSize = 10
        val completePatterns = items.size / patternSize
        val remainingItems = items.size % patternSize

        // Create complete patterns
        repeat(completePatterns) { patternIndex ->
            val patternStartIndex = patternIndex * patternSize
            val patternItems = items.subList(patternStartIndex, patternStartIndex + patternSize)

            // Row 1: Large left (2x2) + 2 small right (1x1 each)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large item on left (takes 2/3 width)
                JewelryGridItem(
                    item = patternItems[0],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(2f)
                        .height(320.dp)
                )

                // Two small items on right stacked vertically
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JewelryGridItem(
                        item = patternItems[1],
                        onProductClick = onProductClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp)
                    )
                    JewelryGridItem(
                        item = patternItems[2],
                        onProductClick = onProductClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp)
                    )
                }
            }

            // Row 2: Two equal items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JewelryGridItem(
                    item = patternItems[3],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
                JewelryGridItem(
                    item = patternItems[4],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
            }

            // Row 3: 2 small left + Large right (opposite of row 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Two small items on left stacked vertically
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JewelryGridItem(
                        item = patternItems[5],
                        onProductClick = onProductClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp)
                    )
                    JewelryGridItem(
                        item = patternItems[6],
                        onProductClick = onProductClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(154.dp)
                    )
                }

                // Large item on right (takes 2/3 width)
                JewelryGridItem(
                    item = patternItems[7],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(2f)
                        .height(320.dp)
                )
            }

            // Row 4: Two equal items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JewelryGridItem(
                    item = patternItems[8],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
                JewelryGridItem(
                    item = patternItems[9],
                    onProductClick = onProductClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
            }
        }

        // Handle remaining items if any
        if (remainingItems > 0) {
            val remainingItemsList = items.takeLast(remainingItems)

            // Add remaining items in pairs
            val pairs = remainingItemsList.chunked(2)
            pairs.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JewelryGridItem(
                        item = pair[0],
                        onProductClick = onProductClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                    )
                    if (pair.size > 1) {
                        JewelryGridItem(
                            item = pair[1],
                            onProductClick = onProductClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
