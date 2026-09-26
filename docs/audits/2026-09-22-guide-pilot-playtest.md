# Guide and recipe pilot: independent live playtest

Date: 2026-09-22. Scope: bounded Patchouli guide and two acquisition recipes,
not full original Encyclopedia coverage or overall Aura parity.

## Parent Follow-up

The independent observations below are preserved, including both title failures.
After the playtest, the parent changed the crafting heading to `Encyclopedia Aura`
and the overflowing entry name to `Enchanting`, with a source regression check.
The independent follow-up below now verifies both corrected headings at GUI
scales 2 and 3; acceptance is based on actual screenshots, not shorter strings
or passing unit tests. Preserved original failure evidence:
[recipe title](evidence/guide-pilot/37-start-scale3.png) and
[enchanting title](evidence/guide-pilot/22-enchant-scale3.png).

Parent subsequently saved/quit the fixture and exited the client normally.
The monitor recorded exit 0, no memory stop, peak working set 1277.3 MiB and
private allocation 1808.5 MiB for client plus monitor, excluding Codex desktop.
One-second sampling is not an OS-enforced cap or a synchronized peak covering
every short-lived input helper. The live-game slot was then handed back for
coordination with the separate PsiAI task.

## Runtime and method

- Parent handed over sole input ownership of its already-running Minecraft
  1.21.1 Fabric client, PID 12596. No Java, Gradle, or additional client was
  launched by this auditor. Parent owns build validation and memory monitoring;
  their results are not independently certified by this report.
- Authenticated loopback bridge supplied actual framebuffer screenshots,
  Minecraft screen mouse events, keyboard events, and normal held-item use.
  Credentials were read locally and were not printed or copied into this report.
- Confirmed loaded world path ends in
  `build/qa-audit/mod-lab-pilot/saves/guide-baseline-a6acb5e3`, the disposable
  fixture. Actual player information reported `SURVIVAL` for crafting.
- Screenshots were individually opened and visually inspected. Input/API
  acknowledgements and source resources alone did not establish passes.
  Some immediate captures preceded completed screen transitions; settled
  captures are cited below instead.
- Framebuffer: 1280x720. Video Settings visibly confirmed GUI scale 2 and then
  scale 3 (`14-video.png`, `15-scale3.png`); the scale change used the UI.
- Evidence originals and the local bridge helper are under ignored
  `build/qa-audit/guide-pilot-playtest/`. Screenshot names below are relative to
  that directory unless linked. Parent preserved eight selected PNGs under
  `docs/audits/evidence/guide-pilot/`; the relative links below target those
  canonical copies. Other named captures remain local-only evidence.

### Preserved evidence

- [Crystal recipe, scale 2](evidence/guide-pilot/08-crystal-scale2.png)
- [Crystal recipe, scale 3](evidence/guide-pilot/16-crystal-scale3.png)
- [First circuit final page, scale 3](evidence/guide-pilot/18-circuit-last-scale3.png)
- [Iron-center negative](evidence/guide-pilot/28-iron-negative.png)
- [Amethyst-center result before pickup](evidence/guide-pilot/29-amethyst-result.png)
- [Two crafted crystals and empty grid](evidence/guide-pilot/30-crafted-crystals.png)
- [Crafted Encyclopedia and consumed inputs](evidence/guide-pilot/34-crafted-encyclopedia.png)
- [Crafted book opens remembered page](evidence/guide-pilot/35-crafted-book-opens.png)

## Results

