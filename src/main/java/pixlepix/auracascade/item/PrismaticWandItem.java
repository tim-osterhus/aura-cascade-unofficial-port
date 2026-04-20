package pixlepix.auracascade.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;

public final class PrismaticWandItem extends Item {
    public PrismaticWandItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    static boolean supportsClipboardCopy(BlockState state) {
        return !state.isAir() && state.getBlock().asItem() != Items.AIR && !state.hasBlockEntity() && state.getFluidState().isEmpty();
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext tooltipContext,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        tooltipAdder.accept(Component.translatable("tooltip.aura.prismatic_wand.copy_limits").withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable("tooltip.aura.prismatic_wand.material_limits").withStyle(ChatFormatting.DARK_GRAY));
    }
}
