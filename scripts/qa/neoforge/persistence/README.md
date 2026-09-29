# NeoForge Persistence QA Fixture

This test-only mod adds server commands under `/auraqa persistence`. Run it only
in the disposable NeoForge QA server. It requires permission level 4 and the
regular packaged Aura JAR; it deliberately refuses a classes directory or an
unverified Aura load.

## JVM Contract

Pass this property to each server process:

```text
-Daura.qa.persistence.output=<absolute evidence directory outside the world>
```

Reuse the same evidence directory and world for preparation and check. The
server rejects an evidence directory inside the active world. The helper reads
the loaded Aura mod path through NeoForge `ModList`, hashes that regular JAR,
and records its SHA-256 in every report. It does not pin a candidate hash; the
restart check requires the loaded hash to match the preparation report, so a
rebuilt beta is automatically bound to the artifact actually loaded by both
processes.

The process start record contains the JVM PID and a fresh UUID marker. The disk
check requires both to differ from preparation. Stop the first server normally
and start a new server JVM before checking.

## Commands

`/auraqa persistence white-full <processor-x> <processor-y> <processor-z>`
starts an asynchronous full-cycle test, bounded to 2,500 server ticks. The
coordinates identify the processor block. It requires a clear disposable
fixture area with no nearby item entities or players. It places fresh real Aura
blocks with zero aura, power, progress, links, pump fuel, and output; verifies
40 unpowered ticks retain one iron ingot and one white wool; then supplies
eight dropped White crystals and one coal. The production node ticker absorbs
the crystals, the burning pump performs a five-block lift, and falling White
aura powers the processor. The result must be exactly one White Arcane Ingot.
This is a command-supplied machine progression, not survival acquisition.

`/auraqa persistence prepare <node-x> <node-y> <node-z>` creates the separate
disk-restart precondition fixture asynchronously. A real isolated Aura node
absorbs three dropped White crystals. A real storage bookshelf receives a
named MOD storage book through the production block-entity helpers; the book
stores two White crystals and three White Arcane Ingots. A coordinator is
placed beside the shelf, and its live network/browser view is captured. The
result is `PREPARED`, not a persistence pass.

After normal server shutdown and a new process start on the same world and Aura
JAR, run `/auraqa persistence check`. This is read-only with respect to fixture
state: it loads the recorded chunks and compares the node aura/link state,
book variant/name/entry counts, and coordinator shelf/browser view against the
external expectation JSON. It never repairs or reconstructs the expected
world state.

## Evidence

The output directory receives `white-full-result.json`,
`persistence-expectation.json`, and `persistence-check-result.json`. Each run
replaces its prior status before work begins; failures leave `FAIL` JSON and a
completion/error record in the server log. `PREPARED` records only legitimate
pre-restart setup; only a successful `check` records `persistenceVerified` as
true.
