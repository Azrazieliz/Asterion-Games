# Asterion Pilot — Android Game Automation

Asterion Pilot is the continuation of the existing GamePilot Android source, now committed to the user's newly created [Asterion-Pilot repository](https://github.com/Azrazieliz/Asterion-Pilot). This is an **in-progress automation application**, not a claimed fully autonomous master of 26 games.

## Implemented in source

- **26 separate game profiles** transcribed from the supplied Android launcher image. Where launcher names are truncated, names remain explicitly incomplete pending on-device package identification.
- Installed-app picker (Android launcher enumeration) and editable per-game package, local daily start time, per-session timeout, adaptive/scripted mode, and optional actions.
- Android AccessibilityService direct on-screen gesture injection (taps and swipes), accessibility text clicks for scripted mode, sequential task queue, stop control.
- Daily alarms, permission-aware exact scheduling, rescheduling on reboot/time/timezone changes; wake/launch attempts and status logging.
- Live MediaProjection **continuous frame stream processed in RAM**, with ML Kit OCR and coarse visual-state fingerprints. No screenshot images or captures are saved, and this does not work by exporting/periodically reading screenshot files.
- An adaptive feedback loop: observe UI words and visual state, choose candidate actions, execute direct gestures, compare later frame state, and persist per-game scene/action preferences. This **does not yet certify completion of quests/farms**, and visual change is only a learning proxy.
- Android GitHub Actions CI for building a debug APK, with artifact named `AsterionPilot-debug-apk`.

## Limitations and correctness

**Android 14+ screen projection requires fresh consent for each capture session.** A scheduled background run cannot silently obtain a MediaProjection permission token. Android may also block unattended game launches from the background and interaction while the device is locked. The app logs those blocks and does not bypass the OS security model. An authorized device-owner/privileged integration would require separate implementation and device evaluation.

**Learning** presently uses OCR cues (such as Claim, Daily, Reward, Start and Japanese/Chinese equivalents), live scene change feedback and optional profile action candidates. It has no trained game-specific reward models, story/battle decision policies, reliable success detectors, account expertise or persistent mastery for the individual 26 titles. The app must not be represented as already performing complete dailies and farming in all these games.

**Do not use for unattended purchases.** The generic policy excludes transaction-related words, but OCR cannot guarantee that all paid/irreversible actions will be recognized. Game terms of service may restrict automation.

## Developer build

Android SDK 35, JDK 17, Gradle 8.11.1 and Android Gradle Plugin 8.9.2. Build `gradle :app:assembleDebug`. GitHub Actions runs on each push to main. No signing secrets are included.

## Future work to reach the requested result

- Map the 26 installed package names and each game's daily/farming goals and reset behavior.
- Replace generic OCR heuristics with game-specific semantic recognition, navigation planners, battle/farming strategies, progress/quest completion confirmation, and saved experience.
- Introduce a privileged **user-authorized** always-on display perception/control path only if required for permission-free unattended sessions; do not misrepresent MediaProjection as capable of that.
- Test actual device launches, language settings, Android 17 limits, rotation handling, task handoffs, battery constraints and crash recovery.
