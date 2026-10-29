package com.gamepilot

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Events {
    @Volatile var current = "Idle"
        private set
    fun record(context: Context, message: String) {
        current = message
        val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
        val prefs = context.getSharedPreferences("events", 0)
        val old = prefs.getString("history", "") ?: ""
        prefs.edit().putString("history", ("$time $message\n" + old).take(12000)).apply()
    }
    fun history(context: Context) = context.getSharedPreferences("events", 0).getString("history", "") ?: ""
}
