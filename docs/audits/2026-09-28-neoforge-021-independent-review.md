# NeoForge 0.2.1 Independent Review

Scope: bounded source and saved visual-evidence review, not release approval.
No Minecraft/build/test runs, product edits, commits, or publication by reviewer.
Only this document is owned by this review lane.

Earlier reviewed candidate SHA-256:
`c93520929bb5b2e92ccde76e44204610a7563ee878d7adcf4ea37adb09b59d26`.
The earlier creative and guide manifests identify this exact production hash.
Sections preceding "Final Candidate UI" retain their original c935 checkpoint
meaning, including references to "current candidate". They are not silently
promoted to the later candidate below.

## Fairy Fix Source Review

Reviewed the actual working-tree diff in `FairySystem.java`,
`AuraFairyEntity.java`, and `FairySystemTest.java`. No concrete introduced
data-loss, orphan, role, lifecycle, or obvious performance issue found.
Reconciliation clears owner canonical mappings before selecting replacements;
duplicate rejection requires a known different canonical UUID. Conditional
UUID removal preserves the selected entity's mapping. Whole-level scans occur
on reconciliation mismatch or lifecycle cleanup, not every fairy tick.

The new tests cover helper decisions, not actual chunk/dimension transitions.
Runtime multiplayer retest remains pending at this review checkpoint; the
previous candidate's observed duplicate-fairy blocker is not closed by source
review or the parent-reported 265-test build result.

## Creative Tab

Evidence: `build/qa-audit/neoforge-021-ui-creative-r2/manifest.json` and
`creative-aura-tab.png`, independently opened and inspected.

Bounded visual PASS: selected Aura tab on page 2 / 2, readable unclipped
"Aura Cascade Reimagined" heading, and 45 populated visible catalog cells.
Visible block, book, and crystal icons are not blank or obvious missing-texture
checkerboards. The manifest reports an exact 161-item catalog match; this is
not visual inspection of all 161 icons. Item-name tooltips were not shown.

## Guide At GUI Scales 2 And 3

Evidence roots under `build/qa-audit/`:

- `neoforge-021-ui-guide2-r2/`
- `neoforge-021-ui-guide3-r2/`

Independently inspected each root's `manifest.json`, `guide-layout.json`, and
all three PNGs: `guide-landing.png`, `guide-getting-started.png`, and
`guide-back-to-landing.png`. Both captures are 1280x720, at GUI scales 2 and 3
respectively.

Bounded visual PASS at both scales. All nine landing links are readable,
separately spaced, and unclipped: Walkthrough, Aura Systems, Storage and Gear,
Fairies, Enchantments, Late Systems, Getting Started, White Aura Crystal, and
First Circuit. Headings, category icons, footer controls, and page edges remain
visible without text overlap. Getting Started shows readable introductory text
and the Encyclopedia recipe spread with rendered ingredient/output icons.
The back captures visibly restore the landing page and its nine links.

Screenshots agree with the recorded action sequence: normal Encyclopedia item
use, click on the rendered Getting Started hyperlink, then the back widget.
Only `aura:getting_started` was navigation-tested; no claim that all nine link
destinations or all guide links were validated. The inherited Binding Ring
wrong-link defect remains disclosed and is not repaired or cleared here.

Both saved font reports contain 41 entries / 371 pages, zero failed pages,
zero glyph overflows, zero title overflows, and no audit failures. Landing text
bottom is 151 against limit 155. These are inspected automated font/layout
results, not manual rendering/navigation acceptance of all 371 pages or proof
of semantic guide correctness.

## HUD Samples

Evidence: `build/qa-audit/neoforge-021-ui-hud-r2/manifest.json` and all five
`hud-frame-{5,20,40,80,120}.png` captures, independently inspected. Manifest
identifies the current candidate hash above, at 1280x720 / GUI scale 2.

