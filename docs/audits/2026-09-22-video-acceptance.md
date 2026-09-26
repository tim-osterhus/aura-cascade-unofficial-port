# Video-derived implementation and acceptance ledger

## Authority and status

This is the canonical feature acceptance and remaining-gameplay ledger, reconciled
on 2026-09-23 through the sixteenth integration and final-candidate runtime checks.
It supersedes the statuses in the [baseline backlog](2026-09-22-remaining-work.md),
not the historical evidence in that report. The guide/toolkit pilot and pauses in
the [execution record](2026-09-22-staggered-execution.md) are earlier checkpoints,
not the current implementation scope. Publication remains governed by the
parent-owned [release gate](2026-09-22-beta-release-gate.md).

Treat Minecraft 1.7.10 and 1.8.9 gameplay as provisionally equivalent where no
conflict exists. The videos themselves span different dates/builds, so visible
workflows are references, not authority for every numeric recipe or timing value.
Resolve actual contradictions against the shipped `AuraCascade-592.jar` before
changing progression. A video's omission is not evidence that a feature was absent.

The full-duration reports, manifest paths, transcript provenance, frame coverage,
and unresolved observations are maintained in the [video-reference index](video-reference/README.md).
Video-derived observations are sampled visual evidence, not continuous playback.
The subsequent conversation-informed [plugin proposal](2026-09-22-plugin-plan.md)
changes the proposed order to a thin toolkit and capability proof, then staggered
parallel toolkit/mod work after an explicit development resume; it also calls for
evaluating Patchouli before a custom encyclopedia screen.

Explicit user-approved modernization takes precedence over legacy recipe fidelity
for the White Aura Crystal recipe only: a center amethyst shard and eight gold
nuggets produce two crystals. This intentional divergence is documented for
developers; the in-game encyclopedia must describe only the current in-world recipe.

## Current evidence checkpoint

- **Fourteenth integration:** 234 tests in 58 suites pass, with no failures,
  errors or skips. Candidate size 1,588,960 bytes, SHA-256
  `282c949d14073c207eb95095c3e622bd30e5b184ef4026b4b24f84f68a207490`.
  Digger/Kaleidoscopic mining composition and bookshelf line-of-sight fixes
  are compiled; targeted fourteenth-runtime confirmation is summarized below.
  Peak build working set/private allocation: 1,449.5/1,555.5 MiB. Earlier runtime results
  below remain tied to their explicitly identified artifacts.
- **Fourteenth packaged recheck:** run `20260923-105310-191` loaded the exact new
  hash in the intermediary client, rendered 40 stable world frames, and passed
  all 41 entries/371 pages/14 quests in real-font audit v2. Normal save/exit
  returned 0; working set/private peaks were 1,244.4/1,778.2 MiB. Gameplay fix
  confirmation remains separate from this loading/layout recheck.
- **Fourteenth Digger composition:** independent actual client mining-speed
  inspection passes all four controls: base 2.0, four Diggers 2.519424,
  Orange 1/Yellow 1 pair alone 2.875, both together 3.621672. Physical ring
  unequip and held-component checks support the comparison. This closes the
  thirteenth speed-suppression defect; see [the composition audit](2026-09-23-mining-speed-composition.md).
- **Fifteenth integration:** parent reports `test`, `build`, `auditClientLaunch`,
  `qaObserverRemapJar` and `qaMultiplayerRemapJar` passed: 238 tests in 59 suites,
  with zero failures, errors or skips. The reviewed JAR is 1,589,012 bytes with
  SHA-256 `27502af9b5e763c16464fbccdac7381929326aa698569eb1c2debbeaa3811fb5`.
  The fifteenth integrated gameplay session exited 0 and its bounded results are
  below. At that checkpoint, packaged-client and multiplayer runtime checks had
  not been run against the fifteenth hash; current-candidate checks are tracked
  below. The earlier packaged and MP runs remain tied to their recorded older
  candidates. See the [build summary](../../build/qa-audit/integrated-restoration-fifteenth/summary.json)
  and [fifteenth gameplay log](../../build/qa-audit/fifteenth-gameplay/stdout.log).
- **Sixteenth integration:** source 16 is fully green: 244 tests across 60
  suites in 44 seconds. It restores the original non-fiery Red Hole explosion
  (`fire=false`) and skips the remainder of normal item ticking only on each
  eruption tick. The next 100 world ticks contain 99 normal item-age updates.
  The bounded live rerun passed; see [final validation](2026-09-23-sixteenth-release-validation.md).
  Astra's review found no identified remaining finite gameplay
  gate.
