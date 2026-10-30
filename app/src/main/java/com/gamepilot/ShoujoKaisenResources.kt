package com.gamepilot

/**
 * Shoujo Kaisen resource decisions shared by global (game 26) and JP (game 22),
 * but NEVER shares account inventory, daily progress or unverified regional mechanics.
 * The caller must provide values verified in the current game UI or its trusted codex.
 */
object ShoujoKaisenResources {
    const val GLOBAL_GAME_ID = 26
    const val JAPANESE_GAME_ID = 22
    const val FREE_DISPATCH_REFRESHES = 3
    const val OPTIONAL_PAID_REFRESHES = 2
    const val PAID_REFRESH_GOLD = 50L
    /**
     * Provisional conservative operational interpretation of "way more than 50":
     * at least 5x gross gold (250), never 51. This is NOT the user's chosen exact
     * multiple. Unknown future reroll rewards NEVER count as guaranteed.
     */
    const val PAID_REFRESH_MIN_VERIFIED_GOLD_RETURN = 250L

    enum class Account { GLOBAL, JAPAN }
    fun account(gameId: Int): Account? = when (gameId) {
        GLOBAL_GAME_ID -> Account.GLOBAL
        JAPANESE_GAME_ID -> Account.JAPAN
        else -> null
    }

    enum class Currency {
        GOLD, RED_STONE_SHARDS, BLUE_STONE_SHARDS, KARMA_STONES,
        STARS, DIAMOND_CHEST, SHOP_TOKENS, OTHER, UNKNOWN
    }
    enum class Decision { ALLOW, DENY, NEED_EVIDENCE }
    enum class Reason {
        FREE_DISPATCH_REFRESH, PROFITABLE_PAID_DISPATCH_REFRESH,
        EXPLICITLY_AUTHORIZED_PURCHASE, ROUTINE_PURCHASE, FARMING, OTHER
    }
    data class Spend(
        val currency: Currency,
        val amount: Long?,
        val reason: Reason,
        val guaranteedGoldReturn: Long? = null,
        val alreadyPaidRefreshes: Int = 0
    )

    /**
     * Named resource reserves are not generic spending currencies. The sole presently
     * defined gold exception is at most two 50-gold dispatch refreshes with guaranteed
     * direct gold return > 50. Do not convert stars/chests to invented gold values.
     */
    fun authorizeSpend(spend: Spend): Decision {
        if (spend.amount == null || spend.amount < 0L ||
            spend.currency == Currency.UNKNOWN) return Decision.NEED_EVIDENCE
        if (spend.amount == 0L) return Decision.ALLOW
        if (spend.currency in setOf(Currency.RED_STONE_SHARDS,
                Currency.BLUE_STONE_SHARDS, Currency.KARMA_STONES)) return Decision.DENY
        if (spend.currency == Currency.GOLD) return when {
            spend.reason != Reason.PROFITABLE_PAID_DISPATCH_REFRESH -> Decision.DENY
            spend.amount != PAID_REFRESH_GOLD -> Decision.DENY
            spend.alreadyPaidRefreshes >= OPTIONAL_PAID_REFRESHES -> Decision.DENY
            spend.guaranteedGoldReturn == null -> Decision.NEED_EVIDENCE
            spend.guaranteedGoldReturn >= PAID_REFRESH_MIN_VERIFIED_GOLD_RETURN -> Decision.ALLOW
            else -> Decision.DENY
        }
        // Non-reserved resources are not blanket-approved: a specific reason is needed.
        return when (spend.reason) {
            Reason.EXPLICITLY_AUTHORIZED_PURCHASE, Reason.ROUTINE_PURCHASE -> Decision.ALLOW
            else -> Decision.NEED_EVIDENCE
        }
    }

