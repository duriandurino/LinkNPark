package com.example.linknpark.ui.login

interface LoginContract {
    interface View {
        fun showUsernameError(message: String)
        fun showPasswordError(message: String)
        fun showLoginError(message: String)
        fun showLoading(show: Boolean)
        fun navigateToStaffHome()
        fun navigateToUserHome()
    }

    interface Presenter {
        fun attach(view: View)
        fun detach()
        fun onLoginClicked(username: String?, password: String?)
    }
}