- **Final sixteenth packaged client:** run `20260923-121601-864` loaded exact
  artifact SHA-256
  `cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`, rendered
  40 frames, and passed the font audit for 41 entries, 371 pages and 14 quests.
  It exited normally with code 0.
- **Final sixteenth multiplayer:** run `20260923-121724-053` passed all 15
  checks; all four JVMs exited 0. The D3 probe passed all three checks. Peak
  working set/private allocation was 2,510.7/3,025.1 MiB. Server/client state
  and matching fairy UUIDs establish synchronization on both clients. The
  screenshots are not bilateral pixel-visibility proof; rejoin camera angles
  may occlude the fairy.
- **Thirteenth source/build candidate:** `test`, `build`, `auditClientLaunch`,
  `qaObserverRemapJar` and `qaMultiplayerRemapJar` passed. The current JUnit
  report has 227 tests across 57 suites, with zero failures, errors or skips.
  The candidate JAR is 1,588,283 bytes, SHA-256
  `306ab942b05029d814607a155e3a3796eaed3384b85af0cf92423a74c5ac63d4`.
  Peak measured Gradle-tree working set/private allocation was 1,704.3/1,820.4
  MiB. D1-D6 and Aura tool-tag source changes are integrated; source/build green
  does not itself pass their gameplay cases. See the [thirteenth run summary](../../build/qa-audit/integrated-restoration-thirteenth/summary.json)
  and [JUnit report](../../build/reports/tests/test/index.html).
- **Packaged client and font gate:** run `20260923-093128-934` loaded the copied
  world, rendered 40 stable frames, saved/exited normally and used the exact
  thirteenth-candidate hash in the intermediary runtime. Audit v2 loaded 41
  entries, 371 pages and 14 quests with zero visible glyph, title-width or
  bottom-overflow failures. Its 151 remaining text-fragment flags were
  whitespace-only; the real-font layout gate is PASS, but this is not evidence
  that every page was manually clicked. Peak working set/private allocation was
  1,235.7/1,771.3 MiB. See the [client summary](../../build/qa-audit/packaged-client-runs/20260923-093128-934/summary.json),
  [observer manifest](../../build/qa-audit/observer/runs/20260923-093128-934/manifest.json)
  and [font-layout report](../../build/qa-audit/observer/runs/20260923-093128-934/guide-layout.json).
- **Multiplayer and D3:** run `20260923-093241-443` passed all 15 checks on
  the same thirteenth-candidate hash, including owner-only accessory state,
  binding the same fairy UUID on server and both clients, logout removal, and
  rejoin restoring one bound ring and one fairy without reopening the menu.
  All four JVMs exited 0; peak working set/private allocation was
  2,512.7/3,032.2 MiB. Runtime observations establish state/UUID synchronization;
  the owner screenshot shows the fairy, while the witness third-person view is
  occluded and is not bilateral pixel proof. The transformed-item D3 probe also
  passed all three checks: age/persistence, merge, and old-item merge eligibility.
  B5's specific owner/binding synchronization portion is closed. A single
  bookshelf entry/power save-reload now passes under S2; broader storage and
  active-circuit persistence remain separate open work. See the [multiplayer summary](../../build/qa-audit/multiplayer/20260923-093241-443/summary.json),
  [D3 probe](../../build/qa-audit/multiplayer/20260923-093241-443/consumer-lifetime-probe.json)
  and [server observation](../../build/qa-audit/multiplayer/20260923-093241-443/server/latest.json).
- **Earlier LAB7 browser/fairy runtime:** the [tenth-client report](2026-09-23-fairy-gameplay-playtest.md)
  passed storage search/no-results/clear and ordinary sneak-use release with
  inventory restoration. Bounded Fetcher, Fighter, Debuffer, Lighter, Glider
  and Shooter checks passed; the four-Digger result was inconclusive on that
  earlier snapshot and is superseded by the fourteenth actual-speed inspection.
  These results cover only the listed roles, not a full 17-role runtime pass.
- **Thirteenth gameplay:** [the execution record](2026-09-23-thirteenth-gameplay.md)
  closes all 68 corrected recipe/catalyst cases, real consumer refresh and
  despawn controls, full White ingot/Prism/Angelsteel operation without injected
  power or progress, and earned Miner charge-21 ore yield. The Miner alone used
  injected adjacent-node power, not injected charge/progress/output. All fixtures
  supplied raw materials; fully natural acquisition is not claimed. Inventory
  NBT was unchanged, forced chunks released, and normal save/exit passed at
  1,367.1 MiB working set / 1,937.6 MiB private peak.
