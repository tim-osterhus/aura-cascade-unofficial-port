# Digger Composition and Traversal Retest Card

Prepared 2026-09-23. ALL checks below are PENDING / UNVERIFIED on the next build.
This card authorizes no launch or UI activity before explicit parent handoff.
Parent owns compilation, client launch, shutdown and memory-slot allocation.

Execution follow-up: [fourteenth results](2026-09-23-independent-fourteenth-playtest.md).
Keep this preparation card as a procedure, not a PASS ledger. Critical safety
addition after a recovered setup death: loaded floor predicates alone are NOT
sufficient. Remain Creative until TWO actual server samples at least500ms
apart show the correct supported position, OnGround1b and FallDistance0.0f.
Use nonflying Creative for traversal activation controls; do not switch to
Survival immediately after a teleport/fall. A death invalidates subsequent
GUI inputs until actual respawn/screen verification and inventory recovery.

## Scope and evidence

Retest the two mining return-value hooks, then bounded B3 Heels, Traveler's
Bricks and Rebounding Enigma controls. Do not repeat Fruit, Wand or capacity.
Keep the prior [Digger FAIL](2026-09-23-independent-thirteenth-playtest.md)
visible; append new-build results rather than replacing history.

Use the authenticated ignored bridge helper without printing its token.
Record fresh build/PID/listener/world identity and unique screenshots1240+
(check existing filenames before choosing numbers). Preserve action responses,
held components, actual server readbacks and viewed screenshots. An input ack
or a source formula is not a runtime PASS. Do not edit production/bridge code.

## Entry and preservation

1. Verify actual world core-systems-cac43cda and freeze world ticks on entry.
   Read fresh inventory, all four physical accessory slots, health, food,
   effects, game mode, position and relevant gamerules before setup.
2. Last saved baseline:22 inventory entries, three equipped items (wing,
   Shattered Stone in ring2, sash), ring1 EMPTY. Original bound ring remains
   inventory11 with boundFairies[0]; spare ring23 and tier3 ingot24 are original.
   Do not force this assumption if the new snapshot differs: preserve actual
   initial NBT and coordinate any unexpected difference with parent.
3. Physically stow wing in inventory12, Stone in13 and sash in17 only after
   verifying these slots are empty. Move original hotbar8 item to empty35.
   Leave original ring11 untouched. Reserve hotbar8 for QA and inventory25
   for the temporary ring when unequipped. Capture the physical slot state.
4. Stay Creative while moving to408.5,180,108.5 on the existing QA platform.
   Wait for loading and explicitly verify loaded stone408,179,108 BEFORE
   Survival. Freeze does NOT suspend player gravity. No flight, Haste, mining
   fatigue, armor effects or non-test accessories during mining controls.
5. Record settings before changing them; keep remote machines frozen except
   brief necessary sync/physics intervals. Do not touch occupied stations at
   160/180/160,360/180/240,480/180/288 or320, or424/180/264.

## Precise mining fixture

Target408,181,111 currently contains oak_planks from the completed Wand test.
Record that state, replace ONLY this block with stone for the retest, and
restore oak afterward. Leave neighboring dirt and all placed Wand destinations.

Use stance408.5,180,108.5 yaw0 pitch10, Survival, plain wooden pickaxe in
hotbar8, selected with key9 followed by at least250ms input settlement.
Seeded tools are test setup, not crafting/enchanter acceptance.

Plain tool command:
```text
item replace entity @s hotbar.8 with minecraft:wooden_pickaxe 1
```

Pair-only tool command:
```text
item replace entity @s hotbar.8 with minecraft:wooden_pickaxe[minecraft:enchantments={levels:{"aura:kaleidoscopic_orange":1,"aura:kaleidoscopic_yellow":1}}] 1
```

Create a fresh QA Ring of Binding, physically equip ring1 through B, then
normally use exactly four Digger charms. Do not inject boundFairies or summon
pre-bound fairies. Verify actual owner, role=digger and slots0..3; briefly
unfreeze/settle client sync before reading speed. Existing fairy-live.ps1 can
assist, but its setup assumes EMPTY hotbar8/ring1 and its support predicate
does not itself abort on failure: independently check those prerequisites.
Do not run its Remove path over a held pickaxe.

Use ONE fresh ring for all four controls; physically move it to empty25 for
off controls. Give temporary entities time to disappear after unequip and
verify ownership/count before the next measurement. No repeated fresh-ring
rerolls. Never adjust pickup delay on command-created item displays.

