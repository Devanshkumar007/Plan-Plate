package com.example.planplate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray

class GroceryFragment : Fragment() {

    private lateinit var groceryContainer: LinearLayout
    private lateinit var db: DBHelper
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_grocery, container, false)

        db = DBHelper(requireContext())
        session = SessionManager(requireContext())
        groceryContainer = view.findViewById(R.id.groceryContainer)

        loadGroceries()

        return view
    }

    private fun loadGroceries() {
        val currentUser = session.getCurrentUser()
        if (currentUser == null) {
            val textView = TextView(requireContext()).apply {
                text = "⚠️ Please log in to view your grocery list."
                setPadding(12, 8, 12, 8)
            }
            groceryContainer.removeAllViews()
            groceryContainer.addView(textView)
            return
        }

        val plan = db.getLatestPlan(currentUser)
        groceryContainer.removeAllViews()

        if (plan == null) {
            val textView = TextView(requireContext()).apply {
                text = "🛒 No grocery items found."
                setPadding(12, 8, 12, 8)
            }
            groceryContainer.addView(textView)
            return
        }

        val groceryJson = plan.second
        if (groceryJson.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "🛒 No grocery items found."
                setPadding(12, 8, 12, 8)
            }
            groceryContainer.addView(emptyView)
            return
        }

        try {
            val arr = JSONArray(groceryJson)
            if (arr.length() == 0) {
                val emptyView = TextView(requireContext()).apply {
                    text = "🛒 No grocery items found."
                    setPadding(12, 8, 12, 8)
                }
                groceryContainer.addView(emptyView)
                return
            }

            for (i in 0 until arr.length()) {
                val itemObj = arr.getJSONObject(i)
                val item = itemObj.optString("item", "Unknown")
                val qty = itemObj.optString("quantity", "")
                val textView = TextView(requireContext()).apply {
                    text = "✅ $item${if (qty.isNotEmpty()) " - $qty" else ""}"
                    setPadding(12, 8, 12, 8)
                }
                groceryContainer.addView(textView)
            }

        } catch (e: Exception) {
            val errorView = TextView(requireContext()).apply {
                text = "❌ Failed to load groceries: ${e.message}"
                setPadding(12, 8, 12, 8)
            }
            groceryContainer.addView(errorView)
        }
    }
}