- **Independent thirteenth gameplay:** [the independent report](2026-09-23-independent-thirteenth-playtest.md)
  passes Forbidden Fruit EAT/DRINK completion and cancellation controls,
  Extinguisher LAVA/FIRE and owner-fire controls, 15-fairy capacity/16th rejection,
  clean full-inventory release without duplication, and ordinary Wand live-copy,
  occupied/material/protected controls. Original inventory NBT was restored
  exactly. Actual client destroy speed, however, stayed 2.0 with four Diggers
  instead of 2.519424, including a settled repeat on that thirteenth snapshot.
  This was a confirmed then-current defect, superseded by the fourteenth actual
  client-speed controls above. Normal save/exit passed at 1,340.3 MiB working set
  / 1,861.5 MiB private.
- **Prior MP failure superseded:** `20260923-092401-996` on the earlier
  twelfth-candidate hash timed out at bound-fairy observation after the physical
  owner-only ring state synchronized. That timeout was a probe-ordering
  limitation, not evidence of a production failure; the thirteenth-candidate
  15-check run above now passes the bounded synchronization scenario.
- Earlier seeded processor, machine and protection results remain valid only
  for their recorded inputs and snapshots. No row establishes a fully natural,
  guide-led acquisition chain.

Status words below distinguish **source/test**, **seeded runtime**, and
**ordinary operation**. No row establishes a fully natural, guide-led acquisition
chain. A missing live check is not automatically a missing implementation.

## Current Overrides and Historical Reference Tables

### Current Integration Overrides (2026-09-23, Sixteenth Integration)

The feature and session matrices below are retained historical snapshots, not
current status summaries. Apply these current overrides to their corresponding
rows; do not infer a pass from the older row text alone:

- **G2:** real-font measurement passes on the thirteenth and fourteenth
  packaged candidates for all loaded guide pages. The fourteenth run loaded 41
  entries, 371 pages and 14 quests with no visible glyph, heading-width or
  bottom-overflow failures. The 151 whitespace-only flags are audit noise;
  manual page-by-page interaction remains unverified.
- **E1 / Aura tools:** the fourteenth survival fixture passed its bounded Red
  Kaleidoscopic/Angelsteel mining case and Adventure-mode break veto, preserving
  the enchantment and buffs. This does not accept every Fortune/enchantment
  pairing, crop, tool or loot-table case.
- **F1 / B2:** capacity 15, rejection of a 16th charm, and clean full-inventory
  release/repeated-release conservation now pass independently. The fourteenth
  candidate closes Digger's actual-speed defect with base, Digger-only,
  enchantment-only and combined readings. Other bounded earlier role checks
  remain valid as recorded.
- **D1-D6:** source and regression coverage are integrated into the thirteenth
  build. D1/D2/D4/D5 now pass all 68 selected in-world cases, including their
  unpowered and rejection controls. D3 passes real 500-tick refresh/bounds and
  accelerated ordinary-item despawn controls as well as packaged persistence,
  merge and old-item eligibility. D6's actual Extinguisher lava/fire contrast
  and owner-fire clearing now pass independently. See [the gameplay record](2026-09-23-thirteenth-gameplay.md)
  and [the conflict disposition](2026-09-23-guide-conflict-disposition.md).
- **B1 power / vortex:** with fixture-seeded initial Aura, the fourteenth
  fixture passed Orange lateral induced transfer and capacitor
  threshold/redstone inhibition controls. The fifteenth
  fixture persisted four Vortex receipts; after 160 normal ticks, 32 supplied
  raw Yellow crystals were consumed, receipts cleared and one Looter output
  appeared. The existing partial recipe state and raw crystals were fixture
  inputs, not naturally acquired progression. Manual pedestal exchange and a
  wholly natural guide-led chain remain unverified. The sixteenth Survival
  control consumed one of two held White crystals on direct use (node 0 to
  1,000), then consumed a real dropped final crystal after 10 ticks (node to
  2,000; held stack empty and item entity gone).
- **B3 / Fruit:** actual EAT/DRINK completion, early-release and slot-switch
  cancellation, unequipped controls and creative completion without a count
  decrease now pass. Other equipment/combat gates remain separate.
- **B4 Miner / Wand / ritual:** earned Miner charge 21 yielded one actual ore
  with intact containment and no residual charge/progress/power/helper. Wand
  selection, live-source, occupied/material-conservation and Adventure-reject /
  Survival-place controls pass. The fifteenth ritual fixture passed a six-cell
  active-queue save/reload, zero-power continuation, bounded 400-plane result,
  self-removal and outside witness; the same-target paid cycle was a no-op.
  That paid case used fixture-seeded power. The sixteenth Red Hole live retest
  now passes as recorded below.
