# Consumer Inspection and Timing Contract

## Corrections After Full Page Audit

The [592 bytecode disposition](2026-09-23-guide-conflict-disposition.md)
supersedes two incorrect conclusions in the original slice below. Enchanter
`ItemMaterial.materialIndex == 0` means **Arcane Ingot**, not Aura Crystal.
The source now accepts supported-color Arcane Ingots and consumes one on both
successful and failed rolls. Earlier crystal-based live results tested the
substitute input and do not close the corrected catalyst gate.

Dropped-item despawn protection is inherited from `ConsumerTile`, not implemented
inside `FurnaceTile.onUsePower`: all consumers refresh nearby items every 500
server ticks, without requiring power or valid work. The modern implementation
and persistence adaptation are in [the keepalive contract](2026-09-23-consumer-item-keepalive.md).
Both corrections require the next integrated build and runtime checks.
The specialized-machine descriptions below are historical slice findings;
current restoration and runtime results live in the late-game contract and
the acceptance ledger, not those older implementation snapshots.

## Scope and Evidence

This note records the bounded consumer inspection and shared processing changes
for the September 23, 2026 work slice. The canonical source is
`AuraCascade-592.jar` (SHA-256 recorded in `PORTING_NOTES.md`). The relevant
bytecode includes `ConsumerTile`, `ConsumerBlock`, `PlanterTile`, `FisherTile`,
`PotionTile`, and `DyeTile`; the visual reference is Part 2, 04:55, indexed in
`docs/audits/video-reference/4MH53f1itKo.md`.

The original `ConsumerBlock.getTooltipData` exposes `Progress`, `Power per
progress`, and `Last Power`. At 04:55 Part 2 visibly shows `51 / 60`, `150`, and
`33789`. `ConsumerTile` stores `progress`, `storedPower`, and `lastPower` in NBT.
Its tick loop advances only at game-time phases 1 and 2 modulo 20, starts each
eligible update at the consumer's base power cost, and doubles that cost after
each additional progress step in the same update. The per-update cost ramp
therefore restarts on the next eligible update. At phase 18 modulo 20 it casts
`storedPower * 0.25` back to an integer, leaving one quarter of the stored amount.

The legacy receipt readout is not a transfer delta: after collecting neighboring
aura, `lastPower` is set to the resulting stored-power total. It resets to zero
at phase 0 when no receipt is made. The inspection state preserves that meaning
as `lastReceivedPower`/`Last Power`, and reports the current remainder separately
as `storedPower`.

All power-per-progress values were checked against the 592 tile classes; none
mismatch the current port. The Enchanter limit was `999` here versus `1000` in
the jar and is corrected below. The bytecode checks `progress > maxProgress` at
the top of each eligible-loop iteration, resets progress, and invokes
`onUsePower()` before checking affordability of that iteration's next cost.
Each paid increment doubles the cost for the next increment in the same update.
Thus an idle consumer can spend power and advance progress, and a failed/no-op
work callback still follows the same reset boundary. The one-step-per-update
minimum cycle budget remains `powerPerProgress * (maxProgress + 1)`; a same-update
burst may spend more due to the legacy doubling ramp.

The jar also resolves two routing questions. `AngelSteelTile.validItemsNearby()`
returns true, and each powered completion emits one first-tier Angelsteel
ingot. The synthesizer therefore has a real no-input work path, not an
input-backed recipe path. The adapter now produces only that ingot. Separately,
the original Arcane Prism path is a normal processor recipe, not a synthesizer
recipe. The parent owns that recipe/resource migration and the eight-gem vortex
recipes; no catalog or recipe resource is changed here.

## Variant Contract And Remaining Gaps

The source column is the 592 class contract. The video column refers to Part 2
in the evidence table above; it is not treated as a version-matched specification
where it conflicts with the jar.

