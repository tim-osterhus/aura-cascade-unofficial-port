package pixlepix.auracascade.compat;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

public final class AuraAccessoryMenu extends AbstractContainerMenu {
    private static final int INVENTORY_START = AuraAccessoryInventory.SLOT_COUNT;
    private final Player owner;

    public AuraAccessoryMenu(int containerId, Inventory inventory) {
        super(AuraAccessoryNetworking.menuType(), containerId);
        owner = inventory.player;
        Container accessories = owner.level().isClientSide()
            ? new SimpleContainer(AuraAccessoryInventory.SLOT_COUNT)
            : new AttachedContainer(owner);

        for (int index = 0; index < AuraAccessoryInventory.SLOT_COUNT; index++) {
            addSlot(new Slot(accessories, index, 35 + index * 35, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AuraAccessoryInventory.accepts(getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && player.isAlive() && !player.isSpectator();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(source, INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!(source.getItem() instanceof pixlepix.auracascade.item.AuraAccessoryItem accessory)) {
                return ItemStack.EMPTY;
            }
            int target = AuraAccessoryInventory.firstEmpty(accessoryStacks(), accessory.slot());
            if (target < 0 || !moveItemStackTo(source, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private java.util.List<ItemStack> accessoryStacks() {
        return slots.subList(0, INVENTORY_START).stream().map(Slot::getItem).toList();
    }

    private static final class AttachedContainer implements Container {
        private final Player owner;

        private AttachedContainer(Player owner) {
            this.owner = owner;
        }

        @Override
        public int getContainerSize() {
            return AuraAccessoryInventory.SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int index = 0; index < getContainerSize(); index++) {
                if (!getItem(index).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return AuraAccessoryInventory.get(owner, index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack existing = getItem(index);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = existing.copyWithCount(Math.min(count, existing.getCount()));
            AuraAccessoryInventory.set(owner, index, existing.copyWithCount(existing.getCount() - removed.getCount()));
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack removed = getItem(index);
            AuraAccessoryInventory.set(owner, index, ItemStack.EMPTY);
            return removed;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            AuraAccessoryInventory.set(owner, index, stack);
        }

        @Override
        public void setChanged() {
            AuraAccessoryInventory.touch(owner);
        }

        @Override
        public boolean stillValid(Player player) {
            return player == owner && player.isAlive() && !player.isSpectator();
        }

        @Override
        public void clearContent() {
            for (int index = 0; index < getContainerSize(); index++) {
                AuraAccessoryInventory.set(owner, index, ItemStack.EMPTY);
            }
        }
    }
}
