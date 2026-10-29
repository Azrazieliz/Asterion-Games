package com.gamepilot

/**
 * Game-local planning over previously observed UI transitions.
 * A route is eligible only if a later transition has a VERIFIED game outcome.
 * This makes multi-screen paths reusable, unlike one-step-only memorization.
 *
 * Pure Kotlin so the planner can be tested without Android/device privileges.
 */
data class RouteEdge(
    val source: String,
    val destination: String,
    val actionKey: String,
    val label: String,
    val attempts: Int,
    val changes: Int,
    val verified: Int
)

object RoutePlanner {
    data class Ranked(val edge: RouteEdge, val stepsToProof: Int, val score: Double)
    fun ranked(scene: String, edges: List<RouteEdge>, maxDepth: Int = 8): List<Ranked> {
        if (maxDepth < 1) return emptyList()
        val eligible = edges.filter {
            it.attempts > 0 && it.changes > 0 && it.destination.isNotBlank()
        }
        // Backpropagate evidence through the predecessor graph, avoiding cycles.
        val distances = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()
        eligible.filter { it.verified > 0 }.forEach { e ->
            if (distances.putIfAbsent(e.source, 0) == null) queue.add(e.source)
        }
        val predecessors = eligible.groupBy { it.destination }
        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            val distance = distances[state] ?: continue
            if (distance >= maxDepth - 1) continue
            predecessors[state].orEmpty().forEach { previous ->
                if (distances.putIfAbsent(previous.source, distance + 1) == null)
                    queue.add(previous.source)
            }
        }
        return eligible.asSequence().filter { it.source == scene }
            .mapNotNull { edge ->
                val hops = if (edge.verified > 0) 0 else
                    distances[edge.destination]?.plus(1) ?: return@mapNotNull null
                if (hops >= maxDepth) return@mapNotNull null
                val reliability = edge.changes.toDouble() / edge.attempts
                val success = edge.verified.toDouble() / edge.attempts
                val score = success * 20.0 + reliability * 3.0 - hops * 1.8 -
                    (edge.attempts - edge.changes).coerceAtLeast(0) * 0.3
                Ranked(edge, hops, score)
            }
            .sortedWith(compareByDescending<Ranked> { it.score }
                .thenBy { it.stepsToProof }.thenBy { it.edge.actionKey })
            .toList()
    }
}
