package com.example.linknpark.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class ParkingLot(
    val lotId: String = "",
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",
    
    @get:PropertyName("total_spots")
    @set:PropertyName("total_spots")
    var totalSpots: Int = 0,
    
    @get:PropertyName("available_spots")
    @set:PropertyName("available_spots")
    var availableSpots: Int = 0,
    
    @get:PropertyName("hourly_rate")
    @set:PropertyName("hourly_rate")
    var pricePerHour: Double = 0.0,
    
    // status field from Firestore ("ACTIVE", "INACTIVE")
    val status: String = "ACTIVE"
) {
    // Helper property to check if lot is active
    val isActive: Boolean
        get() = status == "ACTIVE"
}

