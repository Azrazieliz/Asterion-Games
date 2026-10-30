# Shoujo Kaisen — Resource and Dispatch Policy (Global + JP)

**2026-10-30, user-provided authority.** This implementation note does not amend `docs/CANONICAL_PURPOSE_AND_ARCHITECTURE_LOCKED.md`.

## Account identity and provenance

| Account | Asterion Pilot game ID | Known identity |
|---|---:|---|
| Global | 26 | Attack on Time = Shoujo Kaisen global |
| Japan | 22 | 少女廻戦 — independent account and some different mechanics |

The **logical policy** may be shared; never share their in-game inventory, mission progress, purchases, schedules, character ownership, refresh allowances or battle strategies as if one account. Japanese UI text needs mapping and current-screen verification. Source inventories from external sites are references, not proof of the user's owned heroes.

## Global dispatch: decision sequence

User's specific objective: maximize **gold, stars, and diamond-chest rewards**. Prioritize visible missions carrying these three reward categories. Lock those missions **before** any list reset.

1. Inspect all live mission rewards. Classify each icon and amount. Unknown icons must not be treated as known gold/chests.
2. Lock the valuable gold/star/diamond-chest missions, leaving other slots unlocked.
3. Use up to **three free list resets**, where available, on unlocked missions only. Locked valuable missions must remain unchanged.
4. If still worthwhile, consider **up to two optional 50-gold paid resets**. Since gold is reserved, each paid reset requires a *verified* return greater than its gold cost, with no speculative exchange-rate conversions across resource types. A random refresh has unknown future output, so it cannot automatically be declared profitable without stronger evidence. **Clarification needed** on the meaning of “output greater than input without comparison”.
5. After desirable missions are locked and resetting is finished, **Quick/Auto Dispatch** remaining ordinary missions; accept automatic hero choices. No special hero-stat optimization for this ordinary dispatch.
6. Collect completed dispatches and run again **during the next scheduled game session**, not by opening the game repeatedly to check long-running missions.
7. Do not use paid completion acceleration without separate user permission.

### Observed UI evidence: image `1000051883.jpg`

Global **Dispatch Level 9**. Top shows dispatch experience `778/100`. A timer reads `Refreshes in 11:55:10`. Visible missions include:

| UI label (visible/truncated) | Duration | Shown reward | Identification confidence |
|---|---|---|---|
| Collect Weapons | 09h 00m | Yellow star icon, 25 | Icon resembles the user-requested stars |
| The Missing Kitty | 06h 00m | Yellow star icon, 12 | As above |
| Courtyard Cleanup | 06h 00m | Gold ingot icon, 200 | Verify exact currency name in game |
| Courtyard Cleanup | 06h 00m | Gold ingot icon, 200 | Second similar mission |
| Steal Weapons | 04h 48m | Dark green crate-like icon, 14 | **Not yet verified** as requested diamond-chest item |

Bottom shows `1 time(s) FREE` at the refresh control, `Quick Dispatch`, and `Claim All`. This is the **remaining free count at capture**, not evidence that there is one total free reset rather than the three specified by the user.

The right-hand overlay visible in the screenshot is external app UI, not a game control. It must not be trained as gameplay input.

## Guild war dispatch / Provisions Transport

This is **not the same as ordinary dispatch**. The strong heroes are needed for battles. Eligible assignment criterion is **grade below +3 OR rarity below UR+**. A UR+ hero at +3 or above is reserved for fights. A high-grade non-UR+ character may still satisfy the user's explicit alternative condition, while unknown rarity/grade must be inspected rather than guessed. Choose suitable lower-value heroes first.

Guild war was **not yet open during the recording**; full competitive strategy is awaiting the next recording.

## Garden and fishing

Use all available planting/fishing actions or resources; if multiple usable options exist, prioritize **higher rarity**. Do not conjure missing stock or mark planting/fishing completed from a menu-open event alone.

## World Extractor

