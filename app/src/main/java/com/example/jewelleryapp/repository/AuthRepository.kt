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


    private fun isValidEmail(email: String): Boolean {
        val trimmedEmail = email.trim()

        return Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    }



    fun getCurrentUser() = firebaseAuth.currentUser

    fun signOut() = firebaseAuth.signOut()

    fun isUserLoggedIn() = firebaseAuth.currentUser != null
}


