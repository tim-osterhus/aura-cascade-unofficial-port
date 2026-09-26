# Encyclopedia Aura Coverage Audit

## Scope and Status

The primary source is `AuraCascade-592.jar`: `assets/aura/lang/en_US.lang`
provides the localized entry/page keys and titles, and
`pixlepix/auracascade/lexicon/LexiconData.class` identifies the legacy categories
and page types. The source contains 101 `aura.entry.*` keys and 261
`aura.page.*` keys. This document maps every entry key; the full 261-key map is
in `docs/audits/2026-09-23-guide-page-coverage.md`. Neither map claims that all
legacy pages or mechanics have been recreated.

The current source tree contains 41 entries and 371 pages, including 188
crafting pages and 14 quest pages. The parent reports thirteenth candidate
`306ab942…` passed 227 tests across 57 suites. Packaged observer run
`build/qa-audit/observer/runs/20260923-093128-934/guide-layout.json` is v2
`success: true`: all 41 entries / 371 pages / 14 quest pages were measured;
visible-text overflow 0, title overflow 0, and 151 whitespace-only advance
overflows. These trailing-space advances are excluded from the visible-text
bounds. This is a real-font layout pass, not a claim that every page was
manually opened or visually clicked. Of the crafting pages, the focused
coverage test checks 187 cards against registered recipe resources; the
Encyclopedia acquisition card is an additional onboarding recipe. Large
recipe collections are split into tool-family, sword-color, accessory-role,
and fairy-charm indexes, with former entry IDs retained as linked landing
hubs. The quest tracker has 14 Patchouli quest pages backed by the parent-owned
book-open advancement hook; the mapping below records each inventory goal,
reward, and advancement ID. Storage browser, four-slot accessory menu, typed
charm binding, node controls, and Red Aura triggers have source-backed
instructions. Consumer workflows cover the stable common machines, and
Kaleidoscopic copy follows the new E1 source/runtime contract. Edited entries
keep first text pages to 40 words, later text/quest pages to 55, and
crafting-page body text to 30, with headings and entry names at 27 characters
or fewer. Static JSON and copy-budget checks also pass. Cross-entry navigation
and recipe presentation remain separate acceptance checks.

The audit maps all 101 original entry keys individually, but does not claim
complete content coverage of the original 261 pages. Aura flow and the
piston-gated link, Fluxing Node export, and optional common-tag processor
conversion have concise source-backed instructions; live acceptance and
specific receiver/tag fixtures remain open. Late-game parity is not claimed.
The quest tracker is implemented by the book-open check plus persisted
advancements. The targeted runtime playtest rendered all 14 quest pages at GUI
scales 2 and 3, completed their goals/rewards, and passed one-time reward and
save/reload persistence checks. The packaged font audit measures page layout;
neither result claims every page was manually opened/clicked or that all
cross-entry navigation was exercised. See
`docs/audits/2026-09-23-accessory-storage-playtest.md` and
`docs/audits/2026-09-23-quest-contract.md`. Other runtime checks remain open
where called out below and in their owner contracts.

## Bounded Copy Pass

The current source tree now includes concise instructions for the confirmed
gaps below. This is a documentation checkpoint, not a full recreation of the
261 legacy pages or a live-acceptance claim.

- **Consumer rhythm:** `consumers_vortex` now warns that progress and power
  use continue without valid work, and that the Monitor reports readiness
  without gating progress. Throughput curves and live acceptance remain open.
- **Angelsteel tools and swords:** `angelsteel_tools` describes the correct
  tool's Efficiency, Shatter, Disintegrate, and eligible Fortune drop effect;
  the wording no longer implies a temporary held-tool change. `angelsteel_swords`
  describes all six color curses without inventing cadence or duration. Effect
  and drop checks remain open.
- **Angel's Heels:** `accessory_belt` explains the two-block step while clear
  and the brief lift against an obstacle. The 0.3-block collision ramp is a
  source detail, not a player-facing requirement; movement acceptance remains
  open.
- **Barbarian Sword:** `gear_utility` describes the wielder's growing strike
  strength for hits 5 through 99 ticks apart, compounding by five percent to a
  100-link cap. The exact source multiplier is `1.05^count`; boundary and
  damage acceptance remain open.
- **Dropped Red Hole:** current `gear_utility` copy says the dropped item
  explodes about every five seconds and disappears after roughly 25 minutes of
  world time. Source records 100-tick intervals, power 12, and a 30,000-tick
  dropped-item lifetime; blast radius and damage are not characterized in the
  guide. Runtime and save/reload acceptance remain open. The earlier “fiery
  eruptions” wording in this audit is superseded by the dated source correction
  below.
