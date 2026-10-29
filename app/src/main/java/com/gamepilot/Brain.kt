package com.gamepilot

import android.content.Context
import android.graphics.Rect
import org.json.JSONObject
import kotlin.math.abs

/** OCR coordinates and coarse live-frame features, all transient in RAM. No screenshot files. */
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
        val textKey = texts.map { it.label.lowercase().trim() }.filter { it.length >= 3 }
            .sorted().take(10).joinToString(",").take(60)
        return "$tileKey:$textKey"
    }
}
object VisionBus {
    @Volatile var latest: LiveFrame? = null
    fun reset() { latest = null }
}

/** Action values are learned separately for every game and observed visual state. */
class Brain(private val context: Context) {
    private val preferences = context.getSharedPreferences("learned_policy_v1", 0)
    private val affirmative = listOf(
        "claim", "collect", "reward", "daily", "mission", "quest", "dispatch",
        "start", "repeat", "auto", "continue", "complete", "receive", "skip",
        "confirm", "accept", "next", "领取", "领取奖励", "日常", "報酬",
        "受取", "一括", "開始", "確認", "次へ", "完了", "受け取る"
    )
    private val blocked = listOf(
        "purchase", "shop", "buy", "top up", "recharge", "checkout",
        "payment", "delete", "unlink", "购买", "充值", "課金", "購入", "削除"
    )
    data class Choice(val action: GameAction, val key: String, val scene: String)
    private fun value(key: String): Double = preferences.getFloat(key, 0f).toDouble()
    private fun encode(p: GameProfile, scene: String, target: String): String =
        "${p.id}:${scene.hashCode()}:${target.lowercase().trim().hashCode()}"
    fun choose(p: GameProfile, frame: LiveFrame): Choice? {
        val scene = frame.sceneKey()
        val targets = frame.texts.mapNotNull { seen ->
            val label = seen.label.lowercase().trim()
            if (blocked.any { label.contains(it) }) return@mapNotNull null
            val matches = affirmative.count { label.contains(it) }
            if (matches == 0) return@mapNotNull null
            val box = seen.bounds
            if (box.isEmpty || frame.width <= 0 || frame.height <= 0) return@mapNotNull null
            val x = box.centerX().toFloat() / frame.width
            val y = box.centerY().toFloat() / frame.height
            if (x !in 0f..1f || y !in 0f..1f) return@mapNotNull null
            val key = encode(p, scene, label)
            Triple(Choice(GameAction.Tap(x, y), key, scene), value(key) + matches, label)
        }
        val scripted = p.actions.filterIsInstance<GameAction.Tap>().mapIndexed { index, action ->
            val key = encode(p, scene, "tap-$index")
            Triple(Choice(action, key, scene), value(key), "tap-$index")
        }
        // Candidate selection is deterministic; negative feedback suppresses nonproductive actions.
        return (targets + scripted).maxWithOrNull(
            compareBy<Triple<Choice, Double, String>> { it.second }
                .thenBy { -it.third.length }
        )?.first
    }
    fun learn(p: GameProfile, chosen: Choice, before: LiveFrame, after: LiveFrame) {
        val delta = if (before.tiles.size == after.tiles.size && before.tiles.isNotEmpty()) {
            before.tiles.indices.sumOf { abs(before.tiles[it] - after.tiles[it]).toDouble() } / before.tiles.size
        } else 0.0
        val oldText = before.texts.map { it.label.lowercase() }.toSet()
        val newText = after.texts.map { it.label.lowercase() }.toSet()
        val usefulChange = delta > 12 || oldText != newText
        // This is a proxy for visual progress, not proof that a daily or farm succeeded.
        val reward = if (usefulChange) 0.6 else -0.8
        val learned = (value(chosen.key) * 0.80 + reward * 0.20).coerceIn(-5.0, 5.0)
        preferences.edit().putFloat(chosen.key, learned.toFloat()).apply()
        Events.record(context, "${p.title}: state changed=$usefulChange, learned value=${"%.2f".format(learned)}")
    }
}
