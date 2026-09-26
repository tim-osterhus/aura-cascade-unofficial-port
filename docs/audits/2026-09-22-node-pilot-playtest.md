# Node and burning-pump pilot: independent playtest

Date: 2026-09-22 (local); action timestamps also use UTC, 2026-09-23.

Latest disposition: both baseline failures are **RESOLVED in the focused
follow-ups below**. Historical FAIL findings and their evidence are retained.
Uncompleted checks remain explicitly UNVERIFIED; this is not full-mod signoff.

## Baseline outcome

**FAIL: an adjacent upper receiver stalls a fueled pump without external redstone.**
**FAIL: Feed a Pump text clips against the bottom page border at GUI scale 2.**

This covers the running `node-pilot-build-2` baseline only. Parent reported103
tests and build/auditClientLaunch passing; this auditor did not run them.
Parent subsequently prepared a source-only correction removing direct node/pump
signals while retaining comparator output. The live binary remained unchanged
during these observations. The correction is **UNVERIFIED** here; the baseline
failure is retained.

Runtime ownership was returned immediately on parent request. No further bridge
calls were made, including no exit-control command. Last independently verified
screen: **Pump Readout, GUI scale3, 1280x720**; control mode last known active.
Parent subsequently reported normal Save and Quit, then Quit Game. Those
parent-owned actions do not establish an independently verified reload pass.

## Environment and evidence

- Client PID **1748** matched the listener at `127.0.0.1:9876`.
- Bridge-confirmed disposable world:
  `build\qa-audit\mod-lab-pilot\saves\guide-baseline-a6acb5e3`.
- Actual player was **SURVIVAL**, despite world metadata's CREATIVE default.
- Command-built platform: x/z35..49, y179. Pump `(40,180,40)`, donor node
  `(44,180,40)`, upper receivers separately at y181 and y183.
- Commands supplied exactly one coal and one white crystal to inventory.
  Actual **Q** input dropped each for ordinary absorption. **No pump fuel,
  power, or speed was injected.** Later comparisons explicitly seeded6,000
  white Aura into the pump, not fuel or receiver output.
- Actual screenshots and read-only server NBT/game times establish results,
  not command acknowledgements. No particle commands were used.
- Ignored originals/helper/action log: `build/qa-audit/node-pilot-playtest/`.
  Server observations: `build/qa-audit/node-pilot-client/stdout.log`.
  Selected inspected PNGs are preserved under `evidence/node-pilot/`.
- Auditor made no source edits, Java/Gradle launches, commits, or shutdown calls.

## Acceptance results

| ID | Check | Result |
| --- | --- | --- |
| N01 | Miniblock geometry, outline, collision, occlusion | **PASS for sampled bounds.** Matching outlines, center collision versus open margin, adjacent full-block faces visible. Exhaustive orientations/boundary rays UNVERIFIED. |
| N02 | Empty node/pump HUD | **PASS at scale2.** Names, No Aura, separate Stored Power; pump zero run time/rate and No Fuel. [Pump](evidence/node-pilot/03-pump-empty-scale2.png), [node](evidence/node-pilot/08-node-empty.png). |
| N03 | Live nonzero color sync | **PASS for white.** Ordinary dropped crystal changed empty node to White Aura27, matching server27 without reload; later receiver displayed5,200. [Absorption](evidence/node-pilot/09-node-live-white.png), [receiver](evidence/node-pilot/16-receiver-live.png). Other colors UNVERIFIED. |
| P01 | Ordinary dropped coal | **PASS.** One coal dropped and consumed; empty selected slot, Fueled, Run time320 s, Nominal rate300 Aura/s; NBT power320/speed300/inhibited0. [Result](evidence/node-pilot/05-coal-consumed.png). |
| P02 | No upper target freezes fuel | **PASS.** Game times41510 to42106:320 to320 across596 unpaused ticks. |
| P03 | Empty target spends fuel | **PASS.** +3 receiver, times42108 to42329:320 to309, eleven eligible cycles in221 ticks; both Aura stores zero. |
| P04 | External redstone freeze/resume | **PASS in baseline.** Fuel and Aura unchanged207 ticks with inhibited1; removal resumed nine cycles/900 Aura. [HUD](evidence/node-pilot/15-external-inhibited-scale2.png). |
| P05 | Adjacent+1 versus+3 control | **FAIL at+1; +3 flows.** First adjacent transfer inhibits pump with substantial fuel/Aura remaining. [Stall](evidence/node-pilot/11-adjacent-stall.png). |
| V01 | Positive transfer particles | **PASS for sampled white transfer.** Visible particles plus measured source decrease/receiver increase. [Positive1](evidence/node-pilot/13-transfer-1.png), [positive3](evidence/node-pilot/13-transfer-3.png). |
| V02 | Idle particle negative | **PASS, bounded sample.** Three matching inhibited-path captures after settling showed no trail; stores unchanged207 ticks. [Negative](evidence/node-pilot/14-inhibited-negative-3.png). Not an every-frame/all-colors claim. |
| R01 | Save/quit/reload persistence | **UNVERIFIED.** Deferred at parent handback. Pausing/autosaving is not reload testing. |
| H01 | HUD scales2 and3 | **PARTIAL: scale2 PASS, scale3 UNVERIFIED.** Empty/nonzero/fueled/inhibited scale2 HUDs fit; looking away clears HUD. Scale3 selected, but no scale3 node/pump HUD captured. |
| G01 | Adjacent guide pages | **Feed a Pump scale2 FAIL. Pump Readout scales2/3 PASS.** Actual held-book use and navigation. Feed a Pump scale3 UNVERIFIED. |

