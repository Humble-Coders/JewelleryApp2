package com.example.jewelleryapp.screen.storeInfoScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.model.StoreInfo
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInfoScreen(
    viewModel: StoreInfoViewModel,
    navController: NavController,
    onBackClick: () -> Unit
) {
    val storeInfo by viewModel.storeInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Information") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFB78628))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $error", color = Color.Red)
                }
            }
            storeInfo != null -> {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StoreImagesSection(storeInfo!!.storeImages)
                    StoreDetailsCard(
                        storeInfo = storeInfo!!,
                        onGetDirectionsClick = { viewModel.openGoogleMaps(context) }
                    )
                    ContactInfoCard(
                        storeInfo = storeInfo!!,
                        onWhatsAppClick = { viewModel.openWhatsApp(context) }
                    )
                    StoreHoursCard(storeInfo!!, viewModel.getCurrentDayHours())
                }
            }
        }
    }
}

@Composable
private fun StoreImagesSection(images: List<String>) {
    if (images.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyRow(
                modifier = Modifier.height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(images) { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Store Image",
                        modifier = Modifier
                            .width(250.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreDetailsCard(storeInfo: StoreInfo, onGetDirectionsClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = storeInfo.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB78628)
            )

            if (storeInfo.establishedYear.isNotEmpty()) {
                Text(
                    text = "Established ${storeInfo.establishedYear}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Address",
                value = storeInfo.address
            )

            Button(
                onClick = onGetDirectionsClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB78628)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Get Directions",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get Directions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ContactInfoCard(storeInfo: StoreInfo, onWhatsAppClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Contact Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (storeInfo.phonePrimary.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "Primary Phone",
                    value = storeInfo.phonePrimary
                )
            }

            if (storeInfo.phoneSecondary.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "Secondary Phone",
                    value = storeInfo.phoneSecondary
                )
            }

            if (storeInfo.whatsappNumber.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Message,
                    label = "WhatsApp",
                    value = storeInfo.whatsappNumber
                )
            }

            if (storeInfo.email.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = storeInfo.email
                )
            }

            if (storeInfo.whatsappNumber.isNotEmpty()) {
                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat on WhatsApp",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreHoursCard(storeInfo: StoreInfo, todayHours: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Store Hours",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (todayHours != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB78628).copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "Today: $todayHours",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB78628),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            val daysOrder = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
            daysOrder.forEach { day ->
                val hours = storeInfo.storeHours[day]
                if (hours != null) {
                    val isToday = day == getCurrentDay()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.replaceFirstChar { it.uppercase() },
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) Color(0xFFB78628) else Color.Black
                        )
                        Text(
                            text = hours,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) Color(0xFFB78628) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFB78628),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

private fun getCurrentDay(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "sunday"
        Calendar.MONDAY -> "monday"
        Calendar.TUESDAY -> "tuesday"
        Calendar.WEDNESDAY -> "wednesday"
        Calendar.THURSDAY -> "thursday"
        Calendar.FRIDAY -> "friday"
        Calendar.SATURDAY -> "saturday"
        else -> ""
    }
}