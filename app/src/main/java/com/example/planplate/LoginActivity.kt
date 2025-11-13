package com.example.planplate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DBHelper(this)
        session = SessionManager(this)

        // Find Views
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val inputPassword = findViewById<TextInputEditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvSignUpRedirect = findViewById<TextView>(R.id.tvSignUpRedirect)

        // Login Button Click
        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailPattern = android.util.Patterns.EMAIL_ADDRESS
            if (!emailPattern.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else if (db.validateUser(email, password)) {
                session.setCurrentUser(email)
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Login Failed: invalid credentials",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Redirect to Sign Up
        tvSignUpRedirect.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Forgot Password (offline app cannot send email)
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Offline app: password reset is not available", Toast.LENGTH_SHORT).show()
        }
    }
}
