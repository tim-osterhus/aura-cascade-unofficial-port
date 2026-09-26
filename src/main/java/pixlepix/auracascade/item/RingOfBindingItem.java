package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.util.NbtCompat;

public final class RingOfBindingItem extends AuraAccessoryItem {
    public static final int MAX_BOUND_FAIRIES = 15;
    private static final String BOUND_FAIRIES_TAG = "boundFairies";
    private static final String BOUND_FAIRY_COUNT_TAG = "boundFairyCount";
    private static final String LEGACY_FAIRY_LIST_TAG = "fairyList";

    public RingOfBindingItem() {
        this(new Item.Properties());
    }

    public RingOfBindingItem(Item.Properties properties) {
        super(AuraAccessorySlot.RING, properties);
    }

    public static List<FairyRole> boundFairies(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        boolean storedRoles = tag.contains(BOUND_FAIRIES_TAG);
        boolean legacyRoles = !storedRoles && tag.contains(LEGACY_FAIRY_LIST_TAG);
        int[] encodedRoles = storedRoles
            ? NbtCompat.getIntArrayOr(tag, BOUND_FAIRIES_TAG, new int[0])
            : legacyRoles
                ? NbtCompat.getIntArrayOr(tag, LEGACY_FAIRY_LIST_TAG, new int[0])
                : migrateLegacyCount(NbtCompat.getIntOr(tag, BOUND_FAIRY_COUNT_TAG, 0));
        int boundedCount = Math.min(encodedRoles.length, MAX_BOUND_FAIRIES);
        ArrayList<FairyRole> roles = new ArrayList<>(boundedCount);
        for (int index = 0; index < boundedCount; index++) {
            roles.add(storedRoles ? FairyRole.byStoredIndex(encodedRoles[index]) : FairyRole.byLegacyIndex(encodedRoles[index]));
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
                encoded[index] = currentRoles.get(index).storedIndex();
            }
            encoded[currentRoles.size()] = role.storedIndex();
            tag.putIntArray(BOUND_FAIRIES_TAG, encoded);
            tag.remove(BOUND_FAIRY_COUNT_TAG);
            tag.remove(LEGACY_FAIRY_LIST_TAG);
        });
        return true;
    }

    public static int clearBoundFairies(ItemStack stack) {
        int bound = boundFairyCount(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(BOUND_FAIRIES_TAG);
            tag.remove(BOUND_FAIRY_COUNT_TAG);
            tag.remove(LEGACY_FAIRY_LIST_TAG);
        });
        return bound;
    }

    public static List<FairyRole> releaseBoundFairies(ItemStack stack) {
        List<FairyRole> roles = boundFairies(stack);
        clearBoundFairies(stack);
        return roles;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext tooltipContext,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltip,
        TooltipFlag tooltipFlag
    ) {
        List<FairyRole> roles = boundFairies(stack);
        tooltip.accept(Component.translatable("tooltip.aura.ring_of_binding.count", roles.size(), MAX_BOUND_FAIRIES)
            .withStyle(ChatFormatting.GRAY));
        for (FairyRole role : roles) {
            tooltip.accept(Component.literal("- ").append(role.displayComponent()).withStyle(ChatFormatting.DARK_GRAY));
        }
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
