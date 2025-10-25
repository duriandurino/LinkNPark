package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter : RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder>() {

    private var sessions = listOf<ParkingSession>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

    fun submitList(newSessions: List<ParkingSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_history, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount() = sessions.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tvEndTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(session: ParkingSession) {
            tvSpotCode.text = "Spot ${session.spotCode}"
            tvLicensePlate.text = session.licensePlate

            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            tvStartTime.text = "Started: ${dateFormat.format(session.startTime.toDate())}"
            
            val endTime = session.endTime
            if (endTime != null) {
                tvEndTime.text = "Ended: ${dateFormat.format(endTime.toDate())}"
                tvEndTime.visibility = View.VISIBLE
            } else {
                tvEndTime.visibility = View.GONE
            }

            tvDuration.text = "Duration: ${formatDuration(session.durationMinutes)}"
            tvTotalAmount.text = "PHP ${String.format("%.2f", session.totalAmount)}"
            tvStatus.text = session.status

            // Set status color
            val statusColor = when (session.status) {
                "COMPLETED" -> itemView.context.getColor(R.color.status_available)
                "ACTIVE" -> itemView.context.getColor(R.color.primary_blue)
                "CANCELLED" -> itemView.context.getColor(R.color.status_inactive)
                else -> itemView.context.getColor(R.color.text_secondary)
            }
            tvStatus.setTextColor(statusColor)
        }

        private fun formatDuration(minutes: Int): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins}m"
            }
        }
    }
}
