# Vortex contract, 2026-09-23

This is a bounded V1/P2 implementation record, not full gameplay parity or client acceptance.
Reference: shipped `AuraCascade-592.jar` (`bcede2e852eb622656a402069073916824c682f5b0ade40fb899d04833cb21e3`), especially `CraftingCenterTile`, `AuraTilePedestal`, `PylonRecipe`, `ItemMaterial`, and `ConsumerBlock` bytecode. Part 2 of the video is a 1.7.10 observation, not a version-matched specification.

## Version-matched rules

- Four horizontal adjacent pedestals are required. Items match a four-component recipe without positional ordering.
- Falling transfers deliver power to a pedestal, calculated per moved color from fall distance and relative mass. The pedestal does not spend stored aura to craft. Power for a colored requirement must be the specified color; a White requirement accepts power from any power-generating color. The generic node power credited by the shared transfer kernel must be removed in full after the pedestal callback, including rejected or over-cap power.
- Each pedestal accumulates power up to its assigned component cost. Once all four receipts satisfy their costs, the center consumes the four items, clears receipts and creates the output. There is no separate timer/progress gate in the shipped `CraftingCenterTile`.
- `ItemMaterial.getRecipeItem` gives each colored Arcane Gem one diamond at 60,000 White power and three matching Arcane Ingots at 20,000 White power each. These eight four-pedestal recipes are now local vortex resources. White here means the legacy any-color power wildcard, not specifically white aura storage.
- The same class gives Arcane Prism a normal processor recipe from eight distinct Arcane Gems. The existing ingredient set is correct, but its machine type is still a shared-file correction coordinated with the parent.

## Modern boundary

`VortexPedestalBlockEntity.receiveFallingPower(AuraStorage, int, AuraEnvironment)` is the shared-network hook. `AuraNetworkBlockEntity` must call it immediately after applying a falling transfer into a pedestal, using the moved color quantities and source-minus-target Y distance. Pedestal receipts/requirements and held items are saved to NBT and sent by the existing block-entity update packet. The center derives its state from the four local pedestals, including on the client; `inspectionSnapshot(Level, BlockPos)` exposes immutable recipe id, total received/required power, and per-pedestal item/color/received/required values. `VortexPedestalBlockEntity.inspectionSnapshot()` exposes the immutable local receipt. HUD presentation belongs to the parent.

### Pedestal interaction and visuals

`AuraBlock.func_180639_a` in 592, offsets 114-238, handles non-sneaking pedestal activation on the server: it spawns the existing pedestal stack at the player's position, removes one item from the selected hotbar slot, and makes that one item the new pedestal stack. Empty-hand activation therefore extracts the old item. Sneaking bypasses this pedestal branch. `RenderPedestal.renderTileEntityAt` displays the held stack as a floating, turning item centered at block `y + 1.16`; it uses an item renderer rather than a special pedestal item texture.

The 1.21.1 block now implements the same non-sneaking right-click exchange and empty-hand extraction. It copies exactly one offered item with components intact, consumes one from a survival hand, and resets the previous recipe receipt on every manual replacement so power cannot be reused for a different held item. The old stack goes into player inventory where possible, with a world drop only for overflow; this is an intentional inventory-safety adaptation of 592's unconditional player-position drop. Creative placement copies one without consuming the hand. Sneaking still bypasses exchange. Dropped-item capture remains available. A client block-entity renderer shows the synchronized stack at the legacy height, using its registered item model and the existing original pedestal block textures. A small color-matched particle appears around a pedestal with a positive recipe receipt; the 592 renderer itself contains no receipt particle, so this is a visual feedback adaptation. Neither interaction nor rendering injects stored power.

Client integration hook: `VortexPedestalRenderer.bootstrapClient()` after the pedestal block-entity type is registered. It is intentionally not wired in the shared client initializer by this bounded lane.

The second bounded audit resolved all six earlier vortex resources against `ConsumerBlock.getRecipeItem` and `AuraBlock.getRecipeItem`:

| Port output | 592 recipe kind | Four pedestal inputs or crafting pattern |
| --- | --- | --- |
| Kaleidoscopic Enchanter | Pylon | 4 Black Arcane Gems, 250,000 White power at each pedestal |
| Cascading Looter | Pylon | 4 Yellow Arcane Gems, 100,000 Yellow power at each pedestal |
| Cascading Spawner | Pylon | 4 Violet Arcane Gems, 100,000 Violet power at each pedestal |
| Ritual of the Nether | Pylon | 4 Red Arcane Gems, 100,000 Red power at each pedestal |
| Prismatic Processor | Crafting bench | `GPG/GCG/GGG`, where `G` is glass, `P` is Arcane Prism, and `C` is the base Cascading Processor |
| Fluxing Node | Crafting bench | `RNR`, where `R` is Red Arcane Ingot and `N` is the base Aura Node |

The one-component `PylonRecipe` constructor repeats its component four times; it does not mean one pedestal or one gem. The four true vortex resources now mirror that requirement, and the two crafting-bench outputs were removed from the vortex catalog/resources. No `Ritual Tome` recipe or item was found in the shipped jar's relevant recipe-producing classes; the audited Nether output is the ritual block. The earlier `max_progress` timer fields are gone from corrected resources; the catalog's default field remains a format compatibility detail, not a crafting gate. Both crafting-bench outputs already have registered block/item forms in `AuraContent`, so the missing integration is their vanilla crafting data (one wrong recipe and one absent recipe), not a new block bootstrap.

## Verification and open work

- Focused source tests cover unordered and four-identical item matching, unequal per-pedestal requirements, colored falling-power accounting, receipt NBT round-trip, one-item selection with components intact, and all twelve vortex resource shapes. Gradle/build/client launch are intentionally deferred to the parent validation lane.
- Shared work coordinated with the parent: add the pedestal transfer hook; remove eight processor gem shortcuts; switch the Arcane Prism resource to the normal processor; replace the wrong `consumer_block_ore_adv.json` shaped recipe with `GPG/GCG/GGG`; add the missing Fluxing Node `RNR` crafting resource; update acquisition and world-recipe tests plus guide text. The latter crafting resources and tests are outside this bounded ownership.
- Client acceptance remains open: verify four colored receipts, recipe identity, complete output, and the processor-ingot to vortex-gem to processor-prism chain in survival without injected power. The 1.7.10 video's HUD values corroborate the gem asymmetry but do not establish build-592 visual/UI equivalence.
