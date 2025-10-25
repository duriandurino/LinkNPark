package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Reservation

interface MyBookingsContract {
    
    interface View {
        fun showActiveReservations(reservations: List<Reservation>)
        fun showNoActiveReservations()
        fun showParkingHistory(sessions: List<ParkingSession>)
        fun showNoHistory()
        fun showCancelSuccess(message: String)
        fun showCancelError(message: String)
        fun showLoading(show: Boolean)
        fun showError(message: String)
    }
    
    interface Presenter {
        fun attach(view: View, userId: String)
        fun detach()
        fun loadReservations()
        fun loadHistory()
        fun onCancelReservation(reservationId: String)
    }
}



