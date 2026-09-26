# Accessory and storage pilot: independent runtime QA

Date: 2026-09-23. Parent-supplied client PID28352/session81146,119-test validated
snapshot. Newer working-tree edits were not compiled during this pass. No build,
new client, source edit, or forced menu opening was performed by the auditor.

## Outcome

**BLOCKING FAIL: ordinary empty-hand coordinator use does not open the browser.**
Twice it displayed "No room for this item" with the selected slot empty, bare
hand visible, and no screen. `get_screen_buttons` returned `no screen`.
Consequently browser search, scrolling,128-entry rendering, selected retrieval,
and UI retrieval power gating remain **UNVERIFIED/BLOCKED**, not passes.

Physical accessory slots, sampled slot policy, equip/unequip, and equipped-wing
movement passed. Ordinary coordinator deposit and zero-power deposit rejection
also passed. Save and Quit to Title completed normally; reload persistence was
not tested. Runtime was returned to parent at Title, control mode off, without
Quit Game. Screenshot418 independently confirms Title at1280x720; UI dimensions
match scale2, but Video Settings was not separately opened in this pass.

## Runtime and fixture provenance

Bridge confirmed world path
`build\qa-audit\core-systems\saves\core-systems-cac43cda`.
Actual player Player918 was SURVIVAL. Parent provided exclusive UI ownership;
auditor issued `tick unfreeze` before tests. Processor at106,180,110 and its
power loop were left intact.

All captures400..418, sanitized action records, and `storage-fixture.ps1` are
under ignored `build/qa-audit/node-pilot-playtest/`. Server NBT evidence is in
`build/qa-audit/core-systems-client/stdout.log`, approximately00:30..00:37 local.
No token is included here. Screenshot names alone are not results.

Commands seeded inventory materials, fixture blocks, book contents, and power.
They did not equip accessories, open menus directly, produce retrieval outputs,
or substitute for player use. Ordinary input used actual B/V keys, mouse menu
transactions, and `use_item`.

## Accessories

| Check | Result and capture |
| --- | --- |
| B opens physical inventory | PASS: four distinct slots plus ordinary inventory/hotbar;400. |
| Matching slot types | PASS sampled: wing amulet first, Binding and Shattered Stone rings second/third, sash fourth;403. |
| Wrong-slot rejection | PASS: amulet remained on cursor when clicked onto ring slot;402. Ordinary-item rejection and every wrong pairing UNVERIFIED. |
| Mouse equip | PASS: four seeded items moved from hotbar to separate slots, not duplicated;401 versus403. |
| Both rings full | PASS: ordinary use of third Binding ring retained it in hotbar and left both equipped rings unchanged;404/405. No swap. Single amulet/belt full cases UNVERIFIED. |
| Equipped effect | PASS: V initially reported No clear space found;406. After command-placing landing stone122,183,100, actual V moved player from122.5,180,100.5 to122.5,184,100.5 and displayed Ascended;407 and player telemetry. No teleport command performed that ascent. |
| Mouse unequip | PASS: wing moved to hotbar, first physical slot empty;410. |
| Inventory is not equipped | PASS: after unequip and resetting player to the lower position, V left player at122.5,180,100.5 with the same landing available;411. |
| Ordinary use re-equip | PASS: selected wing, aimed away from blocks, ordinary use moved it back into amulet slot and removed it from hotbar;416. |
| Shift-click, death, dimensions, bindings | UNVERIFIED. Not substituted with direct attachment edits. |

Captures408/409 are **excluded as unequip/negative evidence**: the first click
arrived before the asynchronously opened menu existed, returning `no screen`.
408 still shows the wing equipped. Retest410/411 used a settled menu and actual
slot changes. This was an input-timing issue, not an accessory failure.

## Storage fixture and failure

- Coordinator124,180,116; shelves123,180,116 and125,180,116; power node124,180,117.
- Very Light book seeded with126 component-distinct entries: alternating dirt
  and stone named QA001 through QA126, one each.
- Dense book seeded with128 plain cobblestone and32 separately named Control
  Cobble. Thus **128 distinct item/component identities initially**, including
  an over-stack-size count and a same-item/different-component control.
- Power node explicitly seeded1000 Stored Power. This tests consumption/gating,
  not power generation. Parent's real-power processor test is independent.