## Adjacent receiver failure

All quantities below are white Aura. No external redstone during either distance
comparison. Starting pump Aura was command-seeded, receiver empty, and all fuel
still came from the single ordinary coal drop.

| Offset / game time | Pump Aura | Receiver Aura | Fuel seconds | Speed | Inhibited |
| --- | ---: | ---: | ---: | ---: | ---: |
| +1 /44072 | 6000 | 0 | 267 | 300 | 0 |
| +1 /44262 | 5700 | 300 | 266 | 300 | 1 |
| +1 /44926 | 5700 | Not resampled | 266 | 300 | 1 |
| +3 /44929 | 6000, reseeded | Fresh empty receiver | 266 | 300 | 0 |
| +3 /45066 | 5300 | 700 | 259 | 300 | 0 |

Pump remained unchanged another **664 ticks** after the first stalled sample.
Replacing+1 with fresh+3 cleared inhibition and yielded seven transfers in137
ticks. Different rise legitimately changes rate; the failure is self-inhibition
and stopping, not unequal per-cycle amounts. Source signal methods do not make
this acceptable parity. Parent reported original-bytecode evidence of comparator
functions without direct-power overrides and owns the correction/retest.

## External redstone control

Redstone block `(39,180,40)`, +3 receiver retained:

| Game time | Pump Aura | Receiver Aura | Fuel seconds | Inhibited |
| --- | ---: | ---: | ---: | ---: |
| 45800 | 1700 | 4300 | 223 | 1 |
| 46007 | 1700 | 4300 | 223 | 1 |
| 47042 | External redstone removed | - | - | - |
| 47219 | 800 | 5200 | 214 | 0 |

Speed remained300 and Stored Power0. Redstone was reapplied after the final
sample to stabilize the fixture. Last sampled pump was800 Aura/214 fuel/300
speed; receiver5200. This is not a persistence pass or fresh handback state read.
Crystal donor client27 matched server27 and later displayed2 as natural
horizontal transfers proceeded. All-colors, nonzero Stored Power, and exact
packet latency are UNVERIFIED.

## Geometry observations

Command teleports positioned the survival player above center/margin; actual
gravity/collision determined resting height. Bridge positions are rounded to one
decimal and do not establish exact collision coordinates.

| Sample | Settled player position |
| --- | --- |
| Node center | 44.5 180.8 40.5 |
| Node margin | 43.9 180.0 40.5 |
| Pump center | 40.5 180.8 40.5 |
| Pump margin | 40.5 180.0 39.9 |

[Node beside stone](evidence/node-pilot/21-node-occlusion.png) shows the neighbor
side visible through the margin and miniblock-sized outline.
[Pump beside redstone](evidence/node-pilot/15-external-inhibited-scale2.png)
likewise retains the neighbor face. Exhaustive orientations and exact boundary
selection rays were not tested.

## Guide readability

- [Feed a Pump scale2](evidence/node-pilot/24-feed-pump-scale2.png): **FAIL**.
  First heading displays the entry name, "Pumps and Control". Final "rhythm."
  line reaches into and clips against the lower page border.
- [Pump Readout scale2](evidence/node-pilot/25-pump-readout-scale2.png): **PASS**.
- [Pump Readout scale3](evidence/node-pilot/30-readout-scale3.png): **PASS**.
  Heading and complete paragraph fit. [Video Settings](evidence/node-pilot/29-video-scale3.png)
  confirms scale3. This was the last independently verified screen.

## Preparation history and limits

Before handoff, all checks were explicitly UNVERIFIED while the parent repaired
the integration build. Source review covered node/pump blocks/models/registration,
HUD/text, entity sync/NBT, pump eligibility/fuel logic, transfer visuals/storage,
and focused tests. Those reads prepared the checklist, not runtime passes.

Auditor stopped bridge/UI actions on explicit parent request. Persistence,
scale3 HUD, Feed a Pump scale3, additional colors, and corrected-binary checks
remain **UNVERIFIED**. No broad mod parity or original encyclopedia completeness
claim is made.

## Read-only follow-up: corrected client, 2026-09-22

Independent review of parent-operated runtime evidence only; **no bridge calls
or UI input by this auditor**. Parent reports104 tests/build passing for
`node-feedback-fix-client`. Reviewed its `stdout.log` and actual PNGs102..107
from the existing ignored capture directory. The baseline sections above remain
historical results, not claims about this corrected binary.

### Adjacent receiver correction: PASS

