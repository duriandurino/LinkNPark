package com.example.linknpark.ui.staff.fragments

import android.util.Log
import com.example.linknpark.data.FirebaseStaffRepository
import com.example.linknpark.data.StaffRepository
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.ui.staff.adapter.StaffParkingSpot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParkingPresenter(
    private val repository: StaffRepository = FirebaseStaffRepository()
) : ParkingContract.Presenter {

    private var view: ParkingContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "ParkingPresenter"

    override fun attach(view: ParkingContract.View) {
        this.view = view
        loadParkingData()
    }

    override fun detach() {
        repository.removeSpotListener()
        presenterScope.cancel()
        view = null
    }

    override fun loadParkingData() {
        view?.showLoading(true)
        
        // Setup real-time listener for parking spots
        repository.observeAllParkingSpots("main_lot") { spots ->
            // Convert ParkingSpot to StaffParkingSpot
            val staffSpots = spots.map { spot ->
                StaffParkingSpot(
                    spotId = spot.spotId,
                    spotCode = spot.spotCode,
                    status = spot.status,
                    licensePlate = spot.currentCarLabel
                )
            }
            
            view?.showParkingSpots(staffSpots)
            
            // Calculate stats
            val total = spots.size
            val available = spots.count { it.isAvailable }
            val occupied = spots.count { it.isOccupied }
            
            view?.showStats(total, available, occupied)
            view?.showLoading(false)
            
            Log.d(TAG, "Loaded ${spots.size} parking spots from Firebase")
        }
    }

    override fun refresh() {
        // Real-time listener will automatically update
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getParkingStats("main_lot")
                }
                
                result.onSuccess { stats ->
                    view?.showStats(stats.totalSpots, stats.availableSpots, stats.occupiedSpots)
                    Log.d(TAG, "Refreshed parking stats")
                }.onFailure { error ->
                    view?.showError("Failed to refresh: ${error.message}")
                    Log.e(TAG, "Error refreshing stats", error)
                }
            } catch (e: Exception) {
                view?.showError("Error: ${e.message}")
                Log.e(TAG, "Exception during refresh", e)
            }
        }
    }

    override fun updateSpotStatus(spotId: String, newStatus: String) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.updateSpotStatus(spotId, newStatus)
                }
                
                result.onSuccess {
                    Log.d(TAG, "Updated spot $spotId to $newStatus")
                    view?.showLoading(false)
                    view?.showUpdateSuccess("Spot updated to $newStatus")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to update spot $spotId", error)
                    view?.showLoading(false)
                    view?.showError("Failed to update: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating spot", e)
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }
    
    override fun createSpot(code: String, type: String, hourlyRate: Double) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.createParkingSpot(code, type, hourlyRate, "main_lot")
                }
                
                result.onSuccess { spotId ->
                    Log.d(TAG, "Created spot: $spotId")
                    view?.showLoading(false)
                    view?.showSpotCreated("Spot $code created successfully")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to create spot", error)
                    view?.showLoading(false)
                    view?.showError("Failed to create spot: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating spot", e)
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }
    
    override fun updateSpot(spotId: String, code: String, hourlyRate: Double, status: String) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.updateParkingSpot(spotId, code, hourlyRate, status)
                }
                
                result.onSuccess {
                    Log.d(TAG, "Updated spot: $spotId")
                    view?.showLoading(false)
                    view?.showSpotUpdated("Spot $code updated successfully")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to update spot $spotId", error)
                    view?.showLoading(false)
                    view?.showError("Failed to update spot: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating spot", e)
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }
    
    override fun deleteSpot(spotId: String) {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.deleteParkingSpot(spotId)
                }
                
                result.onSuccess {
                    Log.d(TAG, "Deleted spot: $spotId")
                    view?.showLoading(false)
                    view?.showSpotDeleted("Spot deleted successfully")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to delete spot $spotId", error)
                    view?.showLoading(false)
                    view?.showError("Failed to delete spot: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting spot", e)
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }
}
