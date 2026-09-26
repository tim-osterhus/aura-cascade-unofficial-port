# Aura Network Transfer Contract

## Evidence Boundary

The comparison target is `AuraCascade-592.jar`. The reviewed original methods
are `AuraTile.canTransfer(BlockPos)`, `AuraTile.canTransfer(BlockPos, EnumAura)`,
`AuraTile.getWeight(BlockPos)`, `AuraTile.func_73660_a`, and
`EnumAura$4.onTransfer`. The current implementation is split between
`AuraNetworkBlockEntity` and `AuraKernel`.

## Natural Transfer

- Original position-level eligibility runs before transfer weights are summed.
  Weight is `(20 - EuclideanDistance)^2`; regular nodes add retention weight
  `400`, while capacitors add zero.
- The port now filters source-position eligibility before weight normalization.
  Current source families' `canSendAuraTo` implementations are color-independent
  at this position gate; color flow and target receive restrictions stay after
  position shares are calculated.
- The original natural-transfer cadence is a three-phase cycle: plan at tick
  phase 0 (`gameTime % 20 == 0`), apply at phase 1, and process induced Orange
  bursts at phase 2. Link refresh remains every 200 ticks.
- The post-share checks retain the legacy equilibrium threshold and natural
  directional/color rules. Existing target receive checks remain in the final
  server-side application filter.

## Orange Induction

`EnumAura$4.onTransfer` inspects Aura tiles in the source-centered cube with
radius two, excludes the source and nodes lying in the transfer direction or
its opposite, then considers each matching downstream link. It can induce more
than one downstream current from a nearby branch node.

The port gathers linked Aura network entities from the source-centered cube
rather than limiting candidates to the source's direct `connected` map. It
queues every matching downstream link aligned with the horizontal transfer
direction, with map semantics (a later request for the same branch/link replaces
the earlier amount). Queues are transient and are reconciled at phase 2.

At phase 2, each queued request is compared with the reverse request stored by
the linked node. Only the positive net request transfers; when the reverse is
larger, the excess is left as the reverse node's request for this phase. The
592 scale expression is exactly
`min(1, net / totalAura) - storedOrange`, where `totalAura` includes Orange.
The port preserves that expression and its per-color integer truncation, but
clamps a negative result to zero: legacy `percent` could create negative aura
and the modern `AuraStorage.scaled` contract rejects negative factors. That is
the intentional safety edge, not a claim that corrupted negative legacy aura
is reproduced.

Link scans stop before the first unloaded chunk in a direction, and cached
linked-position lookups skip unloaded chunks without requesting their block
state or entity. Orange candidate scanning likewise queries loaded chunks only.
If a phase-2 downstream endpoint is temporarily unloaded, its transient request
is deferred while the branch node remains loaded; it is discarded if the link
is no longer in that node's cached link set. This avoids forcing a chunk load;
the original's endpoint lookup could load it.

Induced requests transfer non-Orange contents proportionally to the net Orange
request and preserve the existing falling-power pedestal callback. The runtime
keeps the parent-owned Red explosive behavior, capacitor retention setting,
crystal feed API, and pedestal callbacks intact.

## Preserved Adjacent Mechanics

- Red TNT and Creeper absorption/push behavior remains in the parent-owned
  `AuraNetworkBlockEntity` changes.
- Capacitor passive-retention weight remains zero.
- Natural and induced falling transfers still call
  `VortexPedestalBlockEntity.receiveFallingPower`.
- `feedCrystal(AuraColor, int)` adds supplied aura only on an attached server
  entity, marks it dirty, and sends a client block update. Crystal charge and
  consumption are selected by the item callback, not duplicated here.

## Verification

`AuraKernelTransferTest` covers pre-weight source-position rejection, multiple
Orange downstream links, forward/reverse burst reconciliation, and the negative
legacy-factor clamp. `AuraConsumerLogicTest` covers common
`c:ores/<material>` to `c:dusts/<material>` key pairing; processor runtime uses
the active item-tag lookup and only enables conversion when the matching dust
tag has an item. The newly added tests have not been run against this working
tree.

No Gradle or live gameplay validation was run for this patch. The parent's
previous compile and 119 passing tests predate these edits and are not evidence
for them. Live acceptance remains needed for two parallel Orange circuits around
a source (including opposing bursts), natural-transfer phase timing,
source-gated transfer weighting, optional tag-backed 2/3-dust processing,
crystal-use charge/consumption/sync, and Red explosive handling. Do not treat
those fixtures as passed until the parent runs the integrated build and client.
