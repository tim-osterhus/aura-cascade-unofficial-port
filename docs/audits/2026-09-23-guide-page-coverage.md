# Encyclopedia Aura Page-Key Coverage

## Scope

Primary text source: `AuraCascade-592.jar`,
`assets/aura/lang/en_US.lang`. It contains exactly 261 `aura.page.*` keys.
The modern book had 345 pages before the page-coverage pass; the source tree
now has 41 entries and 371 pages. This audit maps every localized page key to a modern
entry or gives a specific reason why its text is not a gameplay instruction.
Page mapping is not a claim that every 592 mechanic or recipe is reproduced.

The comparison uses the current guide JSON, Java implementation, focused
source contracts, and the existing entry-level audit in
`docs/audits/2026-09-23-guide-coverage.md`. It does not infer mechanics from
textures or other assets. `Mapped` means the modern player instruction is
present, including where current behavior intentionally differs from 592.
`Caption` and `Flavor` rows identify nonprocedural legacy text and explain its
modern destination or omission.

## Repairs In This Pass

- Added the falling-power relationship, crosshair device readout, one-way
  redstone node control, and current comparator behavior.
- Added pump feed layout, current fuel offers and rates, alternating output,
  and the lost-container warning.
- Documented storage limits, specialized-book filters, and coordinator load.
- Added Looter, Spawner, Miner containment/release, fortified-block recipe,
  and ritual operating instructions.
- Added Wing controls, amulet effects, utility item actions, Angelsteel tier
  bonuses, and Enchanter success odds.
- Linked Furnace and Dye quests directly to their actionable machine pages.

The Enchanter input warning remains in place: put the offered crystal on a
clear side away from Aura Nodes that could absorb it first.

## Page-Key Mapping