| Consumer | 592 max / base power | Current port behavior and parity note | Video / live acceptance still needed |
| --- | --- | --- | --- |
| Cascading Processor | `60 / 150` | Normal processor recipe family. As in 592, the ore-to-dust scan takes priority over catalog recipes when both routes match. The optional adapter maps installed `c:ores/<material>` item tags to a nonempty matching `c:dusts/<material>` tag and emits 2 dust per ore; absent dust tags do not invent outputs. Recipe outputs use the last consumed entity in nearby-item order as their anchor; ore outputs use the ore entity. Both preserve position, velocity, and pickup delay. Arcane Prism is routed as a processor recipe by the parent. | Part 2 04:54 shows `51 / 60`, `150`, `33789`, and discusses ingot/ore processing. C1 HUD and P1 survival processing remain live checks; test simultaneous ore/recipe inputs and a concrete optional dust-tag fixture. |
| Prismatic Processor | `9 / 1000` | Same processor family and ore-first routing; original ore conversion emits 3 dust and accepts prismatic processor recipes. The adapter uses the optional common-tag route and emits 3 dust when the matching dust tag is nonempty. Output drops preserve the selected input entity's position, velocity, and pickup delay. | Part 2 07:35 discusses prism and 3-dust ore processing, but no cross-mod dust output is demonstrated. Verify simultaneous ore/recipe inputs, prism recipe path, and survival output. |
| Cascading Smelter | `3 / 190` | Original scans nearby dropped items, consumes one smeltable item, and respawns its furnace-recipe result at that input entity while preserving position, velocity, and pickup delay. The port uses the modern smelting recipe manager for this operation. The video narrator's no-despawn claim is not established by sampled frames or the inspected `FurnaceTile.onUsePower` body. | Part 2 10:55-12:16; verify dropped-item transformation, copied drop context, and lifetime in-world. |
| Cascading Grower | `2 / 50` | No input gate; at each completion the original captures the initial target block and invokes its update tick 50 times at exactly `pos + (0,2,0)`. The modern port rereads the target state and stops the remaining batch if its block type changes, avoiding a stale old-block handler call against a replacement. This is a bounded safety adaptation, not exact transition parity. | Part 2 13:10-14:15 shows crop acceleration. Live-check the target position, inputless activation, and a target that replaces itself during a batch; random tick outcomes are stochastic. |
| Cascading Fisher | `200 / 200` | Matches the 4x4 support bounds below offsets `x,z = -1..2`, accepting modern `Blocks.WATER` states including flowing levels. Instead of Forge `FishingHooks`, the port invokes `minecraft:gameplay/fishing` with the vanilla fishing parameter set, a temporary hook, a plain rod, and zero luck, then emits all returned stacks. Datapack fishing-table changes are honored; Forge-only fishing registrations, player luck, and angler context are not represented. | Part 2 12:17-13:10 describes fishing. Live-check all 16 bounds and a controlled loot table. The synthetic hook/zero-luck context is a deliberate modern substitute, not identical to Forge's weighted hook. |
| Cascading Brewer | `25 / 500` | `PotionTile` accepts regular potion items only: water metadata `0` becomes Awkward `16`; Awkward chooses among the same 12 base values; a base value then XORs bit 5 (amplified) or bit 6 (extended) with equal probability, after which that output is no longer an eligible base input. The port maps supported outcomes to modern strong/long potions. Where vanilla has no corresponding variant, it retains the base effect with a private terminal-stage data marker; it does not turn the result into a splash potion. Each completion transforms one unit from every eligible nearby stack and preserves its position, velocity, and pickup delay. | Part 2 12:17-13:10 describes potion processing. Live-check the three stages, all 12 possible bases, long/strong choices, terminal unsupported variants, and multiple stacks. Modern potion components cannot represent every legacy metadata-only variant exactly. |
| Cascading Colorer | `12 / 50` | `DyeTile` selects uniformly from sheep in `PosUtil.getBoundingBox(pos,2)`, sets the selected sheep un-sheared, then chooses dye ID `0..15` uniformly. The port uses the matching inflated box and the same two independent uniform choices. | Part 2 02:40-04:05 shows the behavior but not its distribution. Live-check the radius boundary, one sheep per completion, regrowth, and distribution over repeated trials. |
| Cascading Synthesizer | `50 / 10000` | Source-backed no-input completion emits one first-degree Angelsteel ingot. The adapter no longer tries synthesizer recipes; higher-degree groups-of-three are separate item recipes and outside this consumer change. | Part 2 08:41-10:54 discusses degree progression; verify first ingot and subsequent three-ingot upgrades in survival. |
| Kaleidoscopic Enchanter | `1000 / 500` | Original target is a weapon/digger item plus tier-zero aura material; the color-bearing base Aura Crystal is consumed even when the roll fails. The adapter now uses a supported color Aura Crystal, not an Arcane Ingot, and corrects the limit. The modern enchantment success/effect engine is not changed or claimed byte-for-byte equivalent. | Part 2 14:16-16:34 shows aura-crystal selection and discusses combinations/failure, but no completed result is confirmed. E1 needs live target/catalyst, success/failure, persistence, and effect checks. |
| Cascading Looter (specialized BE) | `100 / 5000` | 592 `LootTile` uses `ChestGenHooks.getOneItem("dungeonChest", ...)` and retries blacklisted results. The separate modern `LateGameBlockEntity` uses a fixed eight-entry item/count table and unlimited item lifetime; not owned or edited in this slice. | Verify survival output distribution and despawn behavior; video does not establish a complete looter cycle. |
| Cascading Spawner (specialized BE) | `15 / 190` | 592 `SpawnTile` chooses from the world's natural spawn list. The separate modern BE uses fixed dimension pools, a six-mob cap, and a two-air-block check; not owned or edited here. | Part 2 11:44 only visibly shows the block and says Peaceful is enabled; it does not establish a completed spawn. Verify normal-difficulty behavior live. |
| Cascading Miner (specialized BE) | `1 / 2500` | 592 `MinerTile` uses a pulse/explosion charge path. The separate modern BE implements a custom charge, fortified-block containment, release yield, and explosion fallback; not owned or edited here. | No complete miner cycle is established by the reviewed Part 2 frames. Isolated survival and containment QA remains open. |
| Ritual of the Nether (specialized BE) | `100 / 5000` | 592 `TileRitualNether` owns the legacy conversion/danger path. The modern specialized BE maps a bounded list of blocks and applies local danger; no biome rewrite is claimed. Not edited here. | Part 2 16:35-19:58 shows a transformed area, but exact mappings are not isolated; F3 reads `Plains` at 19:16. R1 conversion footprint, persistence, and safety remain open; biome mutation is unresolved. |
| Ritual of the End (specialized BE) | `100 / 5000` | 592 End ritual shares the ritual family contract. The modern specialized BE has its own bounded block map/danger behavior; not edited here. | No version-matched End ritual acceptance was established in the reviewed Part 2 segment; live QA remains open. |

