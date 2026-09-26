# Thirteenth independent live QA

2026-09-23, approximately10:09-10:24 local. Existing parent-launched client
PID26180, `thirteenth-independent`, LAB8. No Java/build/source edits or shutdown
by this auditor. Listener127.0.0.1:9876 matched `pid.txt`. Actual world path was
`build/qa-audit/core-systems/saves/core-systems-cac43cda`, Player918.
Parent supplied thirteenth build identity; later proposed fixes are not runtime
evidence. This is a bounded dev-client audit, not packaged/multiplayer closure.

## Findings

| Check | Verdict |
| --- | --- |
| Forbidden Fruit survival EAT/DRINK completion | PASS: apple, plain water potion and milk; unequipped controls produced no amulet effects. |
| Fruit actual early-release/slot-switch cancellation | PASS bounded controls: unchanged item and no effects after the canceled use window. |
| Fruit creative completion without consumption | PASS corrected water-potion trial: same stack remained and effect appeared. |
| Extinguisher owned-fairy LAVA/FIRE contrast | PASS: sampled LAVA became air, FIRE remained at the same fairy block. |
| Extinguisher owner fire | PASS: no-ring Fire159, then re-equipped Fire-20, health17 unchanged across that comparison. |
| Ring15 capacity/16th rejection | PASS:15 owned entities; extra charm remained count1. |
| Full-inventory release/no duplicate repeat | PASS clean repeat:15 returned, repeat remained15, all15 recovered. First contaminated measurement excluded. |
| Four-Digger client mining speed | FAIL: actual client destroy speed2.0 with and without four Diggers; settled repeat2.0, expected2.519424. |
| Wand real selection/live copy/paste | PASS for two plain blocks and player-relative placement. |
| Wand materials/occupied/protected controls | PASS: missing materials no writes; occupied glass preserved; exact charging; Adventure negative then Survival positive. |
| Inventory and original equipment preservation | PASS:22-entry starting/final inventory NBT exactly equal; observed accessory layout restored. No death this session. |

## Baseline and evidence

The observed baseline differed from shorthand handoff wording: there were22
inventory entries and THREE equipped accessories, not four. Original bound
Binding ring was in inventory Slot11 with `boundFairies:[I;0]`; first ring slot
was empty. Wing, Shattered Stone and sash were equipped.
[1202 actual baseline](../../build/qa-audit/node-pilot-playtest/1202-baseline-equipment.png).
Preserved this exact arrangement rather than silently equipping the ring.
The original slot24 tier3 ingot was retained throughout.

World frozen immediately after entry. Health/food20; naturalRegeneration=true,
randomTickSpeed3, doFireTick=true. Original support434,179,264 passed. Moved in
Creative to the prior walled408,180,108 fixture, then explicitly verified both
loaded chunk and stone support BEFORE any survival testing. Forbidden station
coordinates from the handoff were not modified. No new forced chunks.

Equipment swaps used actual B-screen clicks. Test materials/items and health/
hunger resets were commands, not survival acquisition evidence. Role bindings
used ordinary held charm use; no attachment or bound-list injection. Short
stepped/unfrozen periods advanced loaded simulation; unobserved remote outputs
remain outside this audit.

Primary evidence: [stdout](../../build/qa-audit/thirteenth-independent/stdout.log),
[actions](../../build/qa-audit/node-pilot-playtest/actions.jsonl), ignored
[Fruit helper](../../build/qa-audit/node-pilot-playtest/fruit-live.ps1) and
[physical-role helper](../../build/qa-audit/node-pilot-playtest/fairy-live.ps1).
Helper acknowledgements alone are not outcomes. Exact NBT, actual client
diagnostic observations and viewed captures support the findings below.

## Forbidden Fruit

Natural regeneration disabled. Hunger1s/amplifier255 made apples edible;
effects were cleared between cases. The test amulet was physically equipped in
the amulet slot, other accessory slots empty. Actual USE hold2 seconds completed
use; early-release cases held0.4 seconds; readbacks followed a2.3-second wait.

Unequipped survival controls: apple consumed with no active effects; plain
water became glass bottle with no effects; milk became empty bucket with no
effects. Logs record each before/after stack and missing active_effects field.
Command errors for absent NBT fields are interpreted as absence only alongside
the actual stack/control observations, not generic success/failure heuristics.

Equipped apple consumed and granted regeneration amplifier1, duration7186 at
readback, consistent with7200 minus elapsed ticks.
[1208](../../build/qa-audit/node-pilot-playtest/1208-fruit-on-apple.png).
Equipped water became glass bottle and granted fire_resistance amplifier5,
duration2576. Equipped milk became bucket and granted water_breathing
amplifier0, duration3914, visible after milk's normal completion.
[1211 milk](../../build/qa-audit/node-pilot-playtest/1211-fruit-on-milk.png).

