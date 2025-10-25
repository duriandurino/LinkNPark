package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.User
import com.example.linknpark.model.Vehicle

interface ProfileContract {
    
    interface View {
        fun showUserInfo(user: User)
        fun showVehicles(vehicles: List<Vehicle>)
        fun showNoVehicles()
        fun showAddVehicleSuccess(message: String)
        fun showAddVehicleError(message: String)
        fun showDeleteVehicleSuccess(message: String)
        fun showDeleteVehicleError(message: String)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showAddVehicleDialog()
    }
    
    interface Presenter {
        fun attach(view: View, userId: String)
        fun detach()
        fun loadUserInfo()
        fun loadVehicles()
        fun onAddVehicleClicked()
        fun onSaveVehicle(licensePlate: String, make: String, model: String, color: String)
        fun onDeleteVehicle(vehicleId: String)
    }
}




