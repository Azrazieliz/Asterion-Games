package com.gamepilot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {
    fun schedule(context: Context, p: GameProfile) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context, p.id,
            Intent(context, ScheduleReceiver::class.java).putExtra("profileId", p.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
        if (!p.enabled || p.packageName.isBlank()) return
        val whenMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, p.hour)
            set(Calendar.MINUTE, p.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        try {
            if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
                Events.record(context, "Inexact alarm for ${p.title}: exact-alarm permission missing")
            } else manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
        } catch (error: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pending)
            Events.record(context, "${p.title}: exact alarm denied, fallback inexact")
        }
    }
    fun all(context: Context) = Profiles.all(context).forEach { schedule(context, it) }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = Profiles.all(context).find { it.id == intent.getIntExtra("profileId", -1) } ?: return
        Scheduler.schedule(context, p)
        if (!p.enabled || p.packageName.isBlank()) return
        ScheduledJobs.enqueue(context, p)
        val service = GameAccessibilityService.instance
        if (service == null) {
            Events.record(context, "${p.title}: scheduled job queued; accessibility controller not active")
        } else service.enqueue(p)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                Scheduler.all(context)
                Events.record(context, "Alarms restored after system event: ${intent.action}")
            }
        }
    }
}
