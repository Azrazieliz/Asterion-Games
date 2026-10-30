package com.gamepilot
import org.junit.Assert.*
import org.junit.Test
class DragonMineWarsTest {
    private fun mine(id:String,resource:DragonMineWars.Resource,rate:Long,ours:Boolean=false)=
        DragonMineWars.Mine(id,resource,rate,ours,100,0,300)
    private fun team()=DragonMineWars.Formation(listOf("hero-1"),150,false,true,1)
    @Test fun recognizesScreensWithoutMergingJp() {
        assertEquals(DragonMineWars.Screen.INFO,DragonMineWars.identify(
            listOf("Dragon Mine ID: 3-5","Garrisoned Troops","Collection Info")))
        assertTrue(DragonMineWars.requiresPlanner(26,
            listOf("Draconic Mirage","Quick Search")))
        assertFalse(DragonMineWars.requiresPlanner(22,listOf("Draconic Mirage")))
    }
    @Test fun doNotConvertBetweenShardCurrencies() {
        val red=mine("red",DragonMineWars.Resource.RED_SHARDS,216)
        val cyan=mine("cyan",DragonMineWars.Resource.CYAN_ICON_RESOURCE,482)
        val rank=DragonMineWars.bestByResource(listOf(red,cyan),300)
        assertEquals("red",rank[DragonMineWars.Resource.RED_SHARDS]?.id)
        assertEquals("cyan",rank[DragonMineWars.Resource.CYAN_ICON_RESOURCE]?.id)
        assertEquals(64800L,DragonMineWars.yield(red,300))
        assertEquals(20,DragonMineWars.buffPercent(1))
        assertNull(DragonMineWars.buffPercent(7))
    }
    @Test fun noAttackWithoutVerifiedFormation() {
        val red=mine("a",DragonMineWars.Resource.RED_SHARDS,216)
        val state=DragonMineWars.State(emptyList(),4,5,emptyList())
        assertEquals(DragonMineWars.Action.REQUIRE_EVIDENCE,
            DragonMineWars.decide(state,red,red.reward,300).action)
        assertEquals(DragonMineWars.Action.ATTACK_WITH_VERIFICATION,
            DragonMineWars.decide(state.copy(availableFormations=listOf(team())),
                red,red.reward,300).action)
    }
    @Test fun dontReplaceProductiveMineForWorseRate() {
        val held=mine("held",DragonMineWars.Resource.RED_SHARDS,216,true)
        val new=mine("new",DragonMineWars.Resource.RED_SHARDS,158)
        val state=DragonMineWars.State(listOf(held),5,5,listOf(team()))
        assertEquals(DragonMineWars.Action.STAY,
            DragonMineWars.decide(state,new,new.reward,300).action)
        assertEquals(DragonMineWars.Action.CONSIDER_REPLACEMENT,
            DragonMineWars.decide(state,new.copy(ratePerMinute=290),new.reward,300).action)
    }
    @Test fun collectionWindowIsBoundedByVisibleMeter() {
        val m=mine("a",DragonMineWars.Resource.CYAN_ICON_RESOURCE,10)
            .copy(collectionElapsedMin=200,collectionLimitMin=300)
        assertEquals(1000L,DragonMineWars.yield(m,500))
    }
}
