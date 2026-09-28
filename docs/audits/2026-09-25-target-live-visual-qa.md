# Target 1.21.11 Independent Live Visual QA

Date: 2026-09-25. Current status: PARTIAL SAVED-FRAME VISUAL PASS; GAMEPLAY GAPS OPEN.
Latest independent review: 2026-09-27, bookshelf run `20260927-151504-555`;
bounded HUD text/positive-to-zero and bookshelf ARGB label gates PASS.
The initial status and NOT RUN tables below are historical; the appended
client-probe review supplies the current bounded dispositions. Native capture
failure was not repaired by this saved-frame review.

Initial status: LIVE QA BLOCKED BY NATIVE CAPTURE FAILURE.
Scope: bounded AstraLight visual/gameplay checks for Aura Cascade Reimagined
0.2.1 on Minecraft 1.21.11, branch `fabric/1.21.11`.
No target live visual observations or passes yet. No Java/build launch, gameplay
input, product edit, credential access, publication or commit performed.
Assigned-window observation and one recovery activation were attempted below.

## Handoff Gate

Parent owns packaged-client preparation and launch. Before window actions obtain:

- Explicit ready signal and input ownership for the uniquely identified client.
- Process/window context, disposable world/profile and exact artifact SHA-256.
- Actual dependency versions and packaged rather than development provenance.
- Confirmation the first Creative inventory opening remains untouched.
- Monitor/cleanup ownership and any fixture boundaries to preserve.

Select exactly one returned native window matching that context. Use
`node_repl` with `@oai/sky`; no PowerShell UI automation. Skill, guidance, API
and confirmation policy were read fully during preparation. The node tool is
available; package import succeeded. Native screenshot capture failed below.

## Evidence Rules

Read the target port specification, README, PORTING_NOTES, target port audit,
0.2.1 integration and independent visual audits, and remaining-gameplay audit.
Historical 1.21.1 results are context only, never target passes.

Record artifact hash, window identity, frame size, GUI scale, timestamp and
fixture setup with evidence. Ordinary input, command-seeded materials/state,
actual visible effects, parent-reported results and source inferences must stay
distinct. Do not seed particles or claimed crafting outputs. Screenshots must
be inspected; an input acknowledgement or resource file is not visual proof.
Sparse frames cannot establish absence of single-frame flicker or prove animation.
Use temporally separated inspected frames for motion, with cadence disclosed.

Only this report and ignored screenshots/logs are owned by this auditor.
Proposed evidence directory: `build/qa-audit/target-live-visual-20260925/`;
verify ignore status before writing evidence. No other audit or source edits.

## Bounded Checklist

Every row is NOT RUN until an observation is recorded.

| ID | Check | Acceptance and bounded controls | Status |
| --- | --- | --- | --- |
| C1 | Creative first opening | First untouched opening selects Aura Cascade Reimagined; inspect visible item rows and representative tooltips. Do not claim exhaustive live item-ID coverage from icons. | NOT RUN |
| C2 | Creative retention | Select another tab, close/reopen twice; chosen tab remains selected. Record actual page without requiring page zero. | NOT RUN |
| B1 | Patchouli scale 2 | Nine distinct readable landing links, no overlap/clipping; click each from landing and inspect intended destination. Sample recipe, back and category navigation. | NOT RUN |
| B2 | Patchouli scale 3 | Repeat all nine clicks and spacing/clipping inspection; record actual frame dimensions and effective scale. | NOT RUN |
| R1 | White crystal recipe | Inspect guide recipe: one amethyst shard surrounded by eight gold nuggets, output two White Aura Crystals. Use supplied raw ingredients in actual crafting UI to verify output; label materials seeded. | NOT RUN |
| V1 | Colored swords | Inspect actual red/orange/yellow/green/blue/violet attunement appearances, held and inventory where practical; record how each attunement was obtained or seeded. | NOT RUN |
| V2 | Pedestal rendering | Inspect placed pedestal and displayed item from useful angles; check texture, transparency and item placement. Observe temporal motion where expected, not merely a static model. | NOT RUN |
| V3 | Fairy animation | Inspect actual visible fairy across separated frames for animation/transparency; record binding or supplied bound-state provenance. No inference from renderer source alone. | NOT RUN |
| G1 | Ground crafting | Physically drop supplied same-tier Angelsteel ingots: two-input negative control, then third input; inspect real conversion, output count and burst/decay. Do not inject effect/output. | NOT RUN |
| N1 | One-shot placement preview | Physically place empty node near empty peer: initial trace, expiration, look-away/back without replay. Repeat blocked-link and camera-occlusion controls; bounded pump/pedestal preview samples. | NOT RUN |
| N2 | Fresh link geometry | New neighbor and inserted obstruction controls; separate visually observed link behavior from any unavailable exact plan/apply timing proof. | NOT RUN |
| H1 | HUD stability | Observe active transfer and changing values; retain a seen color at truthful zero on same target. Distinguish naturally reached zero from seeded zero. Check intervening entity, look-away/back and different target; scales 2/3 if feasible. | NOT RUN |

## Landing Click Ledger

| Landing label | Expected destination | GUI 2 | GUI 3 |
| --- | --- | --- | --- |
| Walkthrough | Walkthrough | NOT RUN | NOT RUN |
| Aura Systems | Aura Systems | NOT RUN | NOT RUN |
| Storage and Gear | Storage and Gear | NOT RUN | NOT RUN |
| Fairies | Fairies | NOT RUN | NOT RUN |
| Enchantments | Enchantments | NOT RUN | NOT RUN |
| Late Systems | Late Systems | NOT RUN | NOT RUN |
| Getting Started | Getting Started | NOT RUN | NOT RUN |
| White Aura Crystal | White Aura Crystal | NOT RUN | NOT RUN |
| First Circuit | First Aura Circuit | NOT RUN | NOT RUN |

