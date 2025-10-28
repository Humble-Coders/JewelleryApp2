package com.humblecoders.jewelleryapp.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humblecoders.jewelleryapp.model.AvailabilityDoc
import com.humblecoders.jewelleryapp.model.SlotItem
import com.humblecoders.jewelleryapp.screen.homeScreen.BottomNavigationBar
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
fun VideoConsultationScreen(
    navController: NavController,
    viewModel: VideoBookingViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val availabilitiesForDate by viewModel.availabilitiesForDate.collectAsState()
    val selectedAvailability by viewModel.selectedAvailability.collectAsState()
    val slotsForAvailability by viewModel.slotsForAvailability.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()
    val bookingError by viewModel.bookingError.collectAsState()
    val showPhoneDialog by viewModel.showPhoneDialog.collectAsState()
    val phoneUpdateSuccess by viewModel.phoneUpdateSuccess.collectAsState()
    val phoneUpdateError by viewModel.phoneUpdateError.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showBookingDialog by remember { mutableStateOf<SlotItem?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Check phone number when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkUserPhoneNumber()
    }

    // Listen for booking success/error
    LaunchedEffect(bookingSuccess) {
        if (bookingSuccess != null) {
            showSuccessDialog = true
            showBookingDialog = null
        }
    }

    LaunchedEffect(bookingError) {
        if (bookingError != null) {
            showErrorDialog = bookingError
            showBookingDialog = null
        }
    }

    // Listen for phone update success/error
    LaunchedEffect(phoneUpdateSuccess) {
        if (phoneUpdateSuccess == true) {
            phoneError = null
        }
    }

    LaunchedEffect(phoneUpdateError) {
        if (phoneUpdateError != null) {
            phoneError = phoneUpdateError
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Video Consultation", 
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
                },
                actions = {
                    IconButton(
                        onClick = { 
                            navController.navigate("consultation_history")
                        }
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "Consultation History",
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
                            Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Book Your Video Consultation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Connect with our jewelry experts for personalized advice",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Step Indicator
                StepIndicator(
                    currentStep = when {
                        selectedDate == null -> 1
                        selectedAvailability == null -> 2
                        else -> 3
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on current step
                when {
                    selectedDate == null -> {
                        // Step 1: Date Selection
                        DateSelectionStep(
                            onShowDatePicker = { showDatePicker = true }
                        )
                    }
                    selectedAvailability == null -> {
                        // Step 2: Availability Selection
                        AvailabilitySelectionStep(
                            availabilities = availabilitiesForDate,
                            selectedDate = selectedDate,
                            isLoading = isLoading,
                            error = error,
                            onAvailabilitySelected = { availability ->
                                viewModel.selectAvailability(availability)
                            },
                            onBack = {
                                viewModel.resetBookingFlow()
                            }
                        )
                    }
                    else -> {
                        // Step 3: Slot Selection
                        SlotSelectionStep(
                            slots = slotsForAvailability,
                            selectedAvailability = selectedAvailability,
                            isLoading = isLoading,
                            error = error,
                            onSlotSelected = { slot ->
                                showBookingDialog = slot
                            },
                            onBack = {
                                viewModel.clearSelectedAvailability()
                            }
                        )
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = { date ->
                    viewModel.selectDate(date)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        // Booking Confirmation Dialog
        showBookingDialog?.let { slot ->
            BookingConfirmationDialog(
                slot = slot,
                onConfirm = { confirmedSlot ->
                    viewModel.bookSlot(confirmedSlot)
                    showBookingDialog = null
                },
                onDismiss = { showBookingDialog = null }
            )
        }

        // Success Dialog
        if (showSuccessDialog) {
            BookingSuccessDialog(
                bookingId = bookingSuccess?.split("Booking ID: ")?.getOrNull(1) ?: "",
                onDismiss = { 
                    showSuccessDialog = false
                    viewModel.clearBookingMessages()
                    // Slots are automatically refreshed after booking success
                }
            )
        }

        // Error Dialog
        showErrorDialog?.let { error ->
            BookingErrorDialog(
                error = error,
                onDismiss = { 
                    showErrorDialog = null
                    viewModel.clearBookingMessages()
                }
            )
        }

        // Phone Number Dialog
        if (showPhoneDialog) {
            PhoneNumberDialog(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { 
                    phoneNumber = it
                    phoneError = null // Clear error when user types
                },
                phoneError = phoneError,
                isLoading = isLoading,
                onConfirm = {
                    if (phoneNumber.isBlank()) {
                        phoneError = "Phone number is required"
                    } else if (!isValidPhoneNumber(phoneNumber)) {
                        phoneError = "Please enter a valid phone number"
                    } else {
                        viewModel.updateUserPhoneNumber(phoneNumber)
                    }
                },
                onDismiss = {
                    viewModel.hidePhoneDialog()
                    phoneNumber = ""
                    phoneError = null
                }
            )
        }
    }
}

// Phone number validation function
private fun isValidPhoneNumber(phone: String): Boolean {
    // Basic phone validation - at least 10 digits, can include +, -, spaces, parentheses
    val cleaned = phone.replace(Regex("[^\\d+]"), "")
    return cleaned.length >= 10 && cleaned.length <= 15
}

@Composable
private fun SlotCard(slot: SlotItem, onBook: (SlotItem) -> Unit) {
    val df = remember { SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()) }
    val start = remember(slot.startTime) { df.format(Date(slot.startTime.seconds * 1000)) }
    val end = remember(slot.endTime) { df.format(Date(slot.endTime.seconds * 1000)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !slot.isBooked) { if (!slot.isBooked) onBook(slot) },
        colors = CardDefaults.cardColors(
            containerColor = if (slot.isBooked) 
                ThemeColorLight.copy(alpha = 0.3f) 
            else 
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (slot.isBooked) 0.dp else 6.dp
        ),
        border = if (slot.isBooked) 
            BorderStroke(2.dp, ThemeColor.copy(alpha = 0.5f)) 
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = start,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (slot.isBooked) ThemeColor else Color.Black
                )
                Text(
                    text = "to $end",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (slot.isBooked) ThemeColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (slot.isBooked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = ThemeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Already Booked",
                            style = MaterialTheme.typography.labelMedium,
                            color = ThemeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Book Button
            if (!slot.isBooked) {
                Button(
                    onClick = { onBook(slot) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Book Now",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(
                            ThemeColor.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Booked",
                        color = ThemeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingConfirmationDialog(
    slot: SlotItem,
    onConfirm: (SlotItem) -> Unit,
    onDismiss: () -> Unit
) {
    val df = remember { SimpleDateFormat("EEE, dd MMM yyyy 'at' HH:mm", Locale.getDefault()) }
    val start = remember(slot.startTime) { df.format(Date(slot.startTime.seconds * 1000)) }
    val end = remember(slot.endTime) { df.format(Date(slot.endTime.seconds * 1000)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Confirm Booking Request",
                fontWeight = FontWeight.Bold,
                color = ThemeColor
            )
        },
        text = {
            Column {
                Text("You are requesting to book:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📅 Date: $start",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "⏰ Duration: ${start.split(" ").last()} - ${end.split(" ").last()}",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "This will submit a booking request for video consultation. Our admin will review and confirm your appointment. You will receive a confirmation notification once approved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Note: Your slot will be reserved only after admin confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(slot) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColor)
            ) {
                Text("Submit Request", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ThemeColor)
            }
        }
    )
}

@Composable
private fun BookingSuccessDialog(bookingId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Request Submitted!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        },
        text = {
            Column {
                Text("🎉 Your video consultation request has been submitted successfully!")
                Spacer(modifier = Modifier.height(8.dp))
                if (bookingId.isNotEmpty()) {
                    Text(
                        "Request ID: $bookingId",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    "Our admin will review your request and send you a confirmation notification once approved. Please check your notifications or booking history for updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⏳ Status: Pending Admin Approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Got it!", color = Color.White)
            }
        }
    )
}

@Composable
private fun BookingErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Booking Failed",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(error)
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("OK", color = Color.White)
            }
        }
    )
}

@Composable
private fun PhoneNumberDialog(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    phoneError: String?,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = ThemeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Phone Number Required",
                    fontWeight = FontWeight.Bold,
                    color = ThemeColor
                )
            }
        },
        text = {
            Column {
                Text(
                    "To book a video consultation, we need your phone number for contact purposes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = { Text("Phone Number") },
                    placeholder = { Text("Enter your phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            tint = ThemeColor
                        )
                    }
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = ThemeColor)
            }
        }
    )
}

// New composables for three-step flow

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Date", "Availability", "Slot").forEachIndexed { index, step ->
            StepItem(
                stepNumber = index + 1,
                stepName = step,
                isActive = index + 1 == currentStep,
                isCompleted = index + 1 < currentStep
            )
            if (index < 2) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    stepName: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = when {
                        isCompleted -> Color(0xFF4CAF50)
                        isActive -> ThemeColor
                        else -> Color.Gray
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stepName,
            fontSize = 12.sp,
            color = if (isActive || isCompleted) ThemeColor else Color.Gray,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DateSelectionStep(
    onShowDatePicker: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = ThemeColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Select a Date",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Choose the date for your video consultation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onShowDatePicker,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pick a Date",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilitySelectionStep(
    availabilities: List<AvailabilityDoc>,
    selectedDate: com.google.firebase.Timestamp?,
    isLoading: Boolean,
    error: String?,
    onAvailabilitySelected: (AvailabilityDoc) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back button and date display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ThemeColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            selectedDate?.let { date ->
                val df = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                Text(
                    text = df.format(Date(date.seconds * 1000)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Available Time Blocks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ThemeColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (availabilities.isEmpty()) {
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
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = ThemeColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Availability",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No time blocks are available for this date. Please select a different date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availabilities) { availability ->
                    AvailabilityCard(
                        availability = availability,
                        onSelect = { onAvailabilitySelected(availability) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotSelectionStep(
    slots: List<SlotItem>,
    selectedAvailability: AvailabilityDoc?,
    isLoading: Boolean,
    error: String?,
    onSlotSelected: (SlotItem) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back button and availability display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ThemeColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            selectedAvailability?.let { availability ->
                val startDf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val start = startDf.format(Date(availability.startTime.seconds * 1000))
                val end = startDf.format(Date(availability.endTime.seconds * 1000))
                Text(
                    text = "$start - $end",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Available Time Slots",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ThemeColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(slots) { slot ->
                    SlotCard(
                        slot = slot,
                        onBook = onSlotSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityCard(
    availability: AvailabilityDoc,
    onSelect: () -> Unit
) {
    val startDf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val start = startDf.format(Date(availability.startTime.seconds * 1000))
    val end = startDf.format(Date(availability.endTime.seconds * 1000))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = ThemeColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$start - $end",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "${availability.slotDurationMinutes} min slots",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ThemeColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (com.google.firebase.Timestamp) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    // Use a full-screen dialog for better date picker display
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f), // Use 80% of screen height
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Date",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = ThemeColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date Picker with maximum available space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take all available space
                        .padding(horizontal = 4.dp) // Minimal horizontal padding
                ) {
                    DatePicker(
                        state = datePickerState,
                        modifier = Modifier.fillMaxSize(),
                        colors = DatePickerDefaults.colors(
                            selectedDayContainerColor = ThemeColor,
                            todayDateBorderColor = ThemeColor,
                            selectedDayContentColor = Color.White,
                            selectedYearContainerColor = ThemeColor,
                            selectedYearContentColor = Color.White
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ThemeColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val timestamp = com.google.firebase.Timestamp(millis / 1000, ((millis % 1000) * 1_000_000).toInt())
                                onDateSelected(timestamp)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColor)
                    ) {
                        Text("Select", color = Color.White)
                    }
                }
            }
        }
    }
}


