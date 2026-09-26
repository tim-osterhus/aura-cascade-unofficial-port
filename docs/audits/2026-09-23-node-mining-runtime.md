# Node Controls And Player Mining

Parent runtime audit on the compiled tenth snapshot, version 0.2.0+1.21.1.
The snapshot compiled and passed 211 of 212 tests; the remaining failure was a
guide wording assertion. It is not a release candidate sign-off. The later D1-D6
source corrections and custom-tool enchantability repair are NOT in this run.

Client PID 4956, `integrated-restoration-client-tenth-auth`, disposable
`core-systems-cac43cda`, Player918. Private lab7 bridge SHA-256:
`148a8257855086f0ab558d11ee6c44addc1ab2d46c9ca9c7d54907726477cb96`.
Its authenticated loopback listener was verified against that PID. An earlier
launch without the bridge environment token was terminated at the title screen,
before opening any world; it is not a successful runtime run.

## Node Controls

`node-controls.ps1`, its result JSONL, actions JSONL and actual server predicates
record the following bounded passes. Initial mixed storage and Red Aura were
explicitly seeded; successful transfer/refill/power output was not.

- White crystal: isolated empty node stayed empty before phase 10, then absorbed
  exactly one of two crystals for 1,000 White Aura and zero stored power.
- Orange and Black Manipulators: unpowered phase 2 cleared all incoming colors;
  redstone power refilled exactly 100,000 of their own color only; power removal
  cleared it at the next phase 2. The upper link persisted.
- Monitor: empty adjacent Smelter produced actual wire strength 15; offering raw
  iron changed it to 0 without consumption. An ordinary charged node alone did
  not produce a readiness signal.
- Red Aura: no explosive caused no upward movement. TNT above fuse 2 remained
  present without movement or damage; fuse 2 was consumed and moved exactly
  100,000 Red from lower to upper node (two-block rise, 200,000 lift budget).
  Storage conservation, absent TNT/drops and intact stone witnesses passed
  after the remaining fuse interval.

The first TNT attempt incorrectly used legacy uppercase `Fuse`. Actual modern
NBT inspection established lowercase `fuse`; both controls were repeated after
correcting the helper. That failed setup is retained in the action log, not
counted as a Red Aura pass. Four temporary forced chunks were removed.

## Actual Survival Mining

A supported station at 322,180,191 aimed at 322,181,194. Materials and tools
were command-provided, but blocks were broken through held vanilla ATTACK
input, never `setblock` or a helper destruction call. World ticks ran normally.

- Plain diamond pickaxe broke iron ore and yielded one raw iron, damage 1.
- Red 4 / Yellow 4 diamond pickaxe broke the same ore and yielded exactly two
  iron ingots with no raw iron.
- The same pair on a pickaxe at damage 1,560 still yielded two ingots, and the
  last-durability tool disappeared normally.
- Red-only diamond pickaxe yielded one iron ore, proving its Silk Touch path.
- Plain diamond axe removed only the first of four connected oak logs. Green 1
  axe removed all four; an adjacent stone witness stayed unchanged. Four logs
  appeared in three item entities (actual item NBT was inspected).
- Angelsteel pickaxe with Fortune buff 3 and Red 1 retained its original custom
  data and enchantments (plus expected damage 1), with no persisted Fortune.
  However it yielded three raw iron, not Silk Touch ore. The missing custom-tool
  enchantability tag is a real integration failure assigned for correction;
  this is NOT a passed combined-enchantment regression.

Screenshots 943-947 and logs are under the private node-pilot playtest directory.
Initial hotbar slot 8 was preserved in empty slot 11, then restored; only seeded
tools/drops were removed. Final 21 inventory entries and original four equipped
accessories were retained. Player returned to the main supported platform in
Creative, ticks frozen, before exclusive UI handoff to the independent auditor.
No food, combat, veto, fairy-role or final packaged acceptance is implied here.

## Session Closure

The independent auditor subsequently exercised storage search and fairy roles;
those results and the disclosed fixture-setup failure are recorded separately in
[the fairy gameplay report](2026-09-23-fairy-gameplay-playtest.md).
The parent then used Save and Quit to Title followed by Quit Game. The tenth
authenticated client exited normally with code 0. The scoped monitor measured
1,361.7 MiB peak working set and 1,924.9 MiB peak private allocation, including
the client/integrated server and monitor. Desktop/agent hosts are outside the
user's requested budget; one-second samples are not an OS-enforced hard cap.
