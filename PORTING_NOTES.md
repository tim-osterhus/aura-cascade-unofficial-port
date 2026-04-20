# Porting Notes

## Baseline Source Evidence

- Canonical artifact: `AuraCascade-592.jar`
- SHA-256: `bcede2e852eb622656a402069073916824c682f5b0ade40fb899d04833cb21e3`
- Primary evidence files:
  - `mcmod.info`
  - `assets/aura/lang/en_US.lang`
  - `pixlepix/auracascade/data/EnumAura.class`
  - `pixlepix/auracascade/registry/BlockRegistry.class`
  - `pixlepix/auracascade/data/recipe/ProcessorRecipeRegistry.class`
  - `pixlepix/auracascade/data/recipe/PylonRecipeRegistry.class`
  - `pixlepix/auracascade/lexicon/LexiconData.class`

## Identity Baseline

- Shipped mod id: `Aura`
- Shipped display name: `AuraCascade`
- Shipped version: `592`
- Shipped Minecraft target: `1.8.9`
- Shipped description: "A magic mod focused on emergent complexity and enabling unique designs"
- Attribution carried forward from jar metadata: `pixlepix` and `williewillus`

## Aura Baseline

`EnumAura.class` exposes eight legacy colors and establishes them as a fixed parity surface:

- White
- Black
- Orange
- Red
- Yellow
- Green
- Blue
- Violet

Jar-derived behavior anchors recovered from localization and enum strings:

- Red aura absorbs TNT explosions and pushes aura upward.
- Black aura has zero weight, moves vertically only, and is created or destroyed by the black manipulator.
- Orange aura moves horizontally only and induces directional current.
- Yellow aura gives a x2 ascent boost but rapidly decays in nodes.
- Green aura changes effective weight by time of day.
- Blue aura gives a strong rain-only ascent boost and a penalty otherwise.
- Violet aura grows logarithmically in nodes, dissipates below low thresholds, and overloads above the upper cap.

## Registry And Naming Baseline

The shipped English localization file contains 100 item-name keys, 45 tile-name keys, 6 kaleidoscopic enchantment keys, 101 lexicon entry keys, and 261 lexicon page keys. Those counts are the first parity checkpoint for future content tasks.

Core item families recovered from `en_US.lang`:

- Aura Crystal and Encyclopedia Aura
- Arcane ingots for white, black, red, orange, yellow, green, blue, and violet
- Arcane gems for white, black, red, orange, yellow, green, blue, and violet
- Arcane Prism
- Angel's Steel ingots plus pickaxe, axe, sword, and shovel tiers from first through twelfth degree
- Ring of Binding, Fairy Charm, six protection amulets, Amulet of the Angel's Wing, Sash of the Angel's Heels, Amulet of the Forbidden Fruit, Ring of the Shattered Stone
- Prismatic Wand, Mirror of the Angel, Portable Red Hole, Portable Black Hole, Transmuting Sword, Sword of the Barbarian, Sword of the Thief
- Nine storage-book variants plus mineral, mob-drop, farming, and mod-specific books

Core block and machine families recovered from `en_US.lang` and `LexiconData.class`:

- Aura Node, Aura Capacitor, Conserving Aura Node, Black Aura Manipulator, Orange Aura Manipulator, Monitor, Fluxing Node
- Burning, Illumination, Momentum, Projectile, and Redstone pumps plus alternating variants and a creative pump
- Vortex Controller and Vortex Pedestal
- Cascading Processor, Prismatic Processor, Cascading Smelter, Cascading Grower, Cascading Fisher, Cascading Brewer, Cascading Colorer, Cascading Miner, Cascading Looter, Cascading Spawner, Cascading Synthesizer, Kaleidoscopic Enchanter, Ritual of the Nether, Ritual of the End, Bookshelf Coordinator
- Traveler's Bricks, Rebounding Enigma, Storage Bookshelf, and six fortified block variants

Registry shape recovered from class strings:

- `BlockRegistry.class` scans `pixlepix.auracascade.block` and `pixlepix.auracascade.item`, registers blocks, items, tile entities, and special creative-tab visibility.
- `ProcessorRecipeRegistry.class` owns dropped-item processing recipes and distinguishes normal versus prismatic recipes.
- `PylonRecipeRegistry.class` owns vortex infusion outputs and component requirements.

## Guidebook Coverage Baseline

`LexiconData.class` and `en_US.lang` show the shipped guidebook categories:

