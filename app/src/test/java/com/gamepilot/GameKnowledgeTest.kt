package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class GameKnowledgeTest {
    @Test fun rosterContainsExactly26Games() {
        assertEquals(26, GameRoster.titles.size)
        assertEquals(26, GameRoster.titles.distinct().size)
    }
    @Test fun initialObjectivesCoverDailiesRewardsAndFarming() {
        assertEquals(setOf(GoalType.REWARDS, GoalType.DAILIES, GoalType.FARMING),
            GoalSettings.defaults().map { it.type }.toSet())
    }
    @Test fun routineButtonsAreNotFalseProofOfSuccess() {
        assertFalse(GameKnowledge.explicitSuccess(GameGoal(GoalType.REWARDS), "Claim"))
        assertFalse(GameKnowledge.explicitSuccess(GameGoal(GoalType.DAILIES), "Daily"))
        assertFalse(GameKnowledge.explicitSuccess(GameGoal(GoalType.FARMING), "Start"))
    }
    @Test fun actualResultStringsCanBeRecognizedInMultipleLanguages() {
        assertTrue(GameKnowledge.explicitSuccess(GameGoal(GoalType.REWARDS), "Rewards claimed"))
        assertTrue(GameKnowledge.explicitSuccess(GameGoal(GoalType.FARMING), "VICTORY"))
        assertTrue(GameKnowledge.explicitSuccess(GameGoal(GoalType.FARMING), "作戦成功"))
        assertTrue(GameKnowledge.explicitSuccess(GameGoal(GoalType.FARMING), "胜利"))
        assertTrue(GameKnowledge.explicitSuccess(GameGoal(GoalType.DAILIES), "All daily tasks completed"))
    }
    @Test fun avoidDangerousControls() {
        assertTrue(GameKnowledge.dangerous("Purchase"))
        assertTrue(GameKnowledge.dangerous("充值"))
        assertTrue(GameKnowledge.dangerous("ガチャ"))
        assertFalse(GameKnowledge.dangerous("Daily Mission"))
        assertFalse(GameKnowledge.dangerous("Collect"))
    }
    @Test fun normalizationRetainsMultilingualLabels() {
        assertEquals("daily tasks", GameKnowledge.normalize("  DAILY   TASKS "))
        assertEquals("受取", GameKnowledge.normalize("受取"))
        assertEquals("领取成功", GameKnowledge.normalize("领取成功"))
    }
}
