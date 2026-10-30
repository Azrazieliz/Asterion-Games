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

## Unified single-APK implementation — 2026-10-30

The owner has explicitly instructed that Pilot and Game Codex become **one app**, with a straightforward interface. This supersedes the earlier two-app runtime/IPC plan. The frozen master files themselves remain unchanged pending an explicit exact amendment.

- **Source branch:** `feature/unified-pilot-codex-20261030` (Pilot repo).
- **One installed package:** `com.gamepilot`, version `0.4.0-unified`, UI label **Asterion**.
- **Main screen:** three clear sections: Games, Automation and Game Codex; individual game details and advanced settings on demand rather than dozens of buttons shown simultaneously.
- **Full Codex:** original Java/WebView engine included inside the Pilot APK with the original validated 528-character, 102-subsystem, 1,170-artwork asset pack restored from the Codex app-pack artifact. This is not a remote web page or an empty placeholder.
- **Shared local data:** Codex publishes account-scoped snapshots into its **same app sandbox**; Pilot reads these directly. External IPC provider is no longer needed for the unified installation.
- **Unit and APK CI:** build `41095443257` succeeded after Kotlin fixes, full pack restored; compatible signer build `41096017884` succeeded.
- **Installed certificate match:** signed upgrade SHA-256 `060d52c4d8333f6b81c7cca6d8414e1a9f8a487ca0fe32fa5bbc2c34987c112a`, identical to the installed Pilot v0.3.3 certificate verified by ADB and apksigner.
- **Pre-upgrade Pilot backup:** `D:\AsterionPilotAnalysis\pilot-before-unified-20261030.tar`, 12,896 bytes, SHA-256 `688fa27ac27ebed072f0b78ba79da8711171d2ae65373ac3abb7dfb1d98b61c6`.
- **Original Game Codex:** separate previously installed `net.gamecodex.app` left untouched. Its data backup still exists; automatic migration of its WebView account ownership to the unified sandbox has **not** been demonstrated.
- **On-device acceptance:** APK compilation and signature verified, but final UI tap-through, Codex content rendering, migrated personal roster and background automation have not yet been validated at the time of this entry.
- **Master integrity:** no frozen owner-authority file edited.
- **Remaining acceptance:** device screenshot/layout test, real Codex catalogue opening, account-data migration if any exists, complete mastery and unattended game testing.

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

## Validated non-destructive Codex data backup (2026-10-30)

With the original debuggable Codex installation intact, ADB `run-as net.gamecodex.app` was used to create a local archive on the user's authorized computer:

- `D:\\AsterionPilotAnalysis\\Codex-installed-v1.0.0-ADB-backup.tar`
- **118,795,264 bytes**; **1,232 archive entries**; contains `files/codex/index.html`, `app_webview`, `shared_prefs` and `databases`.
- Archive SHA-256: `0fe3e8ed43aa6404b1dbc8849a92458ef6ccc8c84aa6fbaa487016f6a9172442`
- The archive is on the authorized PC, **not committed to GitHub** and **not uploaded to conversation storage**.
- Contents inspected for presence and integrity of archive members. **Full round-trip restore has not been tested.**
- No uninstall, app-data clear or changes to the Codex installation occurred. Re-signing remains blocked by incompatible signer; an explicit replacement/migration decision is still required before destructive steps.

## On-device Pilot installation

- Pilot `com.gamepilot` was installed with `adb install -r` — **Success**, Android `versionCode=6`, `versionName=0.3.3`.
- The app's `MainActivity` launched, exposing native Android controls. A notification permission dialog appeared, and the interface responded to navigation; no game runs or automation services were enabled.
- UI observation alone does **not** verify running tasks, real Codex provider synchronization, or gameplay.

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

## 2026-10-30 — User-requested single-app account UX and safety revision

- Development branch: `feature/account-ui-safety-meta-news-20261030`; fused Codex source pinned to `94cdd3d7` on its independent repository branch.
- Durable global automation master switch implemented and surfaced prominently at top; disables schedules, pending jobs, active controller and live perception. Requires Android device validation, including reboot test.
- Account-centric Games screen and separate Shoujo Kaisen Global / JP panels. Auto-identification uses two identical recognized player IDs from consecutive live observations, no passwords/session tokens and no cross-account merging. Unverified account association is correctly marked not linked.
- Per-account Codex opening uses regional context. Personal imported Codex roster migration is still incomplete.
- Invalid/unsourced meta rankings no longer displayed as factual rankings in Codex. Evidence ledger stores verified account-specific team outcomes, minimum 12 records before giving an estimate, and no invented character-strength attributions.
- Game news section created with real X navigation, but personalized X feed and notifications are **not connected**. Official OAuth app credentials and account authorization are required; no fake connection claimed.
- User has deferred branding to a later stage. This is structural UX functionality, not approved final visual branding.

