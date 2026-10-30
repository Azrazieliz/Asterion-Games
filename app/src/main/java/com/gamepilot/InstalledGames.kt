package com.gamepilot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale

/** Conservative on-device app-label matching; never guess when more than one candidate matches. */
object InstalledGames {
    private val aliases: Map<Int, List<String>> = mapOf(
        1 to listOf("Honkai Impact 3rd", "Honkai Impact"),
        2 to listOf("Arknights Endfield", "Endfield"),
        3 to listOf("Goddess of Victory Nikke", "NIKKE"),
        4 to listOf("Bleach Brave Souls"),
        5 to listOf("One Piece"),
        6 to listOf("Honkai Star Rail", "Star Rail"),
        7 to listOf("Chaos Zero Nightmare"),
        8 to listOf("Arknights"),
        9 to listOf("放置少女"),
        10 to listOf("リバースブルー リバースエンド", "リバ×リバ"),
        11 to listOf("Azur Lane"),
        12 to listOf("Wuthering Waves"),
        13 to listOf("Reverse 1999"),
        14 to listOf("ETE Chronicle", "E.T.E Chronicle"),
        15 to listOf("Goddess P"),
        16 to listOf("Stellar Affinity"),
        17 to listOf("TenkafuMA"),
        18 to listOf("Mirrors High"),
        19 to listOf("Duel Links"),
        20 to listOf("Dokkan"),
        21 to listOf("SLIME IM", "Isekai Memories"),
        22 to listOf("少女廻戦"),
        23 to listOf("7DS", "Seven Deadly Sins"),
        24 to listOf("Fate Grand Order", "Fate GO"),
        25 to listOf("Echocalypse"),
        26 to listOf("Attack on Time", "Attack on T")
    )
    private fun normalized(text: String): String =
        text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
    private fun score(label: String, name: String): Int {
        val a = normalized(label)
        val b = normalized(name)
        if (a.isEmpty() || b.isEmpty()) return 0
        if (a == b) return 100
        if (a.contains(b) && b.length >= 4) return 80
        if (b.contains(a) && a.length >= 5) return 65
        return 0
    }
    fun autoLink(context: Context): Int {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            .filter { it.second != context.packageName }
            .distinctBy { it.second }
        val profiles = Profiles.all(context)
        val alreadyUsed = profiles.map { it.packageName }.filter { it.isNotBlank() }.toMutableSet()
        var count = 0
        val updated = profiles.map { p ->
            if (p.packageName.isNotBlank()) return@map p
            val names = aliases[p.id] ?: listOf(p.title)
            val candidates = apps.map { app ->
                val max = names.maxOf { score(app.first, it) }
                Triple(app.first, app.second, max)
            }.filter { it.third >= 65 && it.second !in alreadyUsed }
                .sortedByDescending { it.third }
            val first = candidates.firstOrNull()
            val next = candidates.getOrNull(1)
            if (first != null && (next == null || first.third > next.third)) {
                count++
                alreadyUsed.add(first.second)
                p.copy(packageName = first.second)
            } else p
        }
        if (count > 0) {
            Profiles.save(context, updated)
            updated.filter { it.enabled }.forEach { Scheduler.schedule(context, it) }
            Events.record(context, "Auto-linked $count installed game packages")
        }
        return count
    }
}
