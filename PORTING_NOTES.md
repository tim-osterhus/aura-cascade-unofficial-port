# Porting Notes

## 1.21.11 Forward Port

Branch `fabric/1.21.11` derives from canonical source checkpoint `44cc057`.
Implementation and acceptance are in progress; no target release is approved yet.
See [the target audit](docs/audits/2026-09-25-port-12111.md) for dependency proof,
memory measurements and target-only changes. All results below belong to the
historical 1.21.1 baseline unless explicitly labeled otherwise.

## 0.2.1 Visual Hotfix

Aura Cascade Reimagined retains the `aura` mod and registry IDs. Final artifact
`0.2.1+1.21.1` SHA-256
`2290a1a344fa8e1e43d631729b5f88d422923d573db90d1a592945fecc1aacc8`
passes 252 tests in 62 suites and packaged intermediary-client smoke
(`20260923-182030-717`).
Actual-font audit v3 includes the opening page as well as 41 entries/371 pages:
all pass, with opening text bottom 151 below the footer limit 155.
Independent visual acceptance and publication are tracked in
[the hotfix QA record](docs/audits/2026-09-23-hotfix-visual-qa.md) and
[release notes](docs/releases/0.2.1-beta.md). The bounded five-target visual gate
has passed; see [the publication record](docs/releases/0.2.1-publication.md)
for CurseForge upload and availability status.

## September 2026 Reopened Audit

### Current Verified Checkpoint

The final sixteenth integration passes 244 tests in 60 suites and adds the
non-fiery per-item Red Hole tick correction to the verified explosion-list,
mining-hook and bookshelf line-of-sight fixes.
Its candidate hash is
`cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`.
Actual equipped/distant/unequipped Shattered Stone controls pass. Final packaged
run `20260923-121601-864` passes all 41 entries/371 pages/14 quests in font audit
v2 and 40 stable rendered frames. Final multiplayer run `20260923-121724-053`
passes all 15 checks and all three transformed-item lifetime checks, with all
four JVM lifetimes exiting normally. Both runs verify this exact candidate hash.
The preceding thirteenth packaged candidate,
SHA-256 `306ab942b05029d814607a155e3a3796eaed3384b85af0cf92423a74c5ac63d4`,
passes real intermediary client/world rendering and actual-font layout for all
41 Encyclopedia entries, 371 pages and 14 quest pages. All 261 legacy guide
keys are mapped; full-book manual interaction is not claimed. Prior bounded
quest reward/persistence and guide-navigation checks remain recorded evidence.

Two packaged clients passed private owner-only equipment synchronization, real
charm binding, logout cleanup and reconnect restoration. The thirteenth live
session also passed 68 restored recipe/catalyst cases, real consumer item
preservation, full White ingot/Prism/Angelsteel cycles without injected power or
progress, and earned charge-21 Miner yield. Fixtures supplied raw materials.
See [integration](docs/audits/2026-09-23-integrated-restoration.md),
[gameplay](docs/audits/2026-09-23-thirteenth-gameplay.md) and
[multiplayer](docs/audits/2026-09-23-packaged-multiplayer-playtest.md).
Subsequent runtime checks pass combat, Mirror, Orange/capacitor controls,
bookshelf conversion/obstruction/disconnection, storage contents/power reload,
an exact active ritual queue reload and continued completion, same-target ritual
rejection, and Yellow vortex receipt persistence followed by actual output.
Portable Red Hole two-blast, marker-continuity, age and frozen-time controls now
pass, as do actual held/dropped White crystal accounting. Independent review
identified no remaining finite B1-B5 gameplay blocker. Publication status is
recorded separately in [the release record](docs/releases/0.2.0-publication.md).
These current results supersede older
pending/closure statements below, which describe historical checkpoints.

### Recovered Contracts

Prismatic Wand source integration now supersedes all older snapshot-clipboard,
stored-content and bucket-material claims below. The 592 wand saves a live source
region and player-relative offset, reads source blocks at paste time and places
only into air. The restored implementation uses those semantics with a 512-cell
geometric safety cap and loaded-chunk/world-bound checks. It charges survival
materials only after successful placement and does not duplicate source block-
entity data or offer undo. Live region, placement and persistence controls remain
required; the old snapshot tests are not acceptance for this implementation.

The new [E1 contract](docs/audits/2026-09-23-kaleidoscopic-e1-contract.md)
supersedes the older enchantment descriptions below. Restored source uses the
original geometric pair strengths, connected growable harvesting, probabilistic
two-ingot conversion, damage reduction before armor after vanilla hit acceptance,
and destroy speed >= 3.0. The
older crop-square, post-hit healing and permanent vanilla-enchantment behavior
is historical, not a current acceptance target. Integrated validation is pending.

