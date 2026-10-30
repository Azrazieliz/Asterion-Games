package com.gamepilot
import org.junit.Assert.*
import org.junit.Test
class MetaEvidenceTest {
  @Test fun unseenOrUnverifiedResultsNeverBecomeRankings() {
    val b = MetaEvidence.Battle(26,"global_player","pvp",listOf(1,2,3),true,1L,"screen_changed")
    assertFalse(b.verifiedSource in setOf("live_battle_result","verified_official_record"))
  }
}
