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

## New proposals (not user-approved canonical requirements yet)

1. **Daily reset/event calendar engine:** different account timezones/server resets, claim deadlines, Mine War and settlement windows; priority interrupts by urgency.
2. **Resource safeguard and audit:** automatic cost/value calculations, reserved-material ledger, post-spend delta confirmation, reversible action history where game allows.
3. **Failure and recovery lab:** failed-screen clips/transition graphs stored locally for opt-in diagnosis, state-driven retry and no blind loop.
4. **Account snapshot diff:** detect newly acquired heroes, unspent energy, expiring materials, shop refreshes and roster changes; update Codex evidence.
5. **Patch impact engine:** compare current verified mechanics with new official updates and **invalidate stale advice** when balance or reward tables change.
6. **Cross-game scheduler:** account/game priority from reward expiry, expected duration, battery/thermal availability and competition times.
7. **Evidence confidence UI:** distinguish known, suggested, unverified and contradicted data with timestamp/source and one-tap correction.
8. **Safe learn/test mode:** trial runs of game-specific routines on the real device with automatic intervention before irreversible decisions.
9. **Selective notification digest:** urgent timed competitions and limited-time rewards immediate; ordinary updates in a single daily summary.
10. **Signed backup + migration:** preserve existing Pilot/old Codex private data, signing identity, migration audit and rollback before replacing any installation.

## Current implementation truth

- `GameNewsDirectory.kt`: **implemented as a static set of public source candidates**; unit tested. The private 54-member list remains private.
- X OAuth and automatic list polling: **not implemented**. Account-level authentication/plan access must be established without scraping account credentials.
- Source feed ingestion, dedupe and important-alert delivery: **not implemented**.
- Push/notification Resume/Pause/Off controls: **not implemented**; only in-app master controls committed.
- Unified UI and controller improvements: **committed, pending latest APK compilation and device validation**.
- Neither frozen master was edited as a consequence of this roadmap.