Expected labels above come from historical documentation, not target observation.
Record any target differences without silently changing acceptance expectations.

## Results And Limits

No live visual results. Source review is preparation only.
Stop on ambiguous window identity, lost input ownership or capture failure;
report harness limitations separately from product defects. Do not broaden this
pass into exhaustive survival, multiplayer, persistence or all-mechanic coverage.
Return input ownership explicitly after the bounded pass; coordinate normal
save/quit with the parent and never terminate another session.

### Assigned Interactive Run: Capture Blocker

Parent authorized only PID `32636`, run `20260925-165037-810`, candidate SHA-256
`271089594f958c259b8bfaa35103ef2a5df40584fdfb91489472908aa953d8d3`.
Read-only process inspection mapped that PID to MainWindowHandle `17304632`,
title `Minecraft* 1.21.11 - Singleplayer`. Native `sky.list_apps` independently
returned exactly that window; the other 1.21.1 Minecraft window was not targeted.

The run's observer manifest reports intermediary namespace, packaged Aura
`0.2.1+1.21.11`, matching SHA-256, 1,599,950 JAR bytes, loaded singleplayer
Overworld `New World`, 40 stable rendered frames and 1280x720 framebuffer.
Guide-layout success reports 41 entries and 371 pages. These are observer
results, not this auditor's independent visual acceptance or click evidence.

After that completion gate, `sky.get_window_state` failed with
`FrameArrived timed out: timed out waiting on channel`. Refreshed native window
selection, rehydrated the same returned window, activated it and retried once.
The same capture error recurred. No screenshot was available for inspection.
No gameplay key, click, chat command or Creative-inventory opening was issued.

Stopped native actions after the permitted recovery failed. All checklist rows
remain NOT RUN, not product failures. No startup crash was established. The
client was not restarted or terminated; normal UI save/quit could not be safely
performed without observation. Parent must recover capture or take back cleanup
ownership. Input ownership is released pending a new handoff/recovery direction.

### Authorized Normal Cleanup

Parent subsequently authorized text-only native observation and a single normal
window-close action, with no gameplay inputs. Fresh process inspection again
mapped PID 32636 to handle 17304632. Fresh `sky.list_windows` selection returned
exactly that target. `sky.get_window_state` with `include_screenshot:false` and
`include_text:true` succeeded and identified the focused Minecraft 1.21.11 window.
The accessibility tree exposed only native window chrome, not the game UI.

Sent exactly one `Alt_L+F4` to that confirmed returned window. Immediate listing
still showed the window during shutdown; subsequent read-only process and native
window checks showed PID 32636 and handle 17304632 absent. No close was retried.
The assigned profile's `logs/latest.log` records `Stopping!` at 17:00:30 and
player/world saves plus `All dimensions are saved` at 17:00:31. Cleanup completed
through normal window lifecycle, not forced process termination.

Exact blocker remains screenshot-enabled `sky.get_window_state` failing twice
with `FrameArrived timed out: timed out waiting on channel`; text-only state works.
This isolates an observed capture-path limitation, not its underlying cause.
Further live visual QA needs a parent-provided in-game framebuffer/control driver
and a new owned-client handoff. No PowerShell UI fallback was attempted. All live
visual checks remain NOT RUN; the first Creative opening was not consumed here.

### Independent Saved Framebuffer Review

Opened the actual Minecraft framebuffer file with `view_image`:
`build/qa-audit/observer/runs/1.21.11/20260925-165037-810/screenshot.png`.
This is a saved observer image, not a recovered native-CU capture payload.

PASS, strictly bounded nonblank-world/assets-visible check: the 1280x720 image
shows a stone platform, blue sky and clouds, the player's hand, two blocks with
multicolored bookshelf faces and purple borders, and a blue/cyan textured block
between them. Surfaces have distinguishable texture detail; this is not a blank
or sky-only capture. No obvious missing-texture checkerboard is visible on these
sampled faces. Exact block identity and texture parity are not established by
the screenshot alone. The purple borders are not themselves proof of a defect.

The image has no visible book screen, Creative inventory, sword, fairy, crafting
effect, link preview or Aura HUD. It proves none of the checklist behaviors or
animation. Those rows remain NOT RUN. Parent separately confirmed normal exit 0.

### Guide Font Report Review And Limits

Parsed the same run's `guide-layout.json` and inspected the current
`GuideLayoutAudit.java` implementation to understand its evidence boundaries.
Report v3: 41 entries, 371 pages (169 text, 188 crafting, 14 quest), zero failed
pages, zero unaudited layout/text pages, zero hidden resource pages, zero glyph
overflows and zero title overflows. It records 151 whitespace-only overflow
fragments, which are distinguished from visible glyph overflow, not 151 clipped
text failures. Advancement locking is enabled; the report's quest line is visible.

Landing text measured x=15..123 against right limit 131, y=43..151 against bottom
limit 155; logical clearances are therefore 8 right and 4 bottom. Line height is
9. The report has no landing click ledger or per-link hitbox separation proof.

Source-based interpretation, not a visual observation: the auditor constructs
and initializes Patchouli landing/entry GUIs at the current scaled dimensions,
uses the client font and Patchouli text layout, and checks text/title geometry.
It does not run the requested link-click matrix or establish rendered recipe
icon correctness. This report does not identify or repeat both GUI scales 2/3,
and page-local logical bounds do not independently prove final-screen placement,
pixel-level clipping, tooltip overlap, navigation, quest-state transitions or
actual crafting. A successful initialized crafting page is not proof of the
amethyst/eight-nugget inputs or two-crystal output. Pending framebuffer-driver
screens and action evidence must supply those separate checks.

