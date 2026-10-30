package com.gamepilot

/**
 * Public-source candidates cross-checked against the user's private X "Games"
 * list through authorized Android UI inspection on 2026-10-30.
 *
 * IMPORTANT: this is NOT a copy of their private List membership, nor proof of
 * ongoing access. Never upload a private list ID, membership dump, credentials,
 * bearer tokens, or personal social graph to the public source repository.
 */
object GameNewsDirectory {
    data class Source(val gameId: Int, val handle: String, val locale: String,
                      val gameName: String, val sourceType: String = "official_candidate") {
        val publicUrl: String get() = "https://x.com/$handle"
    }
    private val seeds = listOf(
        Source(3, "NIKKE_en", "en", "GODDESS OF VICTORY: NIKKE"),
        Source(4, "bleachbrs_en", "en", "Bleach: Brave Souls"),
        Source(5, "ONE_PIECE_TC", "en", "ONE PIECE Treasure Cruise"),
        Source(6, "honkaistarrail", "en", "Honkai Star Rail"),
        Source(7, "CZN_Official_EN", "en", "Chaos Zero Nightmare"),
        Source(8, "ArknightsEN", "en", "Arknights"),
        Source(9, "houchishoujo", "ja", "放置少女"),
        Source(11, "AzurLane_EN", "en", "Azur Lane"),
        Source(12, "Wuthering_Waves", "en", "Wuthering Waves"),
        Source(14, "Chronicle_En", "en", "E.T.E Chronicle"),
        Source(16, "StellaAffinity", "zh", "Stellar Affinity"),
        Source(17, "tenkafumaE", "en", "TenkafuMA"),
        Source(20, "dokkan_global", "en", "Dokkan Battle"),
        Source(21, "SlimeIM_en", "en", "SLIME IM"),
        Source(22, "shoujokaisen", "ja", "少女廻戦 JP"),
        Source(24, "fgoproject", "ja", "Fate/Grand Order"),
        Source(24, "FGO_clubchaldea", "ja", "Fate/Grand Order"),
        Source(25, "Echocalypse_EN", "en", "Echocalypse"),
        Source(26, "Attackontime_sh", "en", "Shoujo Kaisen Global")
    )
    fun candidateSources(gameId: Int): List<Source> = seeds.filter { it.gameId == gameId }

    /** Only a name/source directory; personal X content is not fetched. */
    fun status(gameId: Int): String = when {
        candidateSources(gameId).isEmpty() ->
            "No X account matched yet. News feed not connected."
        else -> "${candidateSources(gameId).size} likely game-source account(s) identified. " +
            "Reading posts and private List updates requires authorized X access."
    }

    fun isAcceptableHandle(handle: String): Boolean =
        Regex("^[A-Za-z0-9_]{1,15}$").matches(handle)
}
