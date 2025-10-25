package com.example.linknpark.ui.login

import android.util.Log
import com.example.linknpark.data.AuthRepository
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginPresenter : LoginContract.Presenter {

    private var view: LoginContract.View? = null
    private val authRepository: AuthRepository = FirebaseAuthRepository()
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun attach(view: LoginContract.View) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }

    override fun onLoginClicked(username: String?, password: String?) {
        // Validate input (username is actually email)
        if (username.isNullOrBlank()) {
            view?.showUsernameError("Email is required")
            return
        }
        if (password.isNullOrBlank()) {
            view?.showPasswordError("Password is required")
            return
        }

        // Show loading
        view?.showLoading(true)

        // Attempt Firebase login
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.login(username, password)
                }

                result.onSuccess { user ->
                    view?.showLoading(false)
                    Log.d("LoginPresenter", "Login successful for ${user.name} with role ${user.role}")
                    
                    // Navigate based on user role
                    when (user.role) {
                        UserRole.STAFF -> {
                            view?.navigateToStaffHome()
                        }
                        UserRole.DRIVER -> {
                            view?.navigateToUserHome()
                        }
                    }
                }.onFailure { error ->
                    view?.showLoading(false)
                    view?.showLoginError(error.message ?: "Login failed")
                    Log.e("LoginPresenter", "Login failed", error)
                }

            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showLoginError("An error occurred: ${e.message}")
                Log.e("LoginPresenter", "Login exception", e)
            }
        }
    }
}