- Player124.5,180,113.5, yaw0/pitch20, aimed at coordinator; selected hotbar2
  empty. NBT inventory read showed only wing, spare ring and stone, no offhand
  or selected-slot item. Two ordinary uses yielded the failure above;412/413.

Current source `BookshelfCoordinatorBlock.useItemOn` has no early empty-stack
fallback and returns sided success after the deposit branch. This is a plausible
explanation for swallowing `useWithoutItem`, not proof of the compiled cause or
a verified fix. Parent was notified promptly; auditor made no source edits.

## Ordinary deposit control

Seeded64 plain cobblestone into selected slot, then used coordinator normally.
Screenshot414 shows Stored64 items and empty hand. Server state changed:

| State | Plain cobble in light book | Plain cobble in dense book | Named Control Cobble | Power |
| --- | ---: | ---: | ---: | ---: |
| Before ordinary deposit | 0 | 128 | 32 | 1000 |
| After ordinary deposit | 16 | 176 | 32 | 989 |

All64 are accounted for; plain total192, named component control unchanged32.
Power cost11 for two connected shelves matches the source-prepared expectation.
Light book now has127 local entries; browser aggregation across books should
still yield128 identities, but that rendering remains unverified.

Then power was explicitly set0 and8 new plain cobblestone supplied. Ordinary
use displayed No power; screenshot415 retains the eight items. Inventory NBT
confirms8, dense book remains176/32, and power remains0. This is **deposit**
power-gating evidence only, not browser retrieval evidence.

## Handoff and remaining work

Four accessories were re-equipped before normal save. Spare Binding ring, stone,
and eight rejected cobblestone remain in inventory. Storage books retain the
fixture above and node power is0. Landing stone remains at122,183,100.
Player last positioned122.5,180,100.5. These are pre-save observations, not a
reload persistence pass.

Pause screen417 and Title418 were visually inspected. Parent owns subsequent
client activity. Required next checks after a compiled opening correction:
normal browser entry,128 mixed-component identities, search/no-results/clear,
scrolling, exact-component retrieval with counts/power conservation, and saved
equipment/storage reload. All remain explicitly UNVERIFIED here.

## Integrated-fifth follow-up, 2026-09-23

The preceding 119-test baseline is preserved verbatim. This follow-up used the
parent-launched PID34480, Player918, same isolated world, survival mode, at
1280x720. Parent identified the compiled snapshot as 183 tests/51 suites and JAR
SHA256 `6833f7759ad2c64f7c74c82715a06156e5dfb39459424362fd950933e49b49e6`.
These build results are parent-provided, not independently rerun here. No build,
source edit, new client, direct equipment attachment edit, or advancement grant
was performed by this auditor.

Evidence: ignored [capture/action directory](../../build/qa-audit/node-pilot-playtest/),
captures600..665 and `actions.jsonl`; server/NBT output in
[runtime stdout](../../build/qa-audit/integrated-restoration-client-retry/stdout.log),
approximately02:26..02:56 local. Screens were actually viewed; filenames and
input acknowledgements alone are not acceptance. Power/material commands are
fixture setup, not crafting or power-generation evidence.

### Storage results

| Check | Result |
| --- | --- |
| Ordinary empty-hand entry | PASS, resolving the earlier opening failure on this binary: actual Bookshelf Coordinator screen602/603. |
| Zero-power retrieval | PASS: selected plain192, Power0/11, disabled Retrieve; attempted click left inventory8, stored192 and power0 unchanged. [603](../../build/qa-audit/node-pilot-playtest/603-zero-selected.png). |
| Scrolling a128-identity fixture | PASS sampled navigation: track click reached last row, QA125 and QA126 separately selectable, final row two entries. [609](../../build/qa-audit/node-pilot-playtest/609-final-entry126.png) shows QA126, Stored1. Did not independently inspect every identity or test mouse wheel. |
| Search/no-results/clear | UNVERIFIED, bridge input blocker: focused field remained blank after `type_text` and `paste_text`, despite acknowledgements;604/605/606/614. No search correctness claim. |
| Over-stack-size retrieval | PASS: actual selection/Max/minus controls requested128; Retrieve moved128 plain cobble into inventory and charged11. [611](../../build/qa-audit/node-pilot-playtest/611-retrieved128.png). |
| Same item, different components | PASS: selected named Control Cobble, requested7, retrieved7 with custom_name intact; plain storage unchanged. [613](../../build/qa-audit/node-pilot-playtest/613-named-retrieved7.png). |
| Scale2/3 browser visual acceptance | FAIL on current binary: missing inventory slot outlines, obscured/overflowing grid counts, full-height scrollbar thumb despite scrollable content. Actual scale3 browser is [624](../../build/qa-audit/node-pilot-playtest/624-browser-scale3-actual.png), not618. |
| Inventory tooltips | Parent independently reported missing rendering and a source-only correction; auditor did not separately establish this hover failure. Runtime correction UNVERIFIED. |
| Storage/power reload | PASS: ordinary save/reload retained plain128, named25 and power967; [662](../../build/qa-audit/node-pilot-playtest/662-storage-reloaded128.png) visibly shows plain128/power967; post-reload block NBT confirms dense112/25. |

