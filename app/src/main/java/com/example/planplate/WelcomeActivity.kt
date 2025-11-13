package com.example.planplate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnEmail = findViewById<Button>(R.id.btnEmail)

        // Google login removed — button will now redirect to signup
        btnGoogle.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        btnEmail.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
