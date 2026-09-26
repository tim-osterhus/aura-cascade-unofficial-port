# Full-Port Beta Publication Gate

The user explicitly requested publication of a new beta on the existing
CurseForge project after the full mod port is finished. This is authorization
for that final publication, not for uploading intermediate pilot builds.

## Existing Target

- Project ID from the existing local configuration: `1519395`.
- Slug: `aura-cascade-unofficial-port`.
- Configured name: Aura Cascade Unofficial Port.
- Minecraft `1.21.1`, Fabric, Java `21`; release type **beta**.
- Reuse existing credentials only at publication time. Never print, copy into
  source, commit, or expose them in command-line arguments or reports.

Verify the actual remote project identity before uploading. Do not create a
second project. Git commits, if made, use Tim Osterhus <tim@millrace.ai> for both
author and committer; preserve the existing dirty Aura worktree.

## Required Before Upload

1. Close the full-port acceptance ledger, not just the guide/recipe pilot.
   Record any deliberate, user-approved differences explicitly. Unimplemented
   original gameplay or major usability gaps are not a finished port.
2. Pass the complete deterministic suite, dedicated-server smoke, and independent
   client progression, persistence, navigation, and representative gameplay QA.
   Check a packaged mod instance as well as the development runtime.
3. Stay within the scoped 4.5 GB Minecraft/build-tool budget with serialized
   builds and clients. Report measured working set and private allocation,
   including sampling limitations.
4. Select a new version after checking existing published files. Prepare accurate
   beta notes, current attribution, supported versions and required dependencies
   including Patchouli. Confirm the packaged JAR matches the reviewed source.
5. Verify the release artifact's contents, size and SHA-256 after the final build.
   Check that no testing bridge, credentials, local paths, worlds or unrelated
   generated files entered the JAR or source publication.
6. Validate the current official CurseForge upload flow and perform a metadata
   dry run before the single authorized beta upload. Verify the upload response,
   file ID, release type, project, game/loader versions and publication status;
   report the actual result rather than assuming an accepted upload is live.

## Current Safety State

### Final Pre-Upload Acceptance (2026-09-23)

The sixteenth candidate closes the finite gameplay gates, including the final
Red Hole repeated-blast/frozen-time controls and held/dropped crystal accounting.
Independent review found no remaining identified B1-B5 gameplay blocker. The
full suite passes 244 tests in 60 suites. Final packaged run
`20260923-121601-864` passes actual-font layout and rendering; multiplayer run
`20260923-121724-053` passes all 15 checks and the three transformed-item lifetime
checks. Both verify SHA-256
`cff6b2cdf435dc8c6318967c0e860d86e5990061af866628b4462317d7ba4a63`.

Artifact contents, dependency relations, attribution, scoped memory, current
public project identity and upload documentation have been reviewed. The
explicit beta manifest passes offline validation and all ten publisher tests
pass. The agreed beta gate is satisfied, subject to the documented coverage
limits; this is not exhaustive compatibility certification. Actual upload/file
status is tracked in [the publication record](../releases/0.2.0-publication.md).
The upload was accepted as file `8959308` after adding the API-required Client
and Server environment labels. Two public-browser checks still returned 404;
public availability and moderation approval remain unconfirmed.
The earlier sections below retain historical preparation states.

### Public Preflight (2026-09-23)

The [public file listing](https://www.curseforge.com/minecraft/mc-mods/aura-cascade-unofficial-port/files/all)
confirms project ID `1519395`, owner `timinator117`, and the existing
`0.1.1+1.21.1` beta. Three files are listed, including two older 1.21.11 files.
Do not replace or relabel those files, and do not publish this 1.21.1 build as
1.21.11. Recheck the listing when selecting the final new version.

The [official upload documentation](https://support.curseforge.com/support/solutions/articles/9000197321-curseforge-upload-api)
confirms multipart `metadata` and `file`, header-based authentication, beta
release type, and required-dependency relations. The current local publisher
does not include relations and still depends on a historical Millrace verdict;
it must not be run unchanged for this release. Final metadata must declare
Fabric API and Patchouli as required dependencies. An upload response ID proves
acceptance by the API, not public availability or moderation approval.

This check used public pages only. No credentials were read and no upload or
remote project change was attempted.

### Candidate Preparation

A second public file-list check still showed the same three releases. The next
candidate version is `0.2.0+1.21.1`; changing the source version does not mark it
release-ready. The original `Copyright (c) 2015 Adly Templeton` MIT notice is now
retained alongside the existing notices, with texture credits traced to the
original CurseForge page.

`scripts/publish_beta.py` is the replacement explicit-manifest publisher. Its
default path validates offline, without reading credentials or making requests.
It checks project 1519395, beta type, exact Minecraft/Fabric version names,
Fabric API/Patchouli dependency relations, source and actual JAR metadata, and
the reviewed JAR hash. Upload requires `--upload` and a positive response file
ID. All nine focused Python tests passed independently in the parent process.
No real manifest has yet been approved and no credentials were accessed.

`curseforge_publish.json` now selects `beta` and has `enabled: false`. No active
legacy publisher process was found when applying this gate. The old script
depends on a Millrace completion verdict; that stale verdict must not authorize
this new release. Re-enable or adapt the publisher only after this gate is met.
No credentials were opened and no upload was attempted when recording this plan.
