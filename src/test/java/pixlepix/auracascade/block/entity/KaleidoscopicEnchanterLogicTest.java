package pixlepix.auracascade.block.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.enchantment.KaleidoscopicLegacyContract;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KaleidoscopicEnchanterLogicTest {
    @Test
    void successRateFallsByThreeQuartersPerAddedColorOrLevel() {
        assertEquals(1.0D, KaleidoscopicEnchanterLogic.successRate(0, 0), 1.0E-9D);
        assertEquals(0.75D, KaleidoscopicEnchanterLogic.successRate(1, 1), 1.0E-9D);
        assertEquals(0.5625D, KaleidoscopicEnchanterLogic.successRate(2, 1), 1.0E-9D);
        assertEquals(Math.pow(0.75D, 5) * 0.25D, KaleidoscopicEnchanterLogic.successRate(5, 5), 1.0E-9D);
    }

    @Test
    void aggregateHelpersTrackTotalAndHighestColorLevel() {
        EnumMap<AuraColor, Integer> levels = new EnumMap<>(AuraColor.class);
        levels.put(AuraColor.RED, 1);
        levels.put(AuraColor.BLUE, 4);
        levels.put(AuraColor.VIOLET, 2);

        assertEquals(7, KaleidoscopicEnchanterLogic.totalLevel(levels));
        assertEquals(4, KaleidoscopicEnchanterLogic.maxLevel(levels));
    }

    @Test
    void legacyContractCoversAllSixBasicEffectsAndAllFifteenPairs() {
        assertEquals("Silk Touch", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.RED));
        assertEquals("Efficiency", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.ORANGE));
        assertEquals("Fortune", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.YELLOW));
        assertEquals("tree-felling", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.GREEN));
        assertEquals("Knockback", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.BLUE));
        assertEquals("hard-material mining speed", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.VIOLET));
        assertEquals(15, KaleidoscopicLegacyContract.legacyInteractions().size());

        Set<Set<AuraColor>> pairs = KaleidoscopicLegacyContract.legacyInteractions().stream()
            .map(interaction -> Set.of(interaction.first(), interaction.second()))
            .collect(java.util.stream.Collectors.toSet());

        assertEquals(15, pairs.size());
        assertTrue(pairs.contains(Set.of(AuraColor.YELLOW, AuraColor.GREEN)));
        assertTrue(pairs.contains(Set.of(AuraColor.RED, AuraColor.ORANGE)));
        assertTrue(pairs.contains(Set.of(AuraColor.BLUE, AuraColor.VIOLET)));
    }
}
