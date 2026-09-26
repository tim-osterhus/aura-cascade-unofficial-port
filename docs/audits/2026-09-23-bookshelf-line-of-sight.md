# Bookshelf Line Of Sight

The original author made a source-only checkpoint. Parent fourteenth integration
subsequently compiled and passed all six tests in `BookshelfNetworkLogicTest`,
including five new real-vanilla raycast regressions, as part of 234 passing tests.
A bounded fourteenth live storage fixture now passes conversion, blocked/clear
LOS, deposit, and independent disconnection cases. Storage reload, powerless
behavior, and retrieval were not part of the fourteenth fixture. A fifteenth
launch now confirms the exact tested stone-entry/power reload; broader storage
reload remains unverified.

## Contract And Evidence

The current guide's Power and Connection page says **clear lines of sight**.
An earlier singular-only search missed that plural wording; it was not evidence
of a concurrent guide removal. README and PORTING_NOTES also describe LOS gating.
The original 592 `aura.page.books1` requires both a bookshelf connection and clear
coordinator-to-storage-shelf LOS.

592's `TileBookshelfCoordinator.hasClearLineOfSight` has no caller in the shipped
direct callgraph. Its shelf list is initialized empty, the apparent tick discovery
branch does nothing, and container/slot extraction iterates the list without a
separate visibility gate. Its unused 0.1-step, integer-truncation, non-air and
endpoint-radius rules therefore do not establish executed legacy behavior.

This repair restores the **intended documented contract using modern collider
semantics**, not those dead helper details. Connectivity and visibility remain
independent requirements. There is no blanket exemption for connected shelves.

## Repair And Verification Boundary

Vanilla `BlockGetter.clip` checks the initial cell; `VoxelShape.clip` can return
an inside hit on the full-cube coordinator. Previously accepting that source hit
incorrectly accepted rays before they examined intervening blocks.

`BookshelfNetworkLogic.hasLineOfSight` now accepts a `BlockGetter` and overrides
only the source and target block shapes to empty in its `ClipContext`. All other
shapes use vanilla COLLIDER semantics; fluids remain NONE. Only a MISS succeeds.
Endpoint neighbors are not exempt. No sampled ray, radius rule, coordinate
truncation, network traversal, power calculation or loaded-chunk guard changed.

Focused tests use map-backed block storage with real vanilla clipping/shapes:
source inside-hit reproduction followed by interior obstruction rejection and
removal recovery; adjacent/full-cube endpoints; intermediate stone/bookshelves
including endpoint neighbors; noncolliding torch/water; diagonal negative
coordinates; and the existing power curve. They do not mock raycast results,
require Aura mixin transformation, or claim world/network integration coverage.

## Fourteenth Live Fixture

The private storage-network run used Survival input in the fourteenth candidate
and seeded only the adjacent Aura nodes' initial `stored_power:1000`. Actual
conversion used `aura:basic_storage_book` on vanilla bookshelves. Its command
log and result records are
[`fourteenth-storage-network.log`](../../build/qa-audit/node-pilot-playtest/fourteenth-storage-network.log)
and
[`storage-network-results.jsonl`](../../build/qa-audit/node-pilot-playtest/storage-network-results.jsonl);
the action response trace is
[`actions.jsonl`](../../build/qa-audit/node-pilot-playtest/actions.jsonl).

- The adjacent converted shelf showed `Connected`, `1000 / 5`.
  [Screenshot](../../build/qa-audit/node-pilot-playtest/storage-direct-adjacent.png)
- The eight-shelf bent path remained connected, but its sole storage shelf was
  hidden by a solid LOS blocker. The browser showed `No visible storage shelves`,
  `1000 / 59`; a real stone deposit attempt retained the held stone.
  [Screenshot](../../build/qa-audit/node-pilot-playtest/storage-bent-los-blocked.png)
- Removing only the LOS blocker restored `Connected`, `1000 / 59` after 21
  game ticks. Power remained 1000 across the wait. One real stone deposit then
  created one stored entry and reduced power to 941.
  [Screenshot](../../build/qa-audit/node-pilot-playtest/storage-bent-los-clear.png)
- Removing the first BFS connector independently disconnected that same
  coordinator from its remote shelf. The browser showed `Disconnected`, `941 / 0`;
  the two-dirt attempt remained held, the original stone entry remained in the
  remote book, and power stayed 941.
  [Screenshot](../../build/qa-audit/node-pilot-playtest/storage-bent-disconnected.png)

After capture, the parent restored the first connector at `(570,180,285)` and
saved the world. After the fourteenth process saved and exited with code 0, a
fresh fifteenth launch verified the saved storage state. The fifteenth
[`latest.log`](../../build/qa-audit/core-systems/logs/latest.log) records the
remote shelf's `stored_book` with one `minecraft:stone` entry at 11:44:25 and
the adjacent node's full NBT with `node_state.stored_power:941` at 11:44:47.
The corresponding
[`actions.jsonl`](../../build/qa-audit/node-pilot-playtest/actions.jsonl)
contains positive `execute if data` predicates for that exact stored entry and
for `{node_state:{stored_power:941}}`.

An earlier read attempted top-level `stored_power` and returned a missing-tag
error; the field is nested under `node_state`. That wrong-path query is not a
failed persistence result: the later full-NBT read and correctly nested
941-power predicate both passed. This closes only the single stone-entry and
941-power save/reload case. The 1000-power fixture value was injected; powerless
rejection, retrieval, broader storage persistence, and naturally generated Aura
remain unverified.
