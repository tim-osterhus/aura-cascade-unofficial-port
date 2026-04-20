package pixlepix.auracascade.parity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.lexicon.EncyclopediaAuraContent;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiscoverabilityParityAuditTest {
    @Test
    void creativeTabMatchesTheRegisteredAuraItemSurface() {
        TestMinecraftBootstrap.AuraRegistrationSnapshot registrations = TestMinecraftBootstrap.auraRegistrationSnapshot();
        TestMinecraftBootstrap.AuraDiscoverabilitySnapshot discoverability = TestMinecraftBootstrap.auraDiscoverabilitySnapshot();

        assertAll(
            () -> assertEquals(Set.of("aura"), discoverability.creativeTabIds()),
            () -> assertTrue(registrations.blockIds().contains("storage_bookshelf")),
            () -> assertFalse(registrations.itemIds().contains("storage_bookshelf")),
            () -> assertEquals(registrations.itemIds(), discoverability.discoverableItemIds()),
            () -> assertTrue(discoverability.discoverableItemIds().contains("aura_node_pump_creative")),
            () -> assertTrue(discoverability.discoverableItemIds().contains("basic_storage_book")),
            () -> assertTrue(discoverability.discoverableItemIds().contains("bookshelf_coordinator"))
        );
    }

    @Test
    void guidebookLocalizationAndParityNotesAgreeOnDiscoverabilityAndStorageBookshelfStory() throws IOException {
        TestMinecraftBootstrap.ensureBootstrapped();

        String guidebook = EncyclopediaAuraContent.createBook(null)
            .getPages(false)
            .stream()
            .map(page -> page.getString())
            .collect(Collectors.joining("\n"));
        JsonObject lang = readJson(Path.of("src/main/resources/assets/aura/lang/en_us.json"));
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertEquals("Aura Cascade", lang.get("itemGroup.aura").getAsString()),
            () -> assertEquals("Created a Storage Bookshelf with %s", lang.get("message.aura.storage_bookshelf_created").getAsString()),
            () -> assertTrue(guidebook.contains("Everything with an item form stays in the Aura creative tab")),
            () -> assertTrue(guidebook.contains("conversion-only block")),
            () -> assertTrue(portingNotes.contains("BlockStorageBookshelf.shouldDisplayInTab()")),
            () -> assertTrue(portingNotes.contains("ItemStorageBook.shouldDisplayInTab()")),
            () -> assertTrue(portingNotes.contains("creative tab now keeps every registered Aura item form reachable")),
            () -> assertTrue(portingNotes.contains("conversion-only block with no standalone item registration"))
        );
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
