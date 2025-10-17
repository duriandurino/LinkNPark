package com.example.linknpark.ui.staff

class StaffHomePresenter(private val view: StaffHomeContract.View) : StaffHomeContract.Presenter {

    override fun onLogoutClicked() {
        // Logic for logout can be added here later (e.g. clearing SharedPreferences)
        view.showMessage("Logged out successfully")
        view.navigateToLogin()
    }
}