Exact ordinary-action accounting (power1000 was explicitly seeded after the
zero-power negative; named and plain cobble are distinct components):

| Point | Stored plain | Stored named | Inventory plain | Inventory named | Power |
| --- | ---: | ---: | ---: | ---: | ---: |
| Before retrieval | 192 | 32 | 8 | 0 | 1000 |
| After actual128 retrieval | 64 | 32 | 136 | 0 | 989 |
| After actual7 named retrieval | 64 | 25 | 136 | 7 | 978 |
| After additional ordinary64 deposit | 128 | 25 | 72 | 7 | 967 |

The additional64 deposit happened during a GUI-transition attempt when held-item
use reached the world. It is recorded, not silently omitted from conservation:
light book plain16, dense book plain112. Original126 named stone/dirt entries
remain. No machine output was injected as retrieval evidence.

Parent fixed inventory outlines, filtered-count scrollbar calculation, count
overlay placement/Z and inventory tooltips in SOURCE ONLY during this run.
Those corrections require a recompiled runtime retry; logic passes above do not
override visual failure. Parent also added an alive/spectator extraction guard
in source only. Required future negative: open menu, become dead/spectator, then
attempt extraction. This was deliberately NOT exploited or credited here.

### Quest rewards and guide pages

PASS for the bounded14-goal default-enabled contract. Inventory goals were
command-seeded; rewards came from actual normal Encyclopedia use, not `/give`
outputs counted as rewards or granted advancements. Initial late-goal control
used a tier2 Angelsteel ingot and the existing spare Binding ring while Crystals
was incomplete. First book opening granted only Angelsteel/Fairies, retaining
the goals. This establishes sampled order independence. A repeat opening did
not repeat these rewards. [627](../../build/qa-audit/node-pilot-playtest/627-quest-incomplete-scale3.png)
actually shows the still-Incomplete Crystals page.

After seeding the other12 goals, actual book opening completed all14. Live
advancement checks, reward messages and before/after inventory NBT agreed:

| Reward group | Actual added quantity |
| --- | ---: |
| Crystals/Furnace/Synthesizer, white crystals | 64 each,192 total;193 including seeded goal |
| Nodes | 16;17 including goal |
| Pumps, sticks | 64 |
| Red Crystals, TNT | 4 |
| Processor, coal | 8 |
| Dye, white wool | 32 |
| Arcane Ingot, white ingots | 15;16 including goal |
| Vortex Infusion, pedestals | 4 |
| Arcane Gem, white gems | 8;9 including goal |
| Arcane Prism | 1;2 including goal |
| Angelsteel, tier1 ingots | 15; tier2 goal retained |
| Fairies | 1 Fairy Charm with custom_data fairyRole="fairy"; ring retained |

Repeated opening before death retained the exact29-entry inventory and produced
no additional reward totals. Frozen `/give` animation item entities were allowed
five ticks to expire; they were not counted as duplicate rewards.

PASS actual navigation/readability for all14 quest pages at both GUI scales:
scale3 first spread628 then630..636, scale2 pages642..649. These include Complete
status, goal and reward text. Scale confirmation638/640 was viewed. Sampled
Fairies -> Accessory Loadout internal link637 and history-back642 worked.
Representative actual captures:
[scale3 final quest](../../build/qa-audit/node-pilot-playtest/636-quest-spread3.png),
[scale2 first spread](../../build/qa-audit/node-pilot-playtest/649-quest-spread2.png).
This does not claim every guide entry/link or every icon is correct: compiled
Patchouli startup still warns about invalid sword/charm IDs; parent assigned
source corrections, not runtime-verified here.

