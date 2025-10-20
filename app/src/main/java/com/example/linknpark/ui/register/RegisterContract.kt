package com.example.linknpark.ui.register

interface RegisterContract {
    interface View {
        fun showNameError(message: String)
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun showConfirmPasswordError(message: String)
        fun showRegistrationError(message: String)
        fun showLoading(show: Boolean)
        fun showRegistrationSuccess(message: String)
        fun navigateToLogin()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onRegisterClicked(
            name: String?,
            email: String?,
            password: String?,
            confirmPassword: String?,
            isDriver: Boolean
        )
    }
}

