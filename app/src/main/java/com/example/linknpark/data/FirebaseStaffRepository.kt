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
            Log.d(TAG, "Fetching parking stats for all spots")
            
            // Query all parking spots (ignoring lotId for now to get aggregate stats)
            val snapshot = firestore.collection("parking_spots")
                .get()
                .await()

            val totalSpots = snapshot.size()
            var availableSpots = 0
            var occupiedSpots = 0
            var reservedSpots = 0

            snapshot.documents.forEach { doc ->
                val status = doc.getString("status") ?: "AVAILABLE"
                
                when (status) {
                    "AVAILABLE" -> availableSpots++
                    "OCCUPIED" -> occupiedSpots++
                    "RESERVED" -> reservedSpots++
                }
            }

            val stats = ParkingStats(
                totalSpots = totalSpots,
                availableSpots = availableSpots,
                occupiedSpots = occupiedSpots,
                reservedSpots = reservedSpots
            )

            Log.d(TAG, "Stats: total=$totalSpots, available=$availableSpots, occupied=$occupiedSpots, reserved=$reservedSpots")
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
                .orderBy("entryTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { doc ->
                try {
                    ParkingSession(
                        sessionId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        lotId = doc.getString("lotId") ?: "",
                        spotCode = doc.getString("spotCode") ?: "",
                        spotNumber = doc.getLong("spotNumber")?.toInt() ?: 0,
                        licensePlate = doc.getString("licensePlate") ?: "",
                        carLabel = doc.getString("carLabel") ?: "",
                        vehicleType = doc.getString("vehicleType") ?: "STANDARD",
                        enteredAt = doc.getTimestamp("entryTime"),
                        exitedAt = doc.getTimestamp("exitTime"),
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        hourlyRate = doc.getDouble("hourlyRate") ?: 50.0,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                        paymentId = doc.getString("paymentId"),
                        paymentStatus = doc.getString("paymentStatus") ?: "PENDING",
                        status = doc.getString("status") ?: "ACTIVE",
                        entryMethod = doc.getString("entryMethod") ?: "DEVTOOLS",
                        exitMethod = doc.getString("exitMethod"),
                        createdAt = doc.getTimestamp("createdAt")
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
    
    // ============== NEW METHODS FOR MVP COMPLIANCE ==============
    
    override suspend fun createParkingSpot(
        code: String,
        type: String,
        hourlyRate: Double,
        lotId: String
    ): Result<String> {
        return try {
            val spotData = hashMapOf(
                "code" to code,
                "type" to type,
                "status" to "AVAILABLE",
                "hourly_rate" to hourlyRate,
                "lot_id" to lotId,
                "created_at" to Timestamp.now()
            )
            
            val docRef = firestore.collection("parking_spots")
                .add(spotData)
                .await()
            
            Log.d(TAG, "Parking spot created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating parking spot", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateParkingSpot(
        spotId: String,
        code: String,
        hourlyRate: Double,
        status: String
    ): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "code" to code,
                "hourly_rate" to hourlyRate,
                "status" to status,
                "updated_at" to Timestamp.now()
            )
            
            firestore.collection("parking_spots")
                .document(spotId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Parking spot updated: $spotId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating parking spot", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteParkingSpot(spotId: String): Result<Boolean> {
        return try {
            firestore.collection("parking_spots")
                .document(spotId)
                .delete()
                .await()
            
            Log.d(TAG, "Parking spot deleted: $spotId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting parking spot", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateSpotStatus(spotId: String, status: String): Result<Boolean> {
        return try {
            firestore.collection("parking_spots")
                .document(spotId)
                .update("status", status)
                .await()
            
            Log.d(TAG, "Spot status updated: $spotId -> $status")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating spot status", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingExits(lotId: String): Result<List<ParkingSession>> {
        return try {
            val snapshot = firestore.collection("parking_sessions")
                .whereEqualTo("status", "ACTIVE")
                .whereIn("paymentStatus", listOf("PENDING", "PENDING_CONFIRMATION"))  // ✅ FIXED
                .orderBy("entryTime", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val sessions = snapshot.documents.mapNotNull { doc ->
                try {
                    ParkingSession(
                        sessionId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        lotId = doc.getString("lotId") ?: "",
                        spotId = doc.getString("spotId"),  // ✅ Make sure this is populated
                        spotCode = doc.getString("spotCode") ?: "",
                        spotNumber = doc.getLong("spotNumber")?.toInt() ?: 0,
                        licensePlate = doc.getString("licensePlate") ?: "",
                        carLabel = doc.getString("carLabel") ?: "",
                        vehicleType = doc.getString("vehicleType") ?: "STANDARD",
                        enteredAt = doc.getTimestamp("entryTime"),
                        exitedAt = doc.getTimestamp("exitTime"),
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        hourlyRate = doc.getDouble("hourlyRate") ?: 0.0,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                        paymentId = doc.getString("paymentId"),
                        paymentStatus = doc.getString("paymentStatus") ?: "PENDING",
                        paymentMethod = doc.getString("paymentMethod"),
                        status = doc.getString("status") ?: "ACTIVE",
                        entryMethod = doc.getString("entryMethod") ?: "CAMERA",
                        exitMethod = doc.getString("exitMethod"),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing session: ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "✓ Pending exits fetched: ${sessions.size}")
            Result.success(sessions)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending exits", e)
            Result.failure(e)
        }
    }
    
    override suspend fun confirmPayment(
        sessionId: String,
        spotId: String?,
        totalAmount: Double,
        paymentMethod: String
    ): Result<Boolean> {
        return try {
            Log.d(TAG, "confirmPayment called - sessionId: $sessionId, spotId: $spotId")
            
            val now = Timestamp.now()
            val updates = hashMapOf<String, Any>(
                "status" to "COMPLETED",
                "exitTime" to now,
                "totalAmount" to totalAmount,
                "paymentStatus" to "PAID",
                "paymentMethod" to paymentMethod,
                "paidAt" to now,
                "confirmedBy" to "STAFF"
            )
            
            // Update session
            firestore.collection("parking_sessions")
                .document(sessionId)
                .update(updates)
                .await()
            
            Log.d(TAG, "✓ Session updated to COMPLETED")
            
            // Update spot status
            if (spotId.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ spotId is null/empty - cannot update spot")
            } else {
                Log.d(TAG, "Updating spot: $spotId → AVAILABLE")
                
                val spotUpdates = hashMapOf<String, Any>(
                    "status" to "AVAILABLE",
                    "currentSessionId" to "",  // Clear session reference
                    "currentCarLabel" to "",   // Clear license plate
                    "updatedAt" to now
                )
                
                firestore.collection("parking_spots")
                    .document(spotId)
                    .update(spotUpdates)
                    .await()
                
                Log.d(TAG, "✓ Spot $spotId marked AVAILABLE")
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error confirming payment", e)
            Result.failure(e)
        }
    }
    
    override suspend fun overrideFee(
        sessionId: String,
        newAmount: Double,
        reason: String
    ): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "totalAmount" to newAmount,
                "feeOverrideReason" to reason,
                "feeOverrideAt" to Timestamp.now()
            )
            
            firestore.collection("parking_sessions")
                .document(sessionId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Fee overridden for session: $sessionId -> $newAmount")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error overriding fee", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getActivityLogs(limit: Int, typeFilter: String?): Result<List<ActivityLog>> {
        return try {
            val snapshot = firestore.collection("parking_sessions")
                .orderBy("entryTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val logs = snapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status") ?: "ACTIVE"
                    val entryTime = doc.getTimestamp("entryTime")?.toDate()
                    val exitTime = doc.getTimestamp("exitTime")?.toDate()
                    val spotCode = doc.getString("spotCode") ?: ""
                    val licensePlate = doc.getString("licensePlate") ?: ""
                    
                    val isEntry = status == "ACTIVE" || exitTime == null
                    val type = if (isEntry) "🚗 Vehicle Entry" else "✓ Vehicle Exit"
                    
                    val dateFormat = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                    val time = if (isEntry && entryTime != null) {
                        dateFormat.format(entryTime)
                    } else if (!isEntry && exitTime != null) {
                        dateFormat.format(exitTime)
                    } else {
                        "N/A"
                    }
                    
                    ActivityLog(
                        type = type,
                        spotCode = spotCode,
                        licensePlate = licensePlate,
                        time = time,
                        isEntry = isEntry
                    )
                } catch (e: Exception) {
                    null
                }
            }.let { logs ->
                if (typeFilter != null && typeFilter != "ALL") {
                    when (typeFilter) {
                        "ENTRY" -> logs.filter { it.isEntry }
                        "EXIT" -> logs.filter { !it.isEntry }
                        else -> logs
                    }
                } else {
                    logs
                }
            }
            
            Log.d(TAG, "Activity logs fetched: ${logs.size}")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activity logs", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchLogs(query: String, limit: Int): Result<List<ActivityLog>> {
        return try {
            // Get all logs and filter client-side
            val logsResult = getActivityLogs(limit * 2, null)
            
            if (logsResult.isSuccess) {
                val allLogs = logsResult.getOrNull() ?: emptyList()
                val queryLower = query.lowercase()
                
                val filteredLogs = allLogs.filter { log ->
                    log.spotCode.lowercase().contains(queryLower) ||
                    log.licensePlate.lowercase().contains(queryLower)
                }.take(limit)
                
                Log.d(TAG, "Search logs for '$query': ${filteredLogs.size} results")
                Result.success(filteredLogs)
            } else {
                Result.failure(logsResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching logs", e)
            Result.failure(e)
        }
    }
}
