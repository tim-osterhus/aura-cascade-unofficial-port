# Independent Fourteenth Gameplay Playtest

2026-09-23, approximately10:57-11:11 local. Parent-launched PID33056,
fourteenth-gameplay, LAB8. Parent reports234 tests/58 suites and build hash
282c949d14073c207eb95095c3e622bd30e5b184ef4026b4b24f84f68a207490.
Those build results are parent evidence, not an independent build here.

Entered the sole world normally from TitleScreen. Actual get_world_info:
build/qa-audit/core-systems/saves/core-systems-cac43cda, Player918.
GUI3,1280x720. Parent retained monitoring; this lane had exclusive input until
explicit handback. No builds, Java launches, production edits or commits.

Primary ignored evidence:
[server/client log](../../build/qa-audit/fourteenth-gameplay/stdout.log),
[action responses](../../build/qa-audit/node-pilot-playtest/actions.jsonl).
Screens1401-1429 are in that action directory. Log timestamps below are local.
Command-seeded test tools, charms and blocks are fixture setup, not acquisition
or crafting acceptance. Actual inputs and observed results are distinguished.

## Results

| Gate | Result |
| --- | --- |
| Digger four-control composition | PASS: exact actual client mining-speed readings below |
| Heels physical BELT, clear attribute, two-block walk, unequip | PASS in grounded nonflying Creative |
| Heels collision ramp +0.3 and first-clear reset | UNVERIFIED; not extended into an edge-case test |
| Road boost versus matched ordinary stone input | PASS for actual gameplay activation/direction of travel |
| Road stationary negative and0.8 contact height | PASS; two stable actual position samples |
| Road exact +5 vector/strict threshold/vertical direction | UNVERIFIED; no paired contact-frame vector sample |
| Enigma real walk launch and off-tile water landing | PASS in nonflying Creative |
| Enigma exact contact Y10/XZ preservation | UNVERIFIED numerically; first post-contact Y9.721600189 observed |
| Survival landing/protection/fall immunity | UNVERIFIED; deliberately not claimed |
| Original inventory/accessory preservation | PASS after documented recovery: exact22-entry inventory restored |
| No-death fixture procedure | FAIL: one QA setup death, recovered; not a mod failure |

## Mining composition

Survival, stone408/181/111, wooden pickaxe, correct-tool=true, no active effects.
Original wing/Stone/Heels physically stowed. The original bound inventory ring
was never equipped or modified. A fresh QA ring was physically equipped and
four charms normally used, not injected into ring NBT. At10:59:00 actual entity
NBT showed four Diggers, same player owner, slots0..3. Brief unfreeze settled
client sync; subsequent readings used the actual LAB8 client
Player.getDestroySpeed(BlockState) observer.

| Actual control | Client game time | Actual speed | Expected |
| --- | --- | --- | --- |
| Plain pickaxe, ring off |118771|2.0|2.0|
| Plain pickaxe, four Diggers |118823|2.519424|2.519424|
| Orange1 + Yellow1, four Diggers |118827|3.621672|3.621672|
| Same pair, physically unequipped ring |118839|2.875|2.875|
| Plain pickaxe restored, ring off |118839|2.0|2.0|

All readings targeted the same stone and reported correct-tool=true. Observer
responses retain held components; actual server NBT10:59:05 independently
confirmed only Orange1+Yellow1 enchantments. At10:59:33 the unequipped ring in
slot25 contained boundFairies[13,13,13,13]. No Red/Green pair or other buffs.
The tool/ring were removed only after verifying these QA slots. Owned fairies
were absent after unequip. Target restored to its prior oak_planks state.

[1406 physical ring](../../build/qa-audit/node-pilot-playtest/1406-four-diggers-equipped.png)
and [1409 unequipped](../../build/qa-audit/node-pilot-playtest/1409-ring-off-control.png)
were visually reviewed; an old hover tooltip obscures part of these frames,
so they are not the numeric speed evidence. Full diagnostic responses are in
actions.jsonl. The earlier thirteenth2.0 failure remains historical; this
fourteenth runtime resolves that bounded Digger/composition failure.

## Setup death and recovery

At11:01 the auditor teleported in Creative to the new runway area BEFORE the
floor existed. The player fell to about129.7 while construction proceeded.
After placing loaded stone and teleporting back to180, the auditor switched
to Survival without first waiting for settled OnGround/FallDistance samples.
At11:01:09 the log reported fell from a high place. The later floor predicate
did not establish that accumulated falling state had cleared. This was an
avoidable QA procedure failure, not an equipment/landing mod defect.

[1412 death screen](../../build/qa-audit/node-pilot-playtest/1412-heels-on-stair.png)
was actually reviewed. Screens1410-1412 and their rejected GUI clicks are
INVALID Heels evidence. A test Heels replacement command issued before the
death was noticed affected the dead player and was not counted as an equipped
test item; it did not appear in the recovered inventory.

Parent was notified immediately. The world remained frozen. Actual scoped
death-area query found25 dropped stacks, corresponding to22 originals plus
three physically stowed accessories. After normal Respawn, Creative and TP,
only these actual drops in x536..544/y175..185/z104..112 received PickupDelay0.
They were collected at the player before the attempted item teleport, which
therefore returned no entity. One tick was stepped. Actual inventory became
25 stacks and the scoped remaining-item predicate failed (none remained).
No broad kill, clear, or replacement of original inventory was performed.

