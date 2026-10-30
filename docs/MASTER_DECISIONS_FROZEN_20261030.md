# ASTERION PILOT × GAME CODEX — FROZEN MASTER DECISIONS

**Created:** 2026-10-30  
**Status:** FROZEN — OWNER-EXACT-AMENDMENT ONLY  
**Reference architecture (also frozen):** [CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md](CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md)

## Modification authority (absolute default: NO)

This master is the durable record of approved architecture and specific game decisions. Neither an assistant, code agent, CI job, maintenance refactor, new conversation, nor perceived improvement may **edit, delete, move, rename, replace, supersede, or append to this file** without the owner's prior explicit approval of the **exact text change**. A general instruction to continue development is **not** such approval. New decisions that require canon must first be proposed as an exact amendment and explicitly approved. Keep unapproved discoveries or ongoing results in `PROGRESS_VS_MASTER.md`, clearly labelled provisional, without altering this file.

The immutable Git commit holding the initial edition is an auditable baseline. A content-hash CI check detects modifications; only GitHub server-side required rules would actually prevent edits by arbitrary maintainers. Do not claim server-enforced immutability without verifying those rules.

## 1. Core product

Asterion Pilot autonomously plays scheduled Android games in the user's stead, from complete daily rewards to farming, without routine human interaction, using **direct live screen access** and actual game controls—not screenshot-file-based navigation. The agent must learn, develop game-specific mastery, verify real results, recover and retain learning. The initial 26 games may expand. Current code is a foundation and does not imply game mastery or unattended correctness. Do not weaken the primary frozen architecture to match technical limitations.

## 2. Game Codex integration

- **Pilot repo:** `Azrazieliz/Asterion-Pilot`; **Codex repo:** `Azrazieliz/Game-Central-Database`.
- Preserve their separate codebases, application identities, signing and independently managed branches. Develop integrations on a **distinct integration branch in each repo**. Do not merge either app into the other or overwrite unrelated established systems.
- Data contract: Game Codex remains the **authority for sourced mechanics, characters, skills, compatibility, and experimental analysis**. Pilot remains the **authority for time-based control, observed state, gameplay actions and verified outcomes**.
- Game Codex's source-opinion tiers must never become hidden or explicit inputs into independent Codex rankings. Preserve the existing Codex source/normalized/opinion/analysis/personal-account separation.
- Sync may inform recommendations and planning, **not authorize rare purchases, upgrading, hero grading, or irreversible actions based on uncertain source inference**. Verify actual available account resources in the live game.
- Global Shoujo Kaisen (**Pilot 26, Attack on Time**) and Japanese 少女廻戦 (**Pilot 22**) are **different accounts** with potentially different rules. Their learned inventories, scheduled tasks and personal roster records are isolated. Generic sourced mechanics may be shared with explicit provenance.
- Codex currently contains Shoujo Kaisen only. Expand other games later through their own adapters as proven, not by copying Shoujo data.

## 3. Shoujo Kaisen / Attack on Time

- Original complete daily screen recording: `Screen_Recording_20261030_131430_Attack on Time.mp4`, 15:03, ADB-sourced on 2026-10-30. At **09:16**, all daily reward tiers had been collected. Completing all daily rewards **does not mean completing every available daily or weekly quest**.
- Routine includes five recurring event families that rotate over approximately a week. Detailed event tactics await later game evidence. No fixed 20-minute time cap; normal daily play ~15–20 min excluding events and farming.
- **Gacha:** free draws only; identify zero cost before clicking, not merely by the word "Free".
- **Daily shop:** two useful **non-gold** purchases per day; Astral Legacy Beads normally acceptable. Weekly shop restocks weekly. Only the previously demonstrated useful selection should be bought, and resource balances must not be unnecessarily drained.
- **Martial Arms:** obtain one of each missing set piece before buying any duplicate. Do not repurchase a mainhand already owned. After obtaining the whole set, prefer UR+ shards with the same currency over more duplicate set pieces; future duplicate logic requires account inventory.
- **Ordinary dispatch:** maximize gold, stars and diamond-chest rewards. Classify and **lock** valuable missions before resetting the rest. Allow up to **three free list resets** (global). If justified, up to **one or two 50-gold paid refreshes**, **only when the expected return is substantially larger than the 50 gold cost**. A near-breakeven 51-gold return is **not** acceptable. The owner has not supplied an exact profit multiple or proven reward-forecasting method; do not invent a guaranteed future refresh return.
- Once valuable missions are locked and refreshes are exhausted or not worth using, **Quick/Auto Dispatch** the remaining normal missions without over-optimizing hero selection. Long missions typically complete by the next scheduled session; collect then.
- **Guild War Provisions Transport:** assign only mid-level heroes, defined as **grade below +3 OR not UR+**. Reserve stronger UR+ / MR heroes for actual guild fights.
- **Fishing/Garden:** use available resources and actions, prefer greater rarity if alternatives exist.
- **World Extractor:** compare actual yields with current task bonuses mathematically; choose the greatest obtainable return among equivalent options.
- **Protected currencies:** gold, red/blue stone shards and karma stones may only be spent for explicitly approved purposes/items. Do not assume unknown resource exchanges or available store items are approved.
- **Farming:** story and trials only; normally skip unless resources are low or farming is specifically necessary.
- **Reinforcement dailies:** equipment enhancement, Soul and Over Boost Stone. Hero grading is more complex and must be informed by verified Game Codex mechanics and further approved decisions.
- **PvP:** rank #4 or better is adequate, losses need not be corrected. If below #4, challenge the **current fourth-ranked opponent** repeatedly if possible to regain the rank; never hard-code an opponent.
- **AFK:** wait for the **60-hour global maximum** before initiating a reward-doubling ad. Close ads/Play Store redirects and return to the game; never follow Install links. JP maxima require separate verification.
- **Cross-server competitions:** specific timed windows, strategies and reward optimization. Wait for the owner's gameplay evidence rather than fabricating scheduling rules.
- The screenshot headed "Shoujo Kaisen" includes Mine War alarms at **00:50**, **19:43** and cross-server PvP at **23:50** with weekday selections. They correspond to Shoujo Kaisen, not a separate game, and must not be imported as unconditional daily launches or JP reset times.

## 4. Acceptance standard

An implemented Kotlin decision rule, code commit, passing unit test, APK build, or recorded video does not demonstrate successful autonomous gameplay. Success requires real-device action, correct game-state interpretation, no protected resource waste, verified outcomes, repeated unattended behavior, and recoverability across failures. Progress is tracked independently in [PROGRESS_VS_MASTER.md](PROGRESS_VS_MASTER.md).

## 5. Amendment protocol

Request exact proposed added/deleted/replaced text. Only proceed after the owner explicitly authorizes **that precise amendment**. Preserve the original frozen commit and its hash. No other changes to this master are allowed without another exact approval. Application source, adapters, tests, progress and implementation details may evolve as long as they do not rewrite or contradict this master.
