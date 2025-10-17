package com.example.linknpark.ui.staff.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.linknpark.R
import com.example.linknpark.ui.staff.StaffHomeActivity

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


        return view
    }
}
