# Core late-game independent live playtest

Date: 2026-09-23, approximately04:04..04:08 local. Bounded continuation of the
parent's seventh198-test integrated build, existing client PID29072. No build,
Java launch, restart, source modification, commit, world exit or client shutdown.
Parent's other machine/progression results are not independently recertified by
this report. In particular, parent captures804 are not used as visual proof.

## Runtime and evidence

- PID29072 independently matched the listener127.0.0.1:9876. Same process as the
  preceding seventh-client audit, with parent-provided198-test build identity.
  This is a dev-classpath runtime; a new packaged-JAR hash was not measured.
- Bridge confirmed world `build/qa-audit/core-systems/saves/core-systems-cac43cda`,
  Player918. Actual player mode CREATIVE, health20/food20. World metadata says
  SURVIVAL; that does not override actual player mode. Config remained
  questline=false; no config edit was made.
- Read the [source-verified acceptance plan](2026-09-23-e1-late-game-live-acceptance.md)
  and parent's machine-checks helper before operating. Height changed80->180.
  Every teleport destination had a solid standing platform; tick freeze does
  not freeze player gravity.
- [Executed helper](../../build/qa-audit/node-pilot-playtest/core-late-checks.ps1),
  [sanitized action records](../../build/qa-audit/node-pilot-playtest/actions.jsonl),
  and850..858 screenshots are ignored local evidence. Actual server NBT and
  predicate chat appear in
  [stdout](../../build/qa-audit/integrated-restoration-client-seventh/stdout.log).
  Screens cited here were independently viewed, not accepted by filename.
- `execute if/unless` success returned command_result1. Failed predicates
  returned a command-syntax error, NOT0; the helper stops on errors. Acceptance
  uses actual item/entity/block/biome predicates and NBT, not input acknowledgements.

## Results

| Bounded check | Result |
| --- | --- |
| E1 powered cycle without crystal | PASS: plain pickaxe unchanged, no enchantment; supply spent and progress reset. |
| Miner unpowered control | PASS:25 stepped ticks, progress1/charge0, no helper. |
| Miner charging/motion/containment | PASS: real helper at charge1; same UUID at3 samples with different positions/velocities; all25 fortified blocks and viewing-platform sentinel survived. |
| Miner release at charge1 | PASS: MINER_RELEASED, charge0, progress0, no helper, no ore drops, second2500 supply spent. |
| Nether unpowered control | PASS: progress100, sample stone/log and source/outside biome probes unchanged after25 ticks. |
| Nether conversion | PASS at bounded block/center-biome predicates, outside control and self-removal. Original plan's lower biome point FAIL; see explicit discrepancy below. |
| End unpowered control | PASS: same negative, also verified center biome plains before power. |
| End conversion | PASS at bounded block/center-biome predicates, outside control and self-removal. Original lower point still plains, not End; same discrepancy preserved. |

These are accelerated functional checks, NOT complete survival cycles. Commands
placed fixtures, seeded near-threshold progress and injected power into adjacent
nodes only. Consumer stored_power, output items, Miner charge/entity motion and
ritual output biomes/blocks were NOT injected as positive results.

## Power timing

Each injection followed an actual gametime query/alignment to phase19; three
ticks then reached phase2. This avoids the node's phase18 power retention step.

| Operation | Seeded progress | Node power | Injection gametime | Observed afterward |
| --- | ---: | ---: | ---: | --- |
| Enchanter no crystal | 1000 | 500 | 97139 | supply0, progress0, last_received_power500 |
| Miner first charge | 1 | 2500 | 97179 | supply0, charge1, progress0 |
| Miner release | 1 | 2500 | 97259 | supply0, charge0, progress0 |
| Nether ritual | 100 | 5000 | 97299 | supply0, ritual removed |
| End ritual | 100 | 5000 | 97339 | supply0, ritual removed |

## Enchanter no-crystal negative

Used a separate station at180,180,160 with node181,180,160, preserving the
parent's station160,180,160. Summoned exactly one disposable plain diamond
pickaxe at179.5,181.2,160.5, tagged `qa_late_no_crystal`, no gravity and maximum
pickup delay. No nearby red crystal; no crystal was supplied. After the actual
powered completion, item NBT remained exactly `{count:1,id:"minecraft:diamond_pickaxe"}`
and an `unless data ... Item.components."minecraft:enchantments"` probe passed.
Node power was0, progress0 and last_received_power500. Actual sample/log visible
in [851](../../build/qa-audit/node-pilot-playtest/851-enchanter-no-crystal.png).
Cleaned only that uniquely tagged disposable pickaxe afterward. Parent's
positive/unpowered enchanter tests were not rerun or claimed as ours.

## Miner motion and release

Fixture: Miner240,180,160; node241,180,160. Fortified-obsidian floor239..241,
y177,z159..161 and walls around the center cavity at y178..179:25 blocks total.
Viewer safely on separate stone platform246..252,y179,z157..163.

The25-tick no-power control retained progress1/charge0 with no helper. The
powered cycle spawned one actual helper and set MINER_CHARGING/charge1. Same
entity UUID across samples (NBT int array
`[-967702973,979455438,-1340263318,486401107]`):

| Sample | Position | Motion |
| --- | --- | --- |
| A, gametime97182 | 240.5,178.363329,160.5 | -0.046688,-0.003720,+0.089834 |
| B,97212 | 240.5,178.656973,160.5 | -0.003664,-0.100513,+0.058391 |
| C,97242 | 240.5,178.403067,160.5 | +0.060940,-0.042987,-0.039514 |

