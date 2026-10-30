package com.gamepilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

/**
 * Evidence-only evolving team meta, isolated by game/account/mode.
 * Old Codex formula scores must NEVER be presented as observed game meta.
 * Individual character contributions cannot be inferred from team wins alone.
 */
object MetaEvidence {
    data class Battle(
        val gameId: Int, val accountId: String, val mode: String,
        val teamIds: List<Int>, val victory: Boolean,
        val observedAt: Long, val verifiedSource: String
    )
    data class TeamEstimate(val teamIds: List<Int>, val wins: Int, val battles: Int,
                            val conservativeRate: Double, val lastSeen: Long)
    private const val MIN_BATTLES = 12
    private val modes = setOf("pvp", "pve", "boss", "guild_war", "cross_server")
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("meta_evidence_v1", 0)
    private fun key(b: Battle) = "${b.gameId}:${b.accountId}:${b.mode}"

    @Synchronized fun recordVerified(ctx: Context, b: Battle): Boolean {
        if (b.gameId <= 0 || b.accountId.isBlank() || b.mode !in modes ||
            b.teamIds.isEmpty() || b.teamIds.size > 10 ||
            b.teamIds.distinct().size != b.teamIds.size || b.teamIds.any { it <= 0 } ||
            b.observedAt <= 0 || b.verifiedSource !in
                setOf("live_battle_result", "verified_official_record")) return false
        val all = prefs(ctx)
        val storeKey = key(b)
        val records = try { JSONArray(all.getString(storeKey, "[]")) }
                      catch (_: Exception) { JSONArray() }
        val sorted = b.teamIds.sorted()
        records.put(JSONObject().put("team", JSONArray(sorted))
            .put("won", b.victory).put("time", b.observedAt)
            .put("source", b.verifiedSource))
        val trimmed = JSONArray()
        for (i in (records.length()-2000).coerceAtLeast(0) until records.length())
            trimmed.put(records.get(i))
        return all.edit().putString(storeKey, trimmed.toString()).commit()
    }
    private fun wilsonLower(wins: Int, n: Int): Double {
        if (n == 0) return 0.0
        val z = 1.96
        val p = wins.toDouble() / n
        val z2 = z*z
        return ((p + z2/(2*n) -
            z*sqrt((p*(1-p)+z2/(4*n))/n)) / (1+z2/n)).coerceIn(0.0,1.0)
    }
    fun rankedTeams(ctx: Context, gameId: Int, accountId: String, mode: String): List<TeamEstimate> {
        if (accountId.isBlank() || mode !in modes) return emptyList()
        val raw = prefs(ctx).getString("$gameId:$accountId:$mode", "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { return emptyList() }
        data class Sample(val ids: List<Int>, val victory: Boolean, val time: Long)
        val samples = (0 until arr.length()).mapNotNull { i ->
            val row = arr.optJSONObject(i) ?: return@mapNotNull null
            val team = row.optJSONArray("team") ?: return@mapNotNull null
            val ids = (0 until team.length()).map { team.optInt(it) }.sorted()
            if (ids.isEmpty() || ids.any { it <= 0 }) null else Sample(ids,
                row.optBoolean("won"), row.optLong("time"))
        }
        return samples.groupBy { it.ids }.mapNotNull { (ids, matches) ->
            if (matches.size < MIN_BATTLES) null else {
                val wins = matches.count { it.victory }
                TeamEstimate(ids,wins,matches.size,wilsonLower(wins,matches.size),
                    matches.maxOf { it.time })
            }
        }.sortedWith(compareByDescending<TeamEstimate> { it.conservativeRate }
            .thenByDescending { it.battles })
    }
    fun status(ctx: Context, game: GameProfile): String {
        val id = GameAccounts.current(ctx,game).playerId
            ?: return "Account ID not verified; no meta ranking can be personalized yet."
        val pvp = rankedTeams(ctx,game.id,id,"pvp")
        return if (pvp.isEmpty())
            "No validated meta ranking. Requires at least $MIN_BATTLES verified results per team."
        else pvp.take(5).joinToString("\n") {
            "Team ${it.teamIds.joinToString("/")} • ${it.wins}/${it.battles} wins • lower confidence ${(it.conservativeRate*100).toInt()}%"
        }
    }
}
