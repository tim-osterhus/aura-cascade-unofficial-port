# Native Hook Runtime Assertions

`NeoForgeHookIntegrationTest` is a JUnit loaded-registration/transformation guard.
Its success is **not** evidence that entity damage, item ticks, or death events pass.

`pixlepix.auracascade.qa.neoforge.NeoForgeHookRuntimeFixture` contains the actual
behavioral assertions. It has no JUnit/testframework dependency and can be called
by the packaged test-only `/aura_qa_hooks` command (permission level 4):

```java
NeoForgeHookRuntimeFixture.runAll(serverLevel, clearLoadedPosition);
```

Run synchronously on the server thread in an isolated disposable test world,
at a clear loaded position, on a tick where `level.getGameTime() % 100 != 0`.
The fixture seeds the world's RNG to establish the Thief reward precondition;
do not run it in a player's world. It unregisters its event listeners and discards
its entities in `finally`. It does not install a command or launch a server itself.
Only package the runtime fixture class into a QA helper, never the production JAR.
JUnit's ephemeral testframework server is not a supported world-interaction fixture.
No testframework dependency is needed for this helper-based route.

## Assertions

- Damage: two real `hurt` calls of 6 then 10. An ordinary villager finishes at
  10 health. A second villager with a native INVULNERABILITY reduction modifier
  returning zero finishes at 4 health, and the modifier executes exactly once.
- Red Hole: real ticks establish the 30,000 lifespan floor. `ItemExpireEvent`
  grants 100 ticks at age 30,000; the item survives with lifespan 30,100. A native
  NBT save/load preserves it; expiry without another extension removes the item.
  Ordinary cobblestone still expires at 6,000. No explosion outcome is injected.
- Thief: a real lethal player-sourced hit on a villager with a known trade invokes
  the production listener. HIGHEST priority establishes a winning random roll;
  LOWEST sees exactly one copied, three-diamond reward in the event collection
  and no earlier spawn. The uncanceled control spawns one reward; the canceled
  case spawns none. No reward or death event is synthesized by the fixture.

Record candidate SHA-256, NeoForge version, each returned assertion result, and
exceptions/logs. The Red Hole check is an NBT round trip, not a disk restart test.
Execution results are recorded in `docs/audits/2026-09-28-neoforge-1211.md`.
The inherited energy acceptance-count risk is outside these assertions.
