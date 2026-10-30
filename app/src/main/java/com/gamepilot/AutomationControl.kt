package com.gamepilot

import android.content.Context
import android.content.Intent

/**
 * A durable master kill switch. OFF persists across process deaths, alarms,
 * boot and app restart. It must never be bypassed by a queued job.
 */
object AutomationControl {
    private const val STORE = "automation_control_v1"
    private const val KEY = "master_enabled"
    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).getBoolean(KEY, true)

    @Synchronized fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val saved = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).commit()
        if (!saved) return false
        if (enabled) {
            Scheduler.all(context)
            Events.record(context, "Master automation ENABLED")
        } else {
            Scheduler.cancelAll(context)
            ScheduledJobs.cancelAll(context)
            GameAccessibilityService.instance?.stopAll(cancelPending = true)
            context.stopService(Intent(context, LiveVisionService::class.java))
            Events.record(context, "Master automation OFF: alarms cancelled; queue cleared; controller and perception stopped")
        }
        return true
    }
}
