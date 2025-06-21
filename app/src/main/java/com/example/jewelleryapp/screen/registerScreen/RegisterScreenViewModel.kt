package com.example.jewelleryapp.screen.registerScreen

// RegisterViewModel.kt

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()


        // Add Google Sign-In methods (same pattern as LoginViewModel)
        fun startGoogleSignIn(): Intent {
            Log.d("GoogleSignIn", "Starting Google Sign-In from Register")
            _registerState.value = RegisterState.GoogleSignInLoading

            // Add timeout to reset state if needed
            viewModelScope.launch {
                delay(30000) // 30 seconds timeout
                if (_registerState.value is RegisterState.GoogleSignInLoading) {
                    Log.w("GoogleSignIn", "Google Sign-In timed out in register")
                    _registerState.value =
                        RegisterState.Error("Google Sign-In timed out. Please try again.")
                }
            }

            return repository.getGoogleSignInIntent()
        }

        fun handleGoogleSignInResult(data: Intent?) {
            viewModelScope.launch {
                try {
                    Log.d(
                        "GoogleSignIn",
                        "Handling Google Sign-In result in Register, data: ${data != null}"
                    )

                    val result = repository.handleGoogleSignInResult(data)

                    Log.d("GoogleSignIn", "Repository result in Register: ${result.isSuccess}")

                    result.fold(
                        onSuccess = {
                            Log.d("GoogleSignIn", "Google Sign-In successful in Register")
                            _registerState.value = RegisterState.Success
                        },
                        onFailure = { exception ->
                            Log.e(
                                "GoogleSignIn",
                                "Google Sign-In failed in Register: ${exception.message}",
                                exception
                            )
                            _registerState.value = RegisterState.Error(
                                exception.message ?: "Google Sign-In failed"
                            )
                        }
                    )
                } catch (e: Exception) {
                    Log.e(
                        "GoogleSignIn",
                        "Unexpected error in Register handleGoogleSignInResult",
                        e
                    )
                    _registerState.value = RegisterState.Error(
                        "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }

        fun cancelGoogleSignIn() {
            if (_registerState.value is RegisterState.GoogleSignInLoading) {
                _registerState.value = RegisterState.Idle
            }
        }

        // RegisterViewModel.kt - Update this method
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
                        _registerState.value = RegisterState.Success
                    },
                    onFailure = {
                        _registerState.value =
                            RegisterState.Error(it.message ?: "Registration failed")
                    }
                )
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
