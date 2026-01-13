package com.humblecoders.jewelleryapp.screen.loginScreen


// LoginViewModel.kt

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.repository.FirebaseAuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {
    private val TAG = "LoginViewModel"

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // LoginViewModel.kt - Add this property and method
    private val _userProfile = MutableStateFlow<Map<String, Any>?>(null)
    val userProfile: StateFlow<Map<String, Any>?> = _userProfile.asStateFlow()


    // Update handleGoogleSignInResult method in LoginViewModel
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Handling Google Sign-In result, data: ${data != null}")

                val result = repository.handleGoogleSignInResult(data)

                Log.d(TAG, "Repository result: ${result.isSuccess}")

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Google Sign-In successful")
                        // Verify user is actually authenticated before setting success
                        val currentUser = repository.getCurrentUser()
                        if (currentUser != null && currentUser.uid.isNotEmpty()) {
                            Log.d(TAG, "User verified after Google Sign-In - UID: ${currentUser.uid}, setting Success state")
                            _loginState.value = LoginState.Success
                            fetchUserProfile(currentUser.uid)
                        } else {
                            Log.e(TAG, "Google Sign-In succeeded but no valid user found - setting Error state")
                            _loginState.value = LoginState.Error("Authentication failed: No user found")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Google Sign-In failed: ${exception.message}", exception)

                        // NEW: Handle specific error messages for email conflicts
                        val errorMessage = when {
                            exception.message?.contains("already registered with email and password") == true ->
                                "This email is already registered. Please sign in using your email and password."
                            else -> exception.message ?: "Google Sign-In failed"
                        }

                        _loginState.value = LoginState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in handleGoogleSignInResult", e)
                _loginState.value = LoginState.Error(
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    // Add timeout handling
    fun startGoogleSignIn(): Intent {
        Log.d(TAG, "Starting Google Sign-In")
        _loginState.value = LoginState.GoogleSignInLoading

        // Add timeout to reset state if needed
        viewModelScope.launch {
            delay(30000) // 30 seconds timeout
            if (_loginState.value is LoginState.GoogleSignInLoading) {
                Log.w(TAG, "Google Sign-In timed out")
                _loginState.value = LoginState.Error("Google Sign-In timed out. Please try again.")
            }
        }

        return repository.getGoogleSignInIntent()
    }

    fun cancelGoogleSignIn() {
        if (_loginState.value is LoginState.GoogleSignInLoading) {
            _loginState.value = LoginState.Idle
        }
    }


    // Fetch user profile after successful login
    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profileSnapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (profileSnapshot.exists()) {
                    _userProfile.value = profileSnapshot.data
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Update signInWithEmailAndPassword method
    fun signInWithEmailAndPassword(email: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.signInWithEmailAndPassword(email, password)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Email/password login successful")
                    // Verify user is actually authenticated before setting success
                    val currentUser = repository.getCurrentUser()
                    if (currentUser != null && currentUser.uid.isNotEmpty()) {
                        Log.d(TAG, "User verified after login - UID: ${currentUser.uid}, setting Success state")
                        _loginState.value = LoginState.Success
                        fetchUserProfile(currentUser.uid)
                    } else {
                        Log.e(TAG, "Login succeeded but no valid user found - setting Error state")
                        _loginState.value = LoginState.Error("Authentication failed: No user found")
                    }
                },
                onFailure = { _loginState.value = LoginState.Error(it.message ?: "Authentication failed") }
            )
        }
    }



    fun resetPassword(email: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val result = repository.resetPassword(email)

            result.fold(
                onSuccess = { _loginState.value = LoginState.PasswordResetSent },
                onFailure = { _loginState.value = LoginState.Error(it.message ?: "Failed to send reset email") }
            )
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    fun isUserLoggedIn() = repository.isUserLoggedIn()
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    object PasswordResetSent : LoginState()
    data class Error(val message: String) : LoginState()
    object GoogleSignInLoading : LoginState() // Add this new state

}
