package pixlepix.auracascade.compat;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AuraAccessoryBridgeRegistry {
    private static AuraAccessoryBridge bridge = new InventoryAuraAccessoryBridge();

    private AuraAccessoryBridgeRegistry() {
    }

    public static synchronized void install(AuraAccessoryBridge bridge) {
        AuraAccessoryBridgeRegistry.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public static synchronized void reset() {
        bridge = new InventoryAuraAccessoryBridge();
    }

    public static synchronized List<ItemStack> equipped(Player player, AuraAccessorySlot slot) {
        return bridge.equipped(player, slot);
    }

    public static synchronized boolean isEquipped(Player player, ItemStack stack, AuraAccessorySlot slot) {
        return bridge.isEquipped(player, stack, slot);
    }

    public static synchronized void equip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        bridge.equip(player, stack, slot);
    }

    public static synchronized void unequip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        bridge.unequip(player, stack, slot);
    }
}
