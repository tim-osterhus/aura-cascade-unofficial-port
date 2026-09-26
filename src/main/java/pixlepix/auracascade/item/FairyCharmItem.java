package pixlepix.auracascade.item;

import java.util.function.Consumer;
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
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.util.NbtCompat;

public final class FairyCharmItem extends Item {
    private static final String FAIRY_ROLE_TAG = "fairyRole";

    public FairyCharmItem() {
        this(new Item.Properties());
    }

    public FairyCharmItem(Item.Properties properties) {
        super(properties.stacksTo(16));
    }

    public static FairyRole role(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return FairyRole.byId(NbtCompat.getStringOr(tag, FAIRY_ROLE_TAG, FairyRole.defaultRole().id()));
    }

    public static ItemStack withRole(ItemStack stack, FairyRole role) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(FAIRY_ROLE_TAG, role.id()));
        return stack;
    }

    public static FairyRole cycleRole(ItemStack stack) {
        FairyRole nextRole = role(stack).next();
        withRole(stack, nextRole);
        return nextRole;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext tooltipContext,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltip,
        TooltipFlag tooltipFlag
    ) {
        tooltip.accept(Component.translatable(role(stack).translationKey()).withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.aura.fairy_charm.cycle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
