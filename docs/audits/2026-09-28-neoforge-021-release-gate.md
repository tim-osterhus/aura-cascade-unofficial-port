# NeoForge 0.2.1 Initial Beta Gate

Status: APPROVED for the initial bounded 0.2.1 NeoForge beta. Runtime and independent review passed. Upload has not yet been attempted.

Current candidate: `f2bc9d9c6b73e59004f5e582196f850f1e4bfc5bd0515a299a5201ead0605f0b`.
The second fairy fix defers reconciliation until the owner's chunk is entity-ticking
and keeps orbit positions in entity-ticking chunks. The earlier `isLoaded` check
was insufficient during dimension transitions. `neoforge-021-fairy-fix-r2` passes
265 tests and all helper builds, exit 0, peak working/private 1238.4/1399.9 MiB.
Final-hash runtime repetitions passed; earlier c935 evidence below is
retained rather than relabeled. Source checkpoint `6d50bfc` is pushed to
`origin/neoforge/1.21.1` as Tim Osterhus <tim@millrace.ai>.

## Final Candidate Evidence

- Final post-documentation regression run `neoforge-021-final-regression`:
  265 tests, zero failures/errors/skips, exit 0. Peak working/private allocation
  1109.4/1280.6 MiB. The frozen production JAR hash was unchanged afterward.
- Production archive: 1,448 entries, 196 recipes, 41 guide entries; no QA-helper
  classes or Fabric metadata. All entry contents except the four `FairySystem`
  class files are byte-identical to c935. The artifact remains frozen at the
  full SHA-256 above; helper-only rebuilds do not replace it.
- Six packaged client runs `neoforge-021-final-{creative,guide2,guide3,hud,placement,animation}`
  all exited 0 without timeout or memory stop. Independent review inspected all
  19 PNGs and accepts the bounded scenes. Creative matches 161 registered item
  IDs; both guide scales audit 41 entries/371 pages and click Getting Started
  plus Back; HUD remains targeted for 120 render frames with five captures;
  placement visibly connects empty nodes; Smelter output has visible cyan
  reaction particles. Highest client-run private allocation: 1895.7 MiB.
- Publisher validator: 18 tests pass. All seven native PowerShell scripts parse.
  `neoforge-021-runner-retry-unit` verifies transient read retry and propagation
  of genuine probe errors. Source/secret scan found no flagged literals in the
  40 reviewed paths; no generated runtime files or credentials were committed.
- Multiplayer `20260928-152948-557`: all 31 state/action checks passed, including
  owner-private attachments, equip/bind, logout/rejoin, unequip/reequip, Nether
  round trip, both keep-inventory death/respawn modes, exact bound-ring drop and
  pickup, and real unpowered bookshelf-menu synchronization. Fairy maxima stay
  at one. Four sequential JVM lifetimes exited 0, with at most three concurrent
  owned games. Peak working/private allocation: 2642.1/3167.7 MiB. Owner fairy
  visuals are clear; witness receipt is state-verified, not clearly visible in
  its captures. No bookshelf retrieval packet was sent.
- `neoforge-021-final-hooks-energy`: all five native hooks and eight energy
  controls passed, including the external Energy Meter release. Normal stop,
  exit 0; candidate hash matches. Peak private allocation 798.9 MiB.
- `neoforge-021-final-progression` and its `-evidence` directory: genuine
  unpowered control plus full White Arcane Ingot cycle passed after 1450 total
  ticks (1410 progression), exactly one output. Normal save/stop, exit 0;
  peak private allocation 804.2 MiB.
- `neoforge-021-final-disk-{prepare,check}` and `-evidence`: preparation PID
  23768 and check PID 26708 have distinct start markers and the same final JAR
  hash. Read-only check reports PASS and `persistenceVerified: true`; saved aura,
  book contents and coordinator snapshot match without reseeding. Both exit 0.

