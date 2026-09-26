package pixlepix.auracascade.compat;

import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.item.AuraAccessoryItem;

final class InventoryAuraAccessoryBridge implements AuraAccessoryBridge {
    @Override
    public List<ItemStack> equipped(Player player, AuraAccessorySlot slot) {
        return AuraAccessoryInventory.equipped(player, slot);
    }

    @Override
    public boolean isEquipped(Player player, ItemStack stack, AuraAccessorySlot slot) {
        return equipped(player, slot).stream().anyMatch(equipped -> equipped == stack);
    }

    @Override
    public boolean equip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        if (player.level().isClientSide() || player.isSpectator() || !player.isAlive()
            || stack.isEmpty() || !(stack.getItem() instanceof AuraAccessoryItem item) || item.slot() != slot) {
            return false;
        }
        Inventory inventory = player.getInventory();
        boolean inInventory = false;
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            if (inventory.getItem(index) == stack) {
                inInventory = true;
                break;
            }
        }
        int target = AuraAccessoryInventory.firstEmpty(player, slot);
        if (!inInventory || target < 0) {
            return false;
        }
        AuraAccessoryInventory.set(player, target, stack.copyWithCount(1));
        stack.shrink(1);
        inventory.setChanged();
        return true;
    }

    @Override
    public void unequip(Player player, ItemStack stack, AuraAccessorySlot slot) {
        if (player.level().isClientSide() || player.isSpectator() || !player.isAlive()) {
            return;
        }
        for (int index = 0; index < AuraAccessoryInventory.SLOT_COUNT; index++) {
            if (AuraAccessoryInventory.slotType(index) != slot || AuraAccessoryInventory.get(player, index) != stack) {
                continue;
            }
            ItemStack removed = stack.copy();
            AuraAccessoryInventory.set(player, index, ItemStack.EMPTY);
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            return;
        }
    }
}