| Legacy page key(s) | Modern destination | Coverage decision |
| --- | --- | --- |
| `aura.page.introduction0` | `aura:getting_started`, `aura:first_aura_circuit` | Mapped to the short onboarding route; no separate legacy walkthrough is needed. |
| `aura.page.basicSetup0`, `aura.page.basicSetup1` | `aura:first_aura_circuit` | Mapped to pump, node, crystal, fuel, and falling-power setup; the second key is a diagram caption. |
| `aura.page.vortexInfusion0` | `aura:consumers_vortex`, `aura:arcane_progression` | Mapped to four-pedestal layout, receipts, wildcard power, and supported progression outputs. |
| `aura.page.accumulate0` | `aura:aura_basics` / Piston Storage | Mapped to the cobblestone-gated straight link and its refresh delay. |
| `aura.page.autoOff0` | `aura:pumps_control` / Power-Saver Signal | Mapped to the current Monitor status; it reports readiness but does not stop work. |
| `aura.page.multiplePumps0` | `aura:pumps_control` / Separate Pump Feeds | Mapped to upward-only pumps and separate lower input branches. |
| `aura.page.basics0`, `aura.page.basics1` | `aura:aura_basics`, `aura:first_aura_circuit` | Mapped to crystals, stored Aura, straight links, and Aura's role as the transport medium for falling power. |
| `aura.page.power0`, `aura.page.power1` | `aura:aura_basics` / Falling Power, `aura:first_aura_circuit` | Mapped to amount, fall distance, color mass, upward pumping, and the lower receiving block. |
| `aura.page.auraFlow0`, `aura.page.auraFlow1` | `aura:aura_basics` / Natural Flow | Mapped to transfer timing, link weighting, reserve, equilibrium gate, and directional color rules. |
| `aura.page.auraFlow2`, `aura.page.auraFlow3` | `aura:aura_basics` / Eight Colors | These are crystal/node image labels; their functions are described in the modern network and color pages. |
| `aura.page.interactions0` | `aura:first_aura_circuit`, `aura:white_aura_crystal`, `aura:aura_basics` / Read a Device | Direct crystal use and dropped absorption are mapped; modern crosshair inspection replaces the old empty-hand click. |
| `aura.page.interactions1` | `aura:node_variants` / Redstone Gate | Mapped to a powered regular Node stopping sends while still accepting Aura. |
| `aura.page.materials0`, `aura.page.materials2` | `aura:arcane_progression`, `aura:consumers_vortex` / Tagged Ore | Mapped to Processor use, supported ore-to-dust conversion, and matching-crystal ingots. The old wool-to-ingot route is listed under source conflicts. |
| `aura.page.materials1` | `aura:consumers_vortex` | `Processing...` is a chapter divider, not an instruction. |
| `aura.page.materials3` | `aura:arcane_progression` / Matching Gem Recipes | Mapped to one diamond and three same-color ingots with per-pedestal White costs. |
| `aura.page.materials4`, `aura.page.materials8` | No gameplay page | `Oohh, Shiny!` and the slogan are flavor text, omitted without losing an instruction. |
| `aura.page.materials5`, `aura.page.materials6` | `aura:arcane_progression` / Arcane Prism, Prismatic Processor | Mapped to eight distinct gems and the registered Processor upgrade pattern. |
| `aura.page.materials7` | `aura:consumers_vortex` / Tagged Ore | Three-dust conversion is mapped; the old wool-to-dye action is not present in the modern Processor and is called out below. |
| `aura.page.monitor0` | `aura:pumps_control` / Power-Saver Signal | Mapped to first-adjacent machine selection and current empty-pump/no-work output. |
| `aura.page.monitor1` | No gameplay page | CRT joke/caption; omitted as nonmechanical flavor. |
| `aura.page.pumps0` | `aura:pumps_control`, `aura:pump_variants` | Mapped to pump families, inputs, upward transfer, and feed layout. |
| `aura.page.pumps1` | `aura:pump_variants` / Light and Fall Offers, Projectile and Wire Offers | Mapped to current eligible run-time and nominal-rate offers. |
| `aura.page.pumps2` | `aura:pump_variants` / Burning | Furnace-fuel operation and the no-container-return warning are mapped; the localized 200 rate conflicts with the recovered bytecode/config value of 300. |
| `aura.page.pumps3` | No gameplay page | `Fire it up!` is a joke/caption, omitted as flavor. |
| `aura.page.comparator0` | `aura:node_variants` / Comparator Readouts | Mapped to current Node capacity and Consumer stored-work-power signals; modern Consumer signal is not work progress. |
| `aura.page.black0` | `aura:aura_colors` / Black | Mapped to vertical-only movement, zero effective weight, and no falling power. |
| `aura.page.black1` | `aura:node_variants` / Capacitor | The joke points at the real interaction; the current threshold-triggered Capacitor instructions cover it. |
| `aura.page.red0` | `aura:aura_colors` / Red | Mapped to TNT/Creeper lift budgets, range, and harmless explosion visuals. |
| `aura.page.red1` | No gameplay page | `Boom.` is sound-effect flavor, omitted. |
| `aura.page.orange0` | `aura:aura_colors` / Orange | Mapped to horizontal-only flow and induced current. |
| `aura.page.orange1` | No gameplay page | Flow pun, omitted as flavor. |
| `aura.page.yellow0` | `aura:aura_colors` / Yellow | Mapped to the ascent boost and passive charge loss. |
| `aura.page.yellow1` | No gameplay page | Disappearance joke, omitted as flavor. |
| `aura.page.green0` | `aura:aura_colors` / Green | Mapped to day/night effective weight. |
| `aura.page.green1` | No gameplay page | Photosynthesis joke, omitted as flavor. |
| `aura.page.blue0` | `aura:aura_colors` / Blue | Mapped to rainy/dry ascent behavior. |
| `aura.page.blue1` | No gameplay page | Weather joke, omitted as flavor. |
| `aura.page.violet0` | `aura:aura_colors` / Violet | Mapped to growth and current 25/2,600 dissipation boundaries. |
| `aura.page.violet1` | No gameplay page | Growth joke, omitted as flavor. |
| `aura.page.alternating0` | `aura:pump_variants` / Alternating Rhythm | Mapped to triple accepted speed and the modern sine-like transfer cycle. |
| `aura.page.alternating2`, `aura.page.alternating3` | `aura:pump_variants` recipe index | Image/caption labels only; all registered alternating pump recipes are shown. |
| `aura.page.flux0`, `aura.page.flux1` | `aura:arcane_progression` / Export Stored Power | Mapped to face-connected energy receivers, four-receiver limit, pause above four, and conversion cost. No named third-party transport compatibility is claimed. |
| `aura.page.flux2` | No gameplay page | Copied-bullet joke, omitted as flavor. |
| `aura.page.capacitor0` | `aura:node_variants` / Capacitor, Set a Threshold | Mapped to threshold cycling, burst, and cooldown. |
| `aura.page.capacitor1` | No gameplay page | Multiblock joke, omitted as flavor. |
| `aura.page.conserve0` | `aura:node_variants` / Conserving Node | Mapped to same-height-only transfer. |
| `aura.page.conserve1` | No gameplay page | Direction joke, omitted as flavor. |
| `aura.page.manipulator0` | `aura:node_variants` / Manipulators | Mapped to powered own-color refill, unpowered drain, and retained links/power. |
| `aura.page.manipulator1`, `aura.page.manipulator2` | `aura:node_variants` recipe cards | Item-name captions; both registered Manipulator recipes appear. |
| `aura.page.pumpFall0` | `aura:pump_variants` / Momentum, Light and Fall Offers | Mapped to fall fuel, added seconds, and nominal speed. |
| `aura.page.pumpFall1` | No gameplay page | Sound-effect joke, omitted as flavor. |
| `aura.page.pumpLight0` | `aura:pump_variants` / Illumination, Light and Fall Offers | Mapped to adjacent Glowstone/Torch consumption and offers. |
| `aura.page.pumpLight1` | No gameplay page | Light pun, omitted as flavor. |
| `aura.page.pumpRedstone0` | `aura:pump_variants` / Redstone, Projectile and Wire Offers | Mapped to contiguous powered wire consumption and current duration scaling. |
| `aura.page.pumpRedstone1` | No gameplay page | Caption/pun, omitted as flavor. |
| `aura.page.pumpProjectile0` | `aura:pump_variants` / Projectiles, Projectile and Wire Offers | Mapped to Arrow, Egg, and Snowball offers. |
| `aura.page.pumpProjectile1` | No gameplay page | Sound-effect joke, omitted as flavor. |
| `aura.page.consumers0` | `aura:consumers_vortex` / Work Rhythm | Mapped to twice-per-second work, per-update cost doubling, reset, and quarter-cycle remainder. |
| `aura.page.crafting0`, `aura.page.crafting1` | `aura:consumers_vortex` / Place a Vortex, Falling Receipts, Complete the Infusion | Mapped to four adjacent pedestals, interchangeable components, color-specific receipts, and completion. |
| `aura.page.crafting2`, `aura.page.crafting3` | `aura:consumers_vortex`, `aura:arcane_progression` recipe cards | Controller/Pedestal labels; current registered recipes and placement instructions are shown. |
| `aura.page.crafting4`, `aura.page.crafting5`, `aura.page.crafting6`, `aura.page.crafting7`, `aura.page.crafting8`, `aura.page.crafting9`, `aura.page.crafting10`, `aura.page.crafting11`, `aura.page.crafting12`, `aura.page.crafting13`, `aura.page.crafting14`, `aura.page.crafting15` | `aura:consumers_vortex`, `aura:arcane_progression` | These legacy pages contain item-name labels, not recipes or procedure text. The modern entries show only the currently registered supported Vortex and material routes; the old gallery is not implied to be reproduced. |
| `aura.page.dye0` | `aura:consumer_fieldwork` / Cascading Colorer | Mapped to the sheep work area, fleece restoration, and one of 16 dye colors per completion. |
| `aura.page.dye1` | No gameplay page | Rainbow slogan, omitted as flavor. |
| `aura.page.angel0` | `aura:consumers_vortex` / Synthesizer, `aura:arcane_progression` | Mapped to the no-input first-tier ingot output and current work cost. |
| `aura.page.angel1` | No gameplay page | Thin-air joke, omitted as flavor. |
| `aura.page.furnace0` | `aura:consumer_fieldwork` / Cascading Smelter | Mapped to item placement and vanilla furnace result; current dropped inputs keep their ordinary item lifetime. |
| `aura.page.furnace1` | No gameplay page | Heat joke, omitted as flavor. |
| `aura.page.fish0` | `aura:consumer_fieldwork` / Cascading Fisher | Mapped to the current 4-by-4 water footprint and gameplay fishing table. |
| `aura.page.fish1` | No gameplay page | Fishing joke, omitted as flavor. |
| `aura.page.loot0` | `aura:late_systems` / Looter and Spawner | Mapped to current dungeon-table item output and stored-power steps. |
| `aura.page.loot1` | No gameplay page | Rupee joke, omitted as flavor. |
| `aura.page.mob0` | `aura:late_systems` / Looter and Spawner | Mapped to the local biome/structure natural monster list and current work cost. |
| `aura.page.mob1` | No gameplay page | Mob-count joke, omitted as flavor. |
| `aura.page.plant0` | `aura:consumer_fieldwork` / Cascading Grower | Mapped to current target position and random-tick operation; no middle Dirt block is required by the port. |
| `aura.page.plant1` | No gameplay page | Fertilizer joke, omitted as flavor. |
| `aura.page.books0` | `aura:storage_network` / Books and Shelves | Mapped to Storage Book use and bookshelf conversion. |
| `aura.page.books1` | `aura:storage_network` / Power and Connection | Mapped to connected shelf chains, clear coordinator sight lines, and opening the browser. |
| `aura.page.books2` | `aura:storage_network` / Power and Connection | Mapped to the current shelf-count power draw and examples. |
| `aura.page.books3` | No gameplay page | Brain caption, omitted as flavor. |
| `aura.page.books4`, `aura.page.books5`, `aura.page.books10`, `aura.page.books15`, `aura.page.books16` | `aura:storage_network` / Book Limits I, Book Limits II | Mapped to Basic, Dense, Light, and specialized limits/filters. |
| `aura.page.books6`, `aura.page.books7`, `aura.page.books8`, `aura.page.books9`, `aura.page.books11`, `aura.page.books12`, `aura.page.books13`, `aura.page.books14` | `aura:storage_network` recipe cards | Storage Book item labels; modern cards show each registered tier recipe. |
| `aura.page.books17`, `aura.page.books18`, `aura.page.books19`, `aura.page.books20` | `aura:storage_network` / Book Limits II | Mapped to current specialized limits; Mineral and Extremely Dense values differ from the legacy captions, as listed below. |
| `aura.page.miner0`, `aura.page.miner1` | `aura:late_systems` / Miner's Charge | Mapped to singularity charging, HUD charge, redstone release, and charge-dependent yield. |
| `aura.page.miner2`, `aura.page.miner3`, `aura.page.miner4` | `aura:late_systems` / Contain the Miner | Mapped to containment risk, all six Fortified forms, and current Processor ingredients. |
| `aura.page.miner5` | No gameplay page | Extra-ore joke, omitted as flavor. |
| `aura.page.nether0` | `aura:consumers_vortex`, `aura:late_systems` / World Rituals | Mapped to the four-Red-Gem recipe, power requirement, world conversion, and self-removal. |
| `aura.page.nether1` | No gameplay page | Fire joke, omitted as flavor. |
| `aura.page.end0` | `aura:late_systems` / World Rituals | Mapped to current End conversion, water behavior, power, and completion. |
| `aura.page.end1` | No gameplay page | End pun, omitted as flavor. |
| `aura.page.brewer0` | `aura:consumer_fieldwork` / Cascading Brewer | Mapped to water-potion input, ordered brew stages, and bottle advancement. |
| `aura.page.brewer1` | No gameplay page | Warning joke, omitted as flavor. |
| `aura.page.amuletAngel0` | `aura:accessory_loadout`, `aura:accessory_amulets` / Activate the Wing | Mapped to Amulet slot and current V-key ascent/descent control. |
| `aura.page.amuletAngel1` | No gameplay page | Film quote, omitted as flavor. |
| `aura.page.beltAngel0` | `aura:accessory_loadout`, `aura:accessory_belt` / Step Up | Mapped to Belt slot, clear two-block step, and obstacle lift. |
| `aura.page.beltAngel1` | No gameplay page | Mountain joke, omitted as flavor. |
| `aura.page.prismaticWand0`, `aura.page.prismaticWand1` | `aura:prismatic_wand` | Mapped to mode selection, two-corner live-source copy, player-relative paste, safety limits, and survival cost. The slogan is nonmechanical. |
| `aura.page.mirror0` | `aura:gear_utility` / Mirror of the Angel | Mapped to right-click use, striking a nearby fireball, and the current Ghast/Blaze target requirement. |
| `aura.page.mirror1` | No gameplay page | User quote, omitted as flavor. |
| `aura.page.angelsteel0` | `aura:arcane_progression` / Cascading Synthesizer, Angelsteel Tiers | Mapped to first-tier output and three-to-one tier advancement. |
| `aura.page.angelsteel1`, `aura.page.angelsteel2` | `aura:angelsteel_tools` / Tier Bonuses, Mining Gifts | Mapped to faster tiers, two random bonus points, and all four mining gifts. |
| `aura.page.angelsteel4`, `aura.page.angelsteel5`, `aura.page.angelsteel6` | No gameplay page | Three puns/quotes, omitted as flavor. |
| `aura.page.angelsteelSword0`, `aura.page.angelsteelSword1` | `aura:angelsteel_swords` / Six Curses | Mapped to tier damage, curse duration, and all six color effects. |
| `aura.page.angelsteelSword2`, `aura.page.angelsteelSword3`, `aura.page.angelsteelSword4`, `aura.page.angelsteelSword5`, `aura.page.angelsteelSword6`, `aura.page.angelsteelSword7` | `aura:angelsteel_swords` / Six Curses | Each color's curse is explicitly summarized; the guide does not invent old numeric cadence or duration. |
| `aura.page.protection0`, `aura.page.protection1` | `aura:accessory_amulets` / Protection Colors | Mapped to the six current damage families and healing exceptions. |
| `aura.page.protection2`, `aura.page.protection3`, `aura.page.protection4`, `aura.page.protection5`, `aura.page.protection6`, `aura.page.protection7` | No gameplay page | Six hazard jokes/captions, omitted as flavor. |
| `aura.page.swordTransmute0`, `aura.page.swordTransmute1` | `aura:gear_utility` / Transmuting Sword | Mapped to all six supported living-entity pairs and preserved name/health. |
| `aura.page.swordTransmute2` | No gameplay page | Film parody, omitted as flavor. |
| `aura.page.swordCombo0` | `aura:gear_utility` / Fury's Rhythm | Mapped to current hit window, five-percent compounding, and 100-link cap. |
| `aura.page.swordCombo1` | No gameplay page | Combo joke, omitted as flavor. |
| `aura.page.explosionRing0` | `aura:late_systems` / Ring of the Shattered Stone | Mapped to three-block per-axis range, spared nonterrain blocks, destructible terrain, and no wearer protection. |
| `aura.page.explosionRing1` | No gameplay page | Hissing sound effect, omitted as flavor. |
| `aura.page.redHole0` | `aura:gear_utility` / A Dropped Red Hole | Mapped to non-fiery explosions about every five seconds and current dropped-item lifetime. |
| `aura.page.redHole1` | No gameplay page | Warning joke, omitted as flavor. |
| `aura.page.blackHole0` | `aura:gear_utility` / Black Hole | Mapped to five-second carried-Cobblestone consumption. |
| `aura.page.blackHole1` | No gameplay page | Retro-game joke, omitted as flavor. |
| `aura.page.magicRoad0` | `aura:travelers_bricks` / A Moving Boost | Mapped to the full-vector movement boost and block contact. |
| `aura.page.magicRoad1` | No gameplay page | Road pun, omitted as flavor. |
| `aura.page.trampoline0` | `aura:rebounding_enigma` / The Next Leap | Mapped to upward launch, preserved horizontal motion, and no fall protection. |
| `aura.page.trampoline1` | No gameplay page | Trampoline joke, omitted as flavor. |
| `aura.page.swordThief0` | `aura:gear_utility` / Thief Sword | Mapped to Villager death, one-in-four chance, and first trade offer. |
| `aura.page.swordThief1` | No gameplay page | Crime-game joke, omitted as flavor. |
| `aura.page.amuletFood0` | `aura:accessory_amulets` / Forbidden Fruit | Mapped to completed eating or drinking gifts, including potions, milk, and Creative use. |
| `aura.page.amuletFood1` | No gameplay page | Apple joke, omitted as flavor; the apple's current regeneration is documented with the effect. |
| `aura.page.enchanter0` | `aura:kaleidoscopic_enchantments`, `aura:enchant_matrix` | Mapped to single-color effects and all fifteen color pairs. |
| `aura.page.enchanter1` | No gameplay page | Mana joke, omitted as flavor. |
| `aura.page.enchanter2` | `aura:consumers_vortex` / Enchanter Inputs | Mapped to supported tool/item and color Aura Crystal input, including the crystal-absorption warning. The old ingot input is not taught. |
| `aura.page.enchanter3`, `aura.page.enchanter4` | `aura:kaleidoscopic_enchantments` / Chance to Bind | Mapped to compounding success chance, level-four penalty, and crystal consumption on failure. |
| `aura.page.basicEffects0`, `aura.page.basicEffects1`, `aura.page.basicEffects2`, `aura.page.basicEffects3`, `aura.page.basicEffects4`, `aura.page.basicEffects5` | `aura:kaleidoscopic_enchantments` | Each single-color effect is described in current port terms. |
| `aura.page.areaOfEffect0`, `aura.page.areaOfEffect1` | `aura:enchant_matrix` / Pairings I | Mapped to the Yellow/Green connected-growth action and Blue/Violet target-centered damage. |
| `aura.page.combat0`, `aura.page.combat1`, `aura.page.combat2` | `aura:enchant_matrix` / Pairings II, Pairings IV | Mapped to healing, ignition, splash fire, and current incoming/outgoing damage pairs. |
| `aura.page.looting0`, `aura.page.looting1` | `aura:enchant_matrix` / Pairings III, Boundaries | Mapped to loot-table bonus rolls and supported ore-to-ingot pairs. |
| `aura.page.negative0`, `aura.page.negative1`, `aura.page.negative2` | `aura:enchant_matrix` / Pairings IV | Mapped to mining slowdown, recoil, and outgoing-damage reduction. |
| `aura.page.speedBoosts0`, `aura.page.speedBoosts1`, `aura.page.speedBoosts2`, `aura.page.speedBoosts3`, `aura.page.speedBoosts4` | `aura:enchant_matrix` / Mining Pairings, Boundaries | Mapped to every Orange mining family and its current hardness/tool gates. |
| `aura.page.fairies0`, `aura.page.fairies1` | `aura:fairy_roles` / Bind a Companion | Mapped to equipped Ring, typed charm use, fifteen-slot limit, and sneak-use release. |
| `aura.page.fairies2` | No gameplay page | Superhero slogan, omitted as flavor. |
| `aura.page.fairies3` | `aura:fairy_roles` / Bind a Companion | Old instruction says right-click the Ring to summon; current typed-charm binding and release controls are documented instead. |
| `aura.page.fairyCombat0` | `aura:fairy_combat_charms` / Charm Recipes | Mapped to Fighter's nearby-hostile strike. |
| `aura.page.fairyCombat1` | No gameplay page | Fighter quote, omitted as flavor. |
| `aura.page.fairyDebuff0` | `aura:fairy_combat_charms` / Hexes and Gifts | Mapped to all six maladies and their duration. |
| `aura.page.fairyDebuff1` | No gameplay page | Debuffer quote, omitted as flavor. |
| `aura.page.fairyBuff0` | `aura:fairy_combat_charms` / Hexes and Gifts | Mapped to six possible gifts and their cadence. |
| `aura.page.fairyBuff1` | No gameplay page | Medic quote, omitted as flavor. |
| `aura.page.fairySteal0` | `aura:fairy_work_charms` / Their Small Services | Mapped to taking a nearby player's held item to the owner's feet; the port only clears the hand after successful insertion. |
| `aura.page.fairySteal1` | No gameplay page | Stealer quote, omitted as flavor. |
| `aura.page.fairyPush0` | `aura:fairy_combat_charms` / Shoves and Spawns | Mapped to non-damaging knockback and resistance. |
| `aura.page.fairyPush1` | No gameplay page | Pusher quote, omitted as flavor. |
| `aura.page.fairyShoot0` | `aura:fairy_combat_charms` / Shoves and Spawns | Mapped to the nearby-arrow damage and critical boost. |
| `aura.page.fairyShoot1` | No gameplay page | Shooter quote, omitted as flavor. |
| `aura.page.fairySavior0` | `aura:fairy_combat_charms` / Charm Recipes | Mapped to the low-health hostile strike. |
| `aura.page.fairySavior1` | No gameplay page | Savior quote, omitted as flavor. |
| `aura.page.fairyFetch0` | `aura:fairy_work_charms` / Their Small Services | Mapped to dropped-item pickup and return. |
| `aura.page.fairyFetch1` | No gameplay page | Fetcher quote, omitted as flavor. |
| `aura.page.fairyBait0` | `aura:fairy_work_charms` / Their Small Services | Mapped to passive-animal types and current spawn chance. |
| `aura.page.fairyBait1` | No gameplay page | Baiter quote, omitted as flavor. |
| `aura.page.fairyBreed0` | `aura:fairy_work_charms` / Ready Hands | Mapped to ready adult animals and the breeding action. |
| `aura.page.fairyBreed1` | No gameplay page | Breeder quote, omitted as flavor. |
| `aura.page.fairyScare0` | `aura:fairy_combat_charms` / Shoves and Spawns | Mapped to reduced arrivals at vanilla natural-spawn points. |
| `aura.page.fairyScare1` | No gameplay page | Scarer quote, omitted as flavor. |
| `aura.page.fairyExtinguisher0` | `aura:fairy_world_charms` / Charm Recipes | Mapped to clearing the owner's fire and putting out fire at the fairy. Nearby lava removal is not present in the port; see source conflicts. |
| `aura.page.fairyExtinguisher1` | No gameplay page | Extinguisher quote, omitted as flavor. |
| `aura.page.fairyDigger0` | `aura:fairy_work_charms` / Ready Hands | Mapped to the worn Ring and compounding break-speed steps. |
| `aura.page.fairyDigger1` | No gameplay page | Digger quote, omitted as flavor. |
| `aura.page.fairyLight0` | `aura:fairy_world_charms` / A Lighter's Glow | Mapped to temporary hidden light in a dark empty location. |
| `aura.page.fairyLight1` | No gameplay page | Lighter quote, omitted as flavor. |
| `aura.page.fairyFall0` | `aura:fairy_travel_charms` / Glider's Grace | Mapped to repeated halving of fall distance and remaining danger. |
| `aura.page.fairyFall1` | No gameplay page | Glider quote, omitted as flavor. |
| `aura.page.fairyTrain0` | `aura:fairy_work_charms` / Their Small Services | Mapped to five-XP drops and current roll chance. |
| `aura.page.fairyTrain1` | No gameplay page | Trainer quote, omitted as flavor. |
| `aura.page.video0`, `aura.page.video1` | `aura:getting_started` | External videos are version-bound references, not mechanics. Their promo copy and outbound links are intentionally not embedded. |
| `aura.page.patreon0` | No gameplay page | Author-support request; intentionally omitted from the mechanics guide. |
| `aura.page.00questDesc` | `aura:quest_progression` / Crystals | Mapped to the crystal goal/reward and White Crystal/Progression links. |
| `aura.page.01questDesc` | `aura:quest_progression` / Nodes | Mapped to the Node goal/reward and Node Variants. |
| `aura.page.02questDesc` | `aura:quest_progression` / Pumps | Mapped to the Pump goal/reward and pump offerings. |
| `aura.page.03questDesc` | `aura:quest_progression` / Furnace | Mapped to the Smelter goal/reward and direct Consumer Workflows link. |
| `aura.page.04questDesc` | `aura:quest_progression` / Red Crystals | Mapped to the Red Crystal goal/reward and Red Aura behavior. |
| `aura.page.05questDesc` | `aura:quest_progression` / Processor | Mapped to the Processor goal/reward and its recipes. |
| `aura.page.06questDesc` | `aura:quest_progression` / Dye | Mapped to the Colorer goal/reward and direct Consumer Workflows link. |
| `aura.page.07questDesc` | `aura:quest_progression` / Arcane Ingot | Mapped to the White Ingot goal/reward and all eight colors. |
| `aura.page.08questDesc` | `aura:quest_progression` / Vortex Infusion | Mapped to the Controller goal/reward and cardinal layout/receipts. |
| `aura.page.09questDesc` | `aura:quest_progression` / Arcane Gem | Mapped to the White Gem goal/reward and matching-color recipe. |
| `aura.page.10questDesc` | `aura:quest_progression` / Arcane Prism | Mapped to the Prism goal/reward and eight-distinct-gem Processor recipe. |
| `aura.page.11questDesc` | `aura:quest_progression` / Synthesizer | Mapped to the no-input Angelsteel output. |
| `aura.page.12questDesc` | `aura:quest_progression` / Angelsteel | Mapped to the tier-two goal, tier-one reward, tools, and swords. |
| `aura.page.13questDesc` | `aura:quest_progression` / Fairies | Mapped to the Ring goal/reward and typed-charm binding route. |
| `aura.page.tutorial0` | `aura:getting_started`, book categories and links | Navigation tutorial text is not a world mechanic; the actual book has direct entry routes. |