Fourteen original quests now have server-awarded advancement progress and reward
definitions. Opening the Encyclopedia checks carried items without consuming
them. Reward quantities and metadata mappings are resource-tested; actual
completion, persistence and Patchouli progress still need client acceptance.

September 23 source integration supersedes the older Shattered Stone descriptions
below: the ring makes nearby blasts spare non-terrain blocks and does not prevent
wearer damage. See the [recovered contract](docs/audits/2026-09-23-shattered-stone-contract.md).
Vortex colored-power accounting, actual gem/prism acquisition, consumer feedback,
storage browsing and physical accessory slots are under integration. Their older
substitution descriptions are historical findings, not approved parity targets.

Earlier closure statements in this document record source/test audits, not
verified full gameplay parity. The live baseline found a non-opening guide,
clipped written-book content, and node presentation defects. Current authority
is the [acceptance ledger](docs/audits/2026-09-22-video-acceptance.md) and
[staggered execution record](docs/audits/2026-09-22-staggered-execution.md).

The active guide pilot uses required Patchouli `1.21.1-93-FABRIC`, retaining
`aura:encyclopedia_aura`. Build and client acceptance are recorded separately;
full legacy guide coverage is not claimed. White crystals intentionally use
eight gold nuggets around an amethyst shard for two crystals. Encyclopedia
acquisition restores the original shaped white-crystal-plus-book recipe without
the port's extra Arcane Prism. Neither recipe's history belongs in player-facing
guide text.

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
- Ring of the Shattered Stone: the pass preserves the explosion-channeling wearer protection role while filtering nearby dirt, stone, sand, and gravel out of explosion block damage inside the wearer's three-block protection radius.
- Mirror of the Angel: right-click or direct hits now deflect nearby fireballs and wither skulls toward the nearest ghast when possible, otherwise along the player's look vector.
- Portable Red Hole and Portable Black Hole: both now persist while dropped via unlimited item lifetime; the red hole explodes every five seconds while dropped, and the black hole continuously deletes cobblestone from player inventories while carried.
- Prismatic Wand: selection, copy, and paste modes are implemented. The clipboard now captures non-air block states, source fluids, and representative block-entity data, then pastes by consuming merged block, stored-item, and bucket materials while replaying copied block-entity data after placement.
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

- Encyclopedia Aura: the earlier vanilla written-book substitution failed live usability checks. A Patchouli integration now replaces that surface in the active pilot, preserving the registered item identity. Complete legacy content and client acceptance remain separate open gates.
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

- Acquisition and placement: `aura_node_black`, `aura_node_capacitor`, `aura_node_conserve`, `aura_node_orange`, `monitor`, `travelers_bricks`, `rebounding_enigma`, `consumer_block_miner`, `ritual_end`, and `bookshelf_coordinator` each now ship direct crafting data under `src/main/resources/data/aura/recipe/`, localized block names in `src/main/resources/assets/aura/lang/en_us.json`, blockstates plus item definitions under `src/main/resources/assets/aura/`, and self-drop loot tables under `src/main/resources/data/aura/loot_table/blocks/`. The fortified containment family keeps processor-backed acquisition under `src/main/resources/data/aura/recipes/processor/` instead of bench recipes, and the late-game `consumer_block_loot`, `consumer_block_spawn`, and `ritual_nether` families keep their intentional four-pedestal vortex acquisition under `src/main/resources/data/aura/recipes/vortex/` rather than bench recipes, so the repo no longer presents them as missing survival content or silent direct-crafting parity.
- Discoverability and conversion: the Aura creative tab now keeps every registered item form reachable, `storage_bookshelf` stays a conversion-only block created by using a Storage Book on a vanilla bookshelf, and `aura_node_pump_creative` remains intentionally creative-only content rather than a missing survival recipe.
- Visual surface: `src/main/resources/assets/aura/textures/**` now carries the shipped Aura block and item texture payload imported from `AuraCascade-592.jar`, the restored block and item models map the current snake_case ids onto that jar-backed art, the repaired animated-texture `.mcmeta` files now parse cleanly on the live client, and `src/test/java/pixlepix/auracascade/parity/VisualAssetParityAuditTest.java` guards the registered Aura surface against missing client resources, malformed texture metadata, or placeholder texture regressions.
- Naming: Aura block items now resolve through block translation keys instead of leaking `item.aura.*`, the closure-slice block ids, the recovered fairy role names, and the ring and prismatic-wand status surfaces now have explicit English localization keys instead of relying on registry ids or direct string fallbacks, and `src/test/java/pixlepix/auracascade/parity/FinalClientParityAuditTest.java` now locks the sampled `monitor` item to `block.aura.monitor`.
- Guidebook truthfulness: the summaries migrated to `src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/entries/` tell players that Storage Bookshelves come from using a storage book on a vanilla bookshelf, that the Prismatic Wand captures non-air block states, source fluids, and supported block-entity data while consuming merged block, stored-item, and bucket materials, that an untuned charm binds a plain `Fairy`, that the Kaleidoscopic Enchanter expresses the recovered single-color table through `Red = temporary Silk Touch mining override`, `Orange = Efficiency sync`, `Yellow = temporary Fortune mining override`, `Green = connected-log tree-felling`, `Blue = Knockback sync`, and `Violet = hard-material haste window`, that the current pairwise runtime includes `Yellow + Green` harvests mature crops in a 3x3 footprint, and that the Ring of the Shattered Stone filters nearby dirt, stone, sand, and gravel out of explosion block damage inside its protection radius. These describe current implementation, not proof of original-mod equivalence; the Ring behavior is specifically disputed in the video acceptance ledger.
- User-visible interaction truth surface: this workspace does not retain the earlier `live_client_validation/` replay archive, so the release-facing client evidence is the repo-owned asset and audit surface. `src/test/java/pixlepix/auracascade/parity/FinalClientParityAuditTest.java` locks the localized `Monitor` item naming surface and release-facing closure wording, `src/test/java/pixlepix/auracascade/parity/VisualAssetParityAuditTest.java` guards Aura textures, models, and animated-texture metadata, and the shipped `src/main/resources/assets/aura/**` files plus guidebook content keep the documented client-facing surface grounded in files that exist in this workspace.

