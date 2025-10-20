package com.example.linknpark.ui.staff.presenter

import com.example.linknpark.data.MockParkingRepository
import com.example.linknpark.data.ParkingRepository
import com.example.linknpark.model.ParkingSummary
import kotlinx.coroutines.*

interface DashboardContract {
    interface View {
        fun showStatistics(summary: ParkingSummary)
        fun showLoading(isLoading: Boolean)
        fun showError(message: String)
    }

    interface Presenter {
        fun loadDashboardData()
        fun refreshData()
        fun onDestroy()
    }
}

class DashboardPresenter(
    private val view: DashboardContract.View,
    private val repository: ParkingRepository = MockParkingRepository()
) : DashboardContract.Presenter {

    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        loadDashboardData()
    }

    override fun loadDashboardData() {
        presenterScope.launch {
            view.showLoading(true)

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getLiveParkingStatus()
                }

                result.onSuccess { response ->
                    val summary = ParkingSummary(
                        totalSpots = response.parking_spots.size,
                        occupiedSpots = response.parking_spots.count { it.occupied },
                        availableSpots = response.parking_spots.count { !it.occupied },
                        totalEntries = response.total_entries,
                        totalExits = response.total_exits,
                        activeCarsCount = response.all_active_cars.size
                    )

                    view.showLoading(false)
                    view.showStatistics(summary)
                }.onFailure { error ->
                    view.showLoading(false)
                    view.showError(error.message ?: "Failed to load dashboard data")
                }
            } catch (e: Exception) {
                view.showLoading(false)
                view.showError("Error: ${e.message}")
            }
        }
    }

    override fun refreshData() {
        presenterScope.launch {
            view.showLoading(true)

            try {
                val result = withContext(Dispatchers.IO) {
                    repository.refreshParkingStatus()
                }

                result.onSuccess { response ->
                    val summary = ParkingSummary(
                        totalSpots = response.parking_spots.size,
                        occupiedSpots = response.parking_spots.count { it.occupied },
                        availableSpots = response.parking_spots.count { !it.occupied },
                        totalEntries = response.total_entries,
                        totalExits = response.total_exits,
                        activeCarsCount = response.all_active_cars.size
                    )

                    view.showLoading(false)
                    view.showStatistics(summary)
                }.onFailure { error ->
                    view.showLoading(false)
                    view.showError(error.message ?: "Failed to refresh data")
                }
            } catch (e: Exception) {
                view.showLoading(false)
                view.showError("Refresh error: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        presenterScope.cancel()
    }
}