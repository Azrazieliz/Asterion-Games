# Unified Asterion APK — Implementation Log

**Active implementation branch:** `feature/unified-pilot-codex-20261030`

**User request (2026-10-30):** Merge the Pilot and Game Codex experiences into **one application**, with a clearly understandable user interface. This supersedes earlier plans to make the user launch two separate apps, but the frozen owner-approved architecture and decision documents remain **unchanged** until an exact textual amendment is explicitly approved.

## One APK

- One installed Android application package: `com.gamepilot`, displaying **Asterion**.
- One home, three simple sections: **Games**, **Automation**, **Game Codex**.
- Original Pilot controller, schedules, 26 game profiles, learning storage and protected resource decision rules remain in place. Detailed per-game settings are accessible on demand, instead of showing dozens of actions per profile on the main screen.
- The **existing Game Codex WebView engine is embedded as an Android activity in the very same APK**, not opened from the separate `net.gamecodex.app` application.
- CI retrieves the **validated Game Codex v1 app-pack artifact** and overlays current Codex UI/JS from a pinned reference commit. Builds fail if the catalogue lacks the verified 520+ characters / 100+ subsystems or the art pack is missing.
- Existing imported Codex game mechanics and artwork are preserved. The app is not a mockup.
- Codex writes account-scoped experimental read-only summaries in the same application's files directory. Pilot reads these directly without the old inter-app provider.
- The previous standalone `net.gamecodex.app` install and all its private data are left unchanged. The user should not uninstall it until its WebView roster and any imported packs have been safely migrated into the new shared app.
- Android signing continuity must be confirmed before any update of an already-installed `com.gamepilot` version. Never uninstall or clear user data merely to install this unified APK.

## Verification table

| Criterion | Status |
|---|---|
| Single-package UI source | Implemented in Kotlin |
| Embedded original Codex activity | Copied from Codex repository |
| Full artwork + 528-character catalogue | CI validation required |
| Android compile and unit tests | CI validation required |
| In-place update on installed Pilot | Signer comparison pending |
| Codex owned-roster migration from old app | Not yet done |
| Same-APK Global/JP character snapshot | Code added; on-device test pending |
| Unattended full game mastery | Not achieved; outside scope of interface fusion |

The frozen specification hashes must continue to pass CI. An existing package's data and authorizations may not be reset for presentation changes.
