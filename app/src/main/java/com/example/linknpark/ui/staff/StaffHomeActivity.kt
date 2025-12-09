package com.example.linknpark.ui.staff

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R
import com.example.linknpark.ui.devtools.StaffDevToolsFragment
import com.example.linknpark.ui.login.LoginActivity
import com.example.linknpark.ui.staff.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class StaffHomeActivity : AppCompatActivity(), StaffHomeContract.View {

    lateinit var presenter: StaffHomePresenter
    private var devToolsTapCount = 0
    private var lastTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_home)

        presenter = StaffHomePresenter(this)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment("Dashboard")
                R.id.nav_logs -> showFragment("Logs")
                R.id.nav_parking -> showFragment("Parking")
                R.id.nav_exit -> showFragment("Exit")
                R.id.nav_settings -> {
                    // Triple tap on Settings to access DevTools
                    handleDevToolsTap()
                    showFragment("Settings")
                }
            }
            true
        }

        showFragment("Dashboard")
    }

    private fun handleDevToolsTap() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastTapTime < 500) {
            devToolsTapCount++
        } else {
            devToolsTapCount = 1
        }
        
        lastTapTime = currentTime
        
        if (devToolsTapCount >= 3) {
            devToolsTapCount = 0
            openDevTools()
        }
    }

    private fun openDevTools() {
        Toast.makeText(this, "🔧 Staff DevTools", Toast.LENGTH_SHORT).show()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, StaffDevToolsFragment.newInstance())
            .commit()
    }

    private fun showFragment(title: String) {
        val fragment = when (title) {
            "Dashboard" -> DashboardFragment()
            "Logs" -> LogsFragment()
            "Parking" -> ParkingFragment()
            "Exit" -> ExitConfirmationFragment.newInstance()
            "Settings" -> SettingsFragment()
            else -> DashboardFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // MVP methods
    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

