# 0.2.1 Integration Validation

Candidate: Aura Cascade Reimagined `0.2.1+1.21.1`.
Final SHA-256: `2290a1a344fa8e1e43d631729b5f88d422923d573db90d1a592945fecc1aacc8`.
Mod and registry IDs remain `aura`; no save migration is introduced.

## Completed Checks

- Final packaged run `20260923-182030-717`: exact final SHA-256 above,
  intermediary namespace, loaded singleplayer world, 40 stable rendered frames,
  guide font audit v3 PASS (41 entries/371 pages plus landing), normal exit 0.
  The final screenshot was opened and inspected.
- Independent live five-target pass completed without an observed product
  failure. Final node tests include clear/blocked/occluded previews, expiration,
  reload, pump/pedestal previews, next-cycle new-neighbor transfer, obstruction
  between plan/apply, inserted nearer node, and removed receiver. UI/effect
  results carry forward from the unchanged earlier candidate; see the QA record
  for sample cadence, seeded fixtures and limits.
- `hotfix-021-links-build`: final `test build qaObserverRemapJar auditClientLaunch`
  succeeded. 252 tests in 62 suites, zero failures and errors. This adds fresh
  connection validation before natural transfer and Orange burst application,
  preventing a newly placed preview from disagreeing with a cached transfer path.
- Earlier UI/effect live evidence below belongs to candidate
  `7de54b17c9ba6ce5ac61e0a6dc7d4a03444a071377bc69fb8a508801d202abd4`.
  The UI/effect classes and resources did not change in the final connection fix.
- `hotfix-021-final-build`: `test build qaObserverRemapJar auditClientLaunch`
  succeeded. 250 tests in 61 suites, zero failures and errors.
- The later QA-only landing-layout observer change compiled successfully in
  `hotfix-021-observer-build`; the product JAR hash was unchanged.
- Earlier packaged run `20260923-175213-866`: intermediary namespace, earlier candidate
  hash, singleplayer world, 40 stable rendered frames, normal exit code 0.
  Its screenshot was opened and inspected, not merely checked for existence.
- Guide actual-font audit v3: 41 entries, 371 pages, no failures. Opening-page
  text occupies x=15..123 (right boundary 131), y=43..151 (footer boundary 155).
  Nine direct links occupy separate rows; runtime clicking is a separate check.
- Publisher unit tests: 10 passing. No credential read or upload during tests.
- Final JAR inventory: 1,595,291 bytes, 1,444 entries, no private QA/bridge/token paths; only
  `META-INF/jars/energy-4.1.0.jar` is bundled.
- Public browser verification: renamed project is
  `https://www.curseforge.com/minecraft/mc-mods/aura-cascade-reimagined`,
  project ID 1519395. The existing 0.2.0 file remains on this same project.

## Memory

Serialized workloads, MiB. Desktop/Codex and unrelated apps excluded as requested.
Sampling is not a hard OS memory limit.

| Workload | Peak working set | Peak private allocation |
| --- | ---: | ---: |
| First build | 1532.5 | 1661.3 |
| Earlier product build | 1360.6 | 1490.8 |
| Final connection-fix build | 1422.3 | 1543.5 |
| Observer-only build | 975.5 | 1145.7 |
| Earlier packaged client + monitor | 1260.5 | 1798.9 |
| UI/effect live client + monitor | 1352.9 | 1872.3 |
| Final node live client + monitor | 1418.3 | 1742.7 |
| Final packaged client + monitor | 1248.5 | 1788.2 |

## Harness Recovery

The first development-client launch lacked the local bridge token environment
variable. The bridge refused to start, as designed. No world or Creative
inventory was opened. Native capture also failed with the host's capture API.
The verified QA-only title-screen process was terminated; its monitor recorded
exit -1. This launch is not acceptance evidence. The client was relaunched
with the existing token passed privately through its environment; no token
was printed, copied into the release, or sent outside loopback.

## Release Gate

See [independent visual QA](2026-09-23-hotfix-visual-qa.md) for live results.
The bounded hotfix gate passed and the final manifest is reviewed. This does not
claim exhaustive survival/modpack coverage, every GUI scale, or absence of every
possible single-frame flicker. Publication is recorded separately in
[the release record](../releases/0.2.1-publication.md).
