# Asterion Pilot × Game Codex — Progress Versus Frozen Master

**Mutable evidence ledger**, current snapshot **2026-10-30**. This file is intended to be **updated on every substantial development batch** without rewriting either frozen master. All status claims require a commit, build, test, device observation or source evidence.

**Immutable authorities:** `CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md`; `MASTER_DECISIONS_FROZEN_20261030.md`. Never edit them without user authorization for the exact diff.

## Current status matrix

| Frozen acceptance criterion | Implemented source | CI verified | Android device verified | Still missing |
|---|---|---|---|---|
| 26 game profiles / independent context | Yes | Prior builds yes | No | Live gameplay in each |
| Direct game input | Accessibility taps/swipes | Prior builds yes | Not validated against games | Unattended real-control validation |
| Live screen perception | In-memory projection, OCR, accessibility tree | Prior builds yes | Not validated against full 26 | Android capture consent and full visual reasoning |
| Reliable unattended schedule | Alarms, persisted jobs | Prior builds yes | No | Keyguard/background startup constraints |
| Learn and retain verified play routes | SQLite policy/graph & episode log | Yes for unit scenarios | No | 26 real game traces and tactics |
| Shoujo daily objectives | Partial task ledger and policy | Yes for decisions | No | Actual UI completeness verification |
| Global/JP separate game identity | Pilot IDs 26 and 22 | Yes | No | Account-scoped roster and rule verification |
| Dispatch value optimization | Locks, refresh limits, reward model | Prior tests yes | No | Live icons, ROI forecast, dispatch execution |
| Resource reserves/shop rules | Pure decision guards | Prior tests yes | No | Live resource-reading and per-item authorization |
| Game Codex synchronization | Not yet on current main | — | No | Two branch-preserving signed Android integration changes |
| Game Codex hero grading | Not implemented | — | No | Verified owned roster, mechanics, grading constraints |
| Competition/game events | Awaiting user evening recording | — | No | Detailed strategy, timing and tests |
| Consistent update signatures | Debug-key cached in CI | Two CI fingerprints matched | Not validated in-place | Production signing |
| Frozen master hash enforcement | Original canonical CI guard | Passed | Not applicable | Add second master hash guard, GH required status checks |

## Development history

- **2026-10-29:** Frozen canonical product purpose and architecture created, archived branch and CI hash check. Android prototype and profiles.
- **2026-10-30:** Direct-control agent, multi-screen transition learning, job recovery and logging; game-26 15-minute session studied.
- **2026-10-30:** User-approved rules for Shoujo daily rewards, free-only draws, weekly shop, rank-four PvP, 60-hour AFK ad, dispatch, protected resources, extractor and farming implemented as tests/partial guards.
- **2026-10-30:** GitHub Codex repository identified: `Azrazieliz/Game-Central-Database`, first adapter Shoujo Kaisen. Integration to be built in two **separate branches**, with neither main replaced.

## Verified vs incomplete

All earlier reported APKs are scaffolding, not autonomous game-mastery verification. The GitHub main version before integration branch is Pilot 0.3.3. The Game Codex Android WebView uses local owned roster state and a source-independent experimental ranking engine; the two apps do not yet have an installed live communications bridge.

**Next tests:** two-repository bridge schema compatibility, CI compile on both branches, live package presence by ADB, manual-independent Android provider query once the updated Codex APK exists, cross-account isolation, and game-26 run result verification. Record actual success/failure here rather than declaring completion when code alone exists.

## Blockers

- No source-grounded guaranteed return forecast for random paid dispatch refresh. "Way more than 50" has no precise owner-selected multiple; reserve gold until forecast clearly and reliably exceeds the 50-gold cost by a significant margin.
- No verified complete owned equipment/inventory for hero grading.
- JP differs from Global in mechanics/refresh limits; preserve separation.
- Screen automation must not modify or erase an unrelated Android installation.
