# Aura Cascade Reimagined

Aura Cascade is an unofficial modern Fabric port of the shipped `AuraCascade-592.jar` artifact. This branch targets Minecraft `1.21.11` and Java `21`; `main` remains the canonical `1.21.1` implementation. This repository treats the shipped jar as the primary parity reference and keeps the release-facing parity audit, substitutions, shipped runtime bounds, and validation evidence in `PORTING_NOTES.md`.

## Attribution And Provenance

- Original mod identity from `AuraCascade-592.jar!/mcmod.info`: mod id `Aura`, display name `AuraCascade`, version `592`.
- Original author: `pixlepix`.
- Credited 1.8.x porter in the shipped jar metadata: `williewillus`.
- Original textures: Futureazoo, Drullkus and pixlepix, as credited on the
  [original project page](https://www.curseforge.com/minecraft/mc-mods/aura-cascade).
- The original MIT copyright notice for Adly Templeton is retained in `LICENSE`.
- This repository is not an official continuation and should be presented as an unofficial modern port.
- The current parity baseline is grounded in the shipped jar's `mcmod.info`, `assets/aura/lang/en_US.lang`, `pixlepix/auracascade/data/EnumAura.class`, `pixlepix/auracascade/registry/BlockRegistry.class`, `pixlepix/auracascade/data/recipe/ProcessorRecipeRegistry.class`, `pixlepix/auracascade/data/recipe/PylonRecipeRegistry.class`, and `pixlepix/auracascade/lexicon/LexiconData.class`.

## Release Status

The `fabric/1.21.11` branch forward-ports the reviewed `0.2.1+1.21.1`
checkpoint `44cc057`, with target-only rendering corrections in `d898c99`.
The release candidate passes 258 tests in 65 suites, packaged client/server,
multiplayer and bounded visual/gameplay checks. Publication status and exact
coverage are recorded in [the target audit](docs/audits/2026-09-25-port-12111.md)
and [target beta notes](docs/releases/0.2.1-1.21.11-beta.md).
CurseForge accepted target beta file `8995986`; public availability is not yet
confirmed. See the [publication record](docs/releases/0.2.1-1.21.11-publication.md).
Requires Java 21, Fabric Loader 0.19.5+, Fabric API and Patchouli: Fabric Edition
1.21.11-94.4-FABRIC. Use a new world or a backed-up trial; old 0.1.x world
compatibility is unverified. The inherited Breeder Fairy juvenile-eligibility
issue remains open. This beta is not an exhaustive Survival or modpack audit.
The results below are historical 1.21.1 evidence, not target-version passes.

Version `0.2.1+1.21.1` is the visual-feedback hotfix for
[Aura Cascade Reimagined](https://www.curseforge.com/minecraft/mc-mods/aura-cascade-reimagined).
It retains the `aura` mod ID and existing world/item IDs. Hotfix validation and
publication are tracked separately from the 0.2.0 baseline below. All five
bounded hotfix checks pass, along with 252 regression tests and the final
packaged-client smoke. CurseForge accepted beta file `8961227`; public
availability is not yet confirmed. See the
[0.2.1 publication record](docs/releases/0.2.1-publication.md).

The September 2026 gameplay audit reopens the earlier parity-closure claims below.
Asset/resource coverage and passing unit tests are not proof of a usable port.
See [the acceptance ledger](docs/audits/2026-09-22-video-acceptance.md) and
[active execution record](docs/audits/2026-09-22-staggered-execution.md) for current
status. The final sixteenth integration passes 244 tests across 60 suites. Its
packaged client loads and renders correctly, and actual-font validation passes all 41
Encyclopedia entries and 371 pages without visible text or heading overflow.
Legacy guide mapping covers all 261 page keys; this is not a claim of manually
clicking every page. Storage search/retrieval and bounded accessory/fairy checks
have live evidence. Two packaged clients pass owner-only accessory state,
binding, logout cleanup and reconnect restoration.

The [progression gameplay session](docs/audits/2026-09-23-thirteenth-gameplay.md)
passes 68 corrected recipe/catalyst cases, consumer item preservation, full
White ingot/Prism/Angelsteel cycles using actual generated power, and earned
Miner ore yield. Raw materials were supplied by fixtures, not naturally acquired.
The independent follow-up passes Fruit use/cancel, Extinguisher, fairy capacity
and release, and Wand placement/material/protection controls. It found Digger
mining-speed suppression; the fourteenth build's chainable mining hooks now
pass actual Digger/enchantment composition controls. Corrected bookshelf
obstruction checks now pass actual conversion, blocked/bent/disconnected network
controls and saved contents/power. Later checks pass combat, Mirror, Orange
transfer, capacitor controls, active ritual queue reload and completion, saved
vortex receipts and actual Yellow Looter production. Shattered Stone now passes
equipped, distant and physically unequipped explosion controls after fixing a
vanilla explosion-list alias bug.
Portable Red Hole repeated-operation and frozen-time controls, direct/dropped
crystal accounting, final packaged-client rendering/font checks and all 15
multiplayer checks now pass on `0.2.0+1.21.1`. These are bounded beta acceptance
results, not exhaustive modpack compatibility or every possible survival path. See
[the completion plan](docs/audits/2026-09-23-completion-plan.md).
The [full-port beta gate](docs/audits/2026-09-22-beta-release-gate.md) has passed.
CurseForge accepted beta file `8959308`; public availability/approval is not yet
confirmed. See the [publication record](docs/releases/0.2.0-publication.md) for
the exact artifact, release result and beta coverage limits.

- The workspace builds a Fabric mod jar under `build/libs/`.
- Regression coverage exercises the aura kernel, color rules, node/pump/control behavior, consumer throughput, vortex crafting, storage books, bookshelf networking, Encyclopedia Aura content, fairy logic, accessory state, late-game systems, and recipe/resource coverage.
- The final parity audit explicitly separates implemented equivalence, documented substitutions, and shipped runtime bounds in `PORTING_NOTES.md`.
- Dedicated-server validation evidence, release-facing parity audits, and passing `./gradlew --console=plain test` plus `./gradlew --console=plain build` results are recorded in `PORTING_NOTES.md`.

## Final Client Audit Snapshot

### Implemented Parity

- The non-creative node, pump, monitor, travel, rebound, miner, Bookshelf Coordinator, fortified, and ritual block families now have explicit survival acquisition paths; direct crafting covers the node/control, pump, travel, rebound, miner, Bookshelf Coordinator, and End-ritual ids, while fortified, looter, spawner, and Nether-ritual content use the documented processor or four-pedestal vortex seams.
- Aura creative-tab discoverability now matches the shipped evidence: every registered Aura item form remains reachable in the creative tab, `storage_bookshelf` stays a conversion-only block created by using a Storage Book on a vanilla bookshelf, and `aura_node_pump_creative` remains an intentionally creative-only item-form block instead of a missing survival recipe.
- The restored closure-slice blocks ship localized names, item definitions, blockstates, models, loot tables, and guidebook copy that match the current port state instead of relying on registry ids or stale parity notes.
- The current Aura client surface pairs the original `177` jar-derived texture and support files with `154` deterministic singular mirrors under `src/main/resources/assets/aura/textures/{item,block}/`, keeping the current block and item models plus the modern item-definition surface on `aura:item/...` and `aura:block/...` lookups while the remaining `minecraft:` references stay limited to the documented structural parent and item-definition type ids.
- Ring of the Shattered Stone now follows the recovered source contract: a wearer within three blocks of an explosion on each axis causes that blast to spare non-terrain blocks; the ring does not prevent wearer damage. Equipped, distant and physically unequipped live controls pass.
- Prismatic Wand copy records a live source region and its offset from the player. Paste reads that region in its current state, targets only air, and charges survival materials only after successful placement. A 512-cell geometric limit and loaded-chunk/world-bound checks bound the work. It does not copy stored block-entity contents or offer undo.
- The Kaleidoscopic Enchanter now maps the recovered single-color table and pairwise matrix through modern runtime seams instead of the old ignite/knockback/mining-efficiency/poison/damage/nausea substitute set.
- `PORTING_NOTES.md` now records the current repo-owned client-facing evidence: `FinalClientParityAuditTest` locks the localized `Monitor` item naming surface, `VisualAssetParityAuditTest` guards Aura textures, models, and animated-texture metadata, and the shipped Aura assets plus guidebook content keep the release-facing client surface grounded in files that exist in this workspace.

### Intentional Modern Substitutions

- The approved crystal recipe uses amethyst and eight gold nuggets. The Encyclopedia uses navigable Patchouli entries. Accessories now have four physical server-owned slots. A custom sprite fairy entity replaces the earlier Allay stand-in; bounded role, capacity, lifecycle and multiplayer checks are recorded in the acceptance ledger. Every role permutation has not been manually playtested.
- Modern compatibility adaptations are recorded in the [enchantment contract](docs/audits/2026-09-23-kaleidoscopic-e1-contract.md): entity loot tables replace removed Forge drop hooks, and common material tags replace the ore dictionary. These adaptations still require integrated gameplay validation.

### Shipped Runtime Bounds

- `PORTING_NOTES.md` carries the representative client-facing evidence that still exists in this workspace, together with the exact Ring, Wand, and kaleidoscopic closure wording used by the guidebook and parity audits.
- The restored enchantment source uses connected growable blocks, probabilistic two-ingot conversion, incoming damage reduction before hurt, and the original hard-block threshold of destroy speed >= 3.0. This supersedes the previous crop-square, post-hit healing, potion-effect and permanent vanilla-enchantment substitutes. Compilation and live acceptance are recorded separately in the current audit documents.

## Build And Test

1. Use Java `21`.
2. Run `./gradlew --console=plain test`.
3. Run `./gradlew --console=plain build`.
4. Collect the mod jar from `build/libs/aura-cascade-<version>.jar`.

The encyclopedia on this branch requires [Patchouli: Fabric Edition](https://www.curseforge.com/minecraft/mc-mods/patchouli-fabric-edition/files/8713841)
`1.21.11-94.4-FABRIC` alongside Fabric API on both
client and server. Gradle resolves the development dependency; standalone
installations must include it separately.

Gradle defaults to one worker, a 1,536 MiB heap, no persistent daemon and a
512 MiB test heap. Do not run builds and game clients together when using the
4.5 GB tooling budget. The PowerShell scripts under `scripts/qa/` measure process
trees for builds, development clients and packaged smoke tests. Their sampled
memory guards are not operating-system memory limits.

The Fluxing Node integration bundles Team Reborn Energy `4.2.0` (MIT) for
modern Fabric energy receivers. The bundled API does not itself add
powered machines; transfer interoperability still needs acceptance fixtures.

## Dedicated Server Smoke

1. Use Java `21`.
2. For a repo-local dedicated server smoke, run `./gradlew --console=plain runServer` once and wait for the server bootstrap to finish.
3. For a standalone Fabric server, copy `build/libs/aura-cascade-<version>.jar` into the server `mods/` directory and start the server with `nogui`.
4. Confirm the log contains `Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.` and no Aura `Parsing error loading recipe` or Fabric dependency failures.
5. See `PORTING_NOTES.md` for the latest verified smoke result, the intentional post-start interrupt notes, and any exact blocker that prevented a standalone smoke.

## License Guidance

- `LICENSE` carries the MIT notice used for this unofficial port workspace together with Aura Cascade attribution and provenance notes.
- `src/main/resources/fabric.mod.json` declares the shipped mod metadata as MIT-licensed for the current release artifact.
- The current repo includes visual asset imports derived from `AuraCascade-592.jar!/assets/aura/textures/**`; `PORTING_NOTES.md` records the restored mapping, lowercase path normalization, and the remaining structural `minecraft:` references on that client surface.
- Any future direct import of upstream code or assets must retain the original notice in addition to this repo-level license file.

## Current Implemented Scope

- Fabric 1.21.1 scaffold with Gradle, wrapper generation target, Java 21 compilation, Fabric metadata, and test roots.
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
- Encyclopedia Aura retains `aura:encyclopedia_aura` and uses Patchouli book definitions under `data/aura/patchouli_books/` with localized content under `assets/aura/patchouli_books/`. The active pilot restores navigation and onboarding; it does not establish complete legacy instructional coverage.
- White Aura Crystals use a shaped ring of eight gold nuggets around one amethyst shard, yielding two crystals. This is an approved modernization. The Encyclopedia itself uses the original early crystal-plus-book recipe, without an Arcane Prism requirement. In-game guide copy describes only the current recipes.
- Fairy runtime lives under `src/main/java/pixlepix/auracascade/fairy/`, reconstructing the recovered role families as custom sprite entities with equipped-ring synchronization, in-world orbiting helpers, and persisted owner, slot, and role data.
- The late-game world slice gives concrete runtime behavior to the looter, spawner, miner, Nether ritual, End ritual, Traveler's Bricks, Rebounding Enigma, and fortified containment families.
- Kaleidoscopic enchantment keys live under `src/main/java/pixlepix/auracascade/enchantment/`, with six data-driven enchantments and a dedicated enchantable-item tag under `src/main/resources/data/aura/enchantment/` and `src/main/resources/data/aura/tags/item/`.
- Localization covers the registered block families plus the progression, storage, guidebook, accessory, fairy, and utility item set under `src/main/resources/assets/aura/lang/en_us.json`, and self-drop loot tables cover the same node, pump, consumer, vortex, monitor, ritual, storage, utility, and fortified block families under `src/main/resources/data/aura/loot_table/blocks/`.
- `AuraColor` preserves the eight-color parity surface with mechanically meaningful rule encoding for mass, ascent, passive ticks, flow restrictions, falling-power eligibility, and orange induced-current behavior.

## Compatibility Direction

- Accessories: player-facing ring, amulet, and sash behavior routes through an internal inventory-backed bridge first; keep any later third-party slot integration behind that seam.
- External energy: preserve aura as the canonical simulation, and keep any later cross-mod energy interoperability behind the existing narrow bridge so Fluxing Node parity does not collapse into generic FE behavior.
