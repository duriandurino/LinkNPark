package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.linknpark.R
import com.example.linknpark.ui.staff.StaffHomeActivity
import com.example.linknpark.utils.FirebaseInitializer
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            (activity as? StaffHomeActivity)?.presenter?.onLogoutClicked()
        }

        val modifyButton = view.findViewById<Button>(R.id.btnModifyParking)
        modifyButton.setOnClickListener {
            val fragment = parentFragmentManager.findFragmentByTag("ParkingFragment") as? ParkingFragment
            fragment?.let { it.presenter.onModifyParkingClicked() }
        }

        // Database initialization button (DEVELOPER TOOL - remove in production)
        val btnInitDatabase = view.findViewById<Button>(R.id.btnInitDatabase)
        val btnClearAndReinit = view.findViewById<Button>(R.id.btnClearAndReinit)
        val btnCheckStats = view.findViewById<Button>(R.id.btnCheckStats)
        val tvDatabaseStats = view.findViewById<android.widget.TextView>(R.id.tvDatabaseStats)
        val progressInit = view.findViewById<ProgressBar>(R.id.progressInit)

        // Load stats on view creation
        loadDatabaseStats(tvDatabaseStats)

        // Initialize database button
        btnInitDatabase.setOnClickListener {
            lifecycleScope.launch {
                setButtonsEnabled(false, btnInitDatabase, btnClearAndReinit, btnCheckStats)
                progressInit.visibility = View.VISIBLE

                val result = FirebaseInitializer.initializeDatabase(forceReinitialize = false)

                progressInit.visibility = View.GONE
                setButtonsEnabled(true, btnInitDatabase, btnClearAndReinit, btnCheckStats)

                result.onSuccess { message ->
                    Toast.makeText(
                        requireContext(),
                        "✅ $message\n\nGo to Parking Layout to see the data.",
                        Toast.LENGTH_LONG
                    ).show()
                    loadDatabaseStats(tvDatabaseStats)
                }.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "⚠️ ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Clear and reinitialize button
        btnClearAndReinit.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear & Reinitialize Database?")
                .setMessage("This will DELETE all parking data (users are safe) and recreate with fresh sample data.\n\nAre you sure?")
                .setPositiveButton("Yes, Clear & Reinit") { _, _ ->
                    lifecycleScope.launch {
                        setButtonsEnabled(false, btnInitDatabase, btnClearAndReinit, btnCheckStats)
                        progressInit.visibility = View.VISIBLE

                        // Step 1: Clear data
                        val clearResult = FirebaseInitializer.clearParkingData()

                        if (clearResult.isSuccess) {
                            // Step 2: Reinitialize
                            val initResult = FirebaseInitializer.initializeDatabase(forceReinitialize = true)

                            progressInit.visibility = View.GONE
                            setButtonsEnabled(true, btnInitDatabase, btnClearAndReinit, btnCheckStats)

                            initResult.onSuccess { message ->
                                Toast.makeText(
                                    requireContext(),
                                    "✅ Database cleared and reinitialized!\n\nFresh data is ready.",
                                    Toast.LENGTH_LONG
                                ).show()
                                loadDatabaseStats(tvDatabaseStats)
                            }.onFailure { error ->
                                Toast.makeText(
                                    requireContext(),
                                    "❌ Reinit failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            progressInit.visibility = View.GONE
                            setButtonsEnabled(true, btnInitDatabase, btnClearAndReinit, btnCheckStats)
                            Toast.makeText(
                                requireContext(),
                                "❌ Clear failed: ${clearResult.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Check database stats button
        btnCheckStats.setOnClickListener {
            loadDatabaseStats(tvDatabaseStats)
            Toast.makeText(requireContext(), "Stats refreshed", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadDatabaseStats(textView: android.widget.TextView) {
        lifecycleScope.launch {
            textView.text = "Loading stats..."
            val stats = FirebaseInitializer.getDataStats()

            if (stats.isNotEmpty()) {
                val statsText = """
                    📊 Database Stats:
                    • Parking Lots: ${stats["parking_lots"] ?: 0}
                    • Parking Spots: ${stats["parking_spots"] ?: 0}
                    • Sessions: ${stats["parking_sessions"] ?: 0}
                    • Reservations: ${stats["reservations"] ?: 0}
                    • Vehicles: ${stats["vehicles"] ?: 0}
                    • Payments: ${stats["payments"] ?: 0}
                    • Users: ${stats["users"] ?: 0}
                """.trimIndent()
                textView.text = statsText
            } else {
                textView.text = "⚠️ Failed to load stats"
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean, vararg buttons: Button) {
        buttons.forEach { it.isEnabled = enabled }
    }
}
