package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSpot

class ParkingSpotsAdapter(
    private val onSpotClick: (ParkingSpot) -> Unit
) : RecyclerView.Adapter<ParkingSpotsAdapter.SpotViewHolder>() {

    private var spots = listOf<ParkingSpot>()

    fun submitList(newSpots: List<ParkingSpot>) {
        spots = newSpots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_spot_grid, parent, false)
        return SpotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpotViewHolder, position: Int) {
        holder.bind(spots[position], onSpotClick)
    }

    override fun getItemCount() = spots.size

    class SpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardSpot)
        private val tvSpotCode: TextView = itemView.findViewById(R.id.tvSpotCode)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(spot: ParkingSpot, onSpotClick: (ParkingSpot) -> Unit) {
            tvSpotCode.text = spot.spotCode
            tvStatus.text = spot.status

            // Set colors based on status
            val (bgColor, textColor) = when {
                spot.isAvailable -> Pair(
                    itemView.context.getColor(R.color.spot_available_bg),
                    itemView.context.getColor(R.color.status_available)
                )
                spot.isOccupied -> Pair(
                    itemView.context.getColor(R.color.spot_occupied_bg),
                    itemView.context.getColor(R.color.status_occupied)
                )
                spot.isReserved -> Pair(
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

            // Only clickable if available
            cardView.isClickable = spot.isAvailable
            if (spot.isAvailable) {
                cardView.setOnClickListener { onSpotClick(spot) }
            } else {
                cardView.setOnClickListener(null)
            }
        }
    }
}

