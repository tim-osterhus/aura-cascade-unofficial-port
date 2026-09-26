# Node Controls and Red Aura Contract

Source: `AuraCascade-592.jar`, `AuraBlock`, `AuraTileCapacitor`,
`AuraTileBlack`, `AuraTileOrange`, `BlockMonitor`, and `EnumAura$3` bytecode.
These source changes await the next integration build and live acceptance.

- Capacitor sneak-use cycles 100, 1,000, 10,000, 100,000, starting at 1,000.
  The port restores empty-hand sneak-use, action-bar feedback, and inspected
  threshold. Ordinary use and other nodes remain unaffected.
- Capacitor natural distribution has zero retention weight, unlike the normal
  node's 400. The threshold gates sending. After base processing, a 19-tick
  threshold check arms the burst; the next divisible-by-five tick clears it
  and sets 110 ticks of blocked receiving. This replaces an inaccurate fixed
  five-tick countdown requiring zero cooldown before arming.
- Black and Orange Manipulators clear their entire aura storage at phase 2 of
  each second, replacing their own color with 100,000 only while powered.
  Thus unpowered manipulators destroy incoming aura. Network links and stored
  power are not discarded with aura storage. Their powered transfer exception
  and single-color receiving remain intact.
- Monitor weak power checks adjacent directions in vanilla order, excluding
  the output's opposite neighbor. The first pump yields 15 when empty and 0
  when fueled; the first consumer yields 15 without valid work and 0 with
  valid work. Ordinary nodes are ignored. Specialized late-game consumers
  always report valid work, as in the original. This is not a strongest-signal
  comparator aggregator. Once-per-second scheduled updates notify redstone
  even when the change came from fuel or item entities rather than placement.
- Red Aura checks a node-block AABB expanded by three every server tick.
  Nonremoved TNT at fuse <=2 is consumed for a 200,000 upward lift budget;
  a Creeper with swelling +2 >= maximum is consumed for 50,000. Each linked
  upper node receives at most budget/rise Red Aura, capped by the source.
  The original only finishes explosion visuals, without computing damaging
  rays; the port likewise emits sound/particles without a damaging explosion.

Focused tests cover registered threshold cycling, directional Monitor pump
selection, and Manipulator destruction without lost links/power. Existing Red
lift arithmetic tests remain; actual entity triggers and all new controls
still need live testing. This contract does not close all color/network parity.
