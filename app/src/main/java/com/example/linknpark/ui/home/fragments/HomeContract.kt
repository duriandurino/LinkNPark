package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession
import com.example.linknpark.model.Reservation

interface HomeContract {
    
    interface View {
        fun showWelcome(userName: String)
        fun showAvailableCount(count: Int)
        fun showActiveReservations(reservations: List<Reservation>)
        fun showActiveSessions(sessions: List<ParkingSession>)
        fun showNoReservations()
        fun showNoSessions()
        fun showError(message: String)
        fun showLoading(show: Boolean)
        fun setRefreshing(refreshing: Boolean)
    }
    
    interface Presenter {
        fun attach(view: View, userId: String, userName: String)
        fun detach()
        fun onRefresh()
        fun onFindParkingClicked()
        fun onViewBookingsClicked()
    }
}

