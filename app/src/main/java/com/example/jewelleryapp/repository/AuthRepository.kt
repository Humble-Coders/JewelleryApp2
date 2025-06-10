package com.example.jewelleryapp.repository

import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseAuthRepository(private val firebaseAuth: FirebaseAuth) {

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                firebaseAuth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun createUserWithEmailAndPassword(name: String, email: String, password: String, phone: String = ""): Result<Unit> {
        return try {
            // Trim and validate email
            val trimmedEmail = email.trim()

            // Log for debugging
            Log.d("AuthRepository", "Attempting to create user with email: '$trimmedEmail'")

            if (!isValidEmail(trimmedEmail)) {
                Log.e("AuthRepository", "Invalid email format: $trimmedEmail")
                return Result.failure(IllegalArgumentException("Invalid email format"))
            }

            withContext(Dispatchers.IO) {
                // Create the authentication user
                val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Failed to create user account")

                Log.d("AuthRepository", "User created successfully with UID: $userId")

                // Check if a temporary user document exists with encoded email as ID
                val encodedEmail = trimmedEmail

                val tempUserDoc = try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(encodedEmail)
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Error checking for temp user: ${e.message}")
                    null
                }

                // Create user profile data
                val userProfileData = hashMapOf(
                    "email" to trimmedEmail,
                    "encodedEmail" to encodedEmail,
                    "name" to name,
                    "phone" to phone,
                    "createdAt" to System.currentTimeMillis(),
                    "isTemporary" to false
                )

                // If temporary document exists, handle data migration
                if (tempUserDoc != null && tempUserDoc.exists()) {
                    Log.d("AuthRepository", "Temporary user document found, migrating data")
                    // Copy any fields you want to preserve from temp user
                    // ...

                    // Delete the temporary document
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(encodedEmail)
                        .delete()
                        .await()
                }

                // Save new user document with Auth UID
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(userProfileData)
                    .await()

                // Update Auth profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
                Log.d("AuthRepository", "User profile updated successfully")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }
    // Add this to your class for testing
    fun testEmailValidation(email: String): Boolean {
        // Debug log the email
        Log.d("EmailValidation", "Testing email: '$email'")

        // Check if email is empty
        if (email.isEmpty()) {
            Log.d("EmailValidation", "Email is empty")
            return false
        }

        // Trim the email and check again
        val trimmedEmail = email.trim()
        if (trimmedEmail != email) {
            Log.d("EmailValidation", "Email had whitespace. Trimmed: '$trimmedEmail'")
        }

        // Test with patterns
        val patternMatches = Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
        Log.d("EmailValidation", "Patterns.EMAIL_ADDRESS match result: $patternMatches")

        return patternMatches
    }

    // Modified isValidEmail function with trimming
    private fun isValidEmail(email: String): Boolean {
        // Trim the email to remove leading/trailing whitespace
        val trimmedEmail = email.trim()

        // Use Android's pattern matcher for email validation
        return android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    }
    // Add the same utility functions as in the desktop app


     // Optional: Add this if you want to update user profile with full name
    suspend fun updateUserProfile(displayName: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    //fun signOut() = firebaseAuth.signOut()

    fun isUserLoggedIn() = firebaseAuth.currentUser != null
}


