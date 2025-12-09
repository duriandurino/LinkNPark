package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.Reservation
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationsAdapter(
    private val onCancelClick: ((Reservation) -> Unit)? = null
) : RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder>() {

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
        holder.bind(reservations[position], onCancelClick)
    }

    override fun getItemCount() = reservations.size

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancel)

        fun bind(reservation: Reservation, onCancelClick: ((Reservation) -> Unit)?) {
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
            
            // Set status color based on state
            val statusColor = when (reservation.status) {
                "ACTIVE" -> ContextCompat.getColor(itemView.context, R.color.status_available)
                "EXPIRED" -> ContextCompat.getColor(itemView.context, R.color.status_occupied)
                "COMPLETED" -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                "CANCELLED" -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
                else -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }
            tvStatus.setTextColor(statusColor)
            
            // Only show cancel button for active reservations
            if (reservation.status == "ACTIVE" && onCancelClick != null) {
                btnCancel.visibility = View.VISIBLE
                btnCancel.setOnClickListener { onCancelClick(reservation) }
            } else {
                btnCancel.visibility = View.GONE
            }
        }
    }
}