- **B5:** owner-only equipment, bound-fairy identity/sync, logout cleanup and
  rejoin restoration pass in the thirteenth-candidate fixture. The fifteenth
  launch also confirms one stored bookshelf stone entry plus 941 node power
  survived save/reload under S2; broader storage and active-circuit
  persistence/reload remain open.
- **B2 / F1 / role runtime:** the fifteenth multiplayer run passed 15 checks,
  including one canonical fairy UUID on server and both clients, owner-only
  binding, logout cleanup and one bound fairy after rejoin. The owner's fairy
  is visible in its screenshot; the witness view is occluded, so bilateral
  pixel proof is not claimed. Earlier bounded role checks and the fourteenth
  actual Digger-speed composition pass stand; this is not a full 17-role action
  acceptance.
- **A2 / R2 / T1 / T2 equipment:** fourteenth Yellow amulet testing confirmed
  the bounded synthetic damage reduction; Mirror redirection, Barbarian combo,
  Transmuting Sword and movement fixtures passed their recorded cases. The
  independent Heels, Traveler's Bricks and Enigma trials passed bounded
  movement checks, not every threshold or immunity claim. Fifteenth Shattered
  Stone near, far and physically unequipped controls preserved ores as expected;
  the wearer was in Creative for the near blast, so no damage protection is
  claimed.
- **U1 / Red Hole, source 16:** the isolated fixture passed two actual blasts
  at game times 124000 and 124100. The marker remained at ages 0 and 99,
  respectively; the 32-block outside witness stayed unchanged, and the
  one-second post-second-blast freeze control produced no extra explosion.
  Before testing, only prior-run fire inside the owned arena box x590-640,
  y180-184, z390-410 was cleared. No broader world cleanup or wearer-immunity
  claim is included. This closes the bounded repeated-blast case; Astra's
  review found no remaining finite gameplay gate. The detailed sixteenth report
  is forthcoming.
- **S2 / Bookshelf network:** the fourteenth Survival fixture passed actual
  vanilla-bookshelf conversion and bounded adjacent/bent-network cases. The
  adjacent converted shelf showed `Connected`, `1000 / 5`. With eight connected
  shelves, a separate LOS blocker produced `No visible storage shelves`,
  `1000 / 59`, and a blocked stone deposit retained the held item. Clearing LOS
  showed `Connected`, `1000 / 59` after 21 game ticks; a real stone deposit
  produced one stored entry and charged 59, leaving 941. Removing the first BFS
  connector then showed `Disconnected`, `941 / 0`; a dirt-two attempt preserved
  both held items, the remote stone entry stayed intact, and power remained 941.
  The connector was restored, the world saved, and the fourteenth process exited
  normally with code 0. A fresh fifteenth launch read back the remote book's
  exact one-stone `storageEntries` payload and nested
  `node_state.stored_power:941`; correctly nested positive data predicates
  passed. An earlier top-level `stored_power`
  query failed because it used the wrong NBT path and is not counted as a failed
  check. This closes only that bounded entry/power round trip. The fixture seeded
  initial node power and did not repeat powerless rejection or retrieval; those
  remain accepted from the earlier seventh storage fixture. Broader storage and
  active-circuit persistence remain open. See the
  [bookshelf LOS audit](2026-09-23-bookshelf-line-of-sight.md), including its
  linked fourteenth run log, fifteenth `latest.log`, result records, action trace
  and screenshots.

### Finite Current Beta Gates (2026-09-23)

All finite gameplay cases tracked by this ledger are now reported closed,
and Astra's review identified no remaining finite gameplay gate. Final-candidate
runtime validation is complete; release approval remains outside this ledger:

- **Final candidate:** packaged-client and multiplayer checks passed on exact
  hash `cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`; see
  runs `20260923-121601-864` and `20260923-121724-053` above.
- **Release:** the parent-owned beta release gate remains offline. This ledger
  does not authorize publication; release stays with that gate.

Already-passed cases above are not reopened for coverage volume. Other rows in
the historical matrices remain evidence boundaries, not an expanding list of
new beta blockers. The parent-owned release gate remains authoritative. This
ledger does not claim full legacy parity, a naturally acquired guide-led chain,
all fairy roles/effects, or manual interaction with every guide page. The Red
Hole runtime pass covers two in-session blasts, not save/reload. Multiplayer
evidence is state/UUID synchronization, not bilateral pixel visibility.

