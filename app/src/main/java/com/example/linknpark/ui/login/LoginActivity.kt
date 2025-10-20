package com.example.linknpark.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.linknpark.R
import com.example.linknpark.ui.staff.StaffHomeActivity
import com.example.linknpark.ui.home.UserHomeActivity
import com.example.linknpark.ui.register.RegisterActivity

class LoginActivity : AppCompatActivity(), LoginContract.View {

    private lateinit var presenter: LoginContract.Presenter
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progress = findViewById(R.id.progress)
        tvError = findViewById(R.id.tvError)
        tvSignUp = findViewById(R.id.tvSignUp)

        presenter = LoginPresenter()
        presenter.attach(this)

        btnLogin.setOnClickListener {
            tvError.visibility = View.GONE
            presenter.onLoginClicked(
                etUsername.text?.toString(),
                etPassword.text?.toString()
            )
        }

        // Navigate to Register Activity
        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showUsernameError(message: String) {
        etUsername.error = message
    }

    override fun showPasswordError(message: String) {
        etPassword.error = message
    }

    override fun showLoginError(message: String) {
        tvError.visibility = View.VISIBLE
        tvError.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
    }

    override fun navigateToStaffHome() {
        val intent = Intent(this, StaffHomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun navigateToUserHome() {
        val intent = Intent(this, UserHomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
