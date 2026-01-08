package com.example.linknpark.data

import android.util.Log
import com.example.linknpark.model.ParkingLot
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Reservation
import com.example.linknpark.model.Vehicle
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

/**
 * Firestore Extension Functions
 * 
 * Provides type-safe conversion from Firestore documents to model objects.
 * Eliminates code duplication across repository classes.
 */

private const val TAG = "FirestoreExt"

// ========== ParkingSpot Extensions ==========

/**
 * Convert DocumentSnapshot to ParkingSpot model.
 * Returns null if parsing fails.
 */
fun DocumentSnapshot.toParkingSpot(): ParkingSpot? = try {
    ParkingSpot(
        spotId = id,
        lotId = getString("lot_id") ?: "",
        spotCode = getString("spot_code") ?: "",
        spotNumber = getLong("spot_number")?.toInt() ?: 0,
        isOccupied = getBoolean("is_occupied") ?: false,
        isReserved = getBoolean("is_reserved") ?: false,
        isAvailable = getBoolean("is_available") ?: true,
        occupiedBySessionId = getString("occupied_by_session_id"),
        reservedByUserId = getString("reserved_by_user_id"),
        currentCarLabel = getString("current_car_label"),
        status = getString("status") ?: "AVAILABLE",
        row = getString("row") ?: "",
        column = getLong("column")?.toInt() ?: 0,
        vehicleType = getString("vehicle_type") ?: "STANDARD",
        createdAt = getTimestamp("created_at"),
        updatedAt = getTimestamp("updated_at")
    )
} catch (e: Exception) {
    Log.e(TAG, "Error parsing ParkingSpot from document $id", e)
    null
}

/**
 * Convert QuerySnapshot to list of ParkingSpots.
 */
fun QuerySnapshot.toParkingSpots(): List<ParkingSpot> =
    documents.mapNotNull { it.toParkingSpot() }


// ========== Reservation Extensions ==========

/**
 * Convert DocumentSnapshot to Reservation model.
 */
fun DocumentSnapshot.toReservation(): Reservation? = try {
    Reservation(
        reservationId = id,
        userId = getString("user_id") ?: "",
        lotId = getString("lot_id") ?: "",
        spotCode = getString("spot_code") ?: "",
        spotNumber = getLong("spot_number")?.toInt() ?: 0,
        licensePlate = getString("license_plate") ?: "",
        reserveStart = getTimestamp("reserve_start"),
        reserveEnd = getTimestamp("reserve_end"),
        reservedAt = getTimestamp("reserved_at"),
        durationHours = getLong("duration_hours")?.toInt() ?: 0,
        totalAmount = getDouble("total_amount") ?: 0.0,
        paymentId = getString("payment_id"),
        paymentStatus = getString("payment_status") ?: "UNPAID",
        status = getString("status") ?: "ACTIVE",
        sessionId = getString("session_id"),
        createdAt = getTimestamp("created_at")
    )
} catch (e: Exception) {
    Log.e(TAG, "Error parsing Reservation from document $id", e)
    null
}

/**
 * Convert QuerySnapshot to list of Reservations.
 */
fun QuerySnapshot.toReservations(): List<Reservation> =
    documents.mapNotNull { it.toReservation() }


// ========== ParkingSession Extensions ==========

/**
 * Convert DocumentSnapshot to ParkingSession model.
 */