### Historical Feature Acceptance Matrix (pre-thirteenth status snapshot)

| ID | Feature and evidence | Acceptance test | Implementation/verification status |
| --- | --- | --- | --- |
| G1 | Encyclopedia as the progression interface; Part 2, 00:29-00:49 focused frames show icon index, Walkthrough entries, illustrated page, Back and Shift-click-for-index | Use the actual Aura item; open categories and entries; follow a topic or recipe and return to the originating page; read all text at GUI scales 2 and 3 | PASS for pilot opening, sampled navigation/back/links and body readability at scales 2/3. Independent follow-up also passed both corrected headings at both scales. Full-book coverage remains open. See guide-pilot-playtest record |
| G2 | Full instructional content rather than the ten-page summary; previous live baseline demonstrated clipped content and no normal opening | Font-measured pagination preserves every line; navigation bounds and resize preserve a valid page; ordinary crafting and custom recipes are reachable | Source/test: seventh baseline has 41 entries/335 pages; later source has 345 pages pending integration. Seventh independent startup found no invalid Patchouli icon warning, resolving that specific failure. All 14 quest pages passed visible scales 2/3 and selected link/back checks; not full-book acceptance. See guide-coverage and accessory-storage reports |
| M1 | User-approved modernization, not video-derived parity: one amethyst shard surrounded by eight gold nuggets -> 2 White Aura Crystals | Check shaped recipe data; survival crafting consumes one shard/eight nuggets and yields two crystals; former iron layout no longer crafts crystals; encyclopedia recipe, output and walkthrough agree, remain readable and navigable, and contain no historical/change commentary | PASS for recipe tests, actual survival GUI craft/exact consumption, iron rejection and crystal page at scales 2/3, using injected materials. Source restores 1,000 charge and direct feeding; exact direct/drop consumption remains runtime-open. WhiteCrystal helper is prepared, not executed evidence. Natural acquisition remains open |
| N1 | Empty-node crosshair readout `No Aura`; Part 1, 02:33 | Aim at an empty node, add crystals, observe correct color/count updates without rejoining, then observe transfer changes | PASS for bounded empty/White Aura live synchronization and sampled pump HUD at scales 2/3; additional colors and scale-3 node readouts remain unverified. See node-pilot-playtest |
| N2 | Node/pump visual form and active transfer streams; Part 1, 02:23-06:14; baseline showed wrong selection and occlusion | Selection/collision match the shipped model; supporting faces remain visible; feedback occurs on actual transfer rather than idle decoration | PASS for representative node/pump collision and occlusion, White transfer particles and idle/inhibited controls. Geometry test follows all 17 family models. Other colors/variants and exact legacy animation cadence are not claimed |
| N3 | Pump readout includes remaining time and power; Part 1, 06:14 and 08:22 | Fuel a survival pump; read remaining fuel time and rate; verify decreasing time and inactive state | PASS for ordinary coal 320/300, no-target preservation, empty-target spending, external redstone and corrected adjacent-receiver flow; sampled relaunch persistence passed. Eligible attempts, not unconditional wall time. Other fuel/trigger families have source/unit evidence but still need live coverage |
| C1 | Consumer progress, required power, last power; Part 2, 04:55 | Aim at running/idle processor and compare HUD values with server state | PASS for ordinary fueled processor HUD/server agreement and White Arcane Ingot output on the earlier snapshot. Seventh seeded completions cover the ten additional machine families listed above, including Brewer output motion/pickup delay and Prism inputs. This does not accept every HUD, natural full cycle, or stochastic distribution |
| V1 | Vortex shows recipe identity and distinct pedestal requirements; Part 2, 06:14 explicitly labels `Power received`, including 20,000 and 60,000 White | Set up a four-pedestal recipe and observe each input/color/required amount and progress, then actual output | PASS for asymmetric 60,000/20,000 White requirements, partial receipt counters, actual gem output and wrong-color rejection. Yellow looter recipe partially powered and saved; reload/completion, manual exchange and restored renderer remain pending. See core-systems playtest |
| P1 | Crystals, pump/falling-power circuit, dropped-item processing; Part 1, 01:16-07:38 | Acquire/craft a survival circuit, fuel it, generate falling power and process input using only in-game guidance | Earlier seeded creative-pump fixture passed only the bounded processing flow; survival acquisition remains unverified |
| P2 | Colored ingots -> vortex gems -> prism -> Angel's Steel; Part 2, 04:06-10:54 | Compare each recipe with 592, then complete each production step with the guide and no injected power | Source recipes restored. Earlier White gem used real falling aura; seventh seeded Prismatic Processor consumed eight distinct gems for one Prism and Synthesizer produced tier-1 Angelsteel. These are actual outputs, not a completed unaccelerated guide-led chain |
| E1 | Kaleidoscopic color inputs, combinations, failure chance; Part 2, 14:16-16:34 | Valid enchantable tag loads; intended tools accept enchants; actual enchanter and color-pair effects work in-world | Source/test hooks and tag correction integrated. Seventh seeded positive consumed Red crystal and applied custom Red enchantment; unpowered and powered-without-crystal negatives passed. Player mining/combat/drop callbacks, veto and last-durability controls remain open; post-seventh Fortune/damage repairs are not validated by these machine tests |
| R1 | Nether ritual's spreading block changes; Part 2, 16:35-19:58 | Version-matched conversion fixture, boundaries, persistence and safety checks | Seventh independent seeded Nether and End conversions passed mapped blocks, center biome, outside control and self-removal; unpowered controls passed. Lower-edge block-position biome probes failed, but parent reproduced the same sampling discrepancy with vanilla fillbiome; not currently a proven ritual defect. Reload/extent remain unverified. See core-late-game and integrated-restoration reports |
| S1 | Coordinator contents browser with item count 128, search field, scrollbar and inventory slots; Part 3, 02:49 | Browse multiple stored item types, filter by name, select a desired item and retrieve the correct amount; verify totals after reload | Fifth ordinary opening, selected 128-item and component-distinct retrieval, power accounting and reload passed. Seventh slot outlines/counts/thumb/tooltips at scales 2/3 and stale-spectator rejection passed, resolving those failures. Search/no-results/clear remains tool-blocked, not a demonstrated mod failure. Dead-menu variant not exercised |
| S2 | Shelf conversion, network visibility and power readout; Part 3, 01:10-03:55 | Convert a bookshelf, insert/remove books, connect visible shelves, power the coordinator, and verify disconnected/blocked/powerless cases | Seeded contents/power with ordinary actions: deposit, component-sensitive retrieval, zero-power negatives and saved counts/power passed. Shelf conversion and broken/disconnected-link gating remain runtime-open; no generated-power or naturally acquired storage claim |
| F1 | Ring of Binding in a Baubles slot; Part 3, 05:43; narrator describes 15 fairy slots | Show equipped/bound state and role; bind/release intended count; verify persistence and role action | Source/test authorization and stale-role rejection are repaired (six AuraFairyEntity tests passed in seventh build). Seventh ordinary default charm binding consumed the charm, showed 1/15 and one owned entity, and persisted through reload. Release input never established server sneaking; no confirmed release bug. Role effects, capacity/lifecycle and owner synchronization remain open; Shooter cache is post-seventh source/test-pending |
| A1 | Accessories category list and return to icon index; Part 3, 00:41-00:53 | Locate each accessory's operating instructions and recipe, follow links and return | Implemented, not planned-only: guide mapping covers accessory/role recipes and instructions. Independent quest-to-loadout link/back passed; all 14 quest pages passed scales 2/3. Seeded goal/reward persistence and restarted questline=false rejection passed. Whole-book navigation is not implied |
| A2 | Red protection amulet demonstrated in lava with survival hearts; Part 3, 12:30 | Measure health/effects with and without the equipped amulet; repeat separately for other hazard/color combinations | Seventh independent six-family synthetic damage checks passed with real physical equip/unequip and exact health controls; Red real placed-lava contact healed with regeneration disabled. Not natural acquisition, armored Yellow ordering, or all real-world hazards. Later damage-hook edits require targeted regression |
| U1 | Portable red hole emits repeated explosion clouds when dropped; Part 3, 09:46-10:15 | Observe repeated real explosions, item persistence and bounded radius/timing in an isolated fixture | Source contract restored: 100-tick cadence, strength 12/fire, 30,000-tick item lifetime. Actual repeated blast and persistence remain unverified; sparse video frames do not establish radius/damage |
| U2 | Prismatic Wand mode/paste demonstration; Part 3, 10:25-11:36 | Select/copy live source coordinates, paste at player-relative origin, skip occupied destinations and charge only successful safe placements; 592 has no undo mode | Source/test repaired: seven seventh-build tests passed for bounded volume, unsafe/protected regions, adventure rejection, live source, inert old snapshots and component-rich material protection. Ordinary input, placement and material conservation remain runtime-open; not a still-unimplemented clipboard |
| R2 | Shattered Stone ring demonstration conflicts with the port README; Part 3, focused 13:35-13:56 frames show ores remaining while nearby stone is lost | Compare ordinary terrain versus ore blocks in a controlled explosion, with/without ring and at measured distances | 592 contract resolved and source/test restored: wearer within the explosion's three-block AABB causes nonterrain blocks to be spared, while listed terrain remains destructible; no wearer damage immunity. Controlled actual explosion remains unverified. See shattered-stone contract |
| T1 | Rebounding Enigma lift and Traveler's Bricks traversal; Part 3, 12:42-13:09 and 17:10-17:29 | Reproduce launch/traversal through actual player input, check collision and controlled stop/landing | Exact source contract restored: 0.8 collision height/full light, direction-preserving road boost and vertical launch 10 without invented fall immunity. Geometry and motion tests pass in the 183-test build; actual player traversal/landing remains open. See 2026-09-23-traversal-contract.md |
| T2 | Swords, mirror, sash and utility items; Part 3, 06:10-08:37 and 14:01-18:30 | Bounded per-item effect tests with controls and persistence checks | Source contracts/hooks restored, not placeholder-only. Wing equip/use versus inventory-only negative and equipment death/reload passed; six sword models passed display only. Actual sword/Mirror/Heels/food effects remain open. Nested Fortune leak and rejected-hit combo timing have post-seventh repairs awaiting tests/live controls |