### Implemented Equivalence

- The port now ships a working Fabric 1.21.1 aura simulation with eight-color storage, straight-line node linking, transfer planning, controlled uphill movement, falling-power accounting, persistence, and inspection coverage.
- The node and control surface named by the shipped jar is implemented with real runtime behavior: Aura Node, Aura Capacitor, Conserving Aura Node, black and orange manipulators, Monitor, and Fluxing Node.
- The distinct pump family is implemented with burning, illumination, momentum, projectile, redstone, alternating, and creative variants instead of collapsing the system into one generic power source.
- The shipped consumer and progression families are implemented with aura-powered runtime behavior: processor, prismatic processor, smelter, grower, fisher, brewer, colorer, synthesizer, miner, looter, spawner, kaleidoscopic enchanter, Bookshelf Coordinator, Nether ritual, End ritual, Vortex Controller, and Vortex Pedestal.
- The material, storage, guidebook, accessory, fairy, utility, late-game, and enchantment surfaces named by the jar evidence are present in the repo with localization, models or blockstates, loot or acquisition notes, recipes or explicit acquisition-path documentation, and focused regression coverage.
- `./gradlew build` produces a Fabric mod jar under `build/libs/`, and the release artifact now carries the license notice alongside the mod metadata.

### Documented Substitutions

- The modern port keeps accessories behind an internal inventory-backed bridge instead of a Baubles dependency and keeps Fluxing Node behavior behind a narrow internal energy seam instead of reintroducing the old RF API directly.
- Fairy runtime is reconstructed as ring-backed tagged allays with custom item-data roles instead of the legacy custom fairy entity renderer and metadata item variants. The untuned charm now maps to the shipped basic `Fairy`, and the attuned role names match the recovered jar labels.
- Encyclopedia Aura is being migrated to Patchouli after the written-book baseline failed; do not treat the old surface as accepted parity. The Bookshelf Coordinator still uses direct interaction plus status messages instead of the demonstrated contents browser and remains an open usability gap.
- The restored acquisition surface keeps the recovered node-upgrade and late-game progression shapes, but the supporting `aura_node`, `aura_node_pump`, `consumer_block_miner`, `fortified_planks`, and four-pedestal vortex formulas use modern equivalent ingredients where legacy metadata items, single-component pylon recipes, or tag-less loaders do not translate directly into the current runtime.
- Rituals preserve destructive late-game block rewrites and danger but do not rewrite biome data in-place on the modern chunk format.
- The Angel's Wing, Angel's Heels, protection-amulet healing, thief-sword villager drop selection, Mirror of the Angel projectile handling, and the Prismatic Wand's structured clipboard state model are all implemented with modern substitutions where the exact legacy control or metadata model no longer exists.
- Kaleidoscopic enchantments keep the shipped names and recovered success-curve math, and the live runtime now maps the recovered single-color table to `Red = temporary Silk Touch mining override`, `Orange = Efficiency sync`, `Yellow = temporary Fortune mining override`, `Green = connected-log tree-felling`, `Blue = Knockback sync`, and `Violet = hard-material haste window`. The pairwise matrix is now expressed through modern runtime seams: `Yellow + Green` harvests mature crops in a 3x3 footprint, `Blue + Violet` deals splash damage around the struck target, `Green + Blue` heals the attacker after direct hits, `Yellow + Blue` adds fire aspect to direct melee hits, `Red + Violet` heals back part of nonfatal incoming damage while held, `Yellow + Violet` duplicates fresh mob drops near the kill, `Yellow + Red` smelts iron, gold, and copper ores and adds one extra ingot, `Red + Green` applies mining fatigue while the block is being worked, `Red + Blue` pushes the attacker backward after direct hits, `Green + Violet` applies temporary Weakness, and the orange pairings add targeted mining haste.
- Storage-book capacities follow recovered class behavior when the old English lexicon text disagrees with the bytecode.

