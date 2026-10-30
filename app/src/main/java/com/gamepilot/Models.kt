package com.gamepilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** All names come from the supplied launcher image. Truncated labels remain marked as such. */
object GameRoster {
    val titles = listOf(
        "Honkai Impact 3rd", "Arknights: Endfield", "GODDESS OF VICTORY: NIKKE",
        "Bleach", "ONE PIECE…", "Honkai: Star Rail", "Chaos Zero Nightmare",
        "Arknights", "放置少女", "リバ×リバ", "Azur Lane", "Wuthering Waves",
        "Reverse: 1999", "E.T.E Chronicle", "Goddess P…", "Stellar Affin…",
        "TenkafuMA!", "Mirrors High", "Yu-Gi-Oh! Duel Links", "Dokkan",
        "SLIME IM", "少女廻戦", "7DS", "Fate/GO", "Echocalypse", "Attack on Time"
    )
}

data class GameProfile(
    val id: Int, val title: String, val packageName: String,
    val hour: Int, val minute: Int, val enabled: Boolean,
    val actions: List<GameAction>, val adaptive: Boolean = true,
    val maxMinutes: Int = 20,
    val resetHour: Int = 0
)

sealed class GameAction {
    data class Tap(val x: Float, val y: Float, val duration: Long = 80) : GameAction()
    data class Swipe(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val duration: Long = 450) : GameAction()
    data class Wait(val milliseconds: Long) : GameAction()
    data class ClickText(val text: String) : GameAction()
}

object Profiles {
    private const val KEY = "game_profiles"
    fun defaults() = GameRoster.titles.mapIndexed { index, name ->
        GameProfile(index + 1, name, "", 6, 0, false, emptyList())
    }
    fun all(context: Context): List<GameProfile> {
        val raw = context.getSharedPreferences("profiles", 0).getString(KEY, null) ?: return defaults()
        return try {
            val array = JSONArray(raw)
            val saved = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val steps = o.optJSONArray("actions") ?: JSONArray()
                GameProfile(
                    o.getInt("id"), o.getString("title"), o.optString("package"),
                    o.optInt("hour", 6), o.optInt("minute", 0), o.optBoolean("enabled"),
                    (0 until steps.length()).mapNotNull { idx -> parseAction(steps.getJSONObject(idx)) },
                    o.optBoolean("adaptive", true), o.optInt("maxMinutes", 20).coerceIn(1, 120),
                    o.optInt("resetHour", 0).coerceIn(0, 23)
                )
            }
            val known = defaults().map { default -> saved.find { it.id == default.id } ?: default }
            known + saved.filter { it.id > defaults().size }.sortedBy { it.id }
        } catch (_: Exception) { defaults() }
    }
    fun save(context: Context, profiles: List<GameProfile>) {
        val all = JSONArray()
        profiles.sortedBy { it.id }.forEach { p ->
            val o = JSONObject().put("id", p.id).put("title", p.title).put("package", p.packageName)
                .put("hour", p.hour).put("minute", p.minute).put("enabled", p.enabled)
                .put("adaptive", p.adaptive).put("maxMinutes", p.maxMinutes)
                .put("resetHour", p.resetHour)
            val actions = JSONArray()
            p.actions.forEach { step ->
                val s = when (step) {
                    is GameAction.Tap -> JSONObject().put("type", "tap").put("x", step.x.toDouble()).put("y", step.y.toDouble()).put("ms", step.duration)
                    is GameAction.Swipe -> JSONObject().put("type", "swipe").put("x1", step.x1.toDouble()).put("y1", step.y1.toDouble()).put("x2", step.x2.toDouble()).put("y2", step.y2.toDouble()).put("ms", step.duration)
                    is GameAction.Wait -> JSONObject().put("type", "wait").put("ms", step.milliseconds)
                    is GameAction.ClickText -> JSONObject().put("type", "text").put("text", step.text)
                }
                actions.put(s)
            }
            o.put("actions", actions)
            all.put(o)
        }
        context.getSharedPreferences("profiles", 0).edit().putString(KEY, all.toString()).apply()
    }
    private fun parseAction(o: JSONObject): GameAction? = when (o.optString("type")) {
        "tap" -> GameAction.Tap(o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.optLong("ms", 80))
        "swipe" -> GameAction.Swipe(o.getDouble("x1").toFloat(), o.getDouble("y1").toFloat(), o.getDouble("x2").toFloat(), o.getDouble("y2").toFloat(), o.optLong("ms", 450))
        "wait" -> GameAction.Wait(o.getLong("ms"))
        "text" -> GameAction.ClickText(o.getString("text"))
        else -> null
    }
}