## Historical Finite Release-Blocking Session Matrix (pre-thirteenth snapshot)

This B1-B5 queue is retained as a pre-thirteenth scope snapshot, not a live
status table. Apply the current evidence checkpoint and overrides above: in
particular, bounded storage search/release and B5 owner/fairy synchronization
have passed, as has the specific S2 stone-entry/power reload case. Broader
storage/circuit reload and other untested cases remain open.
Fixtures may seed raw materials or initial states, but must disclose that fact
and must not seed the positive result being accepted. A helper's parse or
command acknowledgement is not a runtime pass. Do not rerun already-accepted
families merely to raise coverage.

**Boundary:** this table is a pre-thirteenth scope snapshot and is not the live
queue. Later evidence and its exact boundaries are summarized in the current
overrides above; the finite next checks are listed under Current Beta Gates.
The parent-owned build/release gate remains separate and authoritative.

| Session | Finite blocking scope | Exit evidence |
| --- | --- | --- |
| B1: Core power and progression | Current 1,000 crystal direct/drop accounting; Black/Orange manipulator toggle, readiness Monitor, Red absorption/lift/no-damage, Orange parallel transfer and capacitor control. One guide-led ordinary-fuel progression route through ingot/gem/Prism/Angelsteel, including saved vortex receipts and normal pedestal exchange. | Server before/after and negative predicates for controls; actual outputs with no seeded progress/power for the progression route. State which raw inputs were supplied and which were naturally acquired. Earlier fueled White route and seventh seeded machine completions remain valid bounded evidence, not substitutes for this route. Prepared node-controls helper is only part of this session |
| B2: Storage, guide and fairy lifecycle | Actual search/no-results/clear after input bridge repair; normal shelf conversion and disconnected/powerless network behavior. Real sneak-release, capacity boundary and rebind/reload/unequip cleanup; representative action and Digger/Glider hook effects with ring-off controls. Read the changed progression/role instructions while doing these tasks. | Ordinary input plus exact item/component conservation and no orphan/duplicate role action. Browser visual/opening failures are already resolved; do not restart the 128-entry or all-quest audit. Source guard tests and one default Fairy binding do not prove action hooks or release |
| B3: Mining, combat and equipment hooks | E1 mining/drop and representative paired effect; break veto and final-durability controls; combined Angelsteel/Kaleidoscopic Fortune leaves held components unchanged. Accepted versus rejected damage must update Barbarian combo correctly. Finish versus cancel food use; bounded sword/transmutation, Mirror and wearable movement activation/control, including road/Enigma landing. | Real player callbacks, observable effect/drop/health changes and negative controls, not direct helper calls. The confirmed Fortune persistence and damage-phase defects must be closed on the new binary. Sixth-family amulet synthetic passes need only targeted changed-hook regression, not an exhaustive new hazard matrix |
| B4: World edits and destructive machines | Wand selection/paste with live source, occupied/protected or unloaded destination and material-loss controls; Red Hole repeated blast and Shattered Stone terrain/ore contrast. Miner nonzero earned charge/yield and stale-helper cleanup; one ritual save/reload plus same-target negative. | Actual world changes/no-change controls and conservation, with isolated disposable fixtures. Do not count seeded Miner yield or injected ritual outputs. Charge-1 Miner motion/containment and both ritual center conversions are already accepted; the vanilla-reproduced biome edge is not a separate proven release defect |
| B5: Candidate multiplayer/persistence | On the reviewed packaged candidate, two-client owner-only accessory/fairy synchronization and no duplicate effect after owner leave/rejoin; representative storage and active circuit save/reload. | Observe both clients/server state on the identified artifact. Seventh packaged server startup/save/shutdown is already smoke-PASS, not multiplayer acceptance. Final artifact/build/publication work stays in the parent release gate |

