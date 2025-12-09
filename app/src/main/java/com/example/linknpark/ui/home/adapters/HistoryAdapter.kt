package com.example.linknpark.ui.home.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : ListAdapter<ParkingSession, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSpotCode: TextView = view.findViewById(R.id.tvSpotCode)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvLotName: TextView = view.findViewById(R.id.tvLotName)
        private val tvEntryTime: TextView = view.findViewById(R.id.tvEntryTime)
        private val tvExitTime: TextView = view.findViewById(R.id.tvExitTime)
        private val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        private val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
        private val tvPaymentStatus: TextView = view.findViewById(R.id.tvPaymentStatus)

        fun bind(session: ParkingSession) {
            tvSpotCode.text = session.spotCode.ifEmpty { "Unknown" }
            
            // Status badge
            tvStatus.text = session.status
            val statusColor = when (session.status) {
                "COMPLETED" -> Color.parseColor("#4CAF50")  // Green
                "CANCELLED" -> Color.parseColor("#F44336")  // Red
                "PAID" -> Color.parseColor("#2196F3")       // Blue
                else -> Color.parseColor("#9E9E9E")         // Gray
            }
            tvStatus.background.setTint(statusColor)
            
            // Lot name (placeholder - would need to fetch from lot)
            tvLotName.text = "Parking Lot"
            
            // Format timestamps
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            
            tvEntryTime.text = session.enteredAt?.toDate()?.let { dateFormat.format(it) } ?: "—"
            tvExitTime.text = session.exitedAt?.toDate()?.let { dateFormat.format(it) } ?: "—"
            
            // Calculate duration
            val entryMs = session.enteredAt?.toDate()?.time ?: 0
            val exitMs = session.exitedAt?.toDate()?.time ?: System.currentTimeMillis()
            val durationMinutes = ((exitMs - entryMs) / 1000 / 60).toInt()
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            tvDuration.text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            
            // Total amount
            tvTotalAmount.text = "₱${String.format("%.2f", session.totalAmount)}"
            
            // Payment status
            val paymentIcon = when (session.paymentStatus) {
                "PAID" -> "✓"
                else -> "○"
            }
            val paymentMethod = session.paymentId?.let { "GCash" } ?: "Cash"
            tvPaymentStatus.text = "$paymentIcon ${session.paymentStatus} via $paymentMethod"
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
