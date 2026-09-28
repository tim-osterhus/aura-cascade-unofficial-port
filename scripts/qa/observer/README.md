# Packaged Client Observer

This is a test-only client mod, not part of Aura Cascade's main source set or
release JAR. `qaObserverRemapJar` in `scripts/qa/audit.init.gradle` builds its
own intermediary-remapped JAR under `build/qa-audit/observer/`. The task is
opt-in and is not wired into `build`, `jar`, or `remapJar`.

For the packaged-client smoke, put only the remapped observer JAR, the reviewed
packaged Aura Cascade JAR, and its runtime dependencies in the production
Fabric instance's `mods/` directory. Launch the known fixture with Minecraft's
`--quickPlaySingleplayer` argument. Set the JVM property
`-Daura.qa.observer.dir=<absolute-output-directory>` to keep the result outside
the instance. Do not load the Mojmap `-dev.jar` observer artifact.

After 40 consecutive in-world HUD frames with a local player and integrated
server, the observer captures `screenshot.png` from the main render target on
the render thread, writes `manifest.json`, then calls normal `Minecraft.stop()`
on the next client tick. After 2,400 client ticks or three minutes, it attempts
a screenshot, writes a failure manifest, and stops instead. It has no network,
input, command, or world-mutation interface in this default mode.

On this branch the launcher targets Minecraft 1.21.11. Pass `-Interactive` with
`-Execute` to set `aura.qa.observer.keepOpen=true`: the observer still writes its
one-shot result, but leaves the client open for manual QA. Close the client
normally afterward. The launcher still verifies the report and artifact hash;
remaining open is not a visual acceptance result.

## Opt-In Interaction Fixtures

The isolated launcher also accepts `-ClientProbe`, `-FeedbackProbe`, and
`-RenderProbe` with `-Execute`. These separate drivers mutate the disposable
fixture and may supply items, blocks, player position or game mode. They never
belong in the release JAR or a user's ordinary world. Each writes a fresh
`manifest.json` beneath its named output directory; the launcher requires
completion and successful state assertions. The observer keeps the game open
until requested drivers finish, then stops normally.

`-ClientProbe` clicks real screen widgets for Creative tab retention and all
nine encyclopedia landing links at GUI scales 2 and 3. Screen callbacks in this
Minecraft version extract rendering state before GUI drawing completes, so the
driver captures the framebuffer on the following client tick. Successful state
assertions and nonempty PNG files are not substitutes for independent visual
inspection, animation checks, or survival progression.

The client probe also captures the real Bookshelf Coordinator screen at GUI
scale 2 with seeded empty and populated snapshots, clicking an item to inspect
selection text. This checks rendering only: result labels and power are supplied
fixture state, not proof of a network retrieval. The dedicated-server
`-TargetStorage` probe separately exercises production storage APIs.

`manifest.json` reports the runtime mapping namespace, world/player state,
Aura's Fabric mod-origin JAR path, SHA-256 and byte count, PNG dimensions and
size, and `success`/`failure`. `success: true` requires an intermediary runtime,
a regular packaged Aura JAR, a loaded singleplayer world, and a nonempty PNG.
The parent QA pass must compare `auraSha256` to the reviewed release candidate,
inspect screenshot pixels for a nonblank correctly loaded scene, and check a
normal process exit. A dev client or directory-loaded mod reports failure and
does not count as packaged proof.
