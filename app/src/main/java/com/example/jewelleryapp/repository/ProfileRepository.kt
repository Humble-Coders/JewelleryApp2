package com.example.jewelleryapp.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.jewelleryapp.model.ProfileUpdateRequest
import com.example.jewelleryapp.model.UserProfile
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_preferences")

class ProfileRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val tag = "ProfileRepository"

    companion object {
        private val PROFILE_IMAGE_PATH_KEY = stringPreferencesKey("profile_image_path")
    }


    suspend fun updateUserProfile(updateRequest: ProfileUpdateRequest): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No authenticated user"))

            withContext(Dispatchers.IO) {
                val updateData = hashMapOf<String, Any>(
                    "name" to updateRequest.name,
                    "phone" to updateRequest.phone,
                    "dateOfBirth" to updateRequest.dateOfBirth
                )

                firestore.collection("users")
                    .document(currentUser.uid)
                    .update(updateData)
                    .await()

                // Also update Firebase Auth display name if changed
                if (updateRequest.name.isNotBlank() && updateRequest.name != currentUser.displayName) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(updateRequest.name)
                        .build()
                    currentUser.updateProfile(profileUpdates).await()
                }

                Log.d(tag, "Profile updated successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating user profile", e)
            Result.failure(e)
        }
    }

    suspend fun saveProfileImageToLocal(imageUri: Uri): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                val currentUser = firebaseAuth.currentUser
                    ?: return@withContext Result.failure(Exception("No authenticated user"))

                // Create app-specific directory for profile images
                val profileDir = File(context.filesDir, "profile_images")
                if (!profileDir.exists()) {
                    profileDir.mkdirs()
                }

                // Create unique filename
                val fileName = "profile_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                val imageFile = File(profileDir, fileName)

                // Copy image from URI to app storage
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(imageFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val imagePath = imageFile.absolutePath

                // Save path to DataStore
                saveLocalImagePath(imagePath)

                Log.d(tag, "Profile image saved locally: $imagePath")
                Result.success(imagePath)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error saving profile image", e)
            Result.failure(e)
        }
    }

    private suspend fun saveLocalImagePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE_PATH_KEY] = path
        }
    }

    private fun getLocalImagePath(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PROFILE_IMAGE_PATH_KEY] ?: ""
        }
    }

    suspend fun clearLocalImagePath(): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences.remove(PROFILE_IMAGE_PATH_KEY)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error clearing local image path", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(password: String? = null): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No authenticated user"))

            withContext(Dispatchers.IO) {
                // Check if reauthentication is needed
                val signInMethods = currentUser.providerData.map { it.providerId }

                if (signInMethods.contains(EmailAuthProvider.PROVIDER_ID) && password != null) {
                    // Reauthenticate with email/password
                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
                    currentUser.reauthenticate(credential).await()
                    Log.d(tag, "Reauthenticated with email/password")
                } else if (signInMethods.contains(GoogleAuthProvider.PROVIDER_ID)) {
                    // For Google users, we'll handle reauthentication in the ViewModel if needed
                    Log.d(tag, "Google user - reauthentication may be handled separately")
                }

                // Clean up local data first
                clearLocalImagePath()

                // Clear local profile image files
                val profileDir = File(context.filesDir, "profile_images")
                if (profileDir.exists()) {
                    profileDir.listFiles()?.forEach { file ->
                        if (file.name.contains(currentUser.uid)) {
                            file.delete()
                        }
                    }
                }

                // NOTE: We don't delete Firestore data as per requirements
                // Only delete the Firebase Auth user
                currentUser.delete().await()

                Log.d(tag, "User account deleted successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting user account", e)

            // Check if reauthentication is required
            if (e.message?.contains("requires-recent-login") == true) {
                Result.failure(Exception("REAUTHENTICATION_REQUIRED"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // Clear local image path on logout
                clearLocalImagePath()

                // Sign out from Firebase Auth
                firebaseAuth.signOut()

                Log.d(tag, "User signed out successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error signing out", e)
            Result.failure(e)
        }
    }



    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

     fun checkIfReauthenticationRequired(): Boolean {
        return try {
            val currentUser = firebaseAuth.currentUser ?: return false

            // Check if user signed in recently (within last 5 minutes)
            val metadata = currentUser.metadata
            val lastSignInTime = metadata?.lastSignInTimestamp ?: 0
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - lastSignInTime

            // If last sign-in was more than 5 minutes ago, reauthentication might be required
            timeDifference > (5 * 60 * 1000) // 5 minutes
        } catch (e: Exception) {
            Log.e(tag, "Error checking reauthentication requirement", e)
            true // Assume reauthentication is required on error
        }
    }

    // In ProfileRepository.kt, update the getCurrentUserProfile method:

    // In ProfileRepository.kt, update the getCurrentUserProfile method:

    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            // Enhanced auth state checking
            if (firebaseAuth.currentUser == null) {
                Log.d(tag, "No current user, waiting for auth...")
                val authAvailable = waitForAuthState()
                if (!authAvailable) {
                    return Result.failure(Exception("No authenticated user"))
                }
            }

            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No authenticated user"))

            Log.d(tag, "Current user found: ${currentUser.uid}, email: ${currentUser.email}")

            withContext(Dispatchers.IO) {
                // Add retry logic for Firestore document fetch
                var attempt = 0
                var lastException: Exception? = null

                while (attempt < 3) {
                    try {
                        val userDoc = firestore.collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                        if (!userDoc.exists()) {
                            Log.w(tag, "User document not found, attempt ${attempt + 1}")
                            if (attempt < 2) {
                                delay(500L * (attempt + 1)) // Progressive delay
                                attempt++
                                continue
                            }
                            return@withContext Result.failure(Exception("User profile not found"))
                        }

                        val data = userDoc.data!!
                        val isGoogleSignIn = data["googleSignIn"] as? Boolean ?: false

                        // Get local image path from DataStore
                        val localImagePath = getLocalImagePath().first()

                        val profile = UserProfile(
                            id = currentUser.uid,
                            name = data["name"] as? String ?: "",
                            email = data["email"] as? String ?: currentUser.email ?: "",
                            phone = data["phone"] as? String ?: "",
                            dateOfBirth = data["dateOfBirth"] as? String ?: "",
                            profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                            isGoogleSignIn = isGoogleSignIn,
                            createdAt = data["createdAt"] as? Long ?: 0L,
                            localImagePath = localImagePath
                        )

                        Log.d(tag, "Profile loaded successfully for user: ${currentUser.uid}")
                        return@withContext Result.success(profile)

                    } catch (e: Exception) {
                        Log.e(tag, "Error loading profile attempt ${attempt + 1}", e)
                        lastException = e
                        if (attempt < 2) {
                            delay(500L * (attempt + 1))
                            attempt++
                        } else {
                            break
                        }
                    }
                }

                Result.failure(lastException ?: Exception("Failed to load profile after retries"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading user profile", e)
            Result.failure(e)
        }
    }

    // Also update waitForAuthState with better logic:
    suspend fun waitForAuthState(): Boolean {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 25 // Increased attempts

            while (attempts < maxAttempts) {
                val currentUser = firebaseAuth.currentUser

                if (currentUser != null) {
                    Log.d(tag, "Auth state ready: ${currentUser.uid}")
                    return@withContext true
                }

                delay(200) // Check every 200ms
                attempts++
                Log.d(tag, "Waiting for auth state, attempt $attempts/$maxAttempts")
            }

            Log.w(tag, "Auth state not ready after ${maxAttempts * 200}ms")
            false
        }
    }

}