package com.example.linknpark.data

import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.Reservation
import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Vehicle
import java.util.Date

interface DriverRepository {
    
    // Parking Spots
    suspend fun getAvailableParkingSpots(lotId: String = "main_lot"): Result<List<ParkingSpot>>
    suspend fun getAllParkingSpots(lotId: String = "main_lot"): Result<List<ParkingSpot>>
    fun observeParkingSpots(lotId: String = "main_lot", callback: (List<ParkingSpot>) -> Unit)
    fun removeSpotListener()
    suspend fun updateSpotStatus(spotId: String, status: String): Result<Boolean>
    
    // Reservations
    suspend fun reserveSpot(
        userId: String,
        lotId: String,
        spotCode: String,
        spotNumber: Int,
        licensePlate: String,
        durationHours: Int
    ): Result<Reservation>
    
    suspend fun getUserReservations(userId: String): Result<List<Reservation>>
    suspend fun cancelReservation(reservationId: String): Result<Boolean>
    fun observeUserReservations(userId: String, callback: (List<Reservation>) -> Unit)
    fun removeReservationListener()
    
    // Parking Sessions
    suspend fun getUserSessions(userId: String): Result<List<ParkingSession>>
    suspend fun getActiveUserSessions(userId: String): Result<List<ParkingSession>>
    fun observeUserActiveSessions(userId: String, callback: (List<ParkingSession>) -> Unit)
    fun removeSessionListener()
    
    // Session History with Filtering
    suspend fun getSessionHistory(
        userId: String,
        statusFilter: String? = null,
        startDate: Date? = null,
        endDate: Date? = null,
        limit: Int = 100
    ): Result<List<ParkingSession>>
    
    // Payment & Session Completion
    suspend fun completeSession(
        sessionId: String,
        spotId: String?,
        totalAmount: Double,
        paymentMethod: String
    ): Result<Boolean>
    
    // Vehicles
    suspend fun getUserVehicles(userId: String): Result<List<Vehicle>>
    suspend fun addVehicle(vehicle: Vehicle): Result<Vehicle>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Boolean>
    suspend fun deleteVehicle(vehicleId: String): Result<Boolean>
    suspend fun setPrimaryVehicle(userId: String, vehicleId: String): Result<Boolean>
}
