package com.example.planplate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class MealFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvEmptyPlan: TextView
    private lateinit var mealContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meal, container, false)

        db = FirebaseFirestore.getInstance()
        tvEmptyPlan = view.findViewById(R.id.tvEmptyPlan)
        mealContainer = view.findViewById(R.id.mealContainer)
        var chatbot = view.findViewById<Button>(R.id.btnRegeneratePlan)

        chatbot.setOnClickListener {
            val intent = Intent(requireContext(), MealChatActivity::class.java)
            startActivity(intent)
        }

        loadMealPlan()
        return view
    }

    private fun loadMealPlan() {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (userId == null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "⚠️ User not logged in", Toast.LENGTH_SHORT).show()
            }
            return
        }

        db.collection("users")
            .document(userId)
            .collection("plans")
            .document("latest_plan")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val data = document.data
                    android.util.Log.d("MealFragment", "📦 Firestore Data: $data")

                    if (data != null && data.containsKey("meals")) {
                        val rawMeals = data["meals"]
                        android.util.Log.d("MealFragment", "🍽 Raw meals type: ${rawMeals?.javaClass?.name}")

                        val meals = when (rawMeals) {
                            is List<*> -> rawMeals.mapNotNull {
                                android.util.Log.d("MealFragment", "➡️ Meal item: $it")
                                it as? Map<String, Any>
                            }
                            else -> emptyList()
                        }

                        android.util.Log.d("MealFragment", "✅ Parsed meals count: ${meals.size}")
                        displayMeals(meals)
                    } else {
                        Toast.makeText(requireContext(), "No 'meals' field found.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No meal plan found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading meal plan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun displayMeals(meals: List<Map<String, Any>>?) {
        if (!isAdded) return

        val container = view?.findViewById<LinearLayout>(R.id.mealContainer)
        val emptyView = view?.findViewById<TextView>(R.id.tvEmptyPlan)
        container?.removeAllViews()

        if (meals.isNullOrEmpty()) {
            emptyView?.visibility = View.VISIBLE
            container?.visibility = View.GONE
            return
        }

        emptyView?.visibility = View.GONE
        container?.visibility = View.VISIBLE

        for (meal in meals) {
            val day = meal["day"]?.toString() ?: "Unknown"
            val breakfast = meal["breakfast"]?.toString() ?: "-"
            val lunch = meal["lunch"]?.toString() ?: "-"
            val dinner = meal["dinner"]?.toString() ?: "-"

            val card = TextView(requireContext())
            card.text = "🍽️ $day\n• Breakfast: $breakfast\n• Lunch: $lunch\n• Dinner: $dinner\n"
            card.textSize = 16f
            card.setPadding(24, 20, 24, 20)
            card.background = requireContext().getDrawable(R.drawable.meal_card_bg)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            card.layoutParams = params
            container?.addView(card)
        }
    }
}
