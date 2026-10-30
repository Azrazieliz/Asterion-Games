# Attack on Time — First Demonstration Analysis

## Source and custody

Original user-supplied filename on connected Android phone:
`/sdcard/DCIM/Screen recordings/Screen_Recording_20261030_131430_Attack on Time.mp4`

Accessed through the user's authorized ADB connection on 2026-10-30. The source was copied for analysis to `D:\AsterionPilotAnalysis\Attack_on_Time_20261030.mp4` on the connected workstation. **The original on Android was not moved, changed, or deleted.**

- Duration: **903.182689 seconds** (15:03)
- Size: **1,447,409,823 bytes**
- Resolution: **1080×2340 portrait**
- Codec: H.264, one video track
- User interaction events: **No separate touch-event trace was supplied**. This is an uncut screen recording; observed transitions are not identical to a reliable input-coordinate mapping.

No raw video, screenshots, passwords or account tokens are committed to this public repository. All timestamps are elapsed **video time**, not server reset times.

## Observed sequence (video-derived, not a universal required order)

| Video position | Observed section | Inferred role | Important result |
|---|---|---|---|
| 00:00–00:40 | Android launcher, startup, login | Start | Game opens |
| 00:40–01:10 | Login Rewards, monthly sign-in | Collect | Rewards dialog visible |
| 01:10–01:25 | Racing ad | Interruption | Third-party screen; do not tap Install |
| 01:25–02:25 | Hero/UR drawing | Optional rewards | Free Once and resource-priced draws; do not assume all are free |
| 02:25–02:50 | Smithy draws | Optional/resource spending | Paid-resource selections visible |
| ~03:00–03:40 | Hero/equipment screen, battle result | Progress/combat | **Victory** with stage rewards appears; one battle only |
| ~03:55–04:55 | Third-party ad | Interruption | Install CTA; avoid accidental app installation |
| 04:55–05:15 | Conquest, Quick Mode Report | Idle rewards | **Claimed** stamped on quick mode report |
| 05:15–05:55 | Dispatch | Expeditions/dailies | Multiple entries selected and dispatched; event quest progress shown |
| 06:00–06:55 | Combat against other players | Battle | **Lose** displayed; should trigger replanning, never report victory |
| 06:55–07:25 | Spacetime Arena | Competitive activity | Rank 4/challenge list, no whole daily completion confirmed |
| 07:25–07:45 | Outer God Invasion | Boss and rewards | Claim 1/1 shown as available, not guaranteed claimed |
| 07:45–08:55 | Guild/Overlord/Mystic shops | Resources | Purchase dialogs shown; currencies and stock vary |
| 09:00–09:15 | Hero roster and home | Roster | Navigation only |
| 09:15–11:05 | Galaxy Clash event | Event minigame | Event Quest and multiple Rewards popups |
| 11:05–11:50 | Bag and shard inspection | Inventory | Resource inspection; not automatically a daily |
| 11:50–12:15 | Chaos Clash | Competitive | Challenges displayed; win not conclusively shown |
| 12:15–12:55 | Provisions Transport and guild ranking | Guild/resource | Dispatched Heroes panel shown |
| 12:55–13:25 | Quest list, Weekly tab | Goal/status | **Multiple quests remain incomplete**, e.g., arena wins, dispatch target |
| 13:25–13:45 | The World Extractor | Allocation/production | Requires character allocations; no proof of whole objective completion |
| 13:45–14:05 | Garden | Resource production | **Successfully planted** observed |
| 14:05–14:25 | Ponds | Resource collection | Bertha Pond fishing UI |
| 14:25–14:45 | Treasure Ascend | Progression/resource expenditure | **Ascended successfully** observed |
| 14:45–15:03 | Other scene, home | End | No final all-dailies-complete proof |

Times are approximate chapter markers derived from sampled frames, not a verified frame-by-frame touch log.

## Important engineering findings

1. **The recording is useful for state and workflow learning, but it does not prove complete dailies.** It includes weekly-quest panels with incomplete goals.
2. **A screen-wide game-specific planner is required.** Buttons such as `Claim`, `Challenge`, `Start` and `Purchase` have radically different effects depending on the active panel. A generic OCR tapper is insufficient.
3. **A battle result is not all daily work.** A single `Victory` proves one battle outcome, not completion of the game's daily/farming task set.
4. **Losses must be recognized.** A `Lose` scene is visible and must trigger a different response than a victory screen.
5. **Monetization and ads are separate hazards.** Paid draws, shop purchases, resource expenditure, and ad install buttons need distinct safety/state treatment.
6. **Per-game progress must track independent activities**, including login/sign-in, idle rewards, dispatch, arena, boss, events, guild weekly tasks, garden/pond, and optional permanent progression. Some are not daily requirements; the app must not assume all are mandatory.
7. **No fixed touch macro is justified by this video alone.** Real control points and success conditions must be verified against the live game before automatic play.
8. **The transcript is derived from visual inspection of sampled frames.** Exact claim outcomes and click coordinates have not been verified for every action. Avoid fabricating touch traces.

## Implemented from this demonstration

- Game 26 Attack on Time activity cues and video-grounded playbooks.
- An explicit scene classifier and per-screen no-spend / no-install pause rules.
- Individual screen outcome markers that do not claim entire dailies are finished.
- A reference to this evidence and its chronology in code (video itself stays off GitHub).
- Tests for scene identification, single-battle outcome, incomplete daily semantics, advertisements and purchase hazards.

## Still required before autonomous acceptance

- Reliable live UI recognition for all observed game screens, including icon-only menus.
- Actual action/gesture mapping and verification of scene-to-scene transitions on the device.
- Per-activity persistent goal state and completion confirmation, not generic screen change.
- Successful autonomous retries after losses, interruptions or game state drift.
- Unattended Android capture/control permission solution with real-device testing.
- Additional gameplay observations only when different outcomes or mechanics are not covered in this session.

This file is an **implementation/training evidence record**. It does not modify the frozen canonical purpose/architecture file.
