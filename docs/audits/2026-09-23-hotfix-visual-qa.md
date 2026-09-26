# Aura 0.2.1 Targeted Visual QA

Date: 2026-09-23. Status: TARGETED LIVE QA COMPLETE; both clients saved and closed.
Scope: requested Astra Light / low-effort, five-target independent visual pass.
No Java, Gradle, client, server, GUI interaction, or live bridge call was started
during preparation. No product source or tests were edited.

## Runtime Gate

Parent's latest staging handoff (runtime NOT yet granted): final build running;
copied disposable world is build/qa-audit/hotfix-021-world/saves/hotfix-021.
Use a verified unoccupied private test area near (4096, 180, 4096), not the
copied core-systems fixtures. Parent will launch measure-client.ps1 with
-Label hotfix-021-client -GameDirectoryName hotfix-021-world
-Username AuraHotfixQA, 1280 MiB heap and 3800 MiB sampled stop threshold.
After grant, auditor must enter that copied world from title via bridge/GUI.
Parent will leave the first creative opening untouched. Obtain PID and exact
candidate identity at handoff. No full survival progression repeat is in scope.

- Parent must grant sole runtime/input ownership explicitly and identify the
  candidate artifact/version/hash, client PID, disposable world, bridge readiness,
  and memory-monitor owner. Prefer handoff of an already-running client.
- Serialized budget: 4.5 GB excluding Codex; no concurrent build/server/client
  work. Include QA helpers in accounting. Existing client launchers use a
  1280 MiB Java heap and default 3800 MiB sampled stop threshold; this is not an
  OS hard cap or proof of combined budget compliance.
- Do not invoke launchers or stage dependencies before authorization. Parent
  owns staging outside this auditor's two permitted output locations.
- Read computer-use guidance/API/confirmation documents before GUI control;
  the top-level computer-use skill was read during preparation. Approved
  game-bridge introspection can corroborate state, not replace visual evidence.

## Inspected Workflow

Read README.md and PORTING_NOTES.md, existing node-pilot bridge helper,
scripts/qa launch/measurement tooling, node and guide pilot reports, sixteenth
release validation, and natural-progression fixture instructions. The worktree
already contains extensive changes; they are outside this auditor's ownership.
The existing bridge uses authenticated loopback port 9876, POST /api/cmd and
GET /api/screenshot. Its original helper writes into the old evidence folder;
use the private hotfix helper instead. Never print or copy credential contents.

## Execution And Evidence Plan

All results below are NOT RUN. Evidence goes exclusively under
build/qa-audit/hotfix-021/. Record actual frame size, GUI scale, timestamps,
candidate hash and fixture provenance. Open and inspect captured images.

| Target | Procedure and controls | Planned evidence |
| --- | --- | --- |
| Dropped Angelsteel / ground recipe | Supply materials in disposable fixture; physically drop three same-tier ingots, observe grounded conversion into exactly one next-tier ingot and its burst. Separately observe actual consumer recipe completion and its completion visual, plus an incomplete-input negative control. Do not inject particles or recipe output. | 01-drop-before, timed during frames, after/output state, separate consumer-completion sequence; report supplied materials separately from earned output. |
| Newly placed empty-node preview | Physically place empty node near an empty peer with clear path; capture the initial preview and expiration. Repeat with a solid block physically obstructing the link, then with an unobstructed path hidden behind a foreground wall from the camera. Wait beyond the observed/source-confirmed preview lifetime and look away/back to test one-shot behavior. | 02-clear-sequence, blocked-path, camera-occluded, expired, look-back; confirm both nodes empty through read-only state. |
| Creative tab | Preserve the first creative opening of a fresh client session for this check: it must directly select Aura Cascade Reimagined without search or page advance. Page index zero is NOT required. Inspect every scroll row and sample item tooltips. Compare live displayed item IDs against live registered Aura item forms, reporting missing/extra IDs and duplicates; conversion-only blocks without item forms are not omissions. Select another tab, close and reopen inventory, and verify that selection remains; repeat to ensure Aura is not forced on later openings. | 03-first-open-aura, tab tooltip, all rows, runtime item-set comparison, chosen-other-tab and retained-on-reopen. Source counts alone are not full live coverage. |
| Aura HUD | Aim steadily at an actually cycling node through multiple real update cycles and zero crossings. Once a color has appeared, verify its row remains at truthful zero rather than disappearing/reappearing while inspecting that same node; corroborate values with read-only runtime state. Place a nearby pickable entity in the sightline between player and inspected block, comparing HUD continuity with the entity absent/present. Inspect a short continuous observation/frame sequence, then look away/back and inspect another node to check stale values are not carried across targets. Repeat scales 2 and 3 where practical. | 04-active-zero-crossing-sequence, entity-absent/present, scale confirmations, away/back and other-node; report duration/cadence and missed-frame limitations. Sparse screenshots cannot prove zero flicker. |
| Encyclopedia landing | Open book through normal use and navigate to landing screen. Inspect all nine links for clear separation and clipping at scales 2 and 3; click each link from the landing page and verify its intended destination, returning between clicks. | 05-landing-scale2/3, nine destination captures and label-to-destination ledger; acknowledgements alone do not prove clicks worked. |

