package com.gamepilot

import android.content.Context
import org.json.JSONObject

/**
 * Explicit, separate game-account identity. Links only from independently
 * repeated UID/player-ID text on a live game screen; not from guessed login
 * names, other game profiles or a Codex catalogue row.
 */
object GameAccounts {
    data class Account(val gameId: Int, val packageName: String, val region: String,
                       val playerId: String?, val confirmedAt: Long, val evidence: String)
    private val pattern = Regex(
        "(?i)(?:player\\s*id|account\\s*id|user\\s*id|uid|ユーザー\\s*id|玩家\\s*id)\\s*[:：#]?\\s*([A-Za-z0-9_-]{5,32})")
    private val candidates = mutableMapOf<Int, Pair<String, Long>>()

    fun region(gameId: Int): String = when(gameId) {
        22 -> "JP"
        26 -> "Global"
        else -> "Unknown"
    }
    private fun prefs(context: Context) =
        context.getSharedPreferences("game_account_links_v1", Context.MODE_PRIVATE)

    fun current(context: Context, game: GameProfile): Account {
        val json = try {
            JSONObject(prefs(context).getString(game.id.toString(), "{}") ?: "{}")
        } catch (_: Exception) { JSONObject() }
        val matchesPackage = json.optString("package") == game.packageName
        val uid = if (matchesPackage) json.optString("player_id").ifBlank { null } else null
        return Account(game.id, game.packageName, region(game.id), uid,
            if (uid == null) 0L else json.optLong("confirmed_at"),
            if (uid == null) "" else json.optString("evidence"))
    }
    /**
     * Only two observations of the SAME explicitly labeled identifier can
     * link an account. No passwords, auth tokens or cookies are read.
     */
    @Synchronized fun observe(context: Context, game: GameProfile, frame: LiveFrame) {
        if (game.packageName.isBlank()) return
        // Labels and values may be adjacent separate OCR/accessibility nodes.
        val visible = frame.texts.map { it.label.trim() }.filter { it.isNotEmpty() }
        val joined = visible.joinToString(" ")
        val detected = (
            visible.mapNotNull { pattern.find(it)?.groupValues?.getOrNull(1) } +
            listOfNotNull(pattern.find(joined)?.groupValues?.getOrNull(1))
        ).distinct()
        if (detected.size != 1) return
        val uid = detected.single()
        val prior = candidates[game.id]
        if (prior?.first == uid && frame.time - prior.second >= 900L) {
            val previous = current(context, game)
            if (previous.playerId == uid) return
            val doc = JSONObject().put("package", game.packageName)
                .put("player_id", uid).put("confirmed_at", frame.time)
                .put("region", region(game.id)).put("evidence", "repeated_live_uid")
            if (prefs(context).edit().putString(game.id.toString(), doc.toString()).commit())
                Events.record(context, "Verified account link for ${game.title}: labeled UID observed twice")
        } else if (prior?.first != uid || frame.time - prior.second >= 900L) candidates[game.id] = uid to frame.time
    }
}