Bounded visual PASS: the top-left three-line Aura Node HUD is present,
readable, unclipped, and consistently positioned in every saved capture.
Frames 5/20/40 visibly show White Aura 1082 / Stored Power 3246; frame 80
shows 1467 / 4401; frame 120 shows 1690 / 5070, matching the manifest.
Text remains legible across these value updates; node/world rendering is
nonblank. The driver records 120 consecutive target-valid render frames, but
only five screenshots were reviewed. This does not prove absence of flicker
on every intervening frame or stability for other HUD targets/layouts.

## Disk Restart

Evidence under `build/qa-audit/`: `neoforge-021-disk-evidence-r1/` expectation
and check JSON, plus `neoforge-021-disk-prepare-r1/` and
`neoforge-021-disk-check-r1/` monitor summaries and stdout logs.

Bounded restart PASS on the current hash. Prepare used PID 31500 / start
marker `0a855927-4460-4327-a9c8-846c566b1d0c`; check used PID 29352 / marker
`000a0ab8-fffb-4fea-b18e-53be3985eb53`. Logs corroborate both process starts.
Prepare completed and saved/stopped before the later check process started.
Both monitor summaries record expectation met, normal stop, and exit 0; logs
include completed dimension saves. The check references the preparation run.

Expected and actual snapshots agree on 3000 White aura, zero stored power,
the MOD storage book's name/variant/count, two stored types (three White
ingots and two White crystals), one connected storage shelf, and matching
coordinator browser counts. No remaining world items are reported.
The check monitor invokes only `auraqa persistence check`; inspection of
`DiskRestartRun.check`, its chunk-loading helper, and command dispatch shows
reload/snapshot/comparison, not fixture recreation or reseeding. Preparation
does supply storage contents, so this is persistence evidence, not proof of
survival acquisition or storage insertion gameplay. Nonzero power persistence,
player attachments, rituals, and other world states are not established here.

## White Progression

Inspected `build/qa-audit/neoforge-021-persistence-evidence-r2/white-full-result.json`.
It identifies the current candidate and reports PASS after 1447 total ticks:
40 unpowered-control ticks and 1407 progression ticks. The control retains
one iron ingot and one white wool with zero processor progress. Recorded
inputs are eight White crystals and one coal; observed peaks include 428 aura
at the lift target, 349 falling power, and processor progress 60. Final nearby
items contain exactly one `aura:arcane_ingot_white`, with the raw recipe items
absent. This supports the bounded fixture outcome, not full survival progression.

White r2 gameplay PASS is retained as a separate trial, not clean-lifetime
acceptance. The parent reports a monitor completion-regex typo caused forced
timeout after the scenario passed. Inspected
`neoforge-021-progression-runtime-r2/summary.json` confirms expectation false,
normal-stop false, exit -1, and `startup_command_or_shutdown_timeout`.
The later clean-lifetime r3 result below does not erase the failed r2 monitor
trial. No new run was launched here.

R3 bounded PASS: inspected
`neoforge-021-persistence-evidence-r3/white-full-result.json` and
`neoforge-021-progression-runtime-r3/summary.json` plus stdout. The result and
monitor identify the current candidate. One White Arcane Ingot is the only
reported final nearby item after 1451 total / 1411 progression ticks. The log
corroborates completion, saves, and stop; monitor records expectation met,
normal stop, exit 0, and no failure. This closes the clean-lifetime evidence
gap for this bounded White fixture, not full survival progression.

## Smelter Conversion Visuals

Inspected `build/qa-audit/neoforge-021-ui-animation-r1/manifest.json`,
`launch-summary.json`, baseline `item-conversion-before.png`, and all three
`conversion-animation-{1,2,3}.png` captures. Manifest identifies the current
candidate. The monitor records exit 0 without timeout or memory stop.

Frames are valid, with a textured Smelter and readable changing HUD. Particles
above the machinery change across captures at client ticks 98/100/102.
However, the converted dropped item and its specific reaction are not clearly
visible in these views; machinery occludes the item area. Thus the distinct
item-reaction visual was unverified in r1, not a confirmed rendering defect.
Particle changes alone do not distinguish conversion from nearby aura effects.