### Resolved implementation failures and limits

- Coordinator empty-hand dispatch, browser slot/count/thumb/tooltips, invalid
  guide icons and the fairy-light missing-model warning have corresponding
  independent runtime corrections. Fairy-light warning absence is not torch-action QA.
- Fairy canonical UUID authorization, live-ring rejection/cleanup and wand
  bounded/preflight/material guards have source and seventh passing unit evidence;
  their remaining lifecycle/placement checks are runtime acceptance, not "under repair".
- Flux is no longer a production NOOP: `AuraFluxEnergyBridge` uses the bundled
  Energy API, with ten passing seventh transfer tests. A real compatible receiver
  was not exercised. No named external-mod support is accepted by those tests;
  treat optional receiver/tag interoperability as a beta limitation, not an
  invented missing original machine or a blocker demanding another modpack.
- Modern nearest-Enemy targeting, level RNG, Pusher's correct 0.4 impulse,
  Stealer's successful-spawn-before-consume safety and soul-fire handling are
  adaptations, not release-blocking missing roles. Modded fire-material coverage,
  every stochastic distribution, all enchantment pairs, every pump/fuel/biome
  permutation and exhaustive external-mod combinations remain residual beta QA.
- No new source-proven stand-in/no-op progression machine is established by
  these reports. This does not declare full parity: the known post-seventh safety
  fixes and B1-B5 acceptance exits remain open. No uncompiled patch inherits a pass.

