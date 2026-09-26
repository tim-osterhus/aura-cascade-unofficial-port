# E1 and late-game live acceptance (NOT RUN)

Historical fixture plan: the later 592 bytecode check corrected the Enchanter
catalyst to a supported-color **Arcane Ingot**, not Aura Crystal. Do not execute
the crystal-based positive below against the corrected build. Use the restored
recipe fixture and [conflict disposition](2026-09-23-guide-conflict-disposition.md).
Actual seventh-build machine and later tenth-snapshot mining results are recorded
separately in the acceptance ledger and `2026-09-23-node-mining-runtime.md`;
the NOT RUN table below describes this original plan, not current overall status.

This is a disposable-world acceptance recipe, not a PASS record. Use the reviewed
client/mod build in a cheats-enabled singleplayer fixture. The local bridge's
`executeCommand(command)` returns `command_result` (Brigadier result), not the
chat feedback from `/data get`; use the `execute if ...` probes below as 1/0
assertions. Send each line as a separate command, without the leading `/` if
the bridge already strips it. Record mod JAR hash, fixture name, commands,
results, screenshots, and any exception. Do not run the miner or rituals in a
valuable world. These commands presume the tested player has operator rights.

## Powered-machine fixture

Use a clear area near `(160,80,160)` in the Overworld; keep the player away from
dropped outputs. The node at `(161,80,160)` is the **power supply**, not the
consumer. Injecting its public saved `node_state.stored_power` accelerates the
test while the actual server consumer still extracts, spends and advances power.
Likewise `progress` on the consumer is a disclosed near-threshold accelerator;
it does not prove the full machine cycle or natural aura generation.
For each row, place the indicated machine at `(160,80,160)`, wait two ticks,
set its `progress` and node power, then wait about one second before probing.
Re-place the machine and node between rows to clear old state and entities.

```mcfunction
gamemode creative @s
difficulty normal
fill 158 78 158 164 78 164 minecraft:stone
fill 159 79 159 162 83 162 minecraft:air
tp @s 160 79 158
setblock 161 80 160 aura:aura_node
execute if block 161 80 160 aura:aura_node
```

The following are common negative controls. `command_result=1` means the
predicate matched; 0 means it did not. The node must begin without power.
With `progress` at threshold and no supply, work must not occur after one
second. Test a positive only after its input/biome/cage is ready.

```mcfunction
execute if data block 161 80 160 {node_state:{stored_power:0}}
data merge block 160 80 160 {progress:1000}
execute if data block 160 80 160 {progress:1000}
```

Do not use the common `1000` progress value for late-game rows. The enum
constructor is `(powerPerProgress, maxProgress)`, and work triggers when
`progress > maxProgress` after spending one step:

| Machine | Power for final step | Seeded progress |
| --- | ---: | ---: |
| Enchanter | 500 | 1000 |
| Looter | 5000 | 100 |
| Spawner | 190 | 15 |
| Miner | 2500 | 1 |
| Nether/End ritual | 5000 | 100 |

`stored_power`, `progress`, `last_power`,
`last_result`, `miner_charge`, and `miner_entity` are the saved LateGameBlockEntity
fields; `node_state.stored_power` is the node's saved supply field. Never inject
consumer `stored_power` and call that a powered-machine test. The fast fixtures
usually consume the seeded node power in one tick, so compare before/after
predicates rather than trying to read a transient positive `last_power`.

## E1 enchanter and runtime hooks

**Machine, deterministic first enchant.** Place `aura:consumer_block_enchant`
at `(160,80,160)`. Its cost is 500 power per step and completion occurs after
`progress > 1000`. A plain tagged diamond pickaxe plus one red aura crystal are
eligible; the first enchant has success probability 1.0. Summoned items have
pickup delay and no gravity so they stay in the consumer's 3-block scan box.

```mcfunction
setblock 160 80 160 aura:consumer_block_enchant
summon minecraft:item 160.5 81.2 160.5 {Item:{id:"minecraft:diamond_pickaxe",count:1},PickupDelay:32767,NoGravity:1b}
summon minecraft:item 160.8 81.2 160.5 {Item:{id:"aura:aura_crystal_red",count:1},PickupDelay:32767,NoGravity:1b}
data merge block 160 80 160 {progress:1000}
execute if data block 160 80 160 {progress:1000}
data merge block 161 80 160 {node_state:{stored_power:500}}
```

After one second, require one red-enchanted pickaxe item, no red crystal item,
node power spent, and enchanter progress reset. Probe the item with the entity
selector's nested NBT predicate, and capture an in-world screenshot/tooltip to
cross-check component syntax:

```mcfunction
execute if entity @e[type=minecraft:item,x=160,y=81,z=160,distance=..3,nbt={Item:{components:{"minecraft:enchantments":{levels:{"aura:kaleidoscopic_red":1}}}}}]
execute if entity @e[type=minecraft:item,x=160,y=81,z=160,distance=..3,nbt={Item:{id:"aura:aura_crystal_red"}}]
execute if data block 161 80 160 {node_state:{stored_power:0}}
execute if data block 160 80 160 {progress:0}
```

Expected results: `1, 0, 1, 1`. If the item predicate's NBT shape differs in
this runtime, inspect the actual dropped item with `/data get entity` in chat;
do not mark the row passing from progress and crystal disappearance alone.
Negative control: repeat with the crystal absent; there must be no enchantment,
even though the current consumer can spend a completed cycle on no work. Repeat
with crystal present but no adjacent powered node; neither crystal nor pickaxe
may change. Do not infer a guaranteed result for a previously enchanted item:
`successRate = 0.75^totalLevel * 0.25^max(0,maxLevel-4)`.

**Player runtime, bounded representatives.** `/setblock` and `/kill` do not
invoke player mining or attack hooks. Use one real survival break/attack through
the bridge or player input. Give an enchanted tool with 1.21.1 item components;
check the held tooltip before acting. Run each in a cleared patch with dropped
items removed first. These probes cover the distinct break/drop, chain, damage,
and attack callback surfaces; they are not a claim that every color pair passed.

```mcfunction
give @s minecraft:diamond_pickaxe[minecraft:enchantments={levels:{"aura:kaleidoscopic_red":4,"aura:kaleidoscopic_yellow":4}}]
setblock 164 80 160 minecraft:iron_ore
gamemode survival @s
tp @s 162 79 159
```

Break that ore **as the player with the tool**, then require exactly two iron
ingots and no ore/raw-iron drop. Pair strength 4 guarantees the source's
`nextInt(4) < strength` conversion; repeat with a plain pickaxe as a negative
control and require normal raw-iron drops. Source: `KaleidoscopicOriginalEffects`
`conversionIngot`/`dropOrConvert` and `KaleidoscopicOreDropsMixin`.

```mcfunction
give @s minecraft:diamond_axe[minecraft:enchantments={levels:{"aura:kaleidoscopic_green":1}}]
fill 164 80 164 164 83 164 minecraft:oak_log
tp @s 162 79 162
```

Break the bottom log as the player. All four connected logs should be removed;
an adjacent different block should remain. With a plain axe, only the targeted
log should break. This checks the post-break chain and its 25-block level-1 cap,
not a command-driven `setblock` deletion. Source: `afterBreak`.

```mcfunction
give @s minecraft:diamond_sword[minecraft:enchantments={levels:{"aura:kaleidoscopic_blue":1,"aura:kaleidoscopic_violet":1,"aura:kaleidoscopic_yellow":1}}]
summon minecraft:zombie 165 80 160 {NoAI:1b,PersistenceRequired:1b}
summon minecraft:zombie 166 80 160 {NoAI:1b,PersistenceRequired:1b}
summon minecraft:zombie 173 80 160 {NoAI:1b,PersistenceRequired:1b}
tp @s 163 79 160
```

Attack the first zombie once. The nearby second zombie should take 1 splash
damage and ignite; the distant third should not. The primary should ignite and
be pushed. Repeat with a plain sword: no enchantment splash/fire. Use `/data get
entity` on the three zombies or visual/health screenshots for evidence; do not
infer damage from an entity merely disappearing. Source: `onAttack`, pair
strength `ceil(sqrt(a*b))`. A separate Violet-only hit should do +0.5
pre-mitigation damage, while a defending player with Red+Violet should receive
`0.9^pair` damage; these require measured health/combat fixtures and remain
unverified if not performed.

Return to creative and remove the spawned zombies before the late-game rows:

```mcfunction
gamemode creative @s
kill @e[type=minecraft:zombie,x=165,y=80,z=160,distance=..12]
```

## Late-game consumers

**Looter (dungeon table).** Re-place machine/node. With the looter at progress
100 and the node empty, no item should appear and progress should hold. Then
seed 5000 power. Within a second require one nonempty dropped stack above the
looter, `last_result=LOOTED`, progress 0, node power 0. The item's exact ID is
random; compare it against the active `minecraft:chests/simple_dungeon` loot
table, not a fixed invented list. Kill other dropped items before this row.

```mcfunction
setblock 160 80 160 aura:consumer_block_loot
kill @e[type=minecraft:item,x=160,y=81,z=160,distance=..4]
data merge block 160 80 160 {progress:100}
execute if data block 160 80 160 {progress:100}
execute unless entity @e[type=minecraft:item,x=160,y=81,z=160,distance=..2]
data merge block 161 80 160 {node_state:{stored_power:5000}}
execute if data block 160 80 160 {last_result:"LOOTED",progress:0}
execute if entity @e[type=minecraft:item,x=160,y=81,z=160,distance=..2]
```