- Quests
- Walkthrough
- Basics
- Special Aura Colors
- Special Aura Nodes
- Power Consumers
- Fairies
- Accessories
- Enchantments

Major guidebook coverage recovered from the entry keys:

- Onboarding and progression: introduction, tutorial, 14 numbered quest entries, video walkthrough, patron/support page
- Core mechanics: basic setup, basics, power, aura flow, controls, monitor, pumps, comparator interactions
- Aura colors and node variants: black, red, orange, yellow, green, blue, violet, alternating pumps, fluxing node, capacitor, conserving node, manipulators
- Consumers and progression blocks: vortex infusion, colorer, synthesizer, smelter, fisher, looter, spawner, grower, books, miner, ritual of the Nether, ritual of the End, brewer
- Equipment and utility: angel items, prismatic wand, mirror, Angel's Steel, Angel's Steel swords, protection amulets, Transmuting Sword, Sword of the Barbarian, Sword of the Thief, Ring of the Shattered Stone, red hole, black hole, Traveler's Bricks, Rebounding Enigma, forbidden-fruit amulet
- Kaleidoscopic enchantments: base enchanter page plus basic effects, area-of-effect interactions, combat interactions, looting interactions, negative interactions, mining-speed boosts
- Fairy system: overview plus combat, debuff, buff, steal, push, shoot, savior, fetch, bait, breed, scare, extinguisher, digger, light, fall, and training variants

## Selected Compatibility Direction

- Accessories and Baubles replacement: keep the gameplay contract behind an internal equipment abstraction first. The port should reproduce Ring of Binding, amulets, sash, and ring behavior without tying core logic to one third-party slot API before the feature tasks validate a concrete dependency.
- External energy replacement: preserve aura and falling-power behavior as the authoritative simulation. Any later RF or FE style compatibility should be adapter-only, centered on Fluxing Node export behavior, and must not flatten aura transport into a generic power network.

## Accessories And Utility Gear Pass

This execution pass ports the accessory and utility gear behind an internal accessory bridge under `src/main/java/pixlepix/auracascade/compat/`, with a default modern binding surface that lets players explicitly equip one ring, one amulet, and one belt without coupling core aura logic to a specific external accessory mod.

Documented substitutions in this pass:

- Ring of Binding: the ring now binds and releases `Fairy Charm` items up to the shipped 15-fairy cap, and the later fairy pass completed live fairy summoning and persistence through the ring-backed allay runtime.
- Amulet of the Angel's Wing: the old dedicated up/down keybind is replaced with an equipped-use interaction. Using the equipped amulet ascends to the next open standing spot, and looking down while using it descends.
- Sash of the Angel's Heels: the modern pass keeps the hill-climb identity through passive jump assistance and a collision-charged wall climb instead of the exact legacy step-height plus burst-packet behavior.
- Protection amulets: damage-family blocking is preserved, with deterministic modern healing conversion for blocked hits and a partial projectile heal for the yellow amulet. The exact legacy heal ratio is reconstructed rather than bytecode-confirmed in this environment.
- Ring of the Shattered Stone: the pass preserves the explosion-channeling wearer protection role, but it still does not limit explosion block damage to dirt, stone, sand, and gravel within the wearer's protection radius.
- Mirror of the Angel: right-click or direct hits now deflect nearby fireballs and wither skulls toward the nearest ghast when possible, otherwise along the player's look vector.
- Portable Red Hole and Portable Black Hole: both now persist while dropped via unlimited item lifetime; the red hole explodes every five seconds while dropped, and the black hole continuously deletes cobblestone from player inventories while carried.
- Prismatic Wand: selection, copy, and paste modes are implemented. The clipboard now stores only non-air block states with direct item forms, skips block entities and fluids during copy, and consumes the direct block items needed for placement rather than the full legacy drop-and-metadata logic. The modern guidebook, item tooltip, and localized wand status text now name those exact limits instead of leaving them implicit.
- Sword of the Thief: villagers now have the documented 25% drop chance, but the drop is chosen from a surviving trade offer at death time rather than from an exact last-opened trade slot.

## Fairies And Late-Game World Pass

This execution pass ports the late-game fairy and world-interaction slice under `src/main/java/pixlepix/auracascade/fairy/`, `src/main/java/pixlepix/auracascade/block/`, and `src/main/java/pixlepix/auracascade/block/entity/`, with deterministic tests covering fairy role selection, ring serialization, ritual mappings, and miner containment/yield rules.

Documented substitutions in this pass:

- Fairy runtime: the legacy custom fairy entities plus packet sync are reconstructed as ring-backed, tagged allays that orbit the equipped player, carry the attuned `Fairy Charm`, and execute the recovered role families on the server. This preserves in-world summoned helpers and persistence without reintroducing a bespoke legacy renderer/network stack.
- Fairy charms: modern Fabric no longer has metadata-driven item subtypes, so `Fairy Charm` stores its role in custom item data and can be re-attuned in-hand with sneak-use instead of crafting a separate metadata variant for each role. The base untuned charm now carries the shipped basic no-effect `Fairy`, and the attuned names follow the recovered jar strings from `Fighter Fairy` through `Training Fairy`.
- Cascading Miner: the legacy bouncing explosion helper is modernized into a charged singularity state on the block entity. It still stresses nearby fortified containment, explodes when containment is absent, and releases ore on redstone pulse, but the singularity is represented with particles and stored charge instead of a separate invisible bouncing entity.
- Rituals: the Nether and End rituals rewrite nearby block context and apply direct danger to nearby entities, but they do not rewrite biome data in-place on the modern chunk format. This keeps the destructive late-game identity while staying within a narrow block-entity implementation seam.

## Storage, Guidebook, And Content Data Pass

This execution pass ports the storage-book, bookshelf-network, and guidebook slice under `src/main/java/pixlepix/auracascade/item/books/`, `src/main/java/pixlepix/auracascade/block/`, `src/main/java/pixlepix/auracascade/block/entity/`, `src/main/java/pixlepix/auracascade/lexicon/`, and the matching `assets/aura` and `data/aura` surfaces.

Documented substitutions in this pass:

- Encyclopedia Aura: the legacy custom lexicon GUI and `PacketSyncQuestData` flow are replaced with a dynamically populated vanilla written book. The content still ships onboarding and implemented-system entries, but it no longer depends on a bespoke client GUI or network packet.
- Bookshelf Coordinator: the modern pass keeps the connected-bookshelf identity, line-of-sight shelf scan, and recovered power curve `5 * shelfCount * 1.05^shelfCount`, but exposes the interaction through direct use and status messages instead of a dedicated GUI.
- Storage Bookshelf acquisition: the shelf has no standalone item form in this pass. Players obtain it by using any storage book on a vanilla bookshelf, and breaking it returns the vanilla bookshelf while the stored book is ejected separately.
- Storage-book parity source of truth: the recovered bytecode returns `1 x 100000` for Extremely Dense books and `8 x 100000` for Mineral books. The legacy English lexicon pages overstate those values, so this port follows the class behavior and documents the text mismatch instead of preserving the inflated numbers.
- Discoverability closure: bytecode spot-checks show `BlockStorageBookshelf.shouldDisplayInTab()` returned `false`, `ItemStorageBook.shouldDisplayInTab()` returned `true`, and the legacy `AuraBlock` subtype list kept `pumpCreative` visible. The modern `itemGroup.aura` creative tab now keeps every registered Aura item form reachable while `storage_bookshelf` itself stays a conversion-only block with no standalone item registration.

Documented acquisition notes in this pass:

- The port now ships data-driven acquisition for the previously unresolved node, pump, monitor, travel, rebound, fortified, looter, spawner, miner, and ritual block families, plus a supporting `aura_node` crafting recipe so the node-derived chain is usable end to end.
- `aura_node_black`, `aura_node_conserve`, `aura_node_capacitor`, `aura_node_orange`, `monitor`, `travelers_bricks`, `rebounding_enigma`, and `ritual_end` keep the recovered jar bench shapes directly; the supporting base `aura_node` recipe mirrors the legacy shape with modern `quartz` and `redstone`.
- `aura_node_pump`, `consumer_block_miner`, and the themed pump variants keep the recovered legacy shapes, color affinities, and upgrade chain, but substitute modern equivalents where the old 1.8 metadata-only item constants do not map cleanly in this environment: `magma_cream` for the burning catalyst, torches/feathers/arrows/redstone blocks for the pump heads, redstone dust for the alternating upgrade seam, and a diamond pickaxe for the miner head.
- The fortified block family now uses processor recipes that combine obsidian with the modern base block inputs; `fortified_planks` is intentionally keyed to `oak_planks` because the current world-recipe loader accepts concrete item ids rather than plank tags or metadata families.
- `consumer_block_loot`, `consumer_block_spawn`, and `ritual_nether` now use four-pedestal vortex recipes instead of the legacy single-component pylon recipes so the modern port keeps one deterministic late-game crafting seam.
- `aura_node_pump_creative` remains intentionally creative-only content and is not given survival acquisition data.

## Known Substitution Risks

- Baubles slots do not exist on modern Fabric, so ring, amulet, and sash behavior will need a documented modern-equipment binding surface.
- RF and CoFH-era energy APIs do not exist in their 1.8.9 form; `LexiconData.class` still references `CoFHAPI|energy`, so Fluxing Node interoperability will require a modern substitute or a contained internal bridge.
- Legacy entities, item IDs, world generation details, and removed vanilla mechanics will need modern equivalents where 1.8.9 assumptions no longer exist.
- The shipped `en_US.lang` itself carried TODO comments for fairy-name and chat-message localization. This port now localizes the recovered fairy role names and the wand and ring status surfaces owned by this parity pass, while leaving unrelated modern-only status copy on direct English strings where the jar never provided localized keys.
- The shipped jar metadata credits contributors but does not itself settle full source-license provenance for every recoverable asset; copied assets or code must carry explicit notices when they enter the repo.

## Licensing And Release Provenance

- `LICENSE` now carries the repo-level MIT notice for this unofficial modern port together with explicit attribution to `pixlepix`, `williewillus`, and the shipped `AuraCascade-592.jar` provenance trail.
- `src/main/resources/fabric.mod.json` now declares `MIT` instead of the earlier placeholder license marker.
- The build now packages `LICENSE` into the jar and sources jar so the release artifact keeps the notice with the shipped mod output.
- The visual parity pass now imports the shipped `AuraCascade-592.jar!/assets/aura/textures/**` payload into `src/main/resources/assets/aura/textures/**`, with lowercase resource-path normalization so the restored Aura surface resolves on modern case-sensitive filesystems.
- The remaining `minecraft:` references on that restored client surface are limited to structural parent and item-definition type ids: `minecraft:model`, `minecraft:block/cube_all`, `minecraft:block/cube_top`, `minecraft:block/cube_bottom_top`, `minecraft:block/orientable`, `minecraft:item/generated`, and `minecraft:item/handheld`.

## Final Parity Audit

Shipped evidence used for this final audit: `mcmod.info`, `assets/aura/lang/en_US.lang`, `pixlepix/auracascade/data/EnumAura.class`, `pixlepix/auracascade/registry/BlockRegistry.class`, `pixlepix/auracascade/data/recipe/ProcessorRecipeRegistry.class`, `pixlepix/auracascade/data/recipe/PylonRecipeRegistry.class`, and `pixlepix/auracascade/lexicon/LexiconData.class` from `AuraCascade-592.jar`.

### Representative Client-Facing Evidence

- Acquisition and placement: `aura_node_black`, `aura_node_capacitor`, `aura_node_conserve`, `aura_node_orange`, `monitor`, `travelers_bricks`, `rebounding_enigma`, `consumer_block_miner`, and `ritual_end` each now ship direct crafting data under `src/main/resources/data/aura/recipe/`, localized block names in `src/main/resources/assets/aura/lang/en_us.json`, blockstates plus item definitions under `src/main/resources/assets/aura/`, and self-drop loot tables under `src/main/resources/data/aura/loot_table/blocks/`. The late-game `consumer_block_loot`, `consumer_block_spawn`, and `ritual_nether` families keep their intentional four-pedestal vortex acquisition under `src/main/resources/data/aura/recipes/vortex/` rather than bench recipes, so the repo no longer presents them as missing survival content.
- Discoverability and conversion: the Aura creative tab now keeps every registered item form reachable, `storage_bookshelf` stays a conversion-only block created by using a Storage Book on a vanilla bookshelf, and `aura_node_pump_creative` remains intentionally creative-only content rather than a missing survival recipe.
- Visual surface: `src/main/resources/assets/aura/textures/**` now carries the shipped Aura block and item texture payload imported from `AuraCascade-592.jar`, the restored block and item models map the current snake_case ids onto that jar-backed art, the repaired animated-texture `.mcmeta` files now parse cleanly on the live client, and `src/test/java/pixlepix/auracascade/parity/VisualAssetParityAuditTest.java` guards the registered Aura surface against missing client resources, malformed texture metadata, or placeholder texture regressions.
- Naming: Aura block items now resolve through block translation keys instead of leaking `item.aura.*`, the closure-slice block ids, the recovered fairy role names, and the ring and prismatic-wand status surfaces now have explicit English localization keys instead of relying on registry ids or direct string fallbacks, and `src/test/java/pixlepix/auracascade/parity/FinalClientParityAuditTest.java` now locks the sampled `monitor` item to `block.aura.monitor`.
- Guidebook truthfulness: `src/main/java/pixlepix/auracascade/lexicon/EncyclopediaAuraContent.java` now tells players that Storage Bookshelves come from using a storage book on a vanilla bookshelf, that the Prismatic Wand skips block entities and fluids and consumes placed block items directly, that an untuned charm binds a plain `Fairy`, that the recovered kaleidoscopic legacy table was `Red = Silk Touch`, `Orange = Efficiency`, `Yellow = Fortune`, `Green = tree-felling`, `Blue = Knockback`, and `Violet = hard-material mining speed`, that all fifteen pairwise kaleidoscopic interactions are documented as a residual contract, and that the Ring of the Shattered Stone still does not restrict blasts to dirt, stone, sand, and gravel.
- User-visible interaction: fresh live-client evidence for this closure slice is archived under `millrace-agents/runs/run-76955964dbb9474f848a0dfb36ae5bd5/live_client_validation/`, including `inventory-sample-set.png`, `held-item-surface.png`, `in-world-block-render.png`, and `latest.log`. That replay shows localized `Monitor` replacement text, sampled Aura inventory, held-item, and in-world renders using Aura art instead of placeholder squares, and no Aura-side `Missing textures in model`, `Unable to parse metadata from aura:`, failed-model, missing-model, or `item.aura.monitor` log hits.

### Implemented Equivalence

- The port now ships a working Fabric 1.21.11 aura simulation with eight-color storage, straight-line node linking, transfer planning, controlled uphill movement, falling-power accounting, persistence, and inspection coverage.
- The node and control surface named by the shipped jar is implemented with real runtime behavior: Aura Node, Aura Capacitor, Conserving Aura Node, black and orange manipulators, Monitor, and Fluxing Node.
- The distinct pump family is implemented with burning, illumination, momentum, projectile, redstone, alternating, and creative variants instead of collapsing the system into one generic power source.
- The shipped consumer and progression families are implemented with aura-powered runtime behavior: processor, prismatic processor, smelter, grower, fisher, brewer, colorer, synthesizer, miner, looter, spawner, kaleidoscopic enchanter, Bookshelf Coordinator, Nether ritual, End ritual, Vortex Controller, and Vortex Pedestal.
- The material, storage, guidebook, accessory, fairy, utility, late-game, and enchantment surfaces named by the jar evidence are present in the repo with localization, models or blockstates, loot or acquisition notes, recipes or explicit acquisition-path documentation, and focused regression coverage.
- `./gradlew build` produces a Fabric mod jar under `build/libs/`, and the release artifact now carries the license notice alongside the mod metadata.

### Documented Substitutions

- The modern port keeps accessories behind an internal inventory-backed bridge instead of a Baubles dependency and keeps Fluxing Node behavior behind a narrow internal energy seam instead of reintroducing the old RF API directly.
- Fairy runtime is reconstructed as ring-backed tagged allays with custom item-data roles instead of the legacy custom fairy entity renderer and metadata item variants. The untuned charm now maps to the shipped basic `Fairy`, and the attuned role names match the recovered jar labels.
- Encyclopedia Aura uses a vanilla written-book surface instead of the legacy custom lexicon GUI and packet flow, and the Bookshelf Coordinator uses direct interaction plus status messages instead of a dedicated GUI.
- The restored acquisition surface keeps the recovered node-upgrade and late-game progression shapes, but the supporting `aura_node`, `aura_node_pump`, `consumer_block_miner`, `fortified_planks`, and four-pedestal vortex formulas use modern equivalent ingredients where legacy metadata items, single-component pylon recipes, or tag-less loaders do not translate directly into the current runtime.
- Rituals preserve destructive late-game block rewrites and danger but do not rewrite biome data in-place on the modern chunk format.
- The Angel's Wing, Angel's Heels, protection-amulet healing, thief-sword villager drop selection, Mirror of the Angel projectile handling, and Prismatic Wand copy-paste behavior are all implemented with modern substitutions where the exact legacy control or metadata model no longer exists.
- Kaleidoscopic enchantments keep the shipped names and the recovered success-curve math. The jar-backed legacy single-color table was `Red = Silk Touch`, `Orange = Efficiency`, `Yellow = Fortune`, `Green = tree-felling`, `Blue = Knockback`, and `Violet = hard-material mining speed`, and the lexicon pages recover the full pairwise matrix: `Yellow + Green` crop harvest, `Blue + Violet` splash damage, `Green + Blue` life steal, `Yellow + Blue` fire, `Red + Violet` damage reduction, `Yellow + Violet` mob-drop multiplication, `Yellow + Red` double-ingot ore drops, `Red + Green` mining slowdown, `Red + Blue` recoil, `Green + Violet` attack reduction, and orange pairings that speed ores, stone, dirt/sand/grass/gravel, wood, and high-durability blocks. The modern runtime still uses the bounded substitution set `Red = ignite`, `Orange = knockback`, `Yellow = mining efficiency`, `Green = poison`, `Blue = bonus damage`, and `Violet = nausea` instead of those recovered live effects.
- Storage-book capacities follow recovered class behavior when the old English lexicon text disagrees with the bytecode.

