package com.example.jewelleryapp.screen.loginScreen


// LoginViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun signInWithEmailAndPassword(email: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.signInWithEmailAndPassword(email, password)

            result.fold(
                onSuccess = { _loginState.value = LoginState.Success },
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
