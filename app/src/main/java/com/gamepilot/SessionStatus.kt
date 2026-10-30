package com.gamepilot

import android.content.Context

/** Minimal, truthful local status for one real running or paused game session. */
object SessionStatus {
    enum class Stage { IDLE, RUNNING, PAUSED, VERIFIED, BLOCKED }
    data class Snapshot(val gameId: Int, val name: String, val stage: Stage,
                        val action: String, val at: Long)
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("pilot_active_status_v1",0)
    @Synchronized fun set(ctx: Context, gameId: Int, name: String,
                          stage: Stage, action: String = "") {
        prefs(ctx).edit()
            .putInt("gameId",gameId).putString("title",name.take(120))
            .putString("stage",stage.name).putString("action",action.take(200))
            .putLong("at",System.currentTimeMillis()).commit()
    }
    fun current(ctx: Context): Snapshot {
        val p = prefs(ctx)
        return Snapshot(p.getInt("gameId",-1),p.getString("title","")?:"",
            Stage.entries.firstOrNull { it.name == p.getString("stage","IDLE") }
                ?: Stage.IDLE,
            p.getString("action","")?:"",p.getLong("at",0))
    }
    fun description(ctx: Context): String {
        val s=current(ctx)
        val age=if(s.at>0)(System.currentTimeMillis()-s.at).coerceAtLeast(0)/60000 else 0
        return when(s.stage){
            Stage.IDLE -> "Idle — no running game"
            Stage.RUNNING -> "${s.name} — running\nAction: ${s.action.ifBlank { "Reading live game state" }}\nUpdated $age min ago"
            Stage.PAUSED -> "${s.name} — paused; task retained"
            Stage.VERIFIED -> "${s.name} — session ended with verified objectives"
            Stage.BLOCKED -> "${s.name} — blocked: ${s.action}"
        }
    }
}
