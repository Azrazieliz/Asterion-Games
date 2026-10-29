# Asterion Pilot — Android Autonomous Game Agent

**Canonical product purpose:** Read [the frozen specification](docs/CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md). Its purpose and architecture may not be changed without user authorization of the exact modification.

**Current implementation (v0.3.0):** An Android game automation foundation with direct Accessibility gestures, on-device live display perception, a scheduled queue, per-game objectives and persistent local learning. This code has not yet demonstrated autonomous mastery of the 26 games.

## Application systems

- 26 game profiles from the supplied Android launcher; additional profiles can be added.
- Automatic linking by installed package name/launcher title when unambiguous; per-game schedule and configurable daily reset hour.
- Direct gesture execution, continuous in-memory MediaProjection/ML Kit Latin/Chinese/Japanese recognition, and Accessibility UI-tree perception when exposed by Android.
- A per-game goal ledger (rewards, dailies, repeatable farming) with explicit post-action result evidence; state changes alone never count as success.
- Local learning using context-specific action scores, a SQLite experience graph and verified multi-screen path planning. Experience traces are inspectable and portable, without saving screen-image files.
- Persisted alarm tasks with retry cooldown, limited crash recovery, battery wake handling and Android orientation-aware capture.
- Game-specific navigation cues for identifiable titles, sourced from public guides and distinguished from completed mastery.
- User-controlled export/restore of profiles, learned trajectories and task history.
- Automated Gradle Android build + unit tests and frozen-canon integrity verification on GitHub Actions.

## Installation and build

Source: `app/`. Java 17, Android SDK 35 and Gradle 8.11.1.

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

The CI workflow publishes an Android `app-debug.apk` and `SIGNING_CERTIFICATE.txt` in an artifact named `AsterionPilot-debug-apk`. It uses a cached, explicit **debug** signing key. This cache is not equivalent to a stable production signing secret; compare fingerprints before updating an existing installation. See [signing notes](docs/APK_SIGNING.md).

## Status and remaining blockers

Read [implementation status](docs/IMPLEMENTATION_STATUS.md) for verified vs missing features.

- Real game-specific tactics and objective completion remain untrained and unverified for all 26 titles.
- Stock Android may demand explicit MediaProjection permission for each new screen-sharing session, limit background app launches, and block interaction behind the keyguard. The app does not silently circumvent system authorization.
- Gameplay must be measured on a real Android device before claiming full unattended completion.
- Actual game accounts, inventories, stamina, reward claims and training demonstrations are not part of the source repository.

**Source compilation and unit tests do not constitute a proven autonomous game-playing product.**