| Check | Result | Observed evidence |
| --- | --- | --- |
| Normal Aura item use opens Patchouli | PASS | Normal `use_item` on held `aura:encyclopedia_aura` opened visible landing pages; actual screen class was `GuiBookLanding`. `04-guide-delayed.png`. A newly survival-crafted Encyclopedia also opened the guide: `35-crafted-book-opens.png`. |
| Category and entry navigation | PASS, sampled paths | Landing Walkthrough link opened its category (`05-walkthrough.png`); clicking Getting Started opened the entry (`06-start-scale2.png`). The entry's Aura Systems link opened that category (`39-internal-category.png`). |
| Page turns, back, and internal links | PASS, sampled paths | Getting Started next page (`07-start-links-scale2.png`), its White Aura Crystal link (`08-crystal-scale2.png`), crystal-to-first-circuit link (`09-circuit-scale2.png`), and next circuit page (`10-circuit-last-scale2.png`) worked. Back returned to the crystal entry (`11-back.png`). Scale-3 links also reached Pumps, Consumers, Enchantments, and Enchant Matrix (`19` through `24` PNGs). |
| Crystal recipe presentation | PASS, scales 2 and 3 | Visible 3x3 ring of eight gold nuggets, center amethyst shard, white crystal output stack marked 2, and matching readable prose. `08-crystal-scale2.png`, `16-crystal-scale3.png`. |
| Pilot text readability | PARTIAL / FAIL for title layout | Crystal and all three first-circuit pages were readable at both scales. Getting Started body, recipe, and Choose a Path text were readable at both scales, but its recipe title exceeded its page width. Scale-3 Kaleidoscopic title was clipped. Details below. |
| Survival crystal craft and consumption | PASS | Actual table clicks formed the recipe and collected two crystals. All nine ingredient slots emptied. No crystals were command-injected into the crafting test. `29-amethyst-result.png`, `30-crafted-crystals.png`; inventory evidence below. |
| Iron-center negative | PASS | Eight gold nuggets surrounding one iron ingot produced an empty result slot. Clicking that slot did not produce an item. Replacing only iron with amethyst subsequently yielded two crystals. `28-iron-negative.png`, `29-amethyst-result.png`. |
| Encyclopedia craft without Prism | PASS | In the survival player's 2x2 grid, one of the crafted crystals left of one ordinary book yielded one Encyclopedia. Taking it consumed both inputs, leaving the other crystal and untouched iron ingot. No Prism was present. `33-encyclopedia-result.png`, `34-crafted-encyclopedia.png`. |

## Observed layout failures

1. **Encyclopedia recipe title overruns the right-page text area at scales 2
   and 3.** `Assemble the Encyclopedia` starts at/across the center fold and
   reaches the outer decorative border instead of fitting within the page.
   The recipe and body remain readable; this is a title-fit failure, not a
   recipe or navigation failure. Evidence: `06-start-scale2.png` and
   `37-start-scale3.png`.
2. **Kaleidoscopic entry title is clipped at scale 3.**
   `Kaleidoscopic Enchantments` extends beyond the left text/page boundary and
   crosses the center fold. Its initial character is visibly clipped; body
   text is readable. Evidence: `22-enchant-scale3.png`. This entry's scale-2
   rendering was not sampled, so the failure is not asserted at that scale.

No source fixes or runtime resource reloads were performed by this auditor.

## Crafting provenance and accounting

Setup commands cleared the previous four guide/control items, switched the
player to survival, set daytime, positioned the player on the fixture platform,
and placed a crafting table at 13,181,14. Commands then provided exactly:

- 8 `minecraft:gold_nugget`
- 1 `minecraft:amethyst_shard`
- 1 `minecraft:iron_ingot`
- 1 `minecraft:book`

`26-before-crafting.png` and `27-crafting-grid.png` show the starting inventory.
These are injected test materials, not evidence of survival acquisition.
All following ingredient placement, substitution, output pickup, and inventory
placement used real GUI clicks. No command supplied either recipe's output.

After taking the crystal output, the crafting grid was visibly empty. A
read-only `data get entity @s Inventory` produced the following fresh server-log
inventory contents at 21:45:10 (client log timestamp):

```text
Slot 0: aura:aura_crystal_white x2
Slot 2: minecraft:iron_ingot x1
Slot 3: minecraft:book x1
```

After taking the Encyclopedia from the player's 2x2 grid, both occupied input
slots and the result slot were visibly empty. The same read-only query at
21:46:22 yielded:

```text
Slot 0: aura:aura_crystal_white x1
Slot 2: minecraft:iron_ingot x1
Slot 3: aura:encyclopedia_aura x1
```

The two observations are in
`build/qa-audit/guide-pilot-client/stdout.log`; these normalized excerpts omit
local player identity. Screenshots independently show the inventory changes.
Finally, normal use of the crafted Encyclopedia opened its remembered Patchouli
page (`35-crafted-book-opens.png`), linking crafting to functional use.

## Readability coverage and limits

- Visually read at both scales: Getting Started's three pages, White Aura
  Crystal's two pages, and First Aura Circuit's three pages. Their body text
  fits; the Encyclopedia crafting title exception is recorded above.
