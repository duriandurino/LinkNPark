package com.example.linknpark.model

import com.google.firebase.Timestamp

data class ParkingSpot(
    val spotId: String = "",
    val lotId: String = "",
    val spotCode: String = "",
    val spotNumber: Int = 0,
    val isOccupied: Boolean = false,
    val isReserved: Boolean = false,
    val isAvailable: Boolean = true,
    val occupiedBySessionId: String? = null,
    val reservedByUserId: String? = null,
    val currentCarLabel: String? = null,
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, RESERVED, OUT_OF_SERVICE
    val row: String = "",
    val column: Int = 0,
    val vehicleType: String = "STANDARD",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)




