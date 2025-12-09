package com.example.linknpark.ui.staff.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.ui.staff.adapter.StaffParkingSpot
import com.example.linknpark.ui.staff.adapter.StaffParkingSpotAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class ParkingFragment : Fragment(), ParkingContract.View {

    private lateinit var tvTotalSpots: TextView
    private lateinit var tvAvailableSpots: TextView
    private lateinit var tvOccupiedSpots: TextView
    private lateinit var rvParkingGrid: RecyclerView
    private lateinit var btnRefreshGrid: MaterialButton
    private lateinit var loadingOverlay: View
    private lateinit var fabAddSpot: FloatingActionButton

    private lateinit var spotAdapter: StaffParkingSpotAdapter
    private lateinit var presenter: ParkingContract.Presenter
    private val firestore = FirebaseFirestore.getInstance()

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
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        tvTotalSpots = view.findViewById(R.id.tvTotalSpots)
        tvAvailableSpots = view.findViewById(R.id.tvAvailableSpots)
        tvOccupiedSpots = view.findViewById(R.id.tvOccupiedSpots)
        rvParkingGrid = view.findViewById(R.id.rvParkingGrid)
        btnRefreshGrid = view.findViewById(R.id.btnRefreshGrid)
        fabAddSpot = view.findViewById(R.id.fabAddSpot)

        // Setup RecyclerView with 5 columns
        spotAdapter = StaffParkingSpotAdapter { spot ->
            // Handle spot click - show status change dialog
            showSpotStatusDialog(spot)
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
        
        // FAB for adding new spot
        fabAddSpot.setOnClickListener {
            showCreateSpotDialog()
        }
    }

    private fun showSpotStatusDialog(spot: StaffParkingSpot) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_change_spot_status)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvDialogTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvCurrentStatus = dialog.findViewById<TextView>(R.id.tvCurrentStatus)
        val btnSetAvailable = dialog.findViewById<MaterialButton>(R.id.btnSetAvailable)
        val btnSetOccupied = dialog.findViewById<MaterialButton>(R.id.btnSetOccupied)
        val btnSetReserved = dialog.findViewById<MaterialButton>(R.id.btnSetReserved)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)

        tvDialogTitle.text = "Spot ${spot.spotCode}"
        tvCurrentStatus.text = "Current: ${spot.status}"

        // Disable the button for current status
        when (spot.status) {
            "AVAILABLE" -> btnSetAvailable.isEnabled = false
            "OCCUPIED" -> btnSetOccupied.isEnabled = false
            "RESERVED" -> btnSetReserved.isEnabled = false
        }

        btnSetAvailable.setOnClickListener {
            presenter.updateSpotStatus(spot.spotId, "AVAILABLE")
            dialog.dismiss()
        }

        btnSetOccupied.setOnClickListener {
            presenter.updateSpotStatus(spot.spotId, "OCCUPIED")
            dialog.dismiss()
        }

        btnSetReserved.setOnClickListener {
            presenter.updateSpotStatus(spot.spotId, "RESERVED")
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Long press to edit spot details
        tvDialogTitle.setOnLongClickListener {
            dialog.dismiss()
            showEditSpotDialog(spot)
            true
        }

        dialog.show()
    }
    
    private fun showCreateSpotDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_spot, null)
        
        val etSpotCode = dialogView.findViewById<TextInputEditText>(R.id.etSpotCode)
        val etHourlyRate = dialogView.findViewById<TextInputEditText>(R.id.etHourlyRate)
        val chipStandard = dialogView.findViewById<Chip>(R.id.chipStandard)
        val chipMotorcycle = dialogView.findViewById<Chip>(R.id.chipMotorcycle)
        val chipLarge = dialogView.findViewById<Chip>(R.id.chipLarge)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnCreate).setOnClickListener {
            val spotCode = etSpotCode.text.toString().trim()
            val hourlyRate = etHourlyRate.text.toString().toDoubleOrNull() ?: 50.0
            
            if (spotCode.isEmpty()) {
                etSpotCode.error = "Spot code is required"
                return@setOnClickListener
            }
            
            val spotType = when {
                chipMotorcycle.isChecked -> "MOTORCYCLE"
                chipLarge.isChecked -> "LARGE"
                else -> "STANDARD"
            }
            
            // Create spot in Firestore
            val spotData = hashMapOf(
                "code" to spotCode,
                "type" to spotType,
                "status" to "AVAILABLE",
                "hourly_rate" to hourlyRate,
                "lot_id" to "default_lot"
            )
            
            showLoading(true)
            firestore.collection("parking_spots")
                .add(spotData)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(requireContext(), "✓ Spot $spotCode created", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    presenter.refresh()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        
        dialog.show()
    }
    
    private fun showEditSpotDialog(spot: StaffParkingSpot) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_spot, null)
        
        val etSpotCode = dialogView.findViewById<TextInputEditText>(R.id.etSpotCode)
        val etHourlyRate = dialogView.findViewById<TextInputEditText>(R.id.etHourlyRate)
        val chipAvailable = dialogView.findViewById<Chip>(R.id.chipAvailable)
        val chipOccupied = dialogView.findViewById<Chip>(R.id.chipOccupied)
        val chipInactive = dialogView.findViewById<Chip>(R.id.chipInactive)
        
        // Pre-fill current values
        etSpotCode.setText(spot.spotCode)
        etHourlyRate.setText("50") // Default
        
        when (spot.status) {
            "AVAILABLE" -> chipAvailable.isChecked = true
            "OCCUPIED" -> chipOccupied.isChecked = true
            else -> chipInactive.isChecked = true
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnDelete).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Spot")
                .setMessage("Are you sure you want to delete spot ${spot.spotCode}?")
                .setPositiveButton("Delete") { _, _ ->
                    firestore.collection("parking_spots")
                        .document(spot.spotId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "✓ Spot deleted", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            presenter.refresh()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        dialogView.findViewById<View>(R.id.btnSave).setOnClickListener {
            val newCode = etSpotCode.text.toString().trim()
            val newRate = etHourlyRate.text.toString().toDoubleOrNull() ?: 50.0
            val newStatus = when {
                chipOccupied.isChecked -> "OCCUPIED"
                chipInactive.isChecked -> "INACTIVE"
                else -> "AVAILABLE"
            }
            
            val updates = hashMapOf<String, Any>(
                "code" to newCode,
                "hourly_rate" to newRate,
                "status" to newStatus
            )
            
            showLoading(true)
            firestore.collection("parking_spots")
                .document(spot.spotId)
                .update(updates)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(requireContext(), "✓ Spot updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    presenter.refresh()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        
        dialog.show()
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
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showUpdateSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