## Remaining Instruction Gaps

The 261 source keys map one-to-one to 261 unique destinations/omission
decisions. The two copy gaps identified at the eighth-build checkpoint are now
closed in the current guide resources:

- **Forbidden Fruit drinking:** the page now instructs finishing an eating or
  drinking use while equipped, including potions, milk, and Creative use.

The Angelsteel Fortune wording now describes its effect on eligible non-crop
drops without implying that the bonus fades or changing the held tool. No
source-confirmed player instruction gap remains from these two findings; other
source/port differences are listed separately below.

The following are separate concrete behavior differences or absent legacy
features, not unresolved guide copy; they are not silently represented as
current port behavior.

## Concrete Source Conflicts

- The legacy localized Burning Pump page says 200 Aura per second; the
  recovered 592 pump call/config contract and current implementation use 300.
  The guide uses 300. The localized Alternating Pump text says an eight-minute
  cycle; the current sine formula completes one rise-and-fall cycle in 20,000
  world ticks (16 minutes at 20 ticks per second).
- Legacy Extremely Dense books claim one million items; the current book holds
  100,000. Legacy Mineral books claim eight 10,000-item stacks; the current
  variant allows eight item types at up to 100,000 each. Current specialized
  filters use installed common tags, item paths, and the one-namespace Mod
  rule rather than a reproduced Forge classification registry.
