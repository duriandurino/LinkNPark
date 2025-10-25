package com.example.linknpark.ui.home.fragments

import android.util.Log
import com.example.linknpark.data.DriverRepository
import com.example.linknpark.data.FirebaseDriverRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyBookingsPresenter(
    private val repository: DriverRepository = FirebaseDriverRepository()
) : MyBookingsContract.Presenter {

    private var view: MyBookingsContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: String = ""
    private val TAG = "MyBookingsPresenter"

    override fun attach(view: MyBookingsContract.View, userId: String) {
        this.view = view
        this.userId = userId
        
        loadReservations()
        loadHistory()
        setupRealTimeListeners()
    }

    override fun detach() {
        repository.removeReservationListener()
        presenterScope.cancel()
        view = null
    }

    override fun loadReservations() {
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getUserReservations(userId)
                }

                result.onSuccess { reservations ->
                    val activeReservations = reservations.filter { it.status == "ACTIVE" }
                    if (activeReservations.isNotEmpty()) {
                        view?.showActiveReservations(activeReservations)
                    } else {
                        view?.showNoActiveReservations()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading reservations", error)
                    view?.showNoActiveReservations()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading reservations", e)
                view?.showNoActiveReservations()
            }
        }
    }

    override fun loadHistory() {
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getUserSessions(userId)
                }

                result.onSuccess { sessions ->
                    if (sessions.isNotEmpty()) {
                        view?.showParkingHistory(sessions)
                    } else {
                        view?.showNoHistory()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading history", error)
                    view?.showNoHistory()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading history", e)
                view?.showNoHistory()
            }
        }
    }

    override fun onCancelReservation(reservationId: String) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.cancelReservation(reservationId)
                }

                result.onSuccess {
                    view?.showCancelSuccess("Reservation cancelled successfully")
                    loadReservations()
                }.onFailure { error ->
                    view?.showCancelError(error.message ?: "Failed to cancel reservation")
                }

            } catch (e: Exception) {
                view?.showCancelError("An error occurred: ${e.message}")
            } finally {
                view?.showLoading(false)
            }
        }
    }

    private fun setupRealTimeListeners() {
        repository.observeUserReservations(userId) { reservations ->
            if (reservations.isNotEmpty()) {
                view?.showActiveReservations(reservations)
            } else {
                view?.showNoActiveReservations()
            }
        }
    }
}





