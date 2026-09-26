# Integrated Restoration Validation

This is an integration record, not a release sign-off. The full acceptance
ledger and release gate remain open.

## Build Attempts

- `integrated-restoration-first`: Java compilation found three 1.21.1 API
  mismatches: the explosion sound holder, fairy renderer registration access,
  and item pickup-delay access. All three were corrected. Peak sampled working
  set 1,134.6 MiB; private allocation 1,186.7 MiB; no memory guard trip.
- `integrated-restoration-retry`: main and test sources compiled. The suite ran
  165 tests with 13 failures. Peak sampled working set 1,495.3 MiB; private
  allocation 1,619.0 MiB; no memory guard trip. The build stopped at the failing
  tests, so it did not produce a validated release or refreshed client launch.

Failures were assigned by ownership: block-entity test bootstrap, Flux test
accounting, component-sensitive recipe parsing, a missing vortex recipe type,
utility-test registry isolation, and guide content/readability assertions.
Fixes require another complete run; inspection is not a passing result.

- `integrated-restoration-third`: 177 tests, six failures. Main and tests
  compiled; remaining failures were test-registry setup and guide/documentation
  assertions. Peak working set 1,509.5 MiB, private allocation 1,633.1 MiB.
- `integrated-restoration-fourth`: 183 tests, one remaining guide-copy assertion.
  Peak working set 1,384.4 MiB, private allocation 1,538.9 MiB.
- `integrated-restoration-fifth`: **183 tests across 51 suites, zero failures,
  errors or skips**. Test, build, remapping and client-launch preparation passed.
  Peak working set 1,374.4 MiB, private allocation 1,491.1 MiB; no guard trip.
  Distribution SHA-256:
  `6833f7759ad2c64f7c74c82715a06156e5dfb39459424362fd950933e49b49e6`.
  Sources SHA-256:
  `67b6486713a2cd1d5976c10cacb267e8d6ac5330ded40d2e35ce353e0a6d95de`.

This is an integration checkpoint, still using development version
`0.1.1+1.21.1`; it is not authorized for publication as a completed beta.

- `integrated-restoration-sixth`: production and observer compilation passed;
  198 tests ran with one failing item-entity test fixture. The fixture used a
  world-dependent random-motion constructor without a world. Replacement drops
  now take explicit preserved velocity; the context-only unit uses empty
  payloads without a world registry. Peak working set 1,527.4 MiB, private
  allocation 1,650.0 MiB.
- `integrated-restoration-seventh`: **198 tests across 52 suites, zero failures
  or errors**. Build, client launch preparation and separate observer remapping
  passed. Peak working set 1,459.7 MiB, private allocation 1,587.6 MiB; no guard
  trip. Aura SHA-256:
  `0e48a2765e09d19b58c4d5a7c87d4f01181627399966a219ff4be9294128fb85`.
  Observer SHA-256:
  `5334098c25bf0521c8b052b39c63687c7b5d4c85a511df78cd09a76ad5455484`.

## Packaged Dedicated Server

The seventh-build distribution was installed with Fabric Loader 0.19.1,
Fabric API 0.116.11+1.21.1 and Patchouli 1.21.1-93-FABRIC into an isolated
server instance, using bundled Energy API and no testing bridge. It loaded
1,486 recipes and 1,414 advancements, reached `Done`, processed `list` and
`save-all flush`, and stopped normally with all dimensions saved, exit zero.
It listened only on loopback for this offline QA fixture. Peak server-plus-
monitor working set was 715.7 MiB, private allocation 679.1 MiB. This proves
production server startup/save/shutdown, not multiplayer gameplay acceptance.

## Client Integration

The first launch reached the title screen but lacked the bridge token and used
Loom's randomized player name. It was stopped before opening a world. The
launcher now takes an explicit username (default `AuraLab`); the saved fixture
was reopened as its existing `Player918`, with the session token supplied only
through the environment. The token was not printed or placed in arguments.

The retry loaded the saved world with 1,486 recipes and 1,414 advancements, no
recipe parse errors, and the previous player position intact. Startup exposed
invalid Patchouli icon IDs and a missing invisible fairy-light model declaration.
Those require source fixes and a fresh runtime check. Independent storage,
equipment, quest and guide QA is running against the fifth-build snapshot;
later source edits do not retroactively change that snapshot's evidence.

