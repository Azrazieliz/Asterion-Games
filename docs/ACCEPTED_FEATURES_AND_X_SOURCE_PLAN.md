# Unified Asterion — Accepted Improvements and Proposed Extensions

**Updated:** 2026-10-30. **Mutable implementation roadmap**, not an amendment to either frozen canonical document.

## User acceptance — implementation scope

The owner accepted these previously discussed improvements:
1. Persistent notification controls: **Pause / Resume / Turn Off** available during games, retaining queue semantics.
2. Per-game account identity: local profile UID, Global/JP separation, roster/equipment/resources/teams/history and Codex scoped to that account.
3. Evidence-based meta: recent proven PvE/PvP results with sample sizes/confidence and source timestamps; suppress the former invented tiers.
4. Game news: use the owner's existing **private X Games list** as source discovery; game-specific recaps, official patch notices, meaningful alerts. Do not require the owner to rebuild a list.
5. Automation dashboard: what is executing, completed, waiting, blocked, scheduled next, with correct action verification.
6. Data safety: local backups, changelog of approved database corrections, reproducible reason/evidence for important decisions.
7. User clarification: **Pause and Resume differ from Turn Off**. Pause persists pending work and avoids inputs; Turn Off cancels alarms and all scheduled work. Provide visibly separate controls.
8. Global Shoujo Kaisen and Japan Shoujo Kaisen stay distinct accounts, with differing reset windows, shops, inventories and server meta.
9. Branding is explicitly deferred until functional systems and UI are validated.

## X list discovery — private, minimize recorded details

Authorized ADB/UI inspection of the owner's logged-in X app located a **private list named Games**, with **54 member accounts**. In a single pass, **41 unique public account handles** were readable. The remainder were not independently extracted. Membership is private: do **not** commit the complete list, account handle, profile identity, member IDs, session tokens, or list membership dump to this repository.

`GameNewsDirectory.kt` contains only a starter mapping of **public game-publisher/social handles** to already-existing game IDs, based on visible matching descriptions in that list. This is source discovery, **not** a functioning authenticated X news feed. Handles are candidates, not an authorization claim.

For ongoing secure access to the **private** list use owner-authorized X OAuth with appropriate list-read permissions and an API plan supporting list membership/timeline reads. Store tokens in Android protected credential storage; never in GitHub or logs. Official X endpoints:
- `GET /2/lists/{id}/members`
- `GET /2/lists/{id}/tweets`

Fallback: publisher RSS/official patch-note pages and in-app notifications where X is unavailable; do not falsely label public-web sources as the owner's personal X list. Add rate limits, polling limits, deduplication, provenance and expiry.

## User correction on previously suggested "ideas" (2026-10-30)

**Do not re-propose baseline product requirements as optional innovation.**
- Cross-game scheduling and resource evaluation are intrinsic mandatory capabilities. They were mislabelled as novel suggestions.
- Per-account inventory snapshot comparisons are lower-value auxiliary implementation details; do not elevate them as headline features.
- Patch-aware knowledge revision and autonomous gameplay learning are **the entire purpose of the application**, not optional add-ons.
- Failed-session diagnosis, state-driven recovery, and avoiding loops are explicitly useful and accepted.
- Daily/weekly reports of ordinary tasks are not desired. Keep **important game news and genuinely urgent alerts only**; no trivial digests.
- The user already accepted per-game identity, evidence-based meta, relevant game news, meaningful automation status, backups, and separate Pause/Resume/Off. Branding is deferred.

## Distinct additional proposals (NOT automatically owner-approved requirements)

These go beyond obvious scheduling, farming, resource evaluation, patch handling and general learning.

1. **Replay-based regression laboratory:** retain minimal verified state/action traces for each game version and automatically run code/policy changes against those traces before shipping. Detect navigation regressions and unsafe tap selection from patches without repeatedly opening every game. Offline replay cannot prove live Android gameplay, but can catch known breakages.
2. **Competition opponent-behavior intelligence:** learn individual rival occupancy/challenge windows, rank movement, visible formation changes, and time-to-settlement. Test alternative recovery/last-minute strategies where allowed; isolate ranked-game behavior from generic farming. For example Shoujo #4 target and Mine War.
3. **Constrained mechanic experiments:** when the Codex cannot explain a mechanic, formulate a testable hypothesis and propose the lowest-cost reversible in-game action that would discriminate among alternatives. Log observations/contradictions; never spend protected materials or change high-value builds just to learn.
4. **Strategy tournaments with rollback:** compare alternate approved routines over repeated equivalent conditions using actual measured time, reward totals, resource costs and failure rates. Promote only reproducibly superior strategies, version the winner, and revert automatically if performance degrades.
5. **Shadow-validation before autonomy:** run passive observation alongside the owner's normal play, produce an action-prediction confidence score, and verify step-by-step decisions before a particular game/routine becomes eligible for autonomous execution. Do not turn shadow mode into a mandatory obstacle once behavior is demonstrably stable.
6. **Transferable mechanic packages with proof gates:** share learned mechanisms such as dispatch locks, stamina regeneration, timed ranking settlement, and reward-tier collection between *different games or regional versions*, but require UI/rule evidence before reuse; never mix account inventory or assumed timers.
7. **Precision escalation:** automatically isolate the single ambiguous decision that blocks a routine, with the game-screen evidence and a concise choice for the owner; retain the answer as a versioned game rule instead of asking the same question again. Do not spam or interrupt ordinary play.
8. **Evidence conflict adjudicator:** when official patch notes, Codex, community sources, and live observations disagree, detect the contradiction and suspend affected recommendations only. Retain reliable unaffected mechanics, produce a targeted experiment, and track when resolution is proven.
9. **Ad/redirection recovery firewall:** distinguish rewarded ads from store listings, scams/deceptive install UI, loss of foreground game, and unverified purchases; return to the correct game and confirm the reward, without tapping misleading payment/install controls.
10. **Automated gameplay reproducibility packs:** for each new game, generate a portable versioned package of visual states, interaction rules, reset timing, tested routes, objective proofs and minimal evidence to reproduce successes after a device/model upgrade. This is different from a raw account backup.

**Approval status:** These are proposed ideas only, not a modification to any frozen user-approved architecture/master. Existing core requirements remain mandatory.

## Current implementation truth

- `GameNewsDirectory.kt`: **implemented as a static set of public source candidates**; unit tested. The private 54-member list remains private.
- X OAuth and automatic list polling: **not implemented**. Account-level authentication/plan access must be established without scraping account credentials.
- Source feed ingestion, dedupe and important-alert delivery: **not implemented**.
- Push/notification Resume/Pause/Off controls: **not implemented**; only in-app master controls committed.
- Unified UI and controller improvements: **committed, pending latest APK compilation and device validation**.
- Neither frozen master was edited as a consequence of this roadmap.
