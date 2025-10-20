package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingApiResponse
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.ParkingSpace
import com.example.linknpark.ui.staff.adapter.ParkingSpotAdapter
import com.example.linknpark.ui.staff.presenter.ParkingContract
import com.example.linknpark.ui.staff.presenter.ParkingPresenter
import com.google.android.material.button.MaterialButton

class ParkingFragment : Fragment(), ParkingContract.View {

    lateinit var presenter: ParkingPresenter
    private lateinit var rvParkingSpots: RecyclerView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvOccupancyInfo: TextView
    private lateinit var adapter: ParkingSpotAdapter

    private var parkingSpaces = mutableListOf<ParkingSpace>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_parking, container, false)

        // Initialize views
        rvParkingSpots = view.findViewById(R.id.rvParkingSpots)
        btnRefresh = view.findViewById(R.id.btnRefreshParking)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        tvOccupancyInfo = view.findViewById(R.id.tvOccupancyInfo)

        // Set up RecyclerView with GridLayoutManager (6 columns)
        val gridLayoutManager = GridLayoutManager(requireContext(), 6)
        rvParkingSpots.layoutManager = gridLayoutManager

        // Initialize adapter
        adapter = ParkingSpotAdapter(parkingSpaces) { spot ->
            onSpotClicked(spot)
        }
        rvParkingSpots.adapter = adapter

        // Set up refresh button
        btnRefresh.setOnClickListener {
            presenter.refreshParkingStatus()
        }

        // Initialize presenter (this will auto-load data in its init block)
        presenter = ParkingPresenter(this)

        Log.d("ParkingFragment", "Fragment created with RecyclerView setup")

        return view
    }

    private fun onSpotClicked(spot: ParkingSpace) {
        // Toggle spot status (for testing)
        presenter.toggleSpace(spot.id)

        // Show spot details
        showSpotDetails(spot)
    }

    override fun showParkingSpaces(spaces: List<ParkingSpace>, rows: Int, cols: Int) {
        Log.d("ParkingFragment", "showParkingSpaces called with ${spaces.size} spots, $rows x $cols grid")

        if (spaces.isEmpty()) {
            Log.w("ParkingFragment", "No parking spaces to display!")
            return
        }

        activity?.runOnUiThread {
            parkingSpaces.clear()
            parkingSpaces.addAll(spaces)
            adapter.updateSpots(parkingSpaces)

            Log.d("ParkingFragment", "RecyclerView updated with ${parkingSpaces.size} parking spots")
        }
    }

    override fun updateSpace(space: ParkingSpace) {
        val index = parkingSpaces.indexOfFirst { it.id == space.id }
        if (index != -1) {
            parkingSpaces[index] = space
            adapter.updateSpot(space)
            Log.d("ParkingFragment", "Updated spot ${space.id}: ${if (space.isOccupied) "OCCUPIED" else "AVAILABLE"}")
        }
    }

    override fun showModifyDialog(currentRows: Int, currentCols: Int) {
        val dialog = ModifyParkingDialogFragment.newInstance(currentRows, currentCols)
        dialog.onSave = { rows, cols ->
            presenter.updateParkingLayout(rows, cols)
        }
        dialog.show(parentFragmentManager, "ModifyParkingDialog")
    }

    // New methods for API integration
    override fun showParkingData(response: ParkingApiResponse) {
        Log.d("ParkingFragment", "Received parking data: ${response.parking_spots.size} spots")
        Log.d("ParkingFragment", "Total Entries: ${response.total_entries}, Exits: ${response.total_exits}")
        Log.d("ParkingFragment", "Active Cars: ${response.all_active_cars.size}")
    }

    override fun showParkingSpots(spots: List<ParkingSpot>) {
        Log.d("ParkingFragment", "Displaying ${spots.size} parking spots")
        spots.forEachIndexed { index, spot ->
            Log.d("ParkingFragment", "Spot ${spot.spot_id}: ${if (spot.occupied) "OCCUPIED" else "AVAILABLE"}")
            if (spot.occupied && spot.current_car != null) {
                Log.d("ParkingFragment", "  Car: ${spot.current_car?.label} (ID: ${spot.current_car?.car_id})")
            }
        }
    }

    override fun showLoading(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRefresh.isEnabled = !isLoading
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        Log.e("ParkingFragment", "Error: $message")
    }

    override fun updateStatistics(totalSpots: Int, occupied: Int, available: Int, entries: Int, exits: Int) {
        // Update occupancy info
        tvOccupancyInfo.text = "$available available • $occupied occupied"

        Log.d("ParkingFragment", "Stats - Total: $totalSpots, Occupied: $occupied, Available: $available")
        Log.d("ParkingFragment", "Activity - Entries: $entries, Exits: $exits")
    }

    private fun showSpotDetails(space: ParkingSpace) {
        val row = space.id / 6
        val col = space.id % 6
        val spotLabel = "${'A' + row}${col + 1}"
        val status = if (space.isOccupied) "OCCUPIED" else "AVAILABLE"

        Toast.makeText(
            requireContext(),
            "Spot $spotLabel: $status",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }
}