The retry completed with normal Save and Quit, then Quit Game, exit code zero.
Peak sampled client-plus-monitor working set was 1,297.3 MiB and private
allocation 1,808.9 MiB, with no guard trip. The independent report records
selected component-sensitive storage retrieval, all fourteen quest pages at
scales 2/3, one-time reward persistence, and bounded accessory death/reload
checks. See the accessory-storage playtest report for exact controls and
limitations. Search entry, fixed browser visuals, config-disabled quests and
other outstanding checks are not promoted to passes by these results.

## Review Findings

The full page-key audit and independently mapped 592 bytecode subsequently
identified six real gaps: wool-based ingot recipes, advanced wool processing,
shared consumer keepalive, End Stone fortification, Arcane Ingot enchanting,
and Extinguisher lava removal. See `2026-09-23-guide-conflict-disposition.md`.
Source repairs are pending fresh integration. The seventh crystal-based
processor/enchanter checks below remain historical evidence of that binary,
not acceptance of the corrected original recipes. Tenth-snapshot node and
actual mining results are in `2026-09-23-node-mining-runtime.md`; those checks
also found the missing custom-tool enchantability tags, now repaired in source.

### Seventh-Build Machine Checks

The parent ran bounded, near-completion fixtures in the disposable core-systems
world. Consumer progress was explicitly seeded; adjacent node power, not consumer
power, supplied the final step. These checks do not prove natural acquisition or
a full powered cycle. Unpowered controls held their progress and inputs.

- Smelter: one raw iron disappeared and one iron ingot appeared; progress reset
  to zero and `last_received_power` was 190.
- Synthesizer: one `aura:angelsteel_ingot_1` appeared after 10,000 node power.
- Enchanter: the plain diamond pickaxe acquired `aura:kaleidoscopic_red:1`, the
  red crystal was consumed, and progress reset after 500 node power. Without
  power, both inputs and the unenchanted component state were retained.
- Looter: `LOOTED`, seven bones from the dungeon table, and reset progress after
  5,000 node power. No item appeared during the unpowered control.
- Spawner: `SPAWNED`, a newly created 20-health Zombie, and reset progress after
  190 node power. No entity existed during the plains-biome unpowered control.

Private evidence is in `build/qa-audit/node-pilot-playtest/actions.jsonl` and
the core-systems client log. Initial setup errors were corrected before the
passing runs: power injected before the phase-18 retention tick decayed before
work; an enchanter crystal placed too close to its supplying node was absorbed;
and a nearby player immediately collected the synthesizer output. Inputs were
replaced and controls repeated. The successful runs align injection to game-time
phase 19 and step through the next work tick. Screenshots 804 are not visual
acceptance: the player fell off the elevated test platform while world ticks
were frozen. The platform was extended before the independent late-game handoff.

### Source Review

Five additional seventh-build consumer fixtures passed 25-tick unpowered
controls followed by a powered final step: Grower advanced a cactus with random
ticks disabled (50 power); Fisher produced raw cod above its required water
layer (200); Brewer changed water to awkward potion while retaining pickup delay
and output motion (500); Colorer restored wool and changed a nearby sheep to
brown without changing an outside sheep (50); Prismatic Processor consumed eight
distinct gems and produced one Arcane Prism (1,000). These are seeded-progress
mechanical checks, not full survival progression. The private helper is
`remaining-consumers.ps1`; predicates, output NBT and controls are recorded in
the action log. Temporary forced chunks were removed and random ticks restored.

The seventh client subsequently saved and quit normally, exit 0. Its sampled
peak working set was 1,431.4 MiB and private allocation 1,842.7 MiB. The configured
3,200 MiB client guard did not trigger. Desktop/agent-host totals are excluded
from the user's Minecraft/build-only budget. Quest configuration was restored
to true while the client was closed, following the disabled-quest test.

Angelsteel Fortune no longer uses temporary held-item mutations or break-event
bookkeeping. Review found a concrete nested Kaleidoscopic break that could pop
the outer restoration state and then restore the temporary enchantment. Fortune
now decorates only a copied loot tool at the existing drop hook. Five focused
tests accompany the change; see `2026-09-23-angelsteel-drop-fortune.md`. Both the
tests and a combined-enchantment live break await the next build.

