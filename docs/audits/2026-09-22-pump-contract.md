# Original Pump Contract (2026-09-22)

## Evidence

Primary source: the shipped `AuraCascade-592.jar`, inspected locally with
`javap -J-Xmx128m -classpath AuraCascade-592.jar -c -p`. The relevant classes
are `pixlepix.auracascade.block.tile.AuraTilePumpBase`, `AuraTilePump`,
`AuraTilePumpLight`, `AuraTilePumpFall`, `AuraTilePumpProjectile`,
`AuraTilePumpRedstone`, `AuraTilePumpCreative`, their `Alt` subclasses, and
`pixlepix.auracascade.main.Config` (static initializer). This records shipped
bytecode and default config values, not a claim about user-overridden legacy
configurations.

## Tick And Fuel Contract

- `AuraTilePumpBase.func_73660_a` attempts a lift only on server world time
  `% 20 == 2`, with no redstone power and `pumpPower > 0`. It scans 1 through 15
  blocks above for the first `AuraTile` with an open path. With no target it
  preserves fuel. With a target it decrements `pumpPower` **before** calculating
  any color, including when storage is empty, every color rounds to zero, or
  the destination would normally reject a color. The direct add/subtract path
  does not call the destination's `canReceive` method.
- `addFuel(power, speed)` compares `power * speed` against the existing
  `pumpPower * pumpSpeed` before an alternator triples the accepted speed.
  Equal or weaker offers leave the state unchanged. `pumpPower` is therefore
  remaining eligible one-second attempts, not an unconditional wall-clock
  timer. `pumpSpeed` is nominal aura per second at a one-block rise before
  composition, mass, ascent, and (for alternators) sine modulation.
- Default `Config` values and subclass call sites establish these offers:

  | Source | Power (eligible seconds) | Nominal speed |
  | --- | ---: | ---: |
  | Burning item | furnace burn ticks / 5, integer truncation | 300 |
  | Glowstone | 180 | 750 |
  | Torch | 30 | 750 |
  | Fall | `(int)(2 * fall distance)` | 500 |
  | Arrow | 20 | 1,000 |
  | Egg | 90 | 500 |
  | Snowball | 10 | 500 |
  | Powered redstone wire at distance `d` | `(int)(10 * 1.4^d)` | 1,500 |
  | Creative | 2 | 10,000,000 |

  `AuraTilePump` only seeks burning items on `% 20 == 2` while unpowered and
  empty. `AuraTilePumpRedstone` walks contiguous wire from the adjacent block,
  stopping at the first non-wire block; it can consume multiple powered wires
  in one scan. The `Alt` subclasses change only `isAlternator()`.

  The modern burning pump reads `AbstractFurnaceBlockEntity.getFuel()` rather
  than a fixed list. That method is public in the pinned Mojang-mapped 1.21.1
  jar; Fabric API's pinned `AbstractFurnaceBlockEntityMixin` applies registered
  fuels to its map. This follows the modern furnace fuel table, not the exact
  historical 1.8 item set. The shipped `AuraTilePump` shrinks or discards the
  burned item without returning a container, including for a lava bucket;
  the port retains that behavior.

## Transfer Arithmetic

The bytecode truncates to `int` after **each** stage: `speed / rise`, then
alternating factor (when applicable), then color composition, then division
by relative mass, then ascent boost, then source-amount cap. Colors with zero
relative mass receive zero from the pump. The port's pump-specific plan now
follows this order; the shared aura kernel remains unchanged. Movement
particles are emitted only after `AuraKernel.applyTransfer` reports a positive
`moved()` result, never from a request or an idle attempt.

## HUD And Sync

`pumpState()` exposes remaining eligible seconds and nominal speed;
`pumpInhibited()` exposes the current neighbor-redstone inhibition. The
inhibition flag is sampled on the server and saved in the pump's block-entity
tag alongside power and speed. The parent's `syncInspection` hook is called at
the end of every pump server tick so its change-detected update packet can
refresh the client HUD even after an early return inside the lift attempt.

## Validation And Bounds

The first integrated client audit reproduced an adjacent-receiver feedback bug:
after lifting 300 White Aura, the pump retained 5,700 Aura and 266 eligible
seconds with `pump_inhibited: 1b`; a later sample remained unchanged. Replacing
the receiver at +1 with one at +3 cleared inhibition and resumed transfer.
The port had made every node and pump a direct redstone source. Original
`AuraBlock` defines comparator methods `func_149740_M`/`func_180641_l`, but no
`func_149744_f`/`func_180656_a` direct-power overrides; `AuraBlockCapacitor` adds
none either. Original `BlockMonitor` does define those direct-power overrides.
The parent removed direct-power overrides from modern node/pump blocks while
retaining their comparator readout and leaving Monitor output unchanged. A
regression test protects that distinction. The rebuilt client transferred all
6,000 seeded White Aura to the adjacent receiver with inhibition false. A real
redstone block then held another 6,000 Aura and 165 eligible seconds unchanged
for 430 ticks; removing it resumed transfer. Independent screenshot/log review
passed this focused correction. See the node-pilot-playtest record for scope.

`AuraPumpLogicTest` pins default offers, pre-tripling fuel comparison,
intermediate integer truncation, empty-target fuel spending, positive movement
on the last eligible second, furnace-map values beyond the former fixed list,
and pump-tag round-tripping. The parent integration run passed 103 tests and
the build before the subsequent direct-power fix described above. Modern furnace fuels (including Fabric-registered
entries) are not identical to every historical 1.8 fuel; block collision/path
rules and other modernized node behavior are outside this correction. The
parent-owned sync and particle hooks require live acceptance beyond the passing
unit/build checks; third-party Fabric fuel registration remains unverified.
