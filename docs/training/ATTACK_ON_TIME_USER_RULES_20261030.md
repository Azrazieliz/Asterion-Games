# Attack on Time — User-confirmed gameplay rules (2026-10-30)

This is a game-specific implementation contract. **Do not modify the frozen global purpose/architecture file.**

1. The 15:03 video shows a **complete normal daily session**. The user had earned **all daily rewards by 09:16 of the recording**, despite not finishing every quest. Objective completion must depend on collecting all daily reward tiers, not clearing all daily/weekly quests.
2. Exactly **five recurring event systems** exist, with variable event participation. Exact individual strategies await further user instruction.
3. **Gacha: free draws only.** Never infer that a priced `Draw x10` is free.
4. Weekly shops restock weekly. The recording includes the user's normal beginning-of-week purchases. Exact routine useful-item catalogue remains to be specified.
5. **Martial Arms**: complete the set of distinct pieces before any duplicates, to advance UR+ equipment to MR. The **mainhand purchased around video 08:08** is not to be repurchased if one is owned. Once all pieces exist, duplicates are possible, but **UR+ shards are preferable for the same currency**.
6. Shop daily quest needs **two eligible purchases per day**. **Never use gold**; Astral Legacy Beads are a typical option. Daily purchases and weekly restock are different routines.
7. PvP: defeat is immaterial while the user's rank is already **#4 or better**. If **below #4**, repeatedly challenge the **current fourth-ranked opponent** until the position recovers; identify the live opponent, not a hard-coded name.
8. AFK ad is **user-initiated at the 60-hour maximum idle accumulation** to double bonus. Close advertisements after viewing; if redirected to Google Play, return to the game and finish the reward claim. Do not tap Install.
9. Hero/equipment upgrade actions in the recording served daily quests. Reinforcement is complex; its exact method awaits the user's next instructions.
10. Resource allocation is important and highly specific. **No allocation strategy has been authorized yet.**
11. Cross-server rankings/competitions have schedule- and reward-specific behavior. Times and strategy await the user's specifications.
12. **No fixed session time budget**. Dailies usually last 15–20 minutes excluding events, with farming potentially longer.
13. Required integration: **Asterion DB (game codex)**. Repository/schema/connection are not established; do not confuse this with the unrelated character-database workbook.

### Separate alarm screenshot

The image is labeled **Shoujo Kaisen**, not Attack on Time, and displays three enabled alarms: **Mine war! 00:50**, **Mine war 19:43**, and **Cross server pvp 23:50**, with differing weekday selections. **These are not imported as Attack on Time times.**

### Implementation status

`AttackOnTimePolicy.kt` holds tested, deterministic decisions for all-rewards daily completion, free draws, non-gold daily shop purchases, weekly set-piece ownership and UR+ shard preference, rank-four PvP, and the 60h ad trigger. **These are policy decisions only**. The current live-perception controller does not yet reliably extract the needed price, shop stock, inventory set, AFK timer, or opponent rank, nor execute every sequence on-device. Do not report unit-test success as completed autonomous gameplay.

### User correction to earlier video chronology
The user specifically identified the intentionally initiated AFK doubling advertisement at approximately **03:35**. Earlier image-sampling timestamps did not establish the button's activation time. Ads must not be treated uniformly as unsolicited interruptions; the AFK ad is purposeful **only after the 60-hour maximum**.
