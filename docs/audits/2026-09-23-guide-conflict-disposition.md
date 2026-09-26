# Guide Conflict Disposition Against 592

Bounded source review of [page coverage](2026-09-23-guide-page-coverage.md),
2026-09-23. The initial review was read-only; the later bounded D6 source fix
and focused regression are recorded below. No guide or acceptance-ledger edits
were made. No Gradle, tests, game client or UI ran; only small
`javap -J-Xmx128m -c -p` reads with the local Java 21 tool informed the source
findings. These are not live results.

Reference: local `AuraCascade-592.jar`, SHA-256
`bcede2e852eb622656a402069073916824c682f5b0ade40fb899d04833cb21e3`.
Byte offsets below refer to the named method, not source line numbers.
SRG identities were independently checked against `fields.csv` in the official
[MCP stable 22 / Minecraft 1.8.9 mapping archive](https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable/22-1.8.9/mcp_stable-22-1.8.9.zip),
read in memory without extracting files. In particular, `field_151587_i` is
**lava**, while `field_151581_o` is fire; `field_150377_bs` is End Stone,
`field_150325_L` wool, `field_151042_j` iron ingot and `field_151100_aR` dye.

## Six Confirmed Behavior Gaps

Describing current behavior accurately in the guide does not authorize these
substitutions or complete the original implementation.

| ID / comparison | Actual 592 code evidence | Current port and disposition |
| --- | --- | --- |
| D1: Wool + iron versus crystal + iron | `ItemMaterial.getRecipeItem`, offsets 0-101: material index 0 loops `aura.dyes`; constructs non-prismatic `ProcessorRecipe` with one iron ingot (65) and one wool block of the selected metadata (78-84), yielding one Arcane Ingot. | Eight `recipes/processor/arcane_ingot_*.json` files instead require matching Aura Crystals. **Real progression recipe mismatch**, not outdated prose. Restore the original wool routes; retaining an extra crystal route would be a separate compatibility decision, not original parity. |
| D2: Prismatic wool-to-dye | `ProcessorRecipeRegistry.init`, 0-59: for i=0..15, one wool metadata `15-i` becomes one legacy dye metadata `i`; `prismaticOnly=true` at 24. `CommonProxy.init` calls this registry initializer at 26. | No corresponding route in `AuraWorldRecipeCatalog` or its resources. **Missing operation.** Preserve all sixteen mappings and the advanced-only gate; legacy dye metadata includes items later flattened into separate modern IDs, so do not blindly assume every result is a modern named dye. |
| D3: Smelter despawn protection | Inherited `ConsumerTile.func_73660_a`, 273-291: every server `gameTime % 500 == 0`, call `AuraUtil.keepAlive(this,3)`, independently of power/work. `AuraUtil.keepAlive`, 0-83: query all nearby `EntityItem`s in the pos-3 to pos+3 AABB; set `lifespan=Integer.MAX_VALUE` (67-71) and item age 0 (74-77). | No corresponding keep-alive path in modern common/specialized consumer ticking. **Missing shared consumer behavior**, not just Smelter flavor text. Ordinary output age reset during replacement does not protect waiting inputs. A modern lifetime API adaptation needs explicit implementation/tests; a warning about ordinary despawn does not close it. |
| D4: Fortified End Stone versus obsidian | `BlockExplosionContainer.getRecipeItem`: each material branch constructs a normal Processor recipe using End Stone `field_150377_bs`; e.g. Dirt at 35 plus Dirt at 48, Obsidian branch at 600 plus actual Obsidian `field_150343_Z` at 613. Wood supplies six plank-metadata alternatives. | Six `fortified_*.json` recipes use obsidian as the fortifying ingredient; the Obsidian form uses two obsidian. **Real recipe mismatch.** Restore End Stone plus the matching base material, including the original plank alternatives. |
| D5: Enchanter ingot versus crystal | `EnchanterTile.validItemsNearby`, 138-158, and `onUsePower`, 150-170, require `ItemMaterial` with `materialIndex==0`, i.e. Arcane Ingot. Work reads its Aura at 188 and decrements that offered stack at 264-272 after the success/failure branch. | `AuraConsumerBlockEntity.findEnchantInput` calls `AuraItems.auraCrystalColor`; the successful runtime crystal fixture therefore validates the current substitute, **not the original input contract**. Restore supported-color Arcane Ingots and consumption on failed attempts. |
| D6: Extinguisher lava | `EntityExtinguisherFairy.func_70030_z`, 14-28 clears the owner's burning state; 31-70 obtains the block at the fairy's own position and removes it when material equals `field_151587_i` (**lava**). No surrounding lava-volume scan is present. | **Source fix present; focused test not run.** `FairySystem.extinguish` preserves owner fire clearing and now removes only `Blocks.LAVA` at the fairy's current block position; source and flowing level states are covered. It does not clear FIRE/SOUL_FIRE or search a surrounding volume. Live behavior remains unverified. The prior fairy-restoration contract's identification as fire material was wrong and is superseded by this bytecode/mapping evidence. |

