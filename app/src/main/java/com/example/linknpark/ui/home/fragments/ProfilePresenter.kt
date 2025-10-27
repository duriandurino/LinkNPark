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
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository.getInstance()
) : ProfileContract.Presenter {

    private var view: ProfileContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: String = ""
    private var currentUserName: String = ""
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
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.getUserById(userId)
                }

                result.onSuccess { user ->
                    currentUserName = user.name
                    view?.showUserInfo(user)
                    Log.d(TAG, "User info loaded: ${user.name}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading user info from Firebase", error)
                    // Fallback to cached user ONLY if available, with warning
                    val cachedUser = authRepository.getCurrentUserSync()
                    if (cachedUser != null) {
                        Log.w(TAG, "Using cached user data - may be stale!")
                        currentUserName = cachedUser.name
                        view?.showUserInfo(cachedUser)
                    } else {
                        view?.showError("Failed to load user information")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception loading user info", e)
                view?.showError("An error occurred while loading profile")
            }
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

    override fun onEditProfileClicked() {
        view?.showEditProfileDialog(currentUserName)
    }

    override fun onSaveProfile(name: String, password: String) {
        if (name.isBlank() || password.isBlank()) {
            view?.showEditProfileError("Name and password cannot be empty")
            return
        }

        view?.showLoading(true)

        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.updateUserProfile(userId, name, password)
                }

                result.onSuccess { updatedUser ->
                    currentUserName = updatedUser.name
                    view?.showEditProfileSuccess("Profile updated successfully!")
                    view?.showUserInfo(updatedUser)
                }.onFailure { error ->
                    view?.showEditProfileError(error.message ?: "Failed to update profile")
                }

            } catch (e: Exception) {
                view?.showEditProfileError("An error occurred: ${e.message}")
            } finally {
                view?.showLoading(false)
            }
        }
    }
}

