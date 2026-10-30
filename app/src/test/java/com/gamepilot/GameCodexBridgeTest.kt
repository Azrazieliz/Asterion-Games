package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class GameCodexBridgeTest {
    @Test fun globalAndJpCanNeverResolveToSameSnapshot() {
        assertEquals("global", GameCodexBridge.accountFor(26))
        assertEquals("jp", GameCodexBridge.accountFor(22))
        assertNull(GameCodexBridge.accountFor(8))
    }
    @Test fun experimentalRanksAreNeverAutomaticHeroGradingAuthority() {
        val r = GameCodexBridge.SyncResult(GameCodexBridge.SyncState.LINKED,
            "global", 30, 1L)
        assertFalse(GameCodexBridge.isReadyToGrade(r))
    }
}
