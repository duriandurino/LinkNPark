package com.example.linknpark.ui.staff

import com.example.linknpark.data.FirebaseAuthRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StaffHomePresenter(private val view: StaffHomeContract.View) : StaffHomeContract.Presenter {

    override fun onLogoutClicked() {
        // CRITICAL: Clear repository cache on logout
        val authRepository = FirebaseAuthRepository.getInstance()
        GlobalScope.launch {
            authRepository.logout()
        }
        
        view.showMessage("Logged out successfully")
        view.navigateToLogin()
    }
}