    enum class Reward { GOLD, STARS, DIAMOND_CHEST, OTHER, UNKNOWN }
    data class DispatchMission(
        val id: String,
        val reward: Reward,
        val quantity: Long?,
        val locked: Boolean,
        val active: Boolean = false,
        val completed: Boolean = false
    )
    data class DispatchState(
        val missions: List<DispatchMission>,
        val freeRefreshesUsed: Int,
        val paidRefreshesUsed: Int,
        val paidRefreshGuaranteedGoldReturn: Long? = null,
        val account: Account = Account.GLOBAL,
        /** From the current regional UI, not assumed to equal global allowances. */
        val observedFreeRefreshLimit: Int? = null
    )
    enum class DispatchAction {
        CLAIM_COMPLETED, LOCK_HIGH_VALUE, FREE_REFRESH,
        PAID_REFRESH, QUICK_DISPATCH, WAIT_UNTIL_NEXT_SCHEDULE,
        NEED_REWARD_RECOGNITION, NEED_REGIONAL_RULES, NONE
    }
    data class DispatchStep(val action: DispatchAction, val missionId: String? = null)

    fun important(reward: Reward): Boolean =
        reward == Reward.GOLD || reward == Reward.STARS || reward == Reward.DIAMOND_CHEST

    /**
     * Locks valuable missions BEFORE any refresh. Never reset a locked valuable mission.
     * Uses all applicable free refreshes before considering the costly option.
     * Paid refresh requires a demonstrated profitable same-currency result, not speculation.
     */
    fun dispatchStep(s: DispatchState): DispatchStep {
        val freeLimit = when (s.account) {
            Account.GLOBAL -> s.observedFreeRefreshLimit ?: FREE_DISPATCH_REFRESHES
            Account.JAPAN -> s.observedFreeRefreshLimit
                ?: return DispatchStep(DispatchAction.NEED_REGIONAL_RULES)
        }
        require(freeLimit >= 0 && s.freeRefreshesUsed in 0..freeLimit)
        require(s.paidRefreshesUsed in 0..OPTIONAL_PAID_REFRESHES)
        if (s.missions.any { it.completed })
            return DispatchStep(DispatchAction.CLAIM_COMPLETED)
        val ready = s.missions.filter { !it.active }
        if (ready.isEmpty()) return DispatchStep(DispatchAction.WAIT_UNTIL_NEXT_SCHEDULE)
        val desirable = ready.filter { important(it.reward) && !it.locked }
        if (desirable.isNotEmpty()) {
            val priority = mapOf(Reward.DIAMOND_CHEST to 3, Reward.STARS to 2,
                Reward.GOLD to 1)
            val highest = desirable.maxWithOrNull(
                compareBy<DispatchMission> { priority[it.reward] ?: 0 }
                    .thenBy { it.quantity ?: -1L })!!
            return DispatchStep(DispatchAction.LOCK_HIGH_VALUE, highest.id)
        }
        val unlocked = ready.filter { !it.locked }
        if (unlocked.any { it.reward == Reward.UNKNOWN })
            return DispatchStep(DispatchAction.NEED_REWARD_RECOGNITION)
        if (unlocked.isNotEmpty() && s.freeRefreshesUsed < freeLimit)
            return DispatchStep(DispatchAction.FREE_REFRESH)
        if (unlocked.isNotEmpty() &&
            authorizeSpend(Spend(Currency.GOLD, PAID_REFRESH_GOLD,
                Reason.PROFITABLE_PAID_DISPATCH_REFRESH,
                s.paidRefreshGuaranteedGoldReturn, s.paidRefreshesUsed)) == Decision.ALLOW)
            return DispatchStep(DispatchAction.PAID_REFRESH)
        return DispatchStep(DispatchAction.QUICK_DISPATCH)
    }

    enum class Rarity { R, SR, SSR, UR, UR_PLUS, MR, UNKNOWN }
    data class Hero(val id: String, val rarity: Rarity, val grade: Int?)
    /** Exclude developed UR+ and MR characters graded +3 or more from guild transport. */
    fun eligibleForGuildProvisions(hero: Hero): Boolean {
        if (hero.grade == null || hero.rarity == Rarity.UNKNOWN) return false
        return hero.grade < 3 || hero.rarity in setOf(Rarity.R, Rarity.SR, Rarity.SSR, Rarity.UR)
    }
    fun guildProvisionsCandidates(heroes: List<Hero>): List<Hero> =
        heroes.filter(::eligibleForGuildProvisions)
            .sortedWith(compareBy<Hero> { it.rarity.ordinal }.thenBy { it.grade }.thenBy { it.id })

