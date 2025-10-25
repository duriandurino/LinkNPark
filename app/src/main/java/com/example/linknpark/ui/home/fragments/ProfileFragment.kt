package com.example.linknpark.ui.home.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.User
import com.example.linknpark.model.Vehicle
import com.example.linknpark.ui.home.adapters.VehiclesAdapter
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment(), ProfileContract.View {

    private lateinit var presenter: ProfileContract.Presenter
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var rvVehicles: RecyclerView
    private lateinit var tvNoVehicles: TextView
    private lateinit var btnAddVehicle: MaterialButton
    private lateinit var progressBar: ProgressBar

    private lateinit var vehiclesAdapter: VehiclesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvUserRole = view.findViewById(R.id.tvUserRole)
        rvVehicles = view.findViewById(R.id.rvVehicles)
        tvNoVehicles = view.findViewById(R.id.tvNoVehicles)
        btnAddVehicle = view.findViewById(R.id.btnAddVehicle)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup RecyclerView
        vehiclesAdapter = VehiclesAdapter { vehicle ->
            showDeleteVehicleConfirmation(vehicle)
        }
        rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        rvVehicles.adapter = vehiclesAdapter

        // Get user info
        val authRepository = FirebaseAuthRepository()
        val currentUser = authRepository.getCurrentUserSync()
        val userId = currentUser?.uid ?: "unknown"

        // Initialize presenter
        presenter = ProfilePresenter()
        presenter.attach(this, userId)

        // Setup button
        btnAddVehicle.setOnClickListener {
            presenter.onAddVehicleClicked()
        }
    }

    override fun onDestroyView() {
        presenter.detach()
        super.onDestroyView()
    }

    override fun showUserInfo(user: User) {
        tvUserName.text = user.name
        tvUserEmail.text = user.email
        tvUserRole.text = user.role.name
    }

    override fun showVehicles(vehicles: List<Vehicle>) {
        rvVehicles.visibility = View.VISIBLE
        tvNoVehicles.visibility = View.GONE
        vehiclesAdapter.submitList(vehicles)
    }

    override fun showNoVehicles() {
        rvVehicles.visibility = View.GONE
        tvNoVehicles.visibility = View.VISIBLE
    }

    override fun showAddVehicleSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showAddVehicleError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showDeleteVehicleSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showDeleteVehicleError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showAddVehicleDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_vehicle)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etLicensePlate = dialog.findViewById<EditText>(R.id.etLicensePlate)
        val etMake = dialog.findViewById<EditText>(R.id.etMake)
        val etModel = dialog.findViewById<EditText>(R.id.etModel)
        val etColor = dialog.findViewById<EditText>(R.id.etColor)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val licensePlate = etLicensePlate.text.toString()
            val make = etMake.text.toString()
            val model = etModel.text.toString()
            val color = etColor.text.toString()

            presenter.onSaveVehicle(licensePlate, make, model, color)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteVehicleConfirmation(vehicle: Vehicle) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete ${vehicle.licensePlate}?")
            .setPositiveButton("Delete") { _, _ ->
                presenter.onDeleteVehicle(vehicle.vehicleId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}






