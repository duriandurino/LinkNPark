package com.example.linknpark.ui.home.fragments

import android.location.Location
import com.example.linknpark.model.ParkingLot

interface FindParkingMapContract {

    interface View {
        fun displayParkingLots(lots: List<ParkingLot>)
        fun showBottomSheet(lot: ParkingLot)
        fun hideBottomSheet()
        fun centerMapOnUserLocation(location: Location)
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun showNoResultsMessage(query: String)
    }

    interface Presenter {
        fun attach(view: View, userId: String)
        fun detach()
        fun loadParkingLots()
        fun searchParkingLots(query: String)
        fun requestUserLocation()
    }
}