Pick the option with the **highest verified output**, taking task-specific bonuses into account. For a simple additive/multiplicative case: `total = base quantity × multiplier + additive bonus`. Compare like units and actual available task options, not unrelated currencies. More complicated stacking rules require game UI confirmation.

## Protected resources and shop implications

**Preserve:** gold, red stone shards, blue stone shards, karma stones. Spending is restricted to the user's specified reasons/items.

Earlier permission for **two non-gold daily shop purchases** and beginning-of-week useful purchases still applies. `Astral Legacy Beads` are a typical daily option; weekly purchases are restricted to the previously demonstrated list and more specific instructions. Do not buy miscellaneous Dragon/Starwish items just because the displayed shop offers them.

### Store screenshots

- Image `1000051881.jpg`, `Dragon Store`: top balances include red stones `1.5M`, teal/blue-green stones `9.4M` and ingots `28193`. Visible offers include both red and teal-currency prices; **no blanket purchase permission**.
- Image `1000051885.jpg`, `Starwish Store`: visible purple/star currency approximately `157.1K`. First-row SR/SSR/UR options are priced at 30/50/80; the rightmost UR is `Sold Out`. Additional items priced 50/100 and multiple `Sold Out` options are visible. Neither rarity nor available balance alone authorizes purchase.
- Balances are a **moment-in-time observation**, not live inventory for future decisions.

## Farming and daily upgrades

- Farming venues: **story** and **trials** only. Normally no farming unless resources are genuinely low or an explicit task requires it.
- Daily reinforcement: **equipment enhancement, soul, and Over Boost Stone**.
- **Hero grading requires Asterion DB (Game Codex)**; no automatic grading policy is authorized without codex-derived mechanics.
- Guild war competitive schedules and tactics are not yet specified; user will supply event gameplay.

## Reference source research

1. Japanese Game8 hub (updated as of 2026-10-30): https://game8.jp/shoujokaisen
2. Japanese dispatch guide: https://game8.jp/shoujokaisen/418711 — describes dispatch level, missions with prerequisites, auto dispatch, and daily refresh. **Some JP details differ from global**, and allowance counts may depend on player status.
3. Japanese daily checklist: https://game8.jp/shoujokaisen/418235 — covers daily activity reward milestones, rewards/quests, dispatch, free draws and equipment.
4. Japanese equipment guide: https://game8.jp/shoujokaisen/442834 — reinforcement/evolution, soul, and equipment advancement references. It is not the user's individualized hero-grading method.
5. Japanese general store guide: https://game8.jp/shoujokaisen/444411 — generic recommendations **do not override** the user's reserves/weekly purchase instructions.
6. Kaisen Wiki heroes: https://kaisen-wiki.h0rny.net/heroes — searchable hero roster with ~531 current entries, a **reference catalogue**, not verified account inventory and not yet the Asterion DB API.
7. Discord invite provided: https://discord.gg/RT4MTwMd — invitation discovered but server messages were **not accessible** through the available public browsing route.

## Code and acceptance

- `ShoujoKaisenResources.kt` implements pure decision functions for resource spending, priority locking, refresh limits, guild transport, garden/pond rarity, extractor yield and farm/upgrade categories.
- `ShoujoKaisenResourcesTest.kt` verifies the key restrictions and account separation.
- `Brain.kt` now **avoids generic OCR-driven dispatch/shop/extractor commands** where the structured live action classifier is not yet implemented. This is deliberately an incomplete, but safe, integration—not a claim of full automatic dispatch.
- The Android interface can show the distinct Global/JP rules without requesting manual inputs for a routine play session.
- Remaining functional dependency: reliable live recognition of image-backed reward icons and amounts, actual locking/reset/dispatch controller, hero roster ranks/rarities, World Extractor bonus parsing, and end-to-end testing.

**No screenshot files are used as Asterion Pilot runtime navigation inputs.** The three screenshots are developer reference evidence only.