PASS saved one-time progress: normal save/reload retained all14 `done:true`
advancements plus root in the player's saved advancement JSON. Post-reload
normal book opening left the inventory byte-for-byte identical in the logged
NBT payload to pre-save and immediately post-reload. Capture661 shows the
LANDING CATEGORIES page, despite its misleading `quest-reloaded-complete` name;
it is NOT visual evidence of a post-reload Complete page. Saved advancement
state plus unchanged actual inventory supports persistence/no-repeat; pre-save
screens establish visible Complete pages. Config=false needs restart and is
UNVERIFIED/deferred to parent.

### Equipment, death and reload

PASS four readable physical labels at GUI2/3: Amulet, Ring, Ring, Belt; actual
wing, Binding ring, Shattered Stone ring and sash displayed in their slots.
[623](../../build/qa-audit/node-pilot-playtest/623-accessories-scale3-actual.png)
is the settled scale3 screen. Captures617/618 actually show Options and are
excluded from accessory/browser acceptance.

| Check | Result |
| --- | --- |
| keepInventory=true death | PASS: actual death/Respawn retained all4 equipped items and the29 inventory stacks, zero local item drops;650/651. |
| keepInventory=false death | PASS bounded immediate drop/clear:33 local item entities before stepping/pickup, exactly29 inventory stacks plus4 equipment stacks. One equipped Binding ring plus one spare inventory ring were expected. Respawn B screen [652](../../build/qa-audit/node-pilot-playtest/652-keepfalse-cleared.png) shows all equipment/inventory empty. |
| Recovery/equip | Existing local death drops were picked up; existing nearby entities were command-moved to the player for remaining recovery, not respawned via `/give`. Actual mouse transactions restored four equipment slots;656. |
| Equipment reload | PASS: normal save/reload then B showed all4 restored; [660](../../build/qa-audit/node-pilot-playtest/660-reloaded-equipment.png). No direct attachment editing. |

Important recovery limitation:60 stepped ticks during loose-item pickup changed
some quest materials, including Angelsteel tiers, and some materials were no
longer in the recovered inventory. Cause was not isolated. This is NOT evidence
of an accessory/quest duplication, and no full post-death material-conservation
pass is claimed. The exact21-entry recovered/re-equipped inventory became the
new baseline for persistence. Its pre-save, post-reload and post-book-reopen
NBT payloads matched exactly. No new reward grant was inferred from altered
loose-item totals. Effects/full-slot/wrong-slot results above remain the earlier
119-test runtime results; they were not all repeated on this snapshot.

### Exact final state and shutdown

Saved fixture: coordinator124,180,116, shelves123/125,180,116, node124,180,117.
Plain128 = light16+dense112, named Control Cobble25, original126 other named
identities retained; power967. All14 quests complete. Four accessories equipped:
wing amulet, Binding ring, Shattered Stone ring, sash. `keepInventory=false`
restored to its original setting. Explicit safe respawn point120,180,98 remains
from the death control (no prior custom spawnpoint was found). Player saved at
124.5,180,113.5, yaw0/pitch20, selected hotbar9 empty, GUI scale2.

Final inventory (ordinary inventory indices, zero-based; omitted slots empty):
0 Encyclopedia1;1 named Control Cobble7;2 pump1;3 red crystal1;4 colorer1;
5 white crystals64;6 plain cobble64;7 tier2 ingots3;9 prism2;10 white crystals64;
14 nodes17;15 white wool32;16 coal8;17 typed Fairy Charm1;18 white crystal1;
19 pedestals4;20 synthesizer1;21 plain cobble8;22 TNT4;23 spare Binding ring1;
24 tier3 ingot1. Last white64 was moved from hotbar8 to inventory10 to leave
the selected hand empty for the final browser check.

Processor/vortex fixtures were not edited or cleaned. Early unfreezing, brief
reload startup and five/60 tick steps allowed remote simulation; unobserved
remote outputs/despawns are explicitly NOT completion evidence. Ticks were
frozen again before final save; no assertion that tick-freeze persists across
a future reload.

Normal Save and Quit reached independently viewed [Title664](../../build/qa-audit/node-pilot-playtest/664-final-title-scale2.png)
and logs confirmed all dimensions saved. Initially returned at Title with
control mode off. Parent then requested releasing the heavy slot: re-enabled
control solely for actual Quit Game. First click outside button returned false;
verified Title665, corrected click on Quit Game. Log ended `Stopping!` at02:55:57;
PID34480 was absent afterward. No process kill or client restart. Parent owns
monitor18886 and subsequent runtime/build work.

