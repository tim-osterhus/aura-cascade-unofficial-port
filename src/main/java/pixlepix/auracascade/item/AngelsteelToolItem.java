package pixlepix.auracascade.item;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.enchantment.AuraEnchantments;

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

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        AngelsteelToolHelper.ensureBuffs(stack, degreeIndex, level.getRandom());
        applyVanillaBuffEnchants(stack, level.registryAccess());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return super.getDestroySpeed(stack, state) + AngelsteelToolHelper.destroySpeedBonus(stack, state);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltipAdder,
        TooltipFlag tooltipFlag
    ) {
        int[] buffs = AngelsteelToolHelper.getBuffs(stack);
        tooltipAdder.accept(Component.literal("Degree " + AngelsteelToolHelper.displayedDegree(degreeIndex)).withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.literal("Efficiency +" + buffs[0]).withStyle(ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.literal("Fortune +" + buffs[1]).withStyle(ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.literal("Shatter +" + buffs[2]).withStyle(ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.literal("Disintegrate +" + buffs[3]).withStyle(ChatFormatting.DARK_GREEN));
    }

    protected void applyVanillaBuffEnchants(ItemStack stack, HolderLookup.Provider registries) {
        int[] buffs = AngelsteelToolHelper.getBuffs(stack);
        applyIfHigher(stack, registries, net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY, buffs[0]);
        if (kind != AngelsteelToolKind.SWORD) {
            applyIfHigher(stack, registries, net.minecraft.world.item.enchantment.Enchantments.FORTUNE, buffs[1]);
        }
    }

    private static void applyIfHigher(
        ItemStack stack,
        HolderLookup.Provider registries,
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
        int level
    ) {
        if (level <= 0) {
            return;
        }

        var lookup = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = lookup.getOrThrow(key);
        int existing = stack.getEnchantments().getLevel(holder);
        if (existing < level) {
            stack.enchant(holder, level);
        }
    }

    protected Optional<pixlepix.auracascade.parity.AuraColor> swordAura(ItemStack stack) {
        return Optional.empty();
    }
}
