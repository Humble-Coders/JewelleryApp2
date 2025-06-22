package com.example.jewelleryapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FCMHelper {
    private const val TAG = "FCMHelper"

    fun initializeFCM() {
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Send token to server
            saveTokenToFirestore(token)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "User not authenticated, skipping token save")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(user.uid)

        userDoc.update(
            mapOf(
                "fcmToken" to token,
                "lastTokenUpdate" to System.currentTimeMillis()
            )
        ).addOnSuccessListener {
            Log.d(TAG, "FCM token saved successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error saving FCM token", e)

            // If document doesn't exist, create it
            userDoc.set(
                mapOf(
                    "fcmToken" to token,
                    "lastTokenUpdate" to System.currentTimeMillis(),
                    "userId" to user.uid
                )
            ).addOnSuccessListener {
                Log.d(TAG, "FCM token saved successfully (new document)")
            }.addOnFailureListener { createError ->
                Log.e(TAG, "Error creating user document with FCM token", createError)
            }
        }
    }

    fun requestNotificationPermission() {
        // For Android 13+ notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+ detected, notification permission should be requested in activity")
        }
    }
}