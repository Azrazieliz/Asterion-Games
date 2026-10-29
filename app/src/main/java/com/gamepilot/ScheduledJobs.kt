package com.gamepilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable scheduler-to-controller queue. Accessibility service process death no longer
 * drops an alarm silently. Jobs remain pending, not falsely marked completed.
 * Android background-start/lock restrictions still require a permitted launch path.
 */
object ScheduledJobs {
    private const val KEY = "jobs"
    private const val MAX_AGE = 24 * 60 * 60 * 1000L
    private fun prefs(context: Context) = context.getSharedPreferences("scheduled_jobs_v1", 0)
    private fun read(context: Context): List<Pair<Int, Long>> {
        val raw = prefs(context).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { idx ->
                val o = arr.getJSONObject(idx)
                o.getInt("id") to o.getLong("queuedAt")
            }.filter { (_, time) ->
                time > 0L && System.currentTimeMillis() - time in 0..MAX_AGE
            }.distinctBy { it.first }
        } catch (_: Exception) { emptyList() }
    }
    private fun store(context: Context, values: List<Pair<Int, Long>>) {
        val arr = JSONArray()
        values.forEach { (id, time) ->
            arr.put(JSONObject().put("id", id).put("queuedAt", time))
        }
        prefs(context).edit().putString(KEY, arr.toString()).commit()
    }
    @Synchronized fun enqueue(context: Context, game: GameProfile) {
        val old = read(context)
        if (old.none { it.first == game.id }) store(context, old + (game.id to System.currentTimeMillis()))
    }
    @Synchronized fun started(context: Context, game: GameProfile) {
        store(context, read(context).filterNot { it.first == game.id })
        Events.record(context, "${game.title}: durable scheduled job started")
    }
    fun finished(context: Context, game: GameProfile) {
        Events.record(context, "${game.title}: session finished, pending tasks kept in ledger")
    }
    @Synchronized fun recover(context: Context): List<GameProfile> {
        val profiles = Profiles.all(context)
        val pending = read(context)
        store(context, pending)
        return pending.mapNotNull { (id, _) ->
            profiles.find { it.id == id && it.packageName.isNotBlank() }
        }
    }
    fun pendingCount(context: Context) = read(context).size
}
