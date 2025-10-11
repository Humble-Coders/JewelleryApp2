package com.example.jewelleryapp.screen.homeScreen

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
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Category

// Helper composables for gradient search
@Composable
fun GradientSearchItem(
    category: Category,
    onClick: () -> Unit
) {
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(category.imageUrl)
                    .crossfade(true)
                    .build(),
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(category.imageUrl)
                .crossfade(true)
                .build(),
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
