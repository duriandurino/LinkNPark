package com.example.linknpark.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ParkingSession(
    @get:PropertyName("session_id") @set:PropertyName("session_id")
    var sessionId: String = "",
    
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",
    
    @get:PropertyName("lot_id") @set:PropertyName("lot_id")
    var lotId: String = "",
    
    @get:PropertyName("spot_id") @set:PropertyName("spot_id")
    var spotId: String? = null,
    
    @get:PropertyName("spot_code") @set:PropertyName("spot_code")
    var spotCode: String = "",
    
    @get:PropertyName("spot_number") @set:PropertyName("spot_number")
    var spotNumber: Int = 0,
    
    @get:PropertyName("license_plate") @set:PropertyName("license_plate")
    var licensePlate: String = "",
    
    @get:PropertyName("car_label") @set:PropertyName("car_label")
    var carLabel: String = "",
    
    @get:PropertyName("vehicle_type") @set:PropertyName("vehicle_type")
    var vehicleType: String = "STANDARD",
    
    @get:PropertyName("entered_at") @set:PropertyName("entered_at")
    var enteredAt: Timestamp? = null,
    
    @get:PropertyName("parked_at") @set:PropertyName("parked_at")
    var parkedAt: Timestamp? = null,
    
    @get:PropertyName("exited_at") @set:PropertyName("exited_at")
    var exitedAt: Timestamp? = null,
    
    @get:PropertyName("duration_minutes") @set:PropertyName("duration_minutes")
    var durationMinutes: Int = 0,
    
    @get:PropertyName("hourly_rate") @set:PropertyName("hourly_rate")
    var hourlyRate: Double = 0.0,
    
    @get:PropertyName("total_amount") @set:PropertyName("total_amount")
    var totalAmount: Double = 0.0,
    
    @get:PropertyName("amount_paid") @set:PropertyName("amount_paid")
    var amountPaid: Double = 0.0,
    
    @get:PropertyName("payment_id") @set:PropertyName("payment_id")
    var paymentId: String? = null,
    
    @get:PropertyName("payment_status") @set:PropertyName("payment_status")
    var paymentStatus: String = "UNPAID", // UNPAID, PAID, PARTIAL, PENDING_CONFIRMATION
    
    @get:PropertyName("payment_method") @set:PropertyName("payment_method")
    var paymentMethod: String? = null, // Cash, GCash, Maya, Card, AUTO_PAY
    
    var status: String = "ACTIVE", // ACTIVE, COMPLETED, PAID, CANCELLED
    
    @get:PropertyName("entry_method") @set:PropertyName("entry_method")
    var entryMethod: String = "CAMERA", // MANUAL, CAMERA, APP
    
    @get:PropertyName("exit_method") @set:PropertyName("exit_method")
    var exitMethod: String? = null,
    
    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Timestamp? = null
) {
    // Use parkedAt if available, otherwise enteredAt
    val startTime: Timestamp
        get() = parkedAt ?: enteredAt ?: Timestamp.now()
    
    val endTime: Timestamp?
        get() = exitedAt
    
    // Calculate duration dynamically if not set
    val calculatedDurationMinutes: Long
        get() {
            val entry = parkedAt ?: enteredAt ?: return 0L
            val exit = exitedAt ?: Timestamp.now()
            return (exit.seconds - entry.seconds) / 60
        }
    
    // Calculate fee dynamically
    val calculatedAmount: Double
        get() {
            val hours = calculatedDurationMinutes / 60.0
            return hours * hourlyRate
        }
}
