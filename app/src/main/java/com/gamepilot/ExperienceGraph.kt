package com.gamepilot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Per-game on-device experience graph. It preserves successful UI trajectories as
 * transitions, not as screenshots. Only paths with actual verified goal evidence
 * are eligible for autonomous replay without fresh text candidates.
 */
class ExperienceGraph(context: Context) {
    data class Edge(
        val actionKey: String,
        val label: String,
        val x: Float,
        val y: Float,
        val destination: String,
        val attempts: Int,
        val changes: Int,
        val verified: Int
    )
    private val dbHelper = object : SQLiteOpenHelper(context, "pilot_experience_v1.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS trajectory (
                    game INTEGER NOT NULL,
                    goal TEXT NOT NULL,
                    scene TEXT NOT NULL,
                    action TEXT NOT NULL,
                    label TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    destination TEXT NOT NULL,
                    attempts INTEGER NOT NULL,
                    changes INTEGER NOT NULL,
                    verified INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(game, goal, scene, action)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX trajectory_to ON trajectory(game, goal, destination)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
    private fun row(db: SQLiteDatabase, game: Int, goal: String, scene: String, action: String): Edge? {
        db.query("trajectory",
            arrayOf("label", "x", "y", "destination", "attempts", "changes", "verified"),
            "game=? AND goal=? AND scene=? AND action=?",
            arrayOf(game.toString(), goal, scene, action), null, null, null, "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Edge(action, cursor.getString(0), cursor.getFloat(1), cursor.getFloat(2),
                cursor.getString(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6))
        }
    }
    @Synchronized fun observe(game: GameProfile, choice: Brain.Choice,
                               before: LiveFrame, after: LiveFrame, changed: Boolean) {
        val tap = choice.action as? GameAction.Tap ?: return
        try {
            val db = dbHelper.writableDatabase
            val previous = row(db, game.id, choice.goal.id, before.sceneKey(), choice.key)
            val values = ContentValues().apply {
                put("game", game.id)
                put("goal", choice.goal.id)
                put("scene", before.sceneKey())
                put("action", choice.key)
                put("label", choice.label)
                put("x", tap.x)
                put("y", tap.y)
                put("destination", after.sceneKey())
                put("attempts", (previous?.attempts ?: 0) + 1)
                put("changes", (previous?.changes ?: 0) + if (changed) 1 else 0)
                put("verified", previous?.verified ?: 0)
                put("updatedAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("trajectory", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (error: Exception) {
            Events.record(dbHelperContext, "Trajectory write error: ${error.javaClass.simpleName}")
        }
    }
    private val dbHelperContext: Context = context.applicationContext
    @Synchronized fun verified(gameId: Int, goal: GameGoal, scene: String, action: String) {
        try {
            dbHelper.writableDatabase.execSQL(
                "UPDATE trajectory SET verified=verified+1 WHERE game=? AND goal=? AND scene=? AND action=?",
                arrayOf(gameId.toString(), goal.id, scene, action)
            )
        } catch (error: Exception) {
            Events.record(dbHelperContext, "Trajectory verification error: ${error.javaClass.simpleName}")
        }
    }
    private fun evidenceDownstream(db: SQLiteDatabase, gameId: Int, goal: String, scene: String): Int {
        db.rawQuery(
            "SELECT COALESCE(SUM(verified),0) FROM trajectory WHERE game=? AND goal=? AND scene=?",
            arrayOf(gameId.toString(), goal, scene)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
    @Synchronized fun recommend(game: GameProfile, goal: GameGoal, scene: String): Edge? {
        return try {
            val db = dbHelper.readableDatabase
            val edges = mutableListOf<Edge>()
            db.query("trajectory",
                arrayOf("action", "label", "x", "y", "destination", "attempts", "changes", "verified"),
                "game=? AND goal=? AND scene=?", arrayOf(game.id.toString(), goal.id, scene),
                null, null, "verified DESC, changes DESC", "30"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    edges.add(Edge(cursor.getString(0), cursor.getString(1),
                        cursor.getFloat(2), cursor.getFloat(3), cursor.getString(4),
                        cursor.getInt(5), cursor.getInt(6), cursor.getInt(7)))
                }
            }
            edges.filter { it.x in 0f..1f && it.y in 0f..1f &&
                it.attempts >= 1 && it.changes > 0 &&
                (it.verified > 0 || evidenceDownstream(db, game.id, goal.id, it.destination) > 0)
            }.maxByOrNull { edge ->
                edge.verified * 20.0 +
                    evidenceDownstream(db, game.id, goal.id, edge.destination) * 5.0 +
                    edge.changes.toDouble() / edge.attempts.coerceAtLeast(1)
            }
        } catch (error: Exception) {
            Events.record(dbHelperContext, "Trajectory lookup error: ${error.javaClass.simpleName}")
            null
        }
    }
}