### New Candidate Multiplayer Framebuffer Review

Candidate SHA-256 supplied by parent:
`79193134819500bd39cf8e749c2a57b47fd0b6bb3acfedbc307bb91ea954984b`.
Run: `build/qa-audit/multiplayer/20260925-171455-634/`.
Parent reports all 15 multiplayer checks passed and normal exit. Those state
assertions are not independently repeated here and do not establish rendering.
This candidate is distinct from the earlier singleplayer observer candidate.

Enumerated all PNGs recursively: exactly four. Opened and independently inspected
each actual framebuffer using `view_image`, with no client/window inputs.
All four images are 854x480 and show a nonblank world: tiled platform, sky/clouds
and a textured player model. No obvious missing-texture checkerboard appears on
the visible sampled surfaces. This is not a whole-resource-pack acceptance.

| Frame relative to run | Actual visible observation | Bounded disposition |
| --- | --- | --- |
| `owner-initial/capture-4.png` | Player labeled AuraWitnessQA; small luminous pink/purple sprite with pale center immediately to the right of the head. | Fairy-like sprite visible in owner capture; small and partially adjacent to player silhouette, not detailed texture parity proof. |
| `owner-rejoin/capture-1.png` | Same labeled player; luminous pink/purple sprite with pale center clearly separated to the lower right of the body. Background remains visible around its irregular outline. | Stronger owner-side fairy-like asset visibility; no opaque rectangular sprite background or missing-texture checkerboard apparent at this resolution. |
| `witness/capture-1.png` | Player labeled AuraOwnerQA, viewed from side/rear; world and player textures visible. | Nonblank pass only. No clearly identifiable separate fairy sprite in the inspected frame. |
| `witness/capture-2.png` | Same labeled player and similar framing. | Nonblank pass only. No clearly identifiable separate fairy sprite in the inspected frame. |

No animation pass: owner images span initial/rejoin sessions, not a controlled
temporal animation sequence; differing sprite positions cannot establish texture
frame animation. Witness-side fairy rendering remains unproven by these images,
not a demonstrated renderer defect: camera framing, occlusion, capture timing and
entity state must be tied to the screenshot before absence can be diagnosed.
State assertions cannot fill that visual gap. V3 remains incomplete; these stills
add bounded owner-side asset visibility only. No other checklist row is closed.

### Client-Probe GUI And Creative Saved-Frame Review

Reviewed 2026-09-27, read-only evidence inspection followed by this report edit.
Evidence root: `build/qa-audit/observer/runs/1.21.11/20260925-174922-607/`;
the filenames below are relative to its `client-probe/` directory. Opened eight
actual PNGs with `view_image`; did not launch Java, use native capture, control a
client, or alter source/evidence. No input ownership was acquired or exercised.

Provenance from the enclosing run manifest, not inferred from pixels: packaged
Aura `0.2.1+1.21.11`, intermediary namespace, singleplayer `New World`, SHA-256
`79193134819500bd39cf8e749c2a57b47fd0b6bb3acfedbc307bb91ea954984b`.
The probe manifest records 2026-09-26 03:49:54.278239100Z through
03:50:03.932737700Z (September 25, 17:49:54 through 17:50:03 Hawaii time).
All eight inspected frames are 1280x720. Manifest GUI dimensions are 640x360
at scale 2 and 427x240 at scale 3; both landing records report effective
Patchouli scale factor 1.0. Per-frame timestamps and native window identity are
not provided by this probe manifest; no earlier PID is assumed for this run.

Parent reports screenshot timing was corrected after prior failed captures and
had already seen the real GUI-2 landing. Independently, these inspected files
actually contain the named GUI states, rather than just a world image. This
corroborates usable captures for this sample, not a diagnosis of earlier failures
or proof that every uninspected destination frame is correctly timed.

| Inspected PNG | Direct visual observation | Bounded result |
| --- | --- | --- |
| `landing-gui2.png` | Open Encyclopedia Aura with all nine expected blue landing labels, separate readable lines, title and category icons. Last link stays above the bottom controls. | PASS sampled landing readability, separation and book bounds. |
| `landing-gui3.png` | Larger open book; same nine labels fully visible, with no apparent overlap or clipping between links, page text or bottom controls. | PASS sampled landing readability and bounds at scale 3. |
| `destination-08-white_aura_crystal-gui2.png` | White Aura Crystal article and recipe diagram: purple shard in center, eight gold nugget icons around it, white crystal output marked 2. Explanatory text agrees. | PASS guide recipe display only. |
| `destination-08-white_aura_crystal-gui3.png` | Same article and recipe enlarged; ingredients, output count and body text remain readable and within pages. First Aura Circuit tooltip lies between the left title and body. | PASS sampled article/recipe layout; tooltip does not visibly hide the recipe or body text. |
| `creative-first-open-gui2.png` | Aura Cascade Reimagined title and selected crystal tab; five visible rows of varied item/block icons, scrollbar and readable Cascading Colorer tooltip. Tooltip overlays some grid icons. | PASS visible mod-tab contents and representative tooltip; not exhaustive item-ID or texture parity coverage. |
| `creative-other-tab-gui2.png` | Saved Hotbars title, selected bookshelf tab and diagonal paper placeholders. | PASS visible alternate-tab state. |
| `creative-retained-reopen-1-gui2.png` | Saved Hotbars remains displayed with the same tab and placeholder layout. | PASS retained-state appearance; reopen action comes from manifest. |
| `creative-retained-reopen-2-gui2.png` | Saved Hotbars again displayed, not the mod tab. | PASS second retained-state appearance; reopen action comes from manifest. |

