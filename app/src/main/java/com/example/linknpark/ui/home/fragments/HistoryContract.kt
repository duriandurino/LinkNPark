package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession

interface HistoryContract {
    interface View {
        fun showSessions(sessions: List<ParkingSession>)
        fun showEmptyState()
        fun showLoading(show: Boolean)
        fun showError(message: String)
        fun setRefreshing(refreshing: Boolean)
        fun showDateRangeLabel(label: String)
    }

    interface Presenter {
        fun attach(view: View, userId: String)
        fun detach()
        fun loadSessions()
        fun onFilterChanged(filter: String)
        fun onDateRangeSelected(startDate: Long?, endDate: Long?)
        fun onRefresh()
    }
}
