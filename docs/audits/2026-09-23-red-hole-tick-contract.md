# Portable Red Hole Tick Contract

## Fifteenth-Candidate Finding

`build/qa-audit/node-pilot-playtest/fifteenth-red-hole-final.log` records a real
power-12 blast at gameTime 123600: the near stone became fire, the 32-block
witness stayed stone, and the marker's Age was 1. After a 100-tick step to
123700, near stone was gone and the exterior witness remained, but the marker
was absent. Parent also reported a global selector count of zero, excluding
simple displacement outside the fixture box. No invulnerability NBT was seeded.

The earlier immediate stone check followed by successful cleanup detection did
not establish production failure by itself: gameTime is not an end-of-tick
completion fence. However, source inspection now establishes two independent
production contract errors. The marker's exact damage/removal event was not
captured, so this audit does not claim a traced fire-death cause.

## Version-Matched Evidence

Local canonical `AuraCascade-592.jar`, `ItemRedHole.onEntityItemUpdate`:

- Offsets 5-22: server-only, world total time modulo 100 equals zero.
- Offsets 25-42: ordinary item entity as explosion source, its current position,
  power 12, and `World.func_72876_a(Entity, double, double, double, float, boolean)`
  with the last argument true.
- Offsets 46-49: returns true on eruption; otherwise false.
- `getEntityLifespan`, offsets 0-3: 30000.
- The class extends ordinary `Item` and has no custom-entity, damage-immunity,
  fire-resistance, or invulnerability override.

The boolean was previously interpreted incorrectly. The official Minecraft 1.9.4
client JAR, SHA-1 `4a61c873be90bb1196d68dac7b29870408c56969`, was downloaded and
hash-checked **in memory only**, then read as a ZIP/class file in PowerShell. No
Java process or game was launched. The official Forge MCP 1.9.4 SRG mapping maps
this call to `aht.a(Lrr;DDDFZ)Lahp;`. Its bytecode at offsets 9-12 pushes constant
false, then the supplied boolean, and invokes the two-boolean overload. Therefore
the call means **no fire, terrain destruction enabled**, not a fiery explosion.

Primary reference fixtures:

