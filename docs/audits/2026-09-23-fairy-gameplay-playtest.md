# Fairy gameplay and storage search: independent live QA

Date: 2026-09-23, approximately 05:26-05:48 local. Parent-launched PID4956,
`integrated-restoration-client-tenth-auth`, disposable `core-systems-cac43cda`.
Listener independently matched 127.0.0.1:9876/PID4956. Startup log reports
Minecraft 1.21.1, Fabric Loader 0.19.1, Aura 0.2.0+1.21.1.
Parent supplied tenth-snapshot validation: 212 tests, 211 passed and one guide
phrase failure; NOT a fully green build. Parent's newer six source fixes were
not compiled into this session. No build, launch, source edit or shutdown here.

Bridge LAB7 SHA supplied by parent:
`148a8257855086f0ab558d11ee6c44addc1ab2d46c9ca9c7d54907726477cb96`.
This is bridge identity, not an independently measured mod artifact hash.
Parent reported questline=true; no config change or quest acceptance in this run.

## Results

| Bounded check | Result |
| --- | --- |
| Storage search, no-results, clear | PASS at GUI3; actual text input and filtered results viewed. |
| Fetcher pickup, no-ring control | PASS: stationary owner collected the previously out-of-reach dropped item only after re-equipping. |
| Fighter target damage | PASS: isolated target 24 -> 22.5 health; no-ring target remained 24. |
| Debuffer target effects | PASS: all six expected effects on isolated non-undead hostile. |
| Lighter creation and expiry | PASS: actual light block, visible illumination, then air/darkness after removal and bounded accelerated random ticks. |
| Four-Digger mining speed | UNVERIFIED: real mining input worked, but fixed-duration trials did not establish the expected speed difference. |
| Three-Glider actual fall | PASS: identical 16-block falls, control 20 -> 7 health, equipped 20 -> 20. |
| Shooter reacquisition after arrow removal | PASS bounded phase-controlled real-arrow check; an earlier uncontrolled second shot was not empowered and is retained below. |
| Normal sneak-use release | PASS: server sneaking predicate true; bound test ring became empty and returned one correctly typed charm. |
| Preserve original inventory/loadout | PASS after recovery: final inventory NBT exactly equal to initial; four original accessories restored, original Fairy still bound. |
| Initial remote fixture safety | FAIL, auditor setup: unloaded floor fill followed by survival teleport caused death. Fully disclosed/recovered below, not a fairy defect. |

## Evidence and method

Primary [client stdout](../../build/qa-audit/integrated-restoration-client-tenth-auth/stdout.log)
contains actual server NBT, health, entity effects and commands. The ignored
[action log](../../build/qa-audit/node-pilot-playtest/actions.jsonl) records inputs
and dispatcher results. [Finite role helper](../../build/qa-audit/node-pilot-playtest/fairy-live.ps1)
equips/unequips using B-screen mouse clicks and binds by ordinary held-item use.
Command acknowledgements alone are not acceptance. Linked screenshots below
were actually viewed; some GUI shots have an inventory tooltip across the middle,
so role/count proof comes from server entities and returned ring NBT as well.

Fresh test ring for each of seven roles; only a QA identifier was command-seeded
on the empty rings. No injected bound-role lists, equipped attachments or fairy
entities. Typed charms were seeded materials, then consumed by actual normal
use. Original accessories were isolated in ordinary inventory. One ring slot
held the test ring, with the other three accessory slots empty. Survival tests,
natural regeneration disabled; original setting true restored at the end.

The final reserved area is x400..416/z100..116, stone floor y179 and three-block
perimeter walls. Parent stations were not edited. Short unfreezes/stepped ticks
necessarily advanced other loaded simulation; no unobserved remote output is
accepted as evidence. No broad kill or clear selectors were used.

## Storage search

Opened the existing coordinator at124,180,116 using normal empty-hand use.
[1002](../../build/qa-audit/node-pilot-playtest/1002-browser-search-start.png)
shows plain128 and named25 stacks, power945/11. Actual `type_text` Control
produced [1003](../../build/qa-audit/node-pilot-playtest/1003-search-control.png),
only the named25 stack. Appending zzzz produced
[1004 No matches](../../build/qa-audit/node-pilot-playtest/1004-search-no-results.png).
Repeated actual Backspace presses cleared the field and restored the grid in
[1006](../../build/qa-audit/node-pilot-playtest/1006-search-cleared.png).
A held Backspace removed only one character;1005 is NOT clear evidence.
No retrieval/deposit, power change or storage content mutation in this check.
This closes the prior bridge-text gap for this bounded GUI3 sequence; GUI2
search and broader search syntax were not repeated.

