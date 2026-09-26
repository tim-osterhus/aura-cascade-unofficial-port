# Fourteenth Parent Gameplay Checkpoint

2026-09-23, integrated client world `core-systems-cac43cda`, Player918. This
records bounded gameplay observations from the fourteenth candidate, not a
full-port or release acceptance. Parent-reported build context is hash
`282c949d14073c207eb95095c3e622bd30e5b184ef4026b4b24f84f68a207490`, 234 tests
in 58 suites; this documentation pass ran no build or tests.

## Evidence Boundary

The private [action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl)
is cumulative across sessions; the test-specific fixture tags, game times and
the [fourteenth logs](../../build/qa-audit/node-pilot-playtest/) distinguish
these results. Orange/capacitor, projectile, combat, ore, accessory, vortex,
and ritual fixtures were supplied or seeded by QA. Successful behavior below
was driven by real player inputs and checked against game state, but it does
not establish natural acquisition, recipe completion (except where stated), or
unassisted resource generation.

## Bounded Passes

**Orange transfer and capacitor threshold: PASS on retry.** With 100,000 White
seeded in the mainline, 44,751 transferred east; an equal lateral White pair
did not move without Orange. With Orange seeded, 44,751 Orange transferred and
induced the same 44,751 White transfer through the parallel link. The default
1,000 capacitor check held 999 through the transfer window, held 1,000 while
redstone-powered, then emitted exactly 1,000 after power removal. Aura stores
and threshold inputs were fixture-seeded; the observed transfer was ticked
normally. The first fixture run stopped because a zero-valued `data merge`
made no change; [retry output](../../build/qa-audit/node-pilot-playtest/fourteenth-orange-capacitor-retry.log)
records the passing predicates. Six fixture nodes were left for inspection.

**Mirror of the Angel: PASS on retry.** The retry performed three actual held-
Mirror uses against command-summoned projectile/owner fixtures. A nearby
owner-fireball changed from Motion `(0,0,0)` to `(0.466666,0,0)`; the distant
Blaze-owned fireball stayed at `(0,0,0)`, and the recorded owner UUIDs stayed
unchanged. With no qualifying owner target, the candidate stayed at
`(0.125,0,-0.25)`. With a separate Blaze-owned fireball present, a Wither Skull
stayed at `(0.25,0,0.125)` and the distant target stayed at zero. The first
attempt's third case failed during fixture setup because it reused a frozen
entity UUID; this was a fixture collision, not a mod-behavior failure. The
[retry log](../../build/qa-audit/node-pilot-playtest/fourteenth-mirror-retry.log)
and [fixture helper](../../build/qa-audit/node-pilot-playtest/mirror-regression.ps1)
report all three cases passing. Projectiles and owners were seeded; the Mirror
uses were actual inputs.

**Barbarian sword: PASS, controlled melee sequence.** An inert, named Iron
Golem was spawned at 100 health with knockback resistance; the test sword was
fixture-issued. Actual attack inputs produced these samples:

| Game tick | Golem health | Sword combo |
| --- | ---: | ---: |
| 121824 | 93 | 0 |
| 121829 | 93 | 0 |
| 121839 | 86 | 1 |
| 121854 | 78.65 | 2 |

The Golem was the QA target, not a naturally encountered mob. State assertions
and attack inputs are in the [action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl)
around the `aura_qa_combat_barbarian` fixture; the integrated
[client log](../../build/qa-audit/fourteenth-gameplay/stdout.log) records the
matching health and held-item data.

**Transmuting Sword: PASS, three bounded cases.** A named 100-health Cow,
struck with the fixture-issued sword, became a 10-health Mooshroom with its
name preserved. A separate 1-health Cow died without a Mooshroom appearing.
An Iron Golem negative control took the hit to 93 health and retained its UUID.
All mobs and starting health values were fixture-supplied; outcomes came from
melee inputs and entity-state checks in the same action stream.

**Kaleidoscopic Red mining and Adventure veto: PASS.** A QA-seeded
`aura:angelsteel_pickaxe_1` carried Red I and existing `angelbuffs [0,3,0,0]`;
the test ore was a placed `minecraft:iron_ore`. Actual Survival mining removed
the block and produced one iron-ore item. The held tool still had the same
buffs and Red enchantment, with no vanilla Fortune or Silk Touch added. An
actual attack in Adventure left the ore in place, produced no item and left
tool damage unchanged; restoring Survival allowed a second actual break and
one iron-ore drop. This verifies the seeded tool/ore interaction, not their
acquisition. See action records around `fortune_preserved_fourteenth`.

