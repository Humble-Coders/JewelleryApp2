package com.example.jewelleryapp.screen.loginScreen


// LoginViewModel.kt

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    // LoginViewModel.kt - Add this property and method
    private val _userProfile = MutableStateFlow<Map<String, Any>?>(null)
    val userProfile: StateFlow<Map<String, Any>?> = _userProfile.asStateFlow()


    // Update handleGoogleSignInResult method in LoginViewModel
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                Log.d("GoogleSignIn", "Handling Google Sign-In result, data: ${data != null}")

                val result = repository.handleGoogleSignInResult(data)

                Log.d("GoogleSignIn", "Repository result: ${result.isSuccess}")

                result.fold(
                    onSuccess = {
                        Log.d("GoogleSignIn", "Google Sign-In successful")
                        _loginState.value = LoginState.Success

                        repository.getCurrentUser()?.uid?.let { userId ->
                            Log.d("GoogleSignIn", "User ID: $userId")
                            fetchUserProfile(userId)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("GoogleSignIn", "Google Sign-In failed: ${exception.message}", exception)

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
                Log.e("GoogleSignIn", "Unexpected error in handleGoogleSignInResult", e)
                _loginState.value = LoginState.Error(
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    // Add timeout handling
    fun startGoogleSignIn(): Intent {
        Log.d("GoogleSignIn", "Starting Google Sign-In")
        _loginState.value = LoginState.GoogleSignInLoading

        // Add timeout to reset state if needed
        viewModelScope.launch {
            delay(30000) // 30 seconds timeout
            if (_loginState.value is LoginState.GoogleSignInLoading) {
                Log.w("GoogleSignIn", "Google Sign-In timed out")
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
                    // User logged in successfully
                    _loginState.value = LoginState.Success

                    // Get the user ID and fetch profile
                    repository.getCurrentUser()?.uid?.let { userId ->
                        fetchUserProfile(userId)
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
