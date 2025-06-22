package com.example.jewelleryapp.screen.profileScreen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.*
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import java.io.File

private val GoldColor = Color(0xFFB78628)

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

    // Handle sign out - don't rely on isSigningOut state changes
    // The navigation should be triggered from the sign out button directly

    Scaffold(
        topBar = {
            ProfileTopBar(
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
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        when (profileState) {
            is ProfileState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = GoldColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading profile...", color = Color.Gray)
                    }
                }
            }
            is ProfileState.Error -> {
                ErrorView(
                    error = (profileState as ProfileState.Error).message,
                    onRetry = {
                        Log.d("ProfileScreen", "Retry button clicked")
                        viewModel.loadUserProfile()
                    }
                )
            }
            is ProfileState.Success -> {
                ProfileContent(
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
                        // Navigate immediately without waiting for state
                        onSignOut()
                    },
                    onDeleteAccount = { showDeleteDialog = true },
                    getDisplayImagePath = { viewModel.getDisplayImagePath() },
                    isCurrentImageFromGoogle = { viewModel.isCurrentImageFromGoogle() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Reauthentication Dialog
    if (showReauthDialog) {
        ReauthenticationDialog(
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    title: String,
    isEditing: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    isLoading: Boolean
) {
    TopAppBar(
        title = { Text(title, color = GoldColor) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldColor
                )
            }
        },
        actions = {
            if (isEditing) {
                IconButton(onClick = onCancelClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.Gray
                    )
                }
                IconButton(
                    onClick = onSaveClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoldColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = GoldColor
                        )
                    }
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = GoldColor
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
private fun ProfileContent(
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture Section
        ProfileImageSection(
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

        // Profile Information
        ProfileInformationSection(
            profile = profile,
            name = name,
            phone = phone,
            dateOfBirth = dateOfBirth,
            isEditing = isEditing,
            updateState = updateState,
            onNameChange = onNameChange,
            onPhoneChange = onPhoneChange,
            onDateOfBirthChange = onDateOfBirthChange
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Account Actions
        if (!isEditing) {
            AccountActionsSection(
                isSigningOut = isSigningOut,
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount,
                profile = profile
            )
        }
    }
}

@Composable
private fun ProfileImageSection(
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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, GoldColor, CircleShape)
                    .clickable(enabled = isEditing && !profile.isGoogleSignIn) { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    selectedImageUri != null -> {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Profile Picture",
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.ic_launcher_background)
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_background),
                            contentDescription = "Default Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Edit icon overlay
            if (isEditing && !profile.isGoogleSignIn) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .background(GoldColor, CircleShape)
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Loading overlay
            if (isImageSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image action buttons
        if (isEditing) {
            if (selectedImageUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onClearImage,
                        enabled = !isImageSaving
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = onSaveImage,
                        enabled = !isImageSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
                    ) {
                        Text("Save Image")
                    }
                }
            } else if (displayImagePath.isNotEmpty() && !isCurrentImageFromGoogle()) {
                OutlinedButton(
                    onClick = onRemoveLocalImage,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Remove Image")
                }
            }
        }

        // Google sign-in message
        if (profile.isGoogleSignIn && isEditing) {
            Text(
                text = "Google profile picture cannot be changed",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ProfileInformationSection(
    profile: UserProfile,
    name: String,
    phone: String,
    dateOfBirth: String,
    isEditing: Boolean,
    updateState: ProfileUpdateState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Personal Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Name Field
            ProfileField(
                label = "Full Name",
                value = name,
                onValueChange = onNameChange,
                isEditing = isEditing,
                isRequired = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field (Read-only)
            ProfileField(
                label = "Email",
                value = profile.email,
                onValueChange = {},
                isEditing = false,
                readOnlyMessage = "Email cannot be changed"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Field
            ProfileField(
                label = "Phone Number",
                value = phone,
                onValueChange = onPhoneChange,
                isEditing = isEditing,
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth Field
            ProfileField(
                label = "Date of Birth",
                value = dateOfBirth,
                onValueChange = onDateOfBirthChange,
                isEditing = isEditing,
                placeholder = "YYYY-MM-DD"
            )

            // Update error message
            if (updateState is ProfileUpdateState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateState.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    isRequired: Boolean = false,
    readOnlyMessage: String? = null
) {
    Column {
        Text(
            text = if (isRequired) "$label *" else label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (isEditing && readOnlyMessage == null) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldColor,
                    unfocusedBorderColor = Color.LightGray
                )
            )
        } else {
            Text(
                text = if (value.isNotEmpty()) value else "Not provided",
                fontSize = 16.sp,
                color = if (value.isNotEmpty()) Color.Black else Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            if (readOnlyMessage != null) {
                Text(
                    text = readOnlyMessage,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun AccountActionsSection(
    isSigningOut: Boolean,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    profile: UserProfile // Add this parameter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Account Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sign Out Button
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSigningOut,
                colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
            ) {
                if (isSigningOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Sign Out")
            }

            // Only show Delete Account button for email/password users
            if (!profile.isGoogleSignIn) {
                Spacer(modifier = Modifier.height(12.dp))

                // Delete Account Button
                OutlinedButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Something went wrong",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Account",
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete your account? This action cannot be undone. Your account will be permanently removed, but your data will be preserved.",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReauthenticationDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Confirm Your Identity",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "For security reasons, please enter your password to confirm account deletion.",
                    fontSize = 14.sp,
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
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm Delete")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