- Additionally read at scale 3: Pumps and Control, Consumers and Vortex
  including its third page, Kaleidoscopic Enchantments, and all four Enchant
  Matrix pages. Their body text fits in the sampled screenshots; the
  Kaleidoscopic title exception is recorded above.
- Unverified: every remaining entry/page at both scales, every individual
  internal link, other screen sizes, alternate recipe placements/mirroring,
  ingredient gathering, and the gameplay mechanics described by the guide.
- The supplied pilot contains 12 migrated current-fact entries in six Aura
  categories. This test does not establish complete original Encyclopedia
  coverage, original-mod parity, or a complete progression playthrough.
- No mod error was observed during these interactions, but an exhaustive log
  audit, memory certification, and build verification were outside this
  independent input session.

## Handoff

Client left running in the disposable world, survival mode, GUI scale 3, on
the guide's Aura Systems category. Final inventory is the second inventory
listing above. Control mode was explicitly exited and returned false.
Sole runtime-input ownership returns to the parent; no shutdown was requested
or performed. No commits and no source changes were made.

## 2026-09-22 follow-up: corrected headings

Parent handed over the updated, already-running client (reported PID 29432)
for a two-page visual recheck only. The earlier failures above are preserved
as historical findings; both are resolved in this recheck at 1280x720.
Parent's reported 90-test build result is not substituted for visual evidence.

Normal use of the existing book in hotbar slot 4 opened Patchouli. Actual
category and entry clicks reached Getting Started and Enchanting. The bridge
confirmed the same disposable world path on both world loads:
`build/qa-audit/mod-lab-pilot/saves/guide-baseline-a6acb5e3`.
Video Settings visibly confirmed scale 3 (`54-recheck-video-scale3.png`) and
scale 2 (`58-recheck-video-scale2-confirmed.png`). No Java/Gradle launch,
source edits, ingredient injection, crafting, or mechanics retests occurred.

| Corrected heading | GUI scale | Result | Inspected evidence |
| --- | --- | --- | --- |
| Encyclopedia Aura, Getting Started recipe page | 3 | PASS: fully readable inside the right page, clear of fold and outer border | [Scale-3 Encyclopedia heading](evidence/guide-pilot/48-heading-encyclopedia-scale3.png) |
| Enchanting, first entry page | 3 | PASS: fully readable inside the left page, no clipped initial character or fold overlap | [Scale-3 Enchanting heading](evidence/guide-pilot/50-heading-enchanting-scale3.png) |
| Encyclopedia Aura, Getting Started recipe page | 2 | PASS: fully readable inside the right page, no title overrun | [Scale-2 Encyclopedia heading](evidence/guide-pilot/65-heading-encyclopedia-scale2.png) |
| Enchanting, first entry page | 2 | PASS: fully readable inside the left page, no clipping or fold overlap | [Scale-2 Enchanting heading](evidence/guide-pilot/63-heading-enchanting-scale2.png) |

All four links target parent-reviewed and preserved canonical PNGs. Originals
remain under `build/qa-audit/guide-pilot-playtest/`.
Do not use `61-heading-enchanting-scale2.png` as heading evidence: despite
its filename it shows the landing categories. Likewise, `55` still shows
scale 3 after an ineffective right-click; `58` is the scale-2 confirmation.

During the settings transition, an auditor misclick selected Save and Quit
to Title instead of Options. The client process stayed running; the same
fixture was reopened and its real path reconfirmed. This was an input error,
not a mod failure. GUI scale was changed through the title-screen Options UI.

Follow-up scope is closed with four visual passes and no remaining observed
failure on these two heading pages at scales 2 and 3. This does not expand
the earlier coverage or certify other pages, resolutions, or gameplay.

Final handoff: control mode explicitly exited (`false`). Client left running
in the disposable world on Getting Started's first spread, showing the
Encyclopedia Aura crafting page, GUI scale 2. Runtime input ownership is
returned to the parent for shutdown. No further client inputs will be sent
without another handoff.

Parent closeout: the fixture was saved, the integrated server stopped, and the
client exited normally (monitor exit 0). Recheck client plus monitor peaked at
1321.9 MiB working set and 1817.4 MiB private allocation with no memory stop.
The same sampling/scope caveats apply. The shared playtest slot is released.
