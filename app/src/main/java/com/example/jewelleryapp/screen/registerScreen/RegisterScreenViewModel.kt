package com.example.jewelleryapp.screen.registerScreen

// RegisterViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    // RegisterViewModel.kt - Update this method
    fun registerWithEmailAndPassword(fullName: String, email: String, password: String, phone: String = "") {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            val result = repository.createUserWithEmailAndPassword(email, password, fullName, phone)

            result.fold(
                onSuccess = {
                    _registerState.value = RegisterState.Success
                },
                onFailure = {
                    _registerState.value = RegisterState.Error(it.message ?: "Registration failed")
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
}