The authorized target is the initial 0.2.1 NeoForge beta, not canonical 0.2.2 repairs. Preserve the 0.2.1 gameplay baseline and fix loader regressions here. Import canonical 0.2.2 repairs separately when that handoff is supplied.

## Session Ownership

- Global maximum: four Minecraft JVMs, allocated as no more than three clients (including integrated-server clients) plus one dedicated server.
- All owned Minecraft processes exited normally after final acceptance. All client and dedicated-server reservations were explicitly released to Mod Playtesting at completion. No further Minecraft launches are planned for this publication.
- Count actual processes immediately before every launch. Unknown Java game processes consume a slot until classified. Coordinate launches and stop only processes owned by the launching workflow.
- Keep worlds, ports, and outputs distinct. No OBS use; framebuffers supply this workflow's visual evidence.

## Required Checks

- [x] Final f2bc candidate regression/build and QA helper packaging: 265 tests, no failures, errors, or skips.
- [x] Final production archive: 1,448 entries, 196 recipes, 41 guide entries, no QA classes or Fabric metadata; SHA-256 unchanged.
- [x] Fresh-world runtime state/actions: creative Aura tab contents and Encyclopedia open, hyperlink, back, and layout checks passed.
- [x] Independent bounded visual review of all six final-hash scenes and 19 PNGs. Immutable capture manifests retain their original pending-review status; the independent-review document records the verdicts.
- [x] Full two-client owner-only sync, equip/bind/unequip, reconnect, death/respawn, dimension and inventory controls.
- [x] Real accessory/bookshelf menu interaction and inventory conservation. Powered bookshelf retrieval remains outside this gate.
- [x] Final-hash White Arcane Ingot cycle and genuinely unpowered control.
- [x] Disk prepare, normal stop, distinct-process restart, and read-only state/content comparison passed.
- [x] Native empty/full/partial/rejecting/sided energy receivers, four-receiver limit, and external Energy Meter passed all eight checks.
- [x] Reviewed inherited defect disclosure and remaining coverage limits; canonical 0.2.2 repairs are not included.
- [x] Astra reviewed the final source correction, UI, multiplayer and all final native results within their stated bounds; no concrete blocker found.
- [x] Reviewed source checkpoint pushed, release notes/metadata reviewed, and offline upload validator passed for the exact frozen artifact. Publication bookkeeping follows in the final release commit.
- [ ] CurseForge beta upload, returned file metadata, and public availability check.

## Known Baseline Risks

Fabric 0.2.1 has confirmed Breeder juvenile/duplicate-offspring defects and a Binding Ring costs hyperlink pointing to the wrong guide entry. Reproduce these on NeoForge or label them inherited/unverified; do not claim they are repaired here.

Unexpected data loss/duplication, broken core progression, networking failures, or critical loader regressions block this beta. Bounded acceptance is not exhaustive Survival/modpack validation or a guarantee of bug-free behavior.

## Candidate And Build Evidence

Initial candidate: `aura-cascade-neoforge-0.2.1+1.21.1.jar`, SHA-256 `04f1a0f860b2f19d3b1113c17f2ce58070193eaca1e2e86c771aab798817ad5a`.

- `build/qa-audit/neoforge-021-baseline-20260928`: bounded `test build qaObserverJar qaMultiplayerJar`, exit 0; 263 tests, no failures/errors/skips. Peak owned working set 1202.6 MiB; private allocation 1361.1 MiB.
- Initial candidate archive inspection: 1,448 entries, native metadata, 196 recipe files, 41 guide entries; no QA classes or root Fabric metadata.
- `build/qa-audit/neoforge-021-hooks-20260928`: actual packaged dedicated server; native damage, Red Hole lifetime, Thief drops, bonus-loot cancellation, and harvesting-isolation fixtures passed. Normal save/stop, exit 0. Peak owned working set 614.7 MiB; private allocation 592.8 MiB.

Previous revised 0.2.1 runtime candidate: SHA-256 `c93520929bb5b2e92ccde76e44204610a7563ee878d7adcf4ea37adb09b59d26`. Runtime reports below identify this regular packaged Aura JAR through `ModList`.

