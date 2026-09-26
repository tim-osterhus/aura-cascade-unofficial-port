# Minecraft Mod Lab implementation proposal

## Decision and authorization

The supplied ChatGPT conversation is reference material, not an installation or
execution script. The user requested an assessment of how it changes the plan and
expressed interest in a reusable public plugin. The user subsequently authorized
the staggered workflow and immediate public repository creation. The toolkit is
now published at `tim-osterhus/mc-mod-lab`; its live capability proof has opened
the bounded Aura guide/recipe pilot. See the
[execution record](2026-09-22-staggered-execution.md) for actual validation.
Repository publication does not mean the plugin has been installed in Codex.

Recommendation: build a small, reusable verification toolkit, with Aura Cascade as
its first case study. Confirmed product name: **Minecraft Mod Lab**. Intended public
GitHub repository: **`tim-osterhus/mc-mod-lab`**. This project does not use Millrace.
The product is evidence-backed mod development and legacy porting,
not a replacement Minecraft engine, a new general agent orchestrator, or a wrapper
claiming credit for upstream MCP implementations.

All commits made for this work must use **Tim Osterhus <tim@millrace.ai>** as
author and committer. Set and verify repository-local Git identity in each
repository before committing; do not alter global Git configuration. Verify the
authenticated GitHub account is `tim-osterhus` separately before publication:
Git author configuration does not select the publishing account. These are
requirements. The account and both commit identity fields have now been verified
for the published toolkit checkpoints; the dirty Aura repo remains uncommitted.

## What changes from the earlier plan

1. Put a capability and safety proof before resuming broad mod fixes: connect to a
   disposable 1.21.1 world, capture a screenshot, inspect state, interact with a
   real screen, reset a fixture, and export results. Prove the chosen bridge on
   this exact Windows/Fabric/Minecraft combination rather than its README alone.
2. Keep video-first discovery. The three video reports provide approximately 58
   minutes of full-timeline caption analysis with sampled/focused visual evidence.
   Only unresolved, consequential questions need an original-client replay.
3. Replace informal parity claims with per-feature evidence: reference provenance,
   behavioral contract, implementation state, server/client verification, test
   case, artifact and explicit exceptions. Asset existence is not completion.
4. Evaluate Patchouli before writing a bespoke encyclopedia renderer. A framework
   is acceptable when it preserves player actions and presentation requirements;
   a silent downgrade of those requirements is not. Neither Patchouli nor a
   custom screen automatically supplies the missing content or mechanics.
5. Repair one complete gameplay slice at a time, with repeatable failure and
   success evidence. Start with book opening/navigation/readability and one
   pump-to-consumer circuit, then proceed through the progression ledger.

The conversation's confidence about one model being dramatically better than
another is not a measured result for this repository. The auditable improvement
is the verification loop, not a promise attached to a model name.

## Approved Aura crystal modernization

The user explicitly approved replacing the iron ingredient with an amethyst shard
while retaining eight gold nuggets. Use the supplied reference's two-crystal yield:

```text
G G G
G A G
G G G

G = minecraft:gold_nugget
A = minecraft:amethyst_shard
Output = 2 White Aura Crystals
```

This is a shaped crafting recipe. Replace the old base recipe rather than leaving
an alternate iron-based recipe enabled. Do not adopt the attachment's speculative
four-crystal yield, combined iron/amethyst variant, or broader recipe overhaul.
Leave colored-crystal recipes and crystal aura values unchanged by this decision.

The new Aura Encyclopedia must show the actual amethyst recipe and output count
and explain it entirely in-world. It must not mention an old recipe, replacement,
modernization, port history or iron as the previous ingredient. Optional flavor
can describe gold focusing the latent aura in amethyst into a neutral White Aura
Crystal. Keep the crafting display tied to the registered recipe where supported,
and audit related walkthrough text and links for consistency and readability.

Record the intentional legacy divergence in developer-facing parity records, not
in the immersive encyclopedia. Acceptance requires a recipe-data check, actual
survival crafting with exact ingredient consumption and output, rejection of the
former iron layout, and a readable/reachable matching encyclopedia entry. Assess
early amethyst acquisition during progression QA without silently changing yield
or adding alternate recipes. Recipe and guide implementation is now active;
acceptance remains open until the build and client checks pass. The requirement
belongs to the Aura repo, not generic Mod Lab defaults.

## Tool findings, checked September 22, 2026

