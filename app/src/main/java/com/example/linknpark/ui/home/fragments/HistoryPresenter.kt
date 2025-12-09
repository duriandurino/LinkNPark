package com.example.linknpark.ui.home.fragments

import com.example.linknpark.model.ParkingSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class HistoryPresenter : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null
    private var userId: String = ""
    private var currentFilter: String = "ALL"
    private var startDate: Long? = null
    private var endDate: Long? = null
    
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()

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
                var query = firestore.collection("parking_sessions")
                    .whereEqualTo("userId", userId)
                    .orderBy("entryTime", Query.Direction.DESCENDING)
                    .limit(50)
                
                query.get()
                    .addOnSuccessListener { documents ->
                        val sessions = documents.mapNotNull { doc ->
                            try {
                                ParkingSession(
                                    sessionId = doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    spotId = doc.getString("spotId"),
                                    spotCode = doc.getString("spotCode") ?: "",
                                    lotId = doc.getString("lotId") ?: "",
                                    licensePlate = doc.getString("licensePlate") ?: "",
                                    enteredAt = doc.getTimestamp("entryTime"),
                                    exitedAt = doc.getTimestamp("exitTime"),
                                    hourlyRate = doc.getDouble("hourlyRate") ?: 50.0,
                                    totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                    paymentStatus = doc.getString("paymentStatus") ?: "UNPAID",
                                    status = doc.getString("status") ?: "ACTIVE"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        // Apply filters
                        val filteredSessions = applyFilters(sessions)
                        
                        view?.showLoading(false)
                        view?.setRefreshing(false)
                        
                        if (filteredSessions.isEmpty()) {
                            view?.showEmptyState()
                        } else {
                            view?.showSessions(filteredSessions)
                        }
                    }
                    .addOnFailureListener { e ->
                        view?.showLoading(false)
                        view?.setRefreshing(false)
                        view?.showError("Failed to load sessions: ${e.message}")
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
        
        // Apply status filter
        filtered = when (currentFilter) {
            "COMPLETED" -> filtered.filter { it.status == "COMPLETED" }
            "CANCELLED" -> filtered.filter { it.status == "CANCELLED" }
            else -> filtered.filter { it.status != "ACTIVE" } // History = not active
        }
        
        // Apply date range filter
        if (startDate != null && endDate != null) {
            val start = Timestamp(startDate!! / 1000, 0)
            val end = Timestamp(endDate!! / 1000, 0)
            
            filtered = filtered.filter { session ->
                val entryTime = session.enteredAt
                entryTime != null && entryTime >= start && entryTime <= end
            }
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
