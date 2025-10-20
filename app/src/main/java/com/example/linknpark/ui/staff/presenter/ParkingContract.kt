package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.model.ParkingApiResponse
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSpace

interface ParkingContract {
    interface View {
        fun showParkingSpaces(spaces: List<ParkingSpace>, rows: Int, cols: Int)
        fun updateSpace(space: ParkingSpace)
        fun showModifyDialog(currentRows: Int, currentCols: Int)

        // New methods for API data
        fun showParkingData(response: ParkingApiResponse)
        fun showParkingSpots(spots: List<ParkingSpot>)
        fun showLoading(isLoading: Boolean)
        fun showError(message: String)
        fun updateStatistics(totalSpots: Int, occupied: Int, available: Int, entries: Int, exits: Int)
    }

    interface Presenter {
        fun loadParkingSpaces()
        fun toggleSpace(id: Int)
        fun updateParkingLayout(rows: Int, cols: Int)
        fun onModifyParkingClicked()

        // New methods for API integration
        fun loadLiveParkingStatus()
        fun refreshParkingStatus()
        fun onDestroy()
    }
}
