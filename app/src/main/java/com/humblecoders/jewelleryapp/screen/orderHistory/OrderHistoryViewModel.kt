package com.humblecoders.jewelleryapp.screen.orderHistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.humblecoders.jewelleryapp.model.Order
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderHistoryViewModel(
    private val repository: JewelryRepository
) : ViewModel() {

    private val tag = "OrderHistoryViewModel"

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _balance = MutableStateFlow<Double?>(null)
    val balance: StateFlow<Double?> = _balance.asStateFlow()

    private val _isLoadingBalance = MutableStateFlow(false)
    val isLoadingBalance: StateFlow<Boolean> = _isLoadingBalance.asStateFlow()

    private var hasLoadedData = false

    init {
        Log.d(tag, "OrderHistoryViewModel initialized")
        loadOrders()
        loadBalance()
    }

    fun loadOrders(forceRefresh: Boolean = false) {
        if (!forceRefresh && hasLoadedData) {
            Log.d(tag, "Orders already loaded, skipping reload")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val currentUser = FirebaseAuth.getInstance().currentUser
                val customerId = currentUser?.uid

                if (customerId == null) {
                    Log.e(tag, "No user logged in, cannot fetch orders")
                    _error.value = "Please login to view your orders"
                    _isLoading.value = false
                    return@launch
                }

                Log.d(tag, "Loading orders for customer: $customerId")

                repository.getOrdersByCustomerId(customerId).collect { ordersList ->
                    _orders.value = ordersList
                    hasLoadedData = true
                    _isLoading.value = false
                    Log.d(tag, "Loaded ${ordersList.size} orders")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading orders", e)
                _error.value = "Failed to load orders: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun refreshOrders() {
        hasLoadedData = false
        loadOrders(forceRefresh = true)
    }

    fun clearError() {
        _error.value = null
    }

    fun loadBalance() {
        viewModelScope.launch {
            try {
                _isLoadingBalance.value = true
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid

                if (userId == null) {
                    Log.e(tag, "No user logged in, cannot fetch balance")
                    _isLoadingBalance.value = false
                    return@launch
                }

                Log.d(tag, "Loading balance for user: $userId")
                val balance = repository.getUserBalance(userId)
                _balance.value = balance
                _isLoadingBalance.value = false
                Log.d(tag, "Loaded balance: $balance")
            } catch (e: Exception) {
                Log.e(tag, "Error loading balance", e)
                _isLoadingBalance.value = false
            }
        }
    }

    fun refreshBalance() {
        loadBalance()
    }
}
