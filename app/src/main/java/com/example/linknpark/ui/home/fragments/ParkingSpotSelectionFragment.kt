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

class ParkingSpotSelectionFragment : Fragment(), ParkingSpotSelectionContract.View {

    private lateinit var presenter: ParkingSpotSelectionContract.Presenter
    private lateinit var tvParkingLotName: TextView
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

    private var lotId: String? = null
    private var lotName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            lotId = it.getString(ARG_LOT_ID)
            lotName = it.getString(ARG_LOT_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_parking_spot_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvParkingLotName = view.findViewById(R.id.tvParkingLotName)
        tvAvailableCount = view.findViewById(R.id.tvAvailableCount)
        tvOccupiedCount = view.findViewById(R.id.tvOccupiedCount)
        tvReservedCount = view.findViewById(R.id.tvReservedCount)
        rvParkingSpots = view.findViewById(R.id.rvParkingSpots)
        progressBar = view.findViewById(R.id.progressBar)
        chipAll = view.findViewById(R.id.chipAll)
        chipAvailable = view.findViewById(R.id.chipAvailable)
        chipOccupied = view.findViewById(R.id.chipOccupied)
        chipReserved = view.findViewById(R.id.chipReserved)

        // Set parking lot name
        tvParkingLotName.text = lotName ?: "Unknown Parking Lot"

        // Setup RecyclerView with grid layout
        spotsAdapter = ParkingSpotsAdapter { spot -> presenter.onSpotClicked(spot) }
        rvParkingSpots.layoutManager = GridLayoutManager(requireContext(), 3)
        rvParkingSpots.adapter = spotsAdapter

        // Get user info using singleton
        val authRepository = FirebaseAuthRepository.getInstance()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"

        // Initialize presenter with lotId
        val selectedLotId = lotId ?: "main_lot"
        presenter = ParkingSpotSelectionPresenter()
        presenter.attach(this, userId, selectedLotId)

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
        val btnSelectStartTime = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectStartTime)
        val spinnerDuration = dialog.findViewById<Spinner>(R.id.spinnerDuration)
        val tvTotalAmount = dialog.findViewById<TextView>(R.id.tvTotalAmount)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        tvSpotCode.text = "Reserve Spot ${spot.spotCode}"

        // Track selected start time (null = now)
        var selectedStartTime: java.util.Calendar? = null
        val dateFormat = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())

        // Setup vehicle spinner
        val vehicleNames = userVehicles.map { "${it.licensePlate} - ${it.make} ${it.model}" }
        spinnerVehicle.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Setup start time picker
        btnSelectStartTime.setOnClickListener {
            val now = java.util.Calendar.getInstance()
            
            // Show Date Picker first
            android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    // Then show Time Picker
                    android.app.TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            val calendar = java.util.Calendar.getInstance()
                            calendar.set(year, month, day, hour, minute)
                            
                            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                                // Selected time is in the past, use "Now"
                                selectedStartTime = null
                                btnSelectStartTime.text = "Now (Immediate)"
                            } else {
                                selectedStartTime = calendar
                                btnSelectStartTime.text = dateFormat.format(calendar.time)
                            }
                        },
                        now.get(java.util.Calendar.HOUR_OF_DAY),
                        now.get(java.util.Calendar.MINUTE),
                        false
                    ).show()
                },
                now.get(java.util.Calendar.YEAR),
                now.get(java.util.Calendar.MONTH),
                now.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Setup duration spinner
        val durations = listOf("1 hour", "2 hours", "3 hours", "4 hours", "6 hours", "8 hours")
        val durationHours = listOf(1, 2, 3, 4, 6, 8)
        spinnerDuration.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Update total amount when duration changes
        val hourlyRate = 50.0 // TODO: Get from parking lot
        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val hours = durationHours[position]
                val amount = hours * hourlyRate
                tvTotalAmount.text = "Total: PHP ${String.format("%.2f", amount)}"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnConfirm.setOnClickListener {
            val vehicleIndex = spinnerVehicle.selectedItemPosition
            val durationIndex = spinnerDuration.selectedItemPosition
            val vehicleId = userVehicles[vehicleIndex].vehicleId
            val hours = durationHours[durationIndex]

            presenter.onReserveClicked(spot, vehicleId, hours)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        private const val ARG_LOT_ID = "lot_id"
        private const val ARG_LOT_NAME = "lot_name"

        fun newInstance(lotId: String, lotName: String) = ParkingSpotSelectionFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_LOT_ID, lotId)
                putString(ARG_LOT_NAME, lotName)
            }
        }
    }
}
