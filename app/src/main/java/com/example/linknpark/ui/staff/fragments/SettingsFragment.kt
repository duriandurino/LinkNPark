package com.example.linknpark.ui.staff.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.ui.login.LoginActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var tvStaffName: TextView
    private lateinit var tvStaffEmail: TextView
    private lateinit var btnManualEntry: MaterialButton
    private lateinit var btnViewLogs: MaterialButton
    private lateinit var btnLogout: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_staff_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvStaffName = view.findViewById(R.id.tvStaffName)
        tvStaffEmail = view.findViewById(R.id.tvStaffEmail)
        btnManualEntry = view.findViewById(R.id.btnManualEntry)
        btnViewLogs = view.findViewById(R.id.btnViewLogs)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Set staff info (can be loaded from Firebase/SharedPreferences)
        tvStaffName.text = "Staff Member"
        tvStaffEmail.text = "staff@linknpark.com"

        // Manual entry button
        btnManualEntry.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Manual Entry/Exit feature coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // View logs button
        btnViewLogs.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Activity Logs feature coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Logout button
        btnLogout.setOnClickListener {
            // CRITICAL: Clear repository cache before logout
            val authRepository = FirebaseAuthRepository.getInstance()
            GlobalScope.launch {
                authRepository.logout()
            }
            
            // Navigate back to login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}
