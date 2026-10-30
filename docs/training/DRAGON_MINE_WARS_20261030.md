# Attack on Time / Shoujo Kaisen Global — Dragon Mine Wars video

Source: `Screen_Recording_20261030_195058_Attack on Time.mp4` retrieved through authorized ADB from `/sdcard/DCIM/Screen recordings/`. Original left on device. Local authorized working copy `D:\AsterionPilotAnalysis\Attack_on_Time_MineWar_20261030_195058.mp4`. Length **513.505767 s (08:33)**, size **690,562,277 bytes**. No private full-length recording or account screenshots committed to GitHub.

## Verified visual chronology (relative video time)

| Time | Visible behavior | Distinguish fact from inference |
|---|---|---|
| 00:00–00:45 | Starts Android launcher, enters Attack on Time; account lobby, event notices, waits for Mine War notification around 19:43 local | Clock is a user phone alarm, not proven server reset |
| 00:48–01:11 | Navigates Conquest/Trials and fighting screens | Not Mine Wars, do not train mine routine from it |
| 01:24–01:45 | Dragon Mine Wars map visible with Battle Stage I and remaining event countdown; multiple page navigation tabs and mine levels | Stage and overall event clocks separate |
| 01:36 | Opens enemy mine ID **3-5**, Handan Lv34, **cyan +290/min**, collected 0, plunder 0; defender power **177,635,536**; **six-person and x2 icons** | Icons' precise resource meaning not known |
| 02:11–03:15 | Cross-server Occupation Team, player selects and revises units/formation; named hero bonuses promise **Attack +20% (stackable)** | Need verified owned heroes, incompatible/garrisoned status and actual damage to optimize |
| 03:24 | Victory screen against mine-defense battle | Win confirmed, but occupying and resource income must also be checked |
| 03:33 | **My Dragon Mine(s)** panel for Handan Lv29, **cyan +276/min**, **4/5** occupied seen on map, timer 00:00:16/05:00:00 | Occupied mine amount is live observed; cap/collection mechanics not yet proven |
| 03:36–04:45 | Examines another enemy mine and adjusts formation with stronger owned characters; fights, battle report, **Win** stats screen (04:48) | Actual target ranking/win conditions are not derived from raw power alone |
| 05:00–06:47 | Inspects several mine levels/pages; some mine panels show red-shard rates e.g. **+158/min** at Chang'an Lv55. Battles recur | Do not treat menu clicks as battle victories or mined resources |
| 06:13 | Chang'an Lv55 enemy mine: red **+158/min**, defender **102,523,808**, collection info and **50% plunder** field | Plunder uses accumulated target resources; not guaranteed gain |
| 07:08 | Revises formation once again, including remaining usable units; combat follows | Some characters are labeled Garrisoning and are not freely available |
| 07:23 | Own mines summary shows **5 mine positions** and **total efficiency +941 red/min, +2159 cyan/min** | Account-specific snapshot, not an auto-set objective or universal strategy |
| 07:48 | Additional **Victory** screen | Check status after leaving battle rather than assuming ownership |
| 08:00–08:33 | Leaves Mine Wars map, briefly opens an unrelated event, returns home | Mine cycle continues after exit, no app-triggered clicks justified |

## Observed game mechanics

1. **Dragon Mine Wars** map transitions into **Draconic Mirage**; pages can show different clusters of sites, so site names and levels must be read dynamically.
2. Current event uses **Battle Stage I** and a separate overall event countdown. Do not extrapolate the phone alarms as server-defined event hours.
3. Mine details show ID, region/level, owner/faction, garrison power, named hero buffs, per-minute reward, accrued collected amount, half-plunderable amount and a visible **05:00:00** collection progress scale.
4. There are **five occupied mine slots** visible. They were not all full initially (4/5) and reached 5/5 later.
5. Red and cyan-icon resources are different currencies. No supported numerical exchange rate or user-supplied valuation permits simply adding them. The observed total rate of **+941 red/min and +2159 cyan/min** is descriptive.
6. Formation UI has six primary unit placeholders, a separate second rank of silhouettes and mine-specific requirements (person x6 / lightning x2 in screenshots). The exact semantic rule of the lightning icon must be learned before authorizing combat.
7. Certain named hero combinations yield **+20% Attack (stackable)**; bonuses affect combat but cannot be treated as resource yield multipliers. Hero availability changes when a unit is already `Garrisoning`.
8. Enemy can be fought or mine occupied via `Occupy`, `Challenge` and `Plunder`. Raw tap alone is not verified success; explicit Victory and new `My Dragon Mine(s)` occupation must be confirmed.
9. Own mine information offers `Assist` and `Add Buffs`; both require separate proven mechanics and costs before automatic use.
10. Plunder is shown as `Can plunder (50%)`; it is a potential, not a guaranteed payout.

## Implemented source and remaining missing links

- `DragonMineWars.kt`: typed screen classifiers, mine-reward estimates within observed collection meters, currency-separated ranking, named hero stackable attack bonus representation, constrained team/slot decision gating and hold/replacement scenarios.
- `DragonMineWarsTest.kt`: mine classification, regional separation, protected yield comparison, unknown team stop and profitable same-currency replacement tests.
- `Brain.choose` now prevents generic OCR actions on recognized event screens, since automatic clicking Challenge/Occupy without real resource/formation state is unsafe.
- Global GamePlaybooks registers the timed event separately from regular dispatch and daily reward tasks.

**Not yet proven/implemented**: reliable current live extraction of mine detail numbers and associated coordinates, rich hero Codex matching, precise server event schedule, mine-income optimization including risks, a complete combat/occupation feedback loop, and autonomous 5/5 results. Continue integrating only when explicit actual actions and successful results can be verified. Do not alter frozen owner-approved master files.
