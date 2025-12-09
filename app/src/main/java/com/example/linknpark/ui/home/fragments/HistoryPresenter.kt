package com.example.linknpark.ui.home.fragments

import com.example.linknpark.data.DriverRepository
import com.example.linknpark.model.ParkingSession
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryPresenter(
    private val repository: DriverRepository
) : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null
    private var userId: String = ""
    private var currentFilter: String = "ALL"
    private var startDate: Long? = null
    private var endDate: Long? = null
    
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun attach(view: HistoryContract.View, userId: String) {
        this.view = view
        this.userId = userId
        loadSessions()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    override fun loadSessions() {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Convert dates for repository
                val startDateObj = startDate?.let { Date(it) }
                val endDateObj = endDate?.let { Date(it) }
                
                // Use repository to get session history
                val result = repository.getSessionHistory(
                    userId = userId,
                    statusFilter = if (currentFilter == "ALL") null else currentFilter,
                    startDate = startDateObj,
                    endDate = endDateObj,
                    limit = 50
                )
                
                if (result.isSuccess) {
                    val sessions = result.getOrNull() ?: emptyList()
                    
                    // Apply additional filters for history (exclude ACTIVE sessions)
                    val filteredSessions = applyFilters(sessions)
                    
                    view?.showLoading(false)
                    view?.setRefreshing(false)
                    
                    if (filteredSessions.isEmpty()) {
                        view?.showEmptyState()
                    } else {
                        view?.showSessions(filteredSessions)
                    }
                } else {
                    view?.showLoading(false)
                    view?.setRefreshing(false)
                    view?.showError("Failed to load sessions: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.setRefreshing(false)
                view?.showError("Error: ${e.message}")
            }
        }
    }

    private fun applyFilters(sessions: List<ParkingSession>): List<ParkingSession> {
        var filtered = sessions
        
        // Apply status filter - History = not active
        filtered = when (currentFilter) {
            "COMPLETED" -> filtered.filter { it.status == "COMPLETED" }
            "CANCELLED" -> filtered.filter { it.status == "CANCELLED" }
            else -> filtered.filter { it.status != "ACTIVE" } // History = not active
        }
        
        return filtered
    }

    override fun onFilterChanged(filter: String) {
        currentFilter = filter
        loadSessions()
    }

    override fun onDateRangeSelected(startDate: Long?, endDate: Long?) {
        this.startDate = startDate
        this.endDate = endDate
        
        if (startDate != null && endDate != null) {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val startStr = dateFormat.format(Date(startDate))
            val endStr = dateFormat.format(Date(endDate))
            view?.showDateRangeLabel("$startStr - $endStr")
        } else {
            view?.showDateRangeLabel("")
        }
        
        loadSessions()
    }

    override fun onRefresh() {
        loadSessions()
    }
}
