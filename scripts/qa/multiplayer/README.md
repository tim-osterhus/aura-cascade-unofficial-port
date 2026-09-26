# Private Packaged Multiplayer Fixture

**Packaged multiplayer state/ownership/rejoin PASS:** parent run
`20260923-093241-443` passed all 15 checks, with 3032.2 MiB peak private allocation
and 2512.7 MiB working set; all four process instances exited 0. See the
[audit report](../../../docs/audits/2026-09-23-packaged-multiplayer-playtest.md)
for exact artifact hashes, earlier failures and scope. Owner fairy pixels were
confirmed by the parent; witness views are occluded, so both-client visual
acceptance remains open. This separate private QA mod is not a release artifact.

## Small Fixture, Real Gameplay Paths

One separate Fabric test mod has a client driver and a read-only dedicated-server
probe. A single PowerShell runner coordinates them using local JSON files. There
is no custom game protocol, server item grant hook, equipment setter, or native OS
input. Production classes are compiled against, never copied into the test JAR.

1. Start a fresh dedicated server bound to `127.0.0.1`, offline mode, no RCON/query.
2. Start `AuraOwnerQA`, then `AuraWitnessQA`, with distinct offline UUIDs and fresh
   separate game directories. Both stay connected concurrently for sync checks.
3. Explicitly labelled server-stdin commands create a small floor, teleport the
   players, set survival, and provide exactly one real ring and one real charm.
   The actual commands are recorded in `stdin-fixture.json`.
4. The owner sends production `AuraAccessoryNetworking.OpenRequest`, then uses
   vanilla `handleInventoryMouseClick(..., QUICK_MOVE, ...)` on the real ring's
   menu slot. It must arrive in physical accessory slot 1 on server and owner.
5. The owner selects hotbar slot 1 and uses the charm through the normal client
   game mode. The server must consume it and bind one fairy to the equipped ring.
6. Over six seconds of consecutive samples, require one identical fairy UUID,
   owner UUID and fairy slot on server and both clients, one total owner ring,
   zero remaining charms, and a real owner attachment on the owner client. The
   witness's remote-owner attachment must be **absent**, not merely an empty list.
7. Capture each client's actual framebuffer after 40 rendered HUD frames. Stop
   the owner normally. Require owner and fairy absent on server and witness.
8. Relaunch the same owner identity/game directory. **Do not open any menu or send
   equip/use commands.** Require the persisted bound ring to synchronize, and one
   fairy to appear to both clients again. Capture both views and stop normally.

The probe checks `AttachmentTarget.hasAttached` using one read-only reflection
lookup of Aura's own private `ATTACHMENT` key. All inventory/equipment reads use
existing APIs. It never initializes or writes an attachment. Observations scan
each tick (server overworld/client loaded entities), retaining maximum fairy count
to catch observed duplicate lifecycles; JSON is written once per second. The
empty fresh world and one bound charm make a count above one an explicit failure.

This covers disconnect/rejoin persistence with the dedicated server kept alive,
not server restart, death, dimension change, malicious-client security, or packet
sniffing. Framebuffer files plus entity state establish a reviewable visual
fixture; a human must confirm fairy pixels in both images before visual acceptance.
No screenshot content is automatically labelled a visual PASS.

## D3 Transformed Entity Probe

`ConsumerLifetimeProbe` is an independent, opt-in common initializer in this
private test JAR. It does not enable the multiplayer driver and needs no players
or clients on a dedicated server. It runs once at `SERVER_STARTED`, on that
server's thread, using detached real `ItemEntity` instances and real stone stacks.
It does not spawn entities, mutate world blocks, stop the server, or use a bridge.
Without the enable property it does nothing.

The normal JUnit `ConsumerItemKeepAliveTest` retains deterministic cadence,
bounds, lifetime precedence and source tick-wiring coverage. That unit bootstrap
does not load Aura mixins. Three previously mislabelled transformed-entity cases
are relocated here, not replaced by fake entity implementations:

- `ageAndPersistence`: real age reset, protection NBT, full-width age 70,000
  round-trip/resave, ordinary untagged restoration, lifetime precedence.
- `merge`: invoke vanilla's real static merge, retain item count/discard behavior,
  inherit protection and preserve the vanilla minimum age of -6,000.
- `oldMergeEligibility`: vanilla age 6,000 refuses merging; extending protection
  without resetting age makes the same entity eligible.

1. Parent builds `qaMultiplayerRemapJar` in its reserved slot and installs this
   separate test JAR alongside the candidate in a private packaged server. Do not
   distribute/nest this JAR in Aura. Existing sourceSet includes the new file.
