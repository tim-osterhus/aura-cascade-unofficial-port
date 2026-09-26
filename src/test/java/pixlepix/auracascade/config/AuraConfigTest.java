package pixlepix.auracascade.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void missingConfigCreatesEnabledDefault() throws IOException {
        Path file = tempDir.resolve("nested/aura.properties");

        assertTrue(AuraConfig.load(file));
        assertTrue(Files.readString(file).contains("questline=true"));
    }

    @Test
    void readsExplicitFalseAndPreservesOtherSettings() throws IOException {
        Path file = tempDir.resolve("aura.properties");
        Files.writeString(file, "other-setting=kept\nquestline=false\n", StandardCharsets.UTF_8);

        assertFalse(AuraConfig.load(file));
        String contents = Files.readString(file);
        assertTrue(contents.contains("other-setting=kept"));
        assertTrue(contents.contains("questline=false"));
    }

    @Test
    void missingQuestlineKeyIsAddedWithoutResettingExistingSettings() throws IOException {
        Path file = tempDir.resolve("aura.properties");
        Files.writeString(file, "other-setting=kept\n", StandardCharsets.UTF_8);

        assertTrue(AuraConfig.load(file));
        String contents = Files.readString(file);
        assertTrue(contents.contains("other-setting=kept"));
        assertTrue(contents.contains("questline=true"));
    }

    @Test
    void malformedBooleanWarnsAndRetainsEnabledDefault() throws IOException {
        Path file = tempDir.resolve("aura.properties");
        Files.writeString(file, "questline=disabled\n", StandardCharsets.UTF_8);

        assertTrue(AuraConfig.load(file));
        assertTrue(Files.readString(file).contains("questline=disabled"));
    }
}
