package com.example.jewelleryapp.screen.homeScreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.StoreInfo
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class StoreInfoViewModel(
    private val repository: JewelryRepository
) : ViewModel() {

    private val _storeInfo = MutableStateFlow<StoreInfo?>(null)
    val storeInfo: StateFlow<StoreInfo?> = _storeInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadStoreInfo()
    }

    private fun loadStoreInfo() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val info = repository.getStoreInfo().first()
                _storeInfo.value = info
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load store information"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentDayHours(): String? {
        val storeInfo = _storeInfo.value ?: return null
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayName = when (currentDay) {
            Calendar.SUNDAY -> "sunday"
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            else -> ""
        }
        return storeInfo.storeHours[dayName]
    }

    fun openGoogleMaps(context: Context) {
        val storeInfo = _storeInfo.value ?: return

        try {
            // Create geo URI with coordinates
            val geoUri = "geo:${storeInfo.latitude},${storeInfo.longitude}?q=${storeInfo.latitude},${storeInfo.longitude}(${storeInfo.name})"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to browser if Google Maps not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${storeInfo.latitude},${storeInfo.longitude}"))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("StoreInfoViewModel", "Error opening maps", e)
        }
    }

    fun openWhatsApp(context: Context) {
        val storeInfo = _storeInfo.value ?: return

        try {
            val phoneNumber = storeInfo.whatsappNumber.replace("+", "").replace("-", "").replace(" ", "")
            val message = storeInfo.whatsappDefaultMessage

            // Try WhatsApp first
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
            whatsappIntent.setPackage("com.whatsapp")

            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                // Try WhatsApp Business
                val whatsappBusinessIntent = Intent(Intent.ACTION_VIEW)
                whatsappBusinessIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                whatsappBusinessIntent.setPackage("com.whatsapp.w4b")

                if (whatsappBusinessIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(whatsappBusinessIntent)
                } else {
                    // Fallback to browser
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"))
                    context.startActivity(browserIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("StoreInfoViewModel", "Error opening WhatsApp", e)
        }
    }

}