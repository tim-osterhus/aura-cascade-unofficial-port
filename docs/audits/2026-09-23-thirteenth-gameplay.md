# Thirteenth Candidate Gameplay

This is an incremental execution record, not a completed release gate.
The named development client uses the source built by the thirteenth full
integration (227 passing tests). Its corresponding packaged candidate is
`306ab942b05029d814607a155e3a3796eaed3384b85af0cf92423a74c5ac63d4`;
packaged intermediary evidence is recorded separately. LAB8 is private QA only.

The existing disposable `core-systems-cac43cda` world was loaded normally.
The actual inventory baseline was 22 entries, including a tier-3 Angelsteel
ingot in slot 24; the earlier 21-entry baseline is not assumed unchanged.
Player health/food were 20, Creative mode, on the supported main platform.
Commands and before/after NBT are recorded in the private bridge action log
and `core-systems/logs/latest.log`. No non-QA world or CurseForge/account
credentials were accessed; the private local bridge uses its own test token.

## Corrected Recipes

The retired, already-tested seeded Spawner station at 160,180,160 was reused
after confirming no entities in its bounds. Its existing stone floors were
verified by the helper before teleporting. Player inventory was not modified.
The private `restored-recipes.ps1` executes each case with a 25-tick unpowered
control, then seeded near-complete progress and only the final step's adjacent
node power. Input/output item entities are real. These are exact recipe and
completion checks, not full-cycle progression or natural material acquisition.

- D1 PASS: all 16 wool colors plus one iron ingot yield exactly one correctly
  colored Arcane Ingot, with all inputs consumed and no unpowered output.
- D2 PASS: all 16 advanced wool-to-colorant mappings, including bone meal,
  lapis lazuli, cocoa beans and ink sacs, plus ordinary-processor rejection
  controls for all 16. Exact item counts and unpowered retention passed.
- D4 PASS: all 11 End Stone fortification routes, including all six legacy
  plank variants. Exact consumption/output and unpowered controls passed.
- D5 PASS: six supported colored Arcane Ingots are consumed and apply exactly
  the corresponding level-1 enchantment. Red crystal, White ingot and Black
  ingot are rejected without changing the target or consuming the catalyst.
  Each route also passed its unpowered control. Failed-chance distribution is
  not established by these first-enchantment positive cases.

Evidence: private `node-pilot-playtest/thirteenth-D1.log`, `thirteenth-D2.log`,
`thirteenth-D4.log`, `thirteenth-D5.log`, `restored-recipes-results.jsonl`
and `actions.jsonl`. All 68 selected cases completed successfully.
Each successful case removes only its run-tagged items and retains the test
shell/machines. World ticks remain frozen between bounded steps.

## Consumer Item Lifetime

D3 PASS in `thirteenth-D3.log`: at actual game time divisible by 500, both an
unpowered common consumer and an unpowered late-game consumer reset age and
set the persistent protection flag for ordinary items inside their exact
six-block-wide bounds. Items immediately outside were not protected and aged
normally. No node power or progress was supplied.

The protected item was then moved outside the refresh region. Both it and an
ordinary control had age explicitly advanced to 5,999; only the naturally
protected item retained its real protection flag. Three actual ticks later,
the ordinary item was gone and the protected item survived at full-width age
6,002. Only the run-tagged items and two fixture consumers were removed.
This accelerated age-boundary check complements the separate packaged
serialization/merge gate; it is not a real-time five-minute wait or world-reload
test of those exact entities.

## Full White Progression

PASS in `thirteenth-natural.log`: fresh automatic links and zero-progress
processor, eight actual White crystals absorbed for exactly 8,000 aura, and
one actual coal consumed by the pump. Unfueled controls retained both recipe
inputs and produced no work. The first five-block lift moved exactly 60 aura
with 319 fuel attempts remaining. Subsequent upper transfer and falling power
advanced the processor through its full counter to exactly one White Arcane
Ingot from one wool and one iron ingot. Total aura remained 8,000 throughout.

No aura, power, progress, fuel, links or output NBT was injected. Blocks and raw
materials were command-supplied; this is ordinary machine operation, not natural
survival acquisition or physical player crafting. The tagged output was kept
and the pump parked with real redstone; ticks remain frozen. Screenshot
`1002-natural-white-complete.png` records the fixture after completion.

## Full Prism And Angelsteel Progression

Both `thirteenth-Prism-retry.log` and `thirteenth-Synth.log` PASS. Each fresh
station consumed 100 actual Red crystals for 100,000 aura, absorbed real primed
TNT, lifted aura five blocks and generated power through actual falling aura.
No aura, power, links, progress, fuel or result NBT was seeded. Both machines
started at zero and completed their full counters; unpowered controls passed.

- Prismatic Processor: eight distinct Arcane Gems consumed for exactly one
  Arcane Prism, using four TNT bursts and 1,099 stepped ticks including charging.
- Synthesizer: exactly one tier-1 Angelsteel ingot, using ten TNT bursts and
  1,220 stepped ticks including charging. No material input is required.

The output entities remain uniquely tagged and upper nodes were parked by
redstone. Materials/blocks were command-supplied, including all eight gems;
these tests do not establish their upstream survival acquisition. Tick steps
use vanilla timing, not an increased tick rate. The first Prism invocation
stopped before fixture construction due to PowerShell array-expression
precedence; parentheses fixed the private helper and its successful retry used
a fresh tag. That failed setup is retained, not counted as a gameplay failure.

## Earned Miner Yield

PASS in `thirteenth-Miner.log`: 21 actual earned charges and one release cycle
consumed 110,000 power supplied in 22 batches to an adjacent node. Each cycle
observed real progress before completion; Miner/helper charge, progress, yield
and output were never seeded. The real helper remained contained in the
fortified cage. Release produced exactly one Nether Quartz Ore, reset the
machine to zero charge/progress/power and removed the helper. The recorded,
uniquely tagged ore was then removed; no other entities were cleaned up.
This establishes earned nonzero yield, not natural power generation or a
statistical audit of the weighted ore distribution.

## Session Closure

All 22 player inventory entries had exactly matching serialized NBT text before
and after this session. No equipment edits were made. All 27 chunks force-loaded
by these tests were released; retained fixtures and tagged progression outputs
were saved. Player remains Creative at 434.5,180,264.5 on verified stone support.
Save and Quit to Title, then Quit Game, completed normally with exit code 0.

The client/integrated-server plus monitor peaked at 1,367.1 MiB working set and
1,937.6 MiB private allocation. The 3,200 MiB guard did not trigger. The desktop
and agent hosts are excluded under the user's explicit workload-only budget;
one-second sampling is not a hard operating-system memory limit. The monitor
report is `build/qa-audit/thirteenth-gameplay/summary.json`.

## Packaged Artifact Inspection

The thirteenth JAR contains 1,441 entries and only the intended Energy API
nested JAR. A path scan found no QA/bridge, token, environment, session, world
or log paths. Fabric metadata declares Minecraft 1.21.1, Java 21, Fabric API,
Patchouli and embedded Energy; the original MIT notice and attribution are
present. This preliminary path/metadata inspection does not replace final
release scanning after any subsequent build.
