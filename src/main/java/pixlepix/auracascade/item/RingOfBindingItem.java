package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.fairy.FairyRole;

public final class RingOfBindingItem extends AuraAccessoryItem {
    public static final int MAX_BOUND_FAIRIES = 15;
    private static final String BOUND_FAIRIES_TAG = "boundFairies";
    private static final String BOUND_FAIRY_COUNT_TAG = "boundFairyCount";

    public RingOfBindingItem() {
        this(new Item.Properties());
    }

    public RingOfBindingItem(Item.Properties properties) {
        super(AuraAccessorySlot.RING, properties);
    }

    public static List<FairyRole> boundFairies(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        int[] encodedRoles = tag.getIntArray(BOUND_FAIRIES_TAG).orElseGet(() -> migrateLegacyCount(tag.getInt(BOUND_FAIRY_COUNT_TAG).orElse(0)));
        ArrayList<FairyRole> roles = new ArrayList<>(encodedRoles.length);
        for (int encodedRole : encodedRoles) {
            roles.add(FairyRole.byLegacyIndex(encodedRole));
        }
        return List.copyOf(roles);
    }

    public static int boundFairyCount(ItemStack stack) {
        return boundFairies(stack).size();
    }

    public static boolean bindCharm(ItemStack stack) {
        return bindCharm(stack, FairyRole.defaultRole());
    }

    public static boolean bindCharm(ItemStack stack, FairyRole role) {
        List<FairyRole> currentRoles = boundFairies(stack);
        if (currentRoles.size() >= MAX_BOUND_FAIRIES) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            int[] encoded = new int[currentRoles.size() + 1];
            for (int index = 0; index < currentRoles.size(); index++) {
                encoded[index] = currentRoles.get(index).legacyIndex();
            }
            encoded[currentRoles.size()] = role.legacyIndex();
            tag.putIntArray(BOUND_FAIRIES_TAG, encoded);
            tag.remove(BOUND_FAIRY_COUNT_TAG);
        });
        return true;
    }

    public static int clearBoundFairies(ItemStack stack) {
        int bound = boundFairyCount(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(BOUND_FAIRIES_TAG);
            tag.remove(BOUND_FAIRY_COUNT_TAG);
        });
        return bound;
    }

    public static List<FairyRole> releaseBoundFairies(ItemStack stack) {
        List<FairyRole> roles = boundFairies(stack);
        clearBoundFairies(stack);
        return roles;
    }

    private static int[] migrateLegacyCount(int count) {
        int boundedCount = Math.max(0, Math.min(MAX_BOUND_FAIRIES, count));
        int[] migrated = new int[boundedCount];
        for (int index = 0; index < boundedCount; index++) {
            migrated[index] = FairyRole.defaultRole().legacyIndex();
        }
        return migrated;
    }
}