Manifest/state evidence, separately: `complete` and `success` are true and
`failures` is empty. First-open selected-tab assertion matches `aura:aura`;
both reopen assertions match `minecraft:hotbar`, with `currentPage: 1`.
Each of the two landing runs records nine completed and matched destinations,
plus nine consumed back clicks returning to the original landing screen.
All nine historical ledger labels match their expected destination at each
scale (18/18 state assertions). Input is the screen `mouseClicked` API, not
independently observed physical mouse play. These results are not 18 visually
inspected destination passes: only the White Aura Crystal destination PNG at
each scale was opened in this review.

Current checklist disposition:

- C1: bounded PASS for mod-tab selection, visible rows and one tooltip, combining
  manifest first-open provenance with the inspected image. Untouched-first-open
  history cannot be independently established from a still.
- C2: bounded PASS for two-reopen retention, combining manifest sequence with
  three inspected Saved Hotbars states; no requirement that page be zero.
- B1/B2: landing visual PASS and 9/9 destination/back state PASS per scale.
  Full destination visual inspection and broader category navigation remain open;
  do not promote the whole checklist row to an exhaustive visual pass.
- R1: guide diagram/text visual PASS at both scales. Actual crafting UI execution,
  material provenance, consumption and collected output are NOT RUN here.
- V1/V2/V3/G1/N1/N2/H1: not exercised by these eight frames. Earlier bounded fairy
  visibility evidence remains unchanged; no animation or all-mechanics pass.

Defects/gaps: no definite Aura GUI product defect identified in this sample.
The GUI-2 landing has a partially offscreen vanilla movement tutorial toast at
the upper right; it does not cover the book. Another partial toast is visible at
the edge of reopen-2. These stills do not distinguish toast entrance/exit timing
from persistent clipping, so this is a capture caveat, not a diagnosed mod bug.
The Creative tooltip intentionally obscures part of the item grid, leaving those
icons unsuitable for detailed texture inspection. No unhovered comparison,
continuous flicker/animation observation, crafting action or new feedback-effect
frames were inspected. Further feedback frames can extend this record without
overwriting the historical capture blocker or broadening the present claims.

### Runtime Feedback And Render Review: Readiness-Limited Set

Run root: `build/qa-audit/observer/runs/1.21.11/20260927-143756-748/`.
Read enclosing, feedback-probe and render-probe manifests. Independently opened
29 actual 1280x720 PNGs with `view_image`: all three `render-frame-1..3.png`,
all twelve `nodePlacementPreview-00..11.png`, all twelve
`angelsteelGroundCraft-00..11.png`, and only `auraHudRowTransition-00.png` and
`auraHudRowTransition-02.png`. Names belong to `render-probe/` or
`feedback-probe/` respectively. Remaining ten HUD frames were not inspected:
parent redirected review to await a fresh readiness-gated set. No Java launch,
native capture, gameplay input, source edit or evidence modification performed.

Enclosing manifest identifies the same packaged candidate SHA-256
`79193134819500bd39cf8e749c2a57b47fd0b6bb3acfedbc307bb91ea954984b`, Aura
`0.2.1+1.21.11`, intermediary namespace and singleplayer Overworld. Feedback
ran 2026-09-28 00:38:37.698620900Z through 00:38:45.776945700Z; render started
00:38:45.799944900Z (September 27 afternoon Hawaii time). These are manifest
timestamps, not independently measured capture times.

#### Logical Results Are Not Visual Acceptance

Render reports `PASS_LOGICAL_AND_CAPTURE`, seeded fixture state, six attuned
swords, pedestal holding one `aura:arcane_gem_white`, bound BASIC fairy presence,
2.099236214729844 displacement and 12 advanced animation ticks. Progression
was not earned. Capture requests at client ticks 2, 9 and 14 are separated by
7 and 5 ticks; the stated API captures the preceding rendered framebuffer.
Those assertions do not prove these fixtures are visible in those images.

Feedback remains `FAIL`, `success:false`, with exactly one listed failed
assertion: `previewAnchorEmpty`. Other assertions pass, including two-input
negative control, transformation, HUD same-target zero, cleanup and twelve
samples per event. HUD fixture uses seeded adjacent redstone power and then
removes it; Angelsteel inputs are seeded ItemEntity instances, not independently
performed manual drops. Output counts and retained HUD row values are state
evidence, not substituted for reading the PNGs.

#### Direct Visual Findings

| Evidence | Observation and bounded disposition |
| --- | --- |
| Render frames 1-3 | Sky/clouds, hand and hotbar only; no visible terrain, pedestal, displayed gem or fairy. V2/V3 visual acceptance BLOCKED by absent fixture context. No fairy movement, sprite transparency or animation can be confirmed. This is not a diagnosed product renderer defect. |
| Render frame 1 | Earlier mixed-item hotbar still visible, not six swords. This illustrates state/frame misalignment despite the frame's logical sword state. |
| Render frames 2-3 | Six sword icons visible in slots 0-5, with red, orange, yellow, green, blue and violet hilt/guard accents respectively and similarly cyan blades. Bounded V1 hotbar color PASS only; no held-sword comparison or earned-attunement behavior verified. |
| Preview 00-03 | No rendered platform or node models; 01 has a selection outline, 02-03 have cyan/teal particles floating against sky. Insufficient fixture context, not a product defect. |
| Preview 04-11 | Partial platform appears at 04, two gray node blocks and a particle trace are visible from 05 onward. Cyan/teal particles extend across the gap and become sparser toward 09-11. Visible native-effect evidence only; particles remain in 11, so full expiration and no-replay behavior are unproven. Failed anchor precondition prevents empty-peer acceptance. |
| Craft 00-01 | Two separate cyan ingot-like dropped sprites above a rendered platform, with shadows; no gold burst visible. Supports the appearance of the two-input control, not item IDs or exact inventory counts from pixels. |
| Craft 02-07 | Three ingot-like sprites with changing position/orientation; no gold burst visible in these samples. In 07, the manifest already reports one output while pixels still show the earlier three-item scene: do not equate sampled state with the rendered instant. |
| Craft 08-11 | One visible ingot-like sprite above a gold/yellow particle cluster; particle shapes change across samples. Bounded G1 conversion-scene/burst visibility PASS with seeded inputs. Degree and exact output count come from manifest; complete burst decay is not captured and manual-drop gameplay is not verified. |
| HUD 00 and 02 only | Target block and platform visible; adjacent red block is present in 00 and absent in 02. Upper-left dark row backgrounds are visible, but no readable text, Orange label or numeric value appears in either inspected frame. H1 positive-to-zero visual acceptance remains OPEN; cannot confirm 100000 or 0 from pixels. Missing readable HUD text is a rerun concern, with cause undetermined, not an established product defect. |

