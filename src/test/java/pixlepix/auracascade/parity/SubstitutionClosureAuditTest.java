package pixlepix.auracascade.parity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            () -> assertTrue(portingNotes.contains("the ring makes nearby blasts spare non-terrain blocks and does not prevent")),
            () -> assertTrue(portingNotes.contains("Prismatic Wand source integration now supersedes all older snapshot-clipboard")),
            () -> assertTrue(portingNotes.contains("Red = temporary Silk Touch mining override")),
            () -> assertTrue(portingNotes.contains("`Yellow + Green` harvests mature crops in a 3x3 footprint")),
            () -> assertTrue(portingNotes.contains("destroy speed >= 8.0")),
            () -> assertTrue(portingNotes.contains("basic no-effect `Fairy`")),
            () -> assertTrue(lang.contains("\"text.aura.fairy_role.fairy\": \"Fairy\"")),
            () -> assertTrue(lang.contains("\"text.aura.fairy_role.training\": \"Training Fairy\"")),
            () -> assertTrue(lang.contains("\"message.aura.fairy_charm_attuned\": \"Fairy charm attuned to %s\"")),
            () -> assertTrue(lang.contains("\"message.aura.prismatic_wand_switched\": \"Switched to %s\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.prismatic_wand.copy_limits\": \"Live source region: up to 512 cells\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.prismatic_wand.material_limits\": \"Only empty spaces; materials charged on placement\"")),
            () -> assertTrue(lang.contains("\"tooltip.aura.ring_of_shattered_stone.residual\": \"Nearby blasts spare non-terrain blocks; the ring does not prevent wearer damage.\"")),
            () -> assertTrue(portingNotes.contains("## 1.21.11 Forward Port")),
            () -> assertTrue(portingNotes.contains("historical 1.21.1 baseline unless explicitly labeled otherwise")),
            () -> assertFalse(portingNotes.contains("Red = ignite")),
            () -> assertFalse(portingNotes.contains("Orange = knockback")),
            () -> assertFalse(red.contains("\"minecraft:ignite\"")),
            () -> assertFalse(orange.contains("\"minecraft:knockback\"")),
            () -> assertFalse(yellow.contains("\"minecraft:mining_efficiency\"")),
            () -> assertFalse(green.contains("\"minecraft:poison\"")),
            () -> assertFalse(blue.contains("\"minecraft:damage\"")),
            () -> assertFalse(violet.contains("\"minecraft:nausea\"")),
            () -> assertTrue(red.contains("\"translate\": \"enchantment.aura.kaleidoscopic_red\"")),
            () -> assertTrue(orange.contains("\"translate\": \"enchantment.aura.kaleidoscopic_orange\"")),
            () -> assertTrue(yellow.contains("\"translate\": \"enchantment.aura.kaleidoscopic_yellow\"")),
            () -> assertTrue(green.contains("\"translate\": \"enchantment.aura.kaleidoscopic_green\"")),
            () -> assertTrue(blue.contains("\"translate\": \"enchantment.aura.kaleidoscopic_blue\"")),
            () -> assertTrue(violet.contains("\"translate\": \"enchantment.aura.kaleidoscopic_violet\""))
        );
    }
}