## 2026-10-30 — Distinct Pause / Resume / Off clarification

Per owner's explicit clarification, "Pause" is **not** a synonym for turning all automation off. Changes on this branch:
- **Pause** suspends all gameplay inputs, retains active/queued tasks in the persistent job ledger and keeps existing future alarms. It does not erase task progress or terminate live-perception permission.
- **Resume** restores pending jobs and re-evaluates the current game screen before choosing actions; user-requested Resume ignores the generic 10-minute crash-retry cooldown.
- **Turn off** is a stronger explicit command: cancel alarms, pending tasks, active controls and live perception; persists until turned back on.
- Header offers two clearly distinct controls when running (Pause and Turn off), Resume and Turn off when paused, and Turn on when off. A two-row compact Accounts / Automation / News / Codex navigation prevents clipped labels in split-screen.
- These changes are **committed but require compilation and on-device QA**. A running screen-capture test and interrupted-combat recovery are not yet verified.

## 2026-10-30 — Accepted UI/news improvements and discovered X source list

User explicitly accepts notification controls, per-game accounts, evidence-based meta, unified game news, automation status dashboard, and robust backups/reason ledger. User's private X "Games" list was located via authorized ADB; the list has 54 members, 41 unique public handles were visually readable, and 18 matching game source categories were seeded as PUBLIC candidate handles only (private membership dump not committed). Public publisher mappings are not equivalent to ongoing private X-list access. Source of truth and status are in `docs/ACCEPTED_FEATURES_AND_X_SOURCE_PLAN.md`. New features suggested there remain unapproved proposals; no frozen document changed.

## 2026-10-30 — Implementation batch v0.4.2 (new actual code)

Branch: `feature/account-ui-safety-meta-news-20261030`. Working source additions, status **committed**:

1. `AutomationNotification.kt`: Android notification shade **Pause, Resume and Turn off** as real explicit broadcast commands. The same persistent state gates the scheduler, accessibility controller and UI. Android 13+ notification permission and live device interactions remain to be verified.
2. `StrategyExperiment.kt`: account + objective scoped verified session outcome metrics (completion rate, elapsed duration, actions). At least five comparable runs per method and no higher failure rate are required to support a faster variant. This is **measured evaluation**, not automatic proof that one game is mastered. Strategy changes are not automatically promoted; existing user selections are preserved.
3. `FailureRecoveryLab.kt`: persistent and explainable repeated-failure holds, avoiding endless autonomous attempts; a game-specific UI control explicitly clears the hold for another try. Verified goal evidence clears a hold automatically.
4. `SafeExploration.kt`: safely inspect explicit read-only Help/Guide/Details screens on previously unexplored non-Shoujo games after no safe objective action exists; actions are logged as research, never credited as completion or permission to spend. No paid action or scripted transaction is run as exploration.
5. Existing `MainActivity` shows measured strategy evidence and game-specific recovery diagnostics. Shoujo Global/JP remain separate.
6. JUnit tests added for strategy outcome comparisons, failure repetition/expiry, and read-only action selection. GitHub compilation and tests must be checked for this exact commit; on-device behavior is not yet verified.

**Do not claim** UI/notification tests or autonomous game mastery completed without actual ADB/device evidence. The former Codex private data migration remains outstanding. Do not modify either frozen master.

## 2026-10-30 — v0.4.2 exact installed-device verification (ADB)

**Verified installation:** Android package `com.gamepilot`, versionCode **9**, versionName **0.4.2-unified**, after signed `adb install -r` over the previous installed Pilot v0.4.0 (versionCode 7). No uninstall, data clear, or replacement of the standalone Codex was executed.

