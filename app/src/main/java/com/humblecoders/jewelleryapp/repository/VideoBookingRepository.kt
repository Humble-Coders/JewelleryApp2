package com.humblecoders.jewelleryapp.repository

import com.humblecoders.jewelleryapp.model.AvailabilityDoc
import com.humblecoders.jewelleryapp.model.BookingDoc
import com.humblecoders.jewelleryapp.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        }.filter { (it.status == "confirmed" || it.status == "pending") && it.startTime.seconds > now.seconds }
    }

    suspend fun fetchAllConfirmedBookings(): List<BookingDoc> {
        // Fetch all active bookings (PENDING, CONFIRMED) regardless of time for slot availability checking
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
                status = doc.getString("status") ?: "pending",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { it.status == "confirmed" || it.status == "pending" }
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
                status = doc.getString("status") ?: "pending",
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
            )
        }.filter { (it.status == "confirmed" || it.status == "pending") && it.startTime.seconds > now.seconds }
    }

    suspend fun createBooking(
        startTime: Timestamp,
        endTime: Timestamp
    ): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in to book a slot")

        // Check if slot is already booked before creating (check both confirmed and pending)
        val existingBookings = collection
            .whereEqualTo("type", "booking")
            .whereEqualTo("startTime", startTime)
            .get()
            .await()
        
        val conflictingBookings = existingBookings.documents.filter { doc ->
            val status = doc.getString("status") ?: "pending"
            status == "confirmed" || status == "pending"
        }

        if (conflictingBookings.isNotEmpty()) {
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
                "status" to "pending",
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status") ?: "pending"
                        // Include all booking states for real-time updates
                        BookingDoc(
                            docId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                            endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                            status = status,
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
                            val status = doc.getString("status") ?: "pending"
                            // Include all booking states for user bookings
                            BookingDoc(
                                docId = doc.id,
                                userId = doc.getString("userId") ?: "",
                                startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                                endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                                status = status,
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
                    val status = doc.getString("status") ?: "pending"
                    // Include all booking states for consultation history
                    BookingDoc(
                        docId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                        endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                        status = status,
                        createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    // Get current user profile
    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (snapshot.exists()) {
                UserProfile(
                    id = uid,
                    name = snapshot.getString("name") ?: "",
                    email = snapshot.getString("email") ?: "",
                    phone = snapshot.getString("phone") ?: "",
                    dateOfBirth = snapshot.getString("dateOfBirth") ?: "",
                    profilePictureUrl = snapshot.getString("profilePictureUrl") ?: "",
                    googleId = snapshot.getString("googleId") ?: "",
                    isGoogleSignIn = snapshot.getBoolean("isGoogleSignIn") ?: false,
                    createdAt = snapshot.getLong("createdAt") ?: 0L
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Update user phone number
    suspend fun updateUserPhoneNumber(phone: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            
            firestore.collection("users")
                .document(uid)
                .update("phone", phone)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


