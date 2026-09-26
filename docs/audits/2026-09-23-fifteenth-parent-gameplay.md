# Fifteenth Parent Gameplay Checkpoint

2026-09-23, candidate SHA-256
`27502af9b5e763c16464fbccdac7381929326aa698569eb1c2debbeaa3811fb5`.
Parent-reported integration build passed 238 tests in 59 suites in 49 seconds.
This document records bounded gameplay evidence, not final release acceptance.

## Vortex Receipt Reload And Completion

The four Yellow-gem recipe inputs and their partial `power_received` values
survived the preceding save/reload. At cardinal pedestals around the controller,
the loaded receipts were North 89,230, East 73,435, South 99,650, and West
79,360; each pedestal still held its Yellow gem. This establishes persistence
of that partial recipe state, not recipe completion.

At game time 123,700, eight raw Yellow Aura Crystals were dropped at each of
the four upper nodes as tagged item entities. No receipt, output, or player
inventory result was seeded. After 40 ticks, receipts rose to North 91,600,
East 75,805, South 100,000, and West 81,730; raw crystal entities remained.
After another 120 ticks, all four receipts were zero, the four pedestals had no
held item, the tagged raw entities were gone, and one nearby
`aura:consumer_block_loot` output stack was present. The observed remaining
raw counts after the first interval were 1, 7, and 8 across the three
incompletely consumed inputs. The
[1517 screenshot](../../build/qa-audit/node-pilot-playtest/1517-vortex-yellow-output.png)
shows the setup; detailed state predicates are in the cumulative
[action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl).

## Nether Ritual Resume

At `(664,180,464)`, the reloaded ritual retained this exact ordered queue:

```text
[183618443739316, 182518932127924, 181419420483764,
 182518932078772, 183618443722932, 181419420467380]
```

The checkpoint had zero progress and zero stored/last power. After one stepped
tick and a further 20 ticks, all 400 blocks in the bounded conversion plane
matched the expected result, both outside stone witnesses remained unchanged,
the ritual had removed itself, and no dropped Nether ritual item was present.
The resume phase did not seed ritual power. A separate same-target paid-cycle
no-op check used 5,075,000 fixture-seeded power over 25 ticks; the existing
target remained and no queue/result change occurred. That no-op result is not
evidence of naturally supplied power. See
[resume log](../../build/qa-audit/node-pilot-playtest/fifteenth-ritual-resume.log),
[same-target log](../../build/qa-audit/node-pilot-playtest/fifteenth-ritual-same-target.log),
and their per-stage JSONL checkpoints in the same directory.

## Shattered Ring And Red Hole

The equipped Shattered Stone ring lane preserved both ore blocks while stone
was destroyed; the no-ring vanilla control destroyed its stone and both ores.
The physical ring was equipped in an accessory slot. See the
[equipped run](../../build/qa-audit/node-pilot-playtest/fifteenth-shattered-equipped.log)
and [control](../../build/qa-audit/node-pilot-playtest/fifteenth-shattered-unequipped.log).

Red Hole is **not accepted**. The first target at `(602,180,400)` was inside
the actual blast: an immediate stone check ran before the deferred tick outcome
and still saw stone, while cleanup moments later found air. This was outcome
timing, not a misplaced witness. The second attempt stopped on a PowerShell
numeric-literal parse error. On the third attempt, the near stones were
destroyed and the outside control remained stone, but the marker expected
after the second explosion was absent. The
[tick-contract audit](2026-09-23-red-hole-tick-contract.md) explains why game
time alone is not an end-of-tick completion fence and documents independent
source-contract errors; it does not trace the marker's exact removal event.
Sol is investigating that marker lifecycle, so the complete two-explosion
contract remains unaccepted. See the
[first](../../build/qa-audit/node-pilot-playtest/fifteenth-red-hole.log),
[retry](../../build/qa-audit/node-pilot-playtest/fifteenth-red-hole-retry.log),
and [final attempt](../../build/qa-audit/node-pilot-playtest/fifteenth-red-hole-final.log).

## Session Closure

The integrated client saved and quit normally, exit code 0, at 21:56:06 UTC.
All 22 parent-forced chunk tickets had been removed and the final force-load
query returned none. Player health was 20, on-ground was true, and fall
distance was 0 at remote position `2049.5,180,2049.5`. All three accessories were restored
through the actual ring-slot GUI interaction. The initial and final inventory
snapshots (11:46:49 and 11:53:54) match exactly: 1,260 characters each, with
all 22 occupied slots, counts, and components unchanged.

The preserved [runtime log](../../build/qa-audit/node-pilot-playtest/fifteenth-runtime.log)
contains the snapshot and clean shutdown. The
[client-session monitor](../../build/qa-audit/fifteenth-gameplay/summary.json)
reports peak working set 1,426.0 MiB and private bytes 1,916.3 MiB. The separate
[build monitor summary](../../build/qa-audit/integrated-restoration-fifteenth/summary.json)
reports peak working set 1,507.9 MiB and private bytes 1,626.5 MiB; its scope
excludes the desktop and agent hosts. The runtime is closed, but Red Hole
remains pending the marker investigation above.
