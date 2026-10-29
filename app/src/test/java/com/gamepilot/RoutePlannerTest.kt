package com.gamepilot

import org.junit.Assert.*
import org.junit.Test

class RoutePlannerTest {
    private fun edge(from: String, to: String, key: String, verified: Int = 0) =
        RouteEdge(from, to, key, "ocr:$key", 3, 2, verified)

    @Test fun multiScreenRouteLeadsToVerifiedOutcome() {
        val graph = listOf(
            edge("home", "missions", "missions"),
            edge("missions", "battle", "start"),
            edge("battle", "result", "victory", verified = 2)
        )
        val p = RoutePlanner.ranked("home", graph)
        assertEquals("missions", p.first().edge.actionKey)
        assertEquals(2, p.first().stepsToProof)
    }

    @Test fun unrelatedAndUnprovenBranchesCannotBeReplayed() {
        val graph = listOf(
            edge("home", "unknown", "random"),
            edge("unknown", "home", "loop"),
            edge("home", "battle", "battle"),
            edge("battle", "win", "clear", verified = 1)
        )
        assertEquals(listOf("battle"), RoutePlanner.ranked("home", graph).map { it.edge.actionKey })
    }

    @Test fun acceptsProvenActionAndDoesNotRepeatUnprovenCycle() {
        val graph = listOf(
            edge("a", "b", "go"),
            edge("b", "a", "back"),
            edge("a", "finish", "win", verified = 3)
        )
        assertEquals("win", RoutePlanner.ranked("a", graph).first().edge.actionKey)
        assertTrue(RoutePlanner.ranked("nothing", graph).isEmpty())
    }

    @Test fun neverTreatsSceneChangeAloneAsProof() {
        assertTrue(RoutePlanner.ranked("menu", listOf(edge("menu", "battle", "start"))).isEmpty())
    }

    @Test fun depthCapRejectsLongUnverifiedPaths() {
        val edges = (0..11).map { i ->
            edge("s$i", "s${i+1}", "action$i", verified = if (i == 11) 1 else 0)
        }
        assertTrue(RoutePlanner.ranked("s0", edges, maxDepth = 8).isEmpty())
        assertEquals(11, RoutePlanner.ranked("s0", edges, maxDepth = 20).first().stepsToProof)
    }
}
