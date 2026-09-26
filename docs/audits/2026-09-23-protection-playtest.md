# Protection amulets: independent live QA

Date:2026-09-23,04:16..04:26 local. Existing parent-launched PID29072,
seventh198-test integrated build, Player918 in the isolated core-systems world.
Build identity/test count supplied by parent; no build, restart, Java launch,
source edit or commit by this auditor. Exclusive UI ownership was returned at
the end; the client and world remained open.

## Method and controls

Six protection-family cases plus one real lava-contact case. Source expectations
were read from `ProtectionAmuletProfile` and `AuraItems`. Equipment acceptance
used actual B-screen mouse transactions, never attachment/NBT injection.

Initial player:171.5,180,160.5, creative, health20, food20; GUI3 at1280x720.
Original four accessories were wing amulet, Binding ring with one Fairy,
Shattered Stone ring and sash. All four were moved into empty ordinary inventory
slots11/12/13/17, to isolate the test amulet from other accessory behavior.
[900 original](../../build/qa-audit/node-pilot-playtest/900-protection-original-loadout.png)
and [901 empty physical slots](../../build/qa-audit/node-pilot-playtest/901-protection-all-unequipped.png)
were viewed.901's inventory tooltip obscures part of the inventory, not the
four visibly empty equipment slots. No claim about Shattered Stone's own damage
behavior is made from this precaution.

Commands seeded one of each protection amulet into verified empty ordinary
inventory slots25..30. Each was equipped into the actual amulet slot and removed
again through GUI clicks. Tests ran in SURVIVAL. Armor and toughness attribute
queries both returned0. Absorption was0, and no active effects existed initially
or at any asserted damage baseline. No armor stack was present.

`naturalRegeneration` was initially true, explicitly set false during testing,
then restored true. Before each synthetic hit, instant-health setup restored20;
after1.3s, an8-point generic hit established12; another1.3s allowed the hurt
cooldown to expire. Exact Health12, no active_effects, and AbsorptionAmount0
predicates passed before the4-point test hit. Instant Health was fixture reset,
not a claimed amulet heal. World ticks stayed frozen; players still processed
damage, timers and gravity. No tick-unfreeze/step command was used in this lane.

The `/damage @s 4 minecraft:<type>` commands invoke actual server damage hooks
but are **synthetic damage**, not naturally fired arrows, explosions, drowning,
falls or wither encounters. Canceled hits returned Target is invulnerable to the
given damage type. Those errors alone are NOT passes: actual before/after health,
unequipped controls, and physical-slot screenshots establish the result.

## Seven bounded cases

All health values below are points, not hearts. Every synthetic unequipped
control started12 and ended8. Control amulets existed only in ordinary inventory,
with all four physical accessory slots empty.

| Case | Actual equipped result | Evidence and verdict |
| --- | --- | --- |
| 1 Red fire family | IN_FIRE12->12, canceled without heal. LAVA12->16 and ON_FIRE12->16, canceled with4-point heal. All three unequipped types12->8. | PASS. Physical slot [903](../../build/qa-audit/node-pilot-playtest/903-red-physical-slot.png); health [904 IN_FIRE](../../build/qa-audit/node-pilot-playtest/904-red-in_fire-health12.png), [905 LAVA](../../build/qa-audit/node-pilot-playtest/905-red-lava-health16.png), [906 ON_FIRE](../../build/qa-audit/node-pilot-playtest/906-red-on_fire-health16.png). |
| 2 Orange explosion |12->16, canceled and healed4; unequipped12->8. | PASS. [907 slot](../../build/qa-audit/node-pilot-playtest/907-orange-physical-slot.png), [908 health](../../build/qa-audit/node-pilot-playtest/908-orange-explosion-health16.png). |
| 3 Yellow arrow |12->10, actual damage2 from requested4; unequipped12->8. | PASS bounded half-damage check without armor/effects. [909 slot](../../build/qa-audit/node-pilot-playtest/909-yellow-physical-slot.png), [910 health](../../build/qa-audit/node-pilot-playtest/910-yellow-arrow-health10.png). |
| 4 Green fall |12->16, canceled and healed4; unequipped12->8. | PASS synthetic check. [911 slot](../../build/qa-audit/node-pilot-playtest/911-green-physical-slot.png), [912 health](../../build/qa-audit/node-pilot-playtest/912-green-fall-health16.png). |
| 5 Blue drown |12->16, canceled and healed4; unequipped12->8. | PASS. [913 slot](../../build/qa-audit/node-pilot-playtest/913-blue-physical-slot.png), [914 health](../../build/qa-audit/node-pilot-playtest/914-blue-drown-health16.png). |
| 6 Violet wither |12->12, canceled without heal; unequipped12->8. | PASS. [915 slot](../../build/qa-audit/node-pilot-playtest/915-violet-physical-slot.png), [916 health](../../build/qa-audit/node-pilot-playtest/916-violet-wither-health12.png). |
| 7 Red actual short lava contact | Survival16->20 during real contact, natural regeneration disabled and no active effects. Fire300 during contact; after water Fire-20, health20. | PASS bounded environmental contact. [917 Red equipped](../../build/qa-audit/node-pilot-playtest/917-red-before-contact.png), [918 actual lava/fire overlay and full hearts](../../build/qa-audit/node-pilot-playtest/918-red-real-lava-contact.png). |

