package com.example.jewelleryapp.repository

import com.google.firebase.auth.FirebaseAuth
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


    // FirebaseAuthRepository.kt - Add this function
    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    //fun getCurrentUser() = firebaseAuth.currentUser

    //fun signOut() = firebaseAuth.signOut()

    fun isUserLoggedIn() = firebaseAuth.currentUser != null
}