- **Fairy roles:** copy now covers Debuffer, Buffer, Pusher, Shooter, Scarer,
  Breeder, Digger, Lighter, and Glider alongside existing Fighter, Savior,
  Stealer, Fetcher, Baiter, Trainer, and Extinguisher instructions. Contract
  reconciliation confirms Pusher's 0.4 impulse; the nearest-Enemy selector,
  level RNG, and Stealer's item-loss guard are adaptations, not missing roles.
  Remaining source/runtime boundaries are the Shooter cache fix's next
  regression, Scarer's vanilla-natural-spawn hook scope, Digger/Glider hook
  acceptance, and Extinguisher compatibility with modded fire-material
  blocks. Fairy world effects still need live acceptance.

Consumer facts follow `docs/audits/2026-09-23-consumer-contract.md`, equipment
facts follow `docs/audits/2026-09-23-utility-equipment-contract.md`, and role
facts follow `docs/audits/2026-09-23-fairy-restoration-contract.md`.

## Entry-by-Entry Mapping

`Mapped` means a modern entry or section exists, not that the legacy behavior
has passed parity acceptance. Any remaining mechanic or workflow gap is stated
in the final column. `Reserved` marks an area whose contract is owned elsewhere
and is intentionally not expanded in this phase.

| Legacy key | Shipped title | Modern destination | Coverage status or unresolved mechanic |
| --- | --- | --- | --- |
| `aura.entry.introduction` | Introduction to this Walkthrough | `aura:getting_started` | Mapped to the short orientation; the original illustrated walkthrough is not reproduced. |
| `aura.entry.basicSetup` | Basic Setup | `aura:first_aura_circuit` | Mapped to the first circuit; natural acquisition and full survival progression remain unverified. |
| `aura.entry.vortexInfusion` | Vortex Infusion | `aura:consumers_vortex`, `aura:arcane_progression` | Four adjacent pedestals, unordered components, falling-power receipts, White wildcard behavior, and the four accepted outputs are instructed; live survival acceptance remains open. |
| `aura.entry.accumulate` | Energy Storage | `aura:aura_basics` / Piston Storage | A sticky piston moves cobblestone across a straight node link to gate that route; removing it reconnects the nodes after link refresh (up to 200 ticks). Other open links may still transfer Aura, and retained Aura depends on available charge and eligible paths. Live acceptance remains open. |
| `aura.entry.autoOff` | Power Saver | `aura:pumps_control` | The Monitor outputs 15 for the first adjacent empty pump or consumer without valid work, and 0 for a pump holding power or consumer with valid work; ordinary nodes are ignored. Redstone wiring and live acceptance remain to be checked. |
| `aura.entry.multiplePumps` | More power | `aura:pump_variants`, `aura:first_aura_circuit` | Mapped to pump selection and the circuit; multi-pump throughput and survival setup lack live acceptance. |
| `aura.entry.basics` | Basic Concepts | `aura:aura_basics` | Mapped to Aura storage and network overview. |
| `aura.entry.power` | Power | `aura:consumers_vortex`, `aura:consumer_fieldwork`, `aura:first_aura_circuit` | Readouts, eight base-cost/max-progress pairs, cadence, doubling, quarter-retention, and the idle-work warning are documented. The Monitor reports readiness but does not gate progress. Full throughput curves and live acceptance remain open. |
| `aura.entry.auraFlow` | Aura Flow Mechanics | `aura:aura_basics` / Natural Flow | The guide explains the `(20 - distance)^2` link weight, regular-node retention weight 400, once-per-second transfer cycle, >25 source/target difference gate, and color direction rules. Live transfer acceptance remains open. |
| `aura.entry.interactions` | Controls | `aura:first_aura_circuit`, `aura:pumps_control` | Mapped to crystal feeding, redstone control, and pump use; complete control/readout acceptance remains open. |
| `aura.entry.materials` | Materials | `aura:arcane_progression` | Matching Aura Crystal plus iron yields each of eight Arcane Ingots; colored gems and the eight-distinct-gem Prism route are documented. Higher material/tool behavior remains open. |
| `aura.entry.monitor` | Monitor | `aura:pumps_control` | The Monitor checks the first adjacent supported machine, not the strongest signal or aggregate Aura storage; it ignores ordinary nodes and emits the source-confirmed pump/consumer statuses. Directional selection and scheduled updates still need live acceptance. |
| `aura.entry.pumps` | Pumps | `aura:pumps_control`, `aura:pump_variants` | Mapped to family overview and recipes; other-trigger live QA and exact legacy timing remain open. |
| `aura.entry.comparator` | Comparator Interactions | `aura:pumps_control` | Mapped to current network output guidance; legacy comparator scaling across every block is not fully audited. |
| `aura.entry.black` | Black Aura | `aura:aura_colors` / Black | Current vertical-only, zero-weight, no-falling-power rules are covered. Black Manipulator refill/drain behavior is explained in `aura:node_variants`; live acceptance remains open. |
| `aura.entry.red` | Red Aura | `aura:aura_colors` / Red | A Red Node scans an AABB expanded by three each server tick; it consumes primed TNT at fuse <=2 for a 200,000 lift budget or a near-primed Creeper for 50,000, divided by rise and capped by source Aura. Explosion visuals do not damage. Integrated build passed; Red-trigger live acceptance remains open. |
| `aura.entry.orange` | Orange Aura | `aura:aura_colors` / Orange | Current horizontal-only, zero-weight, directional-current rules are covered. |
| `aura.entry.yellow` | Yellow Aura | `aura:aura_colors` / Yellow | Current ascent boost and passive loss are covered; exact legacy decay acceptance remains open. |
| `aura.entry.green` | Green Aura | `aura:aura_colors` / Green | Current day/night mass behavior is covered. |
| `aura.entry.blue` | Blue Aura | `aura:aura_colors` / Blue | Current rain-dependent ascent behavior is covered. |
| `aura.entry.violet` | Violet Aura | `aura:aura_colors` / Violet | Current growth and dissipation boundaries are covered; live persistence/timing acceptance remains open. |
| `aura.entry.alternating` | Alternating Pumps | `aura:pump_variants` | Mapped to separate alternating recipe pages and time-varying output; exact cycle acceptance remains open. |
| `aura.entry.flux` | Fluxing Node | `aura:arcane_progression` / Export Stored Power | The registered recipe and face-connected receiver workflow are documented: export runs each second, supports up to four accepting receivers, pauses above four, and converts at up to 15 energy per stored power. No named modpack compatibility is claimed; an in-game receiver fixture remains open. |
| `aura.entry.capacitor` | Aura Capacitor | `aura:node_variants` / Capacitor | Empty-hand sneak-use cycles 1,000 -> 10,000 -> 100,000 -> 100 -> 1,000; action bar, HUD maximum, and comparator scale follow the threshold. A 19-tick check arms a burst; the next divisible-by-five tick clears it and sets 110 blocked-receiving ticks. Arming does not wait for cooldown zero. Integrated build passed; live acceptance remains open. |
| `aura.entry.conserve` | Conserving Aura Node | `aura:node_variants` / Conserving Node | Current same-height transfer rule is covered. |
| `aura.entry.manipulator` | Aura Manipulator | `aura:node_variants` / Manipulators | Black/Orange forms receive only their color. Every second at phase 2, they clear all Aura and replace it with 100,000 of their own color only while powered; unpowered they drain/destroy incoming Aura. Powered transfer still works, and Aura refresh preserves network links and stored power. Integrated build passed; live acceptance remains open. |
| `aura.entry.pumpFall` | Momentum Pump | `aura:pump_variants` / Momentum | Fall-fed operation and recipe are covered; live trigger and timing acceptance remain open. |
| `aura.entry.pumpLight` | Illumination Pump | `aura:pump_variants` / Illumination | Adjacent glowstone/torch fuel and recipe are covered; live trigger and timing acceptance remain open. |
| `aura.entry.pumpRedstone` | Redstone Pump | `aura:pump_variants` / Redstone | Powered-wire fuel and recipe are covered; full path, consumption, and timing acceptance remain open. |
| `aura.entry.pumpProjectile` | Projectile Pump | `aura:pump_variants` / Projectiles | Arrow, egg, and snowball fuel and recipes are covered; live impact and timing acceptance remain open. |
| `aura.entry.consumers` | Consumers | `aura:consumers_vortex`, `aura:consumer_fieldwork`, `aura:arcane_progression` | Readouts/costs, smelting, inputless Grower work at two blocks above, 4-by-4 Fisher water support, Brewer stages, Colorer sheep work, processor recipes, and Synthesizer output are documented. Optional `c:ores/<material>` to `c:dusts/<material>` conversion is explained (two dust in the normal Processor, three in the Prismatic Processor); a representative tag fixture and live throughput acceptance remain open. |
| `aura.entry.crafting` | Vortex Infusion | `aura:consumers_vortex`, `aura:arcane_progression` | Four-pedestal item capture, falling receipts, color rules, no-timer completion, and the four corrected output recipes are documented; live acceptance remains open. |
| `aura.entry.dye` | Cascading Colorer | `aura:consumer_fieldwork`, `aura:consumers_vortex` | Its 12 / 50 pair and radius-two sheep selection, fleece restoration, and 16-color choice are documented; distribution/live acceptance remain open. |
| `aura.entry.angel` | Cascading Synthesizer | `aura:consumers_vortex`, `aura:arcane_progression` | No-input completion, first-tier Angelsteel output, and 50 / 10,000 max-progress/base-cost pair are documented; live acceptance remains open. |
| `aura.entry.furnace` | Cascading Smelter | `aura:consumer_fieldwork`, `aura:consumers_vortex` | Its 3 / 190 pair and dropped-item vanilla smelting transformation are documented; live input/output acceptance remains open. |
| `aura.entry.fish` | Cascading Fisher | `aura:consumer_fieldwork`, `aura:consumers_vortex` | Its 200 / 200 pair, 4-by-4 support footprint, flowing-water allowance, and modern fishing-table context are documented; live table/layout acceptance remains open. |
| `aura.entry.loot` | Cascading Looter | `aura:late_systems`, `aura:consumers_vortex` | Its four-Yellow-Gem vortex recipe and 100,000 Yellow power per pedestal are documented; chest-loot selection and operation behavior remain unverified. |
| `aura.entry.mob` | Cascading Spawner | `aura:late_systems`, `aura:consumers_vortex` | Its four-Violet-Gem vortex recipe and 100,000 Violet power per pedestal are documented; spawn eligibility and operation behavior remain unverified. |
| `aura.entry.plant` | Cascading Grower | `aura:consumer_fieldwork`, `aura:consumers_vortex` | Its 2 / 50 pair, inputless work, exact two-block target height, and 50 stochastic random ticks are documented; live growth acceptance remains open. |
| `aura.entry.books` | Book Storage | `aura:storage_network` | Browser use, localized-name search, 9-by-3 results, amount/Max retrieval, deposits, quick withdrawal, power/completeness gating, persisted book storage, and all 13 Storage Book plus Coordinator recipes are mapped. Shelf conversion is not presented as a standalone recipe; live UI acceptance remains open. |
| `aura.entry.miner` | Cascading Miner | `aura:late_systems` | Mapped to late-system overview; containment damage, charge release, and ore behavior lack gameplay acceptance. |
| `aura.entry.nether` | Ritual of the Nether | `aura:late_systems`, `aura:consumers_vortex` | Its four-Red-Gem vortex recipe and 100,000 Red power per pedestal are documented; spread boundaries, biome effects, persistence, and safety remain open. |
| `aura.entry.end` | Ritual of the End | `aura:late_systems` | Mapped to late-system overview; source conversion rules and world limits remain open. |
| `aura.entry.brewer` | Cascading Brewer | `aura:consumer_fieldwork`, `aura:consumers_vortex` | Its 25 / 500 pair, water-to-Awkward-to-base-to-modifier stages, and per-stack bottle advancement are documented; random outcomes and live acceptance remain open. |
| `aura.entry.amuletAngel` | Amulet of the Angel's Wing | `aura:accessory_loadout` hub; `aura:accessory_amulets` | The registered recipe and physical Amulet slot are documented; equipped-use ascent/descent bounds and survival behavior need live acceptance. |
| `aura.entry.beltAngel` | Sash of the Angel's Heels | `aura:accessory_loadout` hub; `aura:accessory_belt` | The recipe, physical Belt slot, two-block step while clear, and brief lift against an obstacle are documented. The source's 0.3-block collision ramp and live movement acceptance remain open; player text does not describe modifier internals. |
| `aura.entry.prismaticWand` | Prismatic Wand | `aura:prismatic_wand`; `aura:gear_utility` hub | Sneak-use cycles Selection/Copy/Paste; two corners bound the region. Copy stores live source coordinates and player-relative offset, not a snapshot. Paste follows the offset from the current player, fills only air, and leaves occupied targets alone. Regions cap at 512 total cells including air and are rejected before placement if any source/target cell is unloaded or outside build height/world border. Survival consumes one matching Block Item only after a successful placement. Fluids/block-entity data and undo are not supported. Live acceptance remains open. |
| `aura.entry.mirror` | Mirror of the Angel | `aura:gear_utility` | Mapped to the equipment overview; projectile deflection target and controls remain unverified. |
| `aura.entry.angelsteel` | Angel's Steel | `aura:arcane_progression`, `aura:angelsteel_tools` hub, `aura:angelsteel_axes`, `aura:angelsteel_pickaxes`, `aura:angelsteel_shovels` | First-tier Synthesizer output, tier upgrades, all 36 registered axe/pickaxe/shovel recipe cards, correct-tool speed gifts, and eligible non-crop Fortune drop effects are documented. Current wording describes the drop effect without implying it fades; live drop checks remain open. |
| `aura.entry.angelsteelSword` | Angel's Steel Swords | `aura:angelsteel_swords` hub and six color entries | All 72 registered sword recipe cards are indexed by color; player text now covers Red fire, Orange lift, Yellow lightning, Green magic harm and spread, Blue low-health harm, and Violet position swap. Live effect acceptance remains open; numeric cadence/duration is not specified here. |
| `aura.entry.protection` | Protection Amulets | `aura:accessory_loadout` hub; `aura:accessory_amulets` | Six protection amulet recipes and the Amulet slot are mapped; hazard/color matrix and healing amounts remain open. |
| `aura.entry.swordTransmute` | Transmuting Sword | `aura:gear_utility` | Mapped to the equipment overview; supported entity pairs and survival constraints need acceptance. |
| `aura.entry.swordCombo` | Sword of the Barbarian | `aura:gear_utility` | Player text describes the wielder's strike-strength chain: hits 5 to 99 ticks apart compound by five percent to 100 links. Source scaling is `1.05^count`; live damage and boundary checks remain open. |
| `aura.entry.explosionRing` | Ring of the Shattered Stone | `aura:late_systems` | The ring wearer must be within 3 blocks of the explosion center on each axis; the affected blast spares ores and other nonterrain blocks while listed terrain remains destructible. It does not protect the wearer from damage. |
| `aura.entry.redHole` | Portable Red Hole | `aura:gear_utility` | The recipe and dropped-item warning describe non-fiery explosions about every five seconds, with the dropped item fading after roughly 25 minutes. Source timing is 100 ticks at power 12 and 30,000 ticks to expiry; radius and damage remain uncharacterized. A bounded two-cycle live test passes in source 16; save/reload remains unverified. |
| `aura.entry.blackHole` | Portable Black Hole | `aura:gear_utility` | Mapped to the equipment overview; inventory deletion scope and dropped-item persistence need acceptance. |
| `aura.entry.magicRoad` | Traveler's Bricks | `aura:travelers_bricks`, linked from `aura:late_systems` | The player chapter states the 0.8-block contact height, full light, and the source-confirmed contact rule: motion faster than 0.25 blocks per tick gains a 5-block-per-tick push along its full 3D direction, added to current motion. The registered recipe is linked. Movement/light acceptance remains open. |
| `aura.entry.trampoline` | Rebounding Enigma | `aura:rebounding_enigma`, linked from `aura:late_systems` | The player chapter states the 0.8-block contact height, full light, upward motion set to 10 blocks per tick with horizontal motion preserved, and no fall-damage protection. The registered recipe is linked. Launch/landing acceptance remains open. |
| `aura.entry.swordThief` | Sword of the Thief | `aura:gear_utility` | Mapped to the equipment overview; correct trade-offer selection and drop chance need source-backed tests. |
| `aura.entry.amuletFood` | Amulet of the Forbidden Fruit | `aura:gear_utility`, `aura:accessory_amulets` | Current copy instructs completing an eating or drinking use while equipped, including potions, milk, and Creative use; the source-backed effect is covered. Effect/persistence live acceptance remains open. |
| `aura.entry.enchanter` | Kaleidoscopic Enchantments | `aura:kaleidoscopic_enchantments`, `aura:enchant_matrix`, `aura:consumers_vortex` | Four Black Gems and per-pedestal White wildcard cost, supported tool plus color Aura Crystal input, and the crystal-consumed-on-failure rule are documented. Integrated build passed; E1 live acceptance remains open. |
| `aura.entry.basicEffects` | Basic Effects | `aura:kaleidoscopic_enchantments` | Current single-color mappings are summarized; in-world effect and level acceptance remains open. |
| `aura.entry.areaOfEffect` | Area of Effect interactions | `aura:enchant_matrix` | Growable-block traversal, target-centered splash cube, and mining families are summarized from the E1 contract; live acceptance remains open. |
| `aura.entry.combat` | Combat interactions | `aura:enchant_matrix` | Attack splash, integer half-strength healing, ignition, recoil, outgoing-damage reduction, and incoming-damage multiplier are documented; live acceptance remains open. |
| `aura.entry.looting` | Resource interactions | `aura:enchant_matrix` | Modern killed-entity loot-table roll and bounded ore-to-ingot conversion are documented; Forge rare-drop and ore-dictionary parity remains open. |
| `aura.entry.negative` | Negative interactions | `aura:enchant_matrix` | Mining slowdown, recoil, and outgoing damage subtraction are documented; live effects remain open. |
| `aura.entry.speedBoosts` | Mining Speed boosts | `aura:enchant_matrix` | Source-backed pair multipliers and block families are listed, including the hardness-3 Violet + Orange boundary; live acceptance remains open. |
| `aura.entry.fairies` | Fairies | `aura:fairy_roles`, `aura:fairy_charm_basics`, and role-family charm entries | Ring-backed bind/release workflow, typed recipes, and player-facing instructions for all 16 action-bearing roles are mapped across the role-family chapters; the plain Fairy has no distinct role action. Runtime acceptance and the specific boundaries listed in the bounded copy pass remain open. |
| `aura.entry.fairyCombat` | Fighter Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | Fighter, Savior, Debuffer, Buffer, Pusher, Shooter, and Scarer instructions are present. The nearest-Enemy selector is a documented modernization; Shooter cache regression and Scarer hook-scope checks remain open. |
| `aura.entry.fairyDebuff` | Debuffer Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide describes all six maladies and their 200-tick duration at the three-tick role cadence. Nearest-Enemy targeting is an intentional selector adaptation; live action acceptance remains open. |
| `aura.entry.fairyBuff` | Buffer Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide describes the six possible owner gifts and the 2,400-tick selection cadence/duration. Using the level RNG is an intentional adaptation; live action acceptance remains open. |
| `aura.entry.fairySteal` | Stealer Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide explains that a nearby player's held item appears as a drop at the bound player's position. Clearing the hand only after successful item insertion is an intentional item-loss guard, not a missing transfer feature; multiplayer acceptance remains open. |
| `aura.entry.fairyPush` | Pusher Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide describes the source-equivalent 0.4 knockback impulse every three ticks within two blocks; knockback resistance can blunt it. Nearest-Enemy targeting is an intentional adaptation; live acceptance remains open. |
| `aura.entry.fairyShoot` | Shooter Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide describes nearby arrows below 10 damage gaining 10 damage and becoming critical. The tracked-arrow cache correction is post-198 and requires regression; live action acceptance remains open. |
| `aura.entry.fairySavior` | Savior Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide states that it strikes a nearby hostile for 10 damage when its owner is below five health; live acceptance remains open. |
| `aura.entry.fairyFetch` | Fetcher Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide states that it carries dropped items within four blocks of the fairy to its owner once pickup delay ends; live acceptance remains open. |
| `aura.entry.fairyBait` | Baiter Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide lists the four possible passive animals and source-backed 1-in-3,600 per-tick chance; live spawn acceptance remains open. |
| `aura.entry.fairyBreed` | Breeder Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide says that every three ticks it makes the first ready adult animal within one block eager to breed with its owner. Actual world interaction remains live-unverified. |
| `aura.entry.fairyScare` | Scarer Fairy | `aura:fairy_roles`, `aura:fairy_combat_charms` | The guide explains that nearby Scarers make natural arrivals less likely. Source hook checks vanilla-valid natural-spawn positions; other legacy spawn-event contexts are not established, and live hook acceptance remains open. |
| `aura.entry.fairyExtinguisher` | Extinguisher Fairy | `aura:fairy_roles`, `aura:fairy_world_charms` | Player text describes clearing the owner's fire and putting out fire at the fairy's feet. Vanilla fire/soul-fire support is present; modded blocks using fire material remain a source-identified compatibility gap. |
| `aura.entry.fairyDigger` | Digger Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide explains Ring-of-Binding dependence and compounding break speed in 1.08 steps. The source count starts at -1 and caps at 15; the mining-speed hook remains live-unverified. |
| `aura.entry.fairyLight` | Lighter Fairy | `aura:fairy_roles`, `aura:fairy_world_charms` | The guide describes a hidden fading light placed in an empty spot below light level 10. Runtime registration and expiry remain live-unverified. |
| `aura.entry.fairyFall` | Glider Fairy | `aura:fairy_roles`, `aura:fairy_travel_charms` | With the Ring of Binding, the guide says each nearby Glider halves fall distance; a long fall can still hurt. The fall-distance hook remains live-unverified. |
| `aura.entry.fairyTrain` | Training Fairy | `aura:fairy_roles`, `aura:fairy_work_charms` | The guide states that it may release five experience on a source-backed 1-in-1,200 per-tick chance; live acceptance remains open. |
| `aura.entry.video` | Video Walkthrough | `aura:getting_started` | Mapped to the walkthrough; external video links and their version-specific claims are intentionally not embedded. |
| `aura.entry.00quest` | Crystals | `aura:quest_progression`; linked to `aura:white_aura_crystal`, `aura:aura_colors`, `aura:arcane_progression` | Trigger `aura:quest/crystals`: goal 1 White Aura Crystal in inventory; reward 64 ordinary White Aura Crystals. |
| `aura.entry.01quest` | Nodes | `aura:quest_progression`; linked to `aura:node_variants` | Trigger `aura:quest/nodes`: goal 1 Aura Node in inventory; reward 16 Aura Nodes. |
| `aura.entry.02quest` | Pumps | `aura:quest_progression`; linked to `aura:pump_variants`, `aura:first_aura_circuit` | Trigger `aura:quest/pumps`: goal 1 Aura Node Pump in inventory; reward 64 Sticks. |
| `aura.entry.03quest` | Furnace | `aura:quest_progression`; linked to `aura:consumer_fieldwork`, `aura:consumers_vortex` | Trigger `aura:quest/furnace`: goal 1 Cascading Smelter in inventory; reward 64 ordinary White Aura Crystals. |
| `aura.entry.04quest` | Red Crystals | `aura:quest_progression`; linked to `aura:aura_colors` / Red | Trigger `aura:quest/red_crystals`: goal 1 Red Aura Crystal in inventory; reward 4 TNT. Red-trigger live acceptance remains open. |
| `aura.entry.05quest` | Processor | `aura:quest_progression`; linked to `aura:consumers_vortex`, `aura:arcane_progression` | Trigger `aura:quest/processor`: goal 1 Cascading Processor in inventory; reward 8 Coal. |
| `aura.entry.06quest` | Dye | `aura:quest_progression`; linked to `aura:consumer_fieldwork`, `aura:consumers_vortex` | Trigger `aura:quest/dye`: goal 1 Cascading Colorer in inventory; reward 32 White Wool. |
| `aura.entry.07quest` | Arcane Ingot | `aura:quest_progression`; linked to `aura:arcane_progression` | Trigger `aura:quest/arcane_ingot`: goal 1 White Arcane Ingot in inventory; reward 15 White Arcane Ingots. |
| `aura.entry.08quest` | Vortex Infusion | `aura:quest_progression`; linked to `aura:consumers_vortex`, `aura:arcane_progression` | Trigger `aura:quest/vortex_infusion`: goal 1 Vortex Controller in inventory; reward 4 Vortex Pedestals. |
| `aura.entry.09quest` | Arcane Gem | `aura:quest_progression`; linked to `aura:arcane_progression`, `aura:consumers_vortex` | Trigger `aura:quest/arcane_gem`: goal 1 White Arcane Gem in inventory; reward 8 White Arcane Gems. |
| `aura.entry.10quest` | Arcane Prism | `aura:quest_progression`; linked to `aura:arcane_progression` | Trigger `aura:quest/arcane_prism`: goal 1 Arcane Prism in inventory; reward 1 Arcane Prism. |
| `aura.entry.11quest` | Synthesizer | `aura:quest_progression`; linked to `aura:consumers_vortex`, `aura:arcane_progression` | Trigger `aura:quest/synthesizer`: goal 1 Cascading Synthesizer in inventory; reward 64 ordinary White Aura Crystals. |
| `aura.entry.12quest` | Angel's Steel | `aura:quest_progression`; linked to `aura:arcane_progression`, `aura:angelsteel_tools` | Trigger `aura:quest/angelsteel`: goal 1 `aura:angelsteel_ingot_2` (tier two) in inventory; reward 15 `aura:angelsteel_ingot_1` (tier one). |
| `aura.entry.13quest` | Fairies | `aura:quest_progression`; linked to `aura:fairy_roles` | Trigger `aura:quest/fairies`: goal 1 Ring of Binding in inventory; reward 1 Fairy Charm. All 14 quest pages rendered at GUI scales 2/3 and one-time persisted reward delivery passed targeted playtest; fairy-role copy/acceptance is tracked separately. |
| `aura.entry.patreon` | Support the Author | No modern gameplay entry | Non-mechanical support request; intentionally omitted from the player mechanics guide. |
| `aura.entry.tutorial` | Tutorial | `aura:getting_started` | Mapped to the entry route; original tutorial-page navigation is not recreated. |

