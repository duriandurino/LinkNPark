package com.example.linknpark.ui.staff.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R
import com.example.linknpark.model.ParkingSpace

class ParkingSpotAdapter(
    private var spots: List<ParkingSpace>,
    private val onSpotClick: (ParkingSpace) -> Unit
) : RecyclerView.Adapter<ParkingSpotAdapter.ParkingSpotViewHolder>() {

    inner class ParkingSpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardParkingSpot)
        val layoutContent: LinearLayout = itemView.findViewById(R.id.layoutSpotContent)
        val tvSpotLabel: TextView = itemView.findViewById(R.id.tvSpotLabel)
        val ivCarIcon: ImageView = itemView.findViewById(R.id.ivCarIcon)
        val viewEmptyIndicator: View = itemView.findViewById(R.id.viewEmptyIndicator)
        val tvSpotStatus: TextView = itemView.findViewById(R.id.tvSpotStatus)
        val tvLicensePlate: TextView = itemView.findViewById(R.id.tvLicensePlate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingSpotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_spot_card, parent, false)
        return ParkingSpotViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParkingSpotViewHolder, position: Int) {
        val spot = spots[position]
        val context = holder.itemView.context
        
        // Generate spot label (A1, A2, B1, B2, etc.)
        val row = position / 6  // 6 spots per row
        val col = position % 6
        val rowLetter = ('A' + row).toString()
        val spotLabel = "$rowLetter${col + 1}"
        
        holder.tvSpotLabel.text = spotLabel
        
        // Set background based on status
        val backgroundDrawable = when {
            spot.isOccupied -> R.drawable.parking_spot_occupied
            else -> R.drawable.parking_spot_available
        }
        holder.layoutContent.setBackgroundResource(backgroundDrawable)
        
        // Show/hide car icon and empty indicator
        if (spot.isOccupied) {
            holder.ivCarIcon.visibility = View.VISIBLE
            holder.viewEmptyIndicator.visibility = View.GONE
            holder.tvSpotStatus.text = context.getString(R.string.status_occupied)
            holder.tvSpotStatus.setTextColor(
                ContextCompat.getColor(context, R.color.status_occupied)
            )
            
            // Show license plate if available (mock for now)
            if (position % 3 == 0) {
                holder.tvLicensePlate.visibility = View.VISIBLE
                holder.tvLicensePlate.text = "ABC-${100 + position}"
            } else {
                holder.tvLicensePlate.visibility = View.VISIBLE
                holder.tvLicensePlate.text = "UNKNOWN"
            }
            
            // Tint car icon
            holder.ivCarIcon.setColorFilter(
                ContextCompat.getColor(context, R.color.status_occupied)
            )
        } else {
            holder.ivCarIcon.visibility = View.GONE
            holder.viewEmptyIndicator.visibility = View.VISIBLE
            holder.tvLicensePlate.visibility = View.GONE
            holder.tvSpotStatus.text = context.getString(R.string.status_available)
            holder.tvSpotStatus.setTextColor(
                ContextCompat.getColor(context, R.color.status_available)
            )
        }
        
        // Click listener
        holder.cardView.setOnClickListener {
            onSpotClick(spot)
        }
        
        // Add ripple effect
        holder.cardView.isClickable = true
        holder.cardView.isFocusable = true
    }

    override fun getItemCount(): Int = spots.size

    fun updateSpots(newSpots: List<ParkingSpace>) {
        spots = newSpots
        notifyDataSetChanged()
    }
    
    fun updateSpot(spot: ParkingSpace) {
        val position = spots.indexOfFirst { it.id == spot.id }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
}

