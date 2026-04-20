# Aura Cascade

Aura Cascade is an unofficial modern Fabric port of the shipped `AuraCascade-592.jar` artifact, targeting Minecraft `1.21.11` and Java `21`. This repository treats the shipped jar as the primary parity reference and keeps the release-facing parity audit, substitutions, unresolved gaps, and validation evidence in `PORTING_NOTES.md`.

## Attribution And Provenance

- Original mod identity from `AuraCascade-592.jar!/mcmod.info`: mod id `Aura`, display name `AuraCascade`, version `592`.
- Original author: `pixlepix`.
- Credited 1.8.x porter in the shipped jar metadata: `williewillus`.
- This repository is not an official continuation and should be presented as an unofficial modern port.
- The current parity baseline is grounded in the shipped jar's `mcmod.info`, `assets/aura/lang/en_US.lang`, `pixlepix/auracascade/data/EnumAura.class`, `pixlepix/auracascade/registry/BlockRegistry.class`, `pixlepix/auracascade/data/recipe/ProcessorRecipeRegistry.class`, `pixlepix/auracascade/data/recipe/PylonRecipeRegistry.class`, and `pixlepix/auracascade/lexicon/LexiconData.class`.

## Release Status

- The workspace builds a Fabric mod jar under `build/libs/`.
- Regression coverage exercises the aura kernel, color rules, node/pump/control behavior, consumer throughput, vortex crafting, storage books, bookshelf networking, Encyclopedia Aura content, fairy logic, accessory state, late-game systems, and recipe/resource coverage.
- The final parity audit explicitly separates implemented equivalence, documented substitutions, and unresolved gaps in `PORTING_NOTES.md`.
- Dedicated-server validation evidence and the fresh live-client replay evidence are recorded in `PORTING_NOTES.md`.

## Final Client Audit Snapshot

### Implemented Parity

- The non-creative node, pump, monitor, travel, rebound, miner, fortified, and ritual block families now have explicit survival acquisition paths; direct crafting covers the node/control, pump, travel, rebound, miner, and End-ritual ids, while fortified, looter, spawner, and Nether-ritual content use the documented processor or four-pedestal vortex seams.
- Aura creative-tab discoverability now matches the shipped evidence: every registered Aura item form remains reachable in the creative tab, `storage_bookshelf` stays a conversion-only block created by using a Storage Book on a vanilla bookshelf, and `aura_node_pump_creative` remains an intentionally creative-only item-form block instead of a missing survival recipe.
- The restored closure-slice blocks ship localized names, item definitions, blockstates, models, loot tables, and guidebook copy that match the current port state instead of relying on registry ids or stale parity notes.
- The current Aura client surface pairs the original `177` jar-derived texture and support files with `154` deterministic singular mirrors under `src/main/resources/assets/aura/textures/{item,block}/`, keeping the current block and item models plus the modern item-definition surface on `aura:item/...` and `aura:block/...` lookups while the remaining `minecraft:` references stay limited to the documented structural parent and item-definition type ids.
- `PORTING_NOTES.md` now records a fresh archived live-client replay where the sampled inventory, held-item, and in-world Aura surfaces render with Aura art, the sampled `Monitor` item resolves through the localized block name, and the replay log stays free of Aura-side missing-texture, failed-model, missing-model, and texture-metadata-parse warnings.

### Intentional Modern Substitutions

- Accessories stay behind the internal inventory-backed bridge, fairies remain ring-backed allays, Encyclopedia Aura remains a written book, and the restored late-game acquisition formulas keep the documented modern ingredient and vortex substitutions where 1.8.9 metadata-era inputs do not translate directly.
- The Prismatic Wand, rituals, and kaleidoscopic enchantments keep the recovered player-facing roles but still use the narrower modern interaction set described in `PORTING_NOTES.md`.

### Remaining Unresolved Gaps

- Ring of the Shattered Stone still lacks the legacy loose-block explosion filter.
- Prismatic Wand copy and paste still skips block entities and fluids and consumes direct placement items rather than the full legacy drop-plus-metadata path.
- The recovered kaleidoscopic single-color table and fifteen pairwise interactions are now documented exactly, but the live runtime still uses the bounded modern substitution set instead of those recovered legacy effects.
- `PORTING_NOTES.md` carries the representative client-facing evidence, including the archived live replay that closed the sampled `Monitor` naming leak, together with the exact residual wording used by the guidebook.

## Build And Test

1. Use Java `21`.
2. Run `./gradlew test`.
3. Run `./gradlew build`.
4. Collect the mod jar from `build/libs/aura-cascade-<version>.jar`.

## Dedicated Server Smoke

1. Use Java `21`.
2. For a repo-local dedicated server smoke, run `./gradlew runServer` once and wait for the server bootstrap to finish.
3. For a standalone Fabric server, copy `build/libs/aura-cascade-<version>.jar` into the server `mods/` directory and start the server with `nogui`.
4. Confirm the log contains `Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.` and no Fabric dependency failures.
5. See `PORTING_NOTES.md` for the latest verified smoke result and any exact blocker that prevented a standalone smoke.

## License Guidance

