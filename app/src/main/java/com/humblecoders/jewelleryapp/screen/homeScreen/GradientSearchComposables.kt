package com.humblecoders.jewelleryapp.screen.homeScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.humblecoders.jewelleryapp.R
import com.humblecoders.jewelleryapp.model.Category

// Helper composables for gradient search
@Composable
fun GradientSearchItem(
    category: Category,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val searchCategoryImageRequest = remember(category.id, category.imageUrl) {
                ImageRequest.Builder(context)
                    .data(category.imageUrl)
                    .crossfade(true)
                    .size(80, 80)
                    .allowHardware(true)
                    .memoryCacheKey("search_category_${category.id}")
                    .diskCacheKey("search_category_${category.id}")
                    .build()
            }
            AsyncImage(
                model = searchCategoryImageRequest,
                contentDescription = category.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.necklace_homescreen)
            )

            Text(
                text = category.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.ui.graphics.Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go to category",
                tint = androidx.compose.ui.graphics.Color(0xFF896C6C),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GradientCategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    val context=LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        val gradientCategoryImageRequest = remember(category.id, category.imageUrl) {
            ImageRequest.Builder(context)
                .data(category.imageUrl)
                .crossfade(true)
                .size(120, 120)
                .allowHardware(true)
                .memoryCacheKey("gradient_category_${category.id}")
                .diskCacheKey("gradient_category_${category.id}")
                .build()
        }
        AsyncImage(
            model = gradientCategoryImageRequest,
            contentDescription = category.name,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.necklace_homescreen)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color(0xFF896C6C)
        )
    }
}