## Important evidence boundaries

- The three base videos total about 58 minutes. Their full caption timelines and
  all feature sections are covered by the reports, with sampled frames and focused
  follow-ups. This is not a claim that every frame was watched continuously.
- Part 2's late F3 overlay identifies Minecraft 1.7.10. That anchors the game version,
  not the exact Aura build or every behavior in the other two videos.
- The strongest visible targets are the book's navigable/illustrated pages, node
  and machine readouts, unequal vortex counters, and the storage browser.
- The recovered source contracts now supplement video recipe/timing information.
  Complete enchantment combinations, every role action and external-mod behavior
  are not established by sampled runtime reports. Save/reload passes apply only
  to the specific storage/equipment/quest/binding fixtures described above.
- Failed or ambiguous demonstrations must remain labeled as such. The videos
  provide a player-experience reference, not automatic acceptance of the port.

## Delivery rules

- Preserve the dirty working tree and prior audit evidence. The user authorized a
  beta on the existing CurseForge project only after the full port is finished;
  follow [the release gate](2026-09-22-beta-release-gate.md), not historical closure claims.
- Retain explicit distinctions between visible behavior, narration, source evidence,
  inferred requirements, current implementation, and live verification.
- Build and playtest sequentially with the measured QA wrappers. Their process
  memory guards are not a guarantee for the entire desktop/agent workload.
- The pilot and B1-B5 matrix are historical scope snapshots. Use the current
  checkpoint above to trim accepted subchecks, and retain each row's
  source/test/runtime boundary when recording a new result.

## Historical pre-pause change inventory

The encyclopedia and node workers confirmed they made no file edits and started
no builds, tests or game processes. They were stopped and closed. Before the pause,
the following narrowly scoped edits were already made and have not been reverted:

- `src/main/resources/data/aura/tags/item/kaleidoscopic_enchantable.json`: replace
  the missing 1.21.1 `enchantable/melee_weapon` tag with `enchantable/weapon`.
- `src/test/java/pixlepix/auracascade/parity/KaleidoscopicEnchantableTagAuditTest.java`:
  new recursive vanilla-tag resolution and six-enchantment consistency checks.
- `scripts/qa/measure-client.ps1`: optional validated `Label` parameter to preserve
  separate playtest output folders. PowerShell parsing passed; no client was launched.

No Gradle build, Java client, or gameplay test was run during this video-reference
turn. Earlier baseline results do not validate these new edits.
