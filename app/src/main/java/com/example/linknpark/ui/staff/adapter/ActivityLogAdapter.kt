package com.example.linknpark.ui.staff.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.linknpark.R

data class ActivityLog(
    val type: String,
    val spotCode: String,
    val licensePlate: String,
    val time: String
)

class ActivityLogAdapter : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {

    private var activities = listOf<ActivityLog>()

    fun submitList(newActivities: List<ActivityLog>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount() = activities.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvActivityType: TextView = itemView.findViewById(R.id.tvActivityType)
        private val tvSpotInfo: TextView = itemView.findViewById(R.id.tvSpotInfo)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(activity: ActivityLog) {
            tvActivityType.text = activity.type
            tvSpotInfo.text = "${activity.spotCode} • ${activity.licensePlate}"
            tvTime.text = activity.time
        }
    }
}



