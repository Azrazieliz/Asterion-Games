package com.gamepilot

import android.content.Context
import android.graphics.Rect
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/** Live transient frame descriptors only: no screenshot navigation or stored bitmap history. */
data class SeenText(val label: String, val bounds: Rect)
data class LiveFrame(
    val time: Long,
    val width: Int,
    val height: Int,
    val tiles: IntArray,
    val texts: List<SeenText>
) {
    fun sceneKey(): String {
        val tileKey = tiles.filterIndexed { i, _ -> i % 3 == 0 }
            .joinToString("") { (it / 48).coerceIn(0, 5).toString() }
        val textKey = texts.map { GameKnowledge.normalize(it.label) }
            .filter { it.length >= 3 }.sorted().take(12).joinToString(",").take(90)
        return "$tileKey:$textKey"
    }
    fun textValues(): List<String> = texts.map { GameKnowledge.normalize(it.label) }
}
object VisionBus {
    @Volatile var latest: LiveFrame? = null
    fun reset() { latest = null }
}

/**
 * Per-game contextual bandit with persistent evidence and explicit result verification.
 * Scene changes are small training signals only, never proof of task completion.
 */
class Brain(private val context: Context) {
    data class Choice(
        val action: GameAction,
        val key: String,
        val scene: String,
        val goal: GameGoal,
        val label: String
    )
    data class Feedback(val changed: Boolean, val explicitEvidence: Boolean)

    private data class PendingProof(
        val gameId: Int,
        val goal: GameGoal,
        val actionKey: String,
        val label: String,
        val observedTime: Long
    )
    private val ledger = GoalLedger(context)
    private val journal = EpisodeJournal(context)
    private val graph = ExperienceGraph(context)
    private val prefs = context.getSharedPreferences("mastery_policy_v2", 0)
    private var pending: PendingProof? = null
    private var lastProofScene: String? = null
    private val currentAttempts = mutableMapOf<String, Int>()
    private val totalByScene = mutableMapOf<String, Int>()
    private var lastConfirmedAt = 0L

