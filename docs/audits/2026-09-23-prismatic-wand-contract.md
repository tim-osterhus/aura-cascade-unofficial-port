# Prismatic Wand Contract

Source: `AuraCascade-592.jar`, `ItemPrismaticWand.func_77659_a`, inspected with
`javap -c -p`. This supersedes the earlier port's snapshot-clipboard contract.

## Original Behavior

- Three modes: Selection, Copy, Paste. There is no undo mode or history.
- Each Selection click becomes the first corner, shifting the previous corner
  to the second. Reversed endpoints are normalized for traversal.
- Copy records source coordinates and the source minimum's offset from the
  player position, using truncation toward zero for player coordinates.
- Paste reads source blocks live from those coordinates. Its destination is
  relative to the current player position, not the clicked block.
- Occupied destinations are skipped. Survival needs the corresponding block
  item, with consumption after successful placement. Creative can place source
  states directly. The original does not snapshot block-entity inventories,
  stored power, source-fluid buckets, or numeric runtime block-state IDs.

## Modern Boundaries

The port bounds a selection to 512 total cells, including air, and preflights
source and destination world height, border and loaded chunks before writes.
This prevents an almost-empty selection from causing unbounded iteration or
chunk generation. Only successful placements consume materials. The old port's
serialized snapshot clipboard is inert and is cleared on mode change or a new
copy; unrelated custom item data and selection coordinates remain preserved.

The current implementation places source block states directly rather than
replaying every old Forge ItemBlock placement side effect. Special multi-block
and component-rich placement require explicit negative/control validation; do
not infer arbitrary modded-block compatibility from plain-block tests.

## Validation

Focused tests cover coordinate state, inert snapshot migration, bounded volume,
unsafe-region rejection before any mutation, live source changes, occupied
destinations, failed placement and exact material charging. They require a
successful integration test run. Client selection/mode cycling, player-relative
origin, save/reload and representative survival placements remain open.
