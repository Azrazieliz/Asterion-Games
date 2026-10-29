package com.gamepilot

import java.util.Locale

/**
 * A growing game-separated knowledge seed, NOT a claim that every game's
 * specific battle/navigation UI has already been mastered.
 * Cues are localized to English, Japanese and Simplified Chinese.
 */
object GameKnowledge {
    private val collect = listOf(
        "claim", "collect", "receive", "get reward", "claim all", "take all",
        "報酬受取", "一括受取", "受け取る", "受取", "獲得", "受領",
        "领取", "领取奖励", "一键领取", "收取", "获得", "奖励"
    )
    private val daily = listOf(
        "daily", "mission", "quest", "task", "activity", "dispatch", "expedition",
        "デイリー", "任務", "ミッション", "日課", "依頼", "派遣",
        "每日", "日常", "任务", "委托", "派遣", "活跃"
    )
    private val farm = listOf(
        "battle", "challenge", "start", "auto", "repeat", "retry", "deploy",
        "出撃", "戦闘", "挑戦", "周回", "再戦", "自動", "オート", "開始",
        "战斗", "挑战", "扫荡", "自动", "开始", "再次", "重复", "出击"
    )
    private val earned = listOf(
        "rewards claimed", "reward received", "claimed", "obtained", "received",
        "報酬獲得", "受取完了", "受取済", "獲得しました", "獲得",
        "领取成功", "已领取", "领取完成", "获得奖励"
    )
    private val dailyDone = listOf(
        "daily completed", "mission completed", "task completed", "all daily",
        "ミッション達成", "任務完了", "クリア済", "完了済",
        "每日任务完成", "任务完成", "全部完成"
    )
    private val farmDone = listOf(
        "victory", "battle results", "clear!", "stage clear", "mission clear",
        "勝利", "戦闘結果", "クリア", "作戦成功",
        "胜利", "战斗结算", "挑战成功", "通关", "结算"
    )
    private val critical = listOf(
        "purchase", "buy", "checkout", "top up", "recharge", "payment", "subscribe",
        "confirm purchase", "gacha", "summon", "recruit", "draw", "roll",
        "account", "unlink", "delete", "remove account", "reset data",
        "課金", "購入", "決済", "召喚", "ガチャ", "募集", "削除", "退会",
        "充值", "购买", "支付", "抽卡", "招募", "召唤", "删除", "注销"
    )
    private val selectedGames = mapOf(
        1 to listOf("expeditions", "material", "素材", "遠征"),
        3 to listOf("outpost", "dispatch", "前哨基地", "派遣"),
        6 to listOf("daily training", "assignments", "trailblaze power", "开拓力"),
        8 to listOf("base", "auto deploy", "annihilation", "基建", "剿滅", "代理指揮"),
        11 to listOf("commissions", "dorm", "commission", "委託", "宿舎"),
        12 to listOf("daily activity", "waveplate", "デイリー"),
        13 to listOf("activity", "pneuma", "活性"),
        19 to listOf("auto duel", "オートデュエル"),
        20 to listOf("quest", "event", "冒険", "イベント"),
        23 to listOf("daily", "auto", "デイリー"),
        24 to listOf("chaldea", "daily quests", "周回", "曜日クエスト")
    )
    fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ").trim()
    fun dangerous(text: String): Boolean {
        val value = normalize(text)
        return critical.any { value.contains(normalize(it)) }
    }
    fun candidates(game: GameProfile, goal: GameGoal): List<String> {
        val base = when (goal.type) {
            GoalType.REWARDS -> collect + listOf("reward", "mailbox", "inbox", "gift", "プレゼント", "メール", "邮件")
            GoalType.DAILIES -> daily + collect
            GoalType.FARMING -> farm + (selectedGames[game.id] ?: emptyList())
        }
        return (base + goal.customCues).map(::normalize).distinct()
    }
    fun explicitSuccess(goal: GameGoal, label: String): Boolean {
        val cues = when (goal.type) {
            GoalType.REWARDS -> earned
            GoalType.DAILIES -> dailyDone
            GoalType.FARMING -> farmDone
        }
        val text = normalize(label)
        return cues.any { text.contains(normalize(it)) }
    }
    /** Candidates are never generated from a transaction or destructive screen. */
    fun screenUnsafe(frame: LiveFrame): Boolean {
        return frame.texts.any { dangerous(it.label) &&
            (it.label.contains("confirm", true) || it.label.contains("purchase", true) ||
            it.label.contains("支付") || it.label.contains("購入") || it.label.contains("课金")) }
    }
}
