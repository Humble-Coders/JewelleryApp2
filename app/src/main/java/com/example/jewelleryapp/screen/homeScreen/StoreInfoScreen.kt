package com.example.jewelleryapp.screen.homeScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay
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
                    icon = Icons.AutoMirrored.Filled.Message,
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
                        imageVector = Icons.AutoMirrored.Filled.Message,
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

// ADD these imports to StoreInfoScreen.kt at the top:


// REPLACE the StoreHoursCard function in StoreInfoScreen.kt with this:
@Composable
private fun StoreHoursCard(storeInfo: StoreInfo, todayHours: String?) {
    // Live status calculation
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var isOpen by remember { mutableStateOf(false) }
    var nextStatusChange by remember { mutableStateOf<String?>(null) }
    var timeUntilChange by remember { mutableStateOf<String?>(null) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()

            // Calculate if store is open
            val (open, nextChange, timeUntil) = calculateStoreStatus(storeInfo, currentTime)
            isOpen = open
            nextStatusChange = nextChange
            timeUntilChange = timeUntil

            delay(60000) // Update every minute
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with live status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Store Hours",
                        tint = Color(0xFFB78628),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Store Hours",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live status indicator
                LiveStatusIndicator(
                    isOpen = isOpen,
                    nextStatusChange = nextStatusChange,
                    timeUntilChange = timeUntilChange
                )
            }

            // Current time display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFB78628).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Time",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(currentTime.time),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB78628)
                        )
                    }

                    // Live clock animation
                    LiveClockIndicator()
                }
            }

            // Today's hours highlight
            if (todayHours != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOpen) Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color.Red.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today (${getCurrentDayName()})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = todayHours,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOpen) Color(0xFF4CAF50) else Color.Red
                            )
                        }

                        // Next status change info
                        if (timeUntilChange != null && nextStatusChange != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Next Change",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$nextStatusChange in $timeUntilChange",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Weekly schedule
            Text(
                text = "Weekly Schedule",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )

            val daysOrder = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
            val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

            daysOrder.forEachIndexed { index, day ->
                val hours = storeInfo.storeHours[day]
                val dayName = dayNames[index]
                if (hours != null) {
                    val isToday = day == getCurrentDay()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isToday) Color(0xFFB78628).copy(alpha = 0.05f)
                            else Color.Transparent
                        ),
                        elevation = if (isToday) CardDefaults.cardElevation(defaultElevation = 2.dp)
                        else CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFB78628), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = dayName,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Color(0xFFB78628) else Color.Black,
                                    fontSize = if (isToday) 16.sp else 15.sp
                                )
                            }
                            Text(
                                text = hours,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color(0xFFB78628) else Color.Gray,
                                fontSize = if (isToday) 16.sp else 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ADD these new composables:

@Composable
private fun LiveStatusIndicator(
    isOpen: Boolean,
    nextStatusChange: String?,
    timeUntilChange: String?
) {
    val statusColor = if (isOpen) Color(0xFF4CAF50) else Color.Red
    val statusText = if (isOpen) "OPEN" else "CLOSED"

    // Pulsing animation for the indicator
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(pulseScale)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }

        if (timeUntilChange != null && nextStatusChange != null) {
            Text(
                text = nextStatusChange,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun LiveClockIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing), // 1 minute rotation
            repeatMode = RepeatMode.Restart
        ),
        label = "clock_rotation"
    )

    Canvas(
        modifier = Modifier
            .size(32.dp)
            .scale(0.8f)
    ) {
        val radius = size.minDimension / 2
        val center = size.center

        // Clock circle
        drawCircle(
            color = Color(0xFFB78628),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Hour hand (rotates slowly)
        drawLine(
            color = Color(0xFFB78628),
            start = center,
            end = center.copy(
                x = center.x + (radius * 0.5f * kotlin.math.cos(Math.toRadians((rotation / 12).toDouble())).toFloat()),
                y = center.y + (radius * 0.5f * kotlin.math.sin(Math.toRadians((rotation / 12).toDouble())).toFloat())
            ),
            strokeWidth = 3.dp.toPx()
        )

        // Minute hand (rotates faster)
        drawLine(
            color = Color(0xFFB78628),
            start = center,
            end = center.copy(
                x = center.x + (radius * 0.7f * kotlin.math.cos(Math.toRadians(rotation.toDouble())).toFloat()),
                y = center.y + (radius * 0.7f * kotlin.math.sin(Math.toRadians(rotation.toDouble())).toFloat())
            ),
            strokeWidth = 2.dp.toPx()
        )

        // Center dot
        drawCircle(
            color = Color(0xFFB78628),
            radius = 3.dp.toPx(),
            center = center
        )
    }
}

// ADD these utility functions:

private fun calculateStoreStatus(storeInfo: StoreInfo, currentTime: Calendar): Triple<Boolean, String?, String?> {
    val currentDay = getCurrentDay()
    val todayHours = storeInfo.storeHours[currentDay] ?: return Triple(false, null, null)

    // Parse hours (assuming format like "9:00 AM - 6:00 PM")
    val hoursParts = todayHours.split(" - ")
    if (hoursParts.size != 2) return Triple(false, null, null)

    try {
        val openTime = parseTime(hoursParts[0].trim())
        val closeTime = parseTime(hoursParts[1].trim())

        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val openTotalMinutes = openTime.first * 60 + openTime.second
        val closeTotalMinutes = closeTime.first * 60 + closeTime.second

        val isOpen = currentTotalMinutes in openTotalMinutes until closeTotalMinutes

        // Calculate next status change
        val (nextChange, timeUntil) = if (isOpen) {
            val minutesUntilClose = closeTotalMinutes - currentTotalMinutes
            "Closes" to formatTimeUntil(minutesUntilClose)
        } else if (currentTotalMinutes < openTotalMinutes) {
            val minutesUntilOpen = openTotalMinutes - currentTotalMinutes
            "Opens" to formatTimeUntil(minutesUntilOpen)
        } else {
            // Store is closed for the day, calculate next day opening
            "Opens tomorrow" to null
        }

        return Triple(isOpen, nextChange, timeUntil)
    } catch (e: Exception) {
        return Triple(false, null, null)
    }
}

private fun parseTime(timeStr: String): Pair<Int, Int> {
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    val date = format.parse(timeStr) ?: return Pair(0, 0)
    val calendar = Calendar.getInstance()
    calendar.time = date
    return Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}

private fun formatTimeUntil(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
        }
        else -> {
            val days = minutes / 1440
            "${days}d"
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

private fun getCurrentDayName(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        else -> ""
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