The manifest records naturally ticked Smelter Raw Iron-to-Iron conversion:
one raw iron / zero ingots before, zero raw iron / one ingot after, with raw
crystals supplied to initially empty nodes. This supports the recorded bounded
conversion outcome, not an independent pixel-level proof of the shared item
reaction. No Angelsteel-specific recipe or animation coverage is claimed.

R2 bounded visual PASS supersedes that visibility gap, without relabeling r1.
Inspected `neoforge-021-ui-animation-r2/manifest.json`, launch summary, baseline
PNG, and all three conversion PNGs. Baseline visibly shows the brown raw-iron
item beside the Smelter's right face; subsequent frames show the gray ingot
and a distinct cyan particle reaction around it. The particle shapes/extent
change across client ticks 74/76/78, separate from the higher transfer stream.
This is actual visible sampled reaction evidence, consistent with the
manifest's one raw iron to one ingot transition. Baseline capture already has
naturally accumulated power/progress; it is not a zero-power screenshot.
Manifest identifies the current candidate; launch summary records exit 0,
no timeout, and no memory stop. This covers the shared reaction on a Smelter,
not Angelsteel-specific behavior, every animation frame, or exact duration.

## Energy Interoperability

Inspected `build/qa-audit/neoforge-021-energy-runtime-r3/energy-result.json`,
monitor `summary.json`, and matching PASS/save/stop log records. Monitor binds
the run to the current production hash and records expectation met, normal
stop, exit 0, and no failure. All eight bounded cases pass with consistent
accounting: empty, full, rejecting, partial, sided, four receivers, over four
receivers, and external Energy Meter.

Independently checked the recorded debit arithmetic against
`min(powerBefore, ceil(acceptedFe / 15))` and power-before/after differences.
Empty/sided/four/external accept 29520 FE and debit 1968 power (1968 to 0).
Partial accepts 37 FE and debits 3 (1968 to 1965). Full/rejecting/five-receiver
cases accept zero and preserve all 1968 power. The feeder is recorded removed
before each measurement, preventing continued generation from masking debit.

External Energy Meter 1.21.1-0.5.2 uses its WEST input in CONSUME mode.
Its 29520 FE is measured through the handler interval total, not persistent
stored energy. Source inspection of the QA readout explains the external
`actualInsertCalls: 0`: this counter is uninstrumented for the external case,
not evidence of zero transfer. The JSON and logs support bounded native
capability/accounting acceptance; they do not establish broad modpack or
all-machine compatibility.

## Placement Preview

Placement r1 is an invalid visual test per the parent's diagonal camera-hit
finding, not a production failure or PASS.

R2 bounded visual PASS: inspected `neoforge-021-ui-placement-r2/manifest.json`,
launch summary, and all three placement-preview PNGs. Two textured nodes are
visible in a straight alignment. Capture 1 shows the nodes without a clearly
visible cyan preview; captures 2 and 3 show cyan particles between them, with
changed particle extent. The manifest records successful normal use-item-on
placement at (3, -60, 2), beside the empty fixture node at (2, -60, 2), with
no aura/power seeded. It identifies the current candidate, and the monitor
records exit 0 without timeout or memory stop. This establishes sampled
placement-preview presence, not continuous every-frame visibility or a
powered-transfer test.

## Current-Candidate Hook Rerun

Inspected `build/qa-audit/neoforge-021-hooks-r3/hooks-result.json`, monitor
summary, and stdout. All five bounded controls report PASS: damage, Red Hole
lifetime, Thief drops, bonus-loot cancellation, and harvesting isolation.
Log records corroborate each control and completed saves/stop. The monitor
identifies the current candidate and records expectation met, normal stop,
exit 0, and no failure. These are current-candidate results, not relabeled
old-candidate evidence; no broader hook/modpack coverage is inferred.

