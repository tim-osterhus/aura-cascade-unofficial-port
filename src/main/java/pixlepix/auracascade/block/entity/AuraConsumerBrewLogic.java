package pixlepix.auracascade.block.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;

final class AuraConsumerBrewLogic {
    static final List<Holder<Potion>> LEGACY_BASE_POTIONS = List.of(
        Potions.REGENERATION,
        Potions.SWIFTNESS,
        Potions.FIRE_RESISTANCE,
        Potions.POISON,
        Potions.HEALING,
        Potions.NIGHT_VISION,
        Potions.WEAKNESS,
        Potions.STRENGTH,
        Potions.SLOWNESS,
        Potions.HARMING,
        Potions.WATER_BREATHING,
        Potions.INVISIBILITY
    );

    private static final String TERMINAL_STAGE_TAG = "aura:consumer_brew_terminal";
    private static final Map<Holder<Potion>, Holder<Potion>> LONG_POTIONS = Map.ofEntries(
        Map.entry(Potions.REGENERATION, Potions.LONG_REGENERATION),
        Map.entry(Potions.SWIFTNESS, Potions.LONG_SWIFTNESS),
        Map.entry(Potions.FIRE_RESISTANCE, Potions.LONG_FIRE_RESISTANCE),
        Map.entry(Potions.POISON, Potions.LONG_POISON),
        Map.entry(Potions.NIGHT_VISION, Potions.LONG_NIGHT_VISION),
        Map.entry(Potions.WEAKNESS, Potions.LONG_WEAKNESS),
        Map.entry(Potions.STRENGTH, Potions.LONG_STRENGTH),
        Map.entry(Potions.SLOWNESS, Potions.LONG_SLOWNESS),
        Map.entry(Potions.WATER_BREATHING, Potions.LONG_WATER_BREATHING),
        Map.entry(Potions.INVISIBILITY, Potions.LONG_INVISIBILITY)
    );
    private static final Map<Holder<Potion>, Holder<Potion>> STRONG_POTIONS = Map.ofEntries(
        Map.entry(Potions.REGENERATION, Potions.STRONG_REGENERATION),
        Map.entry(Potions.SWIFTNESS, Potions.STRONG_SWIFTNESS),
        Map.entry(Potions.POISON, Potions.STRONG_POISON),
        Map.entry(Potions.HEALING, Potions.STRONG_HEALING),
        Map.entry(Potions.STRENGTH, Potions.STRONG_STRENGTH),
        Map.entry(Potions.SLOWNESS, Potions.STRONG_SLOWNESS),
        Map.entry(Potions.HARMING, Potions.STRONG_HARMING)
    );

    private AuraConsumerBrewLogic() {
    }

    static boolean canBrew(ItemStack stack) {
        if (!stack.is(Items.POTION)
            || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(TERMINAL_STAGE_TAG)) {
            return false;
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.WATER)
            || contents.is(Potions.AWKWARD)
            || LEGACY_BASE_POTIONS.stream().anyMatch(contents::is);
    }

    static Optional<ItemStack> result(ItemStack input, RandomSource random) {
        if (!canBrew(input)) {
            return Optional.empty();
        }

        PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.is(Potions.WATER)) {
            return Optional.of(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD));
        }
        if (contents.is(Potions.AWKWARD)) {
            Holder<Potion> result = LEGACY_BASE_POTIONS.get(random.nextInt(LEGACY_BASE_POTIONS.size()));
            return Optional.of(PotionContents.createItemStack(Items.POTION, result));
        }

        Holder<Potion> basePotion = LEGACY_BASE_POTIONS.stream()
            .filter(contents::is)
            .findFirst()
            .orElseThrow();
        if (random.nextBoolean()) {
            Holder<Potion> strongPotion = STRONG_POTIONS.get(basePotion);
            return Optional.of(strongPotion == null ? terminalPotion(basePotion, 5) : potion(strongPotion));
        }

        Holder<Potion> longPotion = LONG_POTIONS.get(basePotion);
        return Optional.of(longPotion == null ? terminalPotion(basePotion, 6) : potion(longPotion));
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    private static ItemStack terminalPotion(Holder<Potion> potion, int legacyModifierBit) {
        ItemStack result = potion(potion);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putInt(TERMINAL_STAGE_TAG, legacyModifierBit));
        return result;
    }
}
