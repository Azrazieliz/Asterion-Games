package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class ShoujoKaisenResourcesTest {
    private val p = ShoujoKaisenResources

    @Test fun globalAndJapaneseAccountsShareRulesButNotIdentity() {
        assertEquals(ShoujoKaisenResources.Account.GLOBAL, p.account(26))
        assertEquals(ShoujoKaisenResources.Account.JAPAN, p.account(22))
        assertNull(p.account(25))
    }
    @Test fun lockAllThreeHighPriorityRewardsBeforeRefresh() {
        val rewards = listOf(
            ShoujoKaisenResources.DispatchMission("chest",p.Reward.DIAMOND_CHEST,1,false),
            ShoujoKaisenResources.DispatchMission("stars",p.Reward.STARS,25,false),
            ShoujoKaisenResources.DispatchMission("gold",p.Reward.GOLD,200,false),
            ShoujoKaisenResources.DispatchMission("other",p.Reward.OTHER,200,false))
        val s = p.DispatchState(rewards,0,0)
        assertEquals(p.DispatchStep(p.DispatchAction.LOCK_HIGH_VALUE,"chest"),p.dispatchStep(s))
        val a = s.copy(missions = rewards.map { if(it.id=="chest")it.copy(locked=true)else it })
        assertEquals(p.DispatchStep(p.DispatchAction.LOCK_HIGH_VALUE,"stars"),p.dispatchStep(a))
        val b = a.copy(missions = a.missions.map { if(it.id=="stars")it.copy(locked=true)else it })
        assertEquals(p.DispatchStep(p.DispatchAction.LOCK_HIGH_VALUE,"gold"),p.dispatchStep(b))
        val c = b.copy(missions = b.missions.map { if(it.id=="gold")it.copy(locked=true)else it })
        assertEquals(p.DispatchAction.FREE_REFRESH,p.dispatchStep(c).action)
    }
    @Test fun allFreeRefreshesThenAutodispatchWithoutSpeculativeGoldSpend() {
        val s = p.DispatchState(listOf(p.DispatchMission("trash",p.Reward.OTHER,3,false)),3,0)
        assertEquals(p.DispatchAction.QUICK_DISPATCH,p.dispatchStep(s).action)
        assertEquals(p.DispatchAction.QUICK_DISPATCH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=50)).action)
        assertEquals(p.DispatchAction.PAID_REFRESH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=51)).action)
        assertEquals(p.DispatchAction.QUICK_DISPATCH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=100,paidRefreshesUsed=2)).action)
    }
    @Test fun unknownRewardRequiresClassificationBeforeReset() {
        val s = p.DispatchState(listOf(p.DispatchMission("unknown",p.Reward.UNKNOWN,null,false)),0,0)
        assertEquals(p.DispatchAction.NEED_REWARD_RECOGNITION,p.dispatchStep(s).action)
    }
    @Test fun neverSpendGoldShardsOrKarmaStonesInRoutine() {
        for (currency in listOf(p.Currency.RED_STONE_SHARDS,p.Currency.BLUE_STONE_SHARDS,
            p.Currency.KARMA_STONES,p.Currency.GOLD)) {
            assertEquals(p.Decision.DENY,
                p.authorizeSpend(p.Spend(currency,10,p.Reason.ROUTINE_PURCHASE)))
        }
        assertEquals(p.Decision.NEED_EVIDENCE,
            p.authorizeSpend(p.Spend(p.Currency.GOLD,50,
                p.Reason.PROFITABLE_PAID_DISPATCH_REFRESH)))
        assertEquals(p.Decision.ALLOW,
            p.authorizeSpend(p.Spend(p.Currency.GOLD,50,
                p.Reason.PROFITABLE_PAID_DISPATCH_REFRESH,200,1)))
        assertEquals(p.Decision.DENY,
            p.authorizeSpend(p.Spend(p.Currency.GOLD,50,
                p.Reason.PROFITABLE_PAID_DISPATCH_REFRESH,200,2)))
    }
    @Test fun guildWarUsesMidLevelHeroesAndReservesStrongUrPlus() {
        val heroes = listOf(p.Hero("strong",p.Rarity.UR_PLUS,3),
            p.Hero("weakplus",p.Rarity.UR_PLUS,2),
            p.Hero("weakur",p.Rarity.UR,7),p.Hero("strongmr",p.Rarity.MR,5))
        assertEquals(setOf("weakplus","weakur"),p.guildProvisionsCandidates(heroes).map { it.id }.toSet())
    }
    @Test fun plantsAndFishUseAvailableHigherRarity() {
        val options = listOf(p.AvailableMaterial("low",p.Rarity.SR,100),
            p.AvailableMaterial("high",p.Rarity.UR,1),
            p.AvailableMaterial("empty",p.Rarity.MR,0))
        assertEquals("high",p.preferredMaterial(options)?.name)
    }
    @Test fun extractorComputesOutputRatherThanLookingAtRawBonus() {
        val options = listOf(p.ExtractorOption("a",100.0,1.2),
            p.ExtractorOption("b",80.0,1.75),
            p.ExtractorOption("c",100.0,2.0,0.0,false))
        assertEquals("b",p.extractor(options)?.id)
    }
    @Test fun onlyStoryOrTrialsAndOnlyWhenResourceConstrained() {
        assertFalse(p.shouldFarm(false,false))
        assertTrue(p.shouldFarm(true,false))
        assertEquals(p.Farm.STORY,p.permissibleFarm("Conquest"))
        assertEquals(p.Farm.TRIALS,p.permissibleFarm("Trials"))
        assertNull(p.permissibleFarm("Arena"))
    }
    @Test fun heroGradingWaitsForCodexWhileRoutineUpgradesAreKnown() {
        assertTrue(p.routineUpgrade(p.DailyUpgrade.SOUL))
        assertTrue(p.routineUpgrade(p.DailyUpgrade.OVER_BOOST_STONE))
        assertFalse(p.heroGradingAllowed(false))
    }
}