The parent removed the +3 receiver, placed the +1 receiver, seeded pump white
Aura6000, and removed external redstone. Screenshot103 shows6000/214 fuel and
no inhibition label. At game time57333 the server reports source0, receiver6000,
fuel189, speed300, inhibited0; screenshot104 shows No Aura/189 and the adjacent
receiver. Thus the complete seeded Aura supply actually transferred rather than
stalling after the first300. Fuel spent25 eligible seconds, not merely20:
target-empty cycles may continue spending after all Aura has moved, consistent
with the independently tested baseline rule.

**P05 baseline failure resolved in this focused corrected-binary recheck.**
This does not erase the original5700/300 stall or establish every pump variant's
parity. [Start](evidence/node-pilot/103-adjacent-fix-start.png),
[completed transfer](evidence/node-pilot/104-adjacent-fix-running.png).

### External redstone retained: PASS

Parent reapplied external redstone and reseeded source Aura6000. Receiver
retained6000 from the preceding test; no receiver-output injection is inferred
as a transfer pass.

| Game time | Pump Aura | Receiver Aura | Fuel | Speed | Inhibited |
| --- | ---: | ---: | ---: | ---: | ---: |
| 57813 | 6000 | Prior6000 | 165 | 300 | 1 |
| 58243 | 6000 | 6000 | 165 | 300 | 1 |
| 58734, after power removal | 0 | 12000 | 143 | 300 | 0 |

The inhibited interval spans430 ticks with no fuel or Aura movement. After
power removal, the additional6000 reached the receiver and fuel fell22, with
inhibited0 in the server sample. [Initial inhibition](evidence/node-pilot/105-fix-external-inhibition.png),
[held inhibition](evidence/node-pilot/106-fix-inhibition-held.png),
[post-transfer capture](evidence/node-pilot/107-fix-resumed.png).
Important ordering: screenshot107 shows Redstone inhibited because the parent
**reapplied redstone after the uninhibited server sample and before capture**.
It is not a screenshot of the unpowered moment and not evidence of a new stall.
Its receiver12000 and fuel143 corroborate the completed transfer.

### Reload persistence and scale3 HUD

**Persistence PASS for the sampled state across parent shutdown/relaunch.**
At23:10:30 the corrected client's server reports pump white800, fuel214,
speed300, inhibited1, Stored Power0; the +3 receiver retains5200. This matches
the baseline's final sampled800/214/300/0 and receiver5200, with external
redstone reapplied before save. Screenshot102 independently shows the restored
800/214/300/Fueled/Redstone inhibited readout. This is stronger than an autosave
acknowledgement but is a **cross-client relaunch** check, not the originally
planned same-client save/reload sequence. Exact same-client R01 remains unrun;
nonzero Stored Power persistence remains UNVERIFIED.

**Scale3 pump HUD PASS for the sampled states.** Screenshot102's complete
name, white count, Stored Power, run time, nominal rate, fuel and inhibition
lines fit at1280x720 without clipping or overlap. Screenshots104..107 also show
readable empty/fueled and nonzero/inhibited states. Chat output below is separate
from the HUD. Scale3 empty/nonzero *node* readouts remain UNVERIFIED.
[Reloaded scale3 HUD](evidence/node-pilot/102-fix-reloaded-pump.png).

### Remaining guide failure

The baseline Feed a Pump clipping failure remains **OPEN**. Parent reports a
source-only split into shorter Feed and Wire Rhythm pages; no corrected-page
runtime evidence was reviewed here, so that proposed fix is **UNVERIFIED**.
Additional colors and broad mod parity remain outside this bounded recheck.

## Final read-only guide recheck, 2026-09-22

Parent reports `node-guide-pagination-build` passed104 tests/build. This auditor
made no bridge calls or UI inputs and independently viewed actual saved images
124, 132, 127, and131, rather than accepting capture names as scale evidence.

**Feed a Pump baseline clipping: RESOLVED. Corrected first spread PASS at
GUI scales2 and3, 1280x720.** The left heading remains "Pumps and Control";
the complete shortened fuel paragraph ends with "strike them." well above the
lower border. The right "Wire and Rhythm" heading and complete paragraph ending
"transfer rhythm over time." also fit. Neither spread shows clipped text,
heading collisions, or text crossing the spine or page borders.

- [Corrected spread, scale3](evidence/node-pilot/124-feed-wire-scale3.png),
  with [GUI Scale3 confirmation](evidence/node-pilot/127-guide-fix-video3.png).
- [Corrected spread, scale2](evidence/node-pilot/132-feed-wire-scale2.png),
  with [GUI Scale2 confirmation](evidence/node-pilot/131-guide-scale2-confirmed.png).

Capture128 was not used as scale2 evidence: its name is misleading and parent
reports the attempted right-click did not change scale. The earlier OPEN guide
status records the pre-pagination review; this rendered recheck supersedes it
without erasing the original clipped screenshot24. This review covers only the
corrected first spread, not new verification of every guide page or mechanic.
Runtime and normal shutdown remained parent-owned throughout.
