# ASTERION PILOT — CANONICAL PURPOSE AND ARCHITECTURE
**Classification:** FROZEN / USER-AUTHORITY ONLY  
**Canonical version:** 1.0  
**Established:** 2026-10-29  
**Repository:** `Azrazieliz/Asterion-Pilot`  
**Modification authority:** The user, by explicitly authorizing the **exact proposed change**.

> **DO NOT MODIFY, DELETE, REPLACE, RENAME, MOVE, OR SUPERSEDE THIS DOCUMENT without the user's prior explicit authorization of the specific textual modification.** Neither an assistant, agent, developer, automated refactor, code generator, reviewer, nor build process has authority to change this file by inference, convenience, “improvement,” or reinterpretation. Changes in code or a new design proposal do not constitute consent to amend this specification. This document is the source of truth for the intended product, not an assertion that all features have already been built.

## 1. Purpose — user-defined

Asterion Pilot is an **Android application that plays the user's games in the user's stead**. The user does not have to operate the games while automation is running. At **designated times**, it must **launch and operate the games autonomously**, carrying out **daily activities, farming, and other gameplay needed to accomplish assigned objectives**.

The initial portfolio comprises **26 Android games** shown by the user. The number is a current inventory, not an architectural limit. Each game must be treated according to its own mechanics and requirements. This is not merely a macro recorder, scheduler, menu-clicker, or generic idle tapper.

**Developing mastery of the games is an explicit, indispensable requirement.** The app must acquire and apply game knowledge, learn from its actions and results, improve its competence, and retain useful progress/strategies between sessions. Simply detecting changing pixels, OCR words, or repeating touches is not a substitute for gameplay mastery.

## 2. Non-negotiable functional requirements

1. **Android-native operating target.** The user operates the app on Android; the game-playing capability is intended to operate on the Android device.
2. **Direct, live screen access.** The agent must perceive the actual live game state. **Screenshot-based navigation is expressly prohibited.** A process that saves, retrieves, and navigates from screenshot stills is not an acceptable substitute for direct screen access. Any visual-access implementation must be evaluated against the user's direct-access requirement; implementation terminology alone does not establish compliance.
3. **Direct gameplay control.** The agent must execute the interactions games actually need, including taps, swipes, holds, menu navigation, and other applicable touch/game controls.
4. **Truly autonomous decision-making.** The agent must understand the current game state, select context-appropriate actions, act, observe results, and adjust without needing the user to choose every step.
5. **Scheduling and unattended execution.** For each configured run, designated start times should trigger game launch and execution with no routine user participation during gameplay. Runs must proceed across tasks and games.
6. **Full task scope.** Routine dailies, repeatable farming, collecting resources and rewards, and other assigned game-specific tasks are in scope. Merely launching a game or reaching its home screen does not complete a run.
7. **Mastery and persistent improvement.** The app must learn each game's mechanics, navigation, relevant resources, task priorities, tactical decisions, and signs of actual success/failure. Learning and validated effective strategies must survive app restarts and future sessions.
8. **Twenty-six game-specific contexts at inception.** Store and maintain separate game identities, objectives, learned strategies, task states, and schedules. Do not assume one common action flow works for all games.
9. **State-aware execution and recovery.** Detect unexpected screens, interruptions, failures, loops, stalled actions, and inaccurate assumptions; recover appropriately rather than blindly repeating actions.
10. **Verified outcomes.** A daily, farm run, or other objective counts as completed only when game-state evidence supports completion; a screen transition or a scheduled timer expiring is insufficient.
11. **User ownership of requirements.** The user defines goals and authorizes any architectural changes. The application must not silently narrow, simplify, or substitute the requested objective.

These are **target requirements**, not claims of existing Android capabilities or present implementation completeness.

## 3. Logical architecture — required responsibilities, not a mandated library or model

The finished product needs the following **logical capabilities**, regardless of their eventual internal implementation:

| Capability | Required responsibility |
|---|---|
| **Game registry & profiles** | Identify installed titles, distinguish the 26 games, persist game-specific settings, objectives, schedules, knowledge and progression. |
| **Time-based orchestrator** | Start sessions at designated times, select games/jobs, handle ordering, handoff and interruption without routine manual input. |
| **Live screen-state interface** | Read the live state of the active Android game through a direct-access mechanism. Screenshot-based navigation is not an acceptable design. |
| **Game interaction controller** | Deliver direct Android game inputs, check that the intended game is in the foreground, and record resulting actions. |
| **Per-game state interpreter** | Interpret interfaces, modes, screens, UI labels, game-specific mechanics, combat/farming states and observed events. |
| **Goal / task planner** | Decide what needs doing, in which order, with what resources, and which action or strategy will advance each objective. |
| **Game mastery engine** | Build and retain experience and effective strategies from game-specific state, action, result and verified outcome; transfer knowledge appropriately without confusing unrelated games. |
| **Execution monitor and recovery** | Re-evaluate after each meaningful result, verify progress, detect failure or loops, retry/reroute/stop safely, and resume interrupted tasks where possible. |
| **Result verification and records** | Record what was attempted, what was proven complete, what failed, and what remains, on a per-game/per-day basis. |
| **User-facing configuration & oversight** | Configure games, timing and assigned objectives; allow user inspection, override and stopping when desired, without making oversight a routine gameplay prerequisite. |

Logical control loop:

```text
Designated time / assigned objective
  → Find game and its saved objectives + learned knowledge
  → Launch or resume relevant Android game
  → Inspect DIRECT live game state
  → Select a game-aware strategy and next action
  → Execute direct control input
  → Observe and verify the actual outcome
  → Update the game's persistent knowledge and task record
  ↳ Repeat/repair/replan until objective completion is verified
  → Continue the next task / next scheduled game
```

