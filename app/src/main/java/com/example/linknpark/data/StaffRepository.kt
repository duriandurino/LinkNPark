package com.example.linknpark.data

import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSession

interface StaffRepository {
    
    // Parking Spots - Observation
    fun observeAllParkingSpots(lotId: String = "main_lot", callback: (List<ParkingSpot>) -> Unit)
    fun removeSpotListener()
    suspend fun getParkingStats(lotId: String = "main_lot"): Result<ParkingStats>
    
    // Parking Spots - CRUD
    suspend fun createParkingSpot(
        code: String,
        type: String,
        hourlyRate: Double,
        lotId: String = "main_lot"
    ): Result<String>
    
    suspend fun updateParkingSpot(
        spotId: String,
        code: String,
        hourlyRate: Double,
        status: String
    ): Result<Boolean>
    
    suspend fun deleteParkingSpot(spotId: String): Result<Boolean>
    suspend fun updateSpotStatus(spotId: String, status: String): Result<Boolean>
    
    // Exit Confirmation & Payments
    suspend fun getPendingExits(lotId: String = "main_lot"): Result<List<ParkingSession>>
    
    suspend fun confirmPayment(
        sessionId: String,
        spotId: String?,
        totalAmount: Double,
        paymentMethod: String
    ): Result<Boolean>
    
    suspend fun overrideFee(
        sessionId: String,
        newAmount: Double,
        reason: String
    ): Result<Boolean>
    
    // Activity & Sessions
    suspend fun getRecentActivity(limit: Int = 10): Result<List<ParkingSession>>
    suspend fun getTodayRevenue(): Result<Double>
    suspend fun getTodayVehicleCount(): Result<Int>
    
    // Activity Logs
    suspend fun getActivityLogs(
        limit: Int = 100,
        typeFilter: String? = null
    ): Result<List<ActivityLog>>
    
    suspend fun searchLogs(
        query: String,
        limit: Int = 100
    ): Result<List<ActivityLog>>
}

data class ParkingStats(
    val totalSpots: Int,
    val availableSpots: Int,
    val occupiedSpots: Int,
    val reservedSpots: Int
)

data class ActivityLog(
    val type: String,
    val spotCode: String,
    val licensePlate: String,
    val time: String,
    val isEntry: Boolean = true
)
