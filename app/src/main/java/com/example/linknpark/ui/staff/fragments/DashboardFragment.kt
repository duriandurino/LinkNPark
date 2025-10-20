package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSummary
import com.example.linknpark.ui.staff.presenter.DashboardContract
import com.example.linknpark.ui.staff.presenter.DashboardPresenter

class DashboardFragment : Fragment(), DashboardContract.View {

    private lateinit var presenter: DashboardPresenter

    // Views
    private lateinit var tvTotalSpots: TextView
    private lateinit var tvAvailableSpots: TextView
    private lateinit var tvOccupiedSpots: TextView
    private lateinit var tvReservedSpots: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var tvActiveSessions: TextView
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize views
        initializeViews(view)

        // Initialize presenter
        presenter = DashboardPresenter(this)

        return view
    }

    private fun initializeViews(view: View) {
        tvTotalSpots = view.findViewById(R.id.tvTotalSpots)
        tvAvailableSpots = view.findViewById(R.id.tvAvailableSpots)
        tvOccupiedSpots = view.findViewById(R.id.tvOccupiedSpots)
        tvReservedSpots = view.findViewById(R.id.tvReservedSpots)
        tvRevenue = view.findViewById(R.id.tvRevenue)
        tvActiveSessions = view.findViewById(R.id.tvActiveSessions)

        // Optional: Add refresh functionality
//        view.findViewById<View>(R.id.btnRefreshDashboard)?.setOnClickListener {
//            presenter.refreshData()
//        }
    }

    override fun showStatistics(summary: ParkingSummary) {
        Log.d("DashboardFragment", "Updating statistics: $summary")

        // Update total spots
        tvTotalSpots.text = summary.totalSpots.toString()

        // Update available spots
        tvAvailableSpots.text = summary.availableSpots.toString()

        // Update occupied spots
        tvOccupiedSpots.text = summary.occupiedSpots.toString()

        // Update reserved spots (calculate from total - occupied - available, or set to 0 for now)
        val reservedSpots = 0 // Can be updated when reservation feature is implemented
        tvReservedSpots.text = reservedSpots.toString()

        // Calculate mock revenue based on occupied spots (e.g., $5 per spot)
        val mockRevenue = summary.occupiedSpots * 5.0
        tvRevenue.text = String.format("$%.2f", mockRevenue)

        // Update active sessions (use occupiedSpots as active sessions)
        tvActiveSessions.text = summary.occupiedSpots.toString()

        Log.d("DashboardFragment", "Total: ${summary.totalSpots}, Available: ${summary.availableSpots}, Occupied: ${summary.occupiedSpots}")
        Log.d("DashboardFragment", "Entries: ${summary.totalEntries}, Exits: ${summary.totalExits}")
        Log.d("DashboardFragment", "Active Cars: ${summary.activeCarsCount}")
    }

    override fun showLoading(isLoading: Boolean) {
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Optional: dim the content while loading
        view?.alpha = if (isLoading) 0.5f else 1.0f
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e("DashboardFragment", "Error: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }
}
