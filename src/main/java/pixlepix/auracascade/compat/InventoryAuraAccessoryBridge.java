package pixlepix.auracascade.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.item.AuraAccessoryItem;

final class InventoryAuraAccessoryBridge implements AuraAccessoryBridge {
    @Override
    public List<ItemStack> equipped(Player player, AuraAccessorySlot slot) {
        ArrayList<ItemStack> equipped = new ArrayList<>();
        for (ItemStack stack : inventoryStacks(player)) {
            if (stack.isEmpty() || !(stack.getItem() instanceof AuraAccessoryItem accessoryItem) || accessoryItem.slot() != slot) {
                continue;
            }
            if (AuraAccessoryState.isEquipped(stack, slot)) {
                equipped.add(stack);
            }
        }
        return List.copyOf(equipped);
    }

    @Override
    public boolean isEquipped(Player player, ItemStack stack, AuraAccessorySlot slot) {
        return AuraAccessoryState.isEquipped(stack, slot);
    }

    @Override
    public void equip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        AuraAccessoryState.equipExclusive(slot, stack, inventoryStacks(player));
    }

    @Override
    public void unequip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        AuraAccessoryState.setEquipped(stack, slot, false);
    }

    private static List<ItemStack> inventoryStacks(Player player) {
        Inventory inventory = player.getInventory();
        ArrayList<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            stacks.add(inventory.getItem(slot));
        }
        return stacks;
    }
}