Temporal scope: preview after-placement samples span client ticks 50-68 with
1-3 tick gaps; craft third-input samples span ticks 101-118 with 1-2 tick gaps.
Manifest game times and capture-state lag are not exact visual event timestamps.
These sparse samples show changing particles/item appearances, not continuous
animation quality, absence of flicker, full decay or proof of all mechanics.

Raised the sky-only render blocker promptly during review, then the unreadable
HUD concern. Parent subsequently identified missing post-teleport terrain mesh
readiness and reports adding a `LevelRenderer.isSectionCompiledAndVisible`
fixture gate plus client anchor presence before rerunning. That diagnosis/fix is
parent-reported and not independently source-validated here. Stop further old-set
inspection pending fresh frames. Preserve this run as readiness-limited evidence;
do not promote the feedback probe to overall success or close N1/N2, V2/V3 or H1.

### Corrected Feedback Run: Independent Visual Disposition

Run root: `build/qa-audit/observer/runs/1.21.11/20260927-144113-178/`.
This is the current feedback evidence; the failed earlier set remains historical.
Read all three manifests (enclosing run, feedback-probe, render-probe). Enclosing
manifest again identifies packaged Aura `0.2.1+1.21.11` and SHA-256
`79193134819500bd39cf8e749c2a57b47fd0b6bb3acfedbc307bb91ea954984b`.
Feedback timestamps: 2026-09-28 00:41:44.191465300Z to 00:41:51.531169200Z;
render: 00:41:51.599667900Z to 00:41:53.123104900Z (September 27 Hawaii time).

Opened 25 actual 1280x720 PNGs with `view_image`, without Java/native capture,
input, source edits or evidence modifications:

- `render-probe/render-frame-1.png`, `render-frame-2.png`, `render-frame-3.png`.
- `feedback-probe/nodePlacementPreview-{00,02,03,07,11}.png`.
- `feedback-probe/angelsteelGroundCraft-{01,02,08,09,11}.png`.
- All twelve `feedback-probe/auraHudRowTransition-00.png` through `-11.png`.

Both probe manifests now report success with no failures. Feedback specifically
reports `CAPTURED_REVIEW_REQUIRED`, not visual acceptance. `previewAnchorEmpty`
now passes; the other eight assertions also pass. Inputs remain seeded, and HUD
zero follows removal of seeded redstone power. Render remains a seeded fixture,
not earned progression. These logical successes do not close the visual gaps.

| Check | Direct visual evidence | Current bounded disposition |
| --- | --- | --- |
| Empty-node placement effect | Preview 00 shows platform and one gray node without a visible particle trace. Preview 02 shows both nodes and cyan particles at the left node; 03 shows a short trail; 07 spans the gap; 11 has fewer particles, still visible. Terrain and node context now exist throughout the inspected sample. | PASS native preview appearance with manifest-confirmed empty-anchor precondition. Full expiration, no replay, obstruction, occlusion and fresh-neighbor controls remain open; N1/N2 are not fully closed. |
| Angelsteel burst | Craft 01 has edge-on ingot imagery; 02 shows separate ingot-like sprites; 08 has overlapping ingot imagery. In 09 and 11, a gold/orange burst surrounds a remaining edge-on item above the platform. Particle shapes change between those samples. | PASS visible native burst, with logical two-input negative control and three-to-one upgrade. Exact input/output counts and degree are not readable from stacked/edge-on item sprites. Complete decay and manual-drop gameplay remain unverified. |
| Orange HUD | In all twelve frames, the upper-left three dark backgrounds have no visible text or numbers. The middle background is wider in 00-01 than 02-11. Red power block is present in 00-01 and absent afterward; the orange target remains centered. Other text, including the tutorial toast and hotbar counts, renders. | Visual acceptance BLOCKED: Orange label and positive/zero values cannot be read in any frame. Manifest reports 100000 then 0 on the same target, but this is state-only confirmation. Persistent missing HUD glyphs are an observed visual symptom requiring investigation; product-versus-capture cause remains undetermined. |
| Sword colors | All three render frames show six cyan-bladed swords with red, orange, yellow, green, blue and violet hilt accents in slots 0-5. | PASS bounded hotbar color appearance; held models and earned attunement not verified. |
| Pedestal/gem | Render frames predominantly show sky, with a distant platform entering the far-left edge; no identifiable pedestal or displayed white gem is visible. | V2 still BLOCKED by framing. Logical held-item state is not visibility proof. |
| Fairy | Render 3 contains a pale purple/white sprite fragment clipped at the far-left edge. No fully framed, identifiable fairy sequence exists in these three images. | No V3 movement/animation pass. Fragment alone does not establish identity, complete transparency behavior or temporal motion; logical displacement/animation ticks remain separate. |

