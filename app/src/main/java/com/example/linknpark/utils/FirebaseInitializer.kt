package com.example.linknpark.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * One-time utility to initialize Firebase Firestore collections with sample data.
 *
 * Usage: Call FirebaseInitializer.initializeDatabase() once from your app
 * After initialization, you can delete or disable this class.
 */
object FirebaseInitializer {

    private val firestore = FirebaseFirestore.getInstance()
    private const val TAG = "FirebaseInitializer"

    /**
     * Initialize all collections with sample data
     * @param forceReinitialize If true, will overwrite existing data
     */
    suspend fun initializeDatabase(forceReinitialize: Boolean = false): Result<String> {
        return try {
            Log.d(TAG, "Starting database initialization...")

            // Check if already initialized
            val existingLot = firestore.collection("parking_lots").document("main_lot").get().await()
            if (existingLot.exists() && !forceReinitialize) {
                return Result.failure(Exception("Database already initialized! Use 'Force Reinitialize' to overwrite existing data."))
            }

            // Initialize each collection
            createParkingLot()
            createParkingSpots()
            createSampleSession()
            createSampleReservation()
            createSampleVehicle()
            createSamplePayment()

            Log.d(TAG, "Database initialization completed successfully!")
            Result.success("Database initialized successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Database initialization failed", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all parking-related data (keeps users collection intact)
     */
    suspend fun clearParkingData(): Result<String> {
        return try {
            Log.d(TAG, "Clearing parking data...")

            val collectionsToDelete = listOf(
                "parking_lots",
                "parking_spots",
                "parking_sessions",
                "reservations",
                "vehicles",
                "payments",
                "activity_logs"
            )

            for (collectionName in collectionsToDelete) {
                val collection = firestore.collection(collectionName)
                val documents = collection.get().await()

                // Delete in batches
                val batch = firestore.batch()
                var count = 0
                for (doc in documents) {
                    batch.delete(doc.reference)
                    count++
                    if (count >= 500) { // Firestore batch limit
                        batch.commit().await()
                        count = 0
                    }
                }
                if (count > 0) {
                    batch.commit().await()
                }

                Log.d(TAG, "Deleted $collectionName (${documents.size()} documents)")
            }

            Log.d(TAG, "Parking data cleared successfully!")
            Result.success("Parking data cleared successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear parking data", e)
            Result.failure(e)
        }
    }

    /**
     * Complete data count for verification
     */
    suspend fun getDataStats(): Map<String, Int> {
        return try {
            mapOf(
                "parking_lots" to firestore.collection("parking_lots").get().await().size(),
                "parking_spots" to firestore.collection("parking_spots").get().await().size(),
                "parking_sessions" to firestore.collection("parking_sessions").get().await().size(),
                "reservations" to firestore.collection("reservations").get().await().size(),
                "vehicles" to firestore.collection("vehicles").get().await().size(),
                "payments" to firestore.collection("payments").get().await().size(),
                "users" to firestore.collection("users").get().await().size()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get data stats", e)
            emptyMap()
        }
    }

    /**
     * Create parking_lots collection with main_lot document
     */
    private suspend fun createParkingLot() {
        val parkingLotData = hashMapOf(
            "name" to "Main Street Parking Lot",
            "location" to GeoPoint(14.5995, 120.9842),
            "address" to "123 Main Street, Manila, Philippines",
            "total_spots" to 30,
            "available_spots" to 30,
            "occupied_spots" to 0,
            "hourly_rate" to 50.0,
            "currency" to "PHP",
            "operating_hours" to hashMapOf(
                "open" to "06:00",
                "close" to "22:00"
            ),
            "status" to "ACTIVE",
            "created_at" to Timestamp.now(),
            "updated_at" to Timestamp.now()
        )

        firestore.collection("parking_lots")
            .document("main_lot")
            .set(parkingLotData)
            .await()

        Log.d(TAG, "Created parking_lots collection with main_lot")
    }

    /**
     * Create parking_spots collection with 30 spots (A1-E6)
     */
    private suspend fun createParkingSpots() {
        val rows = listOf("A", "B", "C", "D", "E")
        val columns = 1..6
        var spotNumber = 1

        val batch = firestore.batch()
        var batchCount = 0

        for (row in rows) {
            for (column in columns) {
                val spotCode = "$row$column"
                val spotId = "spot_${spotNumber.toString().padStart(3, '0')}"

                val spotData = hashMapOf(
                    "lot_id" to "main_lot",
                    "spot_code" to spotCode,
                    "spot_number" to spotNumber,
                    "is_occupied" to false,
                    "is_reserved" to false,
                    "is_available" to true,
                    "occupied_by_session_id" to null,
                    "reserved_by_user_id" to null,
                    "current_car_label" to null,
                    "status" to "AVAILABLE",
                    "row" to row,
                    "column" to column,
                    "vehicle_type" to "STANDARD",
                    "created_at" to Timestamp.now(),
                    "updated_at" to Timestamp.now()
                )

                val docRef = firestore.collection("parking_spots").document(spotId)
                batch.set(docRef, spotData)
                batchCount++

                // Firestore batch limit is 500, commit every 30 documents
                if (batchCount >= 30) {
                    batch.commit().await()
                    batchCount = 0
                    Log.d(TAG, "Committed batch of parking spots")
                }

                spotNumber++
            }
        }

        // Commit remaining documents
        if (batchCount > 0) {
            batch.commit().await()
        }

        Log.d(TAG, "Created parking_spots collection with 30 spots")
    }

    /**
     * Create sample parking session
     */
    private suspend fun createSampleSession() {
        // Get first user to link session
        val userSnapshot = firestore.collection("users").limit(1).get().await()
        val userId = if (!userSnapshot.isEmpty) userSnapshot.documents[0].id else "test_user"

        val sessionData = hashMapOf(
            "session_id" to "session_001",
            "user_id" to userId,
            "lot_id" to "main_lot",
            "spot_code" to "A3",
            "spot_number" to 3,
            "license_plate" to "DBA 3163",
            "car_label" to "DBA 3163",
            "vehicle_type" to "STANDARD",
            "entered_at" to Timestamp(Timestamp.now().seconds - 7200, 0), // 2 hours ago
            "exited_at" to null,
            "duration_minutes" to 120,
            "hourly_rate" to 50.0,
            "total_amount" to 100.0,
            "amount_paid" to 0.0,
            "payment_id" to null,
            "payment_status" to "UNPAID",
            "status" to "ACTIVE",
            "entry_method" to "CAMERA",
            "exit_method" to null,
            "created_at" to Timestamp(Timestamp.now().seconds - 7200, 0)
        )

        firestore.collection("parking_sessions")
            .document("session_001")
            .set(sessionData)
            .await()

        // Update spot A3 to occupied
        firestore.collection("parking_spots")
            .document("spot_003")
            .update(
                mapOf(
                    "is_occupied" to true,
                    "is_available" to false,
                    "occupied_by_session_id" to "session_001",
                    "current_car_label" to "DBA 3163",
                    "status" to "OCCUPIED",
                    "updated_at" to Timestamp.now()
                )
            )
            .await()

        Log.d(TAG, "Created sample parking session")
    }

    /**
     * Create sample reservation
     */
    private suspend fun createSampleReservation() {
        val userSnapshot = firestore.collection("users").limit(1).get().await()
        val userId = if (!userSnapshot.isEmpty) userSnapshot.documents[0].id else "test_user"

        val reservationData = hashMapOf(
            "reservation_id" to "reservation_001",
            "user_id" to userId,
            "lot_id" to "main_lot",
            "spot_code" to "A5",
            "spot_number" to 5,
            "license_plate" to "XYZ 7890",
            "reserve_start" to Timestamp(Timestamp.now().seconds + 7200, 0), // 2 hours from now
            "reserve_end" to Timestamp(Timestamp.now().seconds + 14400, 0), // 4 hours from now
            "reserved_at" to Timestamp.now(),
            "duration_hours" to 2,
            "total_amount" to 100.0,
            "payment_id" to null,
            "payment_status" to "UNPAID",
            "status" to "ACTIVE",
            "session_id" to null,
            "created_at" to Timestamp.now()
        )

        firestore.collection("reservations")
            .add(reservationData)
            .await()

        // Update spot A5 to reserved
        firestore.collection("parking_spots")
            .document("spot_005")
            .update(
                mapOf(
                    "is_reserved" to true,
                    "is_available" to false,
                    "reserved_by_user_id" to userId,
                    "status" to "RESERVED",
                    "updated_at" to Timestamp.now()
                )
            )
            .await()

        Log.d(TAG, "Created sample reservation")
    }

    /**
     * Create sample vehicle
     */
    private suspend fun createSampleVehicle() {
        val userSnapshot = firestore.collection("users").limit(1).get().await()
        val userId = if (!userSnapshot.isEmpty) userSnapshot.documents[0].id else "test_user"

        val vehicleData = hashMapOf(
            "vehicle_id" to "vehicle_001",
            "user_id" to userId,
            "license_plate" to "DBA 3163",
            "make" to "Toyota",
            "model" to "Vios",
            "color" to "White",
            "vehicle_type" to "SEDAN",
            "year" to 2020,
            "is_primary" to true,
            "is_verified" to true,
            "registered_at" to Timestamp.now(),
            "updated_at" to Timestamp.now()
        )

        firestore.collection("vehicles")
            .add(vehicleData)
            .await()

        Log.d(TAG, "Created sample vehicle")
    }

    /**
     * Create sample payment
     */
    private suspend fun createSamplePayment() {
        val userSnapshot = firestore.collection("users").limit(1).get().await()
        val userId = if (!userSnapshot.isEmpty) userSnapshot.documents[0].id else "test_user"

        val paymentData = hashMapOf(
            "payment_id" to "payment_001",
            "user_id" to userId,
            "session_id" to "session_001",
            "reservation_id" to null,
            "amount" to 100.0,
            "currency" to "PHP",
            "payment_method" to "CASH",
            "payment_type" to "SESSION",
            "paid_at" to Timestamp.now(),
            "processed_by" to userId,
            "transaction_id" to "TXN-${System.currentTimeMillis()}",
            "receipt_number" to "RCP-001",
            "status" to "SUCCESS",
            "notes" to "Initial test payment",
            "created_at" to Timestamp.now()
        )

        firestore.collection("payments")
            .add(paymentData)
            .await()

        Log.d(TAG, "Created sample payment")
    }

    /**
     * Create activity log entry
     */
    private suspend fun createActivityLog(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        description: String
    ) {
        val logData = hashMapOf(
            "user_id" to userId,
            "action" to action,
            "entity_type" to entityType,
            "entity_id" to entityId,
            "description" to description,
            "old_value" to null,
            "new_value" to "ACTIVE",
            "ip_address" to "192.168.1.100",
            "user_agent" to "LinkNPark Android App v1.0",
            "performed_by" to "SYSTEM",
            "timestamp" to Timestamp.now(),
            "metadata" to hashMapOf(
                "source" to "initialization"
            )
        )

        firestore.collection("activity_logs")
            .add(logData)
            .await()
    }
}

