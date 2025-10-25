package com.example.linknpark.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.ui.home.fragments.HomeFragment
import com.example.linknpark.ui.home.fragments.FindParkingFragment
import com.example.linknpark.ui.home.fragments.MyBookingsFragment
import com.example.linknpark.ui.home.fragments.ProfileFragment
import com.example.linknpark.ui.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserHomeActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)
        
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set up bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_find_parking -> {
                    loadFragment(FindParkingFragment())
                    true
                }
                R.id.nav_bookings -> {
                    loadFragment(MyBookingsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        
        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
