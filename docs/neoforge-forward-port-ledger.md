# NeoForge 1.21.1 Forward-Port Ledger

Canonical gameplay is maintained on Fabric Minecraft 1.21.1 `main`.
The native target is `neoforge/1.21.1`; it starts from `44cc057` (0.2.1)
but will release only as 0.2.2 after canonical repairs and target acceptance.

| Canonical source | Target source | Disposition |
| --- | --- | --- |
| `44cc057` | `neoforge/1.21.1` development checkpoint | Native build, 263 regressions, packaged client/server and five hook controls pass; full target acceptance pending |

No canonical 0.2.2 repair commit has been imported yet. The active repair
register is owned by Mod Playtesting and is not frozen. Inherited Breeder and
Binding Ring guide defects remain unresolved here; do not conflate baseline
defects, target regressions, and invalid test fixtures.

For each accepted repair record its ID, canonical SHA, target SHA, affected
tests, packaged candidate hash and acceptance result. Prefer `cherry-pick -x`
for coherent fixes; record explicitly adapted or non-applicable changes. Keep
platform-only corrections separate and do not merge the entire Fabric branch.

Validated candidate SHA-256:
`c841a49b196f2136ddd8b2f401964ebe27aec07836a9d95f9a2f288be1d5eb5d`.
Evidence and target-specific coverage gaps are in
`audits/2026-09-28-neoforge-1211.md`. This is readiness for repair intake,
not completion of canonical 0.2.2 or approval to publish.