**Yellow Protection Amulet: PASS, synthetic arrow-damage control.** With
unarmored armor attribute zero, the no-amulet four-point arrow damage input
took health from 20 to 16. The Yellow amulet was then physically equipped in
the accessory slot; after restoring health to 20, the same damage input left
18 health. The input was `/damage`-style test damage, not a fired projectile.
Natural regeneration was disabled for the paired check and restored afterward.

**Vortex fifth pedestal: PASS, physical exchange and held-item render.** At
`(116,180,103)`, a separate fifth pedestal outside the four cardinal recipe
pedestals accepted a QA-marked Diamond through actual use. Block data showed
the marker-bearing Diamond held by the pedestal; the player's selected test
slot was empty. A second actual use returned the item and emptied the pedestal.
The held item was visible in the captured
[client view](../../build/qa-audit/node-pilot-playtest/1510-vortex-held-render.png).
This was a physical pedestal interaction/render check, not a vortex recipe
completion; the marked Diamond was fixture-issued.

## Not Accepted Or Pending

**Ring of the Shattered Stone: NOT ACCEPTED on this candidate.** The first
equipped-ring attempt failed its arena-position preflight. The retry observed
stone destruction with both ores preserved in the ring lane, but the no-ring
control also left stone and both ores untouched across its trials; the helper
therefore returned `INCONCLUSIVE`. This exposed the source `getToBlow()` list
alias defect in the no-wearer explosion path, so the apparent ring-lane result
is not parity evidence. See the separate
[explosion-list alias record](2026-09-23-explosion-list-alias.md) for the
source diagnosis and later repair/validation; no Shattered Stone PASS is
claimed here. Fourteenth logs are
[equipped attempt](../../build/qa-audit/node-pilot-playtest/fourteenth-shattered-equipped.log)
and [retry](../../build/qa-audit/node-pilot-playtest/fourteenth-shattered-equipped-retry.log).

**Nether ritual queue reload: PENDING.** At `(664,180,464)`, the live Nether
ritual reached `RITUAL_RUNNING` with a six-cell ordered queue, plains source,
zero progress, and zero stored/last power. The exact packed queue was captured
to [checkpoint JSON](../../build/qa-audit/node-pilot-playtest/fourteenth-ritual-nether-checkpoint.json)
after one checkpoint tick. The charge-stage JSONL records 16 merges of 315,000
and seven 5,000 top-ups: 5,075,000 fixture-seeded power across 23 ticks. The
queue checkpoint stage seeded no additional power. The
session later saved and closed normally, but no reopen/compare has verified
queue persistence yet. Setup, charge and checkpoint records are the three
[`fourteenth-ritual-*.log`](../../build/qa-audit/node-pilot-playtest/) files.

**Bookshelf line of sight:** covered by the storage owner's separate
[Bookshelf line-of-sight audit](2026-09-23-bookshelf-line-of-sight.md); this
checkpoint intentionally does not duplicate that result.

## Session Closure

The client/integrated-server run finished with exit code 0. Its
[monitor summary](../../build/qa-audit/fourteenth-gameplay/summary.json)
reports peak working set 1,426.9 MiB and peak private bytes 1,953.5 MiB for the
client, integrated server and monitor; the configured scope excludes desktop
and agent hosts. At final cleanup, nine `forceload remove` operations removed
all 37 parent-forced Overworld chunks; the subsequent `forceload query` found
none. Player918 was left in Creative at the remote safe-floor position
`2049.5, 180, 2049.5`. The fourteenth pre-run inventory and the later
fifteenth final snapshot compare equal except slot 21: `minecraft:cobblestone`
is 8 in the former and 9 in the latter. The fourteenth final raw runtime log
has since been overwritten, so an exact end-of-session inventory match cannot
be claimed. No original item is absent from this cross-run comparison; the
extra cobblestone's origin and timing are unverified. The game log records all
dimensions saved.

Primary run evidence: [integrated log](../../build/qa-audit/fourteenth-gameplay/stdout.log),
[process summary](../../build/qa-audit/fourteenth-gameplay/summary.json),
[inventory before](../../build/qa-audit/node-pilot-playtest/fourteenth-parent-inventory-before.txt),
and [action stream](../../build/qa-audit/node-pilot-playtest/actions.jsonl).