The nine common rows use `AuraConsumerBlockEntity`; looter, spawner, miner, and
both rituals use the separate `LateGameBlockEntity`/`LateGameVariant` path. The
matrix is not a claim that work in the common block entity covers those five
specialized variants. Their table entries remain separate source contracts and
their listed survival checks are still open.

## Implemented Contract

- Consumer progress follows the legacy two eligible ticks per second, with the
  legacy within-tick doubling cost and quarter-retention decay. Progress and
  power spending do not require valid work; the monitor's `hasValidWork()` is a
  separate read-only diagnostic, not a progression gate.
- At the top of each paid-loop iteration, `progress > maxProgress` resets to
  zero and calls the work handler before the next affordability check. A
  no-input or failed handler does not restore progress or refund the spent
  power. Paid work still consumes at most the inputs handled by that callback.
- Processor ore conversions precede catalog recipes. Processor and smelter
  replacements inherit their consumed input entity's position, velocity, and
  pickup delay; the processor recipe anchor is the last reserved input in the
  nearby-entity iteration.
- Grower uses the initial target block identity for the batch contract and
  stops if that block type changes during its 50 random ticks, rather than
  invoking a stale handler on a replacement block.
- The block entity exposes immutable inspection data for progress/max,
  base-required power per progress, the legacy last-power snapshot, and current
  stored power. Client block-entity updates are sent only when one of those
  values changes; no separate `active` claim is made.
