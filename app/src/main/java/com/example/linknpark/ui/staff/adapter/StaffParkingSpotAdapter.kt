package com.example.linknpark.ui.staff.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R

data class StaffParkingSpot(
    val spotCode: String,
    val status: String,
    val licensePlate: String? = null
)

class StaffParkingSpotAdapter(
    private val onSpotClick: (StaffParkingSpot) -> Unit
) : RecyclerView.Adapter<StaffParkingSpotAdapter.ViewHolder>() {

    private var spots = listOf<StaffParkingSpot>()

    fun submitList(newSpots: List<StaffParkingSpot>) {
        spots = newSpots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staff_parking_spot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(spots[position], onSpotClick)
    }

    override fun getItemCount() = spots.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardSpot)
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)

        fun bind(spot: StaffParkingSpot, onSpotClick: (StaffParkingSpot) -> Unit) {
            tvSpotCode.text = spot.spotCode
            tvStatus.text = spot.status

            // Show license plate if occupied
            if (!spot.licensePlate.isNullOrEmpty()) {
                tvLicensePlate.text = spot.licensePlate
                tvLicensePlate.visibility = View.VISIBLE
            } else {
                tvLicensePlate.visibility = View.GONE
            }

            // Set colors based on status
            val (bgColor, textColor) = when (spot.status) {
                "AVAILABLE" -> Pair(
                    itemView.context.getColor(R.color.spot_available_bg),
                    itemView.context.getColor(R.color.status_available)
                )
                "OCCUPIED" -> Pair(
                    itemView.context.getColor(R.color.spot_occupied_bg),
                    itemView.context.getColor(R.color.status_occupied)
                )
                "RESERVED" -> Pair(
                    itemView.context.getColor(R.color.spot_reserved_bg),
                    itemView.context.getColor(R.color.status_reserved)
                )
                else -> Pair(
                    itemView.context.getColor(R.color.background_secondary),
                    itemView.context.getColor(R.color.text_secondary)
                )
            }

            cardView.setCardBackgroundColor(bgColor)
            tvSpotCode.setTextColor(textColor)
            tvStatus.setTextColor(textColor)
            tvLicensePlate.setTextColor(textColor)

            cardView.setOnClickListener { onSpotClick(spot) }
        }
    }
}



