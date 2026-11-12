package com.example.quizmaster

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quiz_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveHistoryItem(item: HistoryItem) {
        val history = getHistory().toMutableList()
        history.add(0, item) // Add to the beginning of the list
        val json = gson.toJson(history)
        prefs.edit().putString("history_list", json).apply()
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history_list", null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearHistory() {
        prefs.edit().remove("history_list").apply()
    }
}
