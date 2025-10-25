package com.example.linknpark.ui.home.fragments

import android.util.Log
import com.example.linknpark.data.DriverRepository
import com.example.linknpark.data.FirebaseDriverRepository
import com.example.linknpark.model.ParkingSpot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FindParkingPresenter(
    private val repository: DriverRepository = FirebaseDriverRepository()
) : FindParkingContract.Presenter {

    private var view: FindParkingContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: String = ""
    private var allSpots = listOf<ParkingSpot>()
    private var currentFilter = "ALL"
    private val TAG = "FindParkingPresenter"

    override fun attach(view: FindParkingContract.View, userId: String) {
        this.view = view
        this.userId = userId
        
        setupRealTimeListener()
        loadUserVehicles()
    }

    override fun detach() {
        repository.removeSpotListener()
        presenterScope.cancel()
        view = null
    }

    private fun setupRealTimeListener() {
        view?.showLoading(true)
        
        repository.observeParkingSpots("main_lot") { spots ->
            allSpots = spots
            updateStats(spots)
            applyFilter()
            view?.showLoading(false)
        }
    }

    private fun updateStats(spots: List<ParkingSpot>) {
        val availableCount = spots.count { it.isAvailable }
        val occupiedCount = spots.count { it.isOccupied }
        val reservedCount = spots.count { it.isReserved }
        
        view?.showAvailableSpots(availableCount)
        view?.showOccupiedSpots(occupiedCount)
        view?.showReservedSpots(reservedCount)
    }

    private fun applyFilter() {
        val filteredSpots = when (currentFilter) {
            "AVAILABLE" -> allSpots.filter { it.isAvailable }
            "OCCUPIED" -> allSpots.filter { it.isOccupied }
            "RESERVED" -> allSpots.filter { it.isReserved }
            else -> allSpots
        }
        
        view?.showParkingSpots(filteredSpots)
    }

    override fun onSpotClicked(spot: ParkingSpot) {
        if (spot.isAvailable && !spot.isReserved) {
            view?.showReserveDialog(spot)
        }
    }

    override fun onReserveClicked(spot: ParkingSpot, vehicleId: String, durationHours: Int) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Get vehicle details
                val vehiclesResult = withContext(Dispatchers.IO) {
                    repository.getUserVehicles(userId)
                }
                
                val licensePlate = vehiclesResult.getOrNull()
                    ?.find { it.vehicleId == vehicleId }
                    ?.licensePlate ?: "UNKNOWN"

                // Create reservation
                val result = withContext(Dispatchers.IO) {
                    repository.reserveSpot(
                        userId = userId,
                        spotCode = spot.spotCode,
                        spotNumber = spot.spotNumber,
                        licensePlate = licensePlate,
                        durationHours = durationHours
                    )
                }

                result.onSuccess {
                    view?.showReservationSuccess("Spot ${spot.spotCode} reserved successfully!")
                    Log.d(TAG, "Reservation successful for spot ${spot.spotCode}")
                }.onFailure { error ->
                    view?.showReservationError(error.message ?: "Failed to reserve spot")
                    Log.e(TAG, "Reservation failed", error)
                }

            } catch (e: Exception) {
                view?.showReservationError("An error occurred: ${e.message}")
                Log.e(TAG, "Error during reservation", e)
            } finally {
                view?.showLoading(false)
            }
        }
    }

    override fun onFilterChanged(filter: String) {
        currentFilter = filter
        applyFilter()
    }

    override fun loadUserVehicles() {
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getUserVehicles(userId)
                }

                result.onSuccess { vehicles ->
                    if (vehicles.isNotEmpty()) {
                        view?.showUserVehicles(vehicles)
                    } else {
                        view?.showNoVehicles()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading vehicles", error)
                    view?.showNoVehicles()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading vehicles", e)
                view?.showNoVehicles()
            }
        }
    }
}





