package pixlepix.auracascade.lexicon;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class EncyclopediaAuraContentTest {
    @Test
    void guidebookMentionsTheImplementedProgressionSlicesOnly() {
        TestMinecraftBootstrap.ensureBootstrapped();

        String text = EncyclopediaAuraContent.createBook(null)
            .getPages(false)
            .stream()
            .map(page -> page.getString())
            .collect(Collectors.joining("\n"));

        assertTrue(text.contains("Aura Basics"));
        assertTrue(text.contains("Pumps And Control"));
        assertTrue(text.contains("Consumers And Vortex"));
        assertTrue(text.contains("Storage Network"));
        assertTrue(text.contains("Fairies"));
        assertTrue(text.contains("Enchantments"));
        assertTrue(text.contains("Late Systems"));
        assertTrue(text.contains("plain Fairy"));
        assertTrue(text.contains("block entities and fluids"));
        assertTrue(text.contains("Red = Silk Touch"));
        assertTrue(text.contains("Yellow + Green harvests crops"));
        assertTrue(text.contains("current runtime keeps a bounded substitution set"));
        assertTrue(text.contains("[ ] Carry an Aura Crystal"));
    }
}
