package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class AttackOnTimeVideoProfileTest {
    @Test fun videoCoversFifteenMinutesAndKnownSections() {
        assertEquals(26, AttackOnTimeVideoProfile.GAME_ID)
        assertEquals(903, AttackOnTimeVideoProfile.DURATION_SECONDS)
        assertTrue(AttackOnTimeVideoProfile.observations.size >= 20)
        assertTrue(AttackOnTimeVideoProfile.observations.any { it.scene == AttackOnTimeVideoProfile.Scene.DISPATCH })
        assertTrue(AttackOnTimeVideoProfile.observations.any { it.scene == AttackOnTimeVideoProfile.Scene.GUILD_QUESTS })
    }
    @Test fun recognizeVideoScreenTitles() {
        val p = AttackOnTimeVideoProfile
        assertEquals(p.Scene.DISPATCH, p.identify(listOf("Dispatch Level: 9", "House Repairs")))
        assertEquals(p.Scene.QUICK_MODE_REPORT, p.identify(listOf("Quick Mode Report", "Claimed")))
        assertEquals(p.Scene.SPACETIME_ARENA, p.identify(listOf("Spacetime Arena", "Current Rank")))
        assertEquals(p.Scene.GALAXY_CLASH, p.identify(listOf("Galaxy Clash", "Raffle Tickets")))
        assertEquals(p.Scene.GARDEN, p.identify(listOf("Garden", "Successfully Planted")))
    }
    @Test fun doNotConfuseLossWithVictory() {
        val p = AttackOnTimeVideoProfile
        assertEquals("Single battle lost", p.outcome(p.Scene.DEFEAT, listOf("Lose")))
        assertNull(p.outcome(p.Scene.VICTORY, listOf("Lose")))
        assertEquals("Single battle won", p.outcome(p.Scene.VICTORY, listOf("Victory")))
    }
    @Test fun guardAdsSpendingAndIrreversibleAdvancement() {
        val p = AttackOnTimeVideoProfile
        assertEquals(p.Scene.ADVERTISEMENT, p.identify(listOf("Gossip Harbor", "Install")))
        assertTrue(p.mustPause(p.Scene.ADVERTISEMENT, listOf("Install")))
        assertTrue(p.mustPause(p.Scene.GUILD_STORE, listOf("Purchase")))
        assertTrue(p.mustPause(p.Scene.TREASURE_ASCEND, listOf("Ascend")))
        assertTrue(p.mustPause(p.Scene.HERO_DRAW, listOf("Draw x10")))
        assertFalse(p.mustPause(p.Scene.DISPATCH, listOf("Quick Dispatch", "Claim All")))
    }
    @Test fun game26HasSpecificRoutineCues() {
        val daily = GamePlaybooks.cues(26, GoalType.DAILIES)
        assertTrue(daily.contains("spacetime arena"))
        assertTrue(daily.contains("galaxy clash"))
        assertTrue(GamePlaybooks.cues(26, GoalType.REWARDS).contains("quick mode report"))
        assertTrue(GamePlaybooks.cues(26, GoalType.FARMING).contains("stage rewards"))
        assertTrue(GamePlaybooks.cues(25, GoalType.DAILIES).isEmpty())
    }
}
