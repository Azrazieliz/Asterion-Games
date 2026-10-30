package com.gamepilot

/**
 * Seed playbooks for recognized games. These are task/navigation hypotheses,
 * NOT saved success records, solved tactics or verified account state.
 *
 * The agent must observe real game states before it can learn successful routes.
 * Unidentified/truncated launchers receive no speculative game-specific facts.
 */
data class GameRoutine(
    val title: String,
    val goal: GoalType,
    val navigationCues: List<String>,
    val reference: String = ""
)

object GamePlaybooks {
    private val HSR = "https://hsr.keqingmains.com/misc/beginner-guide/"
    private val NIKKE = "https://nikkehub.com/en/guide/newbie/daily-contents"
    private val AK = "https://arknights.wiki.gg/wiki/Mission/Daily"
    private fun r(name: String, type: GoalType, vararg cues: String, source: String = "") =
        GameRoutine(name, type, cues.toList(), source)

    private val known: Map<Int, List<GameRoutine>> = mapOf(
        3 to listOf(
            r("Collect outpost resources", GoalType.REWARDS,
                "Outpost", "Defense", "Claim", "前哨基地", "報酬", source = NIKKE),
            r("Dispatch and collect expeditions", GoalType.DAILIES,
                "Dispatch", "Outpost", "派遣", "回収", source = NIKKE),
            r("Simulation Room", GoalType.FARMING,
                "Ark", "Simulation Room", "Simulation", "シミュレーション", source = NIKKE),
            r("Interception", GoalType.FARMING,
                "Ark", "Interception", "迎撃戦", "迎击战", source = NIKKE),
            r("Tower activities", GoalType.DAILIES,
                "Tower", "Tribe Tower", "タワー", source = NIKKE),
            r("Daily Missions", GoalType.DAILIES,
                "Daily Missions", "Missions", "デイリーミッション", source = NIKKE)
        ),
        6 to listOf(
            r("Daily Training", GoalType.DAILIES,
                "Daily Training", "Activity", "日課訓練", "每日实训", source = HSR),
            r("Assignments", GoalType.DAILIES,
                "Assignments", "Dispatch", "派遣", "委托", source = HSR),
            r("Spend Trailblaze Power", GoalType.FARMING,
                "Trailblaze Power", "Calyx", "Cavern", "侵蝕トンネル", "拟造花萼",
                source = HSR),
            r("Collect training rewards", GoalType.REWARDS,
                "Daily Training", "Claim", "领取", source = HSR)
        ),
        8 to listOf(
            r("Daily Missions", GoalType.DAILIES,
                "Daily", "Missions", "デイリー", "每日任务", source = AK),
            r("Factory products and trading orders", GoalType.REWARDS,
                "Base", "Factory", "Trading Post", "基建", "製造所", "贸易站", source = AK),
            r("Recruitment", GoalType.DAILIES,
                "Recruitment", "公開求人", "公开招募", source = AK),
            r("Spend Sanity with auto-deploy", GoalType.FARMING,
                "Sanity", "Auto Deploy", "代理指揮", "理性", "代理指挥", source = AK),
            r("Credit Store", GoalType.REWARDS,
                "Credit Store", "FP", "購買部", "信用交易所", source = AK)
        ),
        11 to listOf(
            r("Commissions", GoalType.DAILIES,
                "Commissions", "Commission", "委託", "委托"),
            r("Dormitory resources", GoalType.REWARDS,
                "Dorm", "Dormitory", "寮舎", "宿舍"),
            r("Daily combat", GoalType.FARMING,
                "Battle", "Sortie", "出撃", "出击")
        ),
        12 to listOf(
            r("Daily Activity", GoalType.DAILIES,
                "Daily", "Guidebook", "Activity", "活跃度"),
            r("Waveplate farming", GoalType.FARMING,
                "Waveplate", "Forgery", "Tacet Field", "結晶波長板", "体力")
        ),
        13 to listOf(
            r("Activity rewards", GoalType.DAILIES,
                "Activity", "Daily", "活性", "任務"),
            r("Resource farming", GoalType.FARMING,
                "Pneuma", "Resource", "素材")
        ),
        19 to listOf(
            r("Daily missions", GoalType.DAILIES,
                "Missions", "Daily", "デイリー"),
            r("Auto Duel", GoalType.FARMING,
                "Auto-Duel", "Auto Duel", "オートデュエル")
        ),
        20 to listOf(
            r("Daily missions", GoalType.DAILIES,
                "Daily", "Missions", "デイリー"),
            r("Training and event quests", GoalType.FARMING,
                "Quest", "Training", "Event", "冒険", "育成")
        ),
        24 to listOf(
            r("Daily quest farming", GoalType.FARMING,
                "Daily Quests", "Ember Gathering", "QP", "曜日クエスト", "周回"),
            r("Master missions", GoalType.DAILIES,
                "Master Mission", "マスターミッション", "ミッション")
        ),
        // Screen names and activities verified from the 2026-10-30 ADB recording.
        // No taps or completed objectives are invented from the recording.
        26 to listOf(
            r("Monthly Login Rewards", GoalType.REWARDS, "Login Rewards", "Sign In",
                "Monthly Sign-ins", "Rewards"),
            r("Conquest Quick Mode", GoalType.REWARDS,
                "Conquest", "Quick Mode Report", "Claimed", "Quick"),
            r("Dispatch missions", GoalType.DAILIES,
                "Dispatch", "Quick Dispatch", "Claim All", "House Repairs",
                "Courtyard Cleanup", "Forge Weapons"),
            r("Spacetime Arena", GoalType.DAILIES,
                "Spacetime Arena", "Current Rank", "Challenge Chance", "Challenge"),
            r("Outer God Invasion", GoalType.DAILIES,
                "Outer God Invasion", "Claim 1/1", "Challenge"),
            r("Galaxy Clash event", GoalType.DAILIES,
                "Galaxy Clash", "Event Quest", "Rewards", "Raffle Tickets"),
            r("Chaos Clash", GoalType.DAILIES,
                "Chaos Clash", "Challenge Chance", "Winner gets"),
            r("Guild Provisions Transport", GoalType.DAILIES,
                "Provisions Transport", "Dispatched Heroes", "Transport"),
            r("Guild weekly quests", GoalType.DAILIES,
                "Quest", "Your Guild Occupies", "Carry out 15 dispatch"),
            r("The World Extractor", GoalType.REWARDS,
                "The World Extractor", "Allocations", "Output Details"),
            r("Garden planting", GoalType.REWARDS,
                "Garden", "Successfully Planted", "Plant x1"),
            r("Ponds fishing", GoalType.REWARDS,
                "Ponds", "Bertha Pond", "Confirm"),
            r("Conquest battles", GoalType.FARMING,
                "Conquest", "Challenge", "Victory", "Stage Rewards")
        )
    )

    fun forGame(gameId: Int): List<GameRoutine> = known[gameId].orEmpty()
    fun cues(gameId: Int, goal: GoalType): List<String> =
        forGame(gameId).filter { it.goal == goal }
            .flatMap { it.navigationCues }.map(GameKnowledge::normalize).distinct()
    fun overview(gameId: Int): String {
        val routines = forGame(gameId)
        return if (routines.isEmpty())
            "No game-specific routines validated or researched yet. " +
                "The app can still learn from direct live gameplay."
        else routines.joinToString("\n") {
            "• ${it.title} [${it.goal}]\n  ${it.navigationCues.take(5).joinToString(", ")}" +
                if (it.reference.isBlank()) "" else "\n  Reference: ${it.reference}"
        } + "\n\nThese are navigation seeds, not completed game mastery."
    }
}
