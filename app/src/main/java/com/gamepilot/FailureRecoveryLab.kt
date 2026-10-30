package com.gamepilot

import android.content.Context
import org.json.JSONObject

/**
 * Bounded, source-preserving failure escalation. Repeated identical failures
 * stop further unattended attempts instead of retrying a broken action forever.
 * The user can inspect the cause and explicitly clear a hold.
 */
object FailureRecoveryLab {
    private const val STORE = "recovery_lab_v1"
    const val MAX_REPEATS = 3
    private const val WINDOW_MS = 24L * 60 * 60 * 1000
    data class Case(val gameId: Int, val cause: String, val occurrences: Int,
                    val firstAt: Long, val latestAt: Long, val scene: String = "") {
        val requiresReview: Boolean get() = occurrences >= MAX_REPEATS
    }
    fun next(previous: Case?, gameId: Int, cause: String,
             scene: String, time: Long): Case {
        require(gameId > 0 && time > 0)
        val reason = cause.take(160)
        return if (previous != null && previous.gameId == gameId &&
            previous.cause == reason &&
            time >= previous.latestAt && time - previous.latestAt <= WINDOW_MS)
            previous.copy(occurrences = (previous.occurrences + 1).coerceAtMost(999),
                latestAt = time, scene = scene.take(180))
        else Case(gameId, reason, 1, time, time, scene.take(180))
    }
    private fun pref(ctx: Context) = ctx.getSharedPreferences(STORE, 0)
    fun read(ctx: Context, gameId: Int): Case? {
        val raw = pref(ctx).getString(gameId.toString(), null) ?: return null
        return try {
            val o = JSONObject(raw)
            Case(gameId, o.getString("cause"), o.getInt("count"),
                o.getLong("first"), o.getLong("latest"), o.optString("scene"))
        } catch (_: Exception) { null }
    }
    @Synchronized fun failure(ctx: Context, gameId: Int, cause: String,
                              scene: String = ""): Case {
        val item = next(read(ctx, gameId), gameId, cause, scene,
            System.currentTimeMillis())
        val json = JSONObject().put("cause", item.cause)
            .put("count", item.occurrences).put("first", item.firstAt)
            .put("latest", item.latestAt).put("scene", item.scene)
        pref(ctx).edit().putString(gameId.toString(), json.toString()).commit()
        if (item.requiresReview) Events.record(ctx,
            "GAME $gameId HELD: ${item.cause}, repeated ${item.occurrences}x; user review required")
        return item
    }
    fun held(ctx: Context, gameId: Int): Boolean =
        read(ctx, gameId)?.requiresReview == true

    @Synchronized fun clear(ctx: Context, gameId: Int) {
        pref(ctx).edit().remove(gameId.toString()).commit()
    }
    fun description(ctx: Context, gameId: Int): String {
        val case = read(ctx, gameId) ?: return "No recorded repeated failure"
        return if (case.requiresReview)
            "REVIEW REQUIRED after ${case.occurrences} failures: ${case.cause}\nScene: ${case.scene}"
        else "${case.occurrences}/$MAX_REPEATS failures: ${case.cause}"
    }
}
