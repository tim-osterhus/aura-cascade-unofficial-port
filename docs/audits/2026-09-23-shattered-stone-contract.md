# Ring of the Shattered Stone Contract

## Source Evidence

The parity reference is `AuraCascade-592.jar` (SHA-256
`bcede2e852eb622656a402069073916824c682f5b0ade40fb899d04833cb21e3`). The
relevant bytecode is `pixlepix.auracascade.main.event.EventHandler.onExplode`
and `EventHandler.onEntityAttacked`, inspected with `javap -J-Xmx128m -c -p`;
the ring implementation is `pixlepix.auracascade.item.ItemExplosionRing`.

`onExplode` creates a seven-entry `Block` allowlist, runs only on the server,
and searches for players in an axis-aligned box extending three blocks from the
explosion center on each axis. If a searched player has an `ItemExplosionRing`,
the handler walks the entire affected-block list and removes each non-air block
whose block identity is not in that allowlist. The ring therefore lets ordinary
terrain be destroyed while sparing every other affected block; it does not
apply a separate distance test to each affected block.

The recovered SRG-to-name mapping identifies `field_150322_A` as sandstone
([mapping reference](https://mappings.dev/1.9.4/net/minecraft/server/v1_9_R2/Blocks.html)).
The old handler compares `Block` identities, so every metadata state of each
listed legacy block qualifies. The modern allowlist maps those state families
as follows:

| Legacy field | Legacy identity and metadata family | Modern blocks allowed to explode |
| --- | --- | --- |
| `field_150349_c` | Grass block | `GRASS_BLOCK` |
| `field_150322_A` | Sandstone: default, chiseled, legacy smooth | `SANDSTONE`, `CHISELED_SANDSTONE`, `CUT_SANDSTONE` |
| `field_150348_b` | Stone: stone, granite, polished granite, diorite, polished diorite, andesite, polished andesite | `STONE`, `GRANITE`, `POLISHED_GRANITE`, `DIORITE`, `POLISHED_DIORITE`, `ANDESITE`, `POLISHED_ANDESITE` |
| `field_150354_m` | Sand: sand and red sand | `SAND`, `RED_SAND` |
| `field_150346_d` | Dirt: dirt, coarse dirt, podzol | `DIRT`, `COARSE_DIRT`, `PODZOL` |
| `field_150347_e` | Cobblestone | `COBBLESTONE` |
| `field_150351_n` | Gravel | `GRAVEL` |

`SMOOTH_SANDSTONE`, red sandstone, rooted dirt, mossy cobblestone, ores, and
building blocks are separate modern block identities not represented by those
legacy allowlist entries; affected non-air instances of them are spared.

Parent review verified the naming trap against Minecraft 1.21.1's own
`BlockStateData` flattening table: legacy sandstone/type=smooth_sandstone (386)
maps to modern cut_sandstone. Modern smooth_sandstone instead comes from the
seamless double stone slab sandstone state (697), not the old sandstone block.

The shipped `ItemExplosionRing` has no damage callback. `EventHandler.onEntityAttacked`
handles damage for other amulets but contains no `ItemExplosionRing` check.
There is no source evidence that the ring prevents damage to its wearer; the
modern ring damage gate is consequently disabled. This is separate from the
block-list effect above.

## Port Contract And Coverage

- Ring detection uses the server-side player query centered on the explosion
  with the legacy three-block AABB bounds.
- With no ring wearer in that query, the affected list is returned unchanged.
- With at least one ring wearer, all affected non-air blocks outside the mapped
  ordinary-terrain allowlist are removed from the list, regardless of their
  distance from the wearer; list order is retained.
- The focused `UtilityGearLogicTest` cases cover every mapped legacy block
  family, non-allowlisted examples, air, a ring wearer in an AABB corner, blocks
  far from that wearer, and the no-wearer pass-through.
- No Gradle build, test task, or client run is part of this bounded correction.

## Parent-Owned Client Copy

Recommended exact value for
`tooltip.aura.ring_of_shattered_stone.residual`:

> Nearby blasts spare non-terrain blocks; the ring does not prevent wearer damage.

The current README, porting notes, guide entry, and substitution-closure wording
still contain the inverse terrain claim and/or imply wearer protection. Update
those parent-owned surfaces to this contract before treating their parity text
as current.