- Legacy Processor text makes Arcane Ingots from Iron and colored Wool; the
  modern recipe uses Iron and a matching Aura Crystal. Legacy Prismatic
  Processor text says colored Wool becomes dye; no such modern Processor
  operation is registered. Three-dust conversion is present.
- Legacy Smelter text says nearby items are protected from despawning. The
  port does not extend dropped-item lifetime; current guide warns that inputs
  retain their ordinary lifetime.
- Legacy Grower text requires Dirt between the machine and plant. The modern
  operation random-ticks a target exactly two blocks above and has no middle-
  Dirt requirement.
- Legacy Fortified recipes specify End Stone plus a matching block; registered
  modern Processor recipes use Obsidian plus the matching base block.
- Legacy Fisher text says a 3-by-3 pool. The current supported footprint is
  4-by-4, including flowing water.
- Legacy Amulet of the Angel's Wing text names Up/Down controls, while the
  recovered legacy activation callback is a no-op. The port supplies a modern
  Activate Wing key (V by default), with looking downward selecting descent.
- Legacy Forbidden Fruit copy allows either beneficial or harmful gifts. The
  current deterministic per-item mapping selects beneficial effects only;
  apples grant regeneration. Current source accepts completed EAT and DRINK
  uses, including potions, milk, and creative completion, now reflected in the
  guide.
