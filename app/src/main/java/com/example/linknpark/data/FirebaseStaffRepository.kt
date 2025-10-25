package com.example.linknpark.data

import android.util.Log
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirebaseStaffRepository : StaffRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseStaffRepo"
    
    private var spotListener: ListenerRegistration? = null

    override fun observeAllParkingSpots(lotId: String, callback: (List<ParkingSpot>) -> Unit) {
        Log.d(TAG, "Setting up real-time listener for all parking spots")
        
        spotListener?.remove()
        
        spotListener = firestore.collection("parking_spots")
            .whereEqualTo("lot_id", lotId)
            .orderBy("spot_number")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to parking spots", error)
                    callback(emptyList())
                    return@addSnapshotListener
                }

                val spots = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ParkingSpot(
                            spotId = doc.id,
                            lotId = doc.getString("lot_id") ?: "",
                            spotCode = doc.getString("spot_code") ?: "",
                            spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                            isOccupied = doc.getBoolean("is_occupied") ?: false,
                            isReserved = doc.getBoolean("is_reserved") ?: false,
                            isAvailable = doc.getBoolean("is_available") ?: true,
                            occupiedBySessionId = doc.getString("occupied_by_session_id"),
                            reservedByUserId = doc.getString("reserved_by_user_id"),
                            currentCarLabel = doc.getString("current_car_label"),
                            status = doc.getString("status") ?: "AVAILABLE",
                            row = doc.getString("row") ?: "",
                            column = doc.getLong("column")?.toInt() ?: 0,
                            vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                            createdAt = doc.getTimestamp("created_at"),
                            updatedAt = doc.getTimestamp("updated_at")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing spot in listener: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Real-time update: ${spots.size} spots")
                callback(spots)
            }
    }

    override fun removeSpotListener() {
        spotListener?.remove()
        spotListener = null
        Log.d(TAG, "Removed spot listener")
    }

    override suspend fun getParkingStats(lotId: String): Result<ParkingStats> {
        return try {
            Log.d(TAG, "Fetching parking stats for lot: $lotId")
            
            val snapshot = firestore.collection("parking_spots")
                .whereEqualTo("lot_id", lotId)
                .get()
                .await()

            val totalSpots = snapshot.size()
            var availableSpots = 0
            var occupiedSpots = 0
            var reservedSpots = 0

            snapshot.documents.forEach { doc ->
                val isAvailable = doc.getBoolean("is_available") ?: true
                val isOccupied = doc.getBoolean("is_occupied") ?: false
                val isReserved = doc.getBoolean("is_reserved") ?: false

                if (isAvailable) availableSpots++
                if (isOccupied) occupiedSpots++
                if (isReserved) reservedSpots++
            }

            val stats = ParkingStats(
                totalSpots = totalSpots,
                availableSpots = availableSpots,
                occupiedSpots = occupiedSpots,
                reservedSpots = reservedSpots
            )

            Log.d(TAG, "Stats: $stats")
            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching parking stats", e)
            Result.failure(e)
        }
    }

    override suspend fun getRecentActivity(limit: Int): Result<List<ParkingSession>> {
        return try {
            Log.d(TAG, "Fetching recent activity (limit: $limit)")
            
            val snapshot = firestore.collection("parking_sessions")
                .orderBy("start_time", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                try {
                    ParkingSession(
                        sessionId = doc.id,
                        userId = doc.getString("user_id") ?: "",
                        lotId = doc.getString("lot_id") ?: "",
                        spotCode = doc.getString("spot_code") ?: "",
                        spotNumber = doc.getLong("spot_number")?.toInt() ?: 0,
                        licensePlate = doc.getString("license_plate") ?: "",
                        vehicleType = doc.getString("vehicle_type") ?: "STANDARD",
                        startTime = doc.getTimestamp("start_time"),
                        endTime = doc.getTimestamp("end_time"),
                        durationMinutes = doc.getLong("duration_minutes")?.toInt() ?: 0,
                        amount = doc.getDouble("amount") ?: 0.0,
                        paymentStatus = doc.getString("payment_status") ?: "UNPAID",
                        status = doc.getString("status") ?: "ACTIVE",
                        reservationId = doc.getString("reservation_id"),
                        createdAt = doc.getTimestamp("created_at")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing session: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${sessions.size} recent activities")
            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent activity", e)
            Result.failure(e)
        }
    }

    override suspend fun getTodayRevenue(): Result<Double> {
        return try {
            Log.d(TAG, "Calculating today's revenue")
            
            // Get start of today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = Timestamp(calendar.time)

            val snapshot = firestore.collection("parking_sessions")
                .whereGreaterThanOrEqualTo("created_at", startOfDay)
                .whereEqualTo("payment_status", "PAID")
                .get()
                .await()

            var totalRevenue = 0.0
            snapshot.documents.forEach { doc ->
                val amount = doc.getDouble("amount") ?: 0.0
                totalRevenue += amount
            }

            Log.d(TAG, "Today's revenue: $totalRevenue")
            Result.success(totalRevenue)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating today's revenue", e)
            Result.failure(e)
        }
    }

    override suspend fun getTodayVehicleCount(): Result<Int> {
        return try {
            Log.d(TAG, "Counting today's vehicles")
            
            // Get start of today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = Timestamp(calendar.time)

            val snapshot = firestore.collection("parking_sessions")
                .whereGreaterThanOrEqualTo("created_at", startOfDay)
                .get()
                .await()

            val count = snapshot.size()
            Log.d(TAG, "Today's vehicle count: $count")
            Result.success(count)

        } catch (e: Exception) {
            Log.e(TAG, "Error counting today's vehicles", e)
            Result.failure(e)
        }
    }
}

