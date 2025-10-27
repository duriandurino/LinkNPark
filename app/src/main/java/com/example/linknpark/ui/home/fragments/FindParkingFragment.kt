package com.example.linknpark.ui.home.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.ParkingSpot
import com.example.linknpark.model.Vehicle
import com.example.linknpark.ui.home.adapters.ParkingSpotsAdapter
import com.google.android.material.chip.Chip

class FindParkingFragment : Fragment(), FindParkingContract.View {

    private lateinit var presenter: FindParkingContract.Presenter
    private lateinit var tvAvailableCount: TextView
    private lateinit var tvOccupiedCount: TextView
    private lateinit var tvReservedCount: TextView
    private lateinit var rvParkingSpots: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipAll: Chip
    private lateinit var chipAvailable: Chip
    private lateinit var chipOccupied: Chip
    private lateinit var chipReserved: Chip

    private lateinit var spotsAdapter: ParkingSpotsAdapter
    private var userVehicles = listOf<Vehicle>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_find_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvOccupiedCount = view.findViewById(R.id.tvOccupiedCount)
        tvReservedCount = view.findViewById(R.id.tvReservedCount)
        rvParkingSpots = view.findViewById(R.id.rvParkingSpots)
        progressBar = view.findViewById(R.id.progressBar)
        chipAll = view.findViewById(R.id.chipAll)
        chipAvailable = view.findViewById(R.id.chipAvailable)
        chipOccupied = view.findViewById(R.id.chipOccupied)
        chipReserved = view.findViewById(R.id.chipReserved)

        // Setup RecyclerView with grid layout
        spotsAdapter = ParkingSpotsAdapter { spot -> presenter.onSpotClicked(spot) }
        rvParkingSpots.layoutManager = GridLayoutManager(requireContext(), 3)
        rvParkingSpots.adapter = spotsAdapter

        // Get user info using singleton
        val authRepository = FirebaseAuthRepository.getInstance()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"

        // Initialize presenter
        presenter = FindParkingPresenter()
        presenter.attach(this, userId)

        // Setup filter chips
        chipAll.setOnClickListener { presenter.onFilterChanged("ALL") }
        chipAvailable.setOnClickListener { presenter.onFilterChanged("AVAILABLE") }
        chipOccupied.setOnClickListener { presenter.onFilterChanged("OCCUPIED") }
        chipReserved.setOnClickListener { presenter.onFilterChanged("RESERVED") }
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showParkingSpots(spots: List<ParkingSpot>) {
        spotsAdapter.submitList(spots)
    }

    override fun showAvailableSpots(count: Int) {
        tvAvailableCount.text = count.toString()
    }

    override fun showOccupiedSpots(count: Int) {
        tvOccupiedCount.text = count.toString()
    }

    override fun showReservedSpots(count: Int) {
        tvReservedCount.text = count.toString()
    }

    override fun showReservationSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showReservationError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showUserVehicles(vehicles: List<Vehicle>) {
        userVehicles = vehicles
    }

    override fun showNoVehicles() {
        userVehicles = emptyList()
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showReserveDialog(spot: ParkingSpot) {
        if (userVehicles.isEmpty()) {
            Toast.makeText(requireContext(), "Please add a vehicle first in Profile", Toast.LENGTH_LONG).show()
            return
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_reserve_spot)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvSpotCode = dialog.findViewById<TextView>(R.id.tvSpotCode)
        val spinnerVehicle = dialog.findViewById<Spinner>(R.id.spinnerVehicle)
        val spinnerDuration = dialog.findViewById<Spinner>(R.id.spinnerDuration)
        val tvTotalAmount = dialog.findViewById<TextView>(R.id.tvTotalAmount)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        tvSpotCode.text = "Reserve Spot ${spot.spotCode}"

        // Setup vehicle spinner
        val vehicleNames = userVehicles.map { "${it.licensePlate} - ${it.make} ${it.model}" }
        spinnerVehicle.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Setup duration spinner
        val durations = listOf("1 hour", "2 hours", "3 hours", "4 hours", "6 hours", "8 hours")
        spinnerDuration.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Update total amount when duration changes
        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val hours = position + 1
                val amount = hours * 50.0
                tvTotalAmount.text = "Total: PHP ${String.format("%.2f", amount)}"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnConfirm.setOnClickListener {
            val vehicleIndex = spinnerVehicle.selectedItemPosition
            val durationIndex = spinnerDuration.selectedItemPosition
            val vehicleId = userVehicles[vehicleIndex].vehicleId
            val durationHours = durationIndex + 1

            presenter.onReserveClicked(spot, vehicleId, durationHours)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}







