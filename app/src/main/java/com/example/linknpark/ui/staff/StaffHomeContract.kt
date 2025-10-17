package com.example.linknpark.ui.staff

interface StaffHomeContract {
    interface View {
        fun showMessage(message: String)
        fun navigateToLogin()
    }

    interface Presenter {
        fun onLogoutClicked()
    }
}