    data class AvailableMaterial(val name: String, val rarity: Rarity, val quantity: Long)
    /** Fish/plant any available material; among choices, prioritize higher rarity. */
    fun preferredMaterial(items: List<AvailableMaterial>): AvailableMaterial? =
        items.filter { it.quantity > 0 && it.rarity != Rarity.UNKNOWN }
            .maxWithOrNull(compareBy<AvailableMaterial> { it.rarity.ordinal }
                .thenBy { it.quantity }.thenBy { it.name })

    data class ExtractorOption(
        val id: String, val baseQuantity: Double, val multiplier: Double,
        val additiveBonus: Double = 0.0, val available: Boolean = true
    ) {
        fun yield(): Double = baseQuantity * multiplier + additiveBonus
    }
    /** Only compare verifiable equivalent quantities on the same task. */
    fun extractor(options: List<ExtractorOption>): ExtractorOption? =
        options.filter { it.available && it.baseQuantity >= 0 &&
            it.multiplier >= 0 && it.additiveBonus >= 0 &&
            it.yield().isFinite() }.maxWithOrNull(
            compareBy<ExtractorOption> { it.yield() }.thenBy { it.id })

    enum class Farm { STORY, TRIALS }
    fun shouldFarm(lowResources: Boolean, explicitlyRequested: Boolean): Boolean =
        lowResources || explicitlyRequested
    fun permissibleFarm(name: String): Farm? = when (name.trim().lowercase()) {
        "story", "conquest", "main story" -> Farm.STORY
        "trials", "trial" -> Farm.TRIALS
        else -> null
    }
    enum class DailyUpgrade { EQUIPMENT_ENHANCEMENT, SOUL, OVER_BOOST_STONE }
    fun routineUpgrade(type: DailyUpgrade): Boolean = when (type) {
        DailyUpgrade.EQUIPMENT_ENHANCEMENT,
        DailyUpgrade.SOUL,
        DailyUpgrade.OVER_BOOST_STONE -> true
    }
    /** Hero grading needs codex-derived rules; no generic autonomous grader. */
    fun heroGradingAllowed(codexRulesVerified: Boolean): Boolean = codexRulesVerified

    /**
     * A generic OCR button-clicker has no authority to select dispatch locks,
     * rerolls, unit assignments or extractor quantities without a structured
     * live-state decision. This is a targeted safety gate, not a finished planner.
     */
    fun needsStructuredResourceDecision(gameId: Int, labels: Collection<String>): Boolean {
        if (account(gameId) == null) return false
        val value = labels.joinToString(" ").lowercase()
        return listOf(
            "dispatch level", "quick dispatch", "dispatch missions",
            "quick refresh", "world extractor", "provisions transport",
            "dragon store", "starwish store", "武将派遣", "派遣任務",
            "おまかせ派遣", "資源輸送", "抽出", "鉱山", "ショップ"
        ).any { value.contains(it) }
    }

    fun safeUnplannedTarget(label: String): Boolean {
        val text = label.lowercase().trim()
        return text == "claim all" || text == "受取" || text == "一括受取"
    }

    fun overview(gameId: Int): String {
        val region = account(gameId) ?: return "Not a Shoujo Kaisen profile"
        val free = if (region == Account.GLOBAL) "3 global free refreshes"
            else "JP refresh allowance requires live verification"
        return """
            Shoujo Kaisen: ${region.name} (independent account)
            Dispatch: gold, stars, diamond chest first. Lock valuable missions
            BEFORE refreshing. $free; at most two conditional 50-gold retries.
            No paid retry when profit is unverified or denominated in another resource.
            Other dispatches: Quick Dispatch; collect on next scheduled session.
            Guild War Provisions: reserve strong UR+ grade +3 or above for fights.
            Garden / Ponds: use available choices, prioritize higher rarity.
            World Extractor: maximize (base output × bonus multiplier + additive bonus).
            Protect gold, red/blue stone shards, and karma stones except approved uses.
            Farming: story and trials only, and only when resources are low or requested.
            Daily upgrades: equipment, soul, Over Boost Stone.
            Hero grading: requires Asterion DB game codex integration.
            Live UI quantities and item identity must be verified before spending.
        """.trimIndent()
    }

}
