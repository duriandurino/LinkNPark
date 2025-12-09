package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionsAdapter(
    private val onEndSessionClick: ((ParkingSession) -> Unit)? = null
) : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

    private var sessions = listOf<ParkingSession>()

    fun submitList(newSessions: List<ParkingSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position], onEndSessionClick)
    }

    override fun getItemCount() = sessions.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvRunningTime: TextView = itemView.findViewById(R.id.tvRunningTime)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
        private val tvEntryTime: TextView = itemView.findViewById(R.id.tvEntryTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnEndSession: MaterialButton = itemView.findViewById(R.id.btnEndSession)

        fun bind(session: ParkingSession, onEndSessionClick: ((ParkingSession) -> Unit)?) {
            tvSpotCode.text = "Spot ${session.spotCode}"
            tvLicensePlate.text = session.licensePlate.ifEmpty { "Unknown vehicle" }
            
            // Status badge
            tvStatus.text = when (session.status) {
                "ACTIVE" -> "🟢 ACTIVE"
                "COMPLETED" -> "✓ COMPLETED"
                else -> session.status
            }
            
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            val entryTime = session.enteredAt?.toDate()
            
            if (entryTime != null) {
                tvEntryTime.text = "Entered: ${dateFormat.format(entryTime)}"
                
                // Calculate running time
                val now = Date()
                val durationMs = now.time - entryTime.time
                val durationMinutes = (durationMs / 60000).toInt()
                val hours = durationMinutes / 60
                val minutes = durationMinutes % 60
                
                tvRunningTime.text = if (hours > 0) "⏱ ${hours}h ${minutes}m" else "⏱ ${minutes}m"
                tvDuration.text = "Duration: $durationMinutes min"
                
                // Calculate estimated fee
                val hourlyRate = session.hourlyRate
                val hoursParked = durationMs / (1000.0 * 60 * 60)
                val estimatedFee = hoursParked * hourlyRate
                tvAmount.text = "Est. Fee: PHP ${String.format("%.2f", estimatedFee)}"
            } else {
                tvEntryTime.text = "Entry time not available"
                tvRunningTime.text = "⏱ --"
                tvDuration.text = "Duration: Unknown"
                tvAmount.text = "Est. Fee: --"
            }
            
            // End session button
            if (session.status == "ACTIVE" && onEndSessionClick != null) {
                btnEndSession.visibility = View.VISIBLE
                btnEndSession.setOnClickListener { onEndSessionClick(session) }
            } else {
                btnEndSession.visibility = View.GONE
            }
        }
    }
}