Timing limits: inspected preview after-placement requests occur at client ticks
34, 45, 54 and 62 (11/9/8 tick gaps); full preview manifest has twelve samples.
Inspected craft requests are at ticks 81, 86, 98, 100 and 104. HUD requests cover
ticks 110 and 112 powered, then 129, 130 and every two ticks through 146 at zero.
Render requests are ticks 18, 24 and 29 (6/5 tick gaps). Feedback capture follows
a HUD render marker at END_CLIENT_TICK; render capture uses the preceding frame.
No wall-clock cadence or exact frame/state simultaneity is inferred from ticks.

Reported render/HUD blockers immediately. Parent independently confirmed render
2 is still sky-facing and reports preparing explicit client yaw/pitch and
third-person-back framing for a render-only rerun. This is a parent-reported
planned correction, not a verified fix. Current conclusion: feedback logical
PASS, bounded preview/craft visual PASS, HUD readable-row visual acceptance
BLOCKED, and pedestal/fairy awaiting reframed evidence. No all-mechanics pass.

### Final Exact Candidate: Independent Visual Review

Reviewed run `build/qa-audit/observer/runs/1.21.11/20260927-144755-211/`.
The enclosing manifest identifies packaged Aura `0.2.1+1.21.11`, intermediary
namespace, 1,600,031 JAR bytes, and exact SHA-256:
`d0aab15d0160819779742443f6a2a881905bfc5e13210265358be41f6f9dc0db`.
This section is the current candidate disposition; preceding runs are history.
Parent reports exit 0 and that the only product change is the invisible Fairy
Torch particle texture fallback. This auditor did not independently verify the
ZIP/bytecode diff, regression test, warning removal, server or multiplayer runs.

Read enclosing and all three probe manifests. Client ran 2026-09-28
00:48:24.942955100Z to 00:48:34.420568600Z; feedback 00:48:34.492067Z to
00:48:41.342870200Z; render 00:48:41.371868100Z to 00:48:44.905337900Z
(September 27 afternoon Hawaii time). All report complete/success with no
failures; feedback remains explicitly `CAPTURED_REVIEW_REQUIRED` and render
`PASS_LOGICAL_AND_CAPTURE`. Visual conclusions below come from actual images.

Opened 25 PNGs with `view_image`, all 1280x720:

- `client-probe/landing-gui2.png`, `landing-gui3.png`, both
  `destination-08-white_aura_crystal-gui{2,3}.png`, and all four
  `creative-*.png` (first open, alternate tab, reopen 1, reopen 2).
- `feedback-probe/nodePlacementPreview-{00,02,05,11}.png`.
- `feedback-probe/angelsteelGroundCraft-{01,02,09,11}.png`.
- `feedback-probe/auraHudRowTransition-{00,02,11}.png`.
- All six `render-probe/render-frame-1.png` through `render-frame-6.png`.

No other final-candidate PNGs were inspected. No Java launch, native capture,
gameplay input, source/evidence edits or artifact changes were performed; only
this audit was edited. Neither final-candidate tooltip coverage nor the sixteen
uninspected destination images inherits visual acceptance from the old hash.

#### GUI And Recipe

C1/C2 bounded PASS: final first-open image visibly shows Aura Cascade Reimagined,
selected crystal tab and five unobscured rows of varied item/block icons. No
tooltip is displayed. Alternate-tab and both reopen images visibly show Saved
Hotbars. Manifest independently supplies first-open/reopen sequence, matching
`aura:aura` then `minecraft:hotbar`, page 1. Untouched history and exhaustive
item-ID coverage cannot be proven by icons/stills.

B1/B2 landing visual PASS: all nine expected links are readable at both scales,
without apparent link overlap or page clipping. Manifest records 9/9 matched
destinations per scale; that is 18 state assertions, not eighteen independently
inspected destination visuals. R1 guide-display PASS: both White Aura Crystal
articles visibly show center amethyst shard, eight surrounding gold nuggets and
white output marked 2, with matching readable prose. No actual crafting UI
execution or consumed materials/collected output was observed.

#### Render Sequence

V1 hotbar PASS: all six frames display six sword icons with red, orange, yellow,
green, blue and violet hilt/guard accents and cyan blades. Held-model comparisons
and earned attunement remain untested; manifest explicitly identifies seeded
stacks and unearned progression.

V2 bounded visibility/held-item motion PASS: to the player's left, a cyan/green
textured pedestal on a gray support is unobstructed in all six frames, with a
small pale gem suspended above it. The gem silhouette changes from broad diamond
to narrow edge-on and back, supporting temporal rotation rather than a static
item image. Exact `aura:arcane_gem_white` identity comes from the manifest. No
obvious missing-texture checkerboard is visible on the sampled pedestal/gem;
fine texture parity, every face and pedestal transparency are not established
at this size and one camera angle.

V3 bounded fairy visibility/movement PASS: a pale pink/purple glow with a bright
center moves from near the player's head (1), to the right (2), lower left by
the body (3), left of the pedestal (4), above-left (5), then above-right (6).
The world/player can be seen around its irregular soft outline; no opaque
rectangular background or obvious missing-texture checkerboard is apparent.
This closes the previous framing blocker for visible spatial motion. It does
not establish a particular sprite-sheet animation cycle, detailed wing animation,
all-view occlusion correctness, continuous smoothness or absence of flicker.
Manifest animation-clock advance is supporting state evidence only.

Render requests at client ticks 23, 33, 42, 51, 60 and 68 span 45 ticks with
10/9/9/9/8 tick gaps. Captures use the preceding rendered frame, not guaranteed
same-instant entity state. The player remains framed and pedestal remains visible;
background platform detail changes in early frames, so complete terrain readiness
must not be inferred from the fixture visibility pass.