- **Source build:** Git commit `03ac4416ae207bd064785e3d4ce461f50d940603` on `feature/account-ui-safety-meta-news-20261030`.
- **Compatible release build:** GitHub Actions run [41106064433](https://github.com/Azrazieliz/Asterion-Pilot/actions/runs/41106064433), succeeded with JUnit and assembleDebug.
- **Installed-compatible signer SHA-256:** `060d52c4d8333f6b81c7cca6d8414e1a9f8a487ca0fe32fa5bbc2c34987c112a`, checked by CI against the known prior installed Pilot signer.
- **APK SHA-256:** `beb1e7cb03aba5bba6257c7324f23b9f4883670f93c756ce335fb557fe1117ba`. 145,689,155 bytes.
- **UI verification:** Accessibility hierarchy shows account-first home, two-row navigation, Pause and Turn off buttons, and Global and JP as **separate entries**. The Automation state UI is responsive and persistent.
- **Codex verification:** Opening Codex from the unified app stays in **com.gamepilot/net.gamecodex.app.MainActivity** (same installed application), displays original Shoujo Kaisen character portraits and **528 characters**, not a blank mock database.
- **Pause test:** ADB observed SharedPreferences `mode_v2=PAUSED` after the on-screen Pause action. A later UI inspection observed `OFF` while the phone was concurrently being used by another app; no claim is made about who selected Off. The phone's OFF state is **preserved**, not forcibly changed for tests.
- **Notification controls:** Implemented and compiled; interactive action-button behavior from notification shade has not been independently validated.
- **Measured strategy results, safe exploration, repeated-failure holds:** Implemented and JUnit-tested, but real-game execution and effectiveness are **unverified**.
- **X personalized feed and alerts:** Not connected to the user's private List API; game-specific public source candidates only.
- **Global/JP personal Codex roster migration and live hero-account grading:** Still incomplete.
- **26-game autonomy:** Not verified. Compilation, embedded Codex and QA of home screen are not evidence that games can play independently.

**Data continuity:** Existing standalone `net.gamecodex.app` was untouched. The recorded legacy Codex backup remains on authorized PC. Do not delete the old installation or modify either frozen master without owner approval.

## 2026-10-30 — v0.4.3 usability and real news iteration, Mine War unaffected

Owner supplied a 130.55-second screen recording `1000052505.mp4`. Visual analysis shows an app UI tour, **not** Mine War battle footage. The earlier app showed 26 installed profiles but no verified player IDs; Codex's Global personal roster was empty; News only linked out to X; the screen was crowded with diagnostics.

Implemented in `feature/account-ui-safety-meta-news-20261030`:
- `GameAccessibilityService.kt` passively detects an explicitly labelled in-game player UID on manual gameplay window/content changes (no gameplay gestures). If live authorized OCR is available, its recent frame may be used; otherwise only accessible UI data is considered. No guessed UID or hidden game-data extraction.
- Game list distinguishes **installed game** from **verified player UID**. Native installed game icons, short text and contextual actions replace a large technical screen. Global and JP remain separate.
- Natural three-tab bottom navigation **Games / Activity / News**; game Codex is reached via the corresponding account. Diagnostic details remain secondary.
- `ImportantGameNews.kt`: actual periodic background fetching of public Google News RSS results, local account/game story cards, initial on-demand auto fetch, and deduplicated high-impact headline alerts. Public reports are always distinguished from authenticated private X-list access (NOT implemented). Direct account/official X feed access still needs authorization and an appropriate API.
- No phone interaction or game restart was performed during user's imminent Mine War. Gameplay has NOT been autonomously recorded or validated from the app UI video.
- Old standalone Codex backup on the authorized PC contains a 144-byte WebView local-storage log and no obvious `game_codex_roster` key; there is no evidence of a complete old personal roster to migrate. Catalog and artwork remain bundled, account ownership still requires actual game observation.

Status: committed with Android CI pending verification for the latest head. **Do not label the private X feed or complete owned-roster learning finished.** Neither frozen master has been edited.

### 2026-10-30 — v0.4.3 compatible installation evidence

- Android package `com.gamepilot` was updated **in place** through the connected ADB phone, no uninstall or clear-data operation. Verified from Android `dumpsys package`: `versionCode=10`, `versionName=0.4.3-unified`.
- The release APK SHA-256 is `e7e7120b9f1a20430294aacc1ead446a8fae5dc15ddafbe96a1e138f84404582`; release signer SHA-256 is `060d52c4d8333f6b81c7cca6d8414e1a9f8a487ca0fe32fa5bbc2c34987c112a`, matching previous Pilot.
- New game account auto-identification and public news scheduler are built and installed but **on-device account-detection and RSS/network/notification deliveries are not yet independently observed**. Do not claim full X OAuth/private-list monitoring or account inventory complete.
- The user provided an imminent Mine War event; app installation was performed only after checking ChatGPT, not the game, was the foreground activity. No game navigation/control occurred.
