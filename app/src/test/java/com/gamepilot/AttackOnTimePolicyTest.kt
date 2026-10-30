package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class AttackOnTimePolicyTest {
    private val p = AttackOnTimePolicy
    private fun offer(item: AttackOnTimePolicy.Item = AttackOnTimePolicy.Item.ASTRAL_LEGACY_BEADS,
        currency: AttackOnTimePolicy.Currency = AttackOnTimePolicy.Currency.NON_GOLD,
        owned: Boolean = false, complete: Boolean = false) =
        AttackOnTimePolicy.Offer(item, currency, 10L, alreadyOwned = owned,
            fullMartialArmsSetOwned = complete)

    @Test fun completingDailiesDoesNotMeanClearingEveryQuest() {
        assertTrue(p.dailiesComplete(true))
        assertFalse(p.dailiesComplete(false))
    }
    @Test fun onlyVerifiedFreeDraws() {
        assertEquals(AttackOnTimePolicy.Decision.FREE_DRAW, p.draw(true, 0))
        assertEquals(AttackOnTimePolicy.Decision.SKIP, p.draw(true, 1500))
        assertEquals(AttackOnTimePolicy.Decision.SKIP, p.draw(true, null))
        assertEquals(AttackOnTimePolicy.Decision.SKIP, p.draw(false, 0))
    }
    @Test fun dailyShopTwoPurchasesButNoGold() {
        assertEquals(AttackOnTimePolicy.Decision.BUY_DAILY, p.dailyShop(offer(), 1))
        assertEquals(AttackOnTimePolicy.Decision.SKIP, p.dailyShop(offer(), 2))
        assertEquals(AttackOnTimePolicy.Decision.SKIP,
            p.dailyShop(offer(currency=AttackOnTimePolicy.Currency.GOLD), 0))
        assertEquals(AttackOnTimePolicy.Decision.EVIDENCE_REQUIRED,
            p.dailyShop(offer(currency=AttackOnTimePolicy.Currency.UNKNOWN), 0))
    }
    @Test fun dailyPurchaseNeverCreatesMartialArmsDuplicate() {
        assertEquals(AttackOnTimePolicy.Decision.SKIP,
            p.dailyShop(offer(AttackOnTimePolicy.Item.MARTIAL_ARMS, owned=true), 0))
    }
    @Test fun completeUniqueMartialArmsBeforeDuplicatesAndPreferShards() {
        val state = AttackOnTimePolicy.WeeklyState(true, false, true)
        val item = AttackOnTimePolicy.Item.MARTIAL_ARMS
        assertEquals(AttackOnTimePolicy.Decision.BUY_MISSING_SET_PIECE,
            p.weeklyShop(offer(item), state))
        assertEquals(AttackOnTimePolicy.Decision.SKIP,
            p.weeklyShop(offer(item, owned=true), state))
        assertEquals(AttackOnTimePolicy.Decision.SKIP,
            p.weeklyShop(offer(item, owned=true, complete=true), state))
        assertEquals(AttackOnTimePolicy.Decision.BUY_UR_PLUS_SHARDS,
            p.weeklyShop(offer(AttackOnTimePolicy.Item.UR_PLUS_SHARDS), state))
    }
    @Test fun rankFourTargetRule() {
        assertEquals(AttackOnTimePolicy.Decision.RANK_OK, p.pvp(4,true))
        assertEquals(AttackOnTimePolicy.Decision.RANK_OK, p.pvp(2,true))
        assertEquals(AttackOnTimePolicy.Decision.CHALLENGE_CURRENT_FOURTH, p.pvp(5,true))
        assertEquals(AttackOnTimePolicy.Decision.EVIDENCE_REQUIRED, p.pvp(5,false))
    }
    @Test fun afkRewardAdOnlyAt60HourMaximum() {
        assertEquals(AttackOnTimePolicy.Decision.WAIT_FOR_AFK_CAP, p.afk(3599,false))
        assertEquals(AttackOnTimePolicy.Decision.WATCH_DOUBLE_AD, p.afk(3600,false))
        assertEquals(AttackOnTimePolicy.Decision.ALREADY_DOUBLED, p.afk(3600,true))
    }
}
