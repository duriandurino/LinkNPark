package com.example.linknpark.model

import com.google.firebase.firestore.GeoPoint

data class ParkingLot(
    val lotId: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",
    val totalSpots: Int = 0,
    val availableSpots: Int = 0,
    val pricePerHour: Double = 0.0,
    val isActive: Boolean = true
)