Samples B/C each followed30 actual stepped ticks. last_charged97180 and
last_explosion97181 persisted; the latter is the collision-check timestamp, not
proof of a destructive explosion. Actual movement plus changing bounce velocity,
surviving helper and25 successful fortified-block predicates establish the
bounded contained behavior. No claim of movement from mere existence.
[852](../../build/qa-audit/node-pilot-playtest/852-miner-motion-a.png) and
[853](../../build/qa-audit/node-pilot-playtest/853-miner-motion-b.png) show fixture
and NBT; the opaque cage does not visually expose the helper inside.

Set redstone239,180,160, reseeded progress1 and supplied another2500 at97259,
before the100-tick expiration deadline. At97262, real block NBT showed
MINER_RELEASED/charge0/progress0; no helper within8 blocks, no items within3 of
the output, node power0. [854](../../build/qa-audit/node-pilot-playtest/854-miner-released.png).
Zero ore is expected at charge1. No charge24/yield injection, destructive
uncontained explosion or full natural charge-cycle test was attempted.

## Ritual conversion and sampling discrepancy

Both runs used station200,180,200 and node201,180,200. Before each run, reset
the one-quart-high196..207,y180..183,z196..207 ring to desert and center
200..203,z200..203 to plains. Samples: stone202,180,201; oak log203,180,201;
outside stone204,180,201. The outside stone was not reset between runs. Viewer
stood on separate stone platform208..214,y179,z198..206.

For each run,25 unpowered ticks left progress100, stone/log unchanged and the
sample/source plains plus outside desert predicates passing. Then actual node
extraction/completion produced:

| Probe | Nether | End |
| --- | --- | --- |
| Inner stone202,180,201 | netherrack | end_stone |
| Inner log203,180,201 | glowstone | obsidian |
| Outside204,180,201 | stone/desert unchanged | stone/desert unchanged |
| Center biome202,182,202 | nether_wastes | the_end |
| Ritual200,180,200 | air | air |
| Supply201,180,200 | stored_power0 | stored_power0 |

Original-plan assertion `execute if biome 202 180 201 minecraft:nether_wastes`
FAILED with Test failed after successful block conversion. Direct follow-up at
that exact point succeeded for plains, not desert. At202,182,202 the target
nether_wastes predicate succeeded. After the End run, the same lower point again
reported plains, while the center reported the_end (and had explicitly reported
plains before the End power injection). This discrepancy is NOT erased or
treated as an input acknowledgement success.

Source `cellBiome` reads the chunk noise-biome quart and `setCellBiome` updates
that x/z quart through all vertical sections. Ordinary `/execute if biome`
samples a block-position biome. The differing sampling locations are a plausible
explanation, not a proven defect diagnosis; a direct palette-versus-command
sampling investigation was outside this bounded run. Therefore the exact lower
point requirement remains FAIL/unresolved, while the observed center conversion
and outside controls pass. No claim that every point in the4x4 sample square
returns the target biome through the command sampler.

[Nether negative855](../../build/qa-audit/node-pilot-playtest/855-nether-unpowered.png),
[Nether discrepancy856](../../build/qa-audit/node-pilot-playtest/856-nether-biome-discrepancy.png),
[End negative855](../../build/qa-audit/node-pilot-playtest/855-end-unpowered.png),
and [End converted856](../../build/qa-audit/node-pilot-playtest/856-end-converted.png)
were viewed. Some blocks are occluded in those angles, so their exact identities
are established by actual predicates, not inferred from distant silhouettes.
[857 side angle](../../build/qa-audit/node-pilot-playtest/857-end-samples-angle.png)
clearly shows end stone, obsidian and unchanged outside stone together.

The initial End helper call stopped because filling the already-existing viewing
platform returned No blocks were filled. Corrected the helper to assert/reuse
that platform and then ran End successfully. This was fixture setup handling,
not a ritual failure. The Nether failed predicate likewise stopped its helper;
remaining probes were subsequently issued individually and recorded.

## Handoff and limits

Returned exclusive UI ownership with world still loaded, tick freeze active,
control mode active, no menu, GUI3. Player171.5,180,160.5, yaw90/pitch0,
CREATIVE, health20/food20, supported by verified stone171,179,160.
[858 handoff](../../build/qa-audit/node-pilot-playtest/858-late-game-handoff.png)
was viewed. No Save/Quit or client exit was requested/performed in this lane.

Disposable stations remain for inspection: enchanter/node180/181,180,160 with
zero progress/power and tagged input removed; intact Miner cage with Miner
MINER_RELEASED/charge0, node power0 and redstone239,180,160 still present;
ritual origin now air, surviving node power0, End samples/center biome and
outside stone/desert as above. Viewer platforms remain. Final predicates found
no local Miner helper and no uniquely tagged no-crystal item. No unrelated
entities were killed and no parent fixture was removed.

Gametime advanced97133->97342 (209 stepped ticks); frozen elapsed wall time is
not simulated time. Other loaded machines/entities could advance during those
steps, so their unobserved activity is not this agent's acceptance evidence.

UNVERIFIED: full survival cost/progression and natural power generation; E1
player mining/drop, chain and attack callback controls; all enchantment pairs;
Miner natural ore yield, expiration/reload and uncontained explosion; ritual
same-target no-op,150-block extent and persistence after reload. Parent retains
all unrelated progression testing. No broad parity closure is claimed.
