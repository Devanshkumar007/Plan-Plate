package com.example.planplate

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
class MealChatActivity : AppCompatActivity() {

    private lateinit var chatScroll: ScrollView
    private lateinit var chatLayout: LinearLayout
    private lateinit var inputBox: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var firestore: FirebaseFirestore
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_chat)

        chatScroll = findViewById(R.id.chatScroll)
        chatLayout = findViewById(R.id.chatLayout)
        inputBox = findViewById(R.id.etMessage)
        sendBtn = findViewById(R.id.btnSend)

        firestore = FirebaseFirestore.getInstance()

        val appContext = applicationContext
        val aiKey = appContext.packageManager
            .getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
            .metaData
            .getString("GEMINI_API_KEY") ?: ""

        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = aiKey
        )

        sendBtn.setOnClickListener {
            val userInput = inputBox.text.toString().trim()
            if (userInput.isNotEmpty()) {
                addMessage(userInput, true)
                inputBox.setText("")
                getGeminiResponse(userInput)
            }
        }
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val textView = TextView(this)
        textView.text = message
        textView.textSize = 16f
        textView.setPadding(16, 12, 16, 12)
        textView.background = if (isUser)
            getDrawable(R.drawable.chat_bubble_user)
        else
            getDrawable(R.drawable.chat_bubble_bot)
        textView.setTextColor(
            if (isUser)
                getColor(R.color.darker_gray)
            else
                getColor(R.color.green)
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(10, 8, 10, 8)
        params.gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
        textView.layoutParams = params
        chatLayout.addView(textView)
        chatScroll.post { chatScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun getGeminiResponse(prompt: String) {
        addMessage("Thinking...", false)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        text(
                            """
                        You are a meal planner AI. Based on the user prompt,
                        return a JSON object with two fields:
                        1. "meals" → List of meal objects { "day": "Monday", "breakfast": "...", "lunch": "...", "dinner": "..." }
                        2. "grocery_list" → List of ingredients with quantities.

                        Example:
                        {
                          "meals": [
                            {"day":"Monday","breakfast":"Oats","lunch":"Grilled tofu","dinner":"Vegetable curry"}
                          ],
                          "grocery_list": [
                            {"item":"Oats","quantity":"1kg"},
                            {"item":"Fruits","quantity":"500g"}
                          ]
                        }

                        User prompt: $prompt
                        """
                        )
                    }
                )

                val textResponse = response.text ?: "Sorry, no response."

                // 🧠 Extract JSON substring safely
                val jsonString = textResponse.substringAfter("{", "").substringBeforeLast("}", "")
                    .let { if (it.isNotEmpty()) "{${it}}" else "" }

                if (jsonString.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        chatLayout.removeViewAt(chatLayout.childCount - 1)
                        addMessage("⚠️ Couldn't find valid JSON in the response:\n$textResponse", false)
                    }
                    return@launch
                }

                try {
                    val jsonData = JSONObject(jsonString)
                    val meals = jsonData.optJSONArray("meals")
                    val groceryList = jsonData.optJSONArray("grocery_list")

                    // Save both to Firestore
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = user?.uid

                    if (userId == null) {
                        withContext(Dispatchers.Main) {
                            chatLayout.removeViewAt(chatLayout.childCount - 1)
                            addMessage("⚠️ User not logged in. Please log in again.", false)
                        }
                        return@launch
                    }


                    val data = hashMapOf(
                        "meals" to meals?.let { JSONArrayToList(it) },
                        "grocery_list" to groceryList?.let { JSONArrayToList(it) }
                    )

                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(userId)
                        .collection("plans")
                        .document("latest_plan")
                        .set(data)
                        .addOnSuccessListener {
                            lifecycleScope.launch(Dispatchers.Main) {
                                chatLayout.removeViewAt(chatLayout.childCount - 1)
                                addMessage("✅ Plan saved successfully for user: $userId!", false)
                            }
                        }
                        .addOnFailureListener { e ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                chatLayout.removeViewAt(chatLayout.childCount - 1)
                                addMessage("❌ Failed to save plan: ${e.message}", false)
                            }
                        }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        chatLayout.removeViewAt(chatLayout.childCount - 1)
                        addMessage("⚠️ JSON Parsing Error: ${e.message}\n\nResponse:\n$textResponse", false)
                    }
                }



            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatLayout.removeViewAt(chatLayout.childCount - 1)
                    addMessage("Error: ${e.message}", false)
                }
            }
        }
    }

    // Helper: Convert JSONArray → List<Map<String, Any>>
    private fun JSONArrayToList(array: JSONArray): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val map = mutableMapOf<String, Any>()
            obj.keys().forEach { key ->
                map[key] = obj.get(key)
            }
            list.add(map)
        }
        return list
    }

    private fun saveToFirestore(response: String) {
        try {
            val json = JSONObject(response)
            val meals = json.getJSONArray("meals")
            val groceries = json.getJSONArray("grocery_list")

            val data = hashMapOf(
                "meals" to meals.toString(),
                "grocery_list" to groceries.toString(),
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("plans").document("latest").set(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Plan saved successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save plan.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