### Shipped Runtime Bounds

- Ring of the Shattered Stone now filters nearby dirt, stone, sand, and gravel out of explosion block damage inside the wearer's three-block protection radius while keeping the wearer explosion-damage gate intact.
- Prismatic Wand copy-paste now captures non-air block states, source fluids, and representative block-entity data, replays block-entity custom data after placement, and consumes merged block, stored-item, and bucket materials instead of only direct placement items.
- Kaleidoscopic runtime bounds stay explicit: `Yellow + Green` is limited to mature `CropBlock`, nether wart, and sweet berry bushes in a 3x3 footprint; `Yellow + Red` is limited to iron, gold, and copper ore families expressed as smelted drops plus one extra ingot; `Red + Violet` heals back part of nonfatal damage while the kaleidoscopic item is held in the main hand; and violet hard-material boosts use `destroy speed >= 8.0` as the supported threshold.
- The jar's fairy-name and wand-chat TODO comments are now closed on the localized surfaces touched by this pass, but unrelated modern-only status text still uses direct English copy where no shipped localization evidence exists.

## Dedicated Server Validation

- `./gradlew --console=plain runServer` was re-run for the repo-local `1.21.1` smoke using Java `21`.
- `run/logs/latest.log` reached `Loading Minecraft 1.21.1`, `Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.`, `Starting minecraft server version 1.21.1`, and `Done (...)! For help, type "help"`.
- The same log no longer reports Aura `Parsing error loading recipe` entries for the repaired `src/main/resources/data/aura/recipe/` tree; the remaining non-fatal startup noise in this pass is the known `aura:kaleidoscopic_enchantable` missing-tag warning plus Fabric's dev-time untranslated item-tag warning.
- `run/logs/latest.log` keeps the full repo-local smoke output for this pass, including any non-fatal startup warnings.
- After those startup markers appeared, the foreground dev server was interrupted intentionally with `SIGINT` so the local smoke did not remain running.
- Because `runServer` stays attached to that foreground JVM, the repo-local command exits `130` after the intentional interrupt.
- This repo-local smoke is the current dedicated-server validation evidence for the `0.1.1+1.21.1` release surface.

## Release Validation

Historical artifact records below belong to the earlier audit, not the current
development build. Their hashes are retained as provenance; new checkpoint
artifacts and validation are recorded in `docs/audits/2026-09-22-staggered-execution.md`.
The documentation unit test checks record structure, not current artifact bytes.

- `env JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH='/tmp/millrace-tools/jdk-21/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' ./gradlew --console=plain test --tests pixlepix.auracascade.parity.ReleaseDocumentationAuditTest` passed.
- `env JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH='/tmp/millrace-tools/jdk-21/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' ./gradlew --console=plain test --tests pixlepix.auracascade.parity.FinalClientParityAuditTest` passed.
- `env JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH='/tmp/millrace-tools/jdk-21/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' ./gradlew --console=plain test` passed.
- `env JAVA_HOME=/tmp/millrace-tools/jdk-21 PATH='/tmp/millrace-tools/jdk-21/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' ./gradlew --console=plain build` passed.
- Built release artifact: `build/libs/aura-cascade-0.1.1+1.21.1.jar` (`1325404` bytes, SHA-256 `edbe73d8878375f0f0129670473b46067605e9e1156252ad6c4c52f279e221ed`).
- Built sources artifact: `build/libs/aura-cascade-0.1.1+1.21.1-sources.jar` (`1117474` bytes, SHA-256 `6ec98cd450f91f5892f161c260f8166027fe13b34b72e5e86cc1182cea357abe`).
- `unzip -l build/libs/aura-cascade-0.1.1+1.21.1.jar` confirms that both `LICENSE` and `fabric.mod.json` are packaged into the shipped jar.
