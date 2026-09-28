# Forward-Port Ledger

`main` is canonical for Minecraft 1.21.1. `fabric/1.21.11` is the target branch;
its platform adaptations do not replace the canonical implementation.

| Canonical checkpoint | Target checkpoint | Gameplay version | Disposition |
| --- | --- | --- | --- |
| `44cc057` | `13a4c39`, `d898c99` | 0.2.1 | Source port and target rendering corrections committed/pushed; bounded target runtime checks pass. CurseForge accepted beta 8995986; public availability unconfirmed. |

The source checkpoint includes the five 0.2.1 feedback/guide/Creative fixes and
the immersive amethyst-plus-eight-gold-nugget crystal recipe. Registry and mod
IDs remain unchanged. Target-specific changes cover registration keys, item
components/models, render state submission, ValueInput/ValueOutput serialization,
recipe ingredient schemas, mixin descriptors and the pinned Fabric toolchain.

Target source validation: 258 tests in 65 suites, none skipped, plus a successful
production build. This is not exhaustive gameplay acceptance. See the
[target audit](audits/2026-09-25-port-12111.md) for candidate hashes, test scope,
runtime failures, corrected fixtures and remaining release gates.

`d898c99` adds opaque alpha to HUD/storage-screen text for the target drawing API
and a particle texture fallback for the invisible Fairy Torch model. No canonical
gameplay counterpart is needed. The separately discovered canonical Breeder Fairy
juvenile-eligibility issue is not fixed by this port and remains a tracked risk.

## Subsequent Changes

1. Commit each coherent gameplay/content change on canonical `main`, with its
   focused tests. Keep unrelated formatting and platform migration out of it.
2. Forward-port that actual commit with `git cherry-pick -x`, resolving target
   API or data-format differences explicitly. Do not merge the entire branch.
3. Record the canonical and target commits here with the affected tests and the
   target packaged smoke result. Track intentionally deferred changes explicitly.
4. Keep target-only API corrections on this branch. If review discovers a shared
   gameplay defect, fix and validate it on canonical before recording both ports.

No post-baseline canonical update has been supplied yet, so a later update
rehearsal is not claimed. Do not invent a content change just to populate this
ledger. Maintain separate JARs and Minecraft suffixes for each release target.