Early-release apple stayed count1, hunger stayed16 and no active effects;
[1209](../../build/qa-audit/node-pilot-playtest/1209-fruit-cancel-apple.png).
Early-release water retained the original potion component/count1, no effects;
[1212](../../build/qa-audit/node-pilot-playtest/1212-fruit-cancel-water.png).
Milk interrupted by switching to hotbar1 after400ms stayed milk/count1 with
no effects. Continuing held USE opened the book in the new slot, as actually
shown by [1213](../../build/qa-audit/node-pilot-playtest/1213-fruit-switch-milk.png).
This is the switch-cancellation result, not a milk completion image.

The first subsequent creative attempt1214 was still on BookScreen, so it is
EXCLUDED, not a mod failure. Added a250ms selected-slot settling wait and
repeated on the actual game screen. Creative water retained the identical
water-potion stack/count1 and granted fire_resistance amplifier5/duration2575.
[1216 corrected](../../build/qa-audit/node-pilot-playtest/1216-fruit-creative-corrected.png).
No creative count decrease was used to infer completion. Fruit was physically
removed and all effects cleared before the owner-burn comparison.

## Extinguisher

One fresh ring, one normally bound Extinguisher, actual owner/role entity read.
Frozen sampled fairy position409.5804,181.0512,107.6097, block409,181,107.
Placed temporary netherrack below it and LAVA at that actual block; LAVA
predicate passed before stepping. One tick later air predicate passed; fairy
position409.6941,181.0571,107.7692 remained in the same block.
[1217](../../build/qa-audit/node-pilot-playtest/1217-extinguisher-lava-removed.png).
Placed FIRE there, stepped one tick: FIRE remained, fairy position409.7844,
181.0680,107.9430 still in the same block.
[1218](../../build/qa-audit/node-pilot-playtest/1218-extinguisher-fire-preserved.png).
doFireTick was temporarily false to isolate the contrast, then restored true.
Removed the temporary fire/support. These screenshots have a fire overlay;
block acceptance is the actual server predicates/positions, not an unobscured
visual assertion or a claim that the player's overlay cleared in this subcase.

Separate owner control: physically unequipped ring, stepped12, verified no
nearby fairy. In Survival contacted command-placed ordinary fire for1.3s,
then moved to dry supported floor and removed the fire. Actual Fire159/Health17
without ring. Physically re-equipped, stepped2: Fire-20/Health17.
[1220](../../build/qa-audit/node-pilot-playtest/1220-owner-fire-cleared.png).
No water, fire-resistance effect or health reset between those two readbacks.
Restored health afterward. No owner lava-contact claim is made here.

## Capacity and release

Normally bound15 basic charms into a fresh physically equipped ring; server
reported15 owned fairies/slots. Given extra charm stayed count1 after normal
use; no16th fairy. Physically unequipped the full ring into selected hand.
Original stacks occupied25 slots while their three accessories were stowed;
ring plus ten64-count QA barrier stacks filled all36 inventory slots.

First release trial1223 is NOT clean conservation proof: pre-release query
already found one charm entity from the recent `/give` setup. After release
there were16, and the initial recovery changed its pickup delay as well. This
setup artifact was not called a mod duplication. All temporary material from
that trial was accounted for and the measurement was repeated.

Clean repeat: normally rebound15, stepped20, verified zero nearby charm drops,
and again verified the16th charm count1 survived rejection. Removed that extra
setup charm. Filled inventory, then normal SHIFT+use released15 count1 entities;
ring NBT lost boundFairies. Repeating use while still sneaking left exactly15,
not30. [1225](../../build/qa-audit/node-pilot-playtest/1225-full-release-clean-fifteen.png).
Removed only QA filler; after30 ticks the drops merged to one real count15
stack, [1226](../../build/qa-audit/node-pilot-playtest/1226-release-recovered-clean.png).
That image is ground-stack proof, NOT yet pickup proof. Teleported that scoped
real stack to the player and stepped20: inventory gained15, no charm entity
remained, and cleanup clear returned15. No pickup-delay mutation in this clean
recovery. Original bound ring/extra empty ring never cleared or modified.

## Digger failure

LAB8 `inspect_mining_target` reports an actual read-only client call to
`Player.getDestroySpeed(BlockState)`, not a hand-computed expected value.
All samples targeted stone408,181,111 with an unenchanted wooden pickaxe,
no active effects, Survival, same position and pose.

| Actual observation | UTC timestamp | Client gametime | Destroy speed |
| --- | --- | --- | --- |
| No test ring |20:19:55.0517252|118587|2.0|
| Four normally bound, owned Diggers |20:19:59.872108|118600|2.0|
| Fresh four-Digger settled repeat, after2.2s unfreeze |20:21:22.8344393|118714|2.0|

