# Asterion Pilot × Game Codex — Progress Versus Frozen Master

**Current verified snapshot:** 2026-10-30. **This is an updatable evidence ledger**, not permission to modify frozen masters.

**FROZEN / UNMODIFIABLE WITHOUT EXACT USER AUTHORIZATION:**
- [Canonical purpose/architecture](CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md)
- [Frozen decisions master](MASTER_DECISIONS_FROZEN_20261030.md)

## Repositories / integration branches (do not merge blindly)

| Component | Repository | Integration branch / source | Last successful validation |
|---|---|---|---|
| Pilot Android controller | Azrazieliz/Asterion-Pilot | `feature/pilot-codex-bridge-20261030` | GitHub Actions run `41088298408` — **PASS** (latest full advisor + UI + frozen-spec checks) |
| Game Codex Android data source | Azrazieliz/Game-Central-Database | `feature/codex-pilot-export-20261030` | GitHub Actions `41087528023` — **PASS**; complete 528-character / 102-subsystem pack preserved |
| Frozen master baseline | Asterion-Pilot | `main` + `canon/frozen-decisions-20261030` | Git blob `7ef195e4f8cf052ed5fc848d3b5604ca6758f46f` |
| Original primary architecture | Asterion-Pilot | `canon/frozen-purpose-architecture-20261029` | Git blob `980da9decedec45e004a896ecb476c78d2bdcfaf` |

## Feature completion against approved architecture

| Area | Code | CI | Real Android device | Remaining |
|---|---|---|---|---|
| 26 game profiles | Implemented | Previously passed | No gameplay verification | Per-game strategies |
| In-memory screen perception / direct gestures | Implemented | Previously passed | Unverified inside games | Live scene recognition, Android capture permissions |
| Session alarms / job recovery | Partial | Previously passed | Unverified unattended | OS background/lock limits |
| Generic visual policy / experience graph | Partial | Tests passed | No actual 26-game mastery verified | Reliable tactical learning |
| Shoujo resource preservation and dispatch logic | Decision engine + guards | Tests passed | Live execution unverified | Recognize on-screen icons, mission locking, auto assignment |
| Conditional 50-gold refresh | Conservative provisional minimum **250 verified gold** (5×; NOT a user-approved exact ratio) | Tests passed | Not used | Actual future output is unknown: do NOT spend until proven |
| Global / JP separation | Account-specific Pilot IDs and Codex scope keys | VM tests passed | Accounts not populated/verified | Assign real local owned roster safely |
| Cross-app Codex IPC | ContentProvider `snapshot/global` and `snapshot/jp`; Pilot client | Both Android APK builds + JS tests passed | **Pilot installed / Codex bridge not installed** | Codex signer mismatch blocks in-place update; real provider query pending |
| Codex experimental scenario advice | Read-only advisory UI on Pilot integration branch | GitHub Actions `41088298408` — **PASS** | No | Real account-owned data/validation, combat optimizer |
| Hero grading from Codex | Intentionally not authorized | Conservative guard | No | Source proof plus owner reinforcement methodology |
| Shoujo events/competition | Awaiting owner gameplay | — | — | Time-specific competition tactics |
| Data retention | Pilot export/import; Codex legacy roster preserved | Tests passed | Device version has live Codex data | Secure in-place update or verified backup+restore |
| Master integrity | Frozen original + decision master hash | Main CI passed | N/A | Server-enforced required ruleset not available in connection |

## Device inspection through authorized ADB (non-destructive)

Device: Samsung Android 17, ADB `R3GYC0LSC7K`, connected via authorized remote workstation.

- Installed Game Codex: `net.gamecodex.app`, version **1.0.0**, versionCode **100**, debug-build debuggable, unchanged.
- Installed Shoujo Kaisen Global: `com.onemt.and.shoujokaisen`, version **1.0.144**, unchanged.
- `com.gamepilot` **installed non-destructively via authorized ADB** from the integration branch; Android reports version 0.3.3 / code 6. Activity launch succeeded. No gameplay or connected provider query validated.
- Current Codex certificate SHA-256 `b5390147b2816ef765f8108682426071d26f804d25274aa1d79a0c8cb5f1cdc6`. New Codex bridge certificate SHA-256 `b7a52cd78eba70511098848c6fd1c62c3559314a7ce3576511abc81f66d03059`. It **differs from the installed Codex certificate**; in-place APK update is not possible. Existing Codex untouched.
- The existing Codex private files are visible through `adb run-as`. **No uninstall, reset, or data clearing performed.**

## Recent implementation commits

- Pilot `main`: `f007a1c19b72` creates frozen decisions + live progress ledger; `d87642885de9` adds second hash integrity gate.
- Pilot bridge `7b9cac548f45`: Android read-only Codex client, Global/JP isolated snapshots, policy requiring high-margin dispatch rewards.
- Game Codex `4f5ab43c21e6`: read-only Android provider, scope-specific local roster publishing, VM isolation tests, and full validated data/art preserving CI package.
- The original Game Codex `main` is unchanged by integration development.

## Next dependencies, not completed

1. **Done:** Pilot integration branch built and installed. Verify full app controls and permission flow without activating risky autonomous game actions.
2. Compare built Codex signature with installed Codex signature; **do not overwrite** old app with incompatible signer. Preserve full WebView roster if an explicit migration becomes necessary.
3. Test native provider query between real installed updated APKs and account scope switching.
4. Use actual game UI readbacks to populate live resource counts, ranks, dispatch returns and Codex roster; do not guess.
5. Collect competition schedule, guild war and hero reinforcement rules from the owner's recording; test repeatable autonomous in-game tasks.
6. Validate true zero-touch Android wake/launch/capture integration or report limitations accurately.

## Reporting rule

Do not claim “apps fused and working on-device” merely because both compile. Explicitly distinguish **committed, CI-passed, installed, inter-app tested, gameplay verified**.
