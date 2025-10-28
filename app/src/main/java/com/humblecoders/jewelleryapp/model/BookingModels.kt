package com.humblecoders.jewelleryapp.model

import com.google.firebase.Timestamp

data class AvailabilityDoc(
    val docId: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val slotDurationMinutes: Int = 30,
    val createdAt: Timestamp = Timestamp.now()
)

data class BookingDoc(
    val docId: String = "",
    val userId: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val status: String = "confirmed",
    val createdAt: Timestamp = Timestamp.now()
)

data class SlotItem(
    val startTime: Timestamp,
    val endTime: Timestamp,
    val isBooked: Boolean
)


