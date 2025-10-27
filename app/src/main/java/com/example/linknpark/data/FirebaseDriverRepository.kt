package com.example.linknpark.data

import android.util.Log
import com.example.linknpark.model.ParkingLot
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.Reservation
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Vehicle
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseDriverRepository : DriverRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseDriverRepo"

    private var parkingLotListener: ListenerRegistration? = null
    private var spotListener: ListenerRegistration? = null
    private var reservationListener: ListenerRegistration? = null
    private var sessionListener: ListenerRegistration? = null

    // ========== Parking Lots ==========

    fun observeParkingLots(callback: (List<ParkingLot>) -> Unit) {
        Log.d(TAG, "Setting up real-time listener for parking lots")

        parkingLotListener?.remove()

        parkingLotListener = firestore.collection("parking_lots")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to parking lots", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val lots = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ParkingLot::class.java)?.copy(lotId = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing parking lot in listener: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Real-time update: ${lots.size} parking lots")
                callback(lots)
            }
    }

    suspend fun searchParkingLots(query: String): List<ParkingLot> {
        return try {
            Log.d(TAG, "Searching parking lots with query: $query")

            if (query.isEmpty()) {
                // Return all active lots if query is empty
                val snapshot = firestore.collection("parking_lots")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()

                return snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ParkingLot::class.java)?.copy(lotId = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing parking lot: ${doc.id}", e)
                        null
                    }
                }
            }

            // Search by name and address (Firestore doesn't support OR, so we need two queries)
            val lowerQuery = query.lowercase()

            val nameResults = firestore.collection("parking_lots")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val results = nameResults.documents.mapNotNull { doc ->
                try {
                    val lot = doc.toObject(ParkingLot::class.java)?.copy(lotId = doc.id)
                    val name = lot?.name?.lowercase() ?: ""
                    val address = lot?.address?.lowercase() ?: ""

                    if (name.contains(lowerQuery) || address.contains(lowerQuery)) {
                        lot
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing parking lot: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${results.size} parking lots matching query")
            results

        } catch (e: Exception) {
            Log.e(TAG, "Error searching parking lots", e)
            emptyList()
        }
    }

    suspend fun getParkingLotById(lotId: String): ParkingLot? {
        return try {
            Log.d(TAG, "Fetching parking lot: $lotId")

            val doc = firestore.collection("parking_lots")
                .document(lotId)
                .get()
                .await()

            if (!doc.exists()) {
                Log.w(TAG, "Parking lot not found: $lotId")
                return null
            }

            val lot = doc.toObject(ParkingLot::class.java)?.copy(lotId = doc.id)

            if (lot?.isActive == false) {
                Log.w(TAG, "Parking lot is inactive: $lotId")
                return null
            }

            lot

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching parking lot", e)
            null
        }
    }

    fun removeParkingLotListener() {
        parkingLotListener?.remove()
        parkingLotListener = null
        Log.d(TAG, "Removed parking lot listener")
    }

    // ========== Parking Spots ==========

    override suspend fun getAvailableParkingSpots(lotId: String): Result<List<ParkingSpot>> {
        return try {
            Log.d(TAG, "Fetching available parking spots for lot: $lotId")
            
            val snapshot = firestore.collection("parking_spots")
                .whereEqualTo("lot_id", lotId)
                .whereEqualTo("is_available", true)
                .get()
                .await()

            val spots = snapshot.documents.mapNotNull { doc ->
                try {
                    ParkingSpot(
                        spotId = doc.id,
                        lotId = doc.getString("lot_id") ?: "",
                        spotCode = doc.getString("spot_code") ?: "",
                        spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                        isOccupied = doc.getBoolean("is_occupied") ?: false,
                        isReserved = doc.getBoolean("is_reserved") ?: false,
                        isAvailable = doc.getBoolean("is_available") ?: true,
                        occupiedBySessionId = doc.getString("occupied_by_session_id"),
                        reservedByUserId = doc.getString("reserved_by_user_id"),
                        currentCarLabel = doc.getString("current_car_label"),
                        status = doc.getString("status") ?: "AVAILABLE",
                        row = doc.getString("row") ?: "",
                        column = doc.getLong("column")?.toInt() ?: 0,
                        vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                        createdAt = doc.getTimestamp("created_at"),
                        updatedAt = doc.getTimestamp("updated_at")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing spot document: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${spots.size} available spots")
            Result.success(spots)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available parking spots", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllParkingSpots(lotId: String): Result<List<ParkingSpot>> {
        return try {
            Log.d(TAG, "Fetching all parking spots for lot: $lotId")
            
            val snapshot = firestore.collection("parking_spots")
                .whereEqualTo("lot_id", lotId)
                .orderBy("spot_number")
                .get()
                .await()

            val spots = snapshot.documents.mapNotNull { doc ->
                try {
                    ParkingSpot(
                        spotId = doc.id,
                        lotId = doc.getString("lot_id") ?: "",
                        spotCode = doc.getString("spot_code") ?: "",
                        spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                        isOccupied = doc.getBoolean("is_occupied") ?: false,
                        isReserved = doc.getBoolean("is_reserved") ?: false,
                        isAvailable = doc.getBoolean("is_available") ?: true,
                        occupiedBySessionId = doc.getString("occupied_by_session_id"),
                        reservedByUserId = doc.getString("reserved_by_user_id"),
                        currentCarLabel = doc.getString("current_car_label"),
                        status = doc.getString("status") ?: "AVAILABLE",
                        row = doc.getString("row") ?: "",
                        column = doc.getLong("column")?.toInt() ?: 0,
                        vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                        createdAt = doc.getTimestamp("created_at"),
                        updatedAt = doc.getTimestamp("updated_at")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing spot document: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${spots.size} total spots")
            Result.success(spots)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all parking spots", e)
            Result.failure(e)
        }
    }

    override fun observeParkingSpots(lotId: String, callback: (List<ParkingSpot>) -> Unit) {
        Log.d(TAG, "Setting up real-time listener for parking spots")
        
        spotListener?.remove()
        
        spotListener = firestore.collection("parking_spots")
            .whereEqualTo("lot_id", lotId)
            .orderBy("spot_number")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to parking spots", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val spots = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ParkingSpot(
                            spotId = doc.id,
                            lotId = doc.getString("lot_id") ?: "",
                            spotCode = doc.getString("spot_code") ?: "",
                            spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                            isOccupied = doc.getBoolean("is_occupied") ?: false,
                            isReserved = doc.getBoolean("is_reserved") ?: false,
                            isAvailable = doc.getBoolean("is_available") ?: true,
                            occupiedBySessionId = doc.getString("occupied_by_session_id"),
                            reservedByUserId = doc.getString("reserved_by_user_id"),
                            currentCarLabel = doc.getString("current_car_label"),
                            status = doc.getString("status") ?: "AVAILABLE",
                            row = doc.getString("row") ?: "",
                            column = doc.getLong("column")?.toInt() ?: 0,
                            vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                            createdAt = doc.getTimestamp("created_at"),
                            updatedAt = doc.getTimestamp("updated_at")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing spot in listener: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Real-time update: ${spots.size} spots")
                callback(spots)
            }
    }

    override fun removeSpotListener() {
        spotListener?.remove()
        spotListener = null
        Log.d(TAG, "Removed spot listener")
    }

    // ========== Reservations ==========

    override suspend fun reserveSpot(
        userId: String,
        lotId: String,
        spotCode: String,
        spotNumber: Int,
        licensePlate: String,
        durationHours: Int
    ): Result<Reservation> {
        return try {
            Log.d(TAG, "Creating reservation for spot: $spotCode in lot: $lotId")

            val now = Timestamp.now()
            val reserveStart = Timestamp(now.seconds, now.nanoseconds)
            val reserveEnd = Timestamp(now.seconds + (durationHours * 3600), now.nanoseconds)

            val reservationData = hashMapOf(
                "user_id" to userId,
                "lot_id" to lotId,
                "spot_code" to spotCode,
                "spot_number" to spotNumber,
                "license_plate" to licensePlate,
                "reserve_start" to reserveStart,
                "reserve_end" to reserveEnd,
                "reserved_at" to now,
                "duration_hours" to durationHours,
                "total_amount" to (50.0 * durationHours), // PHP 50/hour
                "payment_id" to null,
                "payment_status" to "UNPAID",
                "status" to "ACTIVE",
                "session_id" to null,
                "created_at" to now
            )

            val docRef = firestore.collection("reservations").add(reservationData).await()
            
            // Update parking spot to reserved
            firestore.collection("parking_spots")
                .whereEqualTo("spot_code", spotCode)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.reference
                ?.update(
                    mapOf(
                        "is_reserved" to true,
                        "is_available" to false,
                        "reserved_by_user_id" to userId,
                        "status" to "RESERVED",
                        "updated_at" to now
                    )
                )
                ?.await()

            val reservation = Reservation(
                reservationId = docRef.id,
                userId = userId,
                lotId = lotId,
                spotCode = spotCode,
                spotNumber = spotNumber,
                licensePlate = licensePlate,
                reserveStart = reserveStart,
                reserveEnd = reserveEnd,
                reservedAt = now,
                durationHours = durationHours,
                totalAmount = 50.0 * durationHours,
                paymentId = null,
                paymentStatus = "UNPAID",
                status = "ACTIVE",
                sessionId = null,
                createdAt = now
            )

            Log.d(TAG, "Reservation created successfully: ${docRef.id}")
            Result.success(reservation)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating reservation", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserReservations(userId: String): Result<List<Reservation>> {
        return try {
            Log.d(TAG, "Fetching reservations for user: $userId")

            val snapshot = firestore.collection("reservations")
                .whereEqualTo("user_id", userId)
                .whereIn("status", listOf("ACTIVE", "COMPLETED"))
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val reservations = snapshot.documents.mapNotNull { doc ->
                try {
                    Reservation(
                        reservationId = doc.id,
                        userId = doc.getString("user_id") ?: "",
                        lotId = doc.getString("lot_id") ?: "",
                        spotCode = doc.getString("spot_code") ?: "",
                        spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                        licensePlate = doc.getString("license_plate") ?: "",
                        reserveStart = doc.getTimestamp("reserve_start"),
                        reserveEnd = doc.getTimestamp("reserve_end"),
                        reservedAt = doc.getTimestamp("reserved_at"),
                        durationHours = doc.getLong("duration_hours")?.toInt() ?: 0,
                        totalAmount = doc.getDouble("total_amount") ?: 0.0,
                        paymentId = doc.getString("payment_id"),
                        paymentStatus = doc.getString("payment_status") ?: "UNPAID",
                        status = doc.getString("status") ?: "ACTIVE",
                        sessionId = doc.getString("session_id"),
                        createdAt = doc.getTimestamp("created_at")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing reservation: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${reservations.size} reservations")
            Result.success(reservations)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user reservations", e)
            Result.failure(e)
        }
    }

    override suspend fun cancelReservation(reservationId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Cancelling reservation: $reservationId")

            val reservationDoc = firestore.collection("reservations")
                .document(reservationId)
                .get()
                .await()

            val spotCode = reservationDoc.getString("spot_code")

            // Update reservation status
            firestore.collection("reservations")
                .document(reservationId)
                .update("status", "CANCELLED")
                .await()

            // Update parking spot
            if (spotCode != null) {
                firestore.collection("parking_spots")
                    .whereEqualTo("spot_code", spotCode)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
                    ?.update(
                        mapOf(
                            "is_reserved" to false,
                            "is_available" to true,
                            "reserved_by_user_id" to null,
                            "status" to "AVAILABLE",
                            "updated_at" to Timestamp.now()
                        )
                    )
                    ?.await()
            }

            Log.d(TAG, "Reservation cancelled successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling reservation", e)
            Result.failure(e)
        }
    }

    override fun observeUserReservations(userId: String, callback: (List<Reservation>) -> Unit) {
        Log.d(TAG, "Setting up real-time listener for user reservations")
        
        reservationListener?.remove()
        
        reservationListener = firestore.collection("reservations")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("status", "ACTIVE")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to reservations", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val reservations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Reservation(
                            reservationId = doc.id,
                            userId = doc.getString("user_id") ?: "",
                            lotId = doc.getString("lot_id") ?: "",
                            spotCode = doc.getString("spot_code") ?: "",
                            spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                            licensePlate = doc.getString("license_plate") ?: "",
                            reserveStart = doc.getTimestamp("reserve_start"),
                            reserveEnd = doc.getTimestamp("reserve_end"),
                            reservedAt = doc.getTimestamp("reserved_at"),
                            durationHours = doc.getLong("duration_hours")?.toInt() ?: 0,
                            totalAmount = doc.getDouble("total_amount") ?: 0.0,
                            paymentId = doc.getString("payment_id"),
                            paymentStatus = doc.getString("payment_status") ?: "UNPAID",
                            status = doc.getString("status") ?: "ACTIVE",
                            sessionId = doc.getString("session_id"),
                            createdAt = doc.getTimestamp("created_at")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing reservation in listener: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Real-time update: ${reservations.size} active reservations")
                callback(reservations)
            }
    }

    override fun removeReservationListener() {
        reservationListener?.remove()
        reservationListener = null
        Log.d(TAG, "Removed reservation listener")
    }

    // ========== Parking Sessions ==========

    override suspend fun getUserSessions(userId: String): Result<List<ParkingSession>> {
        return try {
            Log.d(TAG, "Fetching all sessions for user: $userId")

            val snapshot = firestore.collection("parking_sessions")
                .whereEqualTo("user_id", userId)
                .orderBy("entered_at", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                parseParkingSession(doc)
            }

            Log.d(TAG, "Found ${sessions.size} sessions")
            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user sessions", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveUserSessions(userId: String): Result<List<ParkingSession>> {
        return try {
            Log.d(TAG, "Fetching active sessions for user: $userId")

            val snapshot = firestore.collection("parking_sessions")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                parseParkingSession(doc)
            }

            Log.d(TAG, "Found ${sessions.size} active sessions")
            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching active sessions", e)
            Result.failure(e)
        }
    }

    override fun observeUserActiveSessions(userId: String, callback: (List<ParkingSession>) -> Unit) {
        Log.d(TAG, "Setting up real-time listener for active sessions")
        
        sessionListener?.remove()
        
        sessionListener = firestore.collection("parking_sessions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to sessions", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    parseParkingSession(doc)
                } ?: emptyList()

                Log.d(TAG, "Real-time update: ${sessions.size} active sessions")
                callback(sessions)
            }
    }

    override fun removeSessionListener() {
        sessionListener?.remove()
        sessionListener = null
        Log.d(TAG, "Removed session listener")
    }

    private fun parseParkingSession(doc: com.google.firebase.firestore.DocumentSnapshot): ParkingSession? {
        return try {
            ParkingSession(
                sessionId = doc.id,
                userId = doc.getString("user_id") ?: "",
                lotId = doc.getString("lot_id") ?: "",
                spotCode = doc.getString("spot_code") ?: "",
                spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                licensePlate = doc.getString("license_plate") ?: "",
                carLabel = doc.getString("car_label") ?: "",
                vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                enteredAt = doc.getTimestamp("entered_at"),
                exitedAt = doc.getTimestamp("exited_at"),
                durationMinutes = doc.getLong("duration_minutes")?.toInt() ?: 0,
                hourlyRate = doc.getDouble("hourly_rate") ?: 0.0,
                totalAmount = doc.getDouble("total_amount") ?: 0.0,
                amountPaid = doc.getDouble("amount_paid") ?: 0.0,
                paymentId = doc.getString("payment_id"),
                paymentStatus = doc.getString("payment_status") ?: "UNPAID",
                status = doc.getString("status") ?: "ACTIVE",
                entryMethod = doc.getString("entry_method") ?: "CAMERA",
                exitMethod = doc.getString("exit_method"),
                createdAt = doc.getTimestamp("created_at")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing session: ${doc.id}", e)
            null
        }
    }

    // ========== Vehicles ==========

    override suspend fun getUserVehicles(userId: String): Result<List<Vehicle>> {
        return try {
            Log.d(TAG, "Fetching vehicles for user: $userId")

            val snapshot = firestore.collection("vehicles")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            val vehicles = snapshot.documents.mapNotNull { doc ->
                try {
                    Vehicle(
                        vehicleId = doc.id,
                        userId = doc.getString("user_id") ?: "",
                        licensePlate = doc.getString("license_plate") ?: "",
                        make = doc.getString("make") ?: "",
                        model = doc.getString("model") ?: "",
                        color = doc.getString("color") ?: "",
                        vehicleType = doc.getString("vehicle_type") ?: "SEDAN",
                        year = doc.getLong("year")?.toInt() ?: 0,
                        isPrimary = doc.getBoolean("is_primary") ?: false,
                        isVerified = doc.getBoolean("is_verified") ?: false,
                        registeredAt = doc.getTimestamp("registered_at"),
                        updatedAt = doc.getTimestamp("updated_at")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing vehicle: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${vehicles.size} vehicles")
            Result.success(vehicles)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user vehicles", e)
            Result.failure(e)
        }
    }

    override suspend fun addVehicle(vehicle: Vehicle): Result<Vehicle> {
        return try {
            Log.d(TAG, "Adding new vehicle: ${vehicle.licensePlate}")

            val now = Timestamp.now()
            val vehicleData = hashMapOf(
                "user_id" to vehicle.userId,
                "license_plate" to vehicle.licensePlate,
                "make" to vehicle.make,
                "model" to vehicle.model,
                "color" to vehicle.color,
                "vehicle_type" to vehicle.vehicleType,
                "year" to vehicle.year,
                "is_primary" to vehicle.isPrimary,
                "is_verified" to false,
                "registered_at" to now,
                "updated_at" to now
            )

            val docRef = firestore.collection("vehicles").add(vehicleData).await()

            val newVehicle = vehicle.copy(
                vehicleId = docRef.id,
                registeredAt = now,
                updatedAt = now
            )

            Log.d(TAG, "Vehicle added successfully: ${docRef.id}")
            Result.success(newVehicle)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding vehicle", e)
            Result.failure(e)
        }
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Boolean> {
        return try {
            Log.d(TAG, "Updating vehicle: ${vehicle.vehicleId}")

            val updateData = hashMapOf(
                "license_plate" to vehicle.licensePlate,
                "make" to vehicle.make,
                "model" to vehicle.model,
                "color" to vehicle.color,
                "vehicle_type" to vehicle.vehicleType,
                "year" to vehicle.year,
                "updated_at" to Timestamp.now()
            )

            firestore.collection("vehicles")
                .document(vehicle.vehicleId)
                .update(updateData as Map<String, Any>)
                .await()

            Log.d(TAG, "Vehicle updated successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating vehicle", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteVehicle(vehicleId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Deleting vehicle: $vehicleId")

            firestore.collection("vehicles")
                .document(vehicleId)
                .delete()
                .await()

            Log.d(TAG, "Vehicle deleted successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting vehicle", e)
            Result.failure(e)
        }
    }

    override suspend fun setPrimaryVehicle(userId: String, vehicleId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Setting primary vehicle: $vehicleId")

            // First, set all user's vehicles to non-primary
            val userVehicles = firestore.collection("vehicles")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            val batch = firestore.batch()
            userVehicles.documents.forEach { doc ->
                batch.update(doc.reference, "is_primary", doc.id == vehicleId)
            }
            batch.commit().await()

            Log.d(TAG, "Primary vehicle set successfully")
            Result.success(true)

        } catch (e: Exception) {
            Log.e(TAG, "Error setting primary vehicle", e)
            Result.failure(e)
        }
    }
}