    fun resetSession() {
        pending = null
        lastProofScene = null
        currentAttempts.clear()
        totalByScene.clear()
    }
    private fun fingerprint(game: Int, goal: GameGoal, scene: String, label: String): String =
        "$game:${goal.id}:${scene.hashCode()}:${label.hashCode()}"
    private fun get(key: String): JSONObject =
        try { JSONObject(prefs.getString(key, "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
    private fun recordedScore(key: String): Double {
        val json = get(key)
        val attempts = json.optInt("attempts", 0)
        val positive = json.optInt("positive", 0)
        val negative = json.optInt("negative", 0)
        val wins = json.optInt("verified", 0)
        val conservative = (positive + wins * 5.0 - negative * 1.5) / (attempts + 3.0)
        return conservative + 0.15 * sqrt(ln(attempts + 2.0) / (attempts + 1.0))
    }
    private fun update(key: String, positive: Boolean, verified: Boolean) {
        val old = get(key)
        val attempts = old.optInt("attempts", 0) + 1
        val successes = old.optInt("positive", 0) + if (positive) 1 else 0
        val failures = old.optInt("negative", 0) + if (positive) 0 else 1
        val wins = old.optInt("verified", 0) + if (verified) 1 else 0
        val updated = JSONObject().put("attempts", attempts)
            .put("positive", successes).put("negative", failures).put("verified", wins)
            .put("updatedAt", System.currentTimeMillis())
        prefs.edit().putString(key, updated.toString()).apply()
    }

    /**
     * Wait for two separate live observations of a contextual result before marking a goal.
     * Pending proof is tied to a recent action and to its specific game and goal.
     */
    fun confirmPending(game: GameProfile, frame: LiveFrame): Boolean {
        val proof = pending ?: return false
        if (game.id != proof.gameId) { pending = null; return false }
        if (frame.time <= proof.observedTime) return true
        val values = frame.textValues()
        val stillPresent = values.any { GameKnowledge.explicitSuccess(proof.goal, it) }
        if (stillPresent && frame.time - proof.observedTime < 10_000L) {
            ledger.verify(game.id, proof.goal, proof.label)
            update(proof.actionKey, positive = true, verified = true)
            graph.verified(game.id, proof.goal, lastProofScene ?: "", proof.actionKey)
            lastConfirmedAt = frame.time
            journal.record(game.id, proof.goal.id, "VERIFIED", frame, proof.label,
                "Two post-action observations match game-result evidence")
            Events.record(context, "${game.title}: VERIFIED ${proof.goal.title}")
        } else {
            journal.record(game.id, proof.goal.id, "UNCONFIRMED", frame, proof.label,
                "Result cue not repeated; no completion credited")
            Events.record(context, "${game.title}: evidence disappeared before confirmation")
        }
        pending = null
        lastProofScene = null
        return false
    }

    fun choose(game: GameProfile, frame: LiveFrame): Choice? {
        if (pending != null) return null
        if (game.id == AttackOnTimeVideoProfile.GAME_ID) {
            val screen = AttackOnTimeVideoProfile.identify(frame.textValues())
            if (AttackOnTimeVideoProfile.mustPause(screen, frame.textValues())) {
                Events.record(context, "${game.title}: paused at guarded screen ${screen.name}")
                return null
            }
        }
        val resourceSensitiveScreen =
            ShoujoKaisenResources.needsStructuredResourceDecision(game.id, frame.textValues())
        if (resourceSensitiveScreen)
            Events.record(context, "${game.title}: structured resource state required before dispatch/shop/extractor")
        if (GameKnowledge.screenUnsafe(frame)) {
            Events.record(context, "${game.title}: transaction/destructive dialogue; automation paused")
            return null
        }
        val goals = GoalSettings.get(context, game.id).filter { it.enabled }
        val pendingGoals = goals.filter {
            val status = ledger.read(game.id, it).status
            status != GoalStatus.VERIFIED && status != GoalStatus.BLOCKED
        }
        if (pendingGoals.isEmpty()) return null
        val scene = frame.sceneKey()
        data class Candidate(val choice: Choice, val score: Double)
        val all = mutableListOf<Candidate>()
        for (goal in pendingGoals) {
            val hints = GameKnowledge.candidates(game, goal)
            val priority = when (goal.type) {
                GoalType.REWARDS -> 0.6
                GoalType.DAILIES -> 0.4
                GoalType.FARMING -> 0.2
            }
            frame.texts.forEach { seen ->
                if (seen.bounds.isEmpty || frame.width < 1 || frame.height < 1) return@forEach
                val word = GameKnowledge.normalize(seen.label)
                if (game.id == 26 &&
                    AttackOnTimeVideoProfile.identify(frame.textValues()) ==
                        AttackOnTimeVideoProfile.Scene.HERO_DRAW) {
                    // Never select the adjacent paid 10-draw button.
                    if (goal.type != GoalType.REWARDS ||
                        !(word.contains("free once") || word.contains("free draw")))
                        return@forEach
                }
                val verifiedFreeLabel = game.id == 26 &&
                    AttackOnTimeVideoProfile.identify(frame.textValues()) ==
                        AttackOnTimeVideoProfile.Scene.HERO_DRAW &&
                    (word.contains("free once") || word.contains("free draw"))
                if (!verifiedFreeLabel && GameKnowledge.dangerous(word)) return@forEach
                if (resourceSensitiveScreen &&
                    !ShoujoKaisenResources.safeUnplannedTarget(word)) return@forEach
                val strength = hints.maxOfOrNull { cue ->
                    when {
                        word == cue -> 3.0
                        word.length >= 3 && word.contains(cue) -> 2.0
                        cue.length >= 4 && cue.contains(word) -> 0.3
                        else -> 0.0
                    }
                } ?: 0.0
                if (strength <= 0) return@forEach
                val x = seen.bounds.centerX().toFloat() / frame.width
                val y = seen.bounds.centerY().toFloat() / frame.height
                if (x !in 0f..1f || y !in 0f..1f) return@forEach
                val label = "ocr:$word"
                val key = fingerprint(game.id, goal, scene, label)
                val attempts = currentAttempts[key] ?: 0
                if (attempts >= 2) return@forEach
                all.add(Candidate(Choice(GameAction.Tap(x, y), key, scene, goal, label),
                    strength + priority + recordedScore(key) - attempts * 2.0))
            }
            // Reuse proven scene-specific trajectories, even for icon-only menus.
            // A cached path must have recorded verified evidence, directly or one step ahead.
            val remembered = graph.recommend(game, goal, scene)
            if (!resourceSensitiveScreen && remembered != null &&
                !GameKnowledge.dangerous(remembered.label) &&
                !(game.id == 26 &&
                  AttackOnTimeVideoProfile.identify(frame.textValues()) ==
                      AttackOnTimeVideoProfile.Scene.HERO_DRAW &&
                  !remembered.label.contains("free once") &&
                  !remembered.label.contains("free draw"))) {
                val attempts = currentAttempts[remembered.actionKey] ?: 0
                if (attempts < 2) {
                    all.add(Candidate(
                        Choice(GameAction.Tap(remembered.x, remembered.y),
                            remembered.actionKey, scene, goal, remembered.label),
                        priority + 4.0 + remembered.verified * 2.0 +
                            remembered.changes.toDouble() / remembered.attempts.coerceAtLeast(1) -
                            attempts * 2.0
                    ))
                }
            }
            // Explicitly configured actions can be learned against the current observed scene.
            // Adaptive replay must not execute unclassified screen-coordinate macros
            // on a screen where free and paid gacha offers coexist.
            val scriptedCandidates = if (resourceSensitiveScreen) emptyList()
                else if (game.id == 26 &&
                AttackOnTimeVideoProfile.identify(frame.textValues()) ==
                    AttackOnTimeVideoProfile.Scene.HERO_DRAW) emptyList()
                else game.actions.filterIsInstance<GameAction.Tap>()
            scriptedCandidates.forEachIndexed { index, tap ->
                val label = "configured:$index"
                val key = fingerprint(game.id, goal, scene, label)
                val attempts = currentAttempts[key] ?: 0
                if (attempts < 2) all.add(Candidate(Choice(tap, key, scene, goal, label),
                    priority + recordedScore(key) - attempts * 2.0))
            }
        }
        return all.maxByOrNull { it.score }?.choice
    }

    fun actionSent(game: GameProfile, choice: Choice) {
        currentAttempts[choice.key] = (currentAttempts[choice.key] ?: 0) + 1
        val sceneCount = (totalByScene[choice.scene] ?: 0) + 1
        totalByScene[choice.scene] = sceneCount
        ledger.attempt(game.id, choice.goal)
        journal.record(game.id, choice.goal.id, "DISPATCHED", null, choice.label,
            "Gesture accepted by Android, result not yet observed")
        Events.record(context, "${game.title}: ${choice.goal.id}, ${choice.label}; scene actions=$sceneCount")
    }

    fun learn(game: GameProfile, choice: Choice, before: LiveFrame, after: LiveFrame): Feedback {
        val delta = if (before.tiles.size == after.tiles.size && before.tiles.isNotEmpty())
            before.tiles.indices.sumOf { abs(before.tiles[it] - after.tiles[it]).toDouble() } / before.tiles.size
            else 0.0
        val previousText = before.textValues().toSet()
        val currentText = after.textValues().toSet()
        val changed = delta > 14.0 || previousText != currentText
        // Never interpret mere navigation, scene change or tap dispatch as a verified result.
        update(choice.key, positive = changed, verified = false)
        graph.observe(game, choice, before, after, changed)
        journal.record(game.id, choice.goal.id, "OBSERVED", after, choice.label,
            if (changed) "Live state changed, not verified completion"
            else "No meaningful state change")
        val evidence = currentText.firstOrNull { label ->
            label !in previousText && GameKnowledge.explicitSuccess(choice.goal, label)
        }
        if (evidence != null) {
            pending = PendingProof(game.id, choice.goal, choice.key, evidence, after.time)
            lastProofScene = choice.scene
            Events.record(context, "${game.title}: candidate goal evidence ${evidence.take(80)}")
        }
        return Feedback(changed, evidence != null)
    }
    fun unresolved(game: GameProfile, reason: String) {
        GoalSettings.get(context, game.id).filter { it.enabled }.forEach { goal ->
            journal.record(game.id, goal.id, "BLOCKED", null, details = reason)
            val old = ledger.read(game.id, goal)
            if (old.status != GoalStatus.VERIFIED)
                ledger.block(game.id, goal, reason)
        }
    }
    fun complete(game: GameProfile): Boolean = GoalSettings.get(context, game.id)
        .filter { it.enabled }.all { ledger.read(game.id, it).status == GoalStatus.VERIFIED }
    fun summary(game: GameProfile): String = ledger.report(game.id)
}
