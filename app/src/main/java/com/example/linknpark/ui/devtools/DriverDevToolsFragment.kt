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
 * Developer Tools for Driver role.
 * Provides quick actions for testing and debugging.
 * Access: Triple-tap on Profile nav item in UserHomeActivity
 */
class DriverDevToolsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = FirebaseAuthRepository.getInstance()
    
    // UI Elements
    private lateinit var tvCurrentUser: TextView
    private lateinit var tvSessionCount: TextView
    private lateinit var tvReservationCount: TextView
    private lateinit var btnStartRandomSession: Button
    private lateinit var btnEndCurrentSession: Button
    private lateinit var btnClearMySessions: Button
    private lateinit var btnClearMyReservations: Button
    private lateinit var btnRefreshData: Button
    private lateinit var tvDebugLog: TextView

    private val debugLog = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_driver_devtools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadUserInfo()
        refreshCounts()
    }

    private fun initViews(view: View) {
        tvCurrentUser = view.findViewById(R.id.tvCurrentUser)
        tvSessionCount = view.findViewById(R.id.tvSessionCount)
        tvReservationCount = view.findViewById(R.id.tvReservationCount)
        btnStartRandomSession = view.findViewById(R.id.btnStartRandomSession)
        btnEndCurrentSession = view.findViewById(R.id.btnEndCurrentSession)
        btnClearMySessions = view.findViewById(R.id.btnClearMySessions)
        btnClearMyReservations = view.findViewById(R.id.btnClearMyReservations)
        btnRefreshData = view.findViewById(R.id.btnRefreshData)
        tvDebugLog = view.findViewById(R.id.tvDebugLog)
    }

    private fun setupClickListeners() {
        btnStartRandomSession.setOnClickListener { startRandomSession() }
        btnEndCurrentSession.setOnClickListener { endCurrentSession() }
        btnClearMySessions.setOnClickListener { clearMySessions() }
        btnClearMyReservations.setOnClickListener { clearMyReservations() }
        btnRefreshData.setOnClickListener { refreshCounts() }
    }

    private fun loadUserInfo() {
        val user = authRepository.getCurrentUserSync()
        if (user != null) {
            tvCurrentUser.text = "User: ${user.email}\nUID: ${user.uid}"
        } else {
            tvCurrentUser.text = "Not logged in"
        }
    }

    private fun refreshCounts() {
        val userId = authRepository.getCurrentUserSync()?.uid ?: return

        // Count sessions
        firestore.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                tvSessionCount.text = "Sessions: ${docs.size()}"
            }

        // Count reservations
        firestore.collection("reservations")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                tvReservationCount.text = "Reservations: ${docs.size()}"
            }
            
        log("Data refreshed")
    }

    private fun startRandomSession() {
        val userId = authRepository.getCurrentUserSync()?.uid ?: run {
            showToast("Not logged in")
            return
        }

        log("Finding available spot...")

        // Find an available spot
        firestore.collection("parking_spots")
            .whereEqualTo("status", "AVAILABLE")
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    log("No available spots found")
                    showToast("No available spots")
                    return@addOnSuccessListener
                }

                val spotDoc = docs.documents.first()
                val spotId = spotDoc.id
                val spotCode = spotDoc.getString("code") ?: "Unknown"
                val lotId = spotDoc.getString("lotId") ?: ""

                log("Starting session at spot: $spotCode")

                // Create session
                val sessionData = hashMapOf(
                    "userId" to userId,
                    "spotId" to spotId,
                    "spotCode" to spotCode,
                    "lotId" to lotId,
                    "entryTime" to Date(),
                    "exitTime" to null,
                    "status" to "ACTIVE",
                    "totalAmount" to 0.0,
                    "paymentStatus" to "PENDING",
                    "entryMethod" to "DEVTOOLS"
                )

                firestore.collection("parking_sessions")
                    .add(sessionData)
                    .addOnSuccessListener { docRef ->
                        log("Session created: ${docRef.id}")
                        
                        // Mark spot as occupied
                        firestore.collection("parking_spots")
                            .document(spotId)
                            .update("status", "OCCUPIED", "currentSessionId", docRef.id)
                            .addOnSuccessListener {
                                log("Spot $spotCode marked OCCUPIED")
                                showToast("Session started at $spotCode")
                                refreshCounts()
                            }
                    }
                    .addOnFailureListener { e ->
                        log("Error: ${e.message}")
                        showToast("Failed to create session")
                    }
            }
            .addOnFailureListener { e ->
                log("Error finding spot: ${e.message}")
            }
    }

    private fun endCurrentSession() {
        val userId = authRepository.getCurrentUserSync()?.uid ?: return

        log("Looking for active session...")

        // Find active session
        firestore.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    log("No active session found")
                    showToast("No active session")
                    return@addOnSuccessListener
                }

                val sessionDoc = docs.documents.first()
                val sessionId = sessionDoc.id
                val spotId = sessionDoc.getString("spotId") ?: ""
                val entryTime = sessionDoc.getDate("entryTime") ?: Date()
                
                // Calculate duration and fee
                val exitTime = Date()
                val durationMs = exitTime.time - entryTime.time
                val durationHours = durationMs / (1000.0 * 60 * 60)
                val hourlyRate = 50.0 // Default rate
                val totalAmount = durationHours * hourlyRate

                log("Ending session: ${String.format("%.2f", durationHours)} hrs, PHP ${String.format("%.2f", totalAmount)}")

                // Update session
                firestore.collection("parking_sessions")
                    .document(sessionId)
                    .update(
                        "exitTime", exitTime,
                        "status", "COMPLETED",
                        "totalAmount", totalAmount,
                        "durationMinutes", (durationMs / 60000).toInt()
                    )
                    .addOnSuccessListener {
                        log("Session ended")
                        
                        // Mark spot as available
                        if (spotId.isNotEmpty()) {
                            firestore.collection("parking_spots")
                                .document(spotId)
                                .update("status", "AVAILABLE", "currentSessionId", null)
                                .addOnSuccessListener {
                                    log("Spot marked AVAILABLE")
                                    refreshCounts()
                                }
                        }
                        
                        showToast("Session ended - PHP ${String.format("%.2f", totalAmount)}")
                    }
            }
    }

    private fun clearMySessions() {
        val userId = authRepository.getCurrentUserSync()?.uid ?: return

        log("Clearing all sessions for user...")

        firestore.collection("parking_sessions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Deleted ${docs.size()} sessions")
                        showToast("Cleared ${docs.size()} sessions")
                        refreshCounts()
                    }
                    .addOnFailureListener { e ->
                        log("Error: ${e.message}")
                    }
            }
    }

    private fun clearMyReservations() {
        val userId = authRepository.getCurrentUserSync()?.uid ?: return

        log("Clearing all reservations for user...")

        firestore.collection("reservations")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        log("Deleted ${docs.size()} reservations")
                        showToast("Cleared ${docs.size()} reservations")
                        refreshCounts()
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
        fun newInstance() = DriverDevToolsFragment()
    }
}