D1 wool metadata, from `EnumAura`'s static initializer: White `[0]`, Green
`[5,13]`, Black `[12,15,7,8]`, Red `[14]`, Orange `[1]`, Yellow `[4]`, Blue
`[3,9,11]`, Violet `[2,6,10]`. Together these cover all sixteen wool colors.
`BlockRegistry.registerRecipe` invokes each item's/block's returned recipe;
`ProcessorRecipe.registerRecipe` adds it to the live Processor registry.
These are registered recipes, not unused recipe-page examples.

## Prose Conflicts, Not New Defects

| Comparison | Actual 592 bytecode | Disposition |
| --- | --- | --- |
| Extremely Dense: one million versus 100,000 | `ExtremelyDenseStorageBook.getMaxStackSize`, 0-2 returns **100000**; `getHeldStacks`, 0-1 returns **1**. | Modern 1 x 100,000 matches. The million-item caption is stale. |
| Mineral: eight x 10,000 versus eight x 100,000 | `MineralStorageBook.getMaxStackSize`, 0-2 returns **100000**; `getHeldStacks`, 0-2 returns **8**. | Modern 8 x 100,000 matches. Classification/tag compatibility is separate from this disproven capacity gap. |
| Grower middle Dirt requirement | `PlanterTile.validItemsNearby`, 0-1 returns true. `onUsePower`, 12-20 selects pos.above(2); 35-70 calls the target block tick fifty times. No check of pos.above(1), Dirt or plant type. | Port's absent Dirt gate matches code. Plant support needs are ordinary block behavior, not a special machine requirement. Existing stop-on-block-type-change safety adaptation is separate. |
| Fisher 3x3 versus 4x4 | `FisherTile.hasWater`, 0-20 iterates inclusive offsets (-1,0,-1) through (2,0,2); 52-89 checks the block below each for still OR flowing water. | **4x4 below** matches current implementation. The 3x3 wording is stale; no deeper/open-water machine gate appears here. Modern fishing loot-table behavior remains its existing platform adaptation. |
| Alternator eight-minute wording | `AuraTilePumpBase.getAlternatingFactor`, 0-24 computes `(1 + sin(pi * gameTime / 10000)) / 2`. | Full period is **20,000 ticks**, 16 minutes 40 seconds at 20 TPS. Not a new cadence defect; do not change pump timing to follow the prose. |
| Red Hole five-times lifetime wording | `ItemRedHole.getEntityLifespan`, 0-3 returns **30000**. | Current 30,000-tick contract matches. Five times vanilla's 6,000 ticks is not a contradictory behavior. |

## Other Coverage-Report Statements

- Burning Pump 300, Barbarian five-percent steps and the three-axis-block
  Shattered Stone range remain the previously source-verified contracts, as
  requested; no reopening from language text. Fruit food/drink was already
  repaired today and is excluded, not recorded as a new missing branch.
- Wing controls, Baubles-to-physical-equipment/fairy binding, Forge loot/spawn
  APIs, ore dictionaries/common tags and absent optional Ender fluid remain
  their existing platform/acceptance boundaries. This review does not turn
  those language comparisons into new defects or silently certify their runtime.
- Miner fortification's concrete ingredient defect is D4. Ritual extent,
  mapped families and loot/spawn distributions remain with their existing
  contracts; nonspecific legacy prose alone cannot establish another mismatch.

## Handoff

Six source-proven gaps were identified. D6 now has a bounded source correction
and focused regression, neither compiled nor run here; its world behavior still
needs live verification. D1-D5 remain for explicit owner disposition and
regression/runtime follow-up: prioritize D1/D5 progression inputs and D3
waiting-item loss, then the bounded D2/D4 recipe restoration. Keep book
capacities, Grower target rules and Fisher footprint unchanged. This document is
a disposition of the specified conflicts, not a new exhaustive port audit or
release sign-off.
