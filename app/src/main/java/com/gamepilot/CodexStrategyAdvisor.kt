package com.gamepilot

import android.content.Context
import org.json.JSONObject

/**
 * Advisory-only Shoujo Kaisen roster interpretation from Game Codex.
 * A list sorted by experimental individual scores is NOT a validated six-hero
 * formation or a hero grading/purchase instruction.
 */
object CodexStrategyAdvisor {
    data class Candidate(
        val id: Int, val name: String, val rarity: String,
        val score: Double, val confidence: Double?
    )
    private val allowedModes = setOf("pvp_opening", "pvp_sustained", "boss",
        "general_pve", "low_investment", "max_investment")

    fun shortlist(context: Context, gameId: Int, mode: String, limit: Int = 6): List<Candidate> {
        if (mode !in allowedModes || GameCodexBridge.accountFor(gameId) == null) return emptyList()
        val snapshot = GameCodexBridge.latest(context, gameId) ?: return emptyList()
        val entries = snapshot.optJSONArray("owned") ?: return emptyList()
        val items = mutableListOf<Candidate>()
        for (i in 0 until entries.length()) {
            val row = entries.optJSONObject(i) ?: continue
            val profile = row.optJSONObject("experimental_analysis")?.optJSONObject(mode) ?: continue
            if (profile.isNull("score")) continue
            val score = profile.optDouble("score", Double.NaN)
            if (!score.isFinite() || score !in 0.0..100.0) continue
            val conf = if (profile.isNull("confidence")) null
                else profile.optDouble("confidence", Double.NaN).takeIf { it.isFinite() }
            items.add(Candidate(row.optInt("id"), row.optString("name"),
                row.optString("rarity"), score, conf))
        }
        return items.sortedByDescending { it.score }.take(limit.coerceIn(1, 24))
    }

    fun overview(context: Context, gameId: Int): String {
        val pve = shortlist(context, gameId, "general_pve")
        val pvp = shortlist(context, gameId, "pvp_opening")
        fun format(title: String, candidates: List<Candidate>): String =
            title + "\n" + if (candidates.isEmpty()) "No account-scoped analyzed heroes yet"
            else candidates.joinToString("\n") {
                "${it.name} (${it.rarity}), experimental score ${"%.1f".format(it.score)}"
            }
        // Do not turn unsourced formula scores into a ranking or a recommendation.
        return "Existing Codex formula rankings are UNVALIDATED and hidden. " +
            "Meta ranking must come from time-stamped, independently verified " +
            "battle outcomes for this specific game account and mode."
    }
}
