# Asterion Pilot — Verified Implementation Ledger

This operational document is **not** the frozen purpose and architecture specification at
`docs/CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md`. Changes here do not amend user canon.

## Current source implementation

| Capability | Implementation evidence | Demonstration status |
|---|---|---|
| Initial 26 Android games; later games optional | `Models.kt`, `InstalledGames.kt` | Registry code compiled; devices not tested |
| User schedules and daily reset windows | `Schedule.kt`, `Goals.kt`, `DailyResetTest.kt` | Unit tests pass; Android power behavior untested |
| Unfinished alarm-job persistence with retry | `ScheduledJobs.kt`, `GameAccessibilityService.kt` | Built; no crash/reboot device validation |
| Direct screen gestures | `GameAccessibilityService.kt` | Built, real gameplay untested |
| Live perception | `LiveVisionService.kt`, `UiTreePerception.kt` | OCR + direct tree live in-memory; user-consent limits remain |
| Multilingual game navigation knowledge | `GameKnowledge.kt`, `GamePlaybooks.kt` | Navigation seeds for several identified games, not mastered routines |
| Game objective tracking with actual result evidence | `Brain.kt`, `Goals.kt` | Generic two-observation evidence only; game-specific verification incomplete |
| Multi-step strategy learning | `RoutePlanner.kt`, `ExperienceGraph.kt`, `RoutePlannerTest.kt` | Multi-step graph planning test-verified; real-game utility unvalidated |
| Episode/trace capture, local only | `EpisodeJournal.kt` | Integrated with agent; device testing pending |
| Export and restoration | `GameDataVault.kt`, `MainActivity.kt` | Implemented; needs interrupted-import and device tests |
| CI debug signer continuity | `.github/workflows/android.yml`, `docs/APK_SIGNING.md` | Debug-key cache, certificate report; **not** stable production signing |
| Canon protection | `.github/workflows/canonical-spec-integrity.yml` | CI integrity test passes; server-side ruleset absent |

## Specific architectural gaps still blocking canonical acceptance

1. **Full game mastery is not implemented.** No full tactics, battle agents, combat timing, per-game reward recognizers, stamina spending models, complete daily workflows or optimized farming policies have been validated on the user account in any of the 26 games.
2. **Unattended device operation is not proved.** On stock Android, new MediaProjection sessions require user consent. Background launching and locked-screen interaction may be restricted. Accessibility tree data cannot show the internals of most GPU-rendered games.
3. **Device behavior is untested.** No verified Android installation/ADB gameplay run, screenshot-free full workflow, rotation recovery, crash/reboot test or 26-title automatic tour has occurred.
4. **Task completion remains provisional.** Two repeated OCR success cues can support a result, but are not a semantic proof that all subgoals or account-specific tasks were actually completed. The ledger must not mislabel these as complete mastery.
5. **Long-term update signing remains blocked.** GitHub cache may preserve a debug certificate temporarily; a secure production keystore stored privately is required for reliable upgrade compatibility.
6. **On-device learned model training is absent.** Episode traces and route reuse can improve local heuristics, but no game-aware perception/planning model is trained from real game recordings.

## User-defined target

A fully autonomous scheduled Android player that completes daily activities, farming and other assigned tasks for 26 games, with persistent game mastery, direct screen access, zero routine user intervention, and evidence that the work was actually completed. That target is unchanged and **has not yet been reached**.

## Training data path

The current implementation stores action/outcome observations in local SQLite without writing screen-image files. After real game sessions are available, these traces can ground stronger game-specific planners and task completion rules. A successful Gradle build is never a substitute for device or game-task verification.

## References used for first navigation seeds

- Honkai: Star Rail dailies/assignments: https://hsr.keqingmains.com/misc/beginner-guide/
- NIKKE daily content: https://nikkehub.com/en/guide/newbie/daily-contents
- Arknights daily mission examples: https://arknights.wiki.gg/wiki/Mission/Daily

These guides describe general tasks, not the user's current account state or an implementation of successful autonomous gameplay.
