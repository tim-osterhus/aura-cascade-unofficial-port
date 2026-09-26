# Staggered Mod Lab and Aura execution

## Scope and authorization

The user resumed the staggered workflow and then explicitly requested immediate
public creation of `tim-osterhus/mc-mod-lab`, with the Sol owner committing and
pushing clean, validated checkpoints. Incomplete work is allowed when labeled
honestly. Author and committer identity: Tim Osterhus <tim@millrace.ai>.

The 4.5 GB limit applies to Minecraft and build/testing tooling, excluding the
Codex/ChatGPT desktop application. Working set and private allocation are reported
separately. Runs remain serialized, with sampled guards below the limit; this is
not a hard operating-system memory reservation.

## Initial toolkit checkpoint

- Public repository: https://github.com/tim-osterhus/mc-mod-lab
- First unit-validated checkpoint: `8d2e43345821efab8171ceeff832163ec8b42df4`.
- Sol reported 24 tests passed, one skipped, plugin validation and patch rebuild.
- Parent independently confirmed public visibility and default branch `main`.
- This initial commit is not a successful real-client CLI or mod-parity result.

The Sol owner subsequently pushed `e0ac666`, `b38ecca`, `3e55015`, and
`79aa58e6498263c959655bf91b49c55adb589007`. The last two package the frozen lab6
player patch, sanitized live pilot evidence, final memory/exit record, and
publication ignores. Parent verified local HEAD equals origin/main, a clean
toolkit worktree, and Tim Osterhus <tim@millrace.ai> as both author and committer
on all five commits. Sol reports 34 passing unit tests plus one skipped test,
with rebuilt patched sources matching the tested local sources apart from line
endings. The separately rebuilt artifact itself has not been runtime-tested.
The final metadata-only checkpoint `c4d9d12974df68b554da12be177c8598ecd9e93e`
corrects the plugin description to its tested alpha scope. Parent verified that
HEAD equals origin/main and that GitHub's `toolkit` workflow passed for this and
the preceding three checkpoints. No plugin installation is claimed.

## Capability proof and actual findings

The pinned Minecraft Mod MCP v0.3.0 Fabric 1.21.1 release artifact was downloaded
and its SHA-256 matched GitHub's release metadata:
`55aab04b1d7ac9203817e071cb83b6d6cf3da7164636867da33877750de64636`.
It was not accepted unmodified: the HTTP server binds all interfaces and has no
authentication. A local derivative requires a per-session bearer token, rejects
browser Origin headers, and binds only 127.0.0.1. Live checks confirmed listener
ownership, unauthenticated HTTP 401 and Origin-bearing HTTP 403.

The proof also found and addressed development-runtime problems in the upstream
adapter: cropped 854x480 screenshots of a 1280x720 framebuffer, cached captures,
false-success mouse input, and placeholder world/player identity. Small reviewed
source patches now use the actual framebuffer dimensions and real Minecraft
screen events, integrated-server world metadata/path and player identity. This
profile is scoped to the tested Mojang-mapped 1.21.1 Fabric development client;
normal distributed/intermediary-mapped clients and other versions are unproven.

Verified locally before delegating gameplay edits:

- Full 1280x720 screenshots with visible screen edges, title and controls.
- Real clicks navigate title, world selection and a cloned disposable save.
- Actual world path and name agree with the selected fixture; mode is creative.
- Actual player name and mode replace upstream empty/default values.
- A server command changes time with matching server-log output.
- The E key opens CreativeModeInventoryScreen.
- The unchanged Aura guide still opens no screen, while a vanilla written-book
  control opens BookViewScreen through the same use-item path.
- Exit control mode before opening PauseScreen: upstream intentionally suppresses
  that screen while takeover is active. Save-and-quit and normal client exit work.

The reusable CLI successfully copied a closed seed to a marked disposable fixture.
Its first live doctor correctly stopped at an unsupported Java `@argfile` launch
identity. Checkpoint `b38ecca` supports that Windows launch form without bypassing
process/world checks. Parent validation against the live client then established:

- Doctor: `ready`, selected launch/PID/world checks accepted.
- Vanilla written-book use: `captured`, actual `BookViewScreen`; parent visually
  reviewed the full 1280x720 PNG and readable control text.
- Deliberately wrong expected screen: `fail`, exit 1, screen mismatch.
- Actual unchanged Aura guide use: `fail`, exit 1, no screen opened.
- Capture reports record acknowledged exit from control mode.

`captured` deliberately does not mean visual parity passed. The bridge still
reports an unknown Minecraft version; the CLI instead verifies the version from
the selected launch's fresh log. Its GUI widget labels remain blank/duplicated.

## Memory evidence

Ignored runtime evidence remains in `build/qa-audit/`; credentials, full local
identity JSON, raw worlds, downloaded JARs and unrevised logs are not publication
inputs. The first fully loaded-world, normally exited proof (`bridge-key-client`)
peaked at 1267.8 MiB working set and 1802.8 MiB private allocation for client plus
monitor. The bridge runs within that client. A separate launch-setup build peaked
at about 1 GiB private allocation. These are measured bounded runs, not guarantees
for a large modpack, cold dependency setup or all future gameplay.

