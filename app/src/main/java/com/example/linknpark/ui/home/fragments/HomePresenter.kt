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

class HomePresenter(
    private val repository: DriverRepository = FirebaseDriverRepository()
) : HomeContract.Presenter {

    private var view: HomeContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: String = ""
    private val TAG = "HomePresenter"

    override fun attach(view: HomeContract.View, userId: String, userName: String) {
        this.view = view
        this.userId = userId
        view.showWelcome(userName)
        
        loadData()
        setupRealTimeListeners()
    }

    override fun detach() {
        repository.removeSpotListener()
        repository.removeReservationListener()
        repository.removeSessionListener()
        presenterScope.cancel()
        view = null
    }

    override fun onRefresh() {
        loadData()
    }

    private fun loadData() {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Load available spots count
                val spotsResult = withContext(Dispatchers.IO) {
                    repository.getAvailableParkingSpots()
                }
                
                spotsResult.onSuccess { spots ->
                    view?.showAvailableCount(spots.size)
                }.onFailure { error ->
                    Log.e(TAG, "Error loading spots", error)
                    view?.showError("Failed to load parking spots")
                }

                // Load user's reservations
                val reservationsResult = withContext(Dispatchers.IO) {
                    repository.getUserReservations(userId)
                }
                
                reservationsResult.onSuccess { reservations ->
                    val activeReservations = reservations.filter { it.status == "ACTIVE" }
                    if (activeReservations.isNotEmpty()) {
                        view?.showActiveReservations(activeReservations)
                    } else {
                        view?.showNoReservations()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading reservations", error)
                    view?.showNoReservations()
                }

                // Load active sessions
                val sessionsResult = withContext(Dispatchers.IO) {
                    repository.getActiveUserSessions(userId)
                }
                
                sessionsResult.onSuccess { sessions ->
                    if (sessions.isNotEmpty()) {
                        view?.showActiveSessions(sessions)
                    } else {
                        view?.showNoSessions()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading sessions", error)
                    view?.showNoSessions()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                view?.showError("Failed to load data")
            } finally {
                view?.showLoading(false)
                view?.setRefreshing(false)
            }
        }
    }

    private fun setupRealTimeListeners() {
        // Real-time updates for available spots
        repository.observeParkingSpots { spots ->
            val availableSpots = spots.filter { it.isAvailable }
            view?.showAvailableCount(availableSpots.size)
        }

        // Real-time updates for reservations
        repository.observeUserReservations(userId) { reservations ->
            if (reservations.isNotEmpty()) {
                view?.showActiveReservations(reservations)
            } else {
                view?.showNoReservations()
            }
        }

        // Real-time updates for active sessions
        repository.observeUserActiveSessions(userId) { sessions ->
            if (sessions.isNotEmpty()) {
                view?.showActiveSessions(sessions)
            } else {
                view?.showNoSessions()
            }
        }
    }

    override fun onFindParkingClicked() {
        // Navigation will be handled by the fragment/activity
    }

    override fun onViewBookingsClicked() {
        // Navigation will be handled by the fragment/activity
    }
}

