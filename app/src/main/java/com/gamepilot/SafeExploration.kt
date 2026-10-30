package com.gamepilot

/**
 * Beginner-game exploration: the agent may inspect read-only information
 * panels without inventing mission outcomes or attempting expenditures.
 *
 * These actions are observations, not reward/combat success. A single scene
 * is explored at most once per session; the live controller must confirm the
 * foreground game and retain normal stop/pause gates.
 */
object SafeExploration {
    val names = setOf("help", "guide", "details", "information", "how to play",
        "tutorial", "rules", "description", "game guide", "help center")

    fun readOnly(label: String): Boolean {
        val normalized = GameKnowledge.normalize(label)
        if (GameKnowledge.dangerous(normalized)) return false
        return normalized in names
    }
    data class Hint(val label: String, val x: Float, val y: Float)
    fun select(candidates: List<Hint>, attemptedLabels: Set<String>): Hint? =
        candidates.firstOrNull { hint ->
            readOnly(hint.label) &&
                hint.x in 0f..1f && hint.y in 0f..1f &&
                GameKnowledge.normalize(hint.label) !in attemptedLabels
        }
}
