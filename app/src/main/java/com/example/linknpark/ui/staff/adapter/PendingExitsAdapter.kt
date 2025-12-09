package com.example.linknpark.ui.staff.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class PendingExitsAdapter(
    private val onConfirmPayment: (ParkingSession) -> Unit,
    private val onOverrideFee: (ParkingSession) -> Unit
) : ListAdapter<ParkingSession, PendingExitsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_exit, parent, false)
        return ViewHolder(view, onConfirmPayment, onOverrideFee)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        view: View,
        private val onConfirmPayment: (ParkingSession) -> Unit,
        private val onOverrideFee: (ParkingSession) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvLicensePlate: TextView = view.findViewById(R.id.tvLicensePlate)
        private val tvSpotCode: TextView = view.findViewById(R.id.tvSpotCode)
        private val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        private val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        private val tvEntryTime: TextView = view.findViewById(R.id.tvEntryTime)
        private val btnConfirmPayment: MaterialButton = view.findViewById(R.id.btnConfirmPayment)
        private val btnOverrideFee: MaterialButton = view.findViewById(R.id.btnOverrideFee)

        fun bind(session: ParkingSession) {
            // License plate
            tvLicensePlate.text = session.licensePlate.ifEmpty { "No Plate" }
            
            // Spot code
            tvSpotCode.text = session.spotCode.ifEmpty { "—" }
            
            // Calculate duration
            val entryMs = session.enteredAt?.toDate()?.time ?: System.currentTimeMillis()
            val now = System.currentTimeMillis()
            val durationMinutes = ((now - entryMs) / 1000 / 60).toInt()
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            tvDuration.text = if (hours > 0) "⏱ ${hours}h ${minutes}m" else "⏱ ${minutes}m"
            
            // Calculate amount
            val hourlyRate = session.hourlyRate ?: 50.0
            val billableHours = Math.ceil(durationMinutes / 60.0).coerceAtLeast(1.0)
            val amount = billableHours * hourlyRate
            tvAmount.text = "₱${String.format("%.2f", amount)}"
            
            // Entry time
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            tvEntryTime.text = "Entry: ${session.enteredAt?.toDate()?.let { dateFormat.format(it) } ?: "Unknown"}"
            
            // Button actions
            btnConfirmPayment.setOnClickListener { onConfirmPayment(session) }
            btnOverrideFee.setOnClickListener { onOverrideFee(session) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ParkingSession>() {
        override fun areItemsTheSame(oldItem: ParkingSession, newItem: ParkingSession): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        override fun areContentsTheSame(oldItem: ParkingSession, newItem: ParkingSession): Boolean {
            return oldItem == newItem
        }
    }
}
