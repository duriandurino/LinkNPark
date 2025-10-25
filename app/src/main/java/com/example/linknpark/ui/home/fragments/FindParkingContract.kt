package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.Vehicle

interface FindParkingContract {
    
    interface View {
        fun showParkingSpots(spots: List<ParkingSpot>)
        fun showAvailableSpots(count: Int)
        fun showOccupiedSpots(count: Int)
        fun showReservedSpots(count: Int)
        fun showReservationSuccess(message: String)
        fun showReservationError(message: String)
        fun showUserVehicles(vehicles: List<Vehicle>)
        fun showNoVehicles()
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showReserveDialog(spot: ParkingSpot)
    }
    
    interface Presenter {
        fun attach(view: View, userId: String)
        fun detach()
        fun onSpotClicked(spot: ParkingSpot)
        fun onReserveClicked(spot: ParkingSpot, vehicleId: String, durationHours: Int)
        fun onFilterChanged(filter: String)
        fun loadUserVehicles()
    }
}





