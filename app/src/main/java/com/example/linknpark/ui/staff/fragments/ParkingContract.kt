package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.ui.staff.adapter.StaffParkingSpot

interface ParkingContract {
    
    interface View {
        fun showParkingSpots(spots: List<StaffParkingSpot>)
        fun showStats(total: Int, available: Int, occupied: Int)
        fun showLoading(isLoading: Boolean)
        fun showError(message: String)
        fun showUpdateSuccess(message: String)
        fun showSpotCreated(message: String)
        fun showSpotUpdated(message: String)
        fun showSpotDeleted(message: String)
    }
    
    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun loadParkingData()
        fun refresh()
        fun updateSpotStatus(spotId: String, newStatus: String)
        fun createSpot(code: String, type: String, hourlyRate: Double)
        fun updateSpot(spotId: String, code: String, hourlyRate: Double, status: String)
        fun deleteSpot(spotId: String)
    }
}
