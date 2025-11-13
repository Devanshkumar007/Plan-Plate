package com.example.planplate

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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
    private lateinit var generativeModel: GenerativeModel
    private lateinit var db: DBHelper
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_chat)

        chatScroll = findViewById(R.id.chatScroll)
        chatLayout = findViewById(R.id.chatLayout)
        inputBox = findViewById(R.id.etMessage)
        sendBtn = findViewById(R.id.btnSend)

        db = DBHelper(this)
        session = SessionManager(this)

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
                        User prompt: $prompt
                        """
                        )
                    }
                )

                val textResponse = response.text ?: "Sorry, no response."

                // Extract JSON substring safely
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
                    val meals = jsonData.optJSONArray("meals") ?: JSONArray()
                    val groceryList = jsonData.optJSONArray("grocery_list") ?: JSONArray()

                    // Convert JSONArrays back to strings for storage
                    val mealsJson = meals.toString()
                    val groceryJson = groceryList.toString()

                    val currentUser = session.getCurrentUser()
                    if (currentUser == null) {
                        withContext(Dispatchers.Main) {
                            chatLayout.removeViewAt(chatLayout.childCount - 1)
                            addMessage("⚠️ User not logged in. Please log in.", false)
                        }
                        return@launch
                    }

                    val saved = db.savePlan(currentUser, mealsJson, groceryJson)
                    withContext(Dispatchers.Main) {
                        chatLayout.removeViewAt(chatLayout.childCount - 1)
                        if (saved) {
                            addMessage("✅ Plan saved locally for user: $currentUser", false)
                        } else {
                            addMessage("❌ Failed to save plan locally.", false)
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

    // Helper: Convert JSONArray -> List<Map<String, Any>> (kept if you need it)
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
}
