package com.humblecoders.jewelleryapp.screen.registerScreen

// RegisterViewModel.kt

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.repository.FirebaseAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {
    private val TAG = "RegisterViewModel"

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()


        // Add Google Sign-In methods (same pattern as LoginViewModel)
        fun startGoogleSignIn(): Intent {
            Log.d(TAG, "Starting Google Sign-In from Register")
            _registerState.value = RegisterState.GoogleSignInLoading

            // Add timeout to reset state if needed
            viewModelScope.launch {
                delay(30000) // 30 seconds timeout
                if (_registerState.value is RegisterState.GoogleSignInLoading) {
                    Log.w(TAG, "Google Sign-In timed out in register")
                    _registerState.value =
                        RegisterState.Error("Google Sign-In timed out. Please try again.")
                }
            }

            return repository.getGoogleSignInIntent()
        }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Handling Google Sign-In result in Register, data: ${data != null}")

                val result = repository.handleGoogleSignInResult(data)

                Log.d(TAG, "Repository result in Register: ${result.isSuccess}")

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Google Sign-In successful in Register")
                        // Verify user is actually authenticated before setting success
                        val currentUser = repository.getCurrentUser()
                        if (currentUser != null && currentUser.uid.isNotEmpty()) {
                            Log.d(TAG, "User verified after Google Sign-In - UID: ${currentUser.uid}, setting Success state")
                            _registerState.value = RegisterState.Success
                        } else {
                            Log.e(TAG, "Google Sign-In succeeded but no valid user found - setting Error state")
                            _registerState.value = RegisterState.Error("Authentication failed: No user found")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Google Sign-In failed in Register: ${exception.message}", exception)

                        // NEW: Handle specific error messages for email conflicts
                        val errorMessage = when {
                            exception.message?.contains("already registered with email and password") == true ->
                                "This email is already registered. Please sign in using your email and password."
                            else -> exception.message ?: "Google Sign-In failed"
                        }

                        _registerState.value = RegisterState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in Register handleGoogleSignInResult", e)
                _registerState.value = RegisterState.Error(
                    "An unexpected error occurred: ${e.message}"
                )
            }
        }
    }

    // UPDATE registerWithEmailAndPassword method to handle Google conflict errors
    fun registerWithEmailAndPassword(
        fullName: String,
        email: String,
        password: String,
        phone: String = ""
    ) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            val result =
                repository.createUserWithEmailAndPassword(fullName, email, password, phone)

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Email/password registration successful")
                    // Verify user is actually authenticated before setting success
                    val currentUser = repository.getCurrentUser()
                    if (currentUser != null && currentUser.uid.isNotEmpty()) {
                        Log.d(TAG, "User verified after registration - UID: ${currentUser.uid}, setting Success state")
                        _registerState.value = RegisterState.Success
                    } else {
                        Log.e(TAG, "Registration succeeded but no valid user found - setting Error state")
                        _registerState.value = RegisterState.Error("Registration failed: No user found")
                    }
                },
                onFailure = { exception ->
                    // NEW: Handle specific error messages for Google conflicts
                    val errorMessage = when {
                        exception.message?.contains("already registered with Google") == true ->
                            "This email is already registered with Google. Please sign in using Google."
                        else -> exception.message ?: "Registration failed"
                    }

                    _registerState.value = RegisterState.Error(errorMessage)
                }
            )
        }
    }

        fun cancelGoogleSignIn() {
            if (_registerState.value is RegisterState.GoogleSignInLoading) {
                _registerState.value = RegisterState.Idle
            }
        }



        fun resetState() {
            _registerState.value = RegisterState.Idle
        }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
    object GoogleSignInLoading : RegisterState() // Add this new state

}
