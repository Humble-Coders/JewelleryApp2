package com.example.jewelleryapp.screen.profileScreen

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.AccountDeletionState
import com.example.jewelleryapp.model.ProfileState
import com.example.jewelleryapp.model.ProfileUpdateRequest
import com.example.jewelleryapp.model.ProfileUpdateState
import com.example.jewelleryapp.model.UserProfile
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: FirebaseAuthRepository
) : ViewModel() {

    private val tag = "ProfileViewModel"

    // Profile loading state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // Profile update state
    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    // Account deletion state
    private val _deletionState = MutableStateFlow<AccountDeletionState>(AccountDeletionState.Idle)
    val deletionState: StateFlow<AccountDeletionState> = _deletionState.asStateFlow()

    // Current profile data
    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    // Image selection state
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // Loading states for specific actions
    private val _isImageSaving = MutableStateFlow(false)
    val isImageSaving: StateFlow<Boolean> = _isImageSaving.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    // Add flag to prevent multiple loading attempts
    private var isCurrentlyLoading = false
    private var signOutRequested = false

    // Don't auto-load in init, let the composable trigger it
    init {
        Log.d(tag, "ProfileViewModel initialized - waiting for explicit load call")
    }

    fun loadUserProfile() {
        if (isCurrentlyLoading) {
            Log.d(tag, "Profile loading already in progress, skipping")
            return
        }

        viewModelScope.launch {
            try {
                isCurrentlyLoading = true
                _profileState.value = ProfileState.Loading
                Log.d(tag, "Loading user profile...")

                // Clear any previous sign out state
                signOutRequested = false

                // Wait a bit for auth to stabilize after sign in
                delay(300)

                // Check auth state with timeout
                val isSignedIn = withTimeout(5000) {
                    var authReady = profileRepository.isUserSignedIn()
                    var attempts = 0

                    while (!authReady && attempts < 15) { // Increased attempts
                        delay(200)
                        authReady = profileRepository.isUserSignedIn()
                        attempts++
                        Log.d(tag, "Auth check attempt $attempts: $authReady")
                    }

                    authReady
                }

                if (!isSignedIn) {
                    Log.w(tag, "User not signed in after waiting")
                    _profileState.value = ProfileState.Error("Please sign in to view profile")
                    isCurrentlyLoading = false
                    return@launch
                }

                Log.d(tag, "User is signed in, loading profile...")

                // Load profile with timeout
                val result = withTimeout(15000) { // Increased timeout
                    profileRepository.getCurrentUserProfile()
                }

                result.fold(
                    onSuccess = { profile ->
                        Log.d(tag, "Profile loaded successfully: ${profile.name}")
                        _currentProfile.value = profile
                        _profileState.value = ProfileState.Success(profile)
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to load profile", exception)
                        _profileState.value = ProfileState.Error(
                            exception.message ?: "Failed to load profile"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error loading profile", e)
                _profileState.value = ProfileState.Error("Network timeout or unexpected error")
            } finally {
                isCurrentlyLoading = false
            }
        }
    }

    fun updateProfile(name: String, phone: String, dateOfBirth: String) {
        viewModelScope.launch {
            try {
                _updateState.value = ProfileUpdateState.Loading
                Log.d(tag, "Updating profile: name=$name, phone=$phone, dob=$dateOfBirth")

                // Validate inputs
                if (name.isBlank()) {
                    _updateState.value = ProfileUpdateState.Error("Name cannot be empty")
                    return@launch
                }

                val updateRequest = ProfileUpdateRequest(
                    name = name.trim(),
                    phone = phone.trim(),
                    dateOfBirth = dateOfBirth.trim()
                )

                val result = withTimeout(10000) {
                    profileRepository.updateUserProfile(updateRequest)
                }

                result.fold(
                    onSuccess = {
                        Log.d(tag, "Profile updated successfully")
                        _updateState.value = ProfileUpdateState.Success

                        // Reload profile to get updated data
                        loadUserProfile()
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to update profile", exception)
                        _updateState.value = ProfileUpdateState.Error(
                            exception.message ?: "Failed to update profile"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error updating profile", e)
                _updateState.value = ProfileUpdateState.Error("Network timeout or unexpected error")
            }
        }
    }

    fun selectProfileImage(imageUri: Uri) {
        Log.d(tag, "Image selected: $imageUri")
        _selectedImageUri.value = imageUri
    }

    fun saveSelectedImage() {
        val imageUri = _selectedImageUri.value
        if (imageUri == null) {
            Log.w(tag, "No image selected to save")
            return
        }

        viewModelScope.launch {
            try {
                _isImageSaving.value = true
                Log.d(tag, "Saving selected image...")

                val result = withTimeout(15000) {
                    profileRepository.saveProfileImageToLocal(imageUri)
                }

                result.fold(
                    onSuccess = { imagePath ->
                        Log.d(tag, "Image saved successfully: $imagePath")

                        // Update current profile with new image path
                        _currentProfile.value?.let { profile ->
                            _currentProfile.value = profile.copy(localImagePath = imagePath)
                        }

                        // Clear selected image
                        _selectedImageUri.value = null

                        // Reload profile to reflect changes
                        loadUserProfile()
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to save image", exception)
                        _updateState.value = ProfileUpdateState.Error(
                            "Failed to save image: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error saving image", e)
                _updateState.value = ProfileUpdateState.Error("Failed to save image")
            } finally {
                _isImageSaving.value = false
            }
        }
    }

    fun clearSelectedImage() {
        Log.d(tag, "Clearing selected image")
        _selectedImageUri.value = null
    }

    fun deleteAccount(password: String? = null) {
        viewModelScope.launch {
            try {
                _deletionState.value = AccountDeletionState.Loading
                Log.d(tag, "Attempting to delete account...")

                // Check if reauthentication is required first
                val needsReauth = profileRepository.checkIfReauthenticationRequired()
                if (needsReauth && password.isNullOrBlank()) {
                    Log.d(tag, "Reauthentication required for account deletion")
                    _deletionState.value = AccountDeletionState.ReauthenticationRequired
                    return@launch
                }

                val result = withTimeout(15000) {
                    profileRepository.deleteUserAccount(password)
                }

                result.fold(
                    onSuccess = {
                        Log.d(tag, "Account deleted successfully")
                        _deletionState.value = AccountDeletionState.Success
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to delete account", exception)

                        if (exception.message == "REAUTHENTICATION_REQUIRED") {
                            _deletionState.value = AccountDeletionState.ReauthenticationRequired
                        } else {
                            _deletionState.value = AccountDeletionState.Error(
                                exception.message ?: "Failed to delete account"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error deleting account", e)
                _deletionState.value = AccountDeletionState.Error("Network timeout or unexpected error")
            }
        }
    }

    fun reauthenticateAndDeleteAccount(password: String) {
        viewModelScope.launch {
            try {
                _deletionState.value = AccountDeletionState.Loading
                Log.d(tag, "Reauthenticating before account deletion...")

                // First reauthenticate
                val reauthResult = withTimeout(10000) {
                    authRepository.reauthenticateWithPassword(password)
                }

                reauthResult.fold(
                    onSuccess = {
                        Log.d(tag, "Reauthentication successful, proceeding with deletion")
                        // Now delete the account
                        deleteAccount(password)
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Reauthentication failed", exception)
                        _deletionState.value = AccountDeletionState.Error(
                            "Authentication failed: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Unexpected error during reauthentication", e)
                _deletionState.value = AccountDeletionState.Error("Authentication timeout")
            }
        }
    }

    fun signOut() {
        signOutRequested = true
        Log.d(tag, "Sign out requested")

        viewModelScope.launch {
            try {
                _isSigningOut.value = true
                Log.d(tag, "Signing out user...")

                coroutineScope {
                    // Perform sign out operations in parallel with timeout
                    val profileSignOutJob = async(Dispatchers.Default) {
                        withTimeout(5000) {
                            profileRepository.signOut()
                        }
                    }

                    val authSignOutJob = async(Dispatchers.Default) {
                        withTimeout(5000) {
                            try {
                                authRepository.signOut()
                                Result.success(Unit)
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }
                    }

                    // Wait for both operations to complete
                    val profileResult = try { profileSignOutJob.await() } catch (e: Exception) { Result.failure(e) }
                    val authResult = try { authSignOutJob.await() } catch (e: Exception) { Result.failure(e) }

                    Log.d(tag, "Sign out completed - Profile: ${profileResult.isSuccess}, Auth: ${authResult.isSuccess}")
                }

                // Clear all local state
                clearAllState()

            } catch (e: Exception) {
                Log.e(tag, "Error during sign out", e)
                // Clear all local state anyway
                clearAllState()
            } finally {
                _isSigningOut.value = false
                Log.d(tag, "Sign out process completed")
            }
        }
    }

    private fun clearAllState() {
        _currentProfile.value = null
        _selectedImageUri.value = null
        _profileState.value = ProfileState.Loading
        _updateState.value = ProfileUpdateState.Idle
        _deletionState.value = AccountDeletionState.Idle
        isCurrentlyLoading = false
        Log.d(tag, "All state cleared")
    }

    fun resetUpdateState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    fun resetDeletionState() {
        _deletionState.value = AccountDeletionState.Idle
    }





    // Helper method to get display image (local or Google URL)
    fun getDisplayImagePath(): String {
        val profile = _currentProfile.value ?: return ""

        return when {
            profile.localImagePath.isNotEmpty() -> profile.localImagePath
            profile.isGoogleSignIn && profile.profilePictureUrl.isNotEmpty() -> profile.profilePictureUrl
            else -> ""
        }
    }

    // Helper method to check if current image is from Google
    fun isCurrentImageFromGoogle(): Boolean {
        val profile = _currentProfile.value ?: return false
        return profile.isGoogleSignIn &&
                profile.profilePictureUrl.isNotEmpty() &&
                profile.localImagePath.isEmpty()
    }

    // Method to remove local profile image
    fun removeLocalProfileImage() {
        viewModelScope.launch {
            try {
                Log.d(tag, "Removing local profile image")

                val result = withTimeout(5000) {
                    profileRepository.clearLocalImagePath()
                }

                result.fold(
                    onSuccess = {
                        Log.d(tag, "Local image path cleared")
                        // Reload profile to reflect changes
                        loadUserProfile()
                    },
                    onFailure = { exception ->
                        Log.e(tag, "Failed to clear local image", exception)
                        _updateState.value = ProfileUpdateState.Error(
                            "Failed to remove image: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "Error removing local image", e)
                _updateState.value = ProfileUpdateState.Error("Failed to remove image")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "ProfileViewModel cleared")
        isCurrentlyLoading = false
    }
}