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
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class MealFragment : Fragment() {

    private lateinit var tvEmptyPlan: TextView
    private lateinit var mealContainer: LinearLayout
    private lateinit var db: DBHelper
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meal, container, false)

        db = DBHelper(requireContext())
        session = SessionManager(requireContext())

        tvEmptyPlan = view.findViewById(R.id.tvEmptyPlan)
        mealContainer = view.findViewById(R.id.mealContainer)
        val chatbot = view.findViewById<Button>(R.id.btnRegeneratePlan)

        chatbot.setOnClickListener {
            startActivity(Intent(requireContext(), MealChatActivity::class.java))
        }

        loadMealPlan()

        return view
    }

    private fun loadMealPlan() {
        val currentUser = session.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(requireContext(), "⚠️ User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val plan = db.getLatestPlan(currentUser)
        if (plan == null) {
            tvEmptyPlan.visibility = View.VISIBLE
            mealContainer.visibility = View.GONE
            return
        }

        val mealsJson = plan.first
        if (mealsJson.isEmpty()) {
            tvEmptyPlan.visibility = View.VISIBLE
            mealContainer.visibility = View.GONE
            return
        }

        try {
            val array = JSONArray(mealsJson)
            val meals = mutableListOf<Map<String, Any>>()

            for (i in 0 until array.length()) {
                val element = array.get(i)

                if (element is JSONObject) {
                    // Proper meal JSON object
                    val map = mutableMapOf<String, Any>()
                    element.keys().forEach { key ->
                        map[key] = element.get(key)
                    }
                    meals.add(map)

                } else if (element is String) {
                    // AI returned a simple string meal
                    meals.add(
                        mapOf(
                            "day" to "Meal ${i + 1}",
                            "breakfast" to element,
                            "lunch" to "-",
                            "dinner" to "-"
                        )
                    )
                }
            }

            displayMeals(meals)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error parsing saved plan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayMeals(meals: List<Map<String, Any>>?) {
        if (!isAdded) return

        mealContainer.removeAllViews()

        if (meals.isNullOrEmpty()) {
            tvEmptyPlan.visibility = View.VISIBLE
            mealContainer.visibility = View.GONE
            return
        }

        tvEmptyPlan.visibility = View.GONE
        mealContainer.visibility = View.VISIBLE

        for (meal in meals) {
            val day = meal["day"]?.toString() ?: "Meal"
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

            mealContainer.addView(card)
        }
    }
}
