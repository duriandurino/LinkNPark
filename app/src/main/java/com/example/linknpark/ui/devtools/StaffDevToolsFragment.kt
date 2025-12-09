package com.example.linknpark.ui.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Developer Tools for Staff role.
 * Provides bulk operations for managing parking data.
 * Access: Triple-tap on Settings nav item in StaffHomeActivity
 */
class StaffDevToolsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = FirebaseAuthRepository.getInstance()
    
    // UI Elements
    private lateinit var tvCurrentUser: TextView
    private lateinit var tvSpotStats: TextView
    private lateinit var tvSessionStats: TextView
    private lateinit var btnMarkAllAvailable: Button
    private lateinit var btnMarkAllOccupied: Button
    private lateinit var btnRandomizeSpots: Button
    private lateinit var btnCreateRandomSessions: Button
    private lateinit var btnClearAllSessions: Button
    private lateinit var btnResetAllSpots: Button
    private lateinit var btnRefreshStats: Button
    private lateinit var tvDebugLog: TextView

    private val debugLog = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_staff_devtools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadUserInfo()
        refreshStats()
    }

    private fun initViews(view: View) {
        tvCurrentUser = view.findViewById(R.id.tvCurrentUser)
        tvSpotStats = view.findViewById(R.id.tvSpotStats)
        tvSessionStats = view.findViewById(R.id.tvSessionStats)
        btnMarkAllAvailable = view.findViewById(R.id.btnMarkAllAvailable)
        btnMarkAllOccupied = view.findViewById(R.id.btnMarkAllOccupied)
        btnRandomizeSpots = view.findViewById(R.id.btnRandomizeSpots)
        btnCreateRandomSessions = view.findViewById(R.id.btnCreateRandomSessions)
        btnClearAllSessions = view.findViewById(R.id.btnClearAllSessions)
        btnResetAllSpots = view.findViewById(R.id.btnResetAllSpots)
        btnRefreshStats = view.findViewById(R.id.btnRefreshStats)
        tvDebugLog = view.findViewById(R.id.tvDebugLog)
    }

    private fun setupClickListeners() {
        btnMarkAllAvailable.setOnClickListener { markAllSpots("AVAILABLE") }
        btnMarkAllOccupied.setOnClickListener { markAllSpots("OCCUPIED") }
        btnRandomizeSpots.setOnClickListener { randomizeSpotStatuses() }
        btnCreateRandomSessions.setOnClickListener { createRandomSessions() }
        btnClearAllSessions.setOnClickListener { clearAllSessions() }
        btnResetAllSpots.setOnClickListener { resetAllSpots() }
        btnRefreshStats.setOnClickListener { refreshStats() }
    }

    private fun loadUserInfo() {
        val user = authRepository.getCurrentUserSync()
        if (user != null) {
            tvCurrentUser.text = "Staff: ${user.email}\nUID: ${user.uid}"
        } else {
            tvCurrentUser.text = "Not logged in"
        }
    }

    private fun refreshStats() {
        // Count spots by status
        firestore.collection("parking_spots")
            .get()
            .addOnSuccessListener { docs ->
                val total = docs.size()
                val available = docs.documents.count { it.getString("status") == "AVAILABLE" }
                val occupied = docs.documents.count { it.getString("status") == "OCCUPIED" }
                val other = total - available - occupied
                
                tvSpotStats.text = "Spots: $total total | $available available | $occupied occupied | $other other"
            }

        // Count sessions
        firestore.collection("parking_sessions")
            .get()
            .addOnSuccessListener { docs ->
                val total = docs.size()
                val active = docs.documents.count { it.getString("status") == "ACTIVE" }
                val completed = total - active
                
                tvSessionStats.text = "Sessions: $total total | $active active | $completed completed"
            }
            
        log("Stats refreshed")
    }

    private fun markAllSpots(status: String) {
        log("Marking all spots as $status...")
        
        firestore.collection("parking_spots")
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    batch.update(doc.reference, "status", status)
                    if (status == "AVAILABLE") {
                        batch.update(doc.reference, "currentSessionId", null)
                    }
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Marked ${docs.size()} spots as $status")
                        showToast("All spots marked $status")
                        refreshStats()
                    }
                    .addOnFailureListener { e ->
                        log("Error: ${e.message}")
                    }
            }
    }

    private fun randomizeSpotStatuses() {
        log("Randomizing spot statuses...")
        
        val statuses = listOf("AVAILABLE", "AVAILABLE", "AVAILABLE", "OCCUPIED") // 75% available
        
        firestore.collection("parking_spots")
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    val randomStatus = statuses.random()
                    batch.update(doc.reference, "status", randomStatus)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Randomized ${docs.size()} spots")
                        showToast("Spot statuses randomized")
                        refreshStats()
                    }
            }
    }

    private fun createRandomSessions() {
        log("Creating random sessions...")
        
        firestore.collection("parking_spots")
            .whereEqualTo("status", "AVAILABLE")
            .limit(5)
            .get()
            .addOnSuccessListener { spotDocs ->
                if (spotDocs.isEmpty) {
                    log("No available spots")
                    showToast("No available spots")
                    return@addOnSuccessListener
                }
                
                var created = 0
                spotDocs.documents.forEach { spotDoc ->
                    val spotId = spotDoc.id
                    val spotCode = spotDoc.getString("code") ?: "S${created + 1}"
                    val lotId = spotDoc.getString("lotId") ?: ""
                    
                    // Create session with random user ID
                    val sessionData = hashMapOf(
                        "userId" to "test_user_${(1000..9999).random()}",
                        "spotId" to spotId,
                        "spotCode" to spotCode,
                        "lotId" to lotId,
                        "entryTime" to Date(System.currentTimeMillis() - (1..4).random() * 3600000L),
                        "exitTime" to null,
                        "status" to "ACTIVE",
                        "totalAmount" to 0.0,
                        "paymentStatus" to "PENDING",
                        "entryMethod" to "DEVTOOLS"
                    )
                    
                    firestore.collection("parking_sessions")
                        .add(sessionData)
                        .addOnSuccessListener { docRef ->
                            // Mark spot as occupied
                            firestore.collection("parking_spots")
                                .document(spotId)
                                .update("status", "OCCUPIED", "currentSessionId", docRef.id)
                            
                            created++
                            if (created == spotDocs.size()) {
                                log("Created $created sessions")
                                showToast("Created $created sessions")
                                refreshStats()
                            }
                        }
                }
            }
    }

    private fun clearAllSessions() {
        log("Clearing all sessions...")
        
        firestore.collection("parking_sessions")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    log("No sessions to clear")
                    showToast("No sessions found")
                    return@addOnSuccessListener
                }
                
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Deleted ${docs.size()} sessions")
                        showToast("Cleared ${docs.size()} sessions")
                        refreshStats()
                    }
            }
    }

    private fun resetAllSpots() {
        log("Resetting all spots to AVAILABLE...")
        
        firestore.collection("parking_spots")
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    batch.update(doc.reference, 
                        "status", "AVAILABLE",
                        "currentSessionId", null,
                        "isOccupied", false,
                        "isReserved", false
                    )
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Reset ${docs.size()} spots")
                        showToast("Reset ${docs.size()} spots to AVAILABLE")
                        refreshStats()
                    }
            }
    }

    private fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        debugLog.insert(0, "[$timestamp] $message\n")
        tvDebugLog.text = debugLog.toString()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance() = StaffDevToolsFragment()
    }
}
