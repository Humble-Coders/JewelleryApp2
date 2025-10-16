package com.example.jewelleryapp.repository

import com.example.jewelleryapp.model.AvailabilityDoc
import com.example.jewelleryapp.model.BookingDoc
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class VideoBookingRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val collection = firestore.collection("bookings")

    suspend fun fetchActiveAvailabilities(now: Timestamp): List<AvailabilityDoc> {
        // Use single-field filter to avoid composite index; filter rest client-side
        val snapshot = collection
            .whereEqualTo("type", "availability")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            AvailabilityDoc(
                docId = doc.id,
                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                slotDurationMinutes = (doc.getLong("slotDuration") ?: 30L).toInt(),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { it.endTime.seconds > now.seconds }
    }

    suspend fun fetchAvailabilitiesForDate(selectedDate: Timestamp): List<AvailabilityDoc> {
        // Get start and end of the selected date
        val calendar = java.util.Calendar.getInstance()
        calendar.time = java.util.Date(selectedDate.seconds * 1000)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val dayStart = Timestamp(calendar.time)
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val dayEnd = Timestamp(calendar.time)

        // Fetch all availabilities and filter by date
        val snapshot = collection
            .whereEqualTo("type", "availability")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            AvailabilityDoc(
                docId = doc.id,
                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                slotDurationMinutes = (doc.getLong("slotDuration") ?: 30L).toInt(),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { availability ->
            availability.startTime.seconds >= dayStart.seconds && 
            availability.startTime.seconds < dayEnd.seconds
        }
    }

    suspend fun fetchUpcomingBookings(now: Timestamp): List<BookingDoc> {
        // Avoid composite indexes: fetch by type only and filter status/time client-side
        val snapshot = collection
            .whereEqualTo("type", "booking")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            BookingDoc(
                docId = doc.id,
                userId = doc.getString("userId") ?: "",
                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                status = doc.getString("status") ?: "confirmed",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { it.status == "confirmed" && it.startTime.seconds > now.seconds }
    }

    suspend fun fetchMyBookings(now: Timestamp): List<BookingDoc> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        // Use single-field filter on userId, filter rest client-side
        val snapshot = collection
            .whereEqualTo("userId", uid)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            if (doc.getString("type") != "booking") return@mapNotNull null
            BookingDoc(
                docId = doc.id,
                userId = doc.getString("userId") ?: "",
                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                status = doc.getString("status") ?: "confirmed",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { it.startTime.seconds > now.seconds }
    }

    suspend fun createBooking(
        startTime: Timestamp,
        endTime: Timestamp
    ): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to book a slot")

        // Check if slot is already booked before creating
        val existingBookings = collection
            .whereEqualTo("type", "booking")
            .whereEqualTo("status", "confirmed")
            .whereEqualTo("startTime", startTime)
            .get()
            .await()

        if (existingBookings.documents.isNotEmpty()) {
            throw IllegalStateException("This slot has already been booked")
        }

        // Use Firebase transaction to ensure atomic booking creation
        return firestore.runTransaction { transaction ->
            // Create the booking within transaction
            val data = hashMapOf(
                "type" to "booking",
                "userId" to uid,
                "startTime" to startTime,
                "endTime" to endTime,
                "status" to "confirmed",
                "createdAt" to Timestamp.now()
            )

            val ref = collection.document()
            transaction.set(ref, data)
            ref.id
        }.await()
    }

    // Real-time listener for new bookings
    fun listenToNewBookings(): Flow<List<BookingDoc>> = callbackFlow {
        val listener = collection
            .whereEqualTo("type", "booking")
            .whereEqualTo("status", "confirmed")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        BookingDoc(
                            docId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                            endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                            status = doc.getString("status") ?: "confirmed",
                            createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(bookings)
            }

        awaitClose { listener.remove() }
    }

    // Real-time listener for user's bookings
    fun listenToUserBookings(): Flow<List<BookingDoc>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = collection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        if (doc.getString("type") == "booking") {
                            BookingDoc(
                                docId = doc.id,
                                userId = doc.getString("userId") ?: "",
                                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                                status = doc.getString("status") ?: "confirmed",
                                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(bookings)
            }

        awaitClose { listener.remove() }
    }

    // Get consultation history for user (all bookings, not just upcoming)
    suspend fun fetchConsultationHistory(): List<BookingDoc> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        
        val snapshot = collection
            .whereEqualTo("userId", uid)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                if (doc.getString("type") == "booking") {
                    BookingDoc(
                        docId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                        endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                        status = doc.getString("status") ?: "confirmed",
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.createdAt }
    }
}


