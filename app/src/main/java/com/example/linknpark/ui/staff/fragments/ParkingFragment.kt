package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.StaffParkingSpot
import com.example.linknpark.ui.staff.adapter.StaffParkingSpotAdapter
import com.google.android.material.button.MaterialButton

class ParkingFragment : Fragment() {

    private lateinit var tvTotalSpots: TextView
    private lateinit var tvAvailableSpots: TextView
    private lateinit var tvOccupiedSpots: TextView
    private lateinit var rvParkingGrid: RecyclerView
    private lateinit var btnRefreshGrid: MaterialButton

    private lateinit var spotAdapter: StaffParkingSpotAdapter

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

        // Load mock data
        loadMockData()

        // Refresh button
        btnRefreshGrid.setOnClickListener {
            loadMockData()
            Toast.makeText(requireContext(), "Refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMockData() {
        // Generate mock parking spots (can be replaced with Firebase calls)
        val mockSpots = mutableListOf<StaffParkingSpot>()
        
        val rows = listOf("A", "B", "C", "D", "E")
        val occupiedSpots = setOf("A1", "A3", "B2", "B4", "C1", "C5", "D3", "E2", "E4")
        val reservedSpots = setOf("A2", "C3", "D1")

        for (row in rows) {
            for (col in 1..6) {
                val spotCode = "$row$col"
                val status = when {
                    occupiedSpots.contains(spotCode) -> "OCCUPIED"
                    reservedSpots.contains(spotCode) -> "RESERVED"
                    else -> "AVAILABLE"
                }
                val licensePlate = if (status == "OCCUPIED") {
                    "ABC-${(100..999).random()}"
                } else null

                mockSpots.add(StaffParkingSpot(spotCode, status, licensePlate))
            }
        }

        spotAdapter.submitList(mockSpots)

        // Update stats
        val available = mockSpots.count { it.status == "AVAILABLE" }
        val occupied = mockSpots.count { it.status == "OCCUPIED" }
        val total = mockSpots.size

        tvTotalSpots.text = total.toString()
        tvAvailableSpots.text = available.toString()
        tvOccupiedSpots.text = occupied.toString()
    }
}
