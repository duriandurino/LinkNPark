package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSession
import java.text.SimpleDateFormat
import java.util.Locale

class SessionsAdapter : RecyclerView.Adapter<SessionsAdapter.SessionViewHolder>() {

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
        holder.bind(sessions[position])
    }

    override fun getItemCount() = sessions.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
        private val tvEntryTime: TextView = itemView.findViewById(R.id.tvEntryTime)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(session: ParkingSession) {
            tvSpotCode.text = "Spot ${session.spotCode}"
            tvLicensePlate.text = session.licensePlate
            
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            val entryTime = session.enteredAt?.toDate()
            
            if (entryTime != null) {
                tvEntryTime.text = "Entered: ${dateFormat.format(entryTime)}"
            } else {
                tvEntryTime.text = "Entry time not available"
            }
            
            tvDuration.text = "${session.durationMinutes} minutes"
            tvAmount.text = "PHP ${String.format("%.2f", session.totalAmount)}"
        }
    }
}

