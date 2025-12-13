package com.example.linknpark.ui.staff.fragments

import com.example.linknpark.ui.staff.adapter.ActivityLog

interface LogsContract {
    
    interface View {
        fun showLogs(logs: List<ActivityLog>)
        fun showEmptyState()
        fun showError(message: String)
        fun showLoading(isLoading: Boolean)
    }
    
    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun loadLogs()
        fun onFilterChanged(filter: String)
        fun onSearchQueryChanged(query: String)
        fun getFilteredLogs(): List<ActivityLog>
    }
}
