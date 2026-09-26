package pixlepix.auracascade.block.entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Source contracts deliberately do not claim a live registry-backed enchanting attempt.
final class AuraConsumerEnchanterContractTest {
    private static final Path CONSUMER = Path.of(
        "src/main/java/pixlepix/auracascade/block/entity/AuraConsumerBlockEntity.java");

    @Test
    void enchanterUsesOnlyArcaneIngotsAndRequiresAnApplicableColor() throws IOException {
        String source = Files.readString(CONSUMER);
        String selection = section(source, "private Optional<EnchantInput> findEnchantInput(",
            "private boolean synthesizeAngelsteel(");
        assertTrue(selection.contains("AuraItems.arcaneIngotColor(ingot.getItem())"));
        assertFalse(selection.contains("auraCrystalColor"));
        assertFalse(selection.contains("arcaneGem"));
        assertTrue(selection.contains("AuraEnchantments.kaleidoscopic(maybeColor.get()).isEmpty()"));
        assertTrue(selection.contains("KaleidoscopicEnchanterLogic.canApply(targetStack, color, level.registryAccess())"));
        assertTrue(source.contains("case ENCHANTER -> findEnchantInput(level, nearbyItems(level, pos)).isPresent()"));
    }

    @Test
    void failedRollStillConsumesExactlyOneIngotAndEmptyDropIsDiscarded() throws IOException {
        String attempt = section(Files.readString(CONSUMER), "private boolean enchantNearbyItem(",
            "private boolean brewPotions(");
        String unconditionalConsumption = """
                ingotStack.shrink(1);
                if (ingotStack.isEmpty()) {
                    input.ingot().discard();
                }

                if (level.getRandom().nextDouble() < successRate) {
            """.strip();
        assertTrue(normalizeIndent(attempt).contains(normalizeIndent(unconditionalConsumption)));
        assertTrue(attempt.indexOf("if (maybeInput.isEmpty())") < attempt.indexOf("ingotStack.shrink(1)"));
        assertTrue(attempt.contains("targetStack.enchant(holder, nextLevel)"));
        assertFalse(attempt.contains("syncPersistentRuntimeEnchantments"));
        assertFalse(attempt.contains(".grow("));
        assertTrue(attempt.stripTrailing().endsWith("return true;\n    }")
            || attempt.stripTrailing().endsWith("return true;\r\n    }"));
    }

    @Test
    void dyeExtractionRemainsRestrictedToPrismaticProcessor() throws IOException {
        String source = Files.readString(CONSUMER);
        assertTrue(source.contains(".filter(recipe -> allowPrismaticRecipes || !recipe.prismaticOnly())"));
    }

    private static String section(String source, String start, String end) {
        int first = source.indexOf(start);
        int last = source.indexOf(end, first);
        assertTrue(first >= 0 && last > first, "Consumer method boundaries must remain available to this contract test");
        return source.substring(first, last);
    }

    private static String normalizeIndent(String source) {
        return source.lines().map(String::strip).collect(java.util.stream.Collectors.joining("\n"));
    }
}
