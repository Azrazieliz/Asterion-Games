package com.gamepilot

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray

/**
 * Portable local game learning data. The user chooses the destination through the
 * Android Storage Access Framework. No account keys, screen images or passwords.
 */
object GameDataVault {
    private const val FORMAT = 1
    private val stores = listOf("profiles", "goal_settings_v1", "goal_ledger_v1",
        "mastery_policy_v2", "scheduled_jobs_v1")
    fun export(context: Context): ByteArray {
        val json = JSONObject().put("format", FORMAT)
            .put("project", "AsterionPilot")
            .put("createdAt", System.currentTimeMillis())
            .put("rosterSize", GameRoster.titles.size)
        val data = JSONObject()
        for (name in stores) {
            val store = JSONObject()
            context.getSharedPreferences(name, 0).all.forEach { (key, value) ->
                when (value) {
                    is String -> store.put(key, value)
                    is Boolean -> store.put(key, value)
                    is Int -> store.put(key, value)
                    is Long -> store.put(key, value)
                    is Float -> store.put(key, value.toDouble())
                }
            }
            data.put(name, store)
        }
        json.put("preferences", data)
        json.put("trajectories", ExperienceGraph(context).exportJson())
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    /** Validate entire backup before mutating preferences or the on-device graph. */
    fun restore(context: Context, bytes: ByteArray) {
        require(bytes.size <= 24 * 1024 * 1024) { "Backup exceeds 24 MB" }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        require(json.getString("project") == "AsterionPilot" && json.getInt("format") == FORMAT)
            { "Unsupported backup format" }
        require(json.getInt("rosterSize") == 26) { "Unexpected roster" }
        val data = json.getJSONObject("preferences")
        val records = json.getJSONArray("trajectories")
        require(records.length() <= 25000) { "Too many learned trajectories" }
        // SQLite import is a transaction and rolls back on invalid rows.
        ExperienceGraph(context).importJson(records)
        for (name in stores) {
            val store = data.getJSONObject(name)
            val editor = context.getSharedPreferences(name, 0).edit()
            editor.clear()
            val keys = store.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when (val value = store.get(key)) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    else -> error("Unsupported backup value")
                }
            }
            check(editor.commit()) { "Storage write failed: $name" }
        }
        Scheduler.all(context)
        Events.record(context, "Imported game profiles, goal history and learned trajectories")
    }
}
