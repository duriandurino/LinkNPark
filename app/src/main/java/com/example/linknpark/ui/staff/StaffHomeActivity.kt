package com.example.linknpark.ui.staff

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R
import com.example.linknpark.ui.login.LoginActivity
import com.example.linknpark.ui.staff.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class StaffHomeActivity : AppCompatActivity(), StaffHomeContract.View {

    lateinit var presenter: StaffHomePresenter

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
                R.id.nav_settings -> showFragment("Settings")
            }
            true
        }

        showFragment("Dashboard")
    }

    private fun showFragment(title: String) {
        val fragment = when (title) {
            "Dashboard" -> DashboardFragment()
            "Logs" -> LogsFragment()
            "Parking" -> ParkingFragment()
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
