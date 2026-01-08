package com.example.linknpark.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Reservation(
    @get:PropertyName("reservation_id") @set:PropertyName("reservation_id")
    var reservationId: String = "",
    
    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",
    
    @get:PropertyName("lot_id") @set:PropertyName("lot_id")
    var lotId: String = "",
    
    @get:PropertyName("spot_code") @set:PropertyName("spot_code")
    var spotCode: String = "",
    
    @get:PropertyName("spot_number") @set:PropertyName("spot_number")
    var spotNumber: Int = 0,
    
    @get:PropertyName("license_plate") @set:PropertyName("license_plate")
    var licensePlate: String = "",
    
    @get:PropertyName("reserve_start") @set:PropertyName("reserve_start")
    var reserveStart: Timestamp? = null,
    
    @get:PropertyName("reserve_end") @set:PropertyName("reserve_end")
    var reserveEnd: Timestamp? = null,
    
    @get:PropertyName("reserved_at") @set:PropertyName("reserved_at")
    var reservedAt: Timestamp? = null,
    
    @get:PropertyName("duration_hours") @set:PropertyName("duration_hours")
    var durationHours: Int = 0,
    
    @get:PropertyName("total_amount") @set:PropertyName("total_amount")
    var totalAmount: Double = 0.0,
    
    @get:PropertyName("payment_id") @set:PropertyName("payment_id")
    var paymentId: String? = null,
    
    @get:PropertyName("payment_status") @set:PropertyName("payment_status")
    var paymentStatus: String = "UNPAID", // UNPAID, PAID
    
    var status: String = "ACTIVE", // ACTIVE, EXPIRED, COMPLETED, CANCELLED
    
    @get:PropertyName("session_id") @set:PropertyName("session_id")
    var sessionId: String? = null,
    
    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Timestamp? = null
) {
    // Helper properties for legacy code
    val startTime: Timestamp?
        get() = reserveStart
    
    val endTime: Timestamp?
        get() = reserveEnd
}