fun DocumentSnapshot.toParkingSession(): ParkingSession? = try {
    ParkingSession(
        sessionId = id,
        userId = getString("user_id") ?: "",
        lotId = getString("lotId") ?: getString("lot_id") ?: "",
        spotCode = getString("spotCode") ?: getString("spot_code") ?: "",
        spotNumber = getLong("spot_number")?.toInt() ?: 0,
        licensePlate = getString("licensePlate") ?: getString("license_plate") ?: "",
        carLabel = getString("car_label") ?: "",
        vehicleType = getString("vehicle_type") ?: "STANDARD",
        enteredAt = getTimestamp("enteredAt") ?: getTimestamp("entered_at"),
        exitedAt = getTimestamp("exitedAt") ?: getTimestamp("exited_at"),
        durationMinutes = getLong("duration_minutes")?.toInt() ?: 0,
        hourlyRate = getDouble("hourlyRate") ?: getDouble("hourly_rate") ?: 0.0,
        totalAmount = getDouble("totalAmount") ?: getDouble("total_amount") ?: 0.0,
        amountPaid = getDouble("amount_paid") ?: 0.0,
        paymentId = getString("payment_id"),
        paymentStatus = getString("paymentStatus") ?: getString("payment_status") ?: "PENDING",
        status = getString("status") ?: "ACTIVE",
        entryMethod = getString("entry_method") ?: "CAMERA",
        exitMethod = getString("exit_method"),
        createdAt = getTimestamp("created_at")
    )
} catch (e: Exception) {
    Log.e(TAG, "Error parsing ParkingSession from document $id", e)
    null
}

/**
 * Convert QuerySnapshot to list of ParkingSessions.
 */
fun QuerySnapshot.toParkingSessions(): List<ParkingSession> =
    documents.mapNotNull { it.toParkingSession() }


// ========== ParkingLot Extensions ==========

/**
 * Convert DocumentSnapshot to ParkingLot model.
 */
fun DocumentSnapshot.toParkingLot(): ParkingLot? = try {
    val location = get("location") as? com.google.firebase.firestore.GeoPoint 
        ?: com.google.firebase.firestore.GeoPoint(0.0, 0.0)
    
    ParkingLot(
        lotId = id,
        name = getString("name") ?: "",
        location = location,
        address = getString("address") ?: "",
        totalSpots = getLong("total_spots")?.toInt() ?: 0,
        availableSpots = getLong("available_spots")?.toInt() ?: 0,
        pricePerHour = getDouble("hourly_rate") ?: 0.0,  // Field is pricePerHour in model
        status = getString("status") ?: "ACTIVE"
    )
} catch (e: Exception) {
    Log.e(TAG, "Error parsing ParkingLot from document $id", e)
    null
}

/**
 * Convert QuerySnapshot to list of ParkingLots.
 */
fun QuerySnapshot.toParkingLots(): List<ParkingLot> =
    documents.mapNotNull { it.toParkingLot() }


// ========== Vehicle Extensions ==========

/**
 * Convert DocumentSnapshot to Vehicle model.
 */
fun DocumentSnapshot.toVehicle(): Vehicle? = try {
    Vehicle(
        vehicleId = id,
        userId = getString("user_id") ?: "",
        licensePlate = getString("license_plate") ?: "",
        make = getString("make") ?: "",
        model = getString("model") ?: "",
        color = getString("color") ?: "",
        vehicleType = getString("vehicle_type") ?: "SEDAN",
        year = getLong("year")?.toInt() ?: 0,
        isPrimary = getBoolean("is_primary") ?: false,
        isVerified = getBoolean("is_verified") ?: false,
        registeredAt = getTimestamp("registered_at"),
        updatedAt = getTimestamp("updated_at")
    )
} catch (e: Exception) {
    Log.e(TAG, "Error parsing Vehicle from document $id", e)
    null
}

/**
 * Convert QuerySnapshot to list of Vehicles.
 */
fun QuerySnapshot.toVehicles(): List<Vehicle> =
    documents.mapNotNull { it.toVehicle() }


// ========== Utility Functions ==========

/**
 * Count available spots in a QuerySnapshot of parking spots.
 */
fun QuerySnapshot.countAvailableSpots(): Int =
    documents.count { doc ->
        doc.getString("status") == "AVAILABLE"
    }

/**
 * Count occupied spots in a QuerySnapshot of parking spots.
 */
fun QuerySnapshot.countOccupiedSpots(): Int =
    documents.count { doc ->
        doc.getString("status") == "OCCUPIED"
    }