## Prepared Commands

Parent clarification while runtime remains pending implementation/build:
Fabric 1.21.1 intentionally places mod tabs on a later creative page. Planned
implementation uses public FabricCreativeInventoryScreen through AFTER_INIT to
select Aura only on the first creative opening per client session. Acceptance
is initial selection and subsequent user-selection retention, not page index.
Parent also plans a block-only HUD raycast instead of entity-sensitive hitResult
and retention of previously seen color rows at truthful zero for the same node.
These are reported implementation intentions, not independently verified results.
Parent subsequently reports nine distinct landing links in final source and
preview handling moved to the common network implementation, including pumps
and pedestals. Keep the primary node controls above and add a bounded freshly
placed pump/pedestal preview spot-check; source scope alone is not visual proof.
Parent must hand off a session whose first creative opening has not been consumed,
or explicitly authorize a fresh serialized session for that check.

From the repository root, only AFTER explicit grant:

```powershell
# Capture only; does not launch a runtime.
& ./build/qa-audit/hotfix-021/bridge.ps1 -RuntimeGranted -Shot '00-baseline'
# For inspected bridge commands, supply the observed command name and JSON.
# For game commands, use -Commands @('...') after confirming disposable world.
```

Do not guess bridge command schemas or GUI coordinates. Inspect the active
bridge/screen first. Use a verified unoccupied fixture area; avoid broad fills,
entity kills, inventory clearing, or changes to existing test stations.

## Results And Handoff

### First Candidate Live Pass

Authenticated dev client PID 30988, username AuraHotfixQA. Parent identifies
compiled 0.2.1 source as equivalent to JAR SHA-256
7de54b17c9ba6ce5ac61e0a6dc7d4a03444a071377bc69fb8a508801d202abd4.
This was a dev-namespace client, not an independent packaged-JAR visual pass.
Bridge confirmed the exact copied world path above. Framebuffer 1280x720,
GUI scale 2 (also reported by direct_scroll). Screenshots below were opened
and visually inspected, not accepted from API acknowledgements.

All evidence paths below are relative to build/qa-audit/hotfix-021/.

| Check | Result and inspected evidence |
| --- | --- |
| Creative initial selection | PASS: untouched first opening selects Aura Cascade Reimagined; 03-first-open-settled.png. Immediate earlier capture preceded the transition and is not the acceptance image. |
| Creative selection retention | PASS: select Saved Hotbars, close/reopen with E; 03-other-tab.png and 03-selection-retained.png show Saved Hotbars retained. |
| Creative visible coverage | Inspected top and overlapping scroll views through the bottom: 03-coverage-top.png, 03-coverage-scroll4/8/12/16.png. Later scroll requests remained at bottom. Tooltips obscure some cells. No live per-ID enumeration was available; full registered-item-set assurance remains the parent's deterministic test, not a claim of reading every live tooltip. |
| Landing spacing and clicks | PASS at scale 2: 05-book-open.png shows nine separate readable links. 05-link1-walkthrough.png, 05-link2-aura-systems.png, 05-link3.png through 05-link7.png, 05-link8-white-crystal.png, and 05-link9-first-circuit.png show all nine intended destinations. Scale 3 remains untested in this live pass. |
| Angelsteel ground effect | PASS bounded actual Q-drop: three supplied tier-one ingots become one tier-two ingot; no tier-one drops remain in the local box. 01-ground-burst-5.png shows gold burst and 01-ground-burst-9.png its decay around the output. Two prior drops visibly remain in 01-two-ingot-negative.png. The attempted exact count-two single-entity assertion failed because this did not establish that separate drops had merged; do not treat that assertion as a recipe failure or a passing count check. |
| Consumer completion effect | PASS bounded smelter completion: supplied raw iron changes to one iron ingot; tagged input gone. 01-consumer-unpowered.png shows initial zero progress/power, 01-consumer-5.png shows cyan completion particles, and later frames show output with effect finished. Adjacent node power was explicitly seeded to 10000; no output or animation was injected and progress was not seeded. This is visual acceptance, not earned-power progression. |
| Active HUD | PASS in inspected temporal samples: 04-active-1/5/9/13/17/21/25/29/32.png show continuous HUD through actual coal/crystal-driven transfers and changing White Aura values. Full sequence contains 32 captures with 150 ms requested spacing plus bridge overhead, about 11 seconds. These samples do not prove absence of every possible single-frame flicker. |
| Entity HUD control | PASS: actual inspect_mining_target reported EntityHitResult with the armor stand between player and node. HUD remains visible in 04-entity-1/4/8/12.png while particles continue. |
| Zero-row control | PASS for explicitly seeded zero only: 04-zero-seeded-control.png retains White Aura: 0 for the same inspected node. Natural cycling was independently observed, but a natural zero crossing was not captured; do not conflate these two checks. |
| Placement/link geometry and timing | DEFERRED on parent instruction after independent review found cached link timing risk. Incidental old-candidate preview particles are not acceptance evidence. Retest the corrected candidate. |

