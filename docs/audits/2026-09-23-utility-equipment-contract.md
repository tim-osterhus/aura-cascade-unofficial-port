# Utility And Equipment Contract

This audit uses `AuraCascade-592.jar` as the legacy behavior source. Focused
regression coverage accompanies the source restorations below; live acceptance
remains with client QA.

## Implemented In This Checkpoint

- Aura crystals carry 1,000 aura. Direct use on an attached aura-network block
  feeds that amount on the server, consumes one item outside creative, and
  succeeds on the client to prevent placement fallthrough. Dropped-crystal
  absorption uses the same charge function. Existing crystal recipes are
  unchanged.
- Protection amulets retain their audited damage families and blocked-family
  healing. Yellow halves projectile damage before vanilla mitigation and no
  longer heals after damage. Tick-wide fire clearing, air refill, fall-distance
  reset, and wither-effect removal remain removed.
- Red Hole dropped items use a per-item tick path, not a world callback. Source
  16 skips vanilla item ticking through ages 0-99 of each 100-tick blast cycle,
  then explodes at power 12 with `fire=false`, matching the non-fiery original.
  Its marker item receives a 30,000-tick `ItemEntity.age` despawn threshold;
  vanilla age persistence carries that limit through save/reload. Black Hole
  removes cobblestone stacks from player inventory every 100 world ticks.
- Angel's Heels uses an Aura-owned transient `STEP_HEIGHT` modifier. It targets
  total step height 2.0 while clear, adds 0.3 per horizontal-collision tick,
  and drops the collision ramp on the first clear tick. It never lowers a
  base-plus-other-modifier value already above 2.0. Unequip removes only Aura's
  modifier. This adapts the legacy direct field writes to attributes: it
  preserves the modern 0.6 base and other modifiers instead of resetting the
  value to the legacy 0.5 on unequip.
- Barbarian Sword multiplies the incoming raw damage once using the prior
  combo count, strict 5-to-99 tick window, `1.05^count`, and 100-count cap.
  The former second post-hit damage call is removed.
- Transmuting Sword rejects dead/nonpositive-health targets, preserves Slime
  size across Slime/Magma Cube swaps, preserves position/name/health, and only
  discards the original after the replacement is accepted by the server.
- Angelsteel pickaxe/axe/shovel buffs use the legacy multiplicative break-speed
  gates only with a correct tool. The later integration raises Fortune only
  on a copied loot tool for a higher buff level on eligible non-crop breaks;
  it never temporarily mutates the held stack. This supersedes the earlier
  mutation/restoration design, which failed nested-break review. Swords are
  excluded from mining buffs. See the drop-Fortune audit and current acceptance
  ledger for integration and actual combined-enchantment verification.
- Angelsteel sword colors use timed harmful effects with legacy duration and
  cadence: red fire placement, orange upward velocity, yellow lightning, green
  magic damage/spread, blue low-health magic damage, and violet position swap.
  Only the six attuned colors register effects; malformed, white, or black
  sword aura data leaves the model unattuned and uses the original red hit
  default. Red fire placement skips unloaded chunks, and violet swaps use the
  server-player teleport packet path where applicable. Parent integration now
  registers the Red Hole lifetime mixin and supplies all six effect names.
- Mirror redirects every nearby legacy fireball candidate within squared
  distance 25, searches a 100-block box for the first Blaze/Ghast-fired target,
  uses the unnormalized target delta divided by 15, and excludes Wither Skulls
  from redirection. The projectile owner is not reassigned.
- Thief Sword uses the original one-in-four roll, only for Villagers, and drops
  the first trade result.
- Forbidden Fruit uses the legacy item-name seed, potion-index rejection
  selection, Gaussian duration and 0-to-5 amplifier. The apple grants 7,200
  ticks of regeneration at amplifier 1. Its effect is deferred until food use
  completes rather than being granted when use starts.
- AuraItems registers `KaleidoscopicOriginalEffects.bootstrap()` only; it must
  not also register the replaced `KaleidoscopicEnchanterLogic.bootstrap()`.

