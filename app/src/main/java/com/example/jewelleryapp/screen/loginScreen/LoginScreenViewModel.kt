package com.example.jewelleryapp.screen.loginScreen


// LoginViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.google.firebase.firestore.FirebaseFirestore
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
}