## Setup failure and recovery

At05:29:44 remote floor/wall fills returned position-not-loaded errors. The
auditor incorrectly continued to teleport and switch to survival before
verifying support. At05:29:49 Player918 fell from a high place.
[1009](../../build/qa-audit/node-pilot-playtest/1009-gear-inspect.png) actually
shows DeathScreen, despite its filename. Also, the first accessory click raced
screen opening and returned no screen: the wing had remained equipped, with
the other three pieces stowed. Neither1007 nor1008 is valid completed-fixture proof.

The game remained frozen. Recovered all25 dropped stacks using a spatially
bounded recovery tag, zeroed pickup delay and teleported actual drops for normal
pickup after respawn. Angelsteel drops were recovered separately without
stepping ticks, preserving 1 tier1,3 tier2,1 tier3 rather than allowing ground
conversion. No substitute copies of original items were injected. No recovery
drop entities remained. Reordered recovered stacks via actual GUI transactions.
Original loadout/inventory equality was verified at final handoff, not assumed.
Completed the platform and asserted its support before further survival checks.
Death/respawn and later vanilla recipe/Monster Hunter unlocks and XP are fixture
side effects; no claim that all player statistics/advancements were unchanged.

## Role observations

### Fetcher

Fresh ring bound one Fetcher, owner UUID matched Player918, Role fetcher/Slot0.
[1015](../../build/qa-audit/node-pilot-playtest/1015-fetcher-bound.png) shows the
physical ring and empty other slots. Unequipped it, seeded a uniquely marked
flint and dropped it with Q. After50 ticks it remained at408.522,180,113.575,
PickupDelay0, while the owner stayed408.5,180,108.5 with no flint in inventory.
Re-equipped the same ring and stepped20 ticks: one flint in inventory, no flint
entity, owner position unchanged. [1017](../../build/qa-audit/node-pilot-playtest/1017-fetcher-collected.png).
Only the temporary flint was removed afterward. The final observation proves
remote collection; no frame-by-frame trajectory claim is made.

### Fighter and Debuffer

Separate fresh rings and separate tagged NoAI pillagers, not a mixed-role ring.
Pillagers avoid undead poison immunity; no external attacks were issued during
measurement. Fighter target remained24 health after20 no-ring ticks, with no
effects. Re-equipping and stepping6 ticks yielded22.5, an actual1.5-point hit.
[1020](../../build/qa-audit/node-pilot-playtest/1020-fighter-hit.png).
No claim that every three-tick attempt bypasses vanilla hurt immunity.

Debuffer's new target had nausea, slowness, poison, hunger, weakness and wither
after12 ticks, each duration196 at readback (default amplifier0).
[1022](../../build/qa-audit/node-pilot-playtest/1022-debuffer-effects.png).
The no-ring target in the preceding control had no effects; a separate timed
Debuffer-only no-ring target was not repeated. Killed only the uniquely tagged
test targets; loot table was empty. Target-selection parity remains untested.

### Lighter

Built an enclosed dark stone cell within the platform; no-ring darkness visible
in [1023](../../build/qa-audit/node-pilot-playtest/1023-lighter-dark-control.png).
With randomTickSpeed0, bound one Lighter and stepped12 ticks. Actual
`aura:fairy_torch` predicate passed at408,181,107; room visibly lit in
[1025](../../build/qa-audit/node-pilot-playtest/1025-lighter-room-lit.png).
Removed the ring, stepped12 ticks and confirmed no nearby fairy. Temporarily
set randomTickSpeed4096, stepped10, then restored3. The recorded torch position
was air and room dark in [1026](../../build/qa-audit/node-pilot-playtest/1026-lighter-expired.png).
This proves accelerated random-tick expiry, not an ordinary lifetime estimate.
Dark-room walls/roof were removed afterward; platform floor/perimeter remain.

### Digger

