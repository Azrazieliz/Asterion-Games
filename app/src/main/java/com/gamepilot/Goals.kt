package com.gamepilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GoalType { REWARDS, DAILIES, FARMING }

data class GameGoal(
    val type: GoalType,
    val enabled: Boolean = true,
    val targetRuns: Int = 1,
    val customCues: List<String> = emptyList()
) {
    val id: String get() = type.name.lowercase(Locale.ROOT)
    val title: String get() = when (type) {
        GoalType.REWARDS -> "Collect available rewards"
        GoalType.DAILIES -> "Complete daily activities"
        GoalType.FARMING -> "Complete repeatable farming runs"
    }
}

/** These are initial tasks, not game-specific completion guarantees. */
object GoalSettings {
    private val types = GoalType.entries
    fun defaults() = types.map { GameGoal(it) }
    fun get(context: Context, gameId: Int): List<GameGoal> {
        val raw = context.getSharedPreferences("goal_settings_v1", 0)
            .getString(gameId.toString(), null) ?: return defaults()
        return try {
            val list = JSONArray(raw)
            val parsed = (0 until list.length()).map { i ->
                val json = list.getJSONObject(i)
                val cues = json.optJSONArray("cues") ?: JSONArray()
                GameGoal(
                    GoalType.valueOf(json.getString("type")),
                    json.optBoolean("enabled", true),
                    json.optInt("targetRuns", 1).coerceIn(1, 999),
                    (0 until cues.length()).map { cues.getString(it).trim() }
                        .filter { it.isNotEmpty() }.take(30)
                )
            }
            types.map { type -> parsed.find { it.type == type } ?: GameGoal(type) }
        } catch (_: Exception) { defaults() }
    }
    fun save(context: Context, gameId: Int, goals: List<GameGoal>) {
        val arr = JSONArray()
        goals.forEach { goal ->
            arr.put(JSONObject().put("type", goal.type.name)
                .put("enabled", goal.enabled)
                .put("targetRuns", goal.targetRuns)
                .put("cues", JSONArray(goal.customCues)))
        }
        context.getSharedPreferences("goal_settings_v1", 0).edit()
            .putString(gameId.toString(), arr.toString()).apply()
    }
}

/**
 * A state is VERIFIED only after explicit post-action game UI evidence,
 * not after timeouts, visual changes, or attempts alone.
 */
enum class GoalStatus { PENDING, RUNNING, VERIFIED, BLOCKED }
data class GoalProgress(
    val status: GoalStatus = GoalStatus.PENDING,
    val verifiedRuns: Int = 0,
    val attempts: Int = 0,
    val reason: String = "",
    val modifiedAt: Long = 0L
)

class GoalLedger(private val context: Context) {
    private val prefs = context.getSharedPreferences("goal_ledger_v1", 0)
    private fun dateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun key(gameId: Int, goal: GameGoal): String = "$gameId:${dateKey()}:${goal.id}"
    fun read(gameId: Int, goal: GameGoal): GoalProgress {
        val raw = prefs.getString(key(gameId, goal), null) ?: return GoalProgress()
        return try {
            val o = JSONObject(raw)
            GoalProgress(
                GoalStatus.valueOf(o.getString("status")),
                o.optInt("verifiedRuns", 0),
                o.optInt("attempts", 0),
                o.optString("reason"),
                o.optLong("modifiedAt")
            )
        } catch (_: Exception) { GoalProgress() }
    }
    fun write(gameId: Int, goal: GameGoal, progress: GoalProgress) {
        val o = JSONObject().put("status", progress.status.name)
            .put("verifiedRuns", progress.verifiedRuns)
            .put("attempts", progress.attempts).put("reason", progress.reason)
            .put("modifiedAt", System.currentTimeMillis())
        prefs.edit().putString(key(gameId, goal), o.toString()).apply()
    }
    fun attempt(gameId: Int, goal: GameGoal) {
        val old = read(gameId, goal)
        write(gameId, goal, old.copy(status = GoalStatus.RUNNING, attempts = old.attempts + 1))
    }
    fun verify(gameId: Int, goal: GameGoal, evidence: String): GoalProgress {
        val old = read(gameId, goal)
        val runs = old.verifiedRuns + 1
        val status = if (runs >= goal.targetRuns) GoalStatus.VERIFIED else GoalStatus.RUNNING
        val next = old.copy(status = status, verifiedRuns = runs,
            reason = "Verified game evidence: ${evidence.take(150)}")
        write(gameId, goal, next)
        Events.record(context, "Game $gameId / ${goal.id}: verified $runs/${goal.targetRuns} ($evidence)")
        return next
    }
    fun block(gameId: Int, goal: GameGoal, reason: String) {
        val old = read(gameId, goal)
        if (old.status != GoalStatus.VERIFIED)
            write(gameId, goal, old.copy(status = GoalStatus.BLOCKED, reason = reason.take(250)))
    }
    fun report(gameId: Int): String = GoalSettings.get(context, gameId)
        .filter { it.enabled }.joinToString("\n") { goal ->
            val state = read(gameId, goal)
            "${goal.title}: ${state.status}, ${state.verifiedRuns}/${goal.targetRuns} verified; ${state.reason}"
        }
}
