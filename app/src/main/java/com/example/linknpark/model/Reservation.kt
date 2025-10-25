package com.example.linknpark.model

import com.google.firebase.Timestamp

data class Reservation(
    val reservationId: String = "",
    val userId: String = "",
    val lotId: String = "",
    val spotCode: String = "",
    val spotNumber: Int = 0,
    val licensePlate: String = "",
    val reserveStart: Timestamp? = null,
    val reserveEnd: Timestamp? = null,
    val reservedAt: Timestamp? = null,
    val durationHours: Int = 0,
    val totalAmount: Double = 0.0,
    val paymentId: String? = null,
    val paymentStatus: String = "UNPAID", // UNPAID, PAID
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, COMPLETED, CANCELLED
    val sessionId: String? = null,
    val createdAt: Timestamp? = null
)




