package pixlepix.auracascade.compat;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.util.NbtCompat;

public final class AuraAccessoryState {
    static final String EQUIPPED_SLOT_TAG = "aura_accessory_slot";

    private AuraAccessoryState() {
    }

    public static Optional<AuraAccessorySlot> equippedSlot(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        String slotId = NbtCompat.getStringOr(tag, EQUIPPED_SLOT_TAG, "");
        if (slotId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AuraAccessorySlot.byId(slotId));
    }

    public static boolean isEquipped(ItemStack stack, AuraAccessorySlot slot) {
        return equippedSlot(stack).filter(equipped -> equipped == slot).isPresent();
    }

    public static void setEquipped(ItemStack stack, AuraAccessorySlot slot, boolean equipped) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (equipped) {
                tag.putString(EQUIPPED_SLOT_TAG, slot.id());
            } else {
                tag.remove(EQUIPPED_SLOT_TAG);
            }
        });
    }

    public static void equipExclusive(AuraAccessorySlot slot, ItemStack target, Iterable<ItemStack> inventoryStacks) {
        for (ItemStack stack : inventoryStacks) {
            if (!stack.isEmpty() && isEquipped(stack, slot)) {
                setEquipped(stack, slot, false);
            }
        }
        setEquipped(target, slot, true);
    }
}
