package com.example.linknpark.data

import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSession

interface StaffRepository {
    
    // Parking Spots
    fun observeAllParkingSpots(lotId: String = "main_lot", callback: (List<ParkingSpot>) -> Unit)
    fun removeSpotListener()
    suspend fun getParkingStats(lotId: String = "main_lot"): Result<ParkingStats>
    
    // Activity & Sessions
    suspend fun getRecentActivity(limit: Int = 10): Result<List<ParkingSession>>
    suspend fun getTodayRevenue(): Result<Double>
    suspend fun getTodayVehicleCount(): Result<Int>
}

data class ParkingStats(
    val totalSpots: Int,
    val availableSpots: Int,
    val occupiedSpots: Int,
    val reservedSpots: Int
)

