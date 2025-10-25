package com.example.linknpark.ui.staff.fragments

import android.util.Log
import com.example.linknpark.data.FirebaseStaffRepository
import com.example.linknpark.data.StaffRepository
import com.example.linknpark.ui.staff.adapter.ActivityLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardPresenter(
    private val repository: StaffRepository = FirebaseStaffRepository()
) : DashboardContract.Presenter {

    private var view: DashboardContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val TAG = "DashboardPresenter"

    override fun attach(view: DashboardContract.View) {
        this.view = view
        loadDashboardData()
    }

    override fun detach() {
        presenterScope.cancel()
        view = null
    }

    override fun loadDashboardData() {
        view?.showLoading(true)
        
        presenterScope.launch {
            try {
                // Load parking stats
                val statsResult = withContext(Dispatchers.IO) {
                    repository.getParkingStats("main_lot")
                }
                
                statsResult.onSuccess { stats ->
                    view?.showAvailableCount(stats.availableSpots)
                    view?.showOccupiedCount(stats.occupiedSpots)
                }
                
                // Load today's revenue
                val revenueResult = withContext(Dispatchers.IO) {
                    repository.getTodayRevenue()
                }
                
                revenueResult.onSuccess { revenue ->
                    view?.showRevenue(String.format("PHP %.2f", revenue))
                }
                
                // Load today's vehicle count
                val vehicleCountResult = withContext(Dispatchers.IO) {
                    repository.getTodayVehicleCount()
                }
                
                vehicleCountResult.onSuccess { count ->
                    view?.showVehicleCount("$count vehicles today")
                }
                
                // Load recent activity
                val activityResult = withContext(Dispatchers.IO) {
                    repository.getRecentActivity(5)
                }
                
                activityResult.onSuccess { sessions ->
                    val activities = sessions.map { session ->
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val timeStr = session.enteredAt?.toDate()?.let { timeFormat.format(it) } ?: "N/A"
                        
                        val activityType = if (session.status == "ACTIVE") "Vehicle Entry" else "Vehicle Exit"
                        
                        ActivityLog(
                            type = activityType,
                            spotCode = session.spotCode,
                            licensePlate = session.licensePlate,
                            time = timeStr
                        )
                    }
                    view?.showRecentActivity(activities)
                }
                
                Log.d(TAG, "Dashboard data loaded successfully")
                
            } catch (e: Exception) {
                view?.showError("Error loading dashboard: ${e.message}")
                Log.e(TAG, "Error loading dashboard data", e)
            } finally {
                view?.showLoading(false)
            }
        }
    }

    override fun refresh() {
        loadDashboardData()
    }
}