Outstanding: search input/no-results/clear; fixed browser visuals/tooltips;
dead/spectator extraction rejection on the next compiled build; questline=false;
multiplayer owner-only attachment synchronization; untested accessory pairings,
shift-click/dimensions and broader effects. These remain UNVERIFIED, not implied
passes from source or unit-test counts.

## Integrated-seventh follow-up, 2026-09-23

Parent supplied the fresh198-test build and a passing packaged dedicated-server
run; those build/server results were not rerun by this auditor. Independently
checked `integrated-restoration-client-seventh/pid.txt` =29072, process start
03:21:30 and listener127.0.0.1:9876 owned by29072. Fresh startup log identifies
Player918, Aura initialization, Minecraft1.21.1 and47 preloaded Patchouli JSONs.
Title700 was viewed before entering the existing core-systems world. Bridge
world path and survival mode matched the saved fixture. No source edits,
builds, native-input workaround or additional client launch.

Evidence700..728 and sanitized actions remain in the ignored capture directory
linked above; current [server log](../../build/qa-audit/integrated-restoration-client-seventh/stdout.log)
covers approximately03:21..03:41. Bridge is still lab6; the parent's prospective
lab7 character-input correction was NOT active in this run.

### Browser correction and negative

| Check | Independent result |
| --- | --- |
| Inventory slot outlines at2/3 | PASS: all36 slots visibly framed; [702](../../build/qa-audit/node-pilot-playtest/702-browser2.png) and [717](../../build/qa-audit/node-pilot-playtest/717-browser3.png). Resolves this earlier visual failure on the new binary. |
| Counts128/25 at2/3 | PASS: legible within respective grid cells, not hidden behind icons; same captures. Very long counts beyond these fixtures remain UNVERIFIED. |
| Scrollbar thumb/128-identity fixture | PASS sampled: short thumb at top; moves down, with final two entries reachable. Scale2 [707](../../build/qa-audit/node-pilot-playtest/707-scroll-end2.png), scale3 [719](../../build/qa-audit/node-pilot-playtest/719-thumb-bottom3.png). Not an all128-item inspection or mouse-wheel test. |
| Actual inventory tooltips at2/3 | PASS: Control Cobble appears on hover in [706](../../build/qa-audit/node-pilot-playtest/706-search-comma2.png) and719. Capture718 was too early for the tooltip;719 is the settled evidence. |
| Search/no-results/clear | UNVERIFIED, tool gap. Paste/hotkey attempts left the focused field blank. No mod search failure inferred; parent identified a likely bridge reflection/interface-charTyped gap and prepared a future fallback. No repeated native-input workaround attempted. |
| Stale spectator extraction | PASS bounded negative: ordinary survival-opened menu remained visible after server `gamemode spectator`; selected plain128/amount1 and clicked enabled Retrieve. [708](../../build/qa-audit/node-pilot-playtest/708-spectator-stale2.png). Before/after player inventory payloads were exactly equal; dense112/25 and power967 unchanged. Restored survival afterward. Dead-player variant remains UNVERIFIED. |

These passes resolve the specifically tested browser rendering failures, not
the still-blocked search acceptance or arbitrarily long count formatting.

### Disabled questline restart control

PASS bounded `questline=false` negative on the freshly launched process. Read
`build/qa-audit/core-systems/config/aura.properties` before joining; it contained
false, set by parent while the previous client was closed. Confirmed Crystals
was already complete, revoked ONLY `aura:quest/crystals` through the actual
server dispatcher, and snapshotted inventory and nearby dropped-item entities.
White crystals were present (64+64+1).

Important excluded attempt:710 shows crosshair on coordinator, empty hand and
Stored1 items. Ordinary block use deposited the Encyclopedia rather than opening
it. This is NOT disabled-quest evidence or a mod defect. Retrieved that same
book using the real browser; deposit/retrieval cost11 each, power967->956->945.
The test was then repeated away from the coordinator, facing open air.

