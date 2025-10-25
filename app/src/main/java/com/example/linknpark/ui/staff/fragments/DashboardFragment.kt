package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.ActivityLog
import com.example.linknpark.ui.staff.adapter.ActivityLogAdapter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment(), DashboardContract.View {

    private lateinit var tvDateTime: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvOccupiedCount: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var tvTotalVehicles: TextView
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private lateinit var activityAdapter: ActivityLogAdapter
    private lateinit var presenter: DashboardContract.Presenter

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
        progressBar = view.findViewById(R.id.progressBar)

        // Setup RecyclerView
        activityAdapter = ActivityLogAdapter()
        rvRecentActivity.layoutManager = LinearLayoutManager(requireContext())
        rvRecentActivity.adapter = activityAdapter

        // Set current date/time
        updateDateTime()

        // Initialize presenter
        presenter = DashboardPresenter()
        presenter.attach(this)

        // Refresh button
        btnRefresh.setOnClickListener {
            presenter.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
    }

    private fun updateDateTime() {
        val dateFormat = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault())
        tvDateTime.text = dateFormat.format(Date())
    }

    override fun showAvailableCount(count: Int) {
        tvAvailableCount.text = count.toString()
    }

    override fun showOccupiedCount(count: Int) {
        tvOccupiedCount.text = count.toString()
    }

    override fun showRevenue(amount: String) {
        tvRevenue.text = amount
    }

    override fun showVehicleCount(count: String) {
        tvTotalVehicles.text = count
    }

    override fun showRecentActivity(activities: List<ActivityLog>) {
        activityAdapter.submitList(activities)
    }

    override fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
