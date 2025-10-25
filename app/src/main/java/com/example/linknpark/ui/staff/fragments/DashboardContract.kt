package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.ui.staff.adapter.ActivityLog

interface DashboardContract {
    
    interface View {
        fun showAvailableCount(count: Int)
        fun showOccupiedCount(count: Int)
        fun showRevenue(amount: String)
        fun showVehicleCount(count: String)
        fun showRecentActivity(activities: List<ActivityLog>)
        fun showLoading(isLoading: Boolean)
        fun showError(message: String)
    }
    
    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun loadDashboardData()
        fun refresh()
    }
}

