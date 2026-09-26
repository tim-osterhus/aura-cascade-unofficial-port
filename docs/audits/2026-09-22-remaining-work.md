# Remaining work after baseline audit

This is a player-experience backlog for the current Minecraft 1.21.1 working tree.
The goal is to preserve the original mod's usable workflows. Describing a missing
workflow as an intentional substitution does not close it.

Evidence reports:

- [Measured build baseline](2026-09-22-baseline.md)
- [Original experience and public feedback](2026-09-22-original-experience.md)
- [Current implementation audit](2026-09-22-current-implementation.md)
- [Actual client playtest](2026-09-22-playtest.md)

## First repairs

| Priority | Work | Evidence so far | Acceptance |
| --- | --- | --- | --- |
| P1 | Repair item opening and replace the ten-page guide summary with a usable encyclopedia | In the live test, the Aura item did not open while a vanilla written-book control did. Copying its generated component to a vanilla book demonstrated clipping: only two of six checklist entries fit on page 1. This diagnostic copy is not a successful normal-item workflow. Source has no click events or pagination; the original jar has categories, index/back, bookmarks and recipe-page classes. | Open the item normally; navigate categories and entries; follow recipe/topic links and return; read every line at supported GUI scales; learn a working first setup without source access. |
| P1 | Restore node/world visual and inspection behavior | Live screenshot shows a full-block selection outline around the small node and an apparent missing stone face beneath it. Source confirms default full shapes and ordinary occlusion properties, with no client node HUD/flow renderer. Collision behavior was not separately tested. The CurseForge rendering report has unknown file version. | Correct model, collision, outline and neighboring-face visibility; inspect stored aura by color and amount; see transfer and falling power during real operation. Compare against a version-matched original fixture. |
| P1 | Repair 1.21.1 enchantable-item tag compatibility | Live resource loading rejects `aura:kaleidoscopic_enchantable` because `#minecraft:enchantable/melee_weapon` is absent. A diamond pickaxe rejected the red Kaleidoscopic enchantment through `/enchant`. Source is `src/main/resources/data/aura/tags/item/kaleidoscopic_enchantable.json`. | No missing-tag error; intended tools/weapons accepted; enchanter single-color and pairwise behavior exercised in-world. The custom machine/effect matrix was not exercised by the command test. |
| P1 | Make progression recipes and machine setup discoverable, then validate the chain | Source-confirmed: fixed custom recipe catalogs exist but guide omits concrete recipes and operating detail. Passing catalog tests do not prove a playable chain. | Starting from survival acquisition, craft nodes/pumps, generate falling power, process an input, perform a vortex recipe and reach the next tier using in-game information. |

## Subsequent repairs and decisions

| Priority | Work | Evidence/limits | Acceptance |
| --- | --- | --- | --- |
| P2 | Resolve adjacent-node interaction against the original | Adjacent upper nodes stalled in the seeded creative-pump test. The same flow layout with an air gap produced an iron ingot through actual falling power. Node redstone output and redstone-gated sending are a plausible cause. | Determine the original adjacency/redstone contract, reproduce it in the port, and explain supported setup in the guide. Keep both fixtures as regression cases; do not call the whole smelter subsystem broken based on the stalled layout. |
| P2 | Correct onboarding milestones | Source marks item possession as placing/running/powering. | Use actual completed actions, or honestly label acquisition milestones; verify before and after performing each action. |
| P2 | Restore useful storage inspection and retrieval | Source exposes aggregate totals and first-entry extraction; original exact selection workflow needs further reference work. | Identify stored contents, retrieve the desired item, preserve item components/counts through save/reload and shelf conversion. |
| P2 | Verify fairy roles and equipment workflows against original | Current guide lists roles without their triggers/effects; allay and inventory-backed substitutions need player-level evaluation. | In-game explanations, observable role behavior, legible equipped state/slot rules, and persistence across reload/death as specified by the original. Avoid adding new interfaces without establishing the needed original workflow. |
| P2 | Implement supported Fluxing Node energy interoperability | Production bridge defaults to NOOP; tests install fake adapters. A seam is not an integration. | Choose the supported modern energy API, export to a real compatible consumer and verify accounting/limits. Honest unsupported messaging is useful but is not parity completion. |
| P2 | Audit late-game tools, rituals and enchantment interactions | Existing tests and substitutions leave world-level consequences unverified. | Bounded, version-referenced fixtures for wand block entities/fluids, explosion protection, ritual bounds, loot, and enchantment combinations; no duplication or data loss. |

## Verification work

1. Replay the original 1.8.9 mod in a separate, sequential session, beginning with
   book navigation/recipes, node hover information, pump transfer and a smelter.
   The original was not run in this audit; jar and gallery evidence are not a replay.
2. Turn each confirmed regression into a meaningful behavioral or client check.
   Keep file/phrase audits as packaging/documentation checks, not gameplay gates.
3. Verify actual packaged-mod loading in addition to the dev client, save/reload,
   dedicated-server behavior, and a second client for synchronization where relevant.
4. Measure cold builds, representative larger worlds, and the combined local
   agent/desktop workload before promising a sustained 4.5 GB process budget.
   Continuous measurements cover build/client plus monitor only. A broader desktop/
   agent-inclusive snapshot reached about 4.42 decimal GB private allocation, close
   to the ceiling, but is not a measured session peak.

## Implementation strategy

Continue in this repository and retain the simulation, resources and tests that
survive comparison. Rebuild deficient client interfaces or individual mechanics
where needed. The evidence does not justify discarding the whole source tree,
but it also does not support calling this a small final-polish task.

Use Luna Max for most bounded implementation and reference/fixture work, Sol High
for difficult engine/persistence corrections, Astra low for independent client
playtests, and the parent for scope, integration and evidence review. Serialize
builds and game clients. A worker's claim of success is not a release criterion.
