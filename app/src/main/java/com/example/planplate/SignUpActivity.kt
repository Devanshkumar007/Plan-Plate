package com.example.planplate

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var db: DBHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        db = DBHelper(this)

        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val inputName = findViewById<EditText>(R.id.inputName)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvLoginRedirect = findViewById<TextView>(R.id.tvLoginRedirect)

        btnSignUp.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()
            val name = inputName.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please input all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = db.registerUser(email, password, name)
            if (ok) {
                Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "User already exists!", Toast.LENGTH_LONG).show()
            }
        }

        tvLoginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
