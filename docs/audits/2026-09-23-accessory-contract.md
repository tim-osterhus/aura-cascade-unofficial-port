# Accessory equipment contract (2026-09-23)

## Original evidence

`AuraCascade-592.jar` `EventHandler.getBaubleFromInv` calls `BaublesApi.getBaubles(player)` and searches indices 0 through 3. The port's prior `InventoryAuraAccessoryBridge` treated `CUSTOM_DATA` markers in ordinary inventory as equipment; that did not represent Baubles' separate slots. The physical layout is amulet, ring, ring, belt.

## Bounded implementation

- `AuraAccessoryInventory` stores four `ItemStack`s in a persistent Fabric player attachment. It copies on respawn. On death without `keepInventory`, equipped stacks are dropped and the attachment is cleared before respawn; with `keepInventory`, they remain attached. Dimension travel and disconnect use attachment persistence.
- The bridge returns actual equipped stack objects for passive effects and fairy binding. A component mutation on an equipped stack must call `AuraAccessoryInventory.touch(player)` so the attachment is re-set for persistence. Right-click equip moves one item into the first empty matching physical slot and returns `false` when both ring slots (or the matching single slot) are full; it never swaps another stack.
- `AuraAccessoryMenu` has four server-authoritative accessory slots and the ordinary player inventory/hotbar. Vanilla menu transactions provide mouse unequip and shift-click in both directions. `AuraAccessoryClient` registers B to open the menu and V to activate the wing in Controls. Both C2S requests carry no item or slot claims; the server validates living/non-spectator state, and the wing receiver checks the actual equipped amulet.

## Integration owned by parent

- Call `AuraAccessoryNetworking.bootstrapCommon()` during common initialization and `AuraAccessoryClient.bootstrapClient()` during client initialization.
- Supply the existing wing movement behavior with `AuraAccessoryNetworking.registerWingAction(...)`; the receiver verifies the equipped amulet first.
- Adjust `AuraItems` right-click status paths to honor the boolean `AuraAccessoryBridgeRegistry.equip` result, remove inventory-marker toggle assumptions, and call `AuraAccessoryInventory.touch(player)` after mutating an equipped ring. Add translations for `menu.aura.accessories`, `key.aura.open_accessories`, `key.aura.activate_wing`, and `key.categories.aura`.

## Validation boundary

Focused slot-policy tests were added. No Gradle build or client launch was run under the parallel-agent heavy-validation constraint. A later client pass must verify menu sync, shift-click, death/respawn with both `keepInventory` settings, dimensions, disconnect/reconnect, fairy bindings, B/V controls, and wing activation.