The completed `bridge-player-client` run, which included the real CLI captures,
exited normally with peaks of 1361.2 MiB working set and 1885.9 MiB private
allocation for client plus monitor. The capture CLI additionally sampled its own
working set. These are separate observations, not a synchronized peak covering
every short-lived helper process.

Earlier experiments stopped on a conservative combined-desktop guard before the
user clarified scope. Preserve those failures as historical evidence; do not
reinterpret them as exceeding the clarified Minecraft/tooling-only budget.

## Active Aura pilot

The parent opened the bounded gameplay gate after the capability proof above.
Luna Max workers own disjoint changes: Patchouli guide integration/resources,
the shaped amethyst crystal recipe, and the Encyclopedia acquisition recipe.
Recipe tests decode the real pattern codec and exercise Minecraft's matcher;
vanilla-registry surrogates do not replace live Aura crafting verification. The existing
guide item identity is retained. All source changes require sequential build/test
validation and independent Astra low client QA before being reported as working.
Full original guide coverage and overall Aura parity remain open.

The integrated pilot now compiles and builds with Patchouli. The first full run
caught an entry referring to nonexistent category `aura:late_systems`; the parent
corrected it to `aura:late_game`, then reran the complete suite. All 89 tests in
32 suites passed with no skipped tests or errors (`guide-pilot-build-2`), including
the earlier enchantment-tag audit. The same run produced the mod JAR and refreshed
client launch configuration. Peak build working set was 1325.7 MiB and private
allocation 1439.5 MiB; exit 0, no memory-guard stop.

The guide contains six categories and twelve entries, including migrated current
system summaries and three onboarding entries. The competing written-book
generator is removed. This does not restore all 101 original entries. Independent
Astra low client QA is recorded in the
[independent playtest](2026-09-22-guide-pilot-playtest.md). It passed normal and
newly crafted book opening, sampled category/entry/back/internal-link navigation,
crystal recipe presentation, actual survival crafting with exact consumption,
iron-center rejection, and early Encyclopedia crafting without a Prism. Test
materials were supplied by commands; outputs were obtained through actual GUI
crafting, not injected. Natural ingredient gathering remains unverified.

Pilot body text fits at GUI scales 2 and 3, but independent review caught two
overflowing headings. The parent shortened those strings and added a regression
check. The independent follow-up subsequently passed both corrected headings
at GUI scales 2 and 3, with all four screenshots reviewed and preserved. Full-book
readability, dedicated-server operation, and production-JAR client testing are
also still open.

The final `guide-title-fix-build-2` test/build/launch-setup run passed, with 90
tests in 32 suites, no skips/failures/errors. Peak working set was 1391.2 MiB and
private allocation 1515.7 MiB; exit 0. This run also corrected a pre-existing
documentation-test coupling: historical release records are checked for structure
instead of compared against mutable outputs from the previous local build.
The earlier release hashes in PORTING_NOTES remain historical evidence.

Guide-pilot checkpoint artifact measurements, taken after that successful build
(later node-pilot builds replace these development artifacts):

| Artifact under `build/libs/` | Bytes | SHA-256 |
| --- | --- | --- |
| `aura-cascade-0.1.1+1.21.1.jar` | 1331924 | `742febd79c7f91a027f73551f04dc55949542d73a7fdf1687a837901d6d4866f` |
| `aura-cascade-0.1.1+1.21.1-sources.jar` | 1127392 | `7b73eec6b2b8ca154578c2a00d216b9eeedad0541395c83de21c68b6f65f8f0d` |

The independent playtest's measured client saved and exited normally (exit 0).
Client plus monitor peaked at 1277.3 MiB working set and 1808.5 MiB private
allocation, with no memory stop. The parent released the live-game slot to the
separate PsiAI task; no simultaneous Minecraft clients were launched. A later
Aura visual recheck was coordinated after PsiAI released the slot and passed.
That client also saved and exited normally: peak working set 1321.9 MiB, private
allocation 1817.4 MiB, no memory stop, exit 0. The slot is released again. No Aura changes were
committed or published from its pre-existing dirty worktree.

