package com.gamepilot

/**
 * Demonstration-derived screen semantics for Attack on Time (roster slot 26).
 *
 * Recording: Screen_Recording_20261030_131430_Attack on Time.mp4
 * 2026-10-30, 903 seconds, Android 1080x2340.
 *
 * These are UI recognition hints and observed activity transitions, not a
 * replay script. Touch coordinates were not captured as separate input events.
 * No goal is marked completed merely because an observed screen appears.
 */
object AttackOnTimeVideoProfile {
    const val GAME_ID = 26
    const val DURATION_SECONDS = 903

    enum class Scene {
        HOME, LOGIN_REWARDS, HERO_DRAW, SMITHY, VICTORY, DEFEAT,
        ADVERTISEMENT, CONQUEST, QUICK_MODE_REPORT, DISPATCH,
        SPACETIME_ARENA, OUTER_GOD_INVASION, GUILD_STORE, MYSTIC_STORE,
        GALAXY_CLASH, BAG, CHAOS_CLASH, PROVISIONS_TRANSPORT,
        GUILD_QUESTS, WORLD_EXTRACTOR, GARDEN, PONDS, TREASURE_ASCEND,
        UNKNOWN
    }
    data class Observation(
        val startSecond: Int, val endSecond: Int, val scene: Scene,
        val evidence: String, val resultVerified: Boolean = false
    )
    val observations = listOf(
        Observation(0, 35, Scene.HOME, "Launcher, game start and login loading"),
        Observation(40, 70, Scene.LOGIN_REWARDS, "Login Rewards / Sign In; a Rewards dialog was shown"),
        Observation(70, 83, Scene.ADVERTISEMENT, "Third-party racing advertisement, not game UI"),
        Observation(85, 145, Scene.HERO_DRAW, "Hero draw screens, Free Once and draw currencies"),
        Observation(145, 170, Scene.SMITHY, "Smithy, Draw x1/Draw x10"),
        Observation(178, 212, Scene.VICTORY, "Battle Victory and stage rewards; one battle only", true),
        Observation(235, 295, Scene.ADVERTISEMENT, "Third-party playable advertisement and Install button"),
        Observation(295, 310, Scene.CONQUEST, "Conquest chapter/map and Quick Mode Report"),
        Observation(310, 320, Scene.QUICK_MODE_REPORT, "Report marked Claimed", true),
        Observation(315, 355, Scene.DISPATCH, "Dispatch slots/Claim All/Quick Dispatch; quests started"),
        Observation(360, 415, Scene.DEFEAT, "Battle Lose followed by combat/battle report", true),
        Observation(416, 442, Scene.SPACETIME_ARENA, "Spacetime Arena rank 4 and challenge list"),
        Observation(445, 466, Scene.OUTER_GOD_INVASION, "Claim 1/1 and Challenge options"),
        Observation(470, 530, Scene.GUILD_STORE, "Guild, Overlord, Mystic stores and in-game currency purchasing"),
        Observation(535, 560, Scene.BAG, "Hero roster and item inspection"),
        Observation(565, 660, Scene.GALAXY_CLASH, "Galaxy Clash board, Event Quest and Rewards popups"),
        Observation(660, 710, Scene.BAG, "Bag item and shard inventory"),
        Observation(712, 737, Scene.CHAOS_CLASH, "Chaos Clash rank and challenge opponents"),
        Observation(738, 775, Scene.PROVISIONS_TRANSPORT, "Dispatched Heroes panel and guild rankings"),
        Observation(782, 800, Scene.GUILD_QUESTS, "Weekly quest progress; multiple goals incomplete"),
        Observation(801, 826, Scene.WORLD_EXTRACTOR, "The World Extractor allocation dialog"),
        Observation(825, 849, Scene.GARDEN, "Garden planted; planting success observed", true),
        Observation(850, 869, Scene.PONDS, "Ponds / Bertha Pond fishing and confirmation"),
        Observation(870, 883, Scene.TREASURE_ASCEND, "Ascended successfully; progression resource spend", true),
        Observation(889, 903, Scene.HOME, "Return to main home screen")
    )