- `build/qa-audit/neoforge-021-fairy-fix-r1`: `test`, `build`, `qaMultiplayerJar`, `qaEnergyJar`, and `qaPersistenceJar`, exit 0; 265 tests, no failures/errors/skips. Peak owned working set 1234.2 MiB; private allocation 1432.3 MiB.
- Earlier development evidence is recorded separately in `2026-09-28-neoforge-1211.md`; it is not relabeled as release-candidate testing.

## Previous Candidate Runtime Evidence

The following notes retain the status at their earlier capture checkpoints.
Later independent review and the final-candidate evidence above supersede their
pending-review wording; they are not additional open release gates.

- Client creative r2: `build/qa-audit/neoforge-021-ui-creative-r2/manifest.json` reports `passed`; the inventory key opened the Aura tab and its live menu matched all 161 expected item IDs. The packaged client exited 0. Screenshot verdict remains pending manual review.
- Guide r2 at GUI scales 2 and 3: `build/qa-audit/neoforge-021-ui-guide2-r2` and `ui-guide3-r2` report passed. Real item use opened the book, the rendered Getting Started link navigated, and Back returned to the landing screen. Each full layout audit covered 41 entries and 371 pages with zero failed pages or glyph overflows. Both clients exited 0; visual review remains pending.
- HUD r2: `build/qa-audit/neoforge-021-ui-hud-r2/manifest.json` captured the target HUD across 120 consecutive rendered frames. This is observed evidence, not a completed visual verdict.
- Placement preview: r1 targeted the diagonal cell `(3,-60,3)` and is not valid visual evidence for the intended aim. R2 targeted the corrected `(3,-60,2)` cell and captured three preview frames after normal placement; visual review remains pending.
- Conversion animation: `build/qa-audit/neoforge-021-ui-animation-r1` observed natural Smelter conversion of one Raw Iron into one Iron Ingot without outcome injection and captured three reaction frames. R2 also captured the natural conversion. Independent visual review remains pending for both.
- White progression: `build/qa-audit/neoforge-021-persistence-evidence-r3/white-full-result.json` reports PASS after 1,451 total ticks. The 40-tick unpowered control retained one iron and one wool with zero pump fuel, aura, power, progress, or output. The real eight-crystal/coal progression observed a five-block lift, falling power, processor progress, conserved 8,000 White aura, and exactly one ingot. `build/qa-audit/neoforge-021-progression-runtime-r3/summary.json` confirms the expected result, normal stop requested, exit 0, and no runner failure.
- Disk persistence: `build/qa-audit/neoforge-021-disk-evidence-r1/persistence-check-result.json` reports PASS and `persistenceVerified: true`. The prepared 3,000-White-aura isolated node, scanned zero-link state, named MOD storage book, two stored item types/five items, coordinator network/browser snapshot, and empty loose-item count all matched after restart. Prepare/check summaries both report normal stop and exit 0; process PIDs and UUID markers differ, and both loaded Aura JAR hashes match the current c935 hash. The expectation correctly remains a pre-restart record with `persistenceVerified: false`.
- Energy runtime r3: `build/qa-audit/neoforge-021-energy-runtime-r3/energy-result.json` reports PASS for all eight cases: empty, full, rejecting, partial, sided, four-receiver limit, over-four rejection, and external Energy Meter. The run summary confirms the expected result, normal stop, exit 0, and the current c935 Aura JAR hash.

## Retained Failed And Invalid Trials

These historical failures are retained for provenance. Their release holds were
resolved by the final full multiplayer/UI/native runs above; none is being
silently counted as a successful run.

- Final-hash multiplayer `20260928-152451-554` passed all lifecycle/conservation
  checks but attempted the final coordinator interaction before its block update
  reached the client. The fixture now waits for an actual read-only client block
  observation. Full run `20260928-152948-557` then passed all 31 checks normally.

