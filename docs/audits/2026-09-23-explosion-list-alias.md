# Explosion List Alias Regression

## Confirmed Defect

Parent-reported fourteenth-candidate live TNT trials: with an equipped Shattered
Stone ring nearby, all three trials destroyed stone while preserving both ores.
With the ring wearer 16 blocks away, all three controls left stone and both ores
undamaged. This is a genuine production integration defect, not a fixture failure.

`RingOfShatteredStoneRuntime.filterProtectedExplosionBlocks` intentionally returns
the exact input list when there are no qualifying wearers. `ServerExplosionMixin`
passed the live `Explosion.getToBlow()` list into that filter, then unconditionally
called `clearToBlow()` and `getToBlow().addAll(filteredBlocks)`. On the no-wearer
path both references name the same list: clearing the destination also clears the
source, so vanilla receives no affected blocks. Existing filter-only tests proved
pass-through identity, but did not cover this destructive integration step.

## Bounded Repair

The mixin retains the original list reference and returns immediately when the
filter returns that same object. This leaves vanilla's list untouched without a
new allocation. A distinct filtered list still follows the existing clear/addAll
path, including an intentionally empty list when every block is protected.

No change to ring equipment detection, three-block search AABB, terrain allowlist,
block ordering, wearer damage, explosion timing, Red Hole behavior, or other
production files. The repair is at the server-side `Explosion.explode` RETURN
hook; it does not cancel the explosion or grant entity-damage immunity.

## Verification Boundary

New `ShatteredStoneExplosionIntegrationTest` covers no-wearer identity/order and
zero state lookups; independent filtered terrain/air versus two ore entries;
intentional empty replacement; and a structural assertion that the registered
server mixin checks identity before its sole clear operation. Existing
`UtilityGearLogicTest` allowlist/search-bounds/pass-through cases are unchanged.

These deterministic filter and source-structure checks are not transformed-mixin
or live explosion tests. This worker launched no Java, Gradle, tests, or client;
the parent owns integrated validation and live reruns.

## Fifteenth Build

Parent reports build fifteen PASS in 49 seconds, peak working set 1507.9 MiB and
private memory 1626.5 MiB. Read-only inspection of `build/test-results/test/TEST-*.xml`
independently confirms **238 tests, 59 suites, zero failures/errors/skips**. The
four `ShatteredStoneExplosionIntegrationTest` cases all passed; its suite timestamp
is `2026-09-23T21:41:35.643Z`.

SHA-256 independently verified from `build/libs/aura-cascade-0.2.0+1.21.1.jar`:

```text
27502af9b5e763c16464fbccdac7381929326aa698569eb1c2debbeaa3811fb5
```

## Completed Live Rerun

Parent-operated fifteenth-candidate run
`qa_b4_816fc191bff74e9388b525feb8453e16` completed with a PASS summary at
`2026-09-23T21:46:03.0811442Z`. Evidence is
`build/qa-audit/node-pilot-playtest/fifteenth-shattered-equipped.log` and the
matching records in `destructive-world-results.jsonl` in the same directory.
Raw command results confirm the actual TNT explosions and subsequent block checks.

- Nearby equipped ring: all three trials destroyed stone and preserved both
  diamond and iron ore.
- Separate control lane with that equipped wearer 16 blocks away: all three
  trials destroyed stone and both ore types. The no-qualifying-wearer alias defect
  no longer suppresses these vanilla block-destruction lists.
- Physical ring slots were checked by the parent's fixture. The wearer was in
  creative; this is no claim of wearer damage protection or survival damage QA.

The separate physically unequipped rerun and Portable Red Hole rerun are not
accepted by this entry yet. They remain parent-owned live checks. The earlier
fourteenth-candidate observations document the genuine defect, not a fixture
substitution or a successful pre-fix control.