[712](../../build/qa-audit/node-pilot-playtest/712-disabled-book-actual.png)
actually shows the Patchouli book after normal held-item use. Live server check
reported Crystals still incomplete. Inventory NBT before/after this actual
opening matched exactly (21 entries); nearby item query returned zero both
times. Thus no Crystals advancement or reward was granted. Restored only the
previously completed Crystals advancement explicitly as QA cleanup; no root
revocation or grant and no fabricated reward pass. The excluded and corrected
attempts each had their own revoke/cleanup; prior all14 acceptance is preserved.
Parent retains responsibility for restoring config=true after client shutdown.

### Models and fairy binding

PASS sampled six colored first-degree sword inventory models. Command-seeded
six `aura:angelsteel_sword_1` stacks with custom_data aura red/yellow/green/blue/
violet/orange. [721](../../build/qa-audit/node-pilot-playtest/721-swords-visible3.png)
shows all six without tooltip obstruction: cyan blades with distinct colored
hilts.720's tooltip hides several swords and is not the clear-model proof.
Only icon/model display is accepted, not sword combat/curse behavior. Removed
the six seeded swords afterward; server clear count was6. No existing swords
were present before setup.

PASS warning-absence check: fresh startup and subsequent world-load log contained
no fairy-light/Aura missing-model warning and no invalid Patchouli icon warning.
This is a log check, not an actual placed fairy-light rendering/behavior test.

PASS ordinary default-role charm binding: moved the existing typed Fairy Charm
from inventory to the selected empty hotbar slot through B/mouse interactions,
then used it facing open air.713 shows Bound Fairy1/15 and consumed charm.
Actual equipped ring tooltip [715](../../build/qa-audit/node-pilot-playtest/715-bound-tooltip.png)
shows Bound Fairies1/15, Fairy. Server entity query after five stepped ticks
returned one `aura:fairy`, Role="fairy", Slot0 and Player918's owner UUID. No
direct ring-NBT, entity summon or attachment edits were used for binding.

PASS bind persistence: normal save/title/rejoin, then ring tooltip
[725](../../build/qa-audit/node-pilot-playtest/725-fairy-reload-tooltip3.png)
still shows1/15 Fairy. One owned role-fairy/slot0 entity was present after reload,
with a new entity UUID consistent with reconciliation; this is retained binding
and respawn, not a claim that the exact entity UUID persists.

Release UNVERIFIED: unequipped bound ring into hand, attempted Shift+ordinary
use using bridge hold_seconds. No charm was released; ring was instead normally
re-equipped and remained bound. Actual server sneaking state was not established,
so this is not a confirmed release defect. No repeated workaround or cleanup by
direct NBT. Role-specific actions, all17 roles,15-capacity boundary, full inventory
release and sprite animation/render inspection remain UNVERIFIED in this pass.

### Seventh-client handoff

Normal Save and Quit completed03:41:34; log confirms all dimensions saved.
[728](../../build/qa-audit/node-pilot-playtest/728-seventh-final-title3.png)
was independently viewed: Title, GUI3,1280x720. Control mode off, client left
RUNNING for parent, no Quit Game. Exclusive UI ownership explicitly returned;
no further bridge calls after handoff.

Exact changes from the previous saved state: player128.5,180,100.5, rotation
-180/0, survival, health20/food20, hotbar9 selected empty. Four original
accessories equipped; Binding ring now contains one default Fairy. Inventory
matches the prior final21-entry inventory except the typed Fairy Charm formerly
in slot17 was consumed by binding, leaving20 entries. All six temporary swords
removed. Book restored to hotbar0. All14 quest completions restored/preserved;
config remains false. Storage plain128 (light16+dense112), named25 and126 other
named identities unchanged; power945 from the additional book deposit/withdraw.
keepInventory=false and custom safe spawn120,180,98 unchanged.

Ticks were frozen before final save; short startup/reload intervals and5+5+3
stepped ticks allowed simulation. Remote processor/vortex fixtures were not
edited; their unobserved outputs/despawns still are NOT independent acceptance.

### Clarification of earlier death recovery

Parent supplied the exact prior Angelsteel comparison:15 tier1 +1 tier2 before
death equals18 tier1-equivalents;3 tier2 +1 tier3 recovered also equals18.
Independent source read of `AuraItems.tickAngelsteelIngots` confirms the
four-tick cadence and conversion of three nearby same-degree ground ingots into
one next-degree ingot. Thus the Angelsteel family is conserved and the observed
tier changes are consistent with expected conversion, not unexplained loss.
This clarification supersedes that part of the earlier recovery limitation;
other missing quest materials remain unexplained and were not retested.