Landing destination ledger: Walkthrough -> Walkthrough; Aura Systems -> Aura
Systems; Storage and Gear -> Storage and Gear; Fairies -> Fairies; Enchantments
-> Enchantments; Late Systems -> Late Systems; Getting Started -> Getting
Started; White Aura Crystal -> White Aura Crystal; First Circuit -> First Aura
Circuit. Each was clicked from the landing screen, returning between checks.

Fixture notes: new floor uses keep at x/z4088..4110,y179; four chunks were
forceloaded. Circuit uses pump (4104,180,4100), upper nodes (4104,183,4100)
and (4105,183,4100), return node (4105,180,4100). Viewer platform y182,
x4103..4107,z4095..4097. Armor stand tag hotfix021_hud_entity remains.
Smelter (4092,180,4104), adjacent power node (4093,180,4104). Original copied
fixtures were not overwritten. World time/daylight and command-feedback settings
were changed for QA. An initial mistaken aura:aura_crystal summon resolved to
invalid Air; it was corrected to aura:aura_crystal_white before active HUD
sampling. The earlier 04-cycle series was idle and is not active-HUD evidence.

Normal shutdown: exit_control_mode, pause_game, re-enter control with pause
already visible, click Save and Quit to Title, inspect 06-title-confirmed.png,
then click Quit Game. Logs confirm all dimensions saved and Stopping; process
check confirms PID 30988 absent. Parent owns monitor result and peak accounting.
All auditor command sessions completed. Runtime/input returned to parent.
Parent subsequently confirmed monitor exit 0 at local 18:08:25 / UTC 04:08:25,
peak working set 1352.9 MiB and private allocation 1872.3 MiB. These are
parent-reported sampled monitor results, not an independently measured hard cap.

Earlier PID 27836 never entered a world: missing token prevented bridge startup;
Windows capture failed twice, and parent terminated that QA-only title client.
It contributes no visual acceptance evidence.

### Rebuild Handoff (Historical)

Parent is building hotfix-021-links-build; no runtime is authorized during that
build. Parent reports core UI/effect classes and resources unchanged. The above
live results remain attributed to the previous candidate hash, with carry-forward
conditional on that unchanged scope; they are not observations of the final JAR.
Parent owns the final-artifact packaged smoke and will provide its final hash.

No full final gate claimed. After explicit new runtime grant: node preview real
geometry and camera occlusion, one-shot expiration, bounded pump/pedestal check;
charged existing node must supply newly placed neighbor on next normal cycle;
blocked/inserted-node old path must not transfer. Also finish natural zero-crossing
HUD observation and scale-3 landing spacing if time permits. Preserve distinctions
between static source/tests, parent-owned packaged font audit, and live evidence.

### Final Candidate Node Pass

Parent-granted dev client PID 24452, hotfix-021-links-client, same copied world,
final candidate JAR SHA-256
2290a1a344fa8e1e43d631729b5f88d422923d573db90d1a592945fecc1aacc8.
Parent reports only network-link revalidation code and its new test changed;
the earlier UI/effect results retain their original-hash attribution. Parent's
252 tests / 62 suites are not substituted for the live checks below.