- `hasValidWork()` is a public, server-only, read-only query over
  `hasWork(level, pos, variant)` for monitor feedback. It does not advance
  progress, spend power, or mutate nearby entities.
- Existing `stored_power` and `progress` save keys remain. The entity also reads
  legacy `storedPower` and `lastPower` aliases, and persists the new
  `last_received_power` field.
- HUD formatting keys returned for parent wiring are
  `text.aura.hud.consumer.progress`,
  `text.aura.hud.consumer.power_per_progress`,
  `text.aura.hud.consumer.last_power`, and
  `text.aura.hud.consumer.stored_power`.

## Verification Boundary

Focused tests now encode tick cadence, decay, geometric burst costs, no-work
spending/reset boundaries, ore-first routing, input-drop context preservation,
grower block-type stopping, common variant limits/budgets, water-state
recognition, common ore-to-dust tag pairing, and brewer stage/result eligibility.
They have not been run against this diff. The reported 183 passing tests and
client world-load check belong to the preceding compiled snapshot and do not
validate these edits. No Gradle command or Java client was started.
HUD translation wiring is owned by the parent; row C1 remains open until the
integrated build and live client compare displayed values with server state.
P1 survival progression, P2 prism/ore output (including the optional modern
`c:ores` to `c:dusts` route), E1 actual enchant behavior, and R1 ritual behavior
remain open. Nothing here claims live acceptance passed.

## Concrete Live Fixtures

- Grower: give the consumer power with no dropped items. Put a random-ticking
  crop/block at exactly two blocks above it and a non-soil block at one block
  above; confirm the cycle still completes and accelerates only the target.
  Use a random-ticking test block that replaces itself during the batch and
  confirm no subsequent tick is sent to the replacement through the old block
  handler. Compare repeated runs rather than requiring an exact crop stage
  count from 50 stochastic random ticks.
- Fisher: fill all 16 support cells at `x,z = -1..2` one block below the
  consumer, mixing source and flowing water. Override the fishing loot table
  with a fixture that returns two known stacks and verify both appear. Remove
  each boundary cell in turn and confirm no work is eligible. The machine uses
  a temporary hook, plain rod, and zero luck; no player or XP output is expected.
- Brewer: feed one regular water potion, then power three completions and
  observe water-to-Awkward, one of the 12 base potions, then a long/strong result
  or a terminal base-effect result where vanilla lacks that variant. Confirm
  no splash potion is produced and modified/terminal results do not re-enter the
  cycle. With two eligible stack entities, one bottle from each should advance
  per completion. Random base and modifier choices need repeated trials.
- Colorer: place sheep just inside and just outside the legacy radius-two box.
  Each completion should affect exactly one eligible sheep, restore shearing,
  and choose a valid color; use repeated trials to check the uniform selection
  rather than expecting a particular single color.
- Consumer monitor integration: exercise one consumer with work and one without
  work, and verify the parent's monitor calls `hasValidWork()` without changing
  progress, stored power, inputs, or outputs.
- Processor dust tags: add one test ore to `c:ores/example_metal` and one test
  dust to `c:dusts/example_metal`; when a catalog recipe and ore are both
  available, confirm the standard processor consumes the ore first for two
  dust and the prismatic processor consumes one for three. Confirm output
  position, motion, and pickup delay follow the consumed input. Remove the dust
  tag and confirm the ore is left untouched and does not report work.
- Idle progression: fuel a common consumer with no valid work. Confirm it still
  increments and spends power at phases 1 and 2, and that completion resets
  before checking affordability of the next doubled cost. A failed/no-op
  completion should not refund power or hold progress above its limit.
- Specialized looter, spawner, miner, and both rituals: use the distinct
  `LateGameBlockEntity` fixtures recorded in their table rows. Common-consumer
  tests do not cover those behaviors; all listed live checks remain open.