- Legacy Enchanter instructions use colored Arcane Ingots. Current supported
  Enchanter input is a color Aura Crystal with a supported item; the crystal
  is consumed even on a failed roll.
- Legacy explosion-ring text gives a four-block range. Current source contract
  uses three blocks on each axis and confirms it does not protect the wearer.
- Legacy Miner fortification was described around a destructive moving entity
  and a Forge ore dictionary. The port uses current Fortified blocks and
  current tagged ores; recipe inputs and the charge/pulse instructions in the
  guide follow the modern resources and implementation.
- Legacy Looter/Spawner draw on Forge chest hooks and world spawn entries.
  Current behavior uses the built-in dungeon chest loot table and the current
  biome/structure natural monster list.
- Legacy Nether/End prose does not specify the current radius and mapped
  families. Modern rituals process an approximately 150-block radius and
  remove themselves at queue completion. End water remains unchanged because
  the optional legacy Ender fluid is absent.
- Legacy Extinguisher text promises nearby lava removal. The modern Fairy
  clears the owner's fire and extinguishes fire at its feet; it does not
  remove lava. Modded fire-material compatibility also remains limited to the
  source-backed support recorded in `docs/audits/2026-09-23-fairy-restoration-contract.md`.
- Legacy Sword of the Barbarian copy says seven-percent damage steps in a
  five-second window. The current implementation compounds five percent for
  hits 5 to 99 ticks apart, capped at 100 links; the guide follows the modern
  source contract.