| Component | What is established | Implication |
| --- | --- | --- |
| Minecraft Mod MCP | Its v0.3.0 GitHub release asset list includes `minecraft-mcp-1.21.1-fabric-v0.3.0.jar` and `minecraft-mcp-1.8.9-forge-v0.3.0.jar`; its bridge documentation describes screenshot/click/command tools | First candidate for the GUI-heavy modern/legacy workflow. Artifact availability is not a successful local runtime test |
| MCPFabric | README advertises structured world/player/inventory access and screenshots for 1.21.1, loopback/auth/capability controls, and Loader >=0.19.3 | Alternative if its structured capabilities are better for the required tests. Current repo uses Loader 0.19.1; do not install blindly or assume arbitrary guidebook GUI interaction is covered |
| Fabric testing | The version-specific 1.21.1 guide distinguishes unit and game tests but only documents unit-test setup there | Establish the exact pinned server GameTest API and runner. Do not transplant current-version client GameTest examples without checking availability |
| Patchouli | Modrinth lists Fabric artifact `Patchouli-1.21.1-93-FABRIC.jar`; official docs cover category/page formats and custom templates/components | Candidate mod dependency for encyclopedia implementation, not a mandatory runtime dependency of the generic plugin |
| Codex plugins | Official documentation supports skills plus MCP packaging; new portable packages use root `plugin.json`, `mcp.json` and `skills/`, with Codex compatibility manifests still supported | Package reusable workflows/tool wiring, but separately verify local installation and supported host surfaces |

