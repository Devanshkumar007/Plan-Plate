package com.example.planplate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroceryFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var groceryContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_grocery, container, false)

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        groceryContainer = view.findViewById(R.id.groceryContainer)

        loadGroceries()

        return view
    }

    private fun loadGroceries() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, show message
            val textView = TextView(requireContext()).apply {
                text = "⚠️ Please log in to view your grocery list."
                setPadding(12, 8, 12, 8)
            }
            groceryContainer.removeAllViews()
            groceryContainer.addView(textView)
            return
        }

        val userId = currentUser.uid

        // Fetch groceries under the current user's document
        db.collection("users").document(userId)
            .collection("plans").document("latest_plan")
            .get()
            .addOnSuccessListener { doc ->
                groceryContainer.removeAllViews()

                if (doc.exists()) {
                    val groceries = doc.get("grocery_list") as? List<Map<String, Any>>
                    if (groceries.isNullOrEmpty()) {
                        val emptyView = TextView(requireContext()).apply {
                            text = "🛒 No grocery items found."
                            setPadding(12, 8, 12, 8)
                        }
                        groceryContainer.addView(emptyView)
                    } else {
                        groceries.forEach { item ->
                            val textView = TextView(requireContext()).apply {
                                text = "✅ ${item["item"]} - ${item["quantity"]}"
                                setPadding(12, 8, 12, 8)
                            }
                            groceryContainer.addView(textView)
                        }
                    }
                } else {
                    val textView = TextView(requireContext()).apply {
                        text = "No grocery plan found yet."
                        setPadding(12, 8, 12, 8)
                    }
                    groceryContainer.addView(textView)
                }
            }
            .addOnFailureListener { e ->
                groceryContainer.removeAllViews()
                val errorView = TextView(requireContext()).apply {
                    text = "❌ Failed to load groceries: ${e.message}"
                    setPadding(12, 8, 12, 8)
                }
                groceryContainer.addView(errorView)
            }
    }
}
