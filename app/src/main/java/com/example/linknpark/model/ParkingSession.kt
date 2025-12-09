package com.example.linknpark.model

import com.google.firebase.Timestamp

data class ParkingSession(
    val sessionId: String = "",
    val userId: String = "",
    val lotId: String = "",
    val spotId: String? = null,
    val spotCode: String = "",
    val spotNumber: Int = 0,
    val licensePlate: String = "",
    val carLabel: String = "",
    val vehicleType: String = "STANDARD",
    val enteredAt: Timestamp? = null,
    val exitedAt: Timestamp? = null,
    val durationMinutes: Int = 0,
    val hourlyRate: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentId: String? = null,
    val paymentStatus: String = "UNPAID", // UNPAID, PAID, PARTIAL
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, PAID, CANCELLED
    val entryMethod: String = "CAMERA", // MANUAL, CAMERA, APP
    val exitMethod: String? = null,
    val createdAt: Timestamp? = null
) {
    val startTime: Timestamp
        get() = enteredAt ?: Timestamp.now()
    
    val endTime: Timestamp?
        get() = exitedAt
}