#### Feedback Effects And HUD

| Evidence | Direct observation | Disposition |
| --- | --- | --- |
| Preview 00, 02, 05, 11 | 00 is sky-only with no fixture. 02 shows platform and right-hand node only. 05 shows both nodes joined by a cyan/teal particle trail; 11 shows a much sparser remaining trail. | Bounded native preview appearance/decay-in-progress PASS. Early before-state capture remains insufficient even though logical anchor-empty now passes. Full expiration, no replay, physical placement, blocked links, camera occlusion and new-neighbor controls remain unverified. |
| Craft 01, 02, 09, 11 | Two separated ingot-like sprites in 01, three in 02, then one visible ingot with gold/orange burst in 09 and 11. Burst shape/density changes and is still present in 11. | Bounded transformation-scene/burst PASS with seeded inputs. Manifest supplies same-degree identity, two-input negative control and exactly one upgraded output. Full burst expiration and manual-drop interaction remain unverified. |
| HUD 00, 02, 11 | Orange target centered; adjacent red power block exists in 00 and is absent in 02/11. Upper-left dark row backgrounds persist and the middle one narrows, but all three images lack readable labels or numbers. Tutorial/hotbar text remains visible. | H1 visual acceptance BLOCKED. Cannot visually confirm Orange 100000 or Orange 0. Manifest reports that transition and retained row, but background geometry alone is not a readable HUD pass. |

Feedback sample request ticks inspected: preview 17/31/37/49, craft 66/71/85/89,
HUD 95/115/132. HUD samples include the powered state, early zero and last zero
frame. Unlike the previous candidate's twelve-frame HUD review, only these three
final-candidate HUD PNGs were inspected; do not claim every final HUD frame was
visually checked. Input provenance remains seeded ground ItemEntities and seeded
redstone power followed by removal, not a naturally exhausted transfer circuit.

#### Final Disposition And Remaining Limits

Independent review of the supplied final set is complete. Bounded visual passes:
Creative tab/retention appearance, GUI-2/3 landing and White Crystal recipe
display, six hotbar sword colors, unobstructed pedestal/gem with changing gem
orientation, visible moving fairy, native preview particles and craft burst.

Unresolved visual symptom: HUD backgrounds without readable glyphs recur on this
exact candidate. This prevents HUD visual acceptance despite logical PASS; the
underlying product-versus-capture cause is not diagnosed here. Early preview
frames also demonstrate incomplete fixture context, a harness/evidence limitation,
not proof of a mod placement defect. No all-mechanics, exhaustive survival,
multiplayer, persistence, full animation-cycle or release-wide visual pass is
claimed. Historical failures and earlier-hash evidence remain preserved above.

### ARGB Fix: HUD Visual Blocker Closed Within Tested Scope

Current candidate SHA-256 from enclosing run manifest:
`8eb1dc90691b875ff5fb8aa9bb4d06aaa0befa31f23e2b1c5b5452346a3450cb`.
Run: `build/qa-audit/observer/runs/1.21.11/20260927-150136-346/`.
Manifest identifies packaged Aura `0.2.1+1.21.11`, intermediary namespace and
1,600,028 JAR bytes. Read enclosing, feedback and render manifests; both probes
report success and no failures. Feedback ran 2026-09-28 01:02:18.266135500Z to
01:02:27.041382900Z, render 01:02:27.091880600Z to 01:02:30.877770700Z
(September 27 Hawaii time). Normal exit, rebuild 37, 258/65 passing tests and
ZIP comparison are parent-reported, not independently executed by this auditor.

Parent confirms the source cause: Minecraft 1.21.11 drawString returns for alpha
zero, while the affected calls used `0xFFFFFF`; explicit FF alpha fixes them.
Parent reports only `AuraCascadeClient.class` and
`BookshelfCoordinatorScreen.class` changed versus the preceding d0aab candidate.
This source/diff attribution is separate from the independent pixel verification
below. Earlier missing-text findings remain valid historical regression evidence.

Opened 22 actual 1280x720 PNGs with `view_image`:

- All twelve `feedback-probe/auraHudRowTransition-00.png` through `-11.png`.
- `feedback-probe/nodePlacementPreview-{00,05,11}.png`.
- `feedback-probe/angelsteelGroundCraft-{01,02,09,11}.png`.
- `render-probe/render-frame-{1,3,6}.png`.

No Java/native capture, input, source/evidence modifications or other artifact
edits. Only this audit updated. Unlisted frames, client-probe GUI and bookshelf
screen captures were not inspected on this hash.

#### All Twelve HUD Frames

| Files | Text independently read in every listed image | Visible context |
| --- | --- | --- |
| `auraHudRowTransition-00.png`, `-01.png` | `Aura Manipulator: Orange`; orange-colored `Orange Aura: 100000`; `Stored Power: 0`. | Target visible, adjacent red power block present. |
| `auraHudRowTransition-02.png` through `-11.png` (all ten) | `Aura Manipulator: Orange`; orange-colored `Orange Aura: 0`; `Stored Power: 0`. | Target remains visible and targeted; adjacent power block absent. |

PASS: missing HUD glyphs are resolved in all twelve inspected frames. All three
rows are readable, remain in the same upper-left positions, fit their dark
backgrounds without apparent clipping, and do not overlap one another. The
Orange row remains present at truthful zero rather than disappearing; its
background narrows with the shorter value. This is now visual evidence, not
merely the manifest's 100000-to-zero assertion.

