package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class GameNewsDirectoryTest {
    @Test fun regionalShoujoSourcesNeverCollapseTogether() {
        val jp = GameNewsDirectory.candidateSources(22)
        val global = GameNewsDirectory.candidateSources(26)
        assertTrue(jp.any { it.handle == "shoujokaisen" && it.locale == "ja" })
        assertTrue(global.any { it.handle == "Attackontime_sh" && it.locale == "en" })
        assertTrue(jp.map { it.handle }.intersect(global.map { it.handle }.toSet()).isEmpty())
    }
    @Test fun allSourceLinksUsePublicIdentifiersOnly() {
        val ids = listOf(3,4,5,6,7,8,9,11,12,14,16,17,20,21,22,24,25,26)
        val accounts = ids.flatMap(GameNewsDirectory::candidateSources)
        assertTrue(accounts.size >= 18)
        assertTrue(accounts.all { GameNewsDirectory.isAcceptableHandle(it.handle) })
        assertTrue(accounts.all { it.publicUrl.startsWith("https://x.com/") })
    }
    @Test fun unsourcedGameIsNotMisrepresentedAsConnected() {
        assertTrue(GameNewsDirectory.candidateSources(1).isEmpty())
        assertTrue(GameNewsDirectory.status(1).contains("not connected"))
    }
}
