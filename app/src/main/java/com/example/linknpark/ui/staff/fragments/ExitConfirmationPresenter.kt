package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.model.ParkingSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import java.util.*

class ExitConfirmationPresenter : ExitConfirmationContract.Presenter {

    private var view: ExitConfirmationContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()

    override fun attach(view: ExitConfirmationContract.View) {
        this.view = view
        loadPendingExits()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    override fun loadPendingExits() {
        view?.showLoading(true)
        
        // Query sessions with status ACTIVE (ready to exit)
        firestore.collection("parking_sessions")
            .whereEqualTo("status", "ACTIVE")
            .orderBy("entryTime", Query.Direction.ASCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                val sessions = documents.mapNotNull { doc ->
                    try {
                        ParkingSession(
                            sessionId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            spotId = doc.getString("spotId"),
                            spotCode = doc.getString("spotCode") ?: "",
                            lotId = doc.getString("lotId") ?: "",
                            licensePlate = doc.getString("licensePlate") ?: "",
                            enteredAt = doc.getTimestamp("entryTime"),
                            hourlyRate = doc.getDouble("hourlyRate") ?: 50.0,
                            status = doc.getString("status") ?: "ACTIVE"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                view?.showLoading(false)
                view?.setRefreshing(false)
                view?.updatePendingCount(sessions.size)
                
                if (sessions.isEmpty()) {
                    view?.showEmptyState()
                } else {
                    view?.showPendingExits(sessions)
                }
            }
            .addOnFailureListener { e ->
                view?.showLoading(false)
                view?.setRefreshing(false)
                view?.showError("Failed to load pending exits: ${e.message}")
            }
    }

    override fun onConfirmPayment(session: ParkingSession) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Calculate final amount
                val now = Timestamp.now()
                val entryMs = session.enteredAt?.toDate()?.time ?: System.currentTimeMillis()
                val exitMs = now.toDate().time
                val durationMinutes = ((exitMs - entryMs) / 1000 / 60).toInt()
                val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
                val totalAmount = billableHours * (session.hourlyRate ?: 50.0)
                
                // Update session
                val updates = hashMapOf<String, Any>(
                    "status" to "COMPLETED",
                    "exitTime" to now,
                    "totalAmount" to totalAmount,
                    "paymentStatus" to "PAID",
                    "paymentMethod" to "CASH",
                    "confirmedBy" to "STAFF",
                    "completedAt" to now
                )
                
                firestore.collection("parking_sessions")
                    .document(session.sessionId)
                    .update(updates)
                    .addOnSuccessListener {
                        // Mark spot as available
                        session.spotId?.let { spotId ->
                            firestore.collection("parking_spots")
                                .document(spotId)
                                .update("status", "AVAILABLE")
                        }
                        
                        view?.showLoading(false)
                        view?.showPaymentSuccess("Payment confirmed: ₱${String.format("%.2f", totalAmount)}")
                        loadPendingExits() // Refresh list
                    }
                    .addOnFailureListener { e ->
                        view?.showLoading(false)
                        view?.showError("Failed to confirm payment: ${e.message}")
                    }
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }

    override fun onOverrideFee(session: ParkingSession) {
        // Calculate original amount
        val entryMs = session.enteredAt?.toDate()?.time ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val durationMinutes = ((now - entryMs) / 1000 / 60).toInt()
        val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
        val originalAmount = billableHours * (session.hourlyRate ?: 50.0)
        
        view?.showFeeOverrideDialog(session, originalAmount)
    }

    override fun onApplyFeeOverride(session: ParkingSession, newAmount: Double, reason: String) {
        view?.showLoading(true)
        
        val now = Timestamp.now()
        
        val updates = hashMapOf<String, Any>(
            "status" to "COMPLETED",
            "exitTime" to now,
            "totalAmount" to newAmount,
            "paymentStatus" to "PAID",
            "paymentMethod" to "CASH",
            "confirmedBy" to "STAFF",
            "feeOverride" to true,
            "overrideReason" to reason,
            "completedAt" to now
        )
        
        firestore.collection("parking_sessions")
            .document(session.sessionId)
            .update(updates)
            .addOnSuccessListener {
                // Mark spot as available
                session.spotId?.let { spotId ->
                    firestore.collection("parking_spots")
                        .document(spotId)
                        .update("status", "AVAILABLE")
                }
                
                view?.showLoading(false)
                view?.showPaymentSuccess("Override applied: ₱${String.format("%.2f", newAmount)}")
                loadPendingExits() // Refresh list
            }
            .addOnFailureListener { e ->
                view?.showLoading(false)
                view?.showError("Failed to apply override: ${e.message}")
            }
    }

    override fun onRefresh() {
        loadPendingExits()
    }
}
