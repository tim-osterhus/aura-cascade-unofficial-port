package pixlepix.auracascade.item;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AngelsteelToolItem extends Item {
    protected final AngelsteelToolKind kind;
    protected final int degreeIndex;

    public AngelsteelToolItem(AngelsteelToolKind kind, int degreeIndex) {
        this(kind, degreeIndex, kind.createProperties(AngelsteelToolHelper.material(degreeIndex)));
    }

    public AngelsteelToolItem(AngelsteelToolKind kind, int degreeIndex, Item.Properties properties) {
        super(properties);
        this.kind = kind;
        this.degreeIndex = AngelsteelToolHelper.clampDegree(degreeIndex);
    }

    public int degreeIndex() {
        return degreeIndex;
    }

    boolean supportsMiningBuffs() {
        return kind != AngelsteelToolKind.SWORD;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (supportsMiningBuffs()) {
            AngelsteelToolHelper.ensureBuffs(stack, degreeIndex, serverLevel.getRandom());
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return super.getDestroySpeed(stack, state) * AngelsteelToolHelper.destroySpeedMultiplier(stack, state);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        java.util.List<Component> tooltip,
        TooltipFlag tooltipFlag
    ) {
        tooltip.add(Component.literal("Degree " + AngelsteelToolHelper.displayedDegree(degreeIndex)).withStyle(ChatFormatting.GRAY));
        if (supportsMiningBuffs()) {
            int[] buffs = AngelsteelToolHelper.getBuffs(stack);
            tooltip.add(Component.literal("Efficiency +" + buffs[0]).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal("Fortune +" + buffs[1]).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal("Shatter +" + buffs[2]).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal("Disintegrate +" + buffs[3]).withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    protected Optional<pixlepix.auracascade.parity.AuraColor> swordAura(ItemStack stack) {
        return Optional.empty();
    }
}
