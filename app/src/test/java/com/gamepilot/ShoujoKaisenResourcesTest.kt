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
            ShoujoKaisenResources.DispatchMission("chest",ShoujoKaisenResources.Reward.DIAMOND_CHEST,1,false),
            ShoujoKaisenResources.DispatchMission("stars",ShoujoKaisenResources.Reward.STARS,25,false),
            ShoujoKaisenResources.DispatchMission("gold",ShoujoKaisenResources.Reward.GOLD,200,false),
            ShoujoKaisenResources.DispatchMission("other",ShoujoKaisenResources.Reward.OTHER,200,false))
        val s = ShoujoKaisenResources.DispatchState(rewards,0,0)
        assertEquals(ShoujoKaisenResources.DispatchStep(ShoujoKaisenResources.DispatchAction.LOCK_HIGH_VALUE,"chest"),p.dispatchStep(s))
        val a = s.copy(missions = rewards.map { if(it.id=="chest")it.copy(locked=true)else it })
        assertEquals(ShoujoKaisenResources.DispatchStep(ShoujoKaisenResources.DispatchAction.LOCK_HIGH_VALUE,"stars"),p.dispatchStep(a))
        val b = a.copy(missions = a.missions.map { if(it.id=="stars")it.copy(locked=true)else it })
        assertEquals(ShoujoKaisenResources.DispatchStep(ShoujoKaisenResources.DispatchAction.LOCK_HIGH_VALUE,"gold"),p.dispatchStep(b))
        val c = b.copy(missions = b.missions.map { if(it.id=="gold")it.copy(locked=true)else it })
        assertEquals(ShoujoKaisenResources.DispatchAction.FREE_REFRESH,p.dispatchStep(c).action)
    }
    @Test fun allFreeRefreshesThenAutodispatchWithoutSpeculativeGoldSpend() {
        val s = ShoujoKaisenResources.DispatchState(listOf(ShoujoKaisenResources.DispatchMission("trash",ShoujoKaisenResources.Reward.OTHER,3,false)),3,0)
        assertEquals(ShoujoKaisenResources.DispatchAction.QUICK_DISPATCH,p.dispatchStep(s).action)
        assertEquals(ShoujoKaisenResources.DispatchAction.QUICK_DISPATCH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=50)).action)
        assertEquals(ShoujoKaisenResources.DispatchAction.PAID_REFRESH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=51)).action)
        assertEquals(ShoujoKaisenResources.DispatchAction.QUICK_DISPATCH,
            p.dispatchStep(s.copy(paidRefreshGuaranteedGoldReturn=100,paidRefreshesUsed=2)).action)
    }
    @Test fun unknownRewardRequiresClassificationBeforeReset() {
        val s = ShoujoKaisenResources.DispatchState(listOf(ShoujoKaisenResources.DispatchMission("unknown",ShoujoKaisenResources.Reward.UNKNOWN,null,false)),0,0)
        assertEquals(ShoujoKaisenResources.DispatchAction.NEED_REWARD_RECOGNITION,p.dispatchStep(s).action)
    }
    @Test fun jpRefreshLimitCannotBeGuessedFromGlobal() {
        val task = ShoujoKaisenResources.DispatchMission(
            "task",ShoujoKaisenResources.Reward.OTHER,12,false)
        val unknown = ShoujoKaisenResources.DispatchState(listOf(task),0,0,
            account=ShoujoKaisenResources.Account.JAPAN)
        assertEquals(ShoujoKaisenResources.DispatchAction.NEED_REGIONAL_RULES,
            p.dispatchStep(unknown).action)
        assertEquals(ShoujoKaisenResources.DispatchAction.FREE_REFRESH,
            p.dispatchStep(unknown.copy(observedFreeRefreshLimit=2)).action)
    }
    @Test fun genericAgentCannotResetOrAutoDispatchBeforeRewardClassification() {
        val labels = listOf("Dispatch Level: 9", "Quick Dispatch", "1 time(s) FREE")
        assertTrue(p.needsStructuredResourceDecision(26, labels))
        assertFalse(p.safeUnplannedTarget("Quick Dispatch"))
        assertFalse(p.safeUnplannedTarget("Refresh"))
        assertTrue(p.safeUnplannedTarget("Claim All"))
        assertFalse(p.needsStructuredResourceDecision(6, labels))
        assertTrue(p.needsStructuredResourceDecision(22, listOf("武将派遣")))
    }
    @Test fun neverSpendGoldShardsOrKarmaStonesInRoutine() {
        for (currency in listOf(ShoujoKaisenResources.Currency.RED_STONE_SHARDS,ShoujoKaisenResources.Currency.BLUE_STONE_SHARDS,
            ShoujoKaisenResources.Currency.KARMA_STONES,ShoujoKaisenResources.Currency.GOLD)) {
            assertEquals(ShoujoKaisenResources.Decision.DENY,
                p.authorizeSpend(ShoujoKaisenResources.Spend(currency,10,ShoujoKaisenResources.Reason.ROUTINE_PURCHASE)))
        }
        assertEquals(ShoujoKaisenResources.Decision.NEED_EVIDENCE,
            p.authorizeSpend(ShoujoKaisenResources.Spend(ShoujoKaisenResources.Currency.GOLD,50,
                ShoujoKaisenResources.Reason.PROFITABLE_PAID_DISPATCH_REFRESH)))
        assertEquals(ShoujoKaisenResources.Decision.ALLOW,
            p.authorizeSpend(ShoujoKaisenResources.Spend(ShoujoKaisenResources.Currency.GOLD,50,
                ShoujoKaisenResources.Reason.PROFITABLE_PAID_DISPATCH_REFRESH,200,1)))
        assertEquals(ShoujoKaisenResources.Decision.DENY,
            p.authorizeSpend(ShoujoKaisenResources.Spend(ShoujoKaisenResources.Currency.GOLD,50,
                ShoujoKaisenResources.Reason.PROFITABLE_PAID_DISPATCH_REFRESH,200,2)))
    }
    @Test fun guildWarUsesMidLevelHeroesAndReservesStrongUrPlus() {
        val heroes = listOf(ShoujoKaisenResources.Hero("strong",ShoujoKaisenResources.Rarity.UR_PLUS,3),
            ShoujoKaisenResources.Hero("weakplus",ShoujoKaisenResources.Rarity.UR_PLUS,2),
            ShoujoKaisenResources.Hero("weakur",ShoujoKaisenResources.Rarity.UR,7),ShoujoKaisenResources.Hero("strongmr",ShoujoKaisenResources.Rarity.MR,5))
        assertEquals(setOf("weakplus","weakur"),p.guildProvisionsCandidates(heroes).map { it.id }.toSet())
    }
    @Test fun plantsAndFishUseAvailableHigherRarity() {
        val options = listOf(ShoujoKaisenResources.AvailableMaterial("low",ShoujoKaisenResources.Rarity.SR,100),
            ShoujoKaisenResources.AvailableMaterial("high",ShoujoKaisenResources.Rarity.UR,1),
            ShoujoKaisenResources.AvailableMaterial("empty",ShoujoKaisenResources.Rarity.MR,0))
        assertEquals("high",p.preferredMaterial(options)?.name)
    }
    @Test fun extractorComputesOutputRatherThanLookingAtRawBonus() {
        val options = listOf(ShoujoKaisenResources.ExtractorOption("a",100.0,1.2),
            ShoujoKaisenResources.ExtractorOption("b",80.0,1.75),
            ShoujoKaisenResources.ExtractorOption("c",100.0,2.0,0.0,false))
        assertEquals("b",p.extractor(options)?.id)
    }
    @Test fun onlyStoryOrTrialsAndOnlyWhenResourceConstrained() {
        assertFalse(p.shouldFarm(false,false))
        assertTrue(p.shouldFarm(true,false))
        assertEquals(ShoujoKaisenResources.Farm.STORY,p.permissibleFarm("Conquest"))
        assertEquals(ShoujoKaisenResources.Farm.TRIALS,p.permissibleFarm("Trials"))
        assertNull(p.permissibleFarm("Arena"))
    }
    @Test fun heroGradingWaitsForCodexWhileRoutineUpgradesAreKnown() {
        assertTrue(p.routineUpgrade(ShoujoKaisenResources.DailyUpgrade.SOUL))
        assertTrue(p.routineUpgrade(ShoujoKaisenResources.DailyUpgrade.OVER_BOOST_STONE))
        assertFalse(p.heroGradingAllowed(false))
    }
}