The acquisition recipe restores the original shaped `CB` (white crystal + book),
yielding one Encyclopedia and fitting the player's 2x2 grid. The previous port
also required an Arcane Prism, incorrectly delaying access to basic guidance.
Both the original 1.8.9 JAR's `ItemLexicon.getRecipeItem()` and upstream
[`ItemLexicon.java`](https://github.com/AdlyTempleton/Aura-Cascade/blob/master/src/main/java/pixlepix/auracascade/item/ItemLexicon.java)
confirm crystal + ordinary book, with no prism. This restoration is distinct from
the user-approved amethyst modernization of the white crystal recipe.

## Node And Pump Pilot

The next bounded slice uses Luna Max for model-matched geometry and the client
inspection HUD, Sol High for bytecode-grounded pump behavior, and Astra low for
independent live acceptance. The parent integrates change-detected server state
updates and color-matched transfer particles. The particles are a bounded modern
presentation of actual movement, not a claim of exact legacy animation parity.

The [pump contract](2026-09-22-pump-contract.md) records the recovered eligible
fuel duration, nominal speed, alternator comparison and staged integer rounding.
The first integration run compiled all main and test sources and ran 103 tests
in 35 suites: 102 passed, with one geometry-test registry-bootstrap failure.
It did not complete the build. Peak working set was 1396.6 MiB, private allocation
1526.0 MiB, no guard stop; evidence is `build/qa-audit/node-pilot-build/`.
This initial result is not live-game acceptance or release readiness.

After moving the geometry fixture to actual registered blocks in Fabric's target
class loader, `node-pilot-build-2` passed all 103 tests in 35 suites, with no
skips, failures or errors, followed by build and client-launch setup. Peak
working set was 1379.4 MiB and private allocation 1513.3 MiB; exit 0 and no
memory stop. The next independently operated client uses
`build/qa-audit/node-pilot-client/`; the PsiAI task confirmed the shared workload
slot was free. No other build or Minecraft client is run alongside it.

Node-pilot checkpoint artifact measurements:

| Artifact under `build/libs/` | Bytes | SHA-256 |
| --- | --- | --- |
| `aura-cascade-0.1.1+1.21.1.jar` | 1346862 | `d7190a4d64adb6ade07df0a72d72adb5c582e394f76cb4c4a06fa536973878b4` |
| `aura-cascade-0.1.1+1.21.1-sources.jar` | 1132221 | `834d47921dce30334a80ed4467ec884c29f94f7ab5eb78527edc66787d8cf162` |

Final publication is now explicitly authorized as a **beta** on the existing
CurseForge project, only after full-port acceptance. See the
[release gate](2026-09-22-beta-release-gate.md). The old verdict-driven publisher
is disabled so historical completion state cannot release this partial repair.

### Integrated Outcome

Independent Astra low gameplay testing found two real failures: an adjacent
receiver directly powered its pump and stopped transfer, and the revised first
pump-guide page clipped its final line. Both baseline failures are retained in
[the playtest record](2026-09-22-node-pilot-playtest.md). The parent removed direct
node/pump redstone output while retaining comparator output, and split the fuel
paragraph into shorter pages. Focused rebuilt-client evidence verifies adjacent
flow, external-redstone freeze/resume, sampled state persistence across client
relaunch, scale-3 pump HUD, and the corrected guide spread at GUI scales 2/3.
The main audit independently exercised ordinary dropped coal, empty/no-target
fuel behavior, white-crystal absorption/live synchronization, bounded white
transfer particles, and representative collision/occlusion.

The initial redstone-regression test used reflection on a protected block method
and failed; it was corrected to query the public block-state API. The complete
`node-feedback-fix-build-2` and final `node-guide-pagination-build` runs passed.
The final run has **104 tests in 35 suites, no failures/errors/skips**, followed
by successful build and launch setup. Final build peak working set 1324.8 MiB,
private allocation 1444.6 MiB, exit 0. This is a development runtime checkpoint,
not packaged-client, dedicated-server or full-mod acceptance.

All three client runs saved and exited normally, with no guard stops:

| Run | Peak working set MiB | Peak private allocation MiB | Exit |
| --- | ---: | ---: | ---: |
| `node-pilot-client` | 1306.6 | 1854.6 | 0 |
| `node-feedback-fix-client` | 1291.0 | 1817.9 | 0 |
| `node-guide-pagination-client` | 1279.8 | 1807.8 | 0 |

Scope is Minecraft/integrated server and its monitor, excluding desktop/agent
hosts as requested. One-second samples do not constitute a hard OS memory cap.
Builds and clients were serialized; final PID 22460 exited, no Java process
remained at verification, and the shared workload slot was returned to PsiAI.

Final checkpoint artifacts (supersede the earlier node-pilot measurements):

| Artifact under `build/libs/` | Bytes | SHA-256 |
| --- | ---: | --- |
| `aura-cascade-0.1.1+1.21.1.jar` | 1346609 | `4815ee1a8a222b436e58719ec940b7586d0da91d334b2cd0828b4549046aaac2` |
| `aura-cascade-0.1.1+1.21.1-sources.jar` | 1132169 | `2fcc873612899ccdcd4b16cda37ee1205c5f7490a4ff2fad137ea3482ab69347` |

Remaining coverage includes additional colors, all pump triggers/variants,
nonzero stored-power persistence, consumer/vortex inspection, full guide content,
storage UI, accessories/fairies/rituals, production-JAR client and dedicated-server
validation. Seeded test Aura proves transfer behavior, not survival acquisition
or full progression. No Aura commit or CurseForge upload was made.
