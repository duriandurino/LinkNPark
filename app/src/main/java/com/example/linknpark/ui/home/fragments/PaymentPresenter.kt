package com.example.linknpark.ui.home.fragments

import com.example.linknpark.data.DriverRepository
import com.example.linknpark.model.ParkingSession
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class PaymentPresenter(
    private val repository: DriverRepository
) : PaymentContract.Presenter {

    private var view: PaymentContract.View? = null
    private var session: ParkingSession? = null
    private var selectedPaymentMethod: String? = null
    private var calculatedTotalAmount: Double = 0.0
    
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        val hourlyRate = session.hourlyRate
        val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
        calculatedTotalAmount = billableHours * hourlyRate
        
        val rateStr = "₱${hourlyRate.toInt()}/hour"
        val totalStr = "₱${String.format("%.2f", calculatedTotalAmount)}"
        
        view?.showSessionDetails(
            spotCode = session.spotCode,
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
                // Simulate payment processing (1.5 second delay)
                delay(1500)
                
                // Mark payment as pending staff confirmation
                val result = repository.markPaymentPending(
                    sessionId = session.sessionId,
                    totalAmount = calculatedTotalAmount,
                    paymentMethod = paymentMethod
                )
                
                if (result.isSuccess) {
                    view?.showLoading(false)
                    view?.showPaymentSuccess(
                        "Payment submitted (₱${String.format("%.2f", calculatedTotalAmount)}). " +
                        "Please proceed to exit gate. Staff will confirm your payment."
                    )
                    
                    // Navigate back after a short delay
                    delay(2000)
                    view?.navigateBack()
                } else {
                    view?.showLoading(false)
                    view?.showPaymentError("Failed to submit payment: ${result.exceptionOrNull()?.message}")
                }
                    
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showPaymentError("Payment failed: ${e.message}")
            }
        }
    }
}