- Final-hash multiplayer `20260928-150601-466` passed the Nether round trip with maximum fairy count one, but its death check could not observe the dead local player after vanilla removed that entity from `ClientLevel.players()`. The client probe now includes `client.player` if missing from that list, without changing gameplay. All JVMs exited normally.
- Final-hash multiplayer `20260928-151217-514` also passed the round trip, keep-inventory death/respawn, and the exact bound-ring drop with keep-inventory disabled. Phase archiving hit a transient null read during atomic JSON replacement. `Save-Phase` now retries only null reads for at most two seconds; genuine probe/duplicate/hash failures still throw. All JVMs exited 0. The full run still needs completion. Probe digest is now pinned before launch, not reread from a possibly rebuilt source helper at shutdown.

- Multiplayer `20260928-145322-812` on c935: real equip/bind, private synchronization, logout/rejoin, unequip/reequip passed. Dimension travel again produced a peak of three fairies on both server and owner client, settling to one by the last published snapshot. All four sequential JVM lifetimes exited 0; maximum concurrent owned JVMs was three. Peak owned working/private memory was 2556.7/3070.1 MiB. This remains a release blocker. The helper now retains the exact peak snapshot because periodic JSONL output missed the transient overlap.
- Hooks r2 on c935: four controls passed but harvesting refused occupied fixture space left by the progression setup. Hooks r3 on the separate native server passed all five with normal save/stop and exit 0. This was an invalid fixture location, not a product failure.
- Independent final visual review accepts placement r2 and animation r2. Animation r1's output/reaction was occluded and remains insufficient visual evidence; the r2 raw input was placed on the visible side without seeding outputs. HUD review covers five captures across value changes and 120 target-stable render frames, not pixel inspection of every frame. Only Getting Started navigation was clicked at each guide scale. Powered bookshelf retrieval and Angelsteel-specific reactions remain outside these bounded checks.

- Multiplayer `20260928-135750-191`: evidence-writer failure from a Windows sharing lock on `latest.json`; all three JVMs exited 0. The runner now opens live reports with shared read/write/delete access. Harness failure, not a gameplay finding.
- Multiplayer `20260928-135935-868`: owner-only synchronization, real equip/bind, logout/reconnect, and unequip/reequip passed. Nether transfer then observed multiple actual entity objects with the same owner/slot (maximum three; two in the final snapshot). Diagnosis and correction/retest are still required. All owned JVMs exited 0. No canonical Fabric behavior is inferred from this NeoForge-only trial.
- Energy runtime r1 did not reach ready and did not meet its runner expectation; it is superseded by energy runtime r3, whose eight checks passed.
- Persistence helper compile r1 reported six White fixture placement argument-type errors. The call sites were corrected; persistence helper compile r2 exited 0.
- White progression evidence r1 failed the actual unpowered control after 40 ticks: the nearby burning pump consumed the White Wool as furnace fuel (the report retained the iron and showed 18 pump fuel attempts). This was a fixture-geometry failure, not a production progression finding. The wool was moved outside the pump fuel scan while remaining in processor range; the recipe is now explicitly checked in range before and after control.
- White progression r2 produced a PASS result JSON, but `build/qa-audit/neoforge-021-progression-runtime-r2/summary.json` records `startup_command_or_shutdown_timeout`, exit `-1`, and no normal stop. The parent runner used an incorrect process-match regex; do not count r2 as a clean process-lifetime acceptance. Correct-pattern r3 completed and exited normally as recorded above.
- Creative r1 failed its creative-screen precondition; creative r2 passed the live tab/item-set assertion. Guide2 r1 opened and audited the guide but its hyperlink click was not accepted; guide2 r2 and guide3 r2 passed navigation/back and layout checks. HUD r1 ended with a render-capture exception; HUD r2 captured the required frames, pending visual review.
- Placement r1 remains captured but visually invalid because its target was the diagonal cell; corrected-aim r2 is the usable capture and still needs review. Animation captures are outcome-observed, not yet visually accepted.