**Implementation freedom does not authorize changing the above product contract.** Details such as a specific AI model, Android capture/control API, operating privileges, deployment, storage, scheduler, training method or user-interface design are **not** approved as canonical choices by this document. They must be justified by whether they satisfy the specified outcome; no technical shortcut may be presented as equivalent when it is not.

## 4. Definition of “mastery” (acceptance meaning)

For a particular game, mastery means the agent can **successfully and repeatedly perform the assigned gameplay with independently chosen actions**, handle multiple game states and deviations, learn successful patterns and game constraints, and improve reliably using **verified outcomes**. It must not label generic OCR/button matching, random exploration, scene-change reward scores, or hard-coded sequences alone as complete mastery.

The test of the product is its real-world, unattended **ability to accomplish the user's requested tasks** in the actual games. Source code, a successfully built APK, a visible image recognition result, and successful input injection are individually necessary foundations but not evidence that mastery or unattended farming has been achieved.

## 5. Initial game portfolio — supplied Android launcher image (26 visible entries)

The following is the original image-based inventory; where an Android launcher label is truncated, its exact identity must be verified from the installed Android package, **not guessed and then treated as canon**.

| # | Launcher entry / recognizable game | Identity certainty |
|---:|---|---|
| 01 | Honkai Impact 3rd | Recognizable; launcher shows “Honkai Imp…” |
| 02 | Arknights: Endfield | Recognizable; launcher shows “Endfield” |
| 03 | GODDESS OF VICTORY: NIKKE | Recognizable; launcher shows “NIKKE” |
| 04 | Bleach | Exact title / edition requires package verification |
| 05 | ONE PIEC… | Exact title requires package verification |
| 06 | Honkai: Star Rail | Recognizable; launcher shows “Honkai: Sta…” |
| 07 | Chaos Zer… | Exact title requires package verification |
| 08 | Arknights | Explicit launcher label |
| 09 | 放置少女 | Explicit launcher label |
| 10 | リバ×リバ | Explicit launcher label |
| 11 | Azur Lane | Explicit launcher label |
| 12 | Wuthering … | Recognizable; full title requires package verification |
| 13 | Reverse: 19… | Recognizable; full title requires package verification |
| 14 | E.T.E Chron… | Recognizable; full title requires package verification |
| 15 | Goddess P… | Exact title requires package verification |
| 16 | Stellar Affin… | Exact title requires package verification |
| 17 | TenkafuMA! | Explicit launcher label |
| 18 | Mirrors High | Explicit launcher label |
| 19 | Duel Links | Explicit launcher label |
| 20 | Dokkan | Explicit launcher label |
| 21 | SLIME IM | Explicit launcher label |
| 22 | 少女廻戦 | Explicit launcher label |
| 23 | 7DS | Explicit launcher label |
| 24 | Fate/GO | Explicit launcher label |
| 25 | Echocalypse | Recognizable; launcher shows “Echocalyp…” |
| 26 | Attack on T… | Exact title requires package verification |

Game-specific mechanics, priorities, reset times, farming preferences, account states and the exact designated schedules have **not yet been supplied** in this conversation. Those are operational inputs to obtain or discover during implementation, **not grounds to weaken the core mandate**. Unknown details shall be identified as unknown; no invented canon.

## 6. Strict boundary between requirement and implementation

- This document specifies **what the user has requested** and the logical responsibilities needed to accomplish it.
- Existing code is **implementation**, not authorization to redefine the product. If code falls short, report and fix the code; do not weaken this document to match the code.
- Existing Android limitations, permissions and API behavior must be accurately tested and reported. They cannot be silently ignored or called solved; any permission or setup step that blocks zero-interaction scheduled execution is an **unsatisfied requirement** until a legitimate working method exists.
- A fallback, partial mode or demo may exist as implementation work, but it cannot be described as the complete requested autonomous application.
- Progress reports must distinguish **specified**, **implemented**, **compiled**, **device-tested**, and **demonstrated in actual gameplay**.

## 7. Immutable specification / exact-change authorization protocol

**Default: NO CHANGES TO THIS FILE.** The following actions require explicit prior user permission specifically identifying the affected text and the exact intended replacement/addition/deletion: editing sentences, changing tables, renaming this document, moving it, deleting it, removing requirements, making a replacement “v2”, or changing its status.

An authorized amendment requires:
1. The user specifies **the exact modification** and affirmatively authorizes it.
2. The precise old/new text (or added/deleted text) is captured before any edit.
3. Only that exact diff is applied; no incidental polishing or additional edits.
4. The Git commit and amendment record identify the authority and exact diff.
5. All other text remains unchanged, and past revisions remain retrievable.

No assistant-generated proposal, tool decision, repository convention, CI failure, or indirect instruction is itself permission to alter this document. A future request to implement features **is not** authorization to amend this specification.

**Integrity controls:** The repository should preserve this file in Git's immutable, content-addressed history and verify its frozen baseline hash in CI. A pinned, frozen Git commit remains an auditable reference. Such measures detect unauthorized edits but are **not equivalent to repository-level write protection**; GitHub branch/ruleset enforcement would be required to block edits at the hosting layer. Never claim the file is technically unmodifiable without that enforcement.

## 8. Precedence

For **purpose and architecture**, this frozen document controls over README files, code comments, older assistant statements, generated plans and implementation conveniences. Only an **explicit user-authorized exact amendment** may change its contents. Work that is inconsistent with it must be corrected in the implementation rather than justified by rewriting the user’s requirements.