2. Add these JVM properties **before** `-jar` in the parent's existing scheduled
   packaged-server command (replace both placeholders):

   ```text
   -Daura.qa.consumerLifetimeProbe=true
   -Daura.qa.consumerLifetimeProbe.output=<fresh-absolute-path>/consumer-lifetime-probe.json
   -Daura.qa.consumerLifetimeProbe.sha256=<candidate-Aura-JAR-SHA256>
   ```

   Obtain the hash with `Get-FileHash -Algorithm SHA256 -LiteralPath <candidate>`.
   No multiplayer output property or two-client run is necessary for this gate.
3. After normal server startup, inspect the fresh JSON. Require `success:true`,
   `runtimeNamespace:intermediary`, `itemEntityTransformed:true`, matching
   `auraOrigin`/`auraSha256`, and all three named checks equal to `PASS`. Missing
   output, startup failure, wrong candidate, or any failed check is a failed gate.
   The probe refuses an existing report path, avoiding stale result acceptance.
4. Stop via the parent's normal server-stdin `stop`. Keep the parent's existing
   startup timeout/memory guard; this probe launches no extra JVM and has no loops
   waiting for game events beyond the single server-start callback.

The D3 runtime probe **passed** in parent runs `20260923-091306-403` and
`20260923-093241-443`, with intermediary namespace, transformed ItemEntity and
matching candidate hashes. All three named cases passed; the latter is recorded
in the linked multiplayer audit. NBT calls on detached entities test production
serialization hooks, not disk world-reload persistence.
Keep the parent's `consumer-lifetime.ps1` real 500-tick cadence/despawn controls
and actual reload gate. A green JUnit run alone cannot close this D3 runtime gate.

## Parent Build Integration

After review, add the following inside the existing audit init script's Loom
plugin block (or an equivalent temporary parent-owned init script). This fixture
does **not** edit that script itself. Use the same Loom task class loader already
used by `qaObserverRemapJar`.

```groovy
def multiplayerSources = sourceSets.create('qaMultiplayer') {
    java.srcDir 'scripts/qa/multiplayer/src/main/java'
    resources.srcDir 'scripts/qa/multiplayer/src/main/resources'
    compileClasspath += sourceSets.main.compileClasspath + sourceSets.main.output
}
def multiplayerJar = tasks.register('qaMultiplayerJar', Jar) {
    dependsOn tasks.named(multiplayerSources.classesTaskName)
    from multiplayerSources.output
    archiveBaseName = 'aura-qa-multiplayer'
    archiveVersion = '1.0.0'
    archiveClassifier = 'dev'
    destinationDirectory = layout.buildDirectory.dir('qa-audit/multiplayer-tool')
}
tasks.register('qaMultiplayerRemapJar', remapTaskType) {
    dependsOn multiplayerJar
    inputFile.set(multiplayerJar.flatMap { it.archiveFile })
    classpath.from(multiplayerSources.compileClasspath)
    sourceNamespace.set('named')
    targetNamespace.set('intermediary')
    addNestedDependencies.set(false)
    useMixinAP.set(false)
    archiveBaseName = 'aura-qa-multiplayer'
    archiveVersion = '1.0.0'
    archiveClassifier = ''
    destinationDirectory = layout.buildDirectory.dir('qa-audit/multiplayer-tool')
}
```

`remapTaskType` is the existing variable resolved from Loom's own classloader in
`scripts/qa/audit.init.gradle`. Build `qaMultiplayerRemapJar` only in the parent's
reserved build slot; never while gameplay clients are running. Verify the result
contains only `pixlepix/auracascade/qa/multiplayer/*` plus its own metadata and does
not appear in the main mod's nested dependencies or release artifacts. No test
mod mixin registration or production entrypoint is required.

## Execution Prerequisites

- Parent reserves the exclusive heavy slot after all single-client gates. The
  runner refuses to start if any `java`/`javaw` process exists, including Gradle.
- Windows x64, PowerShell 7, existing Java 21, built remapped candidate Aura JAR,
  and the separately remapped test JAR.
- An existing successful packaged launch's `launch-manifest.json` **and sibling
  `java.args`**, generated by `scripts/qa/launch-packaged-client.ps1`. The template
  is read only. Only classpath, staged natives/assets, profile version and asset
  index are reused, never its old observer or singleplayer game directory.
- All template libraries/assets/natives are staged inside this repository. No
  global `.minecraft` fallback, dependency download, token discovery or real
  account authentication is performed by this script.
- `build/qa-audit/packaged-server` is already installed and has an already
  accepted `eula.txt`. The runner copies launcher/server JARs, libraries/versions,
  and that EULA into a **new** server directory, not any old world/player data.
- Fabric API and Patchouli JARs are available in the template game's `mods`.
  Optional separate Energy JAR is copied if present; normally Aura nests it.
  Other mods (including old QA observers/bridges) are excluded.