- `LICENSE` carries the MIT notice used for this unofficial port workspace together with Aura Cascade attribution and provenance notes.
- `src/main/resources/fabric.mod.json` declares the shipped mod metadata as MIT-licensed for the current release artifact.
- The current repo includes visual asset imports derived from `AuraCascade-592.jar!/assets/aura/textures/**`; `PORTING_NOTES.md` records the restored mapping, lowercase path normalization, and the remaining structural `minecraft:` references on that client surface.
- Any future direct import of upstream code or assets must retain the original notice in addition to this repo-level license file.

## Current Implemented Scope

- Fabric 1.21.11 scaffold with Gradle, wrapper generation target, Java 21 compilation, Fabric metadata, and test roots.
- Authoritative aura kernel under `src/main/java/pixlepix/auracascade/aura/` with per-color storage, straight-line node linking, natural transfer planning, controlled upward movement, falling-power accounting, per-node persistence, and comparator-friendly inspection state.
- Registered node, manipulator, capacitor, conserving, flux, monitor, pump, consumer, vortex, ritual, utility, and fortified containment content under `src/main/java/pixlepix/auracascade/block/` and `src/main/java/pixlepix/auracascade/block/entity/`, with runtime behavior tied to the shared aura kernel instead of placeholder stubs.
- Core consumer families keep distinct gameplay roles for processing, prismatic processing, smelting, growing, fishing, brewing, coloring, synthesizing, and kaleidoscopic enchanting, spending falling-aura power through nonlinear progress costs instead of generic timer ticks.
- Vortex crafting is implemented with a controller, aura-bearing pedestals, unordered recipe matching, aura gating, progress signaling, and file-backed recipes for the kaleidoscopic enchanter, prismatic processor, and fluxing node.
- Pump families keep distinct trigger semantics for burning fuel, illumination sources, falling momentum, projectiles, redstone wire, alternating output, and creative auto-feed.
- The compatibility package carries both the external-energy seam and an inventory-backed accessory seam, keeping flux export and ring/amulet/sash binding isolated from the internal aura simulation and from any future third-party slot API.
- Client resources cover the registered node, pump, consumer, vortex, monitor, ritual, utility, and fortified block ids with blockstates, block models, legacy item models, and modern item definitions backed by restored jar-derived Aura textures, with only documented structural `minecraft:` parents and item-definition type ids remaining on the touched client surface.
- Progression materials and recipe loading live under `src/main/java/pixlepix/auracascade/item/` and `src/main/java/pixlepix/auracascade/data/recipe/`, with localization, item definitions, models, vanilla crafting recipes, processor and synthesizer world recipes, vortex recipes, and item tags for aura crystals, arcane ingots, arcane gems, the Arcane Prism, twelve Angel's Steel ingot tiers, and degree-scaled tools and swords.
- Accessory and utility gear runtime covers Ring of Binding storage and sync, Fairy Charm attunement, six protection amulets, the Angel's Wing and Angel's Heels wearables, the forbidden-fruit amulet, Mirror of the Angel, Portable Red Hole, Portable Black Hole, Prismatic Wand stateful copy and paste behavior, Transmuting Sword mappings, Sword of the Thief trade-result drops, and Sword of the Barbarian combo tracking.
- Storage books live under `src/main/java/pixlepix/auracascade/item/books/`, preserving the recovered capacity and specialty-filter tradeoffs with NBT-backed large-count persistence instead of oversized stack counts.
- The bookshelf network includes `storage_bookshelf` and `bookshelf_coordinator` under `src/main/java/pixlepix/auracascade/block/` and `src/main/java/pixlepix/auracascade/block/entity/`, with vanilla-bookshelf conversion, connected-shelf scans, line-of-sight gating, adjacent-aura power checks, and the recovered coordinator power curve.
- Encyclopedia Aura lives across `src/main/java/pixlepix/auracascade/item/` and `src/main/java/pixlepix/auracascade/lexicon/`, shipping onboarding plus implemented-system entries as a dynamic vanilla written book instead of the legacy custom GUI and packet flow.
- Fairy runtime lives under `src/main/java/pixlepix/auracascade/fairy/`, reconstructing the recovered role families as server-driven tagged allays with equipped-ring synchronization, in-world orbiting helpers, and persisted owner, slot, and role data.
- The late-game world slice gives concrete runtime behavior to the looter, spawner, miner, Nether ritual, End ritual, Traveler's Bricks, Rebounding Enigma, and fortified containment families.
- Kaleidoscopic enchantment keys live under `src/main/java/pixlepix/auracascade/enchantment/`, with six data-driven enchantments and a dedicated enchantable-item tag under `src/main/resources/data/aura/enchantment/` and `src/main/resources/data/aura/tags/item/`.
- Localization covers the registered block families plus the progression, storage, guidebook, accessory, fairy, and utility item set under `src/main/resources/assets/aura/lang/en_us.json`, and self-drop loot tables cover the same node, pump, consumer, vortex, monitor, ritual, storage, utility, and fortified block families under `src/main/resources/data/aura/loot_table/blocks/`.
- `AuraColor` preserves the eight-color parity surface with mechanically meaningful rule encoding for mass, ascent, passive ticks, flow restrictions, falling-power eligibility, and orange induced-current behavior.

## Compatibility Direction

- Accessories: player-facing ring, amulet, and sash behavior routes through an internal inventory-backed bridge first; keep any later third-party slot integration behind that seam.
- External energy: preserve aura as the canonical simulation, and keep any later cross-mod energy interoperability behind the existing narrow bridge so Fluxing Node parity does not collapse into generic FE behavior.
