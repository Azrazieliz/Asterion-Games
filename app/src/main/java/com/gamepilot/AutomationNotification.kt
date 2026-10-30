package com.gamepilot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Direct controls in the Android notification shade while a game is in front.
 * Uses explicit package-private broadcast intents; never launches or taps games.
 */
object AutomationNotification {
    const val CHANNEL = "asterion_automation_controls_v1"
    const val ID = 47011
    const val ACTION = "com.gamepilot.AUTOMATION_CONTROL"
    const val EXTRA_MODE = "mode"
    private fun manager(context: Context) =
        context.getSystemService(NotificationManager::class.java)

    fun refresh(context: Context) {
        try {
            val nm = manager(context)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Automation controls",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Pause, resume, or turn off Asterion Pilot"
                setShowBadge(false)
            })
            val mode = AutomationControl.mode(context)
            val view = PendingIntent.getActivity(context, 500,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Asterion • " + mode.name.lowercase().replaceFirstChar { it.uppercase() })
                .setContentText(when (mode) {
                    AutomationControl.Mode.RUNNING -> "Automation active • Pause or turn off"
                    AutomationControl.Mode.PAUSED -> "Input paused • Work preserved"
                    AutomationControl.Mode.OFF -> "Automation off • No scheduled actions"
                })
                .setContentIntent(view)
                .setOnlyAlertOnce(true)
                .setOngoing(mode != AutomationControl.Mode.OFF)
                .setCategory(Notification.CATEGORY_SERVICE)
            val buttons = if (mode == AutomationControl.Mode.RUNNING)
                listOf("Pause" to AutomationControl.Mode.PAUSED,
                    "Turn off" to AutomationControl.Mode.OFF)
            else if (mode == AutomationControl.Mode.PAUSED)
                listOf("Resume" to AutomationControl.Mode.RUNNING,
                    "Turn off" to AutomationControl.Mode.OFF)
            else listOf("Turn on" to AutomationControl.Mode.RUNNING)
            buttons.forEachIndexed { idx, (title, target) ->
                val intent = Intent(context, AutomationCommandReceiver::class.java)
                    .setAction(ACTION).putExtra(EXTRA_MODE, target.name)
                val pi = PendingIntent.getBroadcast(context, 510 + idx, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                builder.addAction(Notification.Action.Builder(
                    android.R.drawable.ic_media_pause, title, pi).build())
            }
            nm.notify(ID, builder.build())
        } catch (error: Exception) {
            Events.record(context, "Automation notification unavailable: " +
                error.javaClass.simpleName)
        }
    }
}
class AutomationCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AutomationNotification.ACTION) return
        val requested = intent.getStringExtra(AutomationNotification.EXTRA_MODE)
        val next = AutomationControl.Mode.entries.firstOrNull { it.name == requested } ?: return
        if (AutomationControl.setMode(context, next))
            AutomationNotification.refresh(context)
    }
}
