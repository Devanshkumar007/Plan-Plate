package com.example.planplate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "PlanPlate.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users(
                email TEXT PRIMARY KEY,
                password TEXT,
                name TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE plans(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_email TEXT,
                meals TEXT,
                grocery_list TEXT,
                timestamp INTEGER,
                FOREIGN KEY(user_email) REFERENCES users(email)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS plans")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    // ----- Users -----
    fun registerUser(email: String, password: String, name: String): Boolean {
        val db = writableDatabase
        // check exists
        val cursor = db.rawQuery("SELECT email FROM users WHERE email=?", arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        if (exists) return false

        val cv = ContentValues().apply {
            put("email", email)
            put("password", password)
            put("name", name)
        }
        val res = db.insert("users", null, cv)
        return res != -1L
    }

    fun validateUser(email: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT email FROM users WHERE email=? AND password=?", arrayOf(email, password))
        val ok = cursor.count > 0
        cursor.close()
        return ok
    }

    fun getUserName(email: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name FROM users WHERE email=?", arrayOf(email))
        val name = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()
        return name
    }

    // ----- Plans -----
    /**
     * Save or update latest plan for user. We just insert a new row; when loading we select the latest by timestamp.
     */
    fun savePlan(userEmail: String, mealsJson: String, groceryJson: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("user_email", userEmail)
            put("meals", mealsJson)
            put("grocery_list", groceryJson)
            put("timestamp", System.currentTimeMillis())
        }
        val id = db.insert("plans", null, cv)
        return id != -1L
    }

    /**
     * Get latest plan for the user as Pair(mealsJson, groceryJson) or null if none.
     */
    fun getLatestPlan(userEmail: String): Pair<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT meals, grocery_list FROM plans WHERE user_email=? ORDER BY timestamp DESC LIMIT 1",
            arrayOf(userEmail)
        )
        val result = if (cursor.moveToFirst()) {
            val meals = cursor.getString(0) ?: ""
            val grocery = cursor.getString(1) ?: ""
            Pair(meals, grocery)
        } else null
        cursor.close()
        return result
    }
}
