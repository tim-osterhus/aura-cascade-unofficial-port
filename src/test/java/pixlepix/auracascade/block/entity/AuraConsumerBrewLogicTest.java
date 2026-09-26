package pixlepix.auracascade.block.entity;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraConsumerBrewLogicTest {
    @BeforeEach
    void bootstrapRegistries() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void waterAndAwkwardPotionsAdvanceThroughTheTwelveLegacyBaseValues() {
        assertTrue(AuraConsumerBrewLogic.canBrew(potion(Potions.WATER)));
        ItemStack awkward = AuraConsumerBrewLogic.result(potion(Potions.WATER), RandomSource.create(1L)).orElseThrow();
        assertTrue(contents(awkward).is(Potions.AWKWARD));
        assertTrue(AuraConsumerBrewLogic.canBrew(awkward));

        ItemStack base = AuraConsumerBrewLogic.result(awkward, RandomSource.create(2L)).orElseThrow();
        assertEquals(12, AuraConsumerBrewLogic.LEGACY_BASE_POTIONS.size());
        assertTrue(AuraConsumerBrewLogic.LEGACY_BASE_POTIONS.stream().anyMatch(contents(base)::is));
        assertTrue(AuraConsumerBrewLogic.canBrew(base));
    }

    @Test
    void basePotionsBecomeTerminalLongOrStrongVariantsInsteadOfSplashPotions() {
        for (Map.Entry<Holder<Potion>, Set<Holder<Potion>>> entry : baseStageResults().entrySet()) {
            for (long seed = 0; seed < 32; seed++) {
                ItemStack result = AuraConsumerBrewLogic.result(potion(entry.getKey()), RandomSource.create(seed)).orElseThrow();
                PotionContents resultContents = contents(result);
                assertTrue(entry.getValue().stream().anyMatch(resultContents::is));
                assertFalse(result.is(Items.SPLASH_POTION));
                assertFalse(AuraConsumerBrewLogic.canBrew(result));
            }
        }
    }

    @Test
    void unsupportedPotionItemsAndAlreadyModifiedVariantsAreNotEligibleInputs() {
        assertFalse(AuraConsumerBrewLogic.canBrew(new ItemStack(Items.SPLASH_POTION)));
        assertFalse(AuraConsumerBrewLogic.canBrew(potion(Potions.LONG_SWIFTNESS)));
        assertFalse(AuraConsumerBrewLogic.canBrew(potion(Potions.STRONG_SWIFTNESS)));
        assertFalse(AuraConsumerBrewLogic.canBrew(new ItemStack(Items.DIAMOND)));
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    private static PotionContents contents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }

    private static Map<Holder<Potion>, Set<Holder<Potion>>> baseStageResults() {
        return Map.ofEntries(
            Map.entry(Potions.REGENERATION, Set.of(Potions.LONG_REGENERATION, Potions.STRONG_REGENERATION)),
            Map.entry(Potions.SWIFTNESS, Set.of(Potions.LONG_SWIFTNESS, Potions.STRONG_SWIFTNESS)),
            Map.entry(Potions.FIRE_RESISTANCE, Set.of(Potions.LONG_FIRE_RESISTANCE, Potions.FIRE_RESISTANCE)),
            Map.entry(Potions.POISON, Set.of(Potions.LONG_POISON, Potions.STRONG_POISON)),
            Map.entry(Potions.HEALING, Set.of(Potions.HEALING, Potions.STRONG_HEALING)),
            Map.entry(Potions.NIGHT_VISION, Set.of(Potions.LONG_NIGHT_VISION, Potions.NIGHT_VISION)),
            Map.entry(Potions.WEAKNESS, Set.of(Potions.LONG_WEAKNESS, Potions.WEAKNESS)),
            Map.entry(Potions.STRENGTH, Set.of(Potions.LONG_STRENGTH, Potions.STRONG_STRENGTH)),
            Map.entry(Potions.SLOWNESS, Set.of(Potions.LONG_SLOWNESS, Potions.STRONG_SLOWNESS)),
            Map.entry(Potions.HARMING, Set.of(Potions.HARMING, Potions.STRONG_HARMING)),
            Map.entry(Potions.WATER_BREATHING, Set.of(Potions.LONG_WATER_BREATHING, Potions.WATER_BREATHING)),
            Map.entry(Potions.INVISIBILITY, Set.of(Potions.LONG_INVISIBILITY, Potions.INVISIBILITY))
        );
    }
}
