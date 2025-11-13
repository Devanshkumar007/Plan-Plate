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

        val session = SessionManager(requireContext())
        val currentUser = session.getCurrentUser()
        if (currentUser != null) {
            val db = DBHelper(requireContext())
            val name = db.getUserName(currentUser) ?: "User"
            tvGreeting.text = "Hello, $name 👋"
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
