package pixlepix.auracascade.block.entity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.enchantment.KaleidoscopicLegacyContract;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KaleidoscopicEnchanterLogicTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

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
        assertEquals("outgoing damage", KaleidoscopicLegacyContract.legacyBasicEffects().get(AuraColor.VIOLET));
        assertEquals(15, KaleidoscopicLegacyContract.legacyInteractions().size());

        Set<Set<AuraColor>> pairs = KaleidoscopicLegacyContract.legacyInteractions().stream()
            .map(interaction -> Set.of(interaction.first(), interaction.second()))
            .collect(java.util.stream.Collectors.toSet());

        assertEquals(15, pairs.size());
        assertTrue(pairs.contains(Set.of(AuraColor.YELLOW, AuraColor.GREEN)));
        assertTrue(pairs.contains(Set.of(AuraColor.RED, AuraColor.ORANGE)));
        assertTrue(pairs.contains(Set.of(AuraColor.BLUE, AuraColor.VIOLET)));
    }

    @Test
    void currentContractDescribesActualRuntimeAndItsPlatformLimits() {
        assertEquals("temporary Silk Touch mining override", KaleidoscopicLegacyContract.modernRuntimeEffects().get(AuraColor.RED));
        assertEquals("connected same-block log felling, up to 25 per level",
            KaleidoscopicLegacyContract.modernRuntimeEffects().get(AuraColor.GREEN));
        assertEquals("outgoing damage increased by 0.5 per level",
            KaleidoscopicLegacyContract.modernRuntimeEffects().get(AuraColor.VIOLET));
        assertEquals(15, KaleidoscopicLegacyContract.modernRuntimeInteractions().size());
        assertTrue(KaleidoscopicLegacyContract.modernRuntimeInteractions().stream()
            .anyMatch(interaction -> interaction.summary().contains("Green + Violet subtracts pair strength from outgoing damage")));
        assertTrue(KaleidoscopicLegacyContract.modernRuntimeLimits().stream()
            .anyMatch(limit -> limit.contains("loot table")));
        assertTrue(KaleidoscopicLegacyContract.modernRuntimeLimits().stream()
            .anyMatch(limit -> limit.contains("c:ores/<material>")));
    }

    @Test
    void currentDamageAndMiningUseOriginalPairFormulasNotStatusEffectSubstitutes() {
        EnumMap<AuraColor, Integer> damage = levels(AuraColor.GREEN, 3, AuraColor.VIOLET, 2);
        assertEquals(3, KaleidoscopicOriginalEffects.pair(damage, AuraColor.GREEN, AuraColor.VIOLET));
        assertEquals(4.0F, KaleidoscopicOriginalEffects.damageAmount(6.0F, damage, levels(), true, false), 0.0001F);

        EnumMap<AuraColor, Integer> mining = levels(AuraColor.RED, 2, AuraColor.GREEN, 2);
        assertEquals(1.0F, KaleidoscopicOriginalEffects.breakSpeed(9.0F, mining,
            false, false, false, false, false, 1.0F), 0.0001F);
        assertEquals(24, KaleidoscopicOriginalEffects.additionalConnectedBlocks(1));
    }

    @Test
    void enchantmentDataNoLongerCarriesTheSubstituteEffectIds() throws IOException {
        String red = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_red.json"), StandardCharsets.UTF_8);
        String orange = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_orange.json"), StandardCharsets.UTF_8);
        String yellow = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_yellow.json"), StandardCharsets.UTF_8);
        String green = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_green.json"), StandardCharsets.UTF_8);
        String blue = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_blue.json"), StandardCharsets.UTF_8);
        String violet = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_violet.json"), StandardCharsets.UTF_8);

        assertFalse(red.contains("\"minecraft:ignite\""));
        assertFalse(orange.contains("\"minecraft:knockback\""));
        assertFalse(yellow.contains("\"minecraft:mining_efficiency\""));
        assertFalse(green.contains("\"minecraft:poison\""));
        assertFalse(blue.contains("\"minecraft:damage\""));
        assertFalse(violet.contains("\"minecraft:nausea\""));
    }

    private static EnumMap<AuraColor, Integer> levels(Object... entries) {
        EnumMap<AuraColor, Integer> levels = new EnumMap<>(AuraColor.class);
        for (int index = 0; index < entries.length; index += 2) {
            levels.put((AuraColor) entries[index], (Integer) entries[index + 1]);
        }
        return levels;
    }
}
