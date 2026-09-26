# Legacy Quest Contract

## Source and behavior

The 14 goals and rewards below come from `AuraCascade-592.jar`, primarily
`pixlepix/auracascade/lexicon/LexiconData.class` and `QuestManager`. A goal is
an inventory-presence check (`hasItemStack`), not a consumed item or a count
requirement. Checks run when the Encyclopedia is opened; order does not gate
later goals. On the server, each newly satisfied goal drops its reward at the
player with pickup delay zero, reports completion, and is marked complete once.

Modern Patchouli pages use the coordinated advancement IDs in the final column.
`AuraQuestProgress` performs the server-side book-open check and one-time reward
grant; vanilla advancements persist and synchronize checklist state, which the
Patchouli `quest` pages display.

| # | Quest | Goal present in inventory | Reward | Advancement |
| ---: | --- | --- | --- | --- |
| 0 | Crystals | 1 White Aura Crystal | 64 White Aura Crystals | `aura:quest/crystals` |
| 1 | Nodes | 1 Aura Node | 16 Aura Nodes | `aura:quest/nodes` |
| 2 | Pumps | 1 Aura Node Pump | 64 Sticks | `aura:quest/pumps` |
| 3 | Furnace | 1 Cascading Smelter | 64 White Aura Crystals | `aura:quest/furnace` |
| 4 | Red Crystals | 1 Red Aura Crystal | 4 TNT | `aura:quest/red_crystals` |
| 5 | Processor | 1 Cascading Processor | 8 Coal | `aura:quest/processor` |
| 6 | Dye | 1 Cascading Colorer | 32 White Wool | `aura:quest/dye` |
| 7 | Arcane Ingot | 1 White Arcane Ingot | 15 White Arcane Ingots | `aura:quest/arcane_ingot` |
| 8 | Vortex Infusion | 1 Vortex Controller | 4 Vortex Pedestals | `aura:quest/vortex_infusion` |
| 9 | Arcane Gem | 1 White Arcane Gem | 8 White Arcane Gems | `aura:quest/arcane_gem` |
| 10 | Arcane Prism | 1 Arcane Prism | 1 Arcane Prism | `aura:quest/arcane_prism` |
| 11 | Synthesizer | 1 Cascading Synthesizer | 64 White Aura Crystals | `aura:quest/synthesizer` |
| 12 | Angel's Steel | `aura:angelsteel_ingot_2` | 15 `aura:angelsteel_ingot_1` | `aura:quest/angelsteel` |
| 13 | Fairies | 1 Ring of Binding | 1 Fairy Charm | `aura:quest/fairies` |

The three 64-crystal rewards come from `ItemAuraCrystal.getCrystalFromAuraMax`,
which constructs an ItemStack with count 64 and the requested Aura color; they
are ordinary White Aura Crystals, not a special full-charge crystal.

The Angelsteel goal/reward use the registered second- and first-tier item
variants respectively (legacy metadata 1 and 0).

The Nodes reward's construction uses `AuraBlock.getBlockFromName("")`. This is
not a missing-block case: the AuraBlock default constructor uses the empty name
for the ordinary Aura Node, and `getAuraNodeItemstack()` uses the same
identifier. The exact reward is therefore 16 normal Aura Nodes.

## Guide mapping

`aura:quest_progression` contains one Patchouli `patchouli:quest` page for each
advancement above. Each page states the inventory goal and reward and links to
the relevant instruction entry. Quest pages are a progression checklist, not a
replacement for machine instructions. The legacy introduction says the quest
track is not intended to teach the mod; those mechanics remain in the linked
guide entries.

The mod initializer must call `AuraConfig.bootstrap()` so the legacy questline
toggle is loaded before gameplay. This contract does not claim live acceptance.

## Questline Toggle

The shipped `QuestManager.check` grants no quest when the original
`Config.questline` option is disabled; its default is enabled. The modern port
keeps that behavior through `config/aura.properties`:

```properties
questline=true
```

The config loader creates this default when the file or key is absent, preserves
other properties, accepts only `true` or `false`, and warns while retaining the
enabled default for malformed values or unreadable config. When disabled,
opening Encyclopedia Aura does not award the root or any quest advancement or
reward.
