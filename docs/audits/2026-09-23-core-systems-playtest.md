# Core Systems Live Audit

## Runtime

The disposable `core-systems-cac43cda` fixture runs the validated 119-test
development snapshot from `core-storage-accessory-validated`, not later source
edits. Minecraft 1.21.1, Fabric Loader 0.19.1, Java 21.0.12.1, authenticated
loopback lab6 bridge. The Minecraft/build workload is serialized under the
memory wrapper. Normal Save and Quit followed by Quit Game ended with exit 0.
Sampled workload peaks: 1,315.5 MiB working set, 1,842.8 MiB private allocation;
no memory guard trip. One-second samples are not an OS hard cap. Desktop/agent
host allocation is outside the user's explicitly selected workload scope.

All coordinates, inventories and materials below are seeded test fixtures.
They do not establish natural survival acquisition or full progression.

## C1: Fueled Processor Loop

- Processor `(106,180,110)`, receiving node `(107,180,110)`, upper node
  `(107,185,110)`, ordinary pump `(107,180,109)`, upper pump node
  `(107,185,109)`. Five-block lift and fall; no stored-power injection.
- Dropped 20 White Aura Crystals (300 aura each) and coal near the pump.
  The idle readout showed progress 0/60, cost 150, last/stored power 0.
  Actual circulation then produced progress 4 and last power 301.
- The first crystal dropped atop the processor was absorbed by the adjacent
  node's existing 0.75-block absorption region. A replacement dropped on the
  opposite side at `(104.5,180.2,110.5)` stayed within the consumer's input
  range. This is a setup caution, not evidence that the recipe is missing.
- Server `tick sprint` advanced actual machine, fuel and item-entity ticks.
  The first iron and spare coal despawned during the extended test; fresh
  iron/coal restored operation. This exploratory run is not a throughput
  benchmark or exact-input-conservation proof.
- The processor completed, reset progress to 0, and spawned exactly one
  `aura:arcane_ingot_white` in the final nearby item query. The HUD showed
  last/stored power 379, matching the frozen server block data.
- Parent visually inspected screenshots 305 and 308 under ignored
  `build/qa-audit/node-pilot-playtest/`; 308 shows the output and falling
  particles. Commands and replies are recorded in the local actions log.

Result: ordinary fueled falling-power generation and a processor completion
are live-observed. Other consumer families, clean recipe controls, complete
survival progression and final-build regression remain open.

## Independent UI Audit

See [independent report](2026-09-23-accessory-storage-playtest.md). Physical
accessory transaction and wing checks passed in the sampled cases. The storage
browser was blocked by empty-hand dispatch; parent repaired coordinator and
shelf fallthrough in source and added a regression, pending the next build.

Parent then reloaded the same fixture in the same client. Screenshot 502 shows
all four physical accessories still equipped and the rejected eight cobblestone
plus spare ring/stone still in inventory. This establishes sampled equipment
save/reload persistence, not death or multiplayer behavior.

## V1: Actual Falling-Power Vortex

- Controller `(114,180,103)`, four horizontal adjacent pedestals, and ordinary
  nodes five blocks above each pedestal. Fixtures/materials were command placed.
- Dropped one diamond and three White Arcane Ingots onto distinct pedestals.
  Screenshot 503 shows the Arcane White Gem recipe, zero receipts, and the
  distinct 60,000 diamond / 20,000 each ingot requirements. Block NBT confirms
  each captured item and requirement. Captured items were not visibly rendered
  in this snapshot; a renderer/manual-exchange change is awaiting integration.
- Dropped 64 White Aura Crystals at each upper node. `tick sprint 300t` advanced
  actual absorption and transfer. Screenshot 505 and server queries agree:
  diamond 35,495/60,000, all three ingots 20,000/20,000, total 95,495/120,000.
- After 400 further ticks, exactly one nearby `aura:arcane_gem_white` output
  was queried and visually seen in 506. Pedestal item/receipt/requirement state
  cleared. No receipt or stored-power NBT was injected.
- Replaced inputs with four Yellow Arcane Gems for the Cascading Looter.
  Fresh White aura was supplied above all four; after 100 ticks all four
  receipts remained 0/100,000 Yellow. Screenshot 507 confirms rejection.
- Actual Yellow crystals then supplied falling Yellow aura. After 200 ticks,
  server receipts were 22,200, 22,200, 23,275, 23,275. Screenshot 508 shows
  positive Yellow receipts but trails that final frozen tick (19,310, 19,310,
  20,365, 20,365); this is not claimed as exact simultaneous HUD agreement.
  Client synchronization is periodic and the server was frozen immediately
  after the sprint. The partially charged recipe was saved for relaunch QA.
- Normal save/Title/Quit is shown in 509/510 and the wrapper completed exit 0.

Result: live unequal per-component requirements, actual falling-power gem
craft, wrong-color rejection, and matching-color progress. The full Looter
craft, receipt persistence, manual exchange, visible held items, all output
routes, and final-build regression remain open.
