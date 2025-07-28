package com.example.jewelleryapp.screen.profileScreen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.*
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PrimaryGold = Color(0xFFD4AF37)
private val DarkGold = Color(0xFFB8860B)
private val LightGold = Color(0xFFF5E6A8)
private val BackgroundGray = Color(0xFFF8F9FA)
private val CardBackground = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val ErrorRed = Color(0xFFDC2626)
private val SuccessGreen = Color(0xFF059669)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onSignOut: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val deletionState by viewModel.deletionState.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val isImageSaving by viewModel.isImageSaving.collectAsState()
    val isSigningOut by viewModel.isSigningOut.collectAsState()

    // Form state
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Only allow dates up to today (no future dates)
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

// Helper function to format date
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectProfileImage(it) }
    }

    // Load profile when screen first appears - with delay for auth stability
    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "Screen appeared, loading profile")
        kotlinx.coroutines.delay(100) // Small delay for stability
        viewModel.loadUserProfile()
    }

    // Update form when profile loads
    LaunchedEffect(currentProfile) {
        currentProfile?.let { profile ->
            name = profile.name
            phone = profile.phone
            dateOfBirth = profile.dateOfBirth
        }
    }

    // Handle state changes
    LaunchedEffect(updateState) {
        if (updateState is ProfileUpdateState.Success) {
            isEditing = false
            viewModel.resetUpdateState()
        }
    }

    // Handle deletion state - simplified
    LaunchedEffect(deletionState) {
        when (deletionState) {
            is AccountDeletionState.Success -> {
                onAccountDeleted()
            }
            is AccountDeletionState.ReauthenticationRequired -> {
                showReauthDialog = true
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Scaffold(
            topBar = {
                EnhancedProfileTopBar(
                    title = "My Profile",
                    isEditing = isEditing,
                    onBackClick = { navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    } },
                    onEditClick = { isEditing = true },
                    onSaveClick = {
                        viewModel.updateProfile(name, phone, dateOfBirth)
                    },
                    onCancelClick = {
                        currentProfile?.let { profile ->
                            name = profile.name
                            phone = profile.phone
                            dateOfBirth = profile.dateOfBirth
                        }
                        isEditing = false
                        viewModel.resetUpdateState()
                    },
                    isLoading = updateState is ProfileUpdateState.Loading
                )
            },
            bottomBar = { BottomNavigationBar(navController) },
            containerColor = BackgroundGray
        ) { paddingValues ->
            when (profileState) {
                is ProfileState.Loading -> {
                    EnhancedLoadingView()
                }
                is ProfileState.Error -> {
                    EnhancedErrorView(
                        error = (profileState as ProfileState.Error).message,
                        onRetry = {
                            Log.d("ProfileScreen", "Retry button clicked")
                            viewModel.loadUserProfile()
                        }
                    )
                }
                is ProfileState.Success -> {
                    EnhancedProfileContent(
                        profile = (profileState as ProfileState.Success).profile,
                        selectedImageUri = selectedImageUri,
                        name = name,
                        phone = phone,
                        dateOfBirth = dateOfBirth,
                        isEditing = isEditing,
                        isImageSaving = isImageSaving,
                        isSigningOut = isSigningOut,
                        updateState = updateState,
                        onNameChange = { name = it },
                        onPhoneChange = { phone = it },
                        onDateOfBirthChange = { dateOfBirth = it },
                        onImageClick = { imagePickerLauncher.launch("image/*") },
                        onSaveImage = { viewModel.saveSelectedImage() },
                        onClearImage = { viewModel.clearSelectedImage() },
                        onRemoveLocalImage = { viewModel.removeLocalProfileImage() },
                        onSignOut = {
                            Log.d("ProfileScreen", "Sign out clicked")
                            viewModel.signOut()
                            onSignOut()
                        },
                        onDeleteAccount = { showDeleteDialog = true },
                        getDisplayImagePath = { viewModel.getDisplayImagePath() },
                        isCurrentImageFromGoogle = { viewModel.isCurrentImageFromGoogle() },
                        modifier = Modifier.padding(paddingValues),
                        onShowDatePicker = { showDatePicker = true }, // Add this line

                    )
                }
            }
        }
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        EnhancedDeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Reauthentication Dialog
    if (showReauthDialog) {
        EnhancedReauthenticationDialog(
            onConfirm = { password ->
                showReauthDialog = false
                viewModel.reauthenticateAndDeleteAccount(password)
            },
            onDismiss = {
                showReauthDialog = false
                viewModel.resetDeletionState()
            },
            isLoading = deletionState is AccountDeletionState.Loading
        )
    }

    // Add this after the existing dialogs in ProfileScreen (after ReauthenticationDialog)
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            dateOfBirth = dateFormatter.format(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = PrimaryGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = androidx.compose.material3.DatePickerDefaults.colors(
                    selectedDayContainerColor = PrimaryGold,
                    todayDateBorderColor = PrimaryGold,
                    dayInSelectionRangeContainerColor = PrimaryGold.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// Add this new composable function
@Composable
private fun EnhancedProfileDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    onDatePickerClick: () -> Unit,
    icon: ImageVector
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        if (isEditing) {
            OutlinedButton(
                onClick = onDatePickerClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (value.isEmpty()) TextSecondary else TextPrimary
                ),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (value.isEmpty()) "Select date of birth" else value,
                        fontSize = 16.sp,
                        color = if (value.isEmpty()) TextSecondary else TextPrimary
                    )
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Open date picker",
                        tint = PrimaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9FAFB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (value.isNotEmpty()) value else "Not provided",
                    fontSize = 16.sp,
                    color = if (value.isNotEmpty()) TextPrimary else TextSecondary,
                    fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun EnhancedProfileTopBar(
    title: String,
    isEditing: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp),
        color = CardBackground
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            actions = {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = { fadeIn() with fadeOut() }
                ) { editing ->
                    if (editing) {
                        Row {
                            IconButton(
                                onClick = onCancelClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = onSaveClick,
                                enabled = !isLoading,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = PrimaryGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Save",
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = PrimaryGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBackground)
        )
    }
}

@Composable
private fun EnhancedProfileContent(
    profile: UserProfile,
    selectedImageUri: Uri?,
    name: String,
    phone: String,
    dateOfBirth: String,
    isEditing: Boolean,
    isImageSaving: Boolean,
    isSigningOut: Boolean,
    updateState: ProfileUpdateState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onSaveImage: () -> Unit,
    onClearImage: () -> Unit,
    onRemoveLocalImage: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    getDisplayImagePath: () -> String,
    isCurrentImageFromGoogle: () -> Boolean,
    modifier: Modifier = Modifier,
    onShowDatePicker: () -> Unit,

    ) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture Section with enhanced styling
        EnhancedProfileImageSection(
            profile = profile,
            selectedImageUri = selectedImageUri,
            isEditing = isEditing,
            isImageSaving = isImageSaving,
            onImageClick = onImageClick,
            onSaveImage = onSaveImage,
            onClearImage = onClearImage,
            onRemoveLocalImage = onRemoveLocalImage,
            getDisplayImagePath = getDisplayImagePath,
            isCurrentImageFromGoogle = isCurrentImageFromGoogle
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Information with enhanced cards
        EnhancedProfileInformationSection(
            profile = profile,
            name = name,
            phone = phone,
            dateOfBirth = dateOfBirth,
            isEditing = isEditing,
            updateState = updateState,
            onNameChange = onNameChange,
            onPhoneChange = onPhoneChange,
            onDateOfBirthChange = onDateOfBirthChange,
            onShowDatePicker = onShowDatePicker,
            )

        Spacer(modifier = Modifier.height(24.dp))

        // Account Actions with enhanced styling
        if (!isEditing) {
            EnhancedAccountActionsSection(
                isSigningOut = isSigningOut,
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount,
                profile = profile
            )
        }
    }
}

@Composable
private fun EnhancedProfileImageSection(
    profile: UserProfile,
    selectedImageUri: Uri?,
    isEditing: Boolean,
    isImageSaving: Boolean,
    onImageClick: () -> Unit,
    onSaveImage: () -> Unit,
    onClearImage: () -> Unit,
    onRemoveLocalImage: () -> Unit,
    getDisplayImagePath: () -> String,
    isCurrentImageFromGoogle: () -> Boolean
) {
    val context = LocalContext.current
    val displayImagePath = getDisplayImagePath()

    // Animation for the profile image
    val imageScale by animateFloatAsState(
        targetValue = if (isEditing) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient background ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryGold, DarkGold),
                            radius = 200f
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Profile Image Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CardBackground)
                        .clickable(enabled = isEditing && !profile.isGoogleSignIn) { onImageClick() }
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        displayImagePath.isNotEmpty() -> {
                            AsyncImage(
                                model = if (displayImagePath.startsWith("http")) {
                                    displayImagePath
                                } else {
                                    ImageRequest.Builder(context)
                                        .data(File(displayImagePath))
                                        .build()
                                },
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ic_launcher_background)
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(LightGold, PrimaryGold.copy(alpha = 0.3f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Default Profile Picture",
                                    modifier = Modifier.size(48.dp),
                                    tint = PrimaryGold
                                )
                            }
                        }
                    }
                }

                // Loading overlay with blur effect
                if (isImageSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // Edit icon with enhanced styling
            if (isEditing && !profile.isGoogleSignIn) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryGold, DarkGold)
                            ),
                            shape = CircleShape
                        )
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Welcome text with user's name
        Text(
            text = "Welcome, ${profile.name.ifEmpty { "User" }}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = profile.email,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Image action buttons with enhanced styling
        if (isEditing) {
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedImageUri != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearImage,
                        enabled = !isImageSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, Color.LightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = onSaveImage,
                        enabled = !isImageSaving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Save Image", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (displayImagePath.isNotEmpty() && !isCurrentImageFromGoogle()) {
                Button(
                    onClick = onRemoveLocalImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.1f),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Image", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Google sign-in message with enhanced styling
        if (profile.isGoogleSignIn && isEditing) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F4F6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google profile picture cannot be changed",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedProfileInformationSection(
    profile: UserProfile,
    name: String,
    phone: String,
    dateOfBirth: String,
    isEditing: Boolean,
    updateState: ProfileUpdateState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onShowDatePicker: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Personal Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Name Field
            EnhancedProfileField(
                label = "Full Name",
                value = name,
                onValueChange = onNameChange,
                isEditing = isEditing,
                isRequired = true,
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email Field (Read-only)
            EnhancedProfileField(
                label = "Email",
                value = profile.email,
                onValueChange = {},
                isEditing = false,
                readOnlyMessage = "Email cannot be changed",
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone Field
            EnhancedProfileField(
                label = "Phone Number",
                value = phone,
                onValueChange = onPhoneChange,
                isEditing = isEditing,
                keyboardType = KeyboardType.Phone,
                icon = Icons.Default.Phone
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Date of Birth Field
            EnhancedProfileDateField(
                label = "Date of Birth",
                value = dateOfBirth,
                onValueChange = onDateOfBirthChange,
                isEditing = isEditing,
                onDatePickerClick = {
                    onShowDatePicker()
                },
                icon = Icons.Default.CalendarMonth
            )

            // Update error/success message
            AnimatedVisibility(
                visible = updateState is ProfileUpdateState.Error || updateState is ProfileUpdateState.Success,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                when (updateState) {
                    is ProfileUpdateState.Error -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ErrorRed.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = updateState.message,
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is ProfileUpdateState.Success -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessGreen.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Profile updated successfully!",
                                    color = SuccessGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    isRequired: Boolean = false,
    readOnlyMessage: String? = null,
    icon: ImageVector
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRequired) "$label *" else label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        if (isEditing && readOnlyMessage == null) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        placeholder,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGold,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = PrimaryGold,
                    focusedLabelColor = PrimaryGold
                ),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF9FAFB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (value.isNotEmpty()) value else "Not provided",
                        fontSize = 16.sp,
                        color = if (value.isNotEmpty()) TextPrimary else TextSecondary,
                        fontWeight = if (value.isNotEmpty()) FontWeight.Medium else FontWeight.Normal
                    )
                    if (readOnlyMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = readOnlyMessage,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedAccountActionsSection(
    isSigningOut: Boolean,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    profile: UserProfile
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Account Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Sign Out Button
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSigningOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGold,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSigningOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = if (isSigningOut) "Signing Out..." else "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Only show Delete Account button for email/password users
            if (!profile.isGoogleSignIn) {
                Spacer(modifier = Modifier.height(16.dp))

                // Delete Account Button
                OutlinedButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(40.dp)
            ) {
                // Animated loading indicator
                val infiniteTransition = rememberInfiniteTransition()
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer { rotationZ = rotation }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = PrimaryGold,
                        strokeWidth = 4.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Loading your profile...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Please wait a moment",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(40.dp)
            ) {
                // Error icon with animation
                val errorScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            ErrorRed.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .graphicsLayer {
                            scaleX = errorScale
                            scaleY = errorScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Oops! Something went wrong",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Try Again",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedDeleteAccountDialog(
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
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to delete your account? This action cannot be undone. Your account will be permanently removed, but your data will be preserved.",
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Delete",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = CardBackground
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedReauthenticationDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Your Identity",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "For security reasons, please enter your password to confirm account deletion.",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (isPasswordVisible) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGold,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = PrimaryGold,
                        focusedLabelColor = PrimaryGold
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isLoading) "Confirming..." else "Confirm Delete",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = CardBackground
    )
}