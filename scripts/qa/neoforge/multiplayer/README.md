# Native NeoForge Multiplayer QA Helper Contract

This is a separate, test-only NeoForge mod (`aura_qa_multiplayer`), not part of
the Aura release JAR. It adapts the Fabric multiplayer action/snapshot probe to
the native NeoForge 1.21.1 APIs. No consumer-lifetime probe is included; the
independent native five-hook fixture covers that scope elsewhere. This source
alone is **not** a multiplayer PASS. Candidate-specific executions and results
are recorded in the release gate, including retained failed trials.

## Parent-Owned Bootstrap

The parent owns `build.gradle`, launcher, three-game-JVM coordination and the
global 4 MC process cap. The existing `qaMultiplayerJar` task uses a separate
source set alongside `qaObserver`, following this pattern:

```groovy
sourceSets {
    qaMultiplayer {
        java.srcDirs = ['scripts/qa/neoforge/multiplayer/src/main/java']
        resources.srcDirs = ['scripts/qa/neoforge/multiplayer/src/main/resources']
        compileClasspath += sourceSets.main.output + sourceSets.main.compileClasspath
        runtimeClasspath += sourceSets.main.output + sourceSets.main.runtimeClasspath
    }
}
tasks.register('qaMultiplayerJar', Jar) {
    archiveBaseName = 'aura-neoforge-qa-multiplayer'
    archiveVersion = '1.0.0'
    destinationDirectory = layout.buildDirectory.dir('qa-tools')
    from sourceSets.qaMultiplayer.output
}
```

Stage that JAR alongside the *packaged* Aura candidate on each client and the
dedicated server. Do not nest it in Aura or publish it. The mod remains idle if
`aura.qa.multiplayer.output` is absent. Each JVM needs distinct absolute output
directories, and both JVM properties must precede the game main class:

```text
-Daura.qa.multiplayer.output=<fresh-absolute-output-directory>
-Daura.qa.multiplayer.sha256=<packaged-Aura-JAR-SHA256>
```

On first tick, the probe obtains Aura's loaded mod file through NeoForge
`ModList`, requires a regular `.jar`, hashes those exact bytes, and refuses a
hash mismatch. Reports include `runtimeNamespace: "neoforge"`, `auraOrigin`,
`auraSha256`, `auraJarBytes`, and `auraJarProof: "verified"`. The namespace value
names this native probe; the packaged-origin/hash check is the actual proof.
`failure.json` or missing snapshots fail the phase.

## Actions And State

The parent writes each client's `command.json` atomically with an increasing
integer `sequence` and an `action`. `acknowledged` means the client sent the
action, **not** that the server accepted its gameplay result. Verify subsequent
server and client snapshots in `observations.jsonl`/`latest.json`.

| Action | Fields | Production path |
| --- | --- | --- |
| `open` | none | Aura `OpenRequest` packet |
| `equip`, `reequip` | none | `QUICK_MOVE` from real accessory-menu inventory ring slot |
| `unequip` | none | `QUICK_MOVE` from occupied accessory ring slot |
| `bind` | none | selected hotbar slot 1, vanilla carried-item packet, `gameMode.useItem` with real charm |
| `capture` | none | framebuffer PNG after 40 rendered frames; either client |
| `respawn` | none | vanilla `PERFORM_RESPAWN` packet, only while dead |
| `bookshelfOpen` | integer `x`, `y`, `z` | `gameMode.useItemOn` on a real coordinator within reach, empty selected hand |
| `bookshelfRetrieve` | integer `entryIndex`, positive `count` | Aura `ExtractRequest` for an entry from the actual open menu snapshot |

Only `AuraOwnerQA` may issue gameplay actions. The witness may issue `capture`.
For `bookshelfOpen`, prepare a valid coordinator and use an empty hotbar slot; the
probe does not place blocks or grant items. For retrieval, require a real open
menu, connected storage shelf, complete network, sufficient aura power, matching
stored item, and inventory capacity. The helper does not force any of these
outcomes. Compare `bookshelf.resultCode/resultAmount`, inventory counts, and
entry counts on both sides after the server handles the request.

Snapshots preserve the Fabric 15-phase protocol fields, including per-player
`accessoryAttached`, physical `slots`, bound counts, inventory ring/charm totals,
fairy UUID/owner/slot/role and `maxFairiesObserved`. `accessoryAttached` uses the
registered native `aura:accessories` attachment key with `Player.hasData`:
the witness's remote owner must show **absent**, not merely an empty loadout.
The probe never creates or writes that attachment. Server snapshots include
all loaded dimensions, including read-only `droppedItems` and `ringsDropped` for
death conservation; client snapshots include loaded/rendered entities.
Snapshots are sampled every tick and published every 20 ticks. If the fairy
peak exceeds one, `peakFairySnapshot` retains its exact state even if the
overlap resolves before the next periodic publication.

Additional per-player observations are `alive`, `health`, `dimension`, and,
on the dedicated server, `keepInventory`; `inventory` lists each nonempty slot
for retrieval accounting. A real owner death may be induced by
a clearly labelled server-stdin `/kill` fixture command, followed by client
`respawn`; set and record `gamerule keepInventory` before each case. Check the
post-respawn ring/attachment/fairy state on server and owner, plus remote-owner
absence on witness. For a dimension sync control, a labelled vanilla server
teleport command can move the owner to a prepared destination; this proves the
dimension-change sync path, not portal gameplay. Do not infer successful state
from command acknowledgement alone.

For disconnect/rejoin, stop the owner client normally and relaunch the same
offline identity and game directory while keeping the dedicated server alive.
Clear old `command.json` before relaunch; send no `open`, `equip`, or `bind`
action. Require the saved bound ring and a single matching fairy to return on
server and both clients, while witness remote-owner attachment stays absent.
`stop.request` terminates its own JVM; the probe has a 600-second deadline.

The parent runner alone owns process launch, memory guard, timeout, fixtures,
assertions, screenshots review and audit disposition. A screenshot file is not
proof of visible fairy pixels; inspect the image before a visual PASS.

## Runner Contract

`scripts/qa/neoforge/run-multiplayer.ps1` retains the original owner/witness
join, bind, privacy, logout and no-action rejoin checks. It then exercises real
menu `unequip`/`reequip`, a labelled command-driven Nether roundtrip, and
`keepInventory` true/false deaths with a client respawn packet. The false case
requires exactly one bound ring in the dropped item entity, then natural pickup
and real menu re-equipping, with the total ring count always one. Fixture stdin
commands are retained in `stdin-fixture.json` (initial seed) and
`stdin-fixture.jsonl` (all labelled commands).

The runner opens a real, unpowered Bookshelf Coordinator menu as a bounded UI
control. It does **not** send `bookshelfRetrieve` without a valid entry, connected
storage, and power; the summary explicitly records that packet as not sent.
The helper and runner both retain a 600-second deadline. The runner checks the
global four-game-JVM cap before every launch, while the parent owns slot
reservation and the aggregate memory guard. After the read-only dropped-ring
observation was added, an older helper JAR is rejected; rebuild the separate
`qaMultiplayerJar` before executing this runner.
