package com.example.linknpark.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingLot
import com.google.android.material.button.MaterialButton

class ParkingLotsAdapter(
    private val onLotClick: (ParkingLot) -> Unit,
    private val onGoToMapClick: (ParkingLot) -> Unit,
    private val onViewSpotsClick: (ParkingLot) -> Unit
) : RecyclerView.Adapter<ParkingLotsAdapter.ParkingLotViewHolder>() {

    private var lots = listOf<ParkingLot>()

    fun submitList(newLots: List<ParkingLot>) {
        lots = newLots
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingLotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_lot, parent, false)
        return ParkingLotViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingLotViewHolder, position: Int) {
        holder.bind(lots[position], onLotClick, onGoToMapClick, onViewSpotsClick)
    }

    override fun getItemCount() = lots.size

    class ParkingLotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardParkingLot)
        private val tvLotName: TextView = itemView.findViewById(R.id.tvLotName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tvAvailability)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnGoToMap: MaterialButton = itemView.findViewById(R.id.btnGoToMap)
        private val btnViewSpots: MaterialButton = itemView.findViewById(R.id.btnViewSpots)

        fun bind(
            lot: ParkingLot,
            onLotClick: (ParkingLot) -> Unit,
            onGoToMapClick: (ParkingLot) -> Unit,
            onViewSpotsClick: (ParkingLot) -> Unit
        ) {
            tvLotName.text = lot.name
            tvAddress.text = lot.address
            tvAvailability.text = "${lot.availableSpots} / ${lot.totalSpots} spots available"
            tvPrice.text = "PHP ${String.format("%.2f", lot.pricePerHour)}/hr"

            // Set availability color
            val availabilityRatio = if (lot.totalSpots > 0) {
                lot.availableSpots.toDouble() / lot.totalSpots.toDouble()
            } else 0.0

            val availabilityColor = when {
                availabilityRatio > 0.2 -> ContextCompat.getColor(itemView.context, R.color.status_available)
                availabilityRatio > 0.05 -> ContextCompat.getColor(itemView.context, R.color.accent_orange)
                else -> ContextCompat.getColor(itemView.context, R.color.status_occupied)
            }
            tvAvailability.setTextColor(availabilityColor)

            // Click handlers
            cardView.setOnClickListener { onLotClick(lot) }
            btnGoToMap.setOnClickListener { onGoToMapClick(lot) }
            btnViewSpots.setOnClickListener { onViewSpotsClick(lot) }
        }
    }
}
