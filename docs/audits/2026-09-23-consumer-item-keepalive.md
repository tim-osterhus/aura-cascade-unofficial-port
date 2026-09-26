# D3 Consumer Item Keepalive

Source checkpoint, not a runtime PASS. No build, tests or client executed here.

## Evidence And Scope

`AuraCascade-592.jar`, `ConsumerTile.func_73660_a` offsets 273-291 calls
`AuraUtil.keepAlive(this, 3)` every server game-time multiple of 500, without
requiring power or valid work. `AuraUtil.keepAlive` offsets 0-83 queries dropped
items in the AABB between block position minus three and plus three, sets
`lifespan = Integer.MAX_VALUE`, and resets age to zero. The local JAR bytecode
was rechecked with Java 21 `javap -J-Xmx64m` for this repair.

`ConsumerItemKeepAlive.tick` now runs first in both common and late-game consumer
server ticks. Its query uses existing loaded entity sections only, exact 6x6x6
bounds (not a block AABB inflated to seven), and a 500-game-tick cadence. It does
not load chunks, inspect recipes, consume power, or alter stacks/pickup delays.
All common/specialized variants using these ticks receive the inherited behavior.

## Modern Lifetime Adaptation

Vanilla 1.21.1 has a fixed despawn threshold instead of Forge's mutable lifespan.
`ConsumerItemLifetimeMixin` stores a per-entity protection flag and directly resets
age to zero on refresh. The existing `PortableRedHoleLifetimeMixin` is the sole
tick-threshold modifier: it resolves normal/Red Hole lifetime first, then applies
the consumer maximum. Unprotected Red Holes remain 30,000 ticks; protected ones
receive Integer.MAX_VALUE just like any other nearby item. No competing tick
constant injector is introduced. This protects against age despawn, not damage,
void removal, pickup, or explicit deletion.

Entity NBT stores `aura:consumer_keep_alive` and `aura:consumer_item_age`. The
latter preserves full-width ages beyond vanilla's short `Age` field. Protection
therefore survives leaving range, consumer removal, and entity save/load. Old
untagged items retain vanilla behavior until refreshed. No client attachment or
packet is needed: server-side age despawn is authoritative.

Platform merge adaptation: the surviving entity inherits consumer protection
from its merged source, while vanilla retains its normal minimum-age and stack
count rules. The protected merge-age threshold also uses the maximum lifetime.
This prevents ordinary merging from silently discarding protection. It does not
add a new merge policy or extend ordinary unprotected item lifetimes. The original
592 consumer evidence establishes the lifespan operation, not modern vanilla's
merge internals; this preservation is explicitly an adaptation.

## Integration And Verification

Parent registered **`ConsumerItemLifetimeMixin`** in the common `mixins` list
of `aura.mixins.json`. The old lifetime mixin remains
registered. Production changes in consumer files are one tick call each; their
imports, enchantment code and work logic are untouched.

`ConsumerItemKeepAliveTest` covers server cadence, exact bounds, ordinary/Red Hole
lifetime precedence and tick-call placement. Its unit Knot loader does **not**
load Aura's mixins: the eleventh integration run's three entity casts failed even
though both types belonged to Knot. Same-loader access is not transformation.
The shared test bootstrap is unchanged; no failing runtime assertion is replaced
with a mock or reported as a transformed unit PASS.

Those three cases now live in the opt-in private `ConsumerLifetimeProbe` under
`scripts/qa/multiplayer`. It checks the real transformed interface, age reset,
70,000-tick NBT round-trip/resave, untagged restoration, protection inheritance
through vanilla merge with minimum-age preservation, and merge eligibility at
the ordinary 6,000-tick expiry boundary. It uses real stone stacks and a live
server level, but detached entities; this is not a world restart test. Private
method names are resolved from verified 1.21.1 intermediary mappings. Packaged
namespace and actual Aura JAR SHA-256 must match before any case can pass.

**Packaged detached-entity runtime gate PASS, September 23:** the parent executed
the separate private probe on the packaged server. The report was read back at
`build/qa-audit/multiplayer/20260923-091306-403/consumer-lifetime-probe.json`:

- `success: true`, `runtimeNamespace: intermediary`, `itemEntityTransformed: true`.
- `ageAndPersistence: PASS`, `merge: PASS`, `oldMergeEligibility: PASS`.
- Loaded Aura origin: that run's
  `server-game/mods/aura-cascade-0.2.0+1.21.1.jar`.
- Actual loaded Aura SHA-256:
  `e8177931dcbb3cec34345e3d1f3d2abfb002b2955d2438978c78b407066e4210`.

This executed evidence supersedes the initial source-only checkpoint for these
three checks only. It proves the production mixins operated on detached real
entities in an intermediary server runtime, including production NBT hooks and
vanilla merge invocation. It does not prove live consumer tick cadence, natural
world-entity merging, elapsed-time despawn survival, or disk world save/reload.
The broader multiplayer run is separate and is not marked passed by this report.

**Live 500-tick cadence/despawn and world-reload gates remain pending.** Keep the
parent's separate `consumer-lifetime.ps1` fixture rather than treating this probe
as a replacement. Required live controls are an unpowered consumer
with an ordinary item, a Red Hole and an outside-range control. Observe age zero
and the protection fields at a game-time multiple of 500; protected items must
survive their original lifetime outside range and after reload. Confirm common
and late-game machines both refresh without work/power. A bounded command setup
can age protected items to 5,999/29,999 using the full-width field after the initial
refresh; use ordinary `Age` for the unprotected control. That avoids a 25-minute
manual wait but is a deliberately seeded lifecycle test, not elapsed-time proof.