### Unresolved Gaps

- Ring of the Shattered Stone still does not limit nearby explosion block damage to dirt, stone, sand, and gravel.
- Prismatic Wand copy-paste still skips block entities and fluids and consumes direct placement items rather than the full legacy drop-plus-metadata path.
- The Kaleidoscopic success curve is recovered from the shipped guidebook text, and the recovered residual contract is now exact: legacy single-color behavior was `Red = Silk Touch`, `Orange = Efficiency`, `Yellow = Fortune`, `Green = tree-felling`, `Blue = Knockback`, and `Violet = hard-material mining speed`; pairwise behavior covered `Yellow + Green` crop harvest, `Blue + Violet` splash damage, `Green + Blue` life steal, `Yellow + Blue` fire, `Red + Violet` damage reduction, `Yellow + Violet` mob-drop multiplication, `Yellow + Red` double-ingot ore drops, `Red + Green` mining slowdown, `Red + Blue` recoil, `Green + Violet` attack reduction, and orange mining-speed boosts for ores, stone, dirt/sand/grass/gravel, wood, and high-durability blocks. The current runtime still uses the bounded substitution set `Red = ignite`, `Orange = knockback`, `Yellow = mining efficiency`, `Green = poison`, `Blue = bonus damage`, and `Violet = nausea` instead of those recovered effects.
- The jar's fairy-name and wand-chat TODO comments are now closed on the localized surfaces touched by this pass, but unrelated modern-only status text still uses direct English copy where no shipped localization evidence exists.

## Dedicated Server Validation

- Repo-local dedicated server smoke was exercised with Java 21 through the Loom server entrypoint after setting `run/eula.txt` to `eula=true`.
- The server log for this pass lives at `millrace-agents/runs/run-3389d91e5f0a4f5e802d8023f25c209c/server_smoke.log`.
- The log contains the Aura Cascade initialization lines and reaches `Done (4.828s)! For help, type "help"` on Minecraft `1.21.11`, which satisfies the dedicated-server load requirement for this stage.
- The process was then intentionally interrupted to stop the smoke session, so Gradle reports exit `130` after the successful load. That post-start interrupt is not a runtime blocker.
- First-run `server.properties` creation was logged before the world boot and then resolved automatically during the same startup. The remaining non-blocking warning is Fabric's dev-time untranslated item-tag warning.

## Release Validation

- `JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH=/tmp/millrace-tools/jdk-21/bin:$PATH ./gradlew test --tests pixlepix.auracascade.parity.FinalClientParityAuditTest` passed.
- `JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH=/tmp/millrace-tools/jdk-21/bin:$PATH ./gradlew test --tests pixlepix.auracascade.parity.VisualAssetParityAuditTest --tests pixlepix.auracascade.parity.FinalClientParityAuditTest` passed.
- `JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH=/tmp/millrace-tools/jdk-21/bin:$PATH ./gradlew test` passed.
- `JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH=/tmp/millrace-tools/jdk-21/bin:$PATH ./gradlew build` passed.
- Built release artifact: `build/libs/aura-cascade-0.1.0+1.21.11.jar` (`1273991` bytes, SHA-256 `6b47c8c8ab931e9dbf3b4a56fc472f94b2910e614e24429aca14279fffd3d2a3`).
- Built sources artifact: `build/libs/aura-cascade-0.1.0+1.21.11-sources.jar` (`1103344` bytes, SHA-256 `bb6120cb322ebe2bafeb06b107325278b4b6e35c5f2a381b0cf09283fa087532`).
- `unzip -l build/libs/aura-cascade-0.1.0+1.21.11.jar` confirms that both `LICENSE` and `fabric.mod.json` are packaged into the shipped jar.