All25 stacks retained their counts/components, including the named cobble,
bound ring and all three ingot tiers. Physical GUI swaps restored original
ordering. Two samples600ms apart at11:03:25-26 showed position540.5/180/108.5,
OnGround1b and FallDistance0.0f. The parent then explicitly allowed remaining
movement tests in grounded, nonflying Creative. No further death occurred.
XP/death statistics were not restored or asserted equal: the preservation
claim is inventory/accessories, not exact full player NBT.

## Heels

Used the ORIGINAL recovered Heels, physically moved from inventory17 to BELT;
no new injected accessory was needed. Other accessories stayed stowed.
Actual abilities at11:04:30: Creative, flying0. Clear step attribute0.6 off.
W held1s with no jump stopped at540.5/180/110.699999988 against the two-block
rise. With Heels physically equipped, actual clear attribute2.0 and the same
walk crossed the obstacle. A widened ledge made the elevation explicit:
W0.8s from540.5/180/108.5 ended540.5/182/111.949989930, OnGround1b.
[1418 actual raised ledge](../../build/qa-audit/node-pilot-playtest/1418-heels-on-raised-ledge.png).
Physical unequip and two stepped ticks returned the attribute to0.6.
No jump command or flight was used. Collision-ramp timing was not measured.

## Traveler's Bricks

Single-width patch552/179/106..110 in a wide stone runway. At rest on the
patch, two samples600ms apart at11:07:51 stayed552.5/179.8/108.5, OnGround1b,
Motion[0,-0.0784000015,0]. This observes the0.8 contact height and no stationary
boost; it does not independently measure light emission.

Matched CTRL2s plus W1s from548.5/180/108.5, yaw-90:

| Surface | Final X | Final Z |
| --- | --- | --- |
| Actual Road |561.9254214514062|108.5|
| Ordinary stone replacing the same patch |553.8268373567699|108.5|

Both settled atY180. The actual road produced a clear increased forward
displacement with no lateral drift. [1421 road result](../../build/qa-audit/node-pilot-playtest/1421-road-input.png)
was viewed; matched stone result is recorded at11:09:00. A SHIFT2s+W1s trial
from551.5 ended552.7726082446392/180/108.5 without a boost. Because the sneaking
trial did not establish a contact-frame vector, this is a limited slow-input
negative, NOT proof of the exact <=0.25 threshold. Settled Motion samples do
not capture incoming/outgoing boost vectors; exact+5 and3D direction remain
UNVERIFIED. The patch was left in place.

## Rebounding Enigma

Tile574/179/108, wide water catch575..596/177..179/100..116 over stone176.
Loaded catch/water/support predicates passed. Before input, two actual samples
600ms apart at571.5/180/108.5 gave OnGround1b/FallDistance0.0f. No protection
gear, wing, ring or flight. W1.2s was the only launch movement input.

Actual successive samples11:09:29-31 showed approachX571.93,572.54,573.18,
573.83; first post-contact X574.478/Y180 had MotionY9.721600189208985, then
X574.976/Y199.092,575.615/Y234.266,576.270/Y265.493, and576.983/Y311.547.
Z stayed108.5. This is a real high launch, not a teleport or injected velocity.
[1425 airborne](../../build/qa-audit/node-pilot-playtest/1425-enigma-airborne.png).
ExactY10 was not sampled at contact;9.7216 is consistent with one normal
gravity/drag update from10, an inference rather than an observed10 value.

Forward movement carried the player off the launch tile. The water landing
was visually reviewed in1426; final11:10:32 actual position was
577.7149609151759/177/108.5, OnGround1b, FallDistance0.0f, flying0, MotionY-0.005.
[1427 settled water](../../build/qa-audit/node-pilot-playtest/1427-enigma-settled-water.png).
No repeated bounce. Creative health20 is NOT evidence of fall immunity or
protection behavior. No unprotected dry landing was attempted.

## Restoration and handback

Original wing, Stone and Heels were physically restored to their original
attachment slots; first ring slot empty, original bound ring inventory11.
[1428 restored slots](../../build/qa-audit/node-pilot-playtest/1428-original-accessories-restored.png).
Initial10:57:11 and final11:10:34 inventory NBT payloads are case-sensitive
exact matches,1260 characters each,22 entries. No original item replacement.

Final434.5/180/264.5, Creative flying0, health20/food20, frozen, GUI3/no menu.
Loaded stone434/179/264 passed; two actual samples600ms apart confirmed that
position, OnGround1b and FallDistance0.0f.
[1429 handoff](../../build/qa-audit/node-pilot-playtest/1429-fourteenth-handoff.png).
Explicit UI ownership returned to parent; client left open, no further bridge
calls. No gamerules changed and no forced chunks added.

Reserved QA blocks left documented: stone runway520..600/179/100..116;
ledge539..541/180..181/111..115; Road552/179/106..110; Enigma574/179/108;
catch described above with stone base574..596/176/100..116. Remote fixtures
were not edited. Brief world unfreeze for Digger sync may advance remote
machines; no unseen progression/output is claimed. Fruit/Wand/capacity were
not repeated, nor was other gameplay added.
