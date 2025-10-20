package com.example.linknpark.ui.register

import android.util.Log
import android.util.Patterns
import com.example.linknpark.data.AuthRepository
import com.example.linknpark.data.FirebaseAuthRepository
import com.example.linknpark.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterPresenter : RegisterContract.Presenter {

    private var view: RegisterContract.View? = null
    private val authRepository: AuthRepository = FirebaseAuthRepository()
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun attach(view: RegisterContract.View) {
        this.view = view
    }

    override fun detach() {
        this.view = null
    }

    override fun onRegisterClicked(
        name: String?,
        email: String?,
        password: String?,
        confirmPassword: String?,
        isDriver: Boolean
    ) {
        // Validate name
        if (name.isNullOrBlank()) {
            view?.showNameError("Name is required")
            return
        }
        
        if (name.length < 2) {
            view?.showNameError("Name must be at least 2 characters")
            return
        }

        // Validate email
        if (email.isNullOrBlank()) {
            view?.showEmailError("Email is required")
            return
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view?.showEmailError("Please enter a valid email")
            return
        }

        // Validate password
        if (password.isNullOrBlank()) {
            view?.showPasswordError("Password is required")
            return
        }
        
        if (password.length < 6) {
            view?.showPasswordError("Password must be at least 6 characters")
            return
        }

        // Validate confirm password
        if (confirmPassword.isNullOrBlank()) {
            view?.showConfirmPasswordError("Please confirm your password")
            return
        }
        
        if (password != confirmPassword) {
            view?.showConfirmPasswordError("Passwords do not match")
            return
        }

        // Determine role
        val role = if (isDriver) UserRole.DRIVER else UserRole.STAFF

        // Show loading
        view?.showLoading(true)

        // Attempt Firebase registration
        presenterScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    authRepository.register(name, email, password, role)
                }

                result.onSuccess { user ->
                    view?.showLoading(false)
                    Log.d("RegisterPresenter", "Registration successful for ${user.name} with role ${user.role}")
                    view?.showRegistrationSuccess("Account created successfully!")
                    
                    // Navigate to login after 1.5 seconds
                    kotlinx.coroutines.delay(1500)
                    view?.navigateToLogin()
                    
                }.onFailure { error ->
                    view?.showLoading(false)
                    view?.showRegistrationError(error.message ?: "Registration failed")
                    Log.e("RegisterPresenter", "Registration failed", error)
                }

            } catch (e: Exception) {
                view?.showLoading(false)
                view?.showRegistrationError("An error occurred: ${e.message}")
                Log.e("RegisterPresenter", "Registration exception", e)
            }
        }
    }
}

