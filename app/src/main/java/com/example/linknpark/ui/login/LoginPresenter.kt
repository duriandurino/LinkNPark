package com.example.linknpark.ui.login

import android.os.Handler
import android.os.Looper

class LoginPresenter : LoginContract.Presenter {

    private var view: LoginContract.View? = null

    // Hardcoded credentials
    private val staffUsername = "admin"
    private val staffPassword = "password"

    private val userUsername = "user"
    private val userPassword = "password"

    override fun attach(view: LoginContract.View) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }

    override fun onLoginClicked(username: String?, password: String?) {
        if (username.isNullOrBlank()) {
            view?.showUsernameError("Username is required")
            return
        }
        if (password.isNullOrBlank()) {
            view?.showPasswordError("Password is required")
            return
        }

        view?.showLoading(true)

        Handler(Looper.getMainLooper()).postDelayed({
            view?.showLoading(false)

            when {
                username == staffUsername && password == staffPassword -> {
                    view?.navigateToStaffHome()
                }
                username == userUsername && password == userPassword -> {
                    view?.navigateToUserHome()
                }
                else -> {
                    view?.showLoginError("Invalid credentials")
                }
            }
        }, 600)
    }
}
