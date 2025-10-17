package com.example.linknpark.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Add any initialization you want here
        title = "Home"
    }
}
