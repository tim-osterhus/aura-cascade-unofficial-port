# Angelsteel Drop-Local Fortune Repair

Source checkpoint only. No Gradle, client, or live validation was run in this lane.

## Failure And Repair

The previous BEFORE/AFTER/CANCELED callbacks temporarily enchanted the held tool.
Nested Kaleidoscopic breaks could pop an outer Fortune snapshot, then restore a
captured temporary enchantment after the Fortune bookkeeping was already empty.
The resulting Fortune enchantment could persist on the player's actual item.

All three Fortune callbacks, their map, and their snapshot record are removed.
The existing `Block.playerDestroy` drop redirect now obtains a drop-only tool via
`AngelsteelToolHelper.dropFortuneTool`. Only an Angelsteel mining tool, correct for
the block and not harvesting a `CropBlock`, receives a copy with its existing
Fortune raised to its strictly higher stored Fortune buff. The Fortune holder is
resolved from the actual server enchantment registry. The original tool is never
changed by this helper; all other enchantments, components and stored buffs survive
the copy. There is no drop-time buff reroll. An uninitialized tool remains unchanged
until its ordinary inventory tick initializes its buffs.

The Kaleidoscopic two-ingot conversion branch runs first, unchanged, including its
random draw and eligibility. Only the fallback vanilla loot call uses the copied
tool. Break permissions, cancellation, and normal tool wear remain outside this
drop-only operation. Custom block overrides that bypass base `Block.playerDestroy`
remain an existing compatibility limitation; this repair does not intercept them.

## Focused Regression Coverage

`AngelsteelDropFortuneTest` runs stack-level helper cases with registered Angelsteel
items in the same Fabric target classloader. Explicit tool rules avoid dependence
on unloaded test datapack tags; isolated enchantment holders exercise component
preservation, not vanilla Fortune loot-table probabilities.

- Original tool unchanged, copied Fortune raised, Silk Touch and custom data/name/damage retained.
- Repeated and already-elevated drop calls do not mutate the source or stack bookkeeping.
- Equal/stronger existing Fortune remains unchanged.
- Incorrect tool, crop (even when correct for drops), and non-Angelsteel gates.
- No initialization or randomization for an uninitialized tool.
- Source wiring guard against reintroducing the removed callbacks/map.

Parent validation still needs to execute the tests and exercise a combined
Angelsteel/Kaleidoscopic Red break, including a canceled break, checking held
components before and after. This document records a bounded leak repair, not a
full utility or custom-block compatibility parity claim. Fruit completion and the
parent-owned damage-phase mixin are untouched.
