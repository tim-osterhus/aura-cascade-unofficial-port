package pixlepix.auracascade.parity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SubstitutionClosureAuditTest {
    @Test
    void portingNotesLocalizationAndEnchantmentDataCarryTheClosureEvidence() throws IOException {
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);
        String lang = Files.readString(Path.of("src/main/resources/assets/aura/lang/en_us.json"), StandardCharsets.UTF_8);
        String red = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_red.json"), StandardCharsets.UTF_8);
        String orange = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_orange.json"), StandardCharsets.UTF_8);
        String yellow = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_yellow.json"), StandardCharsets.UTF_8);
        String green = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_green.json"), StandardCharsets.UTF_8);
        String blue = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_blue.json"), StandardCharsets.UTF_8);
        String violet = Files.readString(Path.of("src/main/resources/data/aura/enchantment/kaleidoscopic_violet.json"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertTrue(portingNotes.contains("dirt, stone, sand, and gravel")),
            () -> assertTrue(portingNotes.contains("block entities and fluids")),
            () -> assertTrue(portingNotes.contains("Red = Silk Touch")),
            () -> assertTrue(portingNotes.contains("`Yellow + Green` crop harvest")),
            () -> assertTrue(portingNotes.contains("Red = ignite")),
            () -> assertTrue(portingNotes.contains("Orange = knockback")),
            () -> assertTrue(portingNotes.contains("basic no-effect `Fairy`")),
            () -> assertTrue(lang.contains("\"text.aura.fairy_role.fairy\": \"Fairy\"")),
            () -> assertTrue(lang.contains("\"text.aura.fairy_role.training\": \"Training Fairy\"")),
            () -> assertTrue(lang.contains("\"message.aura.fairy_charm_attuned\": \"Fairy charm attuned to %s\"")),
            () -> assertTrue(lang.contains("\"message.aura.prismatic_wand_switched\": \"Switched to %s\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.prismatic_wand.copy_limits\": \"Skips block entities and fluids\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.prismatic_wand.material_limits\": \"Consumes placed block items directly\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.ring_of_shattered_stone.residual\": \"Does not yet limit blasts to dirt, stone, sand, and gravel\"")),
            () -> assertTrue(red.contains("\"minecraft:ignite\"")),
            () -> assertTrue(orange.contains("\"minecraft:knockback\"")),
            () -> assertTrue(yellow.contains("\"minecraft:mining_efficiency\"")),
            () -> assertTrue(green.contains("\"minecraft:poison\"")),
            () -> assertTrue(blue.contains("\"minecraft:damage\"")),
            () -> assertTrue(violet.contains("\"minecraft:nausea\""))
        );
    }
}
