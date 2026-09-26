# Aura Cascade Unofficial Port

## Unreleased `0.2.1+1.21.11`

Forward port of the corrected 0.2.1 gameplay to Fabric 1.21.11, with Java 21.
Uses the external Patchouli: Fabric Edition dependency and adapts saved data,
rendering, item models, recipes and Minecraft hooks. Validation is in progress;
no target publication is claimed by this entry.

## Beta `0.2.1+1.21.1`

Published as Aura Cascade Reimagined. Restores dropped-item crafting feedback,
one-shot empty-node connection previews, first-open Creative tab discoverability,
stable Aura HUD rows and clearly separated Encyclopedia landing links. Connection
geometry is revalidated before transfers. Existing registry IDs remain unchanged.
See `docs/releases/0.2.1-beta.md` for validation and release details.

The reviewed 1.21.1 source checkpoint was rebuilt on 2026-09-25: 252 tests in
62 suites pass, plus 10 publishing-utility tests. Private runtime files and
credentials are excluded from source publication.

## Beta `0.1.1+1.21.1`

Parity-closure follow-up beta release of the unofficial modern Fabric port of Aura Cascade.

This release targets:

- Minecraft `1.21.1`
- Fabric
- Java `21`

## Highlights

- Ported the core Aura Cascade progression loop to modern Fabric, including aura nodes, pumps, monitors, consumers, vortex crafting, rituals, storage systems, utility gear, fairies, guidebook content, and late-game progression content.
- Restored the mod's main item, block, and texture surface from the original Aura Cascade artifact rather than replacing the mod with a loose spiritual successor.
- Restored Ring of the Shattered Stone loose-block filtering, Prismatic Wand clipboard closure for representative block entities and source fluids, and the kaleidoscopic legacy matrix through modern runtime seams.
- Bumped packaged outputs and release surfaces to `0.1.1+1.21.1`, including the guidebook, localization, README, `PORTING_NOTES.md`, parity audits, and `build/libs/aura-cascade-0.1.1+1.21.1.jar`.

## Included In This Beta

- Eight-color aura storage and transfer gameplay
- Aura nodes, manipulators, monitors, pumps, and fluxing node support
- Consumer and progression block families including processors, synthesizer, miner, looter, spawner, enchanter, rituals, and vortex crafting
- Angel's Steel progression, utility items, swords, accessories, books, and storage systems
- Fairy systems, Ring of Binding support, and guidebook content
- Restored Aura visual assets and modern Fabric packaging

## Crafting And Acquisition Surface

- Direct crafting now loads on `1.21.1` for the node/control, pump, travel, rebound, miner, `bookshelf_coordinator`, and End-ritual block ids, with the supporting `aura_node` recipe restoring the node-derived chain end to end.
- `fortified_planks` and the other fortified containment blocks stay on the documented processor seam, while `consumer_block_loot`, `consumer_block_spawn`, and `ritual_nether` stay on the documented four-pedestal vortex seam instead of silent bench recipes.
- `storage_bookshelf` remains a conversion-only block created by using a Storage Book on a vanilla bookshelf, and `aura_node_pump_creative` remains intentionally creative-only with no survival recipe.
- Fresh repo-local `./gradlew --console=plain runServer` evidence no longer logs Aura `Parsing error loading recipe` entries for the repaired `src/main/resources/data/aura/recipe/` tree.

## Compatibility And Build Notes

- This mod is built for Java `21`.
- The Gradle build targets Java release `21` directly.
- `./gradlew --console=plain test`, `./gradlew --console=plain build`, and repo-local `./gradlew --console=plain runServer` were validated against the Java `21` toolchain during release verification.
- Java `21` is the only supported and verified runtime for this beta release.
- Older Java versions such as `17` are not compatible with this build target.
- Newer Java versions may or may not run, but they have not been validated for this release and should be treated as unsupported until tested explicitly.

## Shipped Runtime Bounds

- Ring of the Shattered Stone now keeps wearer explosion protection while filtering nearby dirt, stone, sand, and gravel out of explosion block damage inside the three-block protection radius.
- Prismatic Wand copy and paste now capture source fluids and representative block-entity state, replay copied block-entity data after placement, and consume merged block, contents, and bucket materials from the clipboard.
- Kaleidoscopic enchantments now map the recovered single-color table and pairwise matrix through modern runtime seams, with explicit shipped bounds for mature-crop 3x3 harvesting, supported iron/gold/copper double-ingot drops, main-hand `Red + Violet` damage recovery, and `destroy speed >= 8.0` hard-material boosts.

## Release Status

This build should be treated as a real beta:

- major systems are present
- the ring, wand, and kaleidoscopic closure surfaces are now reconciled into the release docs and parity audits
- the mod builds and loads successfully
- the remaining limits are documented modern bounds rather than hidden parity regressions

If you are looking for a modern playable Aura Cascade port with the `0.1.1+1.21.1` parity-closure slice shipped and documented honestly, this beta is ready for testing and feedback.
