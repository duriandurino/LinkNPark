package com.example.linknpark.ui.home.fragments

import android.util.Log
import com.example.linknpark.data.DriverRepository
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.data.FirebaseDriverRepository
import com.example.linknpark.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePresenter(
    private val driverRepository: DriverRepository = FirebaseDriverRepository(),
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository()
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: String = ""
    private val TAG = "ProfilePresenter"

    override fun attach(view: ProfileContract.View, userId: String) {
        this.view = view
        this.userId = userId
        
        loadUserInfo()
        loadVehicles()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    override fun loadUserInfo() {
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            view?.showUserInfo(currentUser)
        }
    }

    override fun loadVehicles() {
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    driverRepository.getUserVehicles(userId)
                }

                result.onSuccess { vehicles ->
                    if (vehicles.isNotEmpty()) {
                        view?.showVehicles(vehicles)
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

    override fun onAddVehicleClicked() {
        view?.showAddVehicleDialog()
    }

    override fun onSaveVehicle(licensePlate: String, make: String, model: String, color: String) {
        if (licensePlate.isBlank() || make.isBlank() || model.isBlank()) {
            view?.showAddVehicleError("Please fill in all required fields")
            return
        }

        view?.showLoading(true)

        presenterScope.launch {
            try {
                val vehicle = Vehicle(
                    userId = userId,
                    licensePlate = licensePlate,
                    make = make,
                    model = model,
                    color = color,
                    vehicleType = "SEDAN",
                    year = 2024,
                    isPrimary = false
                )
                
                val result = withContext(Dispatchers.IO) {
                    driverRepository.addVehicle(vehicle)
                }

                result.onSuccess {
                    view?.showAddVehicleSuccess("Vehicle added successfully!")
                    loadVehicles()
                }.onFailure { error ->
                    view?.showAddVehicleError(error.message ?: "Failed to add vehicle")
                }

            } catch (e: Exception) {
                view?.showAddVehicleError("An error occurred: ${e.message}")
            } finally {
                view?.showLoading(false)
            }
        }
    }

    override fun onDeleteVehicle(vehicleId: String) {
        view?.showLoading(true)

        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    driverRepository.deleteVehicle(vehicleId)
                }

                result.onSuccess {
                    view?.showDeleteVehicleSuccess("Vehicle deleted successfully")
                    loadVehicles()
                }.onFailure { error ->
                    view?.showDeleteVehicleError(error.message ?: "Failed to delete vehicle")
                }

            } catch (e: Exception) {
                view?.showDeleteVehicleError("An error occurred: ${e.message}")
            } finally {
                view?.showLoading(false)
            }
        }
    }
}