Expected with four is `2 * 1.08^3 = 2.519424`. The actual ratio was1.0, not
1.259712. Four server entities were confirmed with correct owner/role and
Slots0..3. Returned physically unequipped ring NBT contained four Digger roles.
One two-second ATTACK did break stone, proving working real mining input, not
the bonus. Failure was reported promptly and the one settled repeat ended
testing. All observations persist in the action log; screenshots1227/1229/1231
are contextual frames, NOT on-screen displays of the diagnostic speed.

Parent suspects interaction between two cancellable RETURN injectors and is
preparing a composable fix. That is parent source diagnosis, not independently
verified root cause here. Preserve this FAIL until a new compiled client is
actually retested. The older inconclusive timed trial is not rewritten as PASS.

## Wand

Seeded only the wand/material fixtures. Real normal block uses selected
408,181,111 then409,181,111; NBT firstPos409/secondPos408 verified reversal.
Actual SHIFT+use cycled Selection -> Copy -> Paste. Copy at player408.5,180,
108.5 recorded normalized source408..409,181,111 and offset0,1,3.
AFTER copying, changed the source stone at408,181,111 into oak planks; second
source remained dirt. No clipboard-NBT injection.

Survival paste from412.5,180,108.5 with neither material left targets412/413,
181,111 air. [1233](../../build/qa-audit/node-pilot-playtest/1233-wand-missing-materials.png).
Seeded3 oak planks and3 dirt; occupied second destination with glass. Actual
paste produced OAK, not the original copied-time stone, at412,181,111; glass
at413 remained. Inventory became2 oak/3 dirt, exactly one successful charge.
[1234](../../build/qa-audit/node-pilot-playtest/1234-wand-live-occupied-consumption.png).

Moved player408.5,180,104.5 on asserted support, targeting fresh air408/409,
181,107. In Adventure, actual paste left both air and retained2 oak/3 dirt.
[1235](../../build/qa-audit/node-pilot-playtest/1235-wand-protected-adventure.png).
Changed only game mode to Survival and repeated: actual oak/dirt predicates
passed and inventory became1 oak/2 dirt.
[1236](../../build/qa-audit/node-pilot-playtest/1236-wand-survival-two-blocks.png).
This is an Adventure build-permission negative, not multiplayer claim-region
or spawn-protection coverage. Captures face sky to avoid block-use ambiguity;
geometry outcomes are actual server block predicates, not image inference.
Source and placed fixture blocks remain only within the reserved old platform.

## Restoration and handoff

Removed seeded wand/materials, test rings/charms, pickaxes, Fruit and QA filler.
Original three equipped items restored physically; original bound ring remains
inventory11 and its attachment slot remains empty, exactly as observed at start.
[1237](../../build/qa-audit/node-pilot-playtest/1237-actual-baseline-restored.png).
Initial and final22-entry inventory NBT strings were exactly equal,1260
characters each, including slots/counts/components. No death this session.

Final:434.5,180,264.5 yaw0/pitch0, Creative, health20/food20, no active effects,
frozen, GUI3/no menu, control mode active. Loaded stone434,179,264 verified.
naturalRegeneration=true, randomTickSpeed3, doFireTick=true restored.
[1238 handoff](../../build/qa-audit/node-pilot-playtest/1238-thirteenth-handoff.png).
UI explicitly returned via parent task message. No further bridge calls;
client left open for parent's normal shutdown. No source, bridge or build edits.

UNVERIFIED: Digger fix/retest, exact wall-clock mining ratio, creative apple/milk,
all food identities, death/spectator completion negatives, arbitrary modded or
block-entity Wand placement, wand reload, multiplayer/protected-region controls,
other fairy roles and unrelated progression. No broad acceptance is implied.

## Closure and next-build follow-up (2026-09-23)

Parent reports the thirteenth-independent client closed normally with exit0:
peak working set1340.3MiB, peak private1861.5MiB. These are parent monitor
results, not independently sampled memory. No further UI/bridge calls were made
by this QA lane after handoff.

Parent identified the source cause as two cancellable RETURN injectors into
Player.getDestroySpeed suppressing one another. Independent read-only source
inspection now confirms BOTH KaleidoscopicMiningMixin and FairyMiningSpeedMixin
use chainable MixinExtras ModifyReturnValue and transform the incoming result.
This is source-fix evidence only: the replacement build and live retest are
pending. The thirteenth runtime Digger FAIL above remains unchanged/unresolved.

The next bounded session is specified in the reusable
[Digger/composition and traversal card](2026-09-23-digger-traversal-retest-card.md).
It requires explicit new-client handoff. No Java, build, or UI work was done
during this preparation; PsiAI owns the heavy slot. Fruit, Wand and capacity
are not scheduled for repetition.

Fourteenth follow-up: the new compiled runtime independently passed the exact
base2.0 / four-Digger2.519424 / pair-only2.875 / combined3.621672 controls and
unequip restoration. See the [fourteenth report](2026-09-23-independent-fourteenth-playtest.md).
The thirteenth FAIL above remains the historical baseline; its bounded mining
defect is now resolved by actual new-build evidence, not source inspection alone.
