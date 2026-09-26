# Completion Plan

The user requested completion, not another isolated checkpoint. These are internal
work phases, not approval gates between turns. Continue implementation and
verification across phase boundaries; ask only for a real ambiguity, missing
authority, or external blocker. Do not call the full port complete while required
gameplay or release acceptance remains open.

## Final Checkpoint

The finite implementation and beta gameplay acceptance phases are complete.
The final sixteenth candidate passes 244 tests, packaged font/world checks and
15 multiplayer checks; independent review found no remaining identified B1-B5
gameplay blocker. Mod Lab's agreed alpha was already completed separately.
CurseForge accepted the reviewed 0.2.0+1.21.1 beta as file `8959308`; its public
page initially returned 404, so public availability/approval remains unconfirmed.
See the [publication record](../releases/0.2.0-publication.md). Do not interpret
bounded beta acceptance as exhaustive survival/modpack or endurance coverage.

## Phase 1: Core Progression

- Retain the verified amethyst crystal recipe and navigable guide pilot.
- Complete consumer inspection and correct common consumer timing/power behavior
  against the shipped original, including truthful progress and power feedback.
- Restore vortex semantics and distinct per-pedestal colored power requirements;
  resolve ingot-to-gem, prism, and subsequent progression against the original JAR.
- Integrate the client readouts, actual recipe instructions and surviving state.
- Exercise ordinary acquisition/crafting and fueled falling-power production;
  injected inputs may isolate mechanics but do not prove full progression.
- Finish remaining pump trigger/color controls and representative node variants.

## Phase 2: Remaining Systems

- Restore a usable Bookshelf Coordinator browser, selected item retrieval, search,
  scrolling, counts, power gating and server-authoritative inventory behavior.
- Audit and repair fairies, accessories, enchanting, rituals, traversal and utility
  tools against the original and video ledger, including the disputed Shattered
  Stone behavior. Existing summaries and unit tests are not acceptance by themselves.
- Expand the Encyclopedia to cover the original instructional surface with
  readable pages, recipes and links for actual implemented behavior. Preserve
  immersion and the approved amethyst change; do not hide incomplete mechanics
  behind rewritten instructions or silently approve historical substitutions.
- Add focused persistence, negative/control and multiplayer-authority tests.

## Phase 3: Release Acceptance

- Run the complete deterministic suite, dedicated-server smoke and packaged-client
  checks, plus independent progression, UI, persistence and gameplay regression QA.
- Reconcile every acceptance-ledger row and record approved intentional differences.
- Verify memory under the user's 4.5 GB Minecraft/build/testing workload scope;
  serialize heavyweight runs and keep Codex desktop usage informational.
- Review artifact contents, dependencies, attribution, version and beta notes.
- Publish the completed beta to the existing CurseForge project only after the
  [release gate](2026-09-22-beta-release-gate.md) is satisfied. Verify actual result.

## Independent Mod Lab Track

COMPLETED: the agreed alpha scope is pushed at
`1874f965350d271828a25290f36337b2fee1d07c`. Windows/Linux CI, 51 local tests
(one symlink-privilege skip), sanitized second live fixture, deterministic
packaging and isolated installed-plugin discovery/replay passed. Parent verified
the clean working tree and Tim Osterhus author/committer identity. This is not
a claim of production-intermediary or other-loader compatibility. The Mod Lab
owner has finished; the full Aura port continues independently.

The existing Sol Xhigh owner completes the finite reusable-alpha scope in
`../mc-mod-lab`: manifest/report validation, portable example,
clean packaging/install proof, independent second fixture, CI, documentation and
publication review. It does not wait for Aura's full release. Each validated,
sanitized checkpoint is committed and pushed to `tim-osterhus/mc-mod-lab` as
Tim Osterhus <tim@millrace.ai>. Deliberately unsupported platforms are documented,
not disguised as supported and not expanded into an unrequested universal tool.

Luna Max owns bounded implementation; Sol High handles difficult behavior;
Astra low independently playtests. The parent owns contracts, integration,
memory/runtime coordination, unresolved decisions and final release acceptance.
Preserve the pre-existing dirty Aura worktree and never stage it wholesale.
