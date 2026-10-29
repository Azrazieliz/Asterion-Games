package com.gamepilot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, queryable game-playing experience trace.
 * Stores scene fingerprints and OCR labels, never screen-image files.
 * A trace contains observed decisions, results and validation failures,
 * enabling real-world competence evaluation from future device sessions.
 */
class EpisodeJournal(context: Context) {
    private val app = context.applicationContext
    private val helper = object : SQLiteOpenHelper(app, "pilot_episodes_v1.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE episode (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "at INTEGER NOT NULL, game INTEGER NOT NULL, goal TEXT NOT NULL," +
                "phase TEXT NOT NULL, scene TEXT NOT NULL, action TEXT NOT NULL," +
                "labels TEXT NOT NULL, details TEXT NOT NULL)"
            )
            db.execSQL("CREATE INDEX episode_game_time ON episode(game,at DESC)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
    @Synchronized fun record(
        gameId: Int, goal: String, phase: String, frame: LiveFrame?,
        action: String = "", details: String = ""
    ) {
        try {
            val db = helper.writableDatabase
            val row = ContentValues().apply {
                put("at", System.currentTimeMillis())
                put("game", gameId)
                put("goal", goal.take(50))
                put("phase", phase.take(32))
                put("scene", frame?.sceneKey()?.take(220) ?: "")
                put("action", action.take(130))
                put("labels", frame?.textValues()?.take(15)?.joinToString(" | ")?.take(450) ?: "")
                put("details", details.take(300))
            }
            db.insertOrThrow("episode", null, row)
            // Cap telemetry: each run remains locally inspectable without unbounded growth.
            db.execSQL("DELETE FROM episode WHERE id NOT IN (SELECT id FROM episode ORDER BY id DESC LIMIT 15000)")
        } catch (error: Exception) {
            Events.record(app, "Experience logging unavailable: ${error.javaClass.simpleName}")
        }
    }
    fun recent(gameId: Int, limit: Int = 50): String {
        val rows = mutableListOf<String>()
        try {
            helper.readableDatabase.query(
                "episode", arrayOf("at", "goal", "phase", "action", "details"),
                "game=?", arrayOf(gameId.toString()), null, null,
                "id DESC", limit.coerceIn(1, 100).toString()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    rows.add("${cursor.getLong(0)}  ${cursor.getString(1)} / " +
                        "${cursor.getString(2)}\n${cursor.getString(3)} — ${cursor.getString(4)}")
                }
            }
        } catch (_: Exception) { return "No readable experience records" }
        return rows.joinToString("\n\n").ifBlank { "No experience yet" }
    }
    @Synchronized fun exportJson(): JSONArray {
        val result = JSONArray()
        try {
            helper.readableDatabase.query(
                "episode", arrayOf("at", "game", "goal", "phase", "scene",
                    "action", "labels", "details"),
                null, null, null, null, "id DESC", "15000"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.put(JSONObject().put("at", cursor.getLong(0))
                        .put("game", cursor.getInt(1))
                        .put("goal", cursor.getString(2))
                        .put("phase", cursor.getString(3))
                        .put("scene", cursor.getString(4))
                        .put("action", cursor.getString(5))
                        .put("labels", cursor.getString(6))
                        .put("details", cursor.getString(7)))
                }
            }
        } catch (_: Exception) {}
        return result
    }
    @Synchronized fun importJson(records: JSONArray) {
        require(records.length() <= 15000) { "Experience log exceeds limit" }
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("episode", null, null)
            for (i in 0 until records.length()) {
                val row = records.getJSONObject(i)
                val game = row.getInt("game")
                require(game in 1..1000000)
                val values = ContentValues().apply {
                    put("at", row.getLong("at"))
                    put("game", game)
                    put("goal", row.getString("goal").take(50))
                    put("phase", row.getString("phase").take(32))
                    put("scene", row.getString("scene").take(220))
                    put("action", row.getString("action").take(130))
                    put("labels", row.getString("labels").take(450))
                    put("details", row.getString("details").take(300))
                }
                db.insertOrThrow("episode", null, values)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