**Spawner (biome natural monster pool).** Re-place machine/node, set the local
biome to plains, clear pre-existing mobs, and keep an air column over the block.
With progress 15 and no power, there should be no new mob. Seed 190 power; a
real mob should spawn at y+2 and `last_result=SPAWNED`. Exact species is weighted
by the world's natural MONSTER list (including structure context), not a fixed
dimension pool. A biome with no natural monster entries should hold at 15 and
report `BLOCKED` on a powered attempt; verify the biome's actual spawn list
before treating that as a negative fixture.

```mcfunction
fillbiome 160 80 160 163 83 163 minecraft:plains
kill @e[type=!minecraft:player,x=160,y=81,z=160,distance=..4]
setblock 160 80 160 aura:consumer_block_spawn
data merge block 160 80 160 {progress:15}
execute if data block 160 80 160 {progress:15}
execute unless entity @e[type=!minecraft:player,type=!minecraft:item,x=160,y=82,z=160,distance=..2]
data merge block 161 80 160 {node_state:{stored_power:190}}
execute if data block 160 80 160 {last_result:"SPAWNED",progress:0}
execute if entity @e[type=!minecraft:player,type=!minecraft:item,x=160,y=82,z=160,distance=..2]
```

The last predicate must resolve to one newly created `Mob`; screenshot/type
inspection must confirm that it came from this run, not a pre-existing entity.

**Miner (moving helper, containment and release).** Use a separate disposable
station at `(240,80,160)` so collision cannot strike the other fixtures. Place
a 3x3 fortified-obsidian floor at y77 and walls at x239/241 and z159/161,
y78..79; leave the center cavity `(240,78..79,160)` empty. Place miner at y80,
node at `(241,80,160)`. First assert no entity with no power. The machine's
normal charge threshold is `progress > 1` at cost 2500; seed progress 1 and
2500 node power. Require one moving `aura:miner_explosion`, `miner_charge=1`,
`last_result=MINER_CHARGING`, and no large explosion after several seconds of
contact with fortified blocks. Capture screenshot or entity position/velocity
twice within the first four seconds; mere entity existence does not prove
movement/collision. The helper expires 100 ticks after its last charge, so run
the release probe before that deadline (or charge it again first).

```mcfunction
fill 239 77 159 241 77 161 aura:fortified_obsidian
fill 239 78 159 239 79 161 aura:fortified_obsidian
fill 241 78 159 241 79 161 aura:fortified_obsidian
fill 240 78 159 240 79 159 aura:fortified_obsidian
fill 240 78 161 240 79 161 aura:fortified_obsidian
fill 240 78 160 240 79 160 minecraft:air
setblock 240 80 160 aura:consumer_block_miner
setblock 241 80 160 aura:aura_node
data merge block 240 80 160 {progress:1}
execute unless entity @e[type=aura:miner_explosion,x=240,y=78,z=160,distance=..4]
execute if data block 240 80 160 {progress:1}
data merge block 241 80 160 {node_state:{stored_power:2500}}
execute if data block 240 80 160 {last_result:"MINER_CHARGING",miner_charge:1}
execute if entity @e[type=aura:miner_explosion,x=240,y=78,z=160,distance=..4]
```

For release, put a redstone block at `(239,80,160)`, reseed progress 1 and
node power 2500, then require `MINER_RELEASED` and no helper entity. Charge 1
correctly yields **zero** ore (`floor(charge^1.5/50)` only after charge 20).
An optional accelerated *yield-formula* probe may set the live helper entity's
`charge:24` with `/data merge entity`, then release and require two ore items;
label that as injected state, not evidence of 24 natural charge cycles. A fully
natural yield proof needs repeated powered cycles or automation. Uncontained
collision is destructive (strength-50 flaming explosion) and needs a separate
backed-up fixture; do not infer it from the contained row.

```mcfunction
setblock 239 80 160 minecraft:redstone_block
data merge block 240 80 160 {progress:1}
data merge block 241 80 160 {node_state:{stored_power:2500}}
execute if data block 240 80 160 {last_result:"MINER_RELEASED",miner_charge:0}
execute unless entity @e[type=aura:miner_explosion,x=240,y=78,z=160,distance=..4]
execute unless entity @e[type=minecraft:item,x=240,y=81,z=160,distance=..2]
```

