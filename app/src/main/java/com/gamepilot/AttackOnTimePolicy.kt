package com.gamepilot

/** Exact game-26 decision rules supplied by the user on 2026-10-30.
 * This model is NOT proof that live inventory, ranks, timers or actions are wired up.
 */
object AttackOnTimePolicy {
    const val GAME_ID = 26
    const val DAILY_SHOP_PURCHASES = 2
    const val TARGET_PVP_RANK = 4
    const val AFK_CAP_MINUTES = 60L * 60L
    const val RECURRING_EVENTS = 5

    enum class Decision {
        FREE_DRAW, SKIP, BUY_DAILY, BUY_MISSING_SET_PIECE, BUY_UR_PLUS_SHARDS,
        CONSIDER_OTHER_WEEKLY, EVIDENCE_REQUIRED, CHALLENGE_CURRENT_FOURTH,
        RANK_OK, WATCH_DOUBLE_AD, WAIT_FOR_AFK_CAP, ALREADY_DOUBLED
    }
    enum class Currency { GOLD, NON_GOLD, REAL_MONEY, UNKNOWN }
    enum class Item { MARTIAL_ARMS, UR_PLUS_SHARDS, ASTRAL_LEGACY_BEADS, OTHER }
    data class Offer(
        val item: Item, val currency: Currency, val price: Long?,
        val available: Boolean = true, val alreadyOwned: Boolean = false,
        val fullMartialArmsSetOwned: Boolean = false
    )
    data class WeeklyState(
        val weeklyRefreshDue: Boolean, val purchasedThisWeek: Boolean,
        val urPlusShardsAvailableForSameCurrency: Boolean = false
    )

    /** All DAILY REWARD TIERS, not every quest, must be claimed. */
    fun dailiesComplete(allRewardTiersClaimed: Boolean) = allRewardTiersClaimed

    /** Paid 10-draws are not included. A displayed 'free' label alone is insufficient. */
    fun draw(explicitlyFree: Boolean, verifiedPrice: Long?): Decision =
        if (explicitlyFree && verifiedPrice == 0L) Decision.FREE_DRAW else Decision.SKIP

    fun dailyShop(offer: Offer, purchasesToday: Int): Decision {
        if (purchasesToday >= DAILY_SHOP_PURCHASES || !offer.available ||
            offer.currency == Currency.GOLD || offer.currency == Currency.REAL_MONEY) return Decision.SKIP
        if (offer.currency != Currency.NON_GOLD || offer.price == null) return Decision.EVIDENCE_REQUIRED
        return Decision.BUY_DAILY
    }

    /** No duplicate Martial Arms piece until all distinct set pieces are acquired. */
    fun weeklyShop(offer: Offer, state: WeeklyState): Decision {
        if (!state.weeklyRefreshDue || state.purchasedThisWeek || !offer.available ||
            offer.currency == Currency.GOLD || offer.currency == Currency.REAL_MONEY) return Decision.SKIP
        if (offer.currency != Currency.NON_GOLD || offer.price == null) return Decision.EVIDENCE_REQUIRED
        return when (offer.item) {
            Item.MARTIAL_ARMS -> when {
                !offer.alreadyOwned -> Decision.BUY_MISSING_SET_PIECE
                !offer.fullMartialArmsSetOwned -> Decision.SKIP
                state.urPlusShardsAvailableForSameCurrency -> Decision.SKIP
                else -> Decision.CONSIDER_OTHER_WEEKLY
            }
            Item.UR_PLUS_SHARDS -> Decision.BUY_UR_PLUS_SHARDS
            else -> Decision.CONSIDER_OTHER_WEEKLY
        }
    }

    /** Losses are acceptable. Below rank #4, keep targeting the ACTUAL current #4. */
    fun pvp(ownRank: Int?, fourthRankOpponentKnown: Boolean): Decision {
        if (ownRank == null) return Decision.EVIDENCE_REQUIRED
        if (ownRank <= TARGET_PVP_RANK) return Decision.RANK_OK
        return if (fourthRankOpponentKnown) Decision.CHALLENGE_CURRENT_FOURTH
        else Decision.EVIDENCE_REQUIRED
    }

    /** Request the ad at 60 hours, not when the AFK bonus is only partly accumulated. */
    fun afk(accruedMinutes: Long?, doubledForThisCycle: Boolean): Decision = when {
        doubledForThisCycle -> Decision.ALREADY_DOUBLED
        accruedMinutes == null || accruedMinutes < AFK_CAP_MINUTES -> Decision.WAIT_FOR_AFK_CAP
        else -> Decision.WATCH_DOUBLE_AD
    }
}
