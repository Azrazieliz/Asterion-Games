package com.gamepilot
import org.junit.Assert.*
import org.junit.Test
class SafeExplorationTest {
    @Test fun noPurchaseOrBattleButtonsCanBeExplored() {
        assertTrue(SafeExploration.readOnly("How to Play"))
        assertTrue(SafeExploration.readOnly("Guide"))
        for (s in listOf("Purchase","Claim","Draw x10","Buy", "Battle",
            "Refresh","Pay 50 Gold", "Install","Challenge"))
            assertFalse(SafeExploration.readOnly(s))
    }
    @Test fun noRepeatedOrOffScreenAction() {
        val options=listOf(SafeExploration.Hint("Guide",.5f,1.2f),
            SafeExploration.Hint("Help",.5f,.5f),
            SafeExploration.Hint("Details",.4f,.7f))
        assertEquals("Details",SafeExploration.select(options,setOf("help"))?.label)
        assertNull(SafeExploration.select(options,setOf("help","details")))
    }
}
