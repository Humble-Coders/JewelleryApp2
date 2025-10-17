package com.example.jewelleryapp.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jewelleryapp.model.BookingDoc
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// App theme colors
private val ThemeColor = Color(0xFF896C6C)
private val ThemeColorLight = Color(0xFFEEDDCA)
private val Gold = Color(0xFFE4BE67)
private val NavyBlue = Color(0xFF0E1A3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationHistoryScreen(
    navController: NavController,
    viewModel: VideoBookingViewModel
) {
    val consultationHistory by viewModel.consultationHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showBookingDetailsDialog by remember { mutableStateOf<BookingDoc?>(null) }

    LaunchedEffect(Unit) { 
        viewModel.loadConsultationHistory() 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Consultation History", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColor
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ThemeColorLight.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your Consultation History",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Complete history of your video consultations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ThemeColor)
                    }
                } else if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (consultationHistory.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = ThemeColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Consultation History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "You haven't had any video consultations yet. Book your first consultation to get started!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(consultationHistory) { booking ->
                            ConsultationHistoryCard(
                                booking = booking,
                                onBookingClick = { bookingDoc ->
                                    if (bookingDoc.status.lowercase() == "confirmed") {
                                        showBookingDetailsDialog = bookingDoc
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Booking Details Dialog
        showBookingDetailsDialog?.let { booking ->
            BookingDetailsDialog(
                booking = booking,
                onDismiss = { showBookingDetailsDialog = null }
            )
        }
    }
}

@Composable
private fun ConsultationHistoryCard(
    booking: BookingDoc,
    onBookingClick: (BookingDoc) -> Unit
) {
    val dateOnly = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(booking.startTime.seconds * 1000))
    val timeOnly = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(booking.startTime.seconds * 1000))
    val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(booking.endTime.seconds * 1000))
    val createdDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(booking.createdAt.seconds * 1000))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = booking.status.lowercase() == "confirmed") {
                if (booking.status.lowercase() == "confirmed") {
                    onBookingClick(booking)
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Consultation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColor
                )
                Box(
                    modifier = Modifier
                        .background(
                            when (booking.status.lowercase()) {
                                "confirmed" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                "completed" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                "cancelled" -> Color(0xFFF44336).copy(alpha = 0.1f)
                                else -> ThemeColor.copy(alpha = 0.1f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = booking.status.replaceFirstChar { it.uppercase() },
                        color = when (booking.status.lowercase()) {
                            "confirmed" -> Color(0xFF4CAF50)
                            "completed" -> Color(0xFF2196F3)
                            "cancelled" -> Color(0xFFF44336)
                            else -> ThemeColor
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date and time info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = ThemeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = dateOnly,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "$timeOnly - $endTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Booking created date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = ThemeColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Booked on: $createdDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Booking ID
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Tag,
                    contentDescription = null,
                    tint = ThemeColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ID: ${booking.docId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun BookingDetailsDialog(
    booking: BookingDoc,
    onDismiss: () -> Unit
) {
    val df = remember { SimpleDateFormat("EEE, dd MMM yyyy 'at' HH:mm", Locale.getDefault()) }
    val start = remember(booking.startTime) { df.format(Date(booking.startTime.seconds * 1000)) }
    val end = remember(booking.endTime) { df.format(Date(booking.endTime.seconds * 1000)) }
    val createdDate = remember(booking.createdAt) { 
        SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date(booking.createdAt.seconds * 1000)) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Booking Confirmed!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        },
        text = {
            Column {
                Text(
                    "🎉 Your video consultation is confirmed and ready!",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Booking Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeColorLight.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "📋 Booking Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Date and Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = ThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "📅 Date: $start",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = ThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "⏰ Duration: ${start.split(" ").last()} - ${end.split(" ").last()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Tag,
                                contentDescription = null,
                                tint = ThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🆔 Booking ID: ${booking.docId.take(12)}...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = ThemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "📝 Booked on: $createdDate",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Important Information
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "📞 What happens next?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            "✅ Your booking is confirmed and you will receive a call from our jewelry expert at the designated time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            "📱 Please ensure your phone is available during the consultation time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "💡 Tip: Have your jewelry questions ready for a more productive consultation!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Got it!", color = Color.White)
            }
        }
    )
}