Bounded positive/zero continuity PASS at GUI scale 3: powered requests at client
ticks 118/127, then zero requests at 147/148/150/153/155/157/159/161/163/166.
The ten zero samples span 19 client ticks. Same-target identity and power-removal
sequence are supplied by the manifest and corroborated by the visible fixture.
The 20-tick gap between last powered and first zero sample does not show every
intervening frame. No continuous no-flicker claim, exact transition latency or
proof of all intermediate values. Zero follows removal of seeded redstone power,
not natural exhaustion of an active transfer circuit. H1's text/retained-zero
subcheck is closed; GUI-2 repetition, intervening entity, look-away/back,
different-target controls and broader active-transfer stability remain open.

#### Representative Effect And Render Recheck

Preview 05 visibly has two gray nodes on the platform with cyan/teal particles
extending into the gap; 11 retains only sparse particles. Bounded effect visibility
and decay-in-progress PASS. Preview 00 remains sky-only, so before-state visual
readiness is still an evidence limitation despite passing logical preconditions.
Full expiration, no-replay and obstruction/occlusion controls are not closed.
Inspected preview request ticks: 31/47/65.

Craft 01 shows two separate ingot-like sprites, 02 shows three, and 09/11 show
one visible ingot with changing gold/orange burst particles on the platform.
Bounded conversion-scene and burst visibility PASS; same-degree identity and
exact stack counts remain manifest evidence. Seeded ItemEntity inputs are not
manual player drops. Particles persist in 11, so complete decay remains open.
Inspected craft request ticks: 84/90/106/111.

Render 1/3/6 show all six red/orange/yellow/green/blue/violet sword hilt accents,
an unobstructed cyan/green pedestal with pale suspended gem to the player's left,
and a pale pink/purple fairy glow. The fairy is right of the player in 1, left
near the pedestal in 3, and upper-right in 6; visible spatial motion PASS. The
gem changes from broad to edge-on across the sample, supporting held-item rotation.
No obvious missing-texture checkerboard or opaque rectangular fairy background
is visible. This is a bounded asset/motion pass, not detailed sprite-cycle,
all-angle transparency, earned progression or continuous animation acceptance.
Render request ticks 30/48/73 give 18/25 tick gaps. Only three of six frames
were opened for this representative current-hash recheck.

Current disposition: HUD missing-text blocker CLOSED for this exact hash and
sampled scale/fixture, with retained positive-to-zero row visually confirmed.
Representative preview/craft/render passes reconfirmed. BookshelfCoordinatorScreen
text remains PENDING actual UI captures; its class change and passing tests alone
do not establish appearance. Earlier GUI acceptance remains tied to its tested
hash. No complete H1, all-mechanics or release-wide visual pass is claimed.

### Bookshelf ARGB Screen Gate: Bounded Visual Closure

Run: `build/qa-audit/observer/runs/1.21.11/20260927-151504-555/`.
Read enclosing and client-probe manifests; enclosing manifest confirms packaged
Aura `0.2.1+1.21.11`, 1,600,028 bytes and the current SHA-256
`8eb1dc90691b875ff5fb8aa9bb4d06aaa0befa31f23e2b1c5b5452346a3450cb`.
Client-probe reports complete/success with no failures, from 2026-09-28
01:15:34.328097200Z to 01:15:44.352954200Z (September 27 Hawaii time).
Normal exit is parent-reported. Full client-probe success is logical evidence;
only the two bookshelf PNGs below were independently opened for this review.

Opened both actual files with `view_image` under `client-probe/`. Each is
1280x720 at GUI scale 2, with manifest viewport 640x360 and production screen
logical dimensions 352x220. Manifest identifies
`pixlepix.auracascade.client.screen.BookshelfCoordinatorScreen` at capture.

| PNG | Independently visible labels and layout | Result |
| --- | --- | --- |
| `bookshelf-populated-selected-gui2.png` | Bookshelf Coordinator title; green Connected; Power: 6400 / 1000; Search; Inventory; selected Diamond icon/name and Stored: 64; amount 1, minus/plus, Max, Retrieve; green Retrieved 12. Diamond 64 and Emerald 9 appear in the first two storage cells. Text fits its header, details and control areas without apparent clipping or incoherent overlap. | PASS populated/selected label visibility, fit and readability. |
| `bookshelf-empty-unselected-gui2.png` | Bookshelf Coordinator; green Connected; Power: 2400 / 1000; Search; Inventory; empty storage grid; No item selected; amount 1; subdued controls including Max/Retrieve; Storage empty below the button. All labels remain readable and within the screen, with clear separation between sections. | PASS empty/unselected label visibility, fit and readability. |

Fixture disclosure is essential: manifest explicitly records client-only
synthetic BookshelfCoordinatorMenu snapshots, `fixtureOnly:true`,
`storageReadPerformed:false` and `extractionRequestSent:false` for both states.
Populated snapshot seeds two connected/storage shelves, power 6400/1000,
Diamond 64, Emerald 9, result code 1 and result amount 12. The screen selection
click is recorded as consumed, selecting Diamond/count 64. Empty snapshot seeds
one connected/storage shelf, power 2400/1000, no entries and result code/amount
zero. Thus Connected, Stored: 64 and Retrieved 12 are tested rendered strings
from seeded state, not proof of an actual connected network or successful
storage read/extraction. Button appearance is observed; button operation is not.

Bounded BookshelfCoordinatorScreen ARGB visual gate CLOSED/PASS on this exact
hash at GUI scale 2 in these two states. Together with the preceding twelve-frame
HUD review, both requested ARGB text visibility gates now have independent pixel
evidence. No new defect is apparent in these two screen samples. Other scales,
long/localized labels, search/scroll behavior, quantity controls, actual retrieval,
network synchronization and persistence were not tested. Earlier gameplay limits
remain unchanged; no all-mechanics or exhaustive screen-state pass is implied.
Only this audit was edited; no Java launch, native input or artifact/source edits.
