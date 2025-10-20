package com.example.linknpark.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R
import com.example.linknpark.ui.login.LoginActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private lateinit var presenter: RegisterContract.Presenter
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var rgAccountType: RadioGroup
    private lateinit var rbDriver: RadioButton
    private lateinit var btnRegister: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        rgAccountType = findViewById(R.id.rgAccountType)
        rbDriver = findViewById(R.id.rbDriver)
        btnRegister = findViewById(R.id.btnRegister)
        progress = findViewById(R.id.progress)
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        // Initialize presenter
        presenter = RegisterPresenter()
        presenter.attach(this)

        // Register button click
        btnRegister.setOnClickListener {
            tvError.visibility = View.GONE
            tvSuccess.visibility = View.GONE
            tilName.error = null
            tilEmail.error = null
            tilPassword.error = null
            tilConfirmPassword.error = null

            presenter.onRegisterClicked(
                name = etName.text?.toString(),
                email = etEmail.text?.toString(),
                password = etPassword.text?.toString(),
                confirmPassword = etConfirmPassword.text?.toString(),
                isDriver = rbDriver.isChecked
            )
        }

        // Login link click
        tvLoginLink.setOnClickListener {
            navigateToLogin()
        }
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showNameError(message: String) {
        tilName.error = message
        etName.requestFocus()
    }

    override fun showEmailError(message: String) {
        tilEmail.error = message
        etEmail.requestFocus()
    }

    override fun showPasswordError(message: String) {
        tilPassword.error = message
        etPassword.requestFocus()
    }

    override fun showConfirmPasswordError(message: String) {
        tilConfirmPassword.error = message
        etConfirmPassword.requestFocus()
    }

    override fun showRegistrationError(message: String) {
        tvError.visibility = View.VISIBLE
        tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !show
        etName.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
        rgAccountType.isEnabled = !show
    }

    override fun showRegistrationSuccess(message: String) {
        tvSuccess.visibility = View.VISIBLE
        tvSuccess.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}

