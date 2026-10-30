package com.gamepilot

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/**
 * Read-only bridge to the separately-installed Game Codex application.
 * Account-specific snapshots are never merged or promoted to verified inventory.
 */
object GameCodexBridge {
    const val AUTHORITY = "net.gamecodex.app.pilot"
    const val SCHEMA = 1
    const val MAX_BYTES = 450_000
    const val MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

    enum class SyncState {
        LINKED, NO_DATA, STALE, CODEX_NOT_INSTALLED, BAD_SCHEMA, READ_FAILED, NOT_SHOUJO
    }
    data class SyncResult(
        val state: SyncState,
        val account: String = "",
        val ownedCount: Int = 0,
        val generatedAt: Long = 0L,
        val note: String = ""
    )

    fun accountFor(gameId: Int): String? = when (gameId) {
        26 -> "global"
        22 -> "jp"
        else -> null
    }
    fun isReadyToGrade(result: SyncResult): Boolean = false

    private fun prefs(context: Context) =
        context.getSharedPreferences("pilot_codex_link_v1", 0)

    fun latest(context: Context, gameId: Int): JSONObject? {
        val account = accountFor(gameId) ?: return null
        return try {
            val raw = prefs(context).getString("snapshot:$account", null) ?: return null
            val doc = JSONObject(raw)
            if (doc.optString("account") != account ||
                doc.optString("game_key") != "shoujo_kaisen" ||
                doc.optInt("schema_version") != SCHEMA) null
            else doc
        } catch (_: Exception) { null }
    }

    /** Performs query IPC; safe to run from UI or session start on a bounded snapshot. */
    fun refresh(context: Context, gameId: Int): SyncResult {
        val account = accountFor(gameId) ?: return SyncResult(SyncState.NOT_SHOUJO)
        val packageVisible = try {
            context.packageManager.getPackageInfo("net.gamecodex.app", 0)
            true
        } catch (_: Exception) { false }
        if (!packageVisible) return SyncResult(SyncState.CODEX_NOT_INSTALLED, account)
        return try {
            val uri = Uri.parse("content://$AUTHORITY/snapshot/$account")
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst())
                    return SyncResult(SyncState.NO_DATA, account, note = "Open Codex and select account scope to publish roster")
                val idx = cursor.getColumnIndexOrThrow("payload_json")
                val raw = cursor.getString(idx) ?: return SyncResult(SyncState.NO_DATA, account)
                if (raw.length > MAX_BYTES) return SyncResult(SyncState.BAD_SCHEMA, account)
                val json = JSONObject(raw)
                if (json.optInt("schema_version") != SCHEMA ||
                    json.optString("game_key") != "shoujo_kaisen" ||
                    json.optString("account") != account ||
                    json.optBoolean("verified_game_inventory", true) ||
                    json.optBoolean("grading_authorized", true))
                    return SyncResult(SyncState.BAD_SCHEMA, account)
                val records = json.optJSONArray("owned") ?: return SyncResult(SyncState.BAD_SCHEMA, account)
                if (records.length() > 1000) return SyncResult(SyncState.BAD_SCHEMA, account)
                val time = json.optLong("generated_at", 0L)
                val age = System.currentTimeMillis() - time
                if (time <= 0 || age < -300_000 || age > MAX_AGE_MS)
                    return SyncResult(SyncState.STALE, account, records.length(), time)
                prefs(context).edit().putString("snapshot:$account", raw).apply()
                return SyncResult(SyncState.LINKED, account, records.length(), time,
                    "Experimental Codex character data only; grading still requires account evidence")
            } ?: SyncResult(SyncState.NO_DATA, account)
        } catch (error: Exception) {
            SyncResult(SyncState.READ_FAILED, account, note = error.javaClass.simpleName)
        }
    }

    fun status(context: Context, gameId: Int): String {
        val account = accountFor(gameId) ?: return "This title has no Codex adapter yet"
        val snapshot = latest(context, gameId) ?: return "$account: no synchronized roster available"
        val entries = snapshot.optJSONArray("owned")?.length() ?: 0
        val age = System.currentTimeMillis() - snapshot.optLong("generated_at", 0L)
        return "$account: $entries Codex-owned entries, snapshot age ${age.coerceAtLeast(0) / 60000} min. " +
            "Character ranking is experimental; inventory/grades not verified."
    }
}
