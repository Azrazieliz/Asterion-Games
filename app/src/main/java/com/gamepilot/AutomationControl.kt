package com.gamepilot

import android.content.Context
import android.content.Intent

/**
 * Three independent user controls:
 * RUNNING executes jobs, PAUSED preserves them without dispatching input,
 * OFF cancels pending work and scheduled alarms.
 * State is persisted before touching the controller so process recreation
 * cannot accidentally resume a paused session.
 */
object AutomationControl {
    enum class Mode { RUNNING, PAUSED, OFF }
    private const val STORE = "automation_control_v1"
    private const val KEY = "mode_v2"
    private fun prefs(context: Context) =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    fun mode(context: Context): Mode {
        val p = prefs(context)
        val mode = p.getString(KEY, null)
        return when (mode) {
            "RUNNING" -> Mode.RUNNING
            "PAUSED" -> Mode.PAUSED
            "OFF" -> Mode.OFF
            else -> if (p.getBoolean("master_enabled", true)) Mode.RUNNING else Mode.OFF
        }
    }
    /** Scheduling remains active while paused; no actions are dispatched. */
    fun enabled(context: Context): Boolean = mode(context) != Mode.OFF
    fun running(context: Context): Boolean = mode(context) == Mode.RUNNING

    @Synchronized fun setMode(context: Context, newMode: Mode): Boolean {
        val previous = mode(context)
        if (newMode == previous) return true
        if (!prefs(context).edit().putString(KEY, newMode.name)
                .putBoolean("master_enabled", newMode != Mode.OFF).commit()) return false
        when (newMode) {
            Mode.OFF -> {
                Scheduler.cancelAll(context)
                ScheduledJobs.cancelAll(context)
                GameAccessibilityService.instance?.stopAll(cancelPending = true)
                context.stopService(Intent(context, LiveVisionService::class.java))
                Events.record(context, "OFF: schedules cancelled, queue cleared and live perception stopped")
            }
            Mode.PAUSED -> {
                GameAccessibilityService.instance?.pauseAndRetain()
                Events.record(context, "PAUSED: no taps; pending work preserved for resume")
            }
            Mode.RUNNING -> {
                Scheduler.all(context)
                GameAccessibilityService.instance?.recoverPending()
                Events.record(context, "RUNNING: resumes retained jobs after rechecking live game state")
            }
        }
        AutomationNotification.refresh(context)
        return true
    }
    fun setEnabled(context: Context, enabled: Boolean): Boolean =
        setMode(context, if (enabled) Mode.RUNNING else Mode.OFF)
}
