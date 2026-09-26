# Storage Browser Contract

This note records the Phase 2 Bookshelf Coordinator browser boundary for the
parent bootstrap and localization wiring. The browser uses the existing storage
book data in each reachable storage shelf; it does not mirror contents into a
client inventory or introduce a second storage backend.

## Behavior

- An ordinary use opens the server-backed browser. Sneak-use keeps the legacy
  quick-withdraw action, and using an item keeps the existing deposit action.
- The browser aggregates matching item stacks by item and data components,
  reports full stored counts (including counts larger than a normal stack),
  filters localized item names, and has a stable 9-by-3 scrollable view.
- Retrieval requests carry a selected stack exemplar and requested amount.
  The server checks the active menu, viewer distance, loaded coordinator,
  network completeness, storage shelves, power, exact stack components, and
  player inventory capacity before extracting. Actual remainder stacks are
  inserted into the player's inventory; any insertion remainder is dropped.
- Network scans do not read unloaded positions. If the scan cannot prove the
  connected shelf network or line-of-sight region is loaded, browser actions
  are disabled and deposits/withdrawals are rejected until it is complete.
- Storage remains persisted in the storage book's existing custom data. The
  storage bookshelf block entity keeps its existing book save/load path.

## Parent Registration Hooks

The parent-owned `AuraCascadeMod.onInitialize()` now bootstraps `AuraContent`,
registers the menu type, and then registers networking:

```java
AuraContent.bootstrap();
BookshelfCoordinatorMenu.registerMenuType(Registry.register(
    BuiltInRegistries.MENU,
    ResourceLocation.fromNamespaceAndPath(MOD_ID, "bookshelf_coordinator"),
    new MenuType<>(BookshelfCoordinatorMenu::new, FeatureFlags.VANILLA_SET)
));
BookshelfCoordinatorNetworking.register();
```

The parent-owned `AuraCascadeClient.onInitializeClient()` calls
`BookshelfCoordinatorClientNetworking.register(BookshelfCoordinatorMenu.registeredMenuType())`.
That client hook registers `BookshelfCoordinatorScreen`; common registration
owns the C2S/S2C payload types and server receiver. No fabric metadata changes
are required because the existing client entrypoint remains enabled.

## Translation Keys

Parent-owned `en_us.json` should define these keys (the screen strings may use
other locales through normal Minecraft localization):

```text
screen.aura.bookshelf_coordinator.search
screen.aura.bookshelf_coordinator.amount
screen.aura.bookshelf_coordinator.max
screen.aura.bookshelf_coordinator.retrieve
screen.aura.bookshelf_coordinator.power
screen.aura.bookshelf_coordinator.item_count
screen.aura.bookshelf_coordinator.incomplete_network
screen.aura.bookshelf_coordinator.disconnected
screen.aura.bookshelf_coordinator.powerless
screen.aura.bookshelf_coordinator.no_storage_shelves
screen.aura.bookshelf_coordinator.ready
screen.aura.bookshelf_coordinator.empty
screen.aura.bookshelf_coordinator.no_results
screen.aura.bookshelf_coordinator.select_item
screen.aura.bookshelf_coordinator.retrieved
screen.aura.bookshelf_coordinator.inventory_full
screen.aura.bookshelf_coordinator.item_missing
screen.aura.bookshelf_coordinator.invalid_amount
message.aura.bookshelf_coordinator.disconnected
message.aura.bookshelf_coordinator.incomplete_network
message.aura.bookshelf_coordinator.powerless
message.aura.bookshelf_coordinator.stored
message.aura.bookshelf_coordinator.could_not_store
message.aura.bookshelf_coordinator.empty
message.aura.bookshelf_coordinator.withdrawn
```

The existing `block.aura.bookshelf_coordinator` display name is reused.

## Validation

Focused tests cover component-aware storage entries/extraction with a count
above 64, independent copied-book persistence, and server menu access for
loaded/missing coordinators and the eight-block distance boundary. Run these
with the repository's normal test task after parent registration wiring;
compilation and game verification are intentionally left to the parent build
window.
