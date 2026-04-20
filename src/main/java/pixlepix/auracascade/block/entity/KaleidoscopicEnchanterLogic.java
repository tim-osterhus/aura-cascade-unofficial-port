package pixlepix.auracascade.block.entity;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.parity.AuraColor;

public final class KaleidoscopicEnchanterLogic {
    public static final int MAX_LEVEL = 8;

    private KaleidoscopicEnchanterLogic() {
    }

    public static boolean isValidTarget(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AuraEnchantments.KALEIDOSCOPIC_ENCHANTABLE);
    }

    public static double successRate(int totalLevel, int maxLevel) {
        return Math.pow(0.75D, Math.max(0, totalLevel)) * Math.pow(0.25D, Math.max(0, maxLevel - 4));
    }

    public static Map<AuraColor, Integer> levels(ItemStack stack, HolderLookup.Provider registries) {
        EnumMap<AuraColor, Integer> levels = new EnumMap<>(AuraColor.class);
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Map.Entry<AuraColor, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> entry : AuraEnchantments.allKaleidoscopic().entrySet()) {
            levels.put(entry.getKey(), enchantments.getLevel(lookup.getOrThrow(entry.getValue())));
        }
        return levels;
    }

    public static int totalLevel(Map<AuraColor, Integer> levels) {
        return levels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static int maxLevel(Map<AuraColor, Integer> levels) {
        return levels.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static boolean canApply(ItemStack stack, AuraColor color, HolderLookup.Provider registries) {
        if (!AuraEnchantments.kaleidoscopic(color).isPresent() || !isValidTarget(stack)) {
            return false;
        }
        return levels(stack, registries).getOrDefault(color, 0) < MAX_LEVEL;
    }
}
