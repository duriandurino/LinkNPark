package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.Reservation
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationsAdapter : RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder>() {

    private var reservations = listOf<Reservation>()

    fun submitList(newReservations: List<Reservation>) {
        reservations = newReservations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(reservations[position])
    }

    override fun getItemCount() = reservations.size

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(reservation: Reservation) {
            tvSpotCode.text = "Spot ${reservation.spotCode}"
            
            val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            val startTime = reservation.reserveStart?.toDate()
            val endTime = reservation.reserveEnd?.toDate()
            
            if (startTime != null && endTime != null) {
                tvTimeRange.text = "${dateFormat.format(startTime)} - ${dateFormat.format(endTime)}"
            } else {
                tvTimeRange.text = "Time not available"
            }
            
            tvStatus.text = reservation.status
            tvAmount.text = "PHP ${String.format("%.2f", reservation.totalAmount)}"
        }
    }
}


