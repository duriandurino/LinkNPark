package com.example.linknpark.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R

class UserHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)
        title = "User Dashboard"
    }
}
