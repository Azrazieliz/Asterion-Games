package com.gamepilot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Measured, account-scoped experiments. A policy change is never promoted
 * merely because an animation/scene changed. Compare two routines only when
 * there is enough equivalent *verified* goal evidence.
 *
 * No gold valuation or fabricated rewards: this tracks objective completion,
 * time and actual gesture count until resource deltas can be read reliably.
 */
object StrategyExperiment {
    data class Run(
        val gameId: Int, val accountKey: String, val variant: String,
        val objectiveSignature: String, val startedAt: Long, val endedAt: Long,
        val verifiedGoals: Int, val targetGoals: Int, val gestureCount: Int
    ) {
        val complete: Boolean get() = targetGoals > 0 && verifiedGoals >= targetGoals
        val durationMs: Long get() = endedAt - startedAt
    }
    data class Estimate(
        val variant: String, val trials: Int, val completeRuns: Int,
        val completionRate: Double, val meanDurationMs: Long,
        val meanGestures: Double
    )
    enum class Verdict { UNPROVEN, KEEP_CURRENT, BETTER_SUPPORTED, REGRESSION }
    private const val STORE = "strategy_experiments_v1"
    private const val MAX_HISTORY = 120
    private const val MIN_TRIALS = 5

    fun valid(r: Run): Boolean =
        r.gameId > 0 && r.accountKey.isNotBlank() && r.accountKey.length <= 80 &&
        r.variant in setOf("adaptive", "scripted") && r.objectiveSignature.isNotBlank() &&
        r.objectiveSignature.length <= 180 &&
        r.startedAt > 0 && r.endedAt >= r.startedAt &&
        r.durationMs <= 12L * 60 * 60 * 1000 &&
        r.targetGoals in 1..100 && r.verifiedGoals in 0..r.targetGoals &&
        r.gestureCount in 0..50000

    private fun key(r: Run) = "${r.gameId}:${r.accountKey}:${r.objectiveSignature}"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(STORE, 0)

    @Synchronized fun record(ctx: Context, run: Run): Boolean {
        if (!valid(run)) return false
        val p = prefs(ctx)
        val k = key(run)
        val history = try { JSONArray(p.getString(k, "[]")) }
                      catch (_: Exception) { JSONArray() }
        history.put(JSONObject()
            .put("variant", run.variant).put("start", run.startedAt)
            .put("end", run.endedAt).put("verified", run.verifiedGoals)
            .put("target", run.targetGoals).put("gestures", run.gestureCount))
        val bounded = JSONArray()
        for (i in (history.length() - MAX_HISTORY).coerceAtLeast(0) until history.length())
            bounded.put(history.getJSONObject(i))
        return p.edit().putString(k, bounded.toString()).commit()
    }
    fun summarize(runs: List<Run>, variant: String): Estimate? {
        val matching = runs.filter { it.variant == variant && valid(it) }
        if (matching.isEmpty()) return null
        val victories = matching.count { it.complete }
        return Estimate(variant, matching.size, victories,
            victories.toDouble()/matching.size,
            matching.map { it.durationMs }.average().toLong(),
            matching.map { it.gestureCount }.average())
    }

    /**
     * Only promote after >=5 equivalent runs for BOTH strategies, no weaker
     * verified completion rate and at least 15% faster completion. Failure
     * increase >10 percentage points signals regression, even if faster.
     */
    fun compare(current: Estimate?, candidate: Estimate?): Verdict {
        if (current == null || candidate == null ||
            current.trials < MIN_TRIALS || candidate.trials < MIN_TRIALS)
            return Verdict.UNPROVEN
        if (candidate.completionRate + 0.10 < current.completionRate)
            return Verdict.REGRESSION
        if (candidate.completionRate < current.completionRate)
            return Verdict.KEEP_CURRENT
        if (current.meanDurationMs <= 0L || candidate.meanDurationMs <= 0L)
            return Verdict.UNPROVEN
        return if (candidate.meanDurationMs <= current.meanDurationMs * 0.85)
            Verdict.BETTER_SUPPORTED else Verdict.KEEP_CURRENT
    }
    fun report(ctx: Context, game: GameProfile): String {
        val account = GameAccounts.current(ctx, game).playerId ?: "local-unverified:${game.id}"
        val goals = GoalSettings.get(ctx, game.id).filter { it.enabled }
        val signature = goals.joinToString("|") { "${it.id}:${it.targetRuns}" }
        val key = "${game.id}:$account:$signature"
        val raw = prefs(ctx).getString(key, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val entries = (0 until arr.length()).mapNotNull { i ->
            val j = arr.optJSONObject(i) ?: return@mapNotNull null
            Run(game.id, account, j.optString("variant"), signature,
                j.optLong("start"), j.optLong("end"),
                j.optInt("verified"), j.optInt("target"), j.optInt("gestures"))
        }
        val adaptive = summarize(entries, "adaptive")
        val scripted = summarize(entries, "scripted")
        if (adaptive == null && scripted == null)
            return "No measured sessions for these goals. No strategy is declared superior."
        fun detail(e: Estimate?) = e?.let {
            "${it.variant}: ${it.completeRuns}/${it.trials} completed; " +
                "mean ${it.meanDurationMs / 60000} min, ${it.meanGestures.toInt()} actions"
        } ?: "No runs"
        return detail(adaptive) + "\n" + detail(scripted) +
            "\n\nComparison: ${compare(scripted, adaptive)} (evidence only; your selected strategy is unchanged)."
    }
}
