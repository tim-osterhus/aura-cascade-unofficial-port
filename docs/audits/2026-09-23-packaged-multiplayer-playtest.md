# Packaged Multiplayer State Acceptance

**B5 owner-only accessory synchronization, binding and owner leave/rejoin: PASS.**
Parent executed the private loopback fixture; this report reads its saved results.
This is not full B5 storage/circuit acceptance or a both-clients visual PASS.

## Artifact And Run

- Run: `build/qa-audit/multiplayer/20260923-093241-443`, 2026-09-23
  19:32:42.2202444Z through 19:34:44.6928327Z.
- [Summary](../../build/qa-audit/multiplayer/20260923-093241-443/summary.json):
  `success:true`, no failure, all 15 named checks passed.
- Aura `0.2.0+1.21.1` SHA-256:
  `306ab942b05029d814607a155e3a3796eaed3384b85af0cf92423a74c5ac63d4`.
- Separate private probe SHA-256:
  `08b3f87a892db803ef75b61eb271365ba62195f19bd07f4ebe4a4d7319fd5fd8`.
- Actual loaded Aura origins are packaged JARs; server and both clients report
  `intermediary` and the matching Aura hash. Offline fixture identities
  `AuraOwnerQA` and `AuraWitnessQA`, server bound to `127.0.0.1:25576` only.
- Peak aggregate private allocation **3032.2 MiB**, working set **2512.7 MiB**.
  All four process instances (server, initial owner, witness, rejoined owner)
  exited normally with code 0. Sampling is 250 ms, not an OS hard cap.

## Observed State

Labelled server-stdin commands supplied the floor, raw ring and charm. Actual
production accessory-menu QUICK_MOVE equipped the ring; vanilla selected-slot
and item-use packets bound the charm. No attachment, bound-role list or fairy
entity was injected. Owner `lastUse` records MAIN_HAND, slot 1, one real charm,
and SUCCESS; server state, not that client return alone, establishes binding.

- `initial-bound-{server,owner-initial,witness}.json`: server/owner show one
  physical slot-1 ring, one bound role and zero remaining charms. The witness's
  remote-owner accessory attachment is absent, not merely an empty synchronized
  attachment. All three observe one fairy with UUID
  `9816d7f3-5a9f-45d5-971e-8f23e3066a1d`, owner
  `363b8588-f617-3f12-ad54-3531e058848c`, fairy slot 0.
- `owner-absent-{server,witness}.json`: only the witness player remains, with
  zero fairies after the owner's normal exit.
- `rejoined-bound-{server,owner-rejoin,witness}.json`: same owner identity and
  persisted bound ring return without opening the menu or repeating equip/use.
  Owner-only attachment visibility is retained. Exactly one replacement fairy,
  UUID `79f3ba3d-c12b-48d6-a579-96d076702113`, agrees across server/both clients.
  The UUID changes across logout, not between observers within either phase.
- Maximum observed fairy count is one. This establishes no observed duplicate
  entity lifecycle, not independent measurement of every role's effect cadence.

## Visual Boundary

All four framebuffer capture gates produced PNGs. Parent image inspection found
the pink fairy visible in `owner-initial/capture-4.png`. In witness
`capture-1.png` and `capture-2.png`, the third-person body occludes the owner/fairy;
**witness fairy pixels are not confirmed**. Entity presence and matching UUIDs
prove client state synchronization, not unobscured rendering in those images.
No both-client visual PASS or invisible-fairy production defect is inferred.

## Earlier Attempts And D3

- `20260923-091306-403`: soft 3800 MiB guard stopped before equipment testing;
  peak private 3936.0 / working 2787.4 MiB, all three exits 0. No sync outcome.
- `20260923-092401-996`: reduced memory fit (2980.2 private / 2473.9 working
  MiB), but binding timed out after physical equip; all three exits 0. The QA
  action selected a hotbar slot and used it in the same tick. Installed Fabric
  invokes UseItemCallback before selected-slot synchronization; Aura's SUCCESS
  branch sends use and cancels that synchronization. The private probe now sends
  vanilla slot selection first on the same connection. Third-run binding passed;
  no production binding code was changed for this fixture correction.
- The third run's [D3 report](../../build/qa-audit/multiplayer/20260923-093241-443/consumer-lifetime-probe.json)
  also records transformed ItemEntity, matching candidate hash and PASS for
  `ageAndPersistence`, `merge`, and `oldMergeEligibility`. These are detached
  real-entity checks, not live consumer 500-tick cadence or world-reload proof.

Not covered: server restart, death/dimension lifecycle, storage or active-circuit
reload, malicious packets, every fairy role, or witness fairy-pixel visibility.
The accepted scope is B5 state, ownership and owner disconnect/rejoin persistence.