**Nether and End rituals (isolated biome cell).** Run these serially at
`(200,80,200)` in a disposable copy. Before *each* run, set a 12x12, one-quart-
high desert ring and the central 4x4 plains cell; this limits the ritual BFS to
one cell despite its 150-block radius. Place sample stone and oak log inside
and another stone immediately outside. Put the relevant ritual at the cell
origin, set progress 100, confirm no change without power, then seed 5000 node
power at `(201,80,200)` **after** sample placement. The node is inside the
center cell and next to the ritual. Within several ticks the inner samples and
biome convert, outside stone and desert biome stay unchanged, and the ritual
block destroys itself. Nether: stone -> netherrack, log -> glowstone, biome ->
`minecraft:nether_wastes`. End: stone -> end_stone, log -> obsidian, biome ->
`minecraft:the_end`. Do not treat self-removal alone as success.

```mcfunction
fillbiome 196 80 196 207 83 207 minecraft:desert
fillbiome 200 80 200 203 83 203 minecraft:plains
setblock 202 80 201 minecraft:stone
setblock 203 80 201 minecraft:oak_log
setblock 204 80 201 minecraft:stone
setblock 200 80 200 aura:ritual_nether
setblock 201 80 200 aura:aura_node
data merge block 200 80 200 {progress:100}
execute if block 202 80 201 minecraft:stone
execute if data block 200 80 200 {progress:100}
data merge block 201 80 200 {node_state:{stored_power:5000}}
execute if block 202 80 201 minecraft:netherrack
execute if block 203 80 201 minecraft:glowstone
execute if block 204 80 201 minecraft:stone
execute if biome 202 80 201 minecraft:nether_wastes
execute if biome 204 80 201 minecraft:desert
execute if block 200 80 200 minecraft:air
```

Repeat the setup with `aura:ritual_end`, then assert `minecraft:end_stone`,
`minecraft:obsidian`, unchanged outside stone/desert, inner
`minecraft:the_end`, and removed ritual. Probe only after the queue finishes;
the block entity deliberately disappears, so `last_result=RITUAL_COMPLETE`
cannot be read from it afterward. Use a fresh fixture copy if Nether conversion
altered underlying terrain. A same-target-biome ritual is a separate no-op
control: source currently returns success with `last_result=NONE` and the block
remains; no conversion should be claimed.

```mcfunction
fillbiome 196 80 196 207 83 207 minecraft:desert
fillbiome 200 80 200 203 83 203 minecraft:plains
setblock 202 80 201 minecraft:stone
setblock 203 80 201 minecraft:oak_log
setblock 204 80 201 minecraft:stone
setblock 200 80 200 aura:ritual_end
setblock 201 80 200 aura:aura_node
data merge block 200 80 200 {progress:100}
execute if block 202 80 201 minecraft:stone
execute if data block 200 80 200 {progress:100}
data merge block 201 80 200 {node_state:{stored_power:5000}}
execute if block 202 80 201 minecraft:end_stone
execute if block 203 80 201 minecraft:obsidian
execute if block 204 80 201 minecraft:stone
execute if biome 202 80 201 minecraft:the_end
execute if biome 204 80 201 minecraft:desert
execute if block 200 80 200 minecraft:air
```

## Acceptance record

| Row | Result | Evidence required |
| --- | --- | --- |
| E1 enchanter powered + two negative controls | NOT RUN | Bridge predicates, target item component, crystal count |
| E1 player break/drop and plain-tool control | NOT RUN | Before/after block + exact item drops |
| E1 chain and plain-tool control | NOT RUN | Before/after connected and different blocks |
| E1 attack splash/fire and plain-sword control | NOT RUN | Victim health/fire/position, distant control |
| Looter powered + unpowered | NOT RUN | `LOOTED`, one dungeon-table stack, no unpowered item |
| Spawner powered + unpowered | NOT RUN | `SPAWNED`, newly created natural monster, no unpowered mob |
| Miner charge/motion/containment/release | NOT RUN | Entity motion, no explosion, `MINER_RELEASED`; natural yield separate |
| Nether ritual + outside-cell control | NOT RUN | Mapped blocks, inner/outside biomes, self-removal |
| End ritual + outside-cell control | NOT RUN | Mapped blocks, inner/outside biomes, self-removal |

Source anchors: `AuraConsumerBlockEntity`, `AuraConsumerLogic`,
`KaleidoscopicOriginalEffects`, `KaleidoscopicOreDropsMixin`,
`LateGameBlockEntity`, `LateGameWorldLogic`, and `MinerExplosionEntity`.
See `2026-09-23-kaleidoscopic-e1-contract.md` and
`2026-09-23-late-game-contract.md` for original-592 evidence and the remaining
adaptation limits. A short seeded fixture is live functional evidence, not full
survival progression or general parity closure.
