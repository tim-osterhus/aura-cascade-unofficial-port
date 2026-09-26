package pixlepix.auracascade.enchantment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KaleidoscopicOriginalEffectsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void pairStrengthIsCeilingOfGeometricMean() {
        Map<AuraColor, Integer> levels = Map.of(AuraColor.RED, 1, AuraColor.GREEN, 8,
            AuraColor.YELLOW, 4, AuraColor.VIOLET, 3);
        assertEquals(3, KaleidoscopicOriginalEffects.pair(levels, AuraColor.RED, AuraColor.GREEN));
        assertEquals(4, KaleidoscopicOriginalEffects.pair(levels, AuraColor.YELLOW, AuraColor.VIOLET));
        assertEquals(8, KaleidoscopicOriginalEffects.pair(levels, AuraColor.GREEN, AuraColor.GREEN));
        assertEquals(0, KaleidoscopicOriginalEffects.pair(levels, AuraColor.BLUE, AuraColor.GREEN));
    }

    @Test
    void damageChangesBeforeHurtRatherThanHealingAfterward() {
        Map<AuraColor, Integer> attacker = Map.of(AuraColor.GREEN, 4, AuraColor.VIOLET, 4);
        Map<AuraColor, Integer> defender = Map.of(AuraColor.RED, 2, AuraColor.VIOLET, 2);
        assertEquals(6.48F, KaleidoscopicOriginalEffects.damageAmount(10.0F, attacker, defender, true, true), 0.0001F);
        assertEquals(10.0F, KaleidoscopicOriginalEffects.damageAmount(10.0F, attacker, defender, false, false), 0.0001F);
        assertEquals(0.0F, KaleidoscopicOriginalEffects.damageAmount(1.0F, attacker, Map.of(), true, false), 0.0001F);
    }

    @Test
    void breakSpeedUsesOriginalMultipliersAndToolGate() {
        Map<AuraColor, Integer> levels = Map.of(AuraColor.RED, 2, AuraColor.GREEN, 2,
            AuraColor.ORANGE, 2, AuraColor.VIOLET, 2, AuraColor.YELLOW, 2);
        float expected = (float) (12.0D / 9.0D * Math.pow(1.15D, 2) * Math.pow(1.5D, 2)
            * Math.pow(1.25D, 2));
        assertEquals(expected, KaleidoscopicOriginalEffects.breakSpeed(12.0F, levels,
            true, true, false, false, false, 3.0F), 0.0001F);
        assertEquals(12.0F / 9.0F, KaleidoscopicOriginalEffects.breakSpeed(12.0F, levels,
            false, true, false, false, false, 3.0F), 0.0001F);
        assertEquals(1.0F, KaleidoscopicOriginalEffects.breakSpeed(1.0F, Map.of(AuraColor.VIOLET, 8),
            true, false, false, false, false, 50.0F), 0.0001F);
    }

    @Test
    void oreConversionAndConnectedBreakBudgetMatch592() {
        assertFalse(KaleidoscopicOriginalEffects.convertsOre(0, 0));
        assertTrue(KaleidoscopicOriginalEffects.convertsOre(1, 0));
        assertFalse(KaleidoscopicOriginalEffects.convertsOre(1, 1));
        assertTrue(KaleidoscopicOriginalEffects.convertsOre(4, 3));
        assertEquals(24, KaleidoscopicOriginalEffects.additionalConnectedBlocks(1));
        assertEquals(199, KaleidoscopicOriginalEffects.additionalConnectedBlocks(8));
    }

    @Test
    void oreConversionRequiresEligibleOrePairToolAndSuccessfulRoll() {
        Map<AuraColor, Integer> powers = Map.of(AuraColor.RED, 2, AuraColor.YELLOW, 3);
        AtomicInteger rolls = new AtomicInteger();
        assertEquals(Items.IRON_INGOT, KaleidoscopicOriginalEffects.conversionIngot(
            Blocks.IRON_ORE.defaultBlockState(), powers, true, rolls::getAndIncrement));
        assertEquals(1, rolls.get());
        assertEquals(null, KaleidoscopicOriginalEffects.conversionIngot(
            Blocks.IRON_ORE.defaultBlockState(), powers, false, rolls::getAndIncrement));
        assertEquals(null, KaleidoscopicOriginalEffects.conversionIngot(
            Blocks.DIAMOND_ORE.defaultBlockState(), powers, true, rolls::getAndIncrement));
        assertEquals(null, KaleidoscopicOriginalEffects.conversionIngot(
            Blocks.IRON_ORE.defaultBlockState(), Map.of(AuraColor.RED, 2), true, rolls::getAndIncrement));
        assertEquals(1, rolls.get());
        assertEquals(null, KaleidoscopicOriginalEffects.conversionIngot(
            Blocks.COPPER_ORE.defaultBlockState(), Map.of(AuraColor.RED, 1, AuraColor.YELLOW, 1), true, () -> 1));
    }
}
