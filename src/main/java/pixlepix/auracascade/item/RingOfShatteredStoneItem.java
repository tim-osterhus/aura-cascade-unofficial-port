package pixlepix.auracascade.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import pixlepix.auracascade.compat.AuraAccessorySlot;

public final class RingOfShatteredStoneItem extends AuraAccessoryItem {
    public RingOfShatteredStoneItem(Item.Properties properties) {
        super(AuraAccessorySlot.RING, properties);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext tooltipContext,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        tooltipAdder.accept(Component.translatable("tooltip.aura.ring_of_shattered_stone.residual").withStyle(ChatFormatting.DARK_GRAY));
    }
}
