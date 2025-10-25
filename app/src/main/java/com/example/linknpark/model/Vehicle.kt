package com.example.linknpark.model

import com.google.firebase.Timestamp

data class Vehicle(
    val vehicleId: String = "",
    val userId: String = "",
    val licensePlate: String = "",
    val make: String = "",
    val model: String = "",
    val color: String = "",
    val vehicleType: String = "SEDAN", // SEDAN, SUV, VAN, MOTORCYCLE, TRUCK
    val year: Int = 0,
    val isPrimary: Boolean = false,
    val isVerified: Boolean = false,
    val registeredAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)