All listed slot and health screenshots were independently viewed. Yellow's
result matches the source's0.5 pre-mitigation multiplier in the zero-armor case;
this run does not independently distinguish ordering against armor mitigation,
since armor was deliberately absent. No damage amount beyond4 was used for
the protection measurement itself.

## Actual lava setup

Re-equipped Red through the physical amulet slot. Built a contained stone basin
center171,180,166, floor y179 and walls y180..181 around its one-block interior.
Teleported into the empty basin, reset health20, applied4 generic setup damage,
waited1.3s and asserted16 with no effects. Placed a real lava source at the
player's feet, waited200ms, and captured918 (capture helper adds300ms).
Readback then showed Health20 and Fire300. No synthetic lava damage command was
used during this environmental case. This proves actual contact, not natural
travel into an existing lava lake or survival acquisition of the amulet.

Replaced lava with water, waited700ms, confirmed Fire-20 and Health20, then
removed water and returned to the supported home platform. The now-dry stone
basin remains. An unequipped natural-contact repeat was not attempted; the
synthetic LAVA12->8 control above is the negative control, not environmental
footage. No video comparison or recorded-video equivalence is claimed.

## Fixture limitation

Cumulative damage knockback displaced the player off the narrow elevated
platform during the synthetic sequence. Green/Blue/Violet world screenshots
show the lower grass floor; subsequent telemetry confirmed173.6,-60,156.3,
health12, DeathTime0. No death/respawn was observed or commanded. Returned to
the original platform before the lava case and used a walled basin thereafter.

Per-hit health baselines and final predicates still passed, but no incidental
fall is counted as an extra natural Green acceptance. Its timing/damage was not
isolated. Future damage fixtures should have perimeter containment from the
start. This caveat is retained rather than representing every shot as taken on
the elevated platform.

## Restoration and handoff

All original items were recovered by physical-slot GUI transactions. Removed
exactly the six command-seeded test amulets afterward; each clear returned1.
Final ordinary inventory NBT payload exactly matched the initial21-entry payload,
including slot indices, counts and components. No original inventory loss.
[920 restored four slots](../../build/qa-audit/node-pilot-playtest/920-original-loadout-restored.png)
matches900; [921](../../build/qa-audit/node-pilot-playtest/921-binding-preserved.png)
confirms the original Binding ring still has1/15 Fairy. No direct loadout edit.

Final server checks: health20, Fire-20, DeathTime0, no active effects,
naturalRegeneration=true. Player returned171.5,180,160.5, yaw90/pitch0,
CREATIVE, food20; stone171,179,160 support predicate passed. World frozen,
control mode active, no menu, GUI3. [922 handoff](../../build/qa-audit/node-pilot-playtest/922-protection-handoff.png)
was viewed. Client left running for parent, no Save/Quit or Quit Game. Config
questline=false was not changed. No further bridge calls after returning UI.

## Audit trail and remaining limits

Ignored [protection-checks.ps1](../../build/qa-audit/node-pilot-playtest/protection-checks.ps1)
contains the finite six-family helper; actual lava actions and every command/
result are in [actions.jsonl](../../build/qa-audit/node-pilot-playtest/actions.jsonl).
[Seventh-client stdout](../../build/qa-audit/integrated-restoration-client-seventh/stdout.log)
contains timestamped QA family/control markers, exact health NBT and restoration.

UNVERIFIED: armored Yellow ordering, real projectile/explosion/drowning/wither
scenarios, independently measured natural falls, other damage types/magnitudes,
other mods, multiplayer, persistence across reload in this lane, and survival
crafting/acquisition. These seven bounded cases are not general accessory/parity
closure. Parent retains the remaining live progression work.
