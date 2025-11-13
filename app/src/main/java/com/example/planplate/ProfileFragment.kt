package com.example.planplate

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val nameTextView = view.findViewById<TextView>(R.id.UserName)
        val emailTextView = view.findViewById<TextView>(R.id.UserEmail)

        val session = SessionManager(requireContext())
        val db = DBHelper(requireContext())
        val currentEmail = session.getCurrentUser()

        if (currentEmail != null) {
            val name = db.getUserName(currentEmail) ?: "User"
            nameTextView.text = name
            emailTextView.text = currentEmail
        } else {
            nameTextView.text = "User"
            emailTextView.text = "No Email"
        }

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            session.clearSession()

            // Redirect user to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
