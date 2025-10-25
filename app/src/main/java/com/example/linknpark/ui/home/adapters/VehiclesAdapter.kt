package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.Vehicle

class VehiclesAdapter(
    private val onDeleteClick: (Vehicle) -> Unit
) : RecyclerView.Adapter<VehiclesAdapter.VehicleViewHolder>() {

    private var vehicles = listOf<Vehicle>()

    fun submitList(newVehicles: List<Vehicle>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(vehicles[position], onDeleteClick)
    }

    override fun getItemCount() = vehicles.size

    class VehicleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
        private val tvVehicleInfo: TextView = itemView.findViewById(R.id.tvVehicleInfo)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(vehicle: Vehicle, onDeleteClick: (Vehicle) -> Unit) {
            tvLicensePlate.text = vehicle.licensePlate
            tvVehicleInfo.text = "${vehicle.make} ${vehicle.model} - ${vehicle.color}"

            btnDelete.setOnClickListener {
                onDeleteClick(vehicle)
            }
        }
    }
}
