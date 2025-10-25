package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.StaffParkingSpot
import com.example.linknpark.ui.staff.adapter.StaffParkingSpotAdapter
import com.google.android.material.button.MaterialButton

class ParkingFragment : Fragment(), ParkingContract.View {

    private lateinit var tvTotalSpots: TextView
    private lateinit var tvAvailableSpots: TextView
    private lateinit var tvOccupiedSpots: TextView
    private lateinit var rvParkingGrid: RecyclerView
    private lateinit var btnRefreshGrid: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var spotAdapter: StaffParkingSpotAdapter
    private lateinit var presenter: ParkingContract.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_staff_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvTotalSpots = view.findViewById(R.id.tvTotalSpots)
        tvAvailableSpots = view.findViewById(R.id.tvAvailableSpots)
        tvOccupiedSpots = view.findViewById(R.id.tvOccupiedSpots)
        rvParkingGrid = view.findViewById(R.id.rvParkingGrid)
        btnRefreshGrid = view.findViewById(R.id.btnRefreshGrid)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup RecyclerView with 5 columns
        spotAdapter = StaffParkingSpotAdapter { spot ->
            // Handle spot click
            Toast.makeText(
                requireContext(),
                "Spot ${spot.spotCode}: ${spot.status}",
                Toast.LENGTH_SHORT
            ).show()
        }
        rvParkingGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        rvParkingGrid.adapter = spotAdapter

        // Initialize presenter
        presenter = ParkingPresenter()
        presenter.attach(this)

        // Refresh button
        btnRefreshGrid.setOnClickListener {
            presenter.refresh()
            Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detach()
    }

    override fun showParkingSpots(spots: List<StaffParkingSpot>) {
        spotAdapter.submitList(spots)
    }

    override fun showStats(total: Int, available: Int, occupied: Int) {
        tvTotalSpots.text = total.toString()
        tvAvailableSpots.text = available.toString()
        tvOccupiedSpots.text = occupied.toString()
    }

    override fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
