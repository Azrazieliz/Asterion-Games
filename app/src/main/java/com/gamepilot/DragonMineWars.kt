package com.gamepilot

/**
 * Dragon Mine Wars / Draconic Mirage, Global Attack on Time.
 * Grounded in 2026-10-30 19:50:58 video; JP is NOT assumed identical.
 * No battle, expenditure or occupation is considered successful without a
 * new in-game observation explicitly confirming it.
 */
object DragonMineWars {
    const val GLOBAL_ID=26
    const val OBSERVED_SLOTS=5
    const val OBSERVED_COLLECTION_METER_MIN=300
    enum class Screen { OTHER, MAP, INFO, FORMATION, BATTLE, VICTORY, DEFEAT, HELD_MINES }
    enum class Resource { RED_SHARDS, CYAN_ICON_RESOURCE, UNKNOWN }
    enum class Action { STAY, INSPECT, ATTACK_WITH_VERIFICATION, CONSIDER_REPLACEMENT, REQUIRE_EVIDENCE }

    data class Mine(
        val id:String, val reward:Resource, val ratePerMinute:Long?,
        val ours:Boolean, val defenderPower:Long?=null,
        val collectionElapsedMin:Int?=null, val collectionLimitMin:Int?=null,
        val plunderable:Long?=null
    )
    data class Formation(
        val heroes:List<String>, val power:Long?, val alreadyGarrisoned:Boolean,
        val rulesVerified:Boolean, val qualifyingBuffHeroes:Int=0
    )
    data class State(val held:List<Mine>,val occupiedSlots:Int?,val slotLimit:Int?,
                     val availableFormations:List<Formation>)
    data class Plan(val action:Action,val mineId:String?,val reason:String,
                    val sameCurrencyDifference:Long?=null)

    fun identify(values:Collection<String>):Screen {
        val s=values.joinToString(" ").lowercase()
        return when {
            ("victory" in s || "you win" in s) && "enemy" in s -> Screen.VICTORY
            ("defeat" in s || "you lose" in s) && "enemy" in s -> Screen.DEFEAT
            "battle report" in s && ("round" in s || "damage" in s)->Screen.BATTLE
            "cross-server occupation" in s || ("attack assist" in s && "challenge" in s)->
                Screen.FORMATION
            "my dragon mine" in s && ("total efficiency" in s || "go" in s)->Screen.HELD_MINES
            "dragon mine id" in s || ("garrisoned troops" in s && "collection" in s)->
                Screen.INFO
            "dragon mine wars" in s || "draconic mirage" in s ->Screen.MAP
            else -> Screen.OTHER
        }
    }
    fun requiresPlanner(gameId:Int,values:Collection<String>):Boolean =
        gameId==GLOBAL_ID && identify(values)!=Screen.OTHER

    fun verified(m:Mine):Boolean=m.id.isNotBlank() && m.ratePerMinute!=null &&
        m.ratePerMinute>=0 && m.reward!=Resource.UNKNOWN &&
        (m.collectionElapsedMin==null || m.collectionElapsedMin>=0) &&
        (m.collectionLimitMin==null || m.collectionLimitMin>0)

    /**
     * The screenshot shows a 05:00:00 collection progress bar. This is NOT
     * evidence that the event itself ends every five hours.
     */
    fun yield(m:Mine,horizonMinutes:Int):Long? {
        if(!verified(m) || horizonMinutes<=0)return null
        val available=if(m.collectionLimitMin!=null && m.collectionElapsedMin!=null)
            (m.collectionLimitMin-m.collectionElapsedMin).coerceAtLeast(0)
        else horizonMinutes
        val minutes=minOf(horizonMinutes,available)
        val rate=m.ratePerMinute!!
        if(rate>Long.MAX_VALUE/minutes.coerceAtLeast(1))return null
        return rate*minutes
    }
    /** Rank only within the same resource, never inventing an exchange rate. */
    fun bestByResource(candidates:List<Mine>,minutes:Int):Map<Resource,Mine> =
        candidates.filter(::verified).groupBy{it.reward}.mapNotNull{(resource,mines)->
            mines.maxWithOrNull(compareBy<Mine>{yield(it,minutes)?:-1L}
                .thenBy{it.id})?.let{resource to it}
        }.toMap()

    fun buffPercent(count:Int):Int?=if(count in 0..6)20*count else null

    /**
     * Unknown enemy, uncertain formation, full garrisons or conflicting
     * resource types cannot justify blind aggression. Higher recorded
     * defender power is a necessary *not sufficient* condition to attack.
     */
    fun decide(state:State,target:Mine?,resource:Resource,minutes:Int):Plan {
        if(state.occupiedSlots==null || state.slotLimit==null ||
            state.slotLimit !in 1..20 ||
            state.occupiedSlots !in 0..state.slotLimit)
            return Plan(Action.REQUIRE_EVIDENCE,null,"Mine slot count not verified")
        if(target==null || !verified(target) || target.reward!=resource ||
            resource==Resource.UNKNOWN || minutes<=0)
            return Plan(Action.REQUIRE_EVIDENCE,target?.id,
                "Target resource, rate, identity or collection horizon unknown")
        if(target.ours)return Plan(Action.STAY,target.id,"Already our mine")
        val eligible=state.availableFormations.filter{
            !it.alreadyGarrisoned && it.rulesVerified &&
            it.heroes.isNotEmpty() && it.heroes.size<=6 &&
            it.power!=null && target.defenderPower!=null &&
            it.power>target.defenderPower
        }
        if(eligible.isEmpty())return Plan(Action.REQUIRE_EVIDENCE,target.id,
            "No verified available formation with a favorable observed power comparison")
        if(state.occupiedSlots<state.slotLimit)
            return Plan(Action.ATTACK_WITH_VERIFICATION,target.id,
                "Eligible free slot; require combat victory and ownership proof")
        val old=state.held.filter{it.ours && verified(it) && it.reward==resource}
            .minByOrNull{yield(it,minutes)?:Long.MAX_VALUE}
            ?:return Plan(Action.REQUIRE_EVIDENCE,target.id,
                "All slots filled; cannot compare different resource currencies")
        val next=yield(target,minutes)
        val previous=yield(old,minutes)
        if(next==null || previous==null)return Plan(Action.REQUIRE_EVIDENCE,target.id,
            "Collection outcome missing")
        if(next<=previous)return Plan(Action.STAY,target.id,
            "Current same-currency mine is equally/more productive")
        return Plan(Action.CONSIDER_REPLACEMENT,target.id,
            "Potential same-currency improvement; withdrawal mechanics and attack still need verification",
            next-previous)
    }

    fun overview():String="""
        Dragon Mine Wars/Draconic Mirage (Global).
        Recorded five active mine positions and collection progress 05:00:00.
        Rewards are RED shards/min or cyan-icon resources/min, kept as separate units.
        A mine panel offers 50% plunder from accumulated resources.
        Mine-specific named heroes grant +20% stackable attack buffs.
        A six-unit offense screen and requirement icons were observed.
        The five-slot limit and each visible collection rate must be read
        from the current UI; do not invent timers or resource conversions.
        Battle reports and Victory must confirm occupation before claiming
        the mine was gained. Existing teams may be garrisoned elsewhere.
        At the end of the recording: 5 mines; +941 red/min and +2159 cyan/min.
        The displayed efficiency is an observed snapshot, not a fixed target.
    """.trimIndent()
}
