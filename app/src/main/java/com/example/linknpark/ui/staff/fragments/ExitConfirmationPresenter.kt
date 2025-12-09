package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.data.FirebaseStaffRepository
import com.example.linknpark.data.StaffRepository
import com.example.linknpark.model.ParkingSession
import kotlinx.coroutines.*
import java.util.*

class ExitConfirmationPresenter(
    private val repository: StaffRepository = FirebaseStaffRepository()
) : ExitConfirmationContract.Presenter {

    private var view: ExitConfirmationContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getPendingExits("main_lot")
                }
                
                result.onSuccess { sessions ->
                    view?.showLoading(false)
                    view?.setRefreshing(false)
                    view?.updatePendingCount(sessions.size)
                    
                    if (sessions.isEmpty()) {
                        view?.showEmptyState()
                    } else {
                        view?.showPendingExits(sessions)
                    }
                }.onFailure { error ->
                    view?.showLoading(false)
                    view?.setRefreshing(false)
                    view?.showError("Failed to load pending exits: ${error.message}")
                }
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.setRefreshing(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }

    override fun onConfirmPayment(session: ParkingSession) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Calculate final amount
                val entryMs = session.enteredAt?.toDate()?.time ?: System.currentTimeMillis()
                val exitMs = System.currentTimeMillis()
                val durationMinutes = ((exitMs - entryMs) / 1000 / 60).toInt()
                val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
                val totalAmount = billableHours * session.hourlyRate
                
                // Use repository to confirm payment
                val result = withContext(Dispatchers.IO) {
                    repository.confirmPayment(
                        sessionId = session.sessionId,
                        spotId = session.spotId,
                        totalAmount = totalAmount,
                        paymentMethod = "CASH"
                    )
                }
                
                result.onSuccess {
                    view?.showLoading(false)
                    view?.showPaymentSuccess("Payment confirmed: ₱${String.format("%.2f", totalAmount)}")
                    loadPendingExits() // Refresh list
                }.onFailure { error ->
                    view?.showLoading(false)
                    view?.showError("Failed to confirm payment: ${error.message}")
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
        val originalAmount = billableHours * session.hourlyRate
        
        view?.showFeeOverrideDialog(session, originalAmount)
    }

    override fun onApplyFeeOverride(session: ParkingSession, newAmount: Double, reason: String) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // First override the fee
                val overrideResult = withContext(Dispatchers.IO) {
                    repository.overrideFee(session.sessionId, newAmount, reason)
                }
                
                if (overrideResult.isSuccess) {
                    // Then confirm the payment
                    val confirmResult = withContext(Dispatchers.IO) {
                        repository.confirmPayment(
                            sessionId = session.sessionId,
                            spotId = session.spotId,
                            totalAmount = newAmount,
                            paymentMethod = "CASH"
                        )
                    }
                    
                    confirmResult.onSuccess {
                        view?.showLoading(false)
                        view?.showPaymentSuccess("Override applied: ₱${String.format("%.2f", newAmount)}")
                        loadPendingExits() // Refresh list
                    }.onFailure { error ->
                        view?.showLoading(false)
                        view?.showError("Failed to apply override: ${error.message}")
                    }
                } else {
                    view?.showLoading(false)
                    view?.showError("Failed to override fee: ${overrideResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }

    override fun onRefresh() {
        loadPendingExits()
    }
}
