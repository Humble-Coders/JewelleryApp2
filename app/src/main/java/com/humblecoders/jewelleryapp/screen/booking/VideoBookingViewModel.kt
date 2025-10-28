package com.humblecoders.jewelleryapp.screen.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.model.AvailabilityDoc
import com.humblecoders.jewelleryapp.model.BookingDoc
import com.humblecoders.jewelleryapp.model.SlotItem
import com.humblecoders.jewelleryapp.model.UserProfile
import com.humblecoders.jewelleryapp.repository.VideoBookingRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class VideoBookingViewModel(
    private val repository: VideoBookingRepository
) : ViewModel() {

    private val _slots = MutableStateFlow<List<SlotItem>>(emptyList())
    val slots: StateFlow<List<SlotItem>> = _slots

    private val _myBookings = MutableStateFlow<List<BookingDoc>>(emptyList())
    val myBookings: StateFlow<List<BookingDoc>> = _myBookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // New state for date-based booking
    private val _selectedDate = MutableStateFlow<Timestamp?>(null)
    val selectedDate: StateFlow<Timestamp?> = _selectedDate

    private val _availabilitiesForDate = MutableStateFlow<List<AvailabilityDoc>>(emptyList())
    val availabilitiesForDate: StateFlow<List<AvailabilityDoc>> = _availabilitiesForDate

    private val _selectedAvailability = MutableStateFlow<AvailabilityDoc?>(null)
    val selectedAvailability: StateFlow<AvailabilityDoc?> = _selectedAvailability

    private val _slotsForAvailability = MutableStateFlow<List<SlotItem>>(emptyList())
    val slotsForAvailability: StateFlow<List<SlotItem>> = _slotsForAvailability

    // New state for consultation history
    private val _consultationHistory = MutableStateFlow<List<BookingDoc>>(emptyList())
    val consultationHistory: StateFlow<List<BookingDoc>> = _consultationHistory

    // State for booking confirmation
    private val _bookingSuccess = MutableStateFlow<String?>(null)
    val bookingSuccess: StateFlow<String?> = _bookingSuccess

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError

    // State for phone number validation
    private val _showPhoneDialog = MutableStateFlow(false)
    val showPhoneDialog: StateFlow<Boolean> = _showPhoneDialog

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _phoneUpdateSuccess = MutableStateFlow<Boolean?>(null)
    val phoneUpdateSuccess: StateFlow<Boolean?> = _phoneUpdateSuccess

    private val _phoneUpdateError = MutableStateFlow<String?>(null)
    val phoneUpdateError: StateFlow<String?> = _phoneUpdateError

    // Public methods to reset availability and slots
    fun clearSelectedAvailability() {
        _selectedAvailability.value = null
        _slotsForAvailability.value = emptyList()
    }

    // Initialize without real-time listeners
    init {
        // No real-time listeners - we'll refresh data when needed
    }

    fun loadSlots() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val now = Timestamp.now()
                val availabilities = repository.fetchActiveAvailabilities(now)
                // Fetch all confirmed bookings to properly mark slots as booked
                val bookings = repository.fetchAllConfirmedBookings()
                
                // Create a set of booked start times for efficient lookup
                val bookedStartTimes = bookings.map { it.startTime }.toSet()

                val generated = mutableListOf<SlotItem>()
                availabilities.forEach { a ->
                    val durationMs = TimeUnit.MINUTES.toMillis(a.slotDurationMinutes.toLong())
                    var cursor = a.startTime.toDate().time
                    val endBoundary = a.endTime.toDate().time
                    while (cursor + durationMs <= endBoundary) {
                        val startTimestamp = Timestamp(Date(cursor))
                        val endTimestamp = Timestamp(Date(cursor + durationMs))
                        
                        // Check if this slot is booked by comparing timestamps
                        val isBooked = bookedStartTimes.any { bookingStartTime ->
                            bookingStartTime.seconds == startTimestamp.seconds
                        }
                        
                        generated.add(
                            SlotItem(
                                startTime = startTimestamp,
                                endTime = endTimestamp,
                                isBooked = isBooked
                            )
                        )
                        cursor += durationMs
                    }
                }

                // Sort by start time and update slots
                _slots.value = generated.sortedBy { it.startTime.toDate().time }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMyBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val now = Timestamp.now()
                _myBookings.value = repository.fetchMyBookings(now)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun bookSlot(slot: SlotItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _bookingSuccess.value = null
            _bookingError.value = null
            
            try {
                val bookingId = repository.createBooking(
                    startTime = slot.startTime,
                    endTime = slot.endTime
                )
                
                // Set success message with booking ID
                _bookingSuccess.value = "Booking request submitted successfully! 🎉\nRequest ID: $bookingId"
                
                // Refresh slots to show updated availability
                refreshSlotsAfterBooking()
                
            } catch (e: Exception) {
                _bookingError.value = e.message ?: "Failed to book slot. Please try again."
                
                // If booking failed because slot is already booked, refresh slots
                if (e.message?.contains("already been booked", ignoreCase = true) == true) {
                    refreshSlotsAfterBooking()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Refresh slots after booking success or failure
    private fun refreshSlotsAfterBooking() {
        viewModelScope.launch {
            try {
                // Fetch all confirmed bookings to properly mark slots as booked
                val bookings = repository.fetchAllConfirmedBookings()
                
                // Create a set of booked start times for efficient lookup
                val bookedStartTimes = bookings.map { it.startTime }.toSet()

                // Update slots for selected availability if we have one
                if (_selectedAvailability.value != null) {
                    _slotsForAvailability.value = _slotsForAvailability.value.map { slot ->
                        val isBooked = bookedStartTimes.any { bookingStartTime ->
                            bookingStartTime.seconds == slot.startTime.seconds
                        }
                        slot.copy(isBooked = isBooked)
                    }
                }
                
                // Also update main slots if they exist
                if (_slots.value.isNotEmpty()) {
                    _slots.value = _slots.value.map { slot ->
                        val isBooked = bookedStartTimes.any { bookingStartTime ->
                            bookingStartTime.seconds == slot.startTime.seconds
                        }
                        slot.copy(isBooked = isBooked)
                    }
                }
            } catch (e: Exception) {
                // Silent fail - this is just a refresh operation
                // The main error handling is done in the booking method
            }
        }
    }

    // Load consultation history
    fun loadConsultationHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _consultationHistory.value = repository.fetchConsultationHistory()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load consultation history."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Clear booking messages
    fun clearBookingMessages() {
        _bookingSuccess.value = null
        _bookingError.value = null
    }

    // New functions for date-based booking
    fun selectDate(date: Timestamp) {
        _selectedDate.value = date
        loadAvailabilitiesForDate(date)
        _selectedAvailability.value = null
        _slotsForAvailability.value = emptyList()
    }

    fun loadAvailabilitiesForDate(date: Timestamp) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val availabilities = repository.fetchAvailabilitiesForDate(date)
                _availabilitiesForDate.value = availabilities
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load availabilities for this date."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectAvailability(availability: AvailabilityDoc) {
        _selectedAvailability.value = availability
        generateSlotsForAvailability(availability)
    }

    private fun generateSlotsForAvailability(availability: AvailabilityDoc) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch all confirmed bookings to properly mark slots as booked
                val bookings = repository.fetchAllConfirmedBookings()
                
                // Create a set of booked start times for efficient lookup
                val bookedStartTimes = bookings.map { it.startTime }.toSet()

                val generated = mutableListOf<SlotItem>()
                val durationMs = TimeUnit.MINUTES.toMillis(availability.slotDurationMinutes.toLong())
                var cursor = availability.startTime.toDate().time
                val endBoundary = availability.endTime.toDate().time
                
                while (cursor + durationMs <= endBoundary) {
                    val startTimestamp = Timestamp(Date(cursor))
                    val endTimestamp = Timestamp(Date(cursor + durationMs))
                    
                    // Check if this slot is booked by comparing timestamps
                    val isBooked = bookedStartTimes.any { bookingStartTime ->
                        bookingStartTime.seconds == startTimestamp.seconds
                    }
                    
                    generated.add(
                        SlotItem(
                            startTime = startTimestamp,
                            endTime = endTimestamp,
                            isBooked = isBooked
                        )
                    )
                    cursor += durationMs
                }

                _slotsForAvailability.value = generated.sortedBy { it.startTime.toDate().time }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate slots."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetBookingFlow() {
        _selectedDate.value = null
        _availabilitiesForDate.value = emptyList()
        _selectedAvailability.value = null
        _slotsForAvailability.value = emptyList()
        _error.value = null
    }

    // Check if user has phone number and show dialog if needed
    fun checkUserPhoneNumber() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getCurrentUserProfile()
                _userProfile.value = profile
                
                if (profile?.phone.isNullOrBlank()) {
                    _showPhoneDialog.value = true
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to check user profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update user phone number
    fun updateUserPhoneNumber(phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _phoneUpdateSuccess.value = null
            _phoneUpdateError.value = null
            
            try {
                val result = repository.updateUserPhoneNumber(phone)
                if (result.isSuccess) {
                    _phoneUpdateSuccess.value = true
                    _showPhoneDialog.value = false
                    // Update local user profile
                    _userProfile.value = _userProfile.value?.copy(phone = phone)
                } else {
                    _phoneUpdateError.value = result.exceptionOrNull()?.message ?: "Failed to update phone number"
                }
            } catch (e: Exception) {
                _phoneUpdateError.value = e.message ?: "Failed to update phone number"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Hide phone dialog
    fun hidePhoneDialog() {
        _showPhoneDialog.value = false
    }

    // Clear phone update messages
    fun clearPhoneUpdateMessages() {
        _phoneUpdateSuccess.value = null
        _phoneUpdateError.value = null
    }
}


