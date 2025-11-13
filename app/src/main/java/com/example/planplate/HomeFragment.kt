package com.example.planplate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val btnChat = view.findViewById<Button>(R.id.btnChatWithAI)
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfile)
        val tvPlan = view.findViewById<TextView>(R.id.tvCurrentPlan)
        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)

        // Set greeting text
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "User"
                        tvGreeting.text = "Hello, $name 👋"
                    } else {
                        tvGreeting.text = "Hello, User 👋"
                    }
                }
                .addOnFailureListener {
                    tvGreeting.text = "Hello, User 👋"
                }
        } else {
            tvGreeting.text = "Hello, User 👋"
        }

        btnChat.setOnClickListener {
            val intent = Intent(requireContext(), MealChatActivity::class.java)
            startActivity(intent)
        }

        ivProfile.setOnClickListener {
            (activity as MainActivity).openProfileFragment()
        }

        tvPlan.setOnClickListener {
            (activity as MainActivity).openMealFragment()
        }

        return view
    }
}

