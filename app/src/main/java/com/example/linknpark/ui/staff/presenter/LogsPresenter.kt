package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.data.FirebaseStaffRepository
import com.example.linknpark.data.StaffRepository
import com.example.linknpark.ui.staff.adapter.ActivityLog
import com.example.linknpark.ui.staff.fragments.LogsContract
import kotlinx.coroutines.*

class LogsPresenter(
    private val repository: StaffRepository = FirebaseStaffRepository()
) : LogsContract.Presenter {

    private var view: LogsContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var allLogs = listOf<ActivityLog>()
    private var currentFilter = "ALL"
    private var searchQuery = ""

    override fun attach(view: LogsContract.View) {
        this.view = view
        loadLogs()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    override fun loadLogs() {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getActivityLogs(limit = 100)
                }
                
                result.onSuccess { logs ->
                    // Convert from data layer ActivityLog to adapter ActivityLog
                    allLogs = logs.map { log ->
                        val isEntry = log.type.contains("Entry", ignoreCase = true)
                        ActivityLog(
                            type = if (isEntry) "🚗 Vehicle Entry" else "✓ Vehicle Exit",
                            spotCode = log.spotCode,
                            licensePlate = log.licensePlate,
                            time = log.time,
                            isEntry = isEntry
                        )
                    }
                    
                    view?.showLoading(false)
                    applyFiltersAndShow()
                }.onFailure { error ->
                    view?.showLoading(false)
                    view?.showError("Error loading logs: ${error.message}")
                    view?.showEmptyState()
                }
            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showError("Error: ${e.message}")
                view?.showEmptyState()
            }
        }
    }

    override fun onFilterChanged(filter: String) {
        currentFilter = filter
        applyFiltersAndShow()
    }

    override fun onSearchQueryChanged(query: String) {
        searchQuery = query
        applyFiltersAndShow()
    }

    override fun getFilteredLogs(): List<ActivityLog> {
        var filteredLogs = allLogs
        
        // Apply type filter
        filteredLogs = when (currentFilter) {
            "ENTRY" -> filteredLogs.filter { it.isEntry }
            "EXIT" -> filteredLogs.filter { !it.isEntry }
            else -> filteredLogs
        }
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            filteredLogs = filteredLogs.filter { log ->
                log.spotCode.lowercase().contains(query) ||
                log.licensePlate.lowercase().contains(query)
            }
        }
        
        return filteredLogs
    }
    
    private fun applyFiltersAndShow() {
        val filteredLogs = getFilteredLogs()
        
        if (filteredLogs.isEmpty()) {
            view?.showEmptyState()
        } else {
            view?.showLogs(filteredLogs)
        }
    }
}