| Control | Held enchantments | Actual owned Diggers / equipped ring | Expected speed |
| --- | --- | --- | --- |
| Base | none | none / off |2.0|
| Digger alone | none |4 / on |2.519424|
| Pair alone | Orange1 + Yellow1 only |none / off |2.875|
| Combined | Orange1 + Yellow1 only |4 / on |3.621672|

Current FairyRoleLogic uses1.08^(count-1), so four gives1.259712. It does NOT
use1.06^4. The pair gives1.15*1.25 on stone with a correct tool. Avoid Red/Green
and any extra enchantments. Compare float readings with absolute tolerance
0.00001; this is not a wall-clock mining-time ratio test.

For EACH row call actual inspect_mining_target and retain source,
focusedBlockPos, targetBlockId, heldItemId, heldComponents, hasCorrectToolForDrops,
playerDestroySpeed and clientGameTime. Confirm target stone408/181/111,
wooden_pickaxe, correct-tool=true and exact intended components. Keep the
large component payload in local evidence; print only the relevant fields.
Also retain server ring/owned-fairy evidence and a viewed contextual image.

After the combined row physically unequip and verify return to2.875, then
plain tool2.0. Optional single ordinary ATTACK smoke check is sufficient;
replace the target if broken. First settled mismatch: report promptly, retain
one control, and stop repeating. Do not infer resolution from hook source.

## Bounded traversal fixture

Contracts: [traversal](2026-09-23-traversal-contract.md) and
[utility/equipment](2026-09-23-utility-equipment-contract.md). Source expectations
below are not yet acceptance. Survey an unused area in Creative, record its
coordinates/block state, then build a wide supported runway and contained
water catch away from all existing stations. Load/check the entire expected
travel and catch area before Survival; no blind teleport onto unloaded floor.

| Gate | Actual control and evidence |
| --- | --- |
| Heels physical BELT | Stow sash; equip Heels through B. Read actual step-height attribute while clear:2.0 on versus0.6 off, no other modifier. Walk into the same two-block rise without jump: off blocks progress; on climbs. Capture position change and actual input. |
| Heels ramp, if observable | Against a sufficiently tall wall, record actual horizontal collision and attribute on consecutive ticks: +0.3 per collision tick. Move clear and verify reset to2.0, then unequip0.6. If ticks/collision cannot be sampled reliably, mark this subcheck UNVERIFIED, not a failure inferred from timing. |
| Traveler's Bricks | Wide flat runway with one contact patch to avoid repeated boosts. Actual walk/sprint/jump contact, plus still/slow control. For a measured incoming3D velocity length>0.25, expected outgoing v+5*v/length(v); preserve direction, including vertical component. At or below threshold no boost. Read paired contact-adjacent motion if available; post-friction displacement alone cannot prove exact +5. Capture contact geometry and illumination where observable. |
| Rebounding Enigma | One tile, wide/deep water catch, no wing/Glider/protection in launch baseline. Real collision should set Y10 and preserve X/Z. Steer off the tile during ascent to prevent another bounce; observe water landing and health. Do not claim intrinsic fall immunity from a water landing. Use a separate short, nonlethal dry-drop control if needed to contrast landing health, with/without physically equipped intended protection; do not attempt a full-height unprotected dry landing. |

Allow only a short bounded attempt for exact velocity/ramp readback; if the
available observer cannot resolve contact timing, retain observed gameplay
and mark the exact numeric subcheck UNVERIFIED. No fabricated helper outputs,
production instrumentation, or unsafe death controls. Restore health between
nonlethal controls and record synthetic fixture setup separately from inputs.

## Cleanup and handback

1. Unequip the temporary Binding ring physically; verify no remaining owned
   QA Diggers. Confirm returned ring roles, then remove ONLY seeded QA items
   from individually verified slots. No broad kill/clear and no mutation of
   original ring11. Restore target408,181,111 to its recorded oak state.
2. Remove traversal test equipment physically. Restore wing, Stone and sash
   to original slots; ring1 stays empty if that was the new-run baseline.
   Move original hotbar8 item back from35. Check all22 original inventory
   entries AND all accessory components against the initial exact snapshot.
3. Restore initial gamerules/effects as applicable, health20/food20. Leave
   newly created traversal fixtures documented; remove only scoped temporary
   entities/items, never remote fixtures. Remove any QA-only forced chunks.
4. In Creative return to434.5,180,264.5 after verifying loaded stone support.
   Freeze, no open menu, record screen/GUI scale and complete readbacks. Return
   UI ownership to parent, keep the client open, and make no further calls.