Preview fixture: empty peer (4090,180,4096), fresh source (4096,180,4096),
viewer (4093.5,180,4092.5). Initial source was placed by normal held-item use
after inspect_mining_target confirmed the floor block (4096,179,4096).
Later controlled replacements of that same QA-owned block used commands.
Preview capture ran at tick rate 2 to stretch the 12-tick transient; expiration
was checked after restoring rate 20. No aura or particle commands supplied
these preview effects. Both node White Aura stores were verified zero.

| Final control | Result and inspected evidence |
| --- | --- |
| Clear empty-node preview | PASS: moving cyan trace between empty peers; 07-clear-4.png, 07-clear-8.png, 07-clear-12.png. |
| Expiration / one-shot | PASS bounded sample: no trace in 07-expired-empty.png after normal ticking, or 07-lookback-no-replay.png after looking away/back. |
| Solid block interrupts trace | PASS: stone at (4093,180,4096) blocks the path; 07-blocked-2/5/8.png show no cyan trace during the new source's preview window. |
| Camera occlusion | PASS: foreground wall x4092..4094,y180..181,z4095 leaves the actual link at z4096 clear. 07-occluded-3/6/8.png show exposed endpoint particles but no particles drawn through the wall. |
| Pump spot-check | PASS: fresh burning pump emits cyan trace to peer; 07-pump-3.png and 07-pump-6.png. |
| Pedestal spot-check | PASS: fresh aura_node_crafting_pedestal emits trace; 07-pedestal-corrected-3/6.png. Initial mistaken vortex_pedestal ID was rejected; earlier 07-pedestal-* images are not acceptance evidence. |
| Reload does not replay | PASS in bounded startup sequence: Save and Quit, reopen same copied world, confirm world path. 07-reloaded-1.png is loading, 07-reloaded-2/3/5/8/12.png show the saved empty pedestal/peer without cyan replay. Rain is visible and distinct from the cyan trace. This is sampled startup evidence, not every render frame. |

Live transfer controls use private link-timing.ps1 and actions.jsonl, with
explicit seeded 1000 White Aura and freeze/step at normal rate 20. Every path
cell x4090..4096,y184,z4108 was checked air before fixture creation. These are
actual server state transitions, not unit-test outcomes or survival acquisition.

| Transfer timing control | Observed result |
| --- | --- |
| Existing scanned source, new neighbor immediately before normal cycle | At game time 141699 add receiver, step to 141701: receiver 328. Not the 200-tick refresh boundary. PASS. |
| Insert solid block after phase-0 plan, before phase-1 apply | Receiver remains 0 at 141721. PASS. |
| Remove obstruction | Next cycle receiver 328 at 141741. PASS. |
| Insert nearer node after plan, before apply | Far receiver remains 0 at 141761. PASS. |
| Following cycle with nearer node | Nearer node 419, far receiver 0 at 141781: no direct bypass. PASS. |
| Remove receiver after plan, before apply | Source retains 1000 at 141821. PASS. |

The script's initial parse-only attempt used a reserved PowerShell function
name and did not execute any game commands; corrected run completed all checks.
All script sessions finished. tick rate 20 and tick unfreeze were explicitly
restored before final save. Optional natural HUD zero crossing and scale-3
manual guide repeat were waived as non-blocking by parent; they remain unclaimed.

Final shutdown: inspected 07-final-save-menu.png and 07-final-saved-title.png,
then normal Quit Game. Log reports all dimensions saved at local 18:19:05 and
Stopping at 18:19:21. Process check confirms PID 24452 absent. Parent owns the
monitor exit/memory results and final packaged-JAR smoke/publication decision.
Parent confirmed final monitor exit 0 at 04:19:23 UTC, peak working set
1418.3 MiB and private allocation 1742.7 MiB. These are parent-reported sampled
measurements, not an OS-enforced cap. Parent has reclaimed the runtime slot for
the final packaged smoke; no further live input is authorized or planned here.
No source/test edits or additional Java launches were made by this auditor.

Disposition: the requested five-target bounded visual pass is complete with no
observed product failure, combining unchanged UI/effect evidence from the first
candidate and corrected network evidence from the final candidate. This does
not certify every-frame flicker absence, all-item live ID enumeration, every
GUI scale, survival progression, or final packaged-artifact acceptance.