- Legacy Red Hole copy says five-times-longer dropped lifetime. The port sets
  its dropped marker lifetime to 30,000 ticks; the guide states the current
  approximate world time instead of repeating the old multiplier.
- **Dated source correction (2026-09-23, Source 16):** an earlier mapping in
  this audit called the eruptions fiery. The original explosion is non-fiery
  (`fire=false`); the guide now says “explodes.” The port's source-16 path is
  per-item ticking and skips vanilla item ticks through ages 0-99 per 100-tick
  blast cycle. Source 16 is green at 244 tests/60 suites, and its two-cycle live
  test passed with the 32-block witness unchanged and no extra explosion during
  the one-second freeze control. Save/reload and exact-candidate packaged
  client/multiplayer checks remain pending; see the [current acceptance
  ledger](2026-09-22-video-acceptance.md#finite-current-beta-gates-2026-09-23).
- Legacy fairy instructions describe Baubles-slot controls and older
  summon/release interactions. The modern physical four-slot menu and
  typed-charm binding controls are documented in `aura:accessory_loadout` and
  `aura:fairy_roles`.

These items are source/port differences, not claims that the current guide
should teach obsolete recipes or unavailable actions. The modern book also
does not reproduce every historical Vortex item-label gallery page as prose;
only currently registered recipes are presented as recipe instructions.

## Live QA Boundary (Thirteenth-Candidate Snapshot)

The parent reports thirteenth candidate `306ab942…` passed 227 tests across 57
suites. Packaged observer v2 run
`build/qa-audit/observer/runs/20260923-093128-934/guide-layout.json` passed
with 41 entries, 371 pages, and 14 quest pages measured: 0 visible-text
overflows, 0 title overflows, and 151 whitespace-only advances. This is a
real-font layout-bound result; it does not claim every page was manually opened
or visually clicked. No Gradle, Java, client, or server task was run for this
documentation update. Cross-entry navigation, recipe presentation, and the
remaining runtime checks are separate from the font/layout pass. The 14-quest
render/persistence and storage/accessory playtest evidence remains limited to
the slices described in their audit records. Current acceptance status is
maintained in the [canonical ledger](2026-09-22-video-acceptance.md).
