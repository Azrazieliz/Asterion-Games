package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class StrategyExperimentTest {
    private fun run(mode: String, complete: Boolean, minutes: Long): StrategyExperiment.Run =
        StrategyExperiment.Run(26, "global_account", mode, "dailies:1",
            1000L, 1000L + minutes*60000, if (complete) 1 else 0, 1, 50)

    @Test fun rejectsForgedUnverifiedOrUnboundedSessions() {
        assertFalse(StrategyExperiment.valid(run("adaptive",true,10).copy(targetGoals=0)))
        assertFalse(StrategyExperiment.valid(run("adaptive",true,10).copy(verifiedGoals=2)))
        assertFalse(StrategyExperiment.valid(run("adaptive",true,10).copy(accountKey="")))
        assertFalse(StrategyExperiment.valid(run("adaptive",true,10).copy(variant="unverified_strategy")))
    }
    @Test fun needsEvidenceAndNeverPrefersFailureForSpeed() {
        val a=StrategyExperiment.summarize(List(5){run("scripted",true,20)},"scripted")
        val b=StrategyExperiment.summarize(List(5){run("adaptive",false,4)},"adaptive")
        assertEquals(StrategyExperiment.Verdict.REGRESSION,StrategyExperiment.compare(a,b))
        assertEquals(StrategyExperiment.Verdict.UNPROVEN,StrategyExperiment.compare(a,
            StrategyExperiment.summarize(List(3){run("adaptive",true,8)},"adaptive")))
    }
    @Test fun canCompareMeasuredEquivalentStrategies() {
        val a=StrategyExperiment.summarize(List(7){run("scripted",true,20)},"scripted")
        val b=StrategyExperiment.summarize(List(7){run("adaptive",true,10)},"adaptive")
        assertEquals(StrategyExperiment.Verdict.BETTER_SUPPORTED,StrategyExperiment.compare(a,b))
        val slower=StrategyExperiment.summarize(List(7){run("adaptive",true,19)},"adaptive")
        assertEquals(StrategyExperiment.Verdict.KEEP_CURRENT,StrategyExperiment.compare(a,slower))
    }
}
