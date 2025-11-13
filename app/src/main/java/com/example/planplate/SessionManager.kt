package com.example.planplate

import android.content.Context

class SessionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("planplate_prefs", Context.MODE_PRIVATE)

    fun setCurrentUser(email: String) {
        prefs.edit().putString("current_user", email).apply()
    }

    fun clearSession() {
        prefs.edit().remove("current_user").apply()
    }

    fun getCurrentUser(): String? {
        return prefs.getString("current_user", null)
    }
}
