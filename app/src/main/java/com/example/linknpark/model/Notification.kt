package com.example.linknpark.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notification(
    val id: String = "",
    
    val type: String = "", // reservation_conflict, exit_ready, etc.
    
    val title: String = "",
    
    val message: String = "",
    
    val data: Map<String, Any> = emptyMap(),
    
    val timestamp: Timestamp? = null,
    
    var read: Boolean = false,
    
    @get:PropertyName("recipient_type") @set:PropertyName("recipient_type")
    var recipientType: String = "", // staff, driver
    
    @get:PropertyName("recipient_id") @set:PropertyName("recipient_id")
    var recipientId: String = "" // userId or "all"
) {
    // Helper to check if this is a conflict notification
    val isConflictNotification: Boolean
        get() = type == "reservation_conflict"
    
    // Get spot code from data if available
    val spotCode: String?
        get() = data["spot_code"] as? String
    
    // Get expected plate if applicable
    val expectedPlate: String?
        get() = data["expected_plate"] as? String
    
    // Get actual plate if applicable
    val actualPlate: String?
        get() = data["actual_plate"] as? String
}
