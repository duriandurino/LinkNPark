package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class PaymentPresenter : PaymentContract.Presenter {

    private var view: PaymentContract.View? = null
    private var session: ParkingSession? = null
    private var selectedPaymentMethod: String? = null
    private var calculatedTotalAmount: Double = 0.0
    
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()

    override fun attach(view: PaymentContract.View, session: ParkingSession) {
        this.view = view
        this.session = session
        loadSessionDetails()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    private fun loadSessionDetails() {
        val session = this.session ?: return
        
        // Format entry time
        val entryTime = session.enteredAt?.toDate()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val entryTimeStr = entryTime?.let { timeFormat.format(it) } ?: "Unknown"
        
        // Calculate duration
        val now = Date()
        val durationMs = now.time - (entryTime?.time ?: now.time)
        val durationMinutes = (durationMs / 1000 / 60).toInt()
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        
        // Calculate total amount
        val hourlyRate = session.hourlyRate ?: 50.0
        val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
        calculatedTotalAmount = billableHours * hourlyRate
        
        val rateStr = "₱${hourlyRate.toInt()}/hour"
        val totalStr = "₱${String.format("%.2f", calculatedTotalAmount)}"
        
        view?.showSessionDetails(
            spotCode = session.spotCode ?: "Unknown",
            entryTime = entryTimeStr,
            duration = durationStr,
            rate = rateStr,
            totalAmount = totalStr
        )
    }

    override fun onPaymentMethodSelected(method: String) {
        selectedPaymentMethod = method
        view?.showPaymentMethodSelected(method)
    }

    override fun onConfirmPayment() {
        val session = this.session ?: return
        val paymentMethod = selectedPaymentMethod ?: return
        
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Simulate payment processing (2 second delay)
                delay(2000)
                
                // Update session in Firestore
                val now = Timestamp.now()
                val updates = hashMapOf<String, Any>(
                    "status" to "COMPLETED",
                    "exitTime" to now,
                    "totalAmount" to calculatedTotalAmount,
                    "paymentStatus" to "PAID",
                    "paymentMethod" to paymentMethod,
                    "paidAt" to now
                )
                
                firestore.collection("parking_sessions")
                    .document(session.sessionId)
                    .update(updates)
                    .addOnSuccessListener {
                        // Also update the parking spot to AVAILABLE
                        session.spotId?.let { spotId ->
                            firestore.collection("parking_spots")
                                .document(spotId)
                                .update("status", "AVAILABLE")
                        }
                        
                        view?.showLoading(false)
                        view?.showPaymentSuccess("Payment of ₱${String.format("%.2f", calculatedTotalAmount)} successful via $paymentMethod!")
                        
                        // Navigate back after a short delay
                        presenterScope.launch {
                            delay(1500)
                            view?.navigateBack()
                        }
                    }
                    .addOnFailureListener { e ->
                        view?.showLoading(false)
                        view?.showPaymentError("Payment failed: ${e.message}")
                    }
                    
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showPaymentError("Payment failed: ${e.message}")
            }
        }
    }
}