The post-seventh damage mixin now modifies the `actuallyHurt` call arguments in
`LivingEntity.hurt`, after vanilla's invulnerability/cooldown rejection and
before armor mitigation. Mapped bytecode confirms both accepted branches call
that virtual method, including player overrides. The earlier HEAD hook could
advance Barbarian combo state on rejected damage. Original `ItemComboSword`
updates that state in `LivingHurtEvent`, not `hurtEnemy`; indirect player-owned
damage remains eligible as in the source. This patch needs the next build and
accepted/rejected-hit runtime controls.

The independent late-game report retained a failed lower-edge biome probe after
both rituals, while center probes and mapped blocks passed. Parent comparison
then reproduced that exact boundary with vanilla alone: `/fillbiome 200 -64 200
203 319 203 minecraft:the_end` updated all 96 quart cells of the same column, yet
`202 180 201` still sampled Plains and `202 182 202` sampled The End, before and
after the command. This supports the modern biome sampler boundary explanation,
not a failed ritual palette mutation. The vanilla command is a diagnostic control,
not ritual output evidence; the prior independent ritual results are preserved in
`core-late-game-playtest.md`. A four-block quart column is not guaranteed to give
identical block-position biome probes at its geometric edge.

- Enchantment ore conversion was moved out of the pre-break callback into the
  normal player-drop stage, after block-break vetoes. Live veto and last-tool-
  durability controls are still required.
- Fairy authorization compared the owner UUID with a map containing entity
  UUIDs. Client Digger prediction also depended on unsynchronized accessory
  state. These findings are under repair, with owner-only Fabric attachment
  synchronization added to the existing equipment loadout.
- Unloaded fairies returning after ring removal require explicit cleanup;
  denied role effects alone do not remove the visible stale entity.
- The modern knockback strength parameter must not be confused with the old
  damage argument in the Pusher Fairy call.
- Prismatic Wand selection and paste still need bounded-work, loaded-chunk,
  placement and material-loss review before gameplay acceptance.

## Scope

The default-enabled original questline toggle is restored through
`config/aura.properties` (`questline=false` disables grants after restart).
Fourteen server-awarded quest advancements persist one-time rewards and drive
Patchouli quest pages. The fifth-build guide has 41 entries and 330 pages; resource and
font-layout checks do not replace actual navigation and page-render inspection.

All heavy tooling remains serialized with other Minecraft work. Memory figures
include the Gradle process tree and monitor, exclude Codex and unrelated apps,
and are one-second samples rather than an operating-system hard cap.

## Twelfth Candidate Checkpoint

The twelfth measured integration completed `test`, `build`, `auditClientLaunch`,
`qaObserverRemapJar` and `qaMultiplayerRemapJar`: 226 tests across 57 suites,
zero failures/errors. Peak working set was 1,423.5 MiB; private allocation
1,567.8 MiB. This includes the corrected D1-D6 contracts and Aura tool tags.
Three formerly invalid unit-loader entity tests were moved to an explicit
packaged runtime gate, not counted as unit passes.

Candidate `aura-cascade-0.2.0+1.21.1.jar` was 1,588,295 bytes with SHA-256
`e8177931dcbb3cec34345e3d1f3d2abfb002b2955d2438978c78b407066e4210`.
It is not yet a release-approved artifact.

- The first packaged attempt failed before Minecraft because the private
  launcher omitted Fabric dependencies without inline checksums. The launcher
  now resolves their official Maven SHA-1 sidecars and includes both libraries.
- The next attempt loaded that JAR in the real intermediary runtime but stopped
  at the first-launch accessibility screen. Its observer timed out and correctly
  failed; the fresh isolated profile now initializes onboarding/QA settings.
- Run `20260923-091107-056` loaded the copied world, rendered 40 stable frames
  with visible Aura storage blocks, saved and exited normally. Peak working set
  was 1,237.5 MiB and private allocation 1,774.3 MiB. The separate guide gate
  failed: all 41 entries/371 pages/14 quests loaded, no bottom overflow was
  reported, but 12 heading-width failures and 151 text-fragment width flags need
  correction or measurement diagnosis. This is world-render smoke evidence,
  not a fully passed packaged acceptance run.
