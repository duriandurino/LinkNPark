package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.ActivityLog
import com.example.linknpark.ui.staff.adapter.ActivityLogAdapter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvDateTime: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvOccupiedCount: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var tvTotalVehicles: TextView
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var btnRefresh: MaterialButton
    
    private lateinit var activityAdapter: ActivityLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_staff_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvDateTime = view.findViewById(R.id.tvDateTime)
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvOccupiedCount = view.findViewById(R.id.tvOccupiedCount)
        tvRevenue = view.findViewById(R.id.tvRevenue)
        tvTotalVehicles = view.findViewById(R.id.tvTotalVehicles)
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity)
        btnRefresh = view.findViewById(R.id.btnRefresh)

        // Setup RecyclerView
        activityAdapter = ActivityLogAdapter()
        rvRecentActivity.layoutManager = LinearLayoutManager(requireContext())
        rvRecentActivity.adapter = activityAdapter

        // Set current date/time
        updateDateTime()

        // Load mock data
        loadMockData()

        // Refresh button
        btnRefresh.setOnClickListener {
            loadMockData()
        }
    }

    private fun updateDateTime() {
        val dateFormat = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault())
        tvDateTime.text = dateFormat.format(Date())
    }

    private fun loadMockData() {
        // Mock statistics (can be replaced with Firebase calls)
        tvAvailableCount.text = "45"
        tvOccupiedCount.text = "55"
        tvRevenue.text = "PHP 4,350.00"
        tvTotalVehicles.text = "87 vehicles today"

        // Mock recent activity
        val mockActivities = listOf(
            ActivityLog("Vehicle Entry", "A1", "ABC-123", "10:30 AM"),
            ActivityLog("Vehicle Exit", "B5", "XYZ-789", "10:25 AM"),
            ActivityLog("Vehicle Entry", "C3", "DEF-456", "10:20 AM"),
            ActivityLog("Vehicle Exit", "A2", "GHI-111", "10:15 AM"),
            ActivityLog("Vehicle Entry", "D7", "JKL-222", "10:10 AM")
        )

        activityAdapter.submitList(mockActivities)
    }
}