## Handoff

Assigned source/visual/persistence/energy/hook reviews are complete within
their stated bounds. No new production defect is identified in these final
captures/results. Multiplayer is running separately per the parent and is
not accepted by this document; fairy dimension/lifecycle runtime closure
still requires independent review of that run. This is not release approval.

## Final Candidate UI

New candidate SHA-256:
`f2bc9d9c6b73e59004f5e582196f850f1e4bfc5bd0515a299a5201ead0605f0b`.

Independently opened all 19 actual PNGs and the six manifests under
`build/qa-audit/neoforge-021-final-{creative,guide2,guide3,hud,placement,animation}/`.
All six manifests identify this exact hash. All six completed launch summaries,
including animation, record exit 0, no timeout, and no memory stop. These are
new candidate-bound observations, not reuse of the earlier screenshots.

| Scene | Independent bounded visual verdict |
| --- | --- |
| Creative (1 PNG) | PASS: selected Aura tab, readable heading and localized "Cascading Colorer" tooltip; visible catalog icons are textured, not blank. The 45-cell viewport has some icons obscured by that tooltip. Manifest reports 161 matching entries; neither all 161 icons nor all labels were visually checked. |
| Guide scale 2 (3 PNGs) | PASS: nine readable, separated landing links; readable Getting Started text/recipe; back screenshot restores landing. |
| Guide scale 3 (3 PNGs) | PASS: same bounded navigation/layout sequence, larger readable text, no visible clipping or overlap. |
| HUD (5 PNGs) | PASS: all three HUD lines remain visible and readable across sampled updates. White aura/power pairs are 838/2514, 1838/2514, 2838/2514, 3608/4824, and 3608/4824. |
| Placement (3 PNGs) | PASS: two aligned textured nodes; cyan preview visible between them in captures 2 and 3, not clearly visible in capture 1. Manifest records normal use-item-on placement of an empty node. |
| Smelter animation (baseline plus 3 PNGs) | PASS: brown raw iron visible before; gray ingot and distinct changing cyan reaction particles visible afterwards. Manifest records natural one-raw-iron to one-ingot conversion. |

Both final guide-layout reports were inspected: 41 entries / 371 pages,
success true, zero glyph/title overflows, no failures. This is automated font
evidence, not manual acceptance of every page. Only Getting Started was clicked;
the inherited Binding Ring wrong-link defect remains disclosed. The HUD driver
records 120 consecutive target-valid frames, but only five captures were
visually inspected: no every-frame flicker claim. Placement is sampled preview
presence, not powered transfer. Smelter confirms a shared reaction sample, not
Angelsteel-specific coverage or every frame/duration of the animation.

The second fairy fix previously received a separate read-only source review:
no concrete introduced issue found in deferring equipped/alive-owner
reconciliation until its position is entity-ticking and gating orbit targets
on entity-ticking status. Helper tests do not prove actual lifecycle transitions.
The parent subsequently reported c935 still reached three fairies in trial
145322; that older candidate's source review never established runtime closure.

Multiplayer status is parent-reported context only, not independently accepted
in this UI pass: f2bc trial 150601 had an invalid death observer; 151217 passed
dimension and keep-inventory true death/respawn plus false exact-ring-drop
controls before a SavePhase atomic-read gap stopped the run. The bounded retry
unit test reportedly passes, but the final full MP run awaits a server lease.
These partial trials are not a full MP PASS. Active MP snapshots were not read.
The c935 disk/energy/progression/hook results above remain bound to c935 unless
separately rerun/reviewed for f2bc. Final UI acceptance is not release approval.

## Evidence Boundary

Earlier partial multiplayer/visual evidence belongs to old candidate
`04f1a0f860b2f19d3b1113c17f2ce58070193eaca1e2e86c771aab798817ad5a`.
It is not acceptance of the new candidate. This document establishes only the
bounded reviews above; runtime lifecycle, remaining UI/gameplay, and release
gates retain their separate status.
