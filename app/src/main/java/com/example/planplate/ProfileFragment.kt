package com.example.planplate

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val nameTextView = view.findViewById<TextView>(R.id.UserName)
        val emailTextView = view.findViewById<TextView>(R.id.UserEmail)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val email = document.getString("email") ?: "No Email"

                        nameTextView.text = name
                        emailTextView.text = email
                    } else {
                        nameTextView.text = "User"
                        emailTextView.text = "No Email"
                    }
                }
                .addOnFailureListener {
                    nameTextView.text = "User"
                    emailTextView.text = "No Email"
                }
        } else {
            nameTextView.text = "User"
            emailTextView.text = "No Email"
        }



        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()  // Sign out from Firebase

            // Redirect user to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