## Acceptance Checks

- Use each colored crystal on an aura network in either hand: storage increases
  by exactly 1,000 and one crystal is consumed; creative use preserves the
  stack. A non-network block neither consumes nor places it. Dropped crystals
  charge the same amount.
- Exercise each protection damage family, including `IN_FIRE` versus lava and
  `ON_FIRE`; confirm Yellow halves pre-mitigation projectile damage and verify
  no periodic cleansing/refill behavior.
- Verify Red Hole timing, power, no-fire behavior, and lifetime across
  save/reload; verify Black Hole timing and complete-stack removal from every
  inventory slot.
- Verify Heels reaches 2.0 while clear, ramps by 0.3 per collision tick,
  removes only its ramp on clear, and preserves other step-height modifiers on
  unequip.
- Verify Thief Sword's first trade result and Barbarian combo boundary ticks
  4, 5, 99, and 100 against live incoming damage.
- Test Angelsteel Fortune with correct/incorrect tools, crops, and a tool with
  stronger vanilla Fortune. Exercise each color curse and each transmutation
  pair, including Slime/Magma Cube size.
- Redirect multiple nearby fireballs, test no qualifying Blaze/Ghast fireball,
  and verify Wither Skull immunity and original owner preservation.
- Eat or drink to completion with Forbidden Fruit equipped; cancel use and
  verify no effect. Verify deterministic non-apple effects across relogs.

## Remaining Parity Risks

- The mapped 1.21.1 API has no `Item#getEntityLifespan` override; the Red Hole
  implementation targets the persisted `ItemEntity.age` despawn check instead.
  Source 16's per-item tick/no-fire correction and bounded two-cycle live test
  pass; save/reload remains unverified. See the [current acceptance
  ledger](2026-09-22-video-acceptance.md#finite-current-beta-gates-2026-09-23).
- Angelsteel Fortune matches the correct-tool, strictly-higher-Fortune, and
  crop gates. Live acceptance should cover unusual loot tables and silk-touch
  combinations.
- Source `AngelsteelToolHelper.MAX_DEGREE` is 11 (material indices 0-10), and
  `getDegreeList()` supplies sword degree parameters 1-10. The sword's tab
  predicate also mentions degree 11, but no source material slot or generated
  parameter supports it. The port retains 12th-tier IDs/resources for
  compatibility; that extra entry is not source-backed and remains a parity
  gap rather than a reason to remove an existing ID.
- Angel's Wing activation remains a modern adaptation because its legacy item
  callbacks are no-ops and the video trigger remains acceptance-dependent.
- Prismatic Wand behavior is owned by Sol and is not part of this slice.
- Digger/Glider/Scare Fairy global behavior and mixins belong to the fairy
  owner; this utility slice must not register duplicate hooks.

## Verification State

The parent reported 183 tests green before this source checkpoint. No Gradle,
client, or server command was run here; the parent owns the next integration
compile and client QA owns live acceptance.

This verification paragraph is historical. The thirteenth full build now passes
227 tests; current runtime results and still-open per-item gates are recorded in
the [acceptance ledger](2026-09-22-video-acceptance.md). A later passing build
does not automatically accept every broad test suggestion above.

## Dated Red Hole Correction (2026-09-23, Source 16)

An earlier revision of this contract said the blast used fire. Legacy source
review confirms `fire=false`; that wording was incorrect. Source 16 schedules
the behavior from the marker's per-item tick, skips vanilla item ticking for
ages 0-99 in each 100-tick blast cycle, and retains power 12 and the 30,000-tick
marker lifetime. Source 16 is green at 244 tests across 60 suites, and the live
two-blast retest passed at game times 124000/124100 with marker ages 0/99, an
unchanged 32-block witness, and no extra explosion during the one-second freeze
control. This is a bounded runtime pass, not save/reload or wearer-immunity
acceptance; see the [current acceptance ledger](2026-09-22-video-acceptance.md#finite-current-beta-gates-2026-09-23).