Sources: [Minecraft Mod MCP release](https://github.com/langyo/minecraft-mod-mcp/releases/tag/v0.3.0),
[bridge guide](https://github.com/langyo/minecraft-mod-mcp/blob/v0.3.0/docs/guides/en/AI-TOOLS.md),
[MCPFabric](https://github.com/Etoryx/mcpfabric),
[Fabric 1.21.1 testing](https://docs.fabricmc.net/1.21.1/develop/automatic-testing),
[Patchouli 1.21.1 Fabric artifact](https://modrinth.com/mod/patchouli/version/1.21.1-93-fabric),
[Patchouli pages](https://vazkiimods.github.io/Patchouli/docs/patchouli-basics/page-types/),
[Patchouli components](https://vazkiimods.github.io/Patchouli/docs/patchouli-advanced/default-components/),
[OpenAI plugin packaging](https://developers.openai.com/plugins/build/plugins).
Release filenames above were confirmed through the public GitHub/Modrinth APIs;
no artifacts were installed or executed during this assessment.

## Keep three ownership boundaries

**Reusable plugin repository:** workflow skills, environment/capability checks,
version-pinned MCP wiring, serialized build/run supervision, fixture-run commands,
parity/report schemas, report validation, sanitized example evidence, documentation,
CI and tests for the toolkit itself. Start with one bridge. Add another only for a
demonstrated capability gap, not a speculative universal adapter framework.

**Aura Cascade repository:** gameplay code, recipes, guide content, mod-specific
GameTests, test-world fixtures, reference interpretations and the real parity
manifest. Share normalized reports with the plugin; do not put Aura-specific
mechanics into generic tooling.

**External dependencies:** original Minecraft/mod distributions, Patchouli, the
selected MCP bridge/mod, and optional video-analysis processing. Pin and attribute
them; do not copy private machine paths, secrets or downloaded videos into the
public plugin. A public video-analysis integration must accept portable evidence
bundles or user configuration, not depend on Tim's installed local runtime.

## Minimal useful release

- Environment doctor reports Minecraft/loader/JDK/bridge versions, capabilities,
  isolated world path, available memory and a clear unsupported state.
- A small parity manifest links each requirement to reference timestamps or
  source, deterministic tests, client checks and dated evidence. Verification
  dimensions remain independent; one green status cannot hide a failed client test.
- Skills guide reference collection, feature-slice implementation, world tests,
  independent visual QA and honest release reporting. Substitutions require a
  documented decision; skills cannot silently mark an approximation as parity.
- A runner creates/resets only disposable fixtures, executes bounded actions,
  captures before/after state plus screenshots/logs, and emits JSON/Markdown.
- One known-failing example and its corrected counterpart prove that the test
  system detects a real behavioral regression. Test the test, not just the mod.
- Lightweight CI validates schemas, packaging, scripts and report generation.
  Game/client checks are separately labeled and run only on compatible runners;
  screenshot capture failure is not treated as a visual pass.

Do not start with a dashboard, multiple loader backends, a new launcher/account
manager, an autonomous forever-loop, or a large inventory of unverified skills.

## Safety and memory contract

Require explicit instance identity (version, loader, process and world), not just
the first discoverable Minecraft port. Use local-only transport and authentication
where supported; inspect actual binding/auth before trusting a bridge. Treat
command execution as operator-level authority. Keep world mutation confined to
disposable QA saves and make teardown stop only owned processes.

Separate setup privileges from routine testing. No automatic public servers,
firewall changes, credential copying, global plugin hooks or destructive world
resets. Redact tokens/user paths from reports. Pin bridge and game-mod versions
instead of launching an unreviewed latest package through `npx -y` each time.

Build, original client and modern client run sequentially. Side-by-side demo
footage can be recorded sequentially and composed later. Continue measuring
working set/private allocation for the workload and the combined local process
group. Existing warm-build/client measurements do not guarantee a 4.5 GB total
desktop cap, cold-build peak or future bridge overhead. Exceeding a declared
budget should fail the run clearly, not continue silently.

The user subsequently clarified that the **4.5 GB budget covers Minecraft and
build/testing tools only**, excluding the already-running Codex/ChatGPT desktop
application. Enforce the workload budget with sampling and headroom; combined
desktop telemetry is informational, not a release gate. Report physical working
set and private allocation separately rather than describing both as physical RAM.

## Proposed milestones and parallel work

1. **Reference complete:** retain the three reports and [acceptance ledger](2026-09-22-video-acceptance.md).
2. **Thin toolkit and tooling proof:** after authorization, create the separate
   clean `mc-mod-lab` repository and validate one pinned bridge on the modern
   client; demonstrate screenshot, structured observation, actual GUI interaction,
   fixture reset, failure reporting and memory measurement. Test legacy compatibility
   separately when a specific unresolved contract requires it.
   In parallel, the parent can refine acceptance contracts and investigate book
   framework fit without resuming mod source changes. This gate needs one usable
   end-to-end test loop, not completion of the entire toolkit.
3. **Staggered parallel pilot:** after the tooling proof passes and the user
   resumes mod development, the parent owns Aura integration and the book/first
   circuit pilot while the toolkit owner extends only capabilities needed by
   those concrete cases. Consume tested, pinned toolkit checkpoints, not moving
   work-in-progress files. Keep code ownership separate between repositories.
4. **Plugin alpha:** package the proven workflows and a portable example. Validate
   installation in a fresh local session and exercise a second fixture outside
   Aura-specific assumptions; no hard-coded Tim paths or reliance on conversation
   history. A public alpha need not wait for completion of the full Aura port.
5. **Public release:** review names, license/notices, dependency pins, secret scans,
   security boundaries, reproducibility, setup instructions and clean-install results;
   publish only the reviewed toolkit and redistributable examples.

Proposed ownership: a GPT-6 Sol Xhigh subagent owns the toolkit path, with the
parent owning cross-project contracts, Aura integration and review. Delegate the
majority of bounded scripts/tests/documentation and routine implementation to
Luna Max where practical; use Sol High for difficult mod behavior and Astra low
for independent client QA. The toolkit owner must not independently edit the mod
or change its dependencies. Hand off each checkpoint with its version, supported
commands, test results and known limitations. Keep the public orchestration
policy configurable rather than requiring a specific account/model tier.

Parallel analysis and scoped code edits do not imply parallel heavyweight local
runs. Serialize Gradle, legacy/modern clients and memory-intensive media work
across both lanes. Account for local agent/tool overhead when checking the memory
budget. The user authorized execution after this proposal: Sol Xhigh owns the new
toolkit lane, Luna Max handles bounded work, and the parent owns the live tooling
proof before opening the gameplay implementation gate.

## Public portfolio and demo

The valuable original contribution is a reproducible path from observed legacy
behavior to failing test, corrected implementation and independently checked
evidence. Credit the original mod, MCP projects, Patchouli and other dependencies
visibly; do not present them as our inventions.

A strong first case study starts with the honest baseline: 82 tests passed while
the guide item failed to open. Then show the new test detecting that failure,
the observed legacy interaction, and the corrected port passing the same bounded
scenario. Until the repair is actually verified, that is a planned demo, not a
success story. Publish concise clips, the reproducible command, test output and
remaining limitations together. Reproducibility and useful tooling are stronger
portfolio evidence than an unsupported model-superiority or fully-autonomous claim.

Public GitHub source publication and submission to the public plugin directory
are separate steps. No repository creation, upload or directory submission has
been performed by this planning document.