Example commands, for the parent to execute only after scheduling:

```powershell
$parameters = @{
    JavaHome = $env:JAVA_HOME
    ClientLaunchManifest = Join-Path $PWD 'build/qa-audit/packaged-client-runs/<validated-run>/launch-manifest.json'
    AuraJar = Join-Path $PWD 'build/libs/<candidate-remapped-Aura.jar>'
    ProbeJar = Join-Path $PWD 'build/qa-audit/multiplayer-tool/aura-qa-multiplayer-1.0.0.jar'
}
# Preview does not create files or launch Java.
& .\scripts\qa\multiplayer\run.ps1 @parameters
# In the exclusive execution slot only:
& .\scripts\qa\multiplayer\run.ps1 @parameters -Execute
```

Output is isolated under `build/qa-audit/multiplayer/<timestamp>`. Nothing is
deleted or overwritten from prior runs; owner rejoin intentionally reuses just
this run's owner game directory. Both usernames are fixed fixture identities;
`--accessToken 0` is a dummy value, not a credential. The server's offline mode
must never be exposed outside loopback. A free-port check precedes startup.

## Bounds And Results

Memory-tuned profile: server and each client have a 512 MiB maximum heap,
128 MiB initial heap, Serial GC and `ActiveProcessorCount=2`. The processor limit
reduces processor-sized worker pools without setting a guessed thread-stack or
metaspace limit. Each client stays at 854x480 and 20 FPS, now with render distance
2. Server view/simulation distances are both 2. Client `simulationDistance` is 5,
its valid minimum; the remote dedicated server controls world simulation. All
three retain the 128 MiB direct-memory limit and 96 MiB code cache. Metaspace is
not capped without measurements. Launches remain staggered; no additional
JVM/helper/compiler is started during gameplay.

### First Run And Tuning Boundary

`build/qa-audit/multiplayer/20260923-091306-403/summary.json` records a failed
multiplayer attempt: the 3800 MiB soft guard stopped the run before equipment
testing. All three JVMs exited normally (code 0); no accessory synchronization
outcome was established. The independent D3 detached-entity probe passed, which
is not a multiplayer PASS.

Its `memory.csv` peaks at 3936.0 MiB aggregate private bytes and 2787.4 MiB working
set at `2026-09-23T19:14:17.7552890Z`. The preceding sample was 3747.5 MiB private
bytes, illustrating inter-sample overshoot. All three stderr files are empty;
stdout contains no observed heap/metaspace exhaustion. Both clients explicitly
rejected the old `simulationDistance:3` as outside `[5:17]`, so setting client
simulation distance to 2 would not be a valid reduction either. The trace has no
per-process heap/native breakdown; private bytes must not be called heap usage.

The revised profile reduces combined maximum heap from 2176 to 1536 MiB (640 MiB
less), chunk/render demand and processor-sized pools. It met the **below 3500 MiB
measured aggregate** target in the second and third runs; the successful third
run peaked at 3032.2 MiB private allocation, including the runner. This is bounded
evidence for this fixture, not a general memory guarantee: heap caps do not bound
all native allocation. A heap OOM, sustained GC/timeout or another memory-guard stop is a failed
fixture, not permission to raise the guards. Retain the next run's logs/trace for
further diagnosis rather than blindly capping metaspace or weakening validation.

The runner samples aggregate working set **and** private bytes of all its active
JVMs plus itself every 250 ms. Above 3800 MiB it aborts the fixture and requests normal
stops; at 4000 MiB it force-stops only its owned process trees. The user budget is
decimal **4.5 GB, approximately 4291 MiB**, not 4.5 GiB. The forced guard leaves
approximately 291 MiB of headroom. This is a measured guard, **not an OS-enforced
hard memory reservation**; overshoot between samples remains a residual risk.
If the parent requires an absolute hard cap, do not run this fixture without its
own process-container limit. No Win32/native-control mechanism is introduced.

Phase timeout: 150 seconds. Total runner/probe deadline: 600 seconds. Normal
cleanup has 25 seconds before forced termination marks the result failed. Every
probe requires runtime namespace `intermediary` and hashes the actual loaded
Aura origin JAR against the candidate; a dev classpath/loose source cannot pass.

Inspect `summary.json`, `memory.csv`, labelled stdin commands, per-phase JSON,
per-process `observations.jsonl`, logs and PNGs. A nonzero exit, timeout, missing
sync, unwanted remote attachment, missing/duplicate fairy or forced shutdown is
a failure. `success:true` reflects the executed state checks; screenshot-file
existence alone does not certify visible fairy pixels. Preserve the third run's
bounded state PASS and its unresolved witness-image limitation separately.