- Multiplayer run `20260923-091306-403` passed the real transformed item-lifetime
  persistence, merging and old-item eligibility checks on the same JAR hash.
  The run then aborted at the conservative 3,800 MiB soft memory guard before
  equipment/fairy checks. All three Java processes exited normally with code 0;
  aggregate peaks were 2,787.4 MiB working set and 3,936.0 MiB private allocation.
  This is not multiplayer synchronization acceptance. Memory tuning is pending;
  the guard is not being raised to obtain a pass.

The private reports remain under `build/qa-audit`. Raw material fixtures and
runtime probe code are not part of the distributed mod. No CurseForge upload
or credential access occurred during this checkpoint.

## Thirteenth Candidate Checkpoint

The next full measured integration passed all five tasks above: 227 tests in
57 suites, no failures, errors or skips. Working set peaked at 1,704.3 MiB and
private allocation at 1,820.4 MiB. The candidate is 1,588,283 bytes, SHA-256
`306ab942b05029d814607a155e3a3796eaed3384b85af0cf92423a74c5ac63d4`.

Packaged run `20260923-093128-934` passed the intermediary-runtime, actual-JAR,
world-render and guide gates, then saved and exited normally. All 41 entries,
371 pages and 14 quest pages loaded. Actual-font audit v2 found zero visible
text-width, title-width or bottom-overflow failures. The 151 earlier fragment
alarms were trailing whitespace beyond the line, not visible glyph overflow;
the raw measurements remain in the report. Twelve actual oversized headings
were shortened. This is whole-book font/layout evidence, not a claim that a
human clicked every page. The parent inspected the world framebuffer. Peak
working set was 1,235.7 MiB; private allocation was 1,771.3 MiB.

Multiplayer run `20260923-092401-996` fit the memory budget but timed out at
binding. Its test driver selected a hotbar slot and immediately used the item;
Fabric's successful use callback cancelled before vanilla selected-slot sync.
The private driver now sends the ordinary carried-slot packet before use and
records the held item/result. No production fix was made for that fixture bug.

Run `20260923-093241-443` passed all 15 state checks on the thirteenth candidate:
real ring equip and charm use, matching fairy identity on server/both clients,
private owner-only accessory state, logout cleanup and reconnect restoration
without another menu/equip action. All four JVM lifetimes exited normally.
Peak aggregate working set was 2,512.7 MiB and private allocation 3,032.2 MiB.
The transformed item-lifetime persistence/merge/eligibility gate also passed.
The owner screenshot shows a fairy; the witness's third-person body obstructs
the view, so witness fairy pixels are not independently accepted. Matching
remote entity state is accepted, and is not being relabelled pixel evidence.

The private LAB8 bridge adds read-only mining-speed inspection for the next
gameplay session. Neither it nor the separate multiplayer/observer mods belongs
in the release JAR. Remaining gameplay gates still apply; no upload occurred.

## Fourteenth Candidate Checkpoint

Full `test`, `build`, `auditClientLaunch`, `qaObserverRemapJar` and
`qaMultiplayerRemapJar` integration passed in 53 seconds: 234 tests across
58 suites, zero failures/errors/skips. The measured launcher closed with code 0;
working set peaked at 1,449.5 MiB and private allocation at 1,555.5 MiB. All Java
processes were closed before returning the shared heavy-workload slot.

Candidate size is 1,588,960 bytes, SHA-256
`282c949d14073c207eb95095c3e622bd30e5b184ef4026b4b24f84f68a207490`.
This integrates chainable Digger/Kaleidoscopic mining return modifiers and
source/target-only raycast exemptions for bookshelf visibility. Five real
vanilla clipping regressions and two structural mining-hook audits were added.
The mining tests do not claim to transform Minecraft in JUnit. Independent
client confirmation and the remaining finite gameplay gates still apply.

Evidence: `build/qa-audit/integrated-restoration-fourteenth/summary.json` and
the current JUnit XML reports. No credentials were accessed or upload attempted.

Packaged run `20260923-105310-191` then passed on that exact fourteenth JAR:
intermediary namespace, world loaded, 40 stable rendered frames, and all 41
entries/371 pages/14 quests passing actual-font audit v2 with no failures. The
parent inspected the world screenshot. Normal save/exit returned code 0;
working set/private peaks were 1,244.4/1,778.2 MiB. This repeats packaged loading
and whole-book layout, not every individual gameplay interaction.