Four ordinary bindings produced four owned Digger entities, Slots0..3. Identical
stone408,181,111, same player pose, unenchanted wooden pickaxes, no Haste/effects.
Two no-ring and two four-Digger ATTACK hold_seconds1 trials all left stone.
Viewed crack-progress samples [control](../../build/qa-audit/node-pilot-playtest/1030-digger-control-1.png)
and [equipped](../../build/qa-audit/node-pilot-playtest/1032-digger-equipped-1.png)
do not establish a reliable timing ratio. A subsequent two-second equipped hold
broke the block, actual air predicate and pickaxe damage1 in
[1034](../../build/qa-audit/node-pilot-playtest/1034-digger-two-second-break.png).
The custom fairy sprite is visible there; animation/camera-facing behavior was
not independently measured. Expected1.259712 multiplier remains UNVERIFIED,
not inferred from source or counted as a speed pass. No calibrated server/client
break-time comparison was completed. Removed only the bounded test cobblestone
drop and seeded pickaxes.

### Glider

Contained shaft centered403,103 with verified floor y179, surrounding walls
y180..198. Survival teleports to legs y196 initiated actual falls to y180;
no synthetic fall-damage command. No-ring20 ->7 in
[1035](../../build/qa-audit/node-pilot-playtest/1035-glider-no-ring-fall.png).
Reset health, physically bound three Gliders, confirmed three owned entities,
then identical fall20 ->20 in
[1037](../../build/qa-audit/node-pilot-playtest/1037-glider-protected-fall.png).
Natural regeneration remained false and no protection amulet was equipped.

### Shooter and normal release

Seeded plain bow/arrows; actual held USE charged and released each shot.
No-ring arrow had base damage2. First equipped shot had12. Removed that actual
test arrow, kept the same Shooter fairy/ring, then a free-running second shot
remained2: its time within action range was not established, so this attempt
does NOT prove reacquisition or a cache defect.

Bounded diagnostic retained the same fairy UUID
`[I;-1811676255,748833898,-1289757584,-2128100206]` and removed the second arrow.
At frozen gametime111029 (phase2 mod3), normally fired another real arrow.
Before stepping it was at the owner with base damage2. One tick later, same
arrow UUID `[I;48520590,-1835515062,-1193805486,111940522]` had damage12,
crit1 and moved forward. [1044](../../build/qa-audit/node-pilot-playtest/1044-shooter-controlled-reacquisition.png)
plus05:44:56 stdout provide the bounded reacquisition proof. Critical flag was
already1 on the fully charged bow shot, so its transition is NOT independently
attributed to the fairy. Cleanup targeted only arrows inside the reserved area.

Physically unequipped that bound Shooter ring into hand. Held SHIFT2 seconds;
actual server inline entity-properties predicate `flags.is_sneaking=true`
passed immediately before normal use. Returned inventory showed the QA ring
without boundFairies and exactly one charm with fairyRole shooter. No nearby
fairy remained after12 ticks. [1045](../../build/qa-audit/node-pilot-playtest/1045-shooter-release.png)
is the action frame; subsequent stdout NBT proves the result, not the use ack.
Removed only this test ring and returned test charm. Original bound ring untouched.

## Restoration and handoff

Final inventory's21-entry NBT payload was byte-for-byte equal to the initial
05:26:15 payload, including slots, counts and components. Four original pieces
were restored by GUI, [1048](../../build/qa-audit/node-pilot-playtest/1048-restored-four-slots-clear.png).
[1046](../../build/qa-audit/node-pilot-playtest/1046-original-equipment-restored.png)
confirms original Binding ring still1/15 Fairy. No original item loss remained.
All seven seeded role rings, their charms, flint, bows, arrows and pickaxes were
consumed/removed as documented, not left among original inventory.

Final:408.5,180,108.5, yaw0/pitch0, creative, health20/food20, no active effects,
naturalRegeneration=true, randomTickSpeed3, frozen, GUI3/no menu, control mode
active. [1049](../../build/qa-audit/node-pilot-playtest/1049-supported-handoff.png)
shows the supported walled area. Original ring's role entity was not re-stepped
after final re-equip; reconciliation can occur when parent resumes ticks.
Client remained open; no save/title, quit, restart or further bridge calls after
explicit UI return. Fixture platform and fall shaft remain for parent reuse.

UNVERIFIED/deferred: Digger speed ratio, all remaining fairy roles, broad target
ordering/parity, multiplayer, capacity/full-inventory release, fresh reload,
sprite animation, GUI2 search, optional Fruit eat/drink/cancel and uncompiled
Extinguisher changes. No Angelsteel combined-item behavior test was attempted.