- [Mojang 1.9.4 client JAR](https://piston-data.mojang.com/v1/objects/4a61c873be90bb1196d68dac7b29870408c56969/client.jar)
- [Forge MCP 1.9.4 SRG mappings](https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/1.9.4/mcp-1.9.4-srg.zip)
- [MCP stable 26 fields/methods](https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/26-1.9.4/mcp_stable-26-1.9.4.zip):
  `field_77286_a` is `isFlaming`; `func_72876_a` is `createExplosion`.
- [Forge 1.9.4 Explosion constructor patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.9.4/patches/minecraft/net/minecraft/world/Explosion.java.patch)
  retains first-boolean assignment to `field_77286_a`.
- [Forge EntityItem patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.9.4/patches/minecraft/net/minecraft/entity/item/EntityItem.java.patch)
  invokes the item hook before the rest of `onUpdate` and immediately returns on
  true. The [Item API patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.9.4/patches/minecraft/net/minecraft/item/Item.java.patch)
  documents that skip contract and defaults to no custom item entity.

## Modern Callback Error

Installed Fabric lifecycle-events 2.3.8+0684cd1248 sources inject END_WORLD_TICK
at `ServerLevel.tick` TAIL without a frozen-time guard. Cached mapped 1.21.1
bytecode shows `tickTime()` guarded by `TickRateManager.runsNormally()` at offsets
168-174, while the method still reaches its end when frozen. Thus the removed
`AuraItems.tickWorldUtilityItems` world scan could explode repeatedly at the same
frozen divisible-by-100 gameTime; it also bypassed normal item entity tick-range
eligibility. This is a real callback-placement error, not simply slow visibility.

In contrast, `ServerLevel.method_31420`, the entity-tick lambda, returns at offsets
21-29 when `TickRateManager.isEntityFrozen(entity)` is true and checks entity-ticking
range before dispatch. Attaching the effect to actual `ItemEntity.tick` follows
those vanilla gates without another global timer, counter, or retained world map.

Modern `Explosion.explode` passes its source entity to `Level.getEntities` at
offsets 546-578. Keeping the Red Hole entity as source preserves its exclusion
from its own direct blast damage. This is not blanket immunity to fire, lava,
another explosion, or other external damage sources.

## Bounded Repair

- Remove only the obsolete world-utility tick registration and scan in `AuraItems`.
  The Black Hole inventory consumer and all other accessory/ingot callbacks stay.
- Add server-side `PortableRedHoleItem.onEntityItemTick`: matching item, world-time
  modulo 100, power 12, fire false, BLOCK interaction, same source entity.
- Extend the already registered `PortableRedHoleLifetimeMixin` with a cancellable
  ItemEntity.tick HEAD hook. Cancel the remainder only when the item hook erupts,
  matching Forge's true return. No new mixin registration or entity type.
- Preserve 30000 normal item-age lifetime and the D3 consumer-lifetime override.
  Age, pickup delay, motion, and other normal item-tick work are skipped on the
  eruption tick, as in 592. A fresh marker stepped directly onto a boundary now
  remains Age 0, and is Age 99 at the next boundary 100 world ticks later.

No synthetic immortality, fireproof component, self-respawn, tag replacement,
world-time mutation, or global explosion filtering is introduced. External damage
can still destroy the marker. World-generation/fire already present in a fixture
must be cleared or a fresh isolated station used for the post-fix run.

## Verification And Follow-Up

`PortableRedHoleTickTest` adds cadence/non-item rules, the 99-normal-update cadence
count, and structural registered-mixin/server/non-fiery-call/no-world-scan checks.
These are deterministic rule/source tests, not transformed mixin execution. No
Java, Gradle, test runner, bridge, or client was run by this worker.

Parent's `integrated-restoration-sixteenth-retry` full build passed in a reported
44 seconds, peak working set 1391.6 MiB and private memory 1501.1 MiB. Read-only
inspection of the final XML reports independently confirms **244 tests across 60
suites, zero failures/errors/skips**, including all six `PortableRedHoleTickTest`
cases (suite timestamp `2026-09-23T22:08:21.275Z`). The initial sixteenth attempt's
only failure was the stale guide assertion requiring the old fire phrase; parent
corrected that assertion before this green full retry.

Verified SHA-256 of `build/libs/aura-cascade-0.2.0+1.21.1.jar`:

```text
cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63
```

This establishes compilation and deterministic/source-structure test acceptance,
separately from the live frozen-negative and marker-survival results below.

Parent rerun should observe two successive real blasts, same surviving marker
UUID/count, expected Age 0 then 99, outside witness unchanged, no new fire blocks,
and no extra destruction while held frozen at a cadence boundary. Set a fresh
near stone only after the first blast completes, hold frozen and require it stays
stone, then advance exactly 100 ticks for the second blast. Use bounded completion
polling, not gameTime alone as a completion fence. Do not seed immunity. Retain
save/reload and ordinary external-damage/lifetime checks as separate acceptance
rows rather than claiming all from two blasts.

## Sixteenth Live Acceptance

Parent's clean-arena run `qa_b4_c19a1de03a474205b1d5e323f05b0d0c` passed on the
sixteenth candidate identified above. Read-only review of
`build/qa-audit/node-pilot-playtest/sixteenth-red-hole-clean-arena.log` and matching
`destructive-world-results.jsonl` entries confirms:

| Check | Observed result |
| --- | --- |
| First real item tick onto gameTime 124000 | Near stone at 602,180,400 became air; witness at 632,180,400 stayed stone; tagged marker count 1, Age 0. |
| One wall-clock second frozen at 124000 | Replaced near stone remained stone; gameTime unchanged; same tagged marker present, Age 0. No repeated frozen-boundary blast. |
| Exactly 100 further ticks to 124100 | Near stone became air again; 32-block witness remained stone; same tagged marker count 1, Age 99. |

The frozen-control PASS was recorded at `2026-09-23T22:11:07.4130297Z`; final
summary PASS at `2026-09-23T22:11:14.3359654Z`. The log contains one initial summon,
no subsequent marker recreation, and cleanup only after the checks. Its disclosed
NoGravity, stationary motion and pickup delay keep the fixture in place; no
invulnerability, fire resistance, lifespan, age, or health override was seeded.
The evidence tracks a unique fixture tag and count, not a separately recorded
UUID comparison.

The first sixteenth preflight stopped before the test because the previous
fifteenth-candidate fiery run had contaminated the arena. Parent verified fire
at 600,180,400 and reports removing 142 fire blocks only within the owned region
x590..640, y180..184, z390..410, then reran. That cleanup is setup remediation,
not a sixteenth-candidate failure or a seeded explosion outcome. The clean run
independently required the marker and both witness positions to be air at entry.

Accepted scope: two real eruptions, the 100-world-tick cadence, the skipped item
age increment on each eruption, bounded marker survival, and the frozen-negative
regression. The local near witnesses became air, not fire; no exhaustive regional
fire census was performed during this run. The source `fire=false` contract is
also independently covered by the structural regression.

This does not measure the complete blast footprint, survival-player damage,
external fire/lava/other-explosion immunity, full 30000-item-age expiry, or Red Hole
save/reload. A single intact witness 32 blocks away is not an exact radius proof.
No blanket immunity or full utility-item parity claim follows from this pass.

The source finding required parent-owned guide/lang/audits calling the Red Hole
eruption "fiery" to describe non-fiery destructive explosions. In particular, the guide
coverage/page-coverage wording is not source evidence for retaining fire. This
worker did not edit those shared resources or private runtime fixture helpers.
