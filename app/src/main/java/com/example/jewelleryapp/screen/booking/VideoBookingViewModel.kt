package com.example.jewelleryapp.screen.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.AvailabilityDoc
import com.example.jewelleryapp.model.BookingDoc
import com.example.jewelleryapp.model.SlotItem
import com.example.jewelleryapp.repository.VideoBookingRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    // Public methods to reset availability and slots
    fun clearSelectedAvailability() {
        _selectedAvailability.value = null
        _slotsForAvailability.value = emptyList()
    }

    // Initialize real-time listeners
    init {
        startRealTimeListeners()
    }

    private fun startRealTimeListeners() {
        // Listen to user's bookings in real-time
        repository.listenToUserBookings()
            .onEach { bookings ->
                val now = Timestamp.now()
                _myBookings.value = bookings.filter { 
                    it.status == "confirmed" && it.startTime.seconds > now.seconds 
                }
            }
            .catch { e ->
                _error.value = "Failed to listen to user bookings: ${e.message}"
            }
            .launchIn(viewModelScope)

        // Listen to ALL bookings to update slot availability in real-time
        repository.listenToNewBookings()
            .onEach { allBookings ->
                // Update slots when bookings change
                updateSlotsWithLatestBookings(allBookings)
            }
            .catch { e ->
                _error.value = "Failed to listen to all bookings: ${e.message}"
            }
            .launchIn(viewModelScope)
    }

    private fun updateSlotsWithLatestBookings(allBookings: List<BookingDoc>) {
        val now = Timestamp.now()
        val confirmedBookings = allBookings.filter { 
            it.status == "confirmed" && it.startTime.seconds > now.seconds 
        }
        val bookedStartMillis = confirmedBookings.map { it.startTime.toDate().time }.toSet()

        // Update main slots
        _slots.value = _slots.value.map { slot ->
            slot.copy(isBooked = bookedStartMillis.contains(slot.startTime.toDate().time))
        }

        // Update slots for selected availability
        _slotsForAvailability.value = _slotsForAvailability.value.map { slot ->
            slot.copy(isBooked = bookedStartMillis.contains(slot.startTime.toDate().time))
        }
    }

    fun loadSlots() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val now = Timestamp.now()
                val availabilities = repository.fetchActiveAvailabilities(now)
                val bookings = repository.fetchUpcomingBookings(now)
                val bookedStartMillis = bookings.map { it.startTime.toDate().time }.toSet()

                val generated = mutableListOf<SlotItem>()
                availabilities.forEach { a ->
                    val durationMs = TimeUnit.MINUTES.toMillis(a.slotDurationMinutes.toLong())
                    var cursor = a.startTime.toDate().time
                    val endBoundary = a.endTime.toDate().time
                    while (cursor + durationMs <= endBoundary) {
                        val isBooked = bookedStartMillis.contains(cursor)
                        generated.add(
                            SlotItem(
                                startTime = Timestamp(cursor / 1000, ((cursor % 1000) * 1_000_000).toInt()),
                                endTime = Timestamp((cursor + durationMs) / 1000, (((cursor + durationMs) % 1000) * 1_000_000).toInt()),
                                isBooked = isBooked
                            )
                        )
                        cursor += durationMs
                    }
                }

                // Sort by start time and update slots
                _slots.value = generated.sortedBy { it.startTime.toDate().time }
                
                // Trigger real-time update to ensure consistency
                updateSlotsWithLatestBookings(bookings)
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
                _bookingSuccess.value = "Slot successfully booked! 🎉\nBooking ID: $bookingId"
                
                // Reload slots to reflect the new booking
                loadSlots()
                
            } catch (e: Exception) {
                _bookingError.value = e.message ?: "Failed to book slot. Please try again."
            } finally {
                _isLoading.value = false
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
                val now = Timestamp.now()
                val bookings = repository.fetchUpcomingBookings(now)
                val bookedStartMillis = bookings.map { it.startTime.toDate().time }.toSet()

                val generated = mutableListOf<SlotItem>()
                val durationMs = TimeUnit.MINUTES.toMillis(availability.slotDurationMinutes.toLong())
                var cursor = availability.startTime.toDate().time
                val endBoundary = availability.endTime.toDate().time
                
                while (cursor + durationMs <= endBoundary) {
                    val isBooked = bookedStartMillis.contains(cursor)
                    val startTimestamp = Timestamp(Date(cursor))
                    val endTimestamp = Timestamp(Date(cursor + durationMs))
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
                
                // Trigger real-time update to ensure consistency
                updateSlotsWithLatestBookings(bookings)
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
}