    private val signatures: List<Pair<Scene, List<String>>> = listOf(
        Scene.LOGIN_REWARDS to listOf("login rewards", "monthly sign-ins", "free make-ups"),
        Scene.HERO_DRAW to listOf("draw(s)", "free once", "hero shards"),
        Scene.SMITHY to listOf("smithy", "genesis dragon shard"),
        Scene.VICTORY to listOf("victory", "stage rewards", "basic bonus"),
        Scene.DEFEAT to listOf("lose", "how to get stronger"),
        Scene.CONQUEST to listOf("conquest", "chapter 48", "universal love"),
        Scene.QUICK_MODE_REPORT to listOf("quick mode report", "drop details"),
        Scene.DISPATCH to listOf("dispatch level", "quick dispatch", "house repairs", "courtyard cleanup"),
        Scene.SPACETIME_ARENA to listOf("spacetime arena", "current rank", "challenge chance"),
        Scene.OUTER_GOD_INVASION to listOf("outer god invasion", "date masamune"),
        Scene.GUILD_STORE to listOf("guild store", "overlord store", "mystic store", "starwish store"),
        Scene.GALAXY_CLASH to listOf("galaxy clash", "steel balls", "raffle tickets"),
        Scene.CHAOS_CLASH to listOf("chaos clash", "winner gets"),
        Scene.PROVISIONS_TRANSPORT to listOf("provisions transport", "dispatched heroes"),
        Scene.GUILD_QUESTS to listOf("quest", "your guild occupies", "carry out 15 dispatch"),
        Scene.WORLD_EXTRACTOR to listOf("world extractor", "select the number of allocations"),
        Scene.GARDEN to listOf("garden", "successfully planted", "plant x1"),
        Scene.PONDS to listOf("ponds", "bertha pond"),
        Scene.TREASURE_ASCEND to listOf("ascended successfully", "all heroes' hp", "required items"),
        Scene.BAG to listOf("shard details", "bag", "mansion"),
        Scene.HOME to listOf("idle play", "conquest", "chests")
    )

    /** Specific strong title markers outrank weak overlaps such as 'Quest' and 'Home'. */
    fun identify(labels: Collection<String>): Scene {
        val normalized = labels.map(GameKnowledge::normalize)
        if (normalized.isEmpty()) return Scene.UNKNOWN
        if (normalized.any { it.contains("install") } && normalized.none { it.contains("the world extractor") }) {
            return Scene.ADVERTISEMENT
        }
        val candidates = signatures.mapNotNull { (scene, cues) ->
            val match = cues.maxOfOrNull { cue ->
                if (normalized.any { it.contains(cue) }) cue.length else 0
            } ?: 0
            if (match >= 5) scene to match else null
        }
        return candidates.maxByOrNull { it.second }?.first ?: Scene.UNKNOWN
    }

    /** Only true when a specific observed outcome is on-screen, not because a menu was opened. */
    fun outcome(scene: Scene, labels: Collection<String>): String? {
        val words = labels.map(GameKnowledge::normalize)
        return when (scene) {
            Scene.VICTORY -> words.firstOrNull { it.contains("victory") }?.let { "Single battle won" }
            Scene.DEFEAT -> words.firstOrNull { it == "lose" || it.contains("you lose") }?.let { "Single battle lost" }
            Scene.QUICK_MODE_REPORT -> words.firstOrNull { it.contains("claimed") }?.let { "Quick Mode rewards claimed" }
            Scene.GARDEN -> words.firstOrNull { it.contains("successfully planted") }?.let { "Garden seeds planted" }
            Scene.TREASURE_ASCEND -> words.firstOrNull { it.contains("ascended successfully") }?.let { "Treasure ascended" }
            else -> null
        }
    }
    /** High-risk or irrelevant overlays cannot be treated as daily task targets. */
    fun mustPause(scene: Scene, words: Collection<String>): Boolean {
        if (scene == Scene.ADVERTISEMENT || scene == Scene.SMITHY ||
            scene == Scene.GUILD_STORE || scene == Scene.TREASURE_ASCEND) return true
        // This screen may contain paid draws; only the labelled free button is eligible.
        if (scene == Scene.HERO_DRAW && words.none {
                val value = GameKnowledge.normalize(it)
                value.contains("free once") || value.contains("free draw")
            }) return true
        val labels = words.map(GameKnowledge::normalize)
        return labels.any {
            it.contains("install") || it.contains("real money") ||
            it.contains("purchase") || it.contains("recharge") || it.contains("top up")
        }
    }
}