## Dated Source Correction (2026-09-23, Source 16)

The earlier “fiery eruptions” description above records the superseded reading
of legacy copy, not current behavior. Source review confirms the original Red
Hole explosion is non-fiery (`fire=false`). Source 16 changes the port to a
per-item tick path, skips vanilla item ticking through ages 0-99 of each
100-tick blast cycle, and uses the guide's corrected “explodes” wording. This
Source 16 is integrated and green at 244 tests across 60 suites. Its bounded
live test passed two blasts at game times 124000/124100, with the marker retained
at ages 0/99 and a 32-block outside witness unchanged. The one-second freeze
control produced no extra explosion. Save/reload and exact-candidate packaged
client/multiplayer checks remain pending; see the [current acceptance
ledger](2026-09-22-video-acceptance.md#finite-current-beta-gates-2026-09-23).

## Recipe and Validation Boundary

The earlier coverage note began with a 22-page pilot and six progression
cards; that count is no longer the current scope. The expanded catalog uses
registered recipe identifiers, with examples including:

- Aura Crystals: `aura:aura_crystal_white`, `aura:aura_crystal_black`, `aura:aura_crystal_red`, `aura:aura_crystal_orange`, `aura:aura_crystal_yellow`, `aura:aura_crystal_green`, `aura:aura_crystal_blue`, `aura:aura_crystal_violet`.
- Node forms: `aura:aura_node`, `aura:aura_node_capacitor`, `aura:aura_node_conserve`, `aura:aura_node_black`, `aura:aura_node_orange`.
- Pumps: `aura:aura_node_pump`, `aura:aura_node_pump_alt`, `aura:aura_node_pump_light`, `aura:aura_node_pump_light_alt`, `aura:aura_node_pump_fall`, `aura:aura_node_pump_fall_alt`, `aura:aura_node_pump_projectile`, `aura:aura_node_pump_projectile_alt`, `aura:aura_node_pump_redstone`, `aura:aura_node_pump_redstone_alt`.
- Progression: `aura:consumer_block_ore`, `aura:aura_node_crafting_center`, `aura:aura_node_crafting_pedestal`, `aura:consumer_block_angel`, `aura:aura_node_flux`, `aura:consumer_block_ore_adv`.

`EncyclopediaAuraGuideCoverageTest` maps all 101 original entry keys one-to-one,
checks expanded-entry names/titles and text bounds, verifies 187 expected
recipe IDs resolve to registered crafting resources, and compares all 14
Patchouli quest triggers with the parent manifest and advancement files. It
also checks selected rewards and source-backed instructions; crafting-page
body text is bounded at 30 words and quest-page text at 55. The ring entry
follows `docs/audits/2026-09-23-shattered-stone-contract.md`:
`CUT_SANDSTONE`, not `SMOOTH_SANDSTONE`, represents the legacy smooth
sandstone state, and the ring does not protect its wearer from damage.

The source tree has 41 entries, 371 pages, 188 crafting pages, and 14 quest
pages. The parent reports thirteenth candidate `306ab942…` passed 227 tests
across 57 suites. Packaged observer v2 run
`build/qa-audit/observer/runs/20260923-093128-934/guide-layout.json` passed
using the game font: 0 visible-text overflows, 0 title overflows, and 151
whitespace-only advance overflows (not visible-text failures). All 371 page
layouts were measured. This does not establish that every page was manually
opened or visually clicked.
The targeted runtime record in
`docs/audits/2026-09-23-accessory-storage-playtest.md` also passes all 14 quest
pages at GUI scales 2 and 3, non-consuming goals, one-time rewards, and saved
advancement persistence after reload. This is not a full-guide
visual/navigation pass. An earlier storage-browser playtest recorded search
input as unverified and scale-2/3 visual failures; parent source fixes were
included in the passing build, but post-fix browser visuals/search are not yet
verified in the cited playtest.

The following was the thirteenth-candidate audit-time boundary, not a current
open-issues list: cross-entry navigation and recipe presentation,
storage-browser search and corrected visuals, questline toggle behavior, the
multiplayer binding-phase retry, and runtime behaviors called out in the
mapping and owner contracts. Later bounded results and exact-candidate checks
are tracked in the [current acceptance ledger](2026-09-22-video-acceptance.md).
The fairy role contract remains bounded by the per-role evidence in that
ledger; modded fire-material compatibility is still a stated limitation. This
documentation-only update ran no Gradle/client task and captured no
screenshot/video. The 101-entry map is complemented by the 261-page-key
mapping; the font pass does not claim complete visual clicking, navigation, or
runtime acceptance.
