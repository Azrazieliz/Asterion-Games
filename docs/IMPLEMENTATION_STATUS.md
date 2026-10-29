# Asterion Pilot — Implementation Status (separate from locked canon)

This is an operational implementation record. **It does not amend or replace** `docs/CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md`.

## What has genuinely been implemented

- 26 launcher-title profiles, installed-game auto-matching, editable per-game scheduled time.
- A direct AccessibilityService gesture actuator for taps and swipes.
- In-memory live MediaProjection frame capture plus Latin, Chinese, Japanese OCR. Source code includes frame downscaling and resize handling. It does not save screenshot files.
- An Android alarm queue with persistence for scheduled handoff, and bounded task timeouts.
- Three classes of goals per game (rewards, daily objectives, farming), configurable run targets, extra UI keywords, independent local-day progress ledgers.
- A policy learner for choosing text-related actions, with a small scene-change reward, and strict separation of attempted actions from evidence-verified completion.
- A two-distinct-frame verification step for a new contextual success cue after an action. This is conservative but is **not a perfect semantic verifier**.
- A persistent SQLite transition graph keyed by game, objective, scene, and action. It stores successful routes (not image files) and supports limited reuse of trajectories that have led to verified outcomes.
- CI builds debug APKs and tests selected goal/label behavior.
- Frozen canonical purpose file with passing integrity checks and a separate archival branch.

## Outstanding for the user's required result

- **Zero-touch scheduled sessions across phone sleep/lock/Android capture restrictions**: not satisfied on stock Android. Android 14+ requires user consent before each new MediaProjection capture session. Background activity launching and secure keyguard also restrict behavior. The app does not bypass these requirements and cannot claim full unattended launch capability without a legally authorized higher-privilege integration or an already-active projection and accessible display.
- **True mastery of 26 games**: not achieved. No model has been trained for those games; no in-game tactics, state machines, drop-rate optimization, story progression, resource/stamina accounting, combat decisions or validated navigation graphs are available for all titles.
- **Game-specific verification**: Generic result text in a game can indicate one stage cleared rather than an entire daily goal; the current rules remain partial. Per-game objective definitions and observational tests are required before reporting success.
- **Real Android device gameplay verification**: not yet done. Compilation and unit tests do not establish on-device control reliability.
- **Stable production signing**: CI debug signatures are runner-dependent. A persistent secure signing key must be established before treating incremental APK installation as reliably updatable. Never publish a release private key in this public repository.

## Safety/integrity considerations

Game automation must not trigger unintended purchases, gacha, irreversible account controls, or unrelated app taps. The current text guard is not comprehensive; it does not certify safety for all graphical in-game controls. Scripted user actions bypass OCR guard and require separate review.

Logs must never equate a successful gesture or changed scene with an objectively completed daily or farming run.

## Android primary references

- https://developer.android.com/about/versions/14/behavior-changes-14
- https://developer.android.com/guide/topics/ui/accessibility/service
- https://developer.android.com/guide/components/activities/secure-bal
