package pixlepix.auracascade.parity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FinalClientParityAuditTest {
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/aura");
    private static final Path DATA_ROOT = Path.of("src/main/resources/data/aura");

    @Test
    void representativeClosureBlocksStayAcquirablePlaceableAndNamed() throws IOException {
        JsonObject lang = readJson(ASSET_ROOT.resolve("lang/en_us.json"));

        assertAll(
            () -> assertCraftingBackedBlock("bookshelf_coordinator", "Bookshelf Coordinator", lang),
            () -> assertCraftingBackedBlock("aura_node_black", "Aura Manipulator: Black", lang),
            () -> assertCraftingBackedBlock("aura_node_capacitor", "Aura Capacitor", lang),
            () -> assertCraftingBackedBlock("aura_node_conserve", "Conserving Aura Node", lang),
            () -> assertCraftingBackedBlock("aura_node_orange", "Aura Manipulator: Orange", lang),
            () -> assertCraftingBackedBlock("monitor", "Monitor", lang),
            () -> assertCraftingBackedBlock("travelers_bricks", "Traveler's Bricks", lang),
            () -> assertCraftingBackedBlock("rebounding_enigma", "Rebounding Enigma", lang),
            () -> assertCraftingBackedBlock("consumer_block_miner", "Cascading Miner", lang),
            () -> assertCraftingBackedBlock("ritual_end", "Ritual of the End", lang),
            () -> assertProcessorBackedBlock("fortified_planks", "Fortified Planks", "processor/fortified_planks.json", lang),
            () -> assertVortexBackedBlock("consumer_block_loot", "Cascading Looter", "vortex/consumer_block_loot.json", lang),
            () -> assertVortexBackedBlock("consumer_block_spawn", "Cascading Spawner", "vortex/consumer_block_spawn.json", lang),
            () -> assertVortexBackedBlock("ritual_nether", "Ritual of the Nether", "vortex/ritual_nether.json", lang),
            () -> assertConversionOnlyBlock("storage_bookshelf", "Storage Bookshelf", lang),
            () -> assertCreativeOnlyBlock("aura_node_pump_creative", "Creative Pump (Creative Only)", lang)
        );
    }

    @Test
    void guidebookAndParityNotesStayTruthfulAboutClosureSliceResiduals() throws IOException {
        String guidebook = readGuideEntries();
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertTrue(guidebook.contains("first adjacent supported machine")),
            () -> assertTrue(guidebook.contains("Aura creative tab")),
            () -> assertTrue(guidebook.contains("Use any Storage Book on a vanilla bookshelf")),
            () -> assertTrue(guidebook.contains("source coordinates and offset from you, not a snapshot")),
            () -> assertTrue(guidebook.contains("Only air is filled; occupied destinations and source-air cells stay unchanged")),
            () -> assertTrue(guidebook.contains("There is no undo")),
            () -> assertTrue(guidebook.contains("Binding consumes the charm; a ring holds up to 15")),
            () -> assertTrue(guidebook.contains("Red = temporary Silk Touch mining override")),
            () -> assertTrue(guidebook.contains("Yellow + Green breaks up to 25 x pair-strength connected growable blocks")),
            () -> assertTrue(guidebook.contains("it spares ores and other nonterrain blocks")),
            () -> assertTrue(guidebook.contains("It does not protect its wearer from damage")),
            () -> assertTrue(portingNotes.contains("### Representative Client-Facing Evidence")),
            () -> assertTrue(portingNotes.contains("creative tab now keeps every registered Aura item form reachable")),
            () -> assertTrue(portingNotes.contains("Storage Bookshelves come from using a storage book on a vanilla bookshelf")),
            () -> assertTrue(portingNotes.contains("workspace does not retain the earlier `live_client_validation/` replay archive")),
            () -> assertTrue(portingNotes.contains("localized `Monitor` item naming surface")),
            () -> assertTrue(portingNotes.contains("repo-owned asset and audit surface")),
            () -> assertTrue(portingNotes.contains("bookshelf_coordinator")),
            () -> assertTrue(portingNotes.contains("src/main/resources/data/aura/recipes/processor/")),
            () -> assertTrue(portingNotes.contains("`storage_bookshelf` stays a conversion-only block")),
            () -> assertTrue(portingNotes.contains("`aura_node_pump_creative` remains intentionally creative-only content")),
            () -> assertTrue(portingNotes.contains("`Yellow + Green` harvests mature crops in a 3x3 footprint")),
            () -> assertTrue(portingNotes.contains("consumer_block_loot")),
            () -> assertTrue(portingNotes.contains("aura_node_black")),
            () -> assertTrue(portingNotes.contains("VisualAssetParityAuditTest")),
            () -> assertTrue(portingNotes.contains("no longer reports Aura `Parsing error loading recipe` entries")),
            () -> assertEquals(-1, portingNotes.indexOf("run-76955964dbb9474f848a0dfb36ae5bd5/live_client_validation/")),
            () -> assertTrue(portingNotes.contains("## 1.21.11 Forward Port")),
            () -> assertTrue(portingNotes.contains("Implementation and acceptance are in progress; no target release is approved yet."))
        );
    }

    @Test
    void registeredAuraBlockItemsUseBlockTranslationPrefixes() {
        TestMinecraftBootstrap.AuraRegistrationSnapshot registrations = TestMinecraftBootstrap.auraRegistrationSnapshot();
        Set<String> leakingTranslationIds = registrations.blockIds().stream()
            .filter(registrations.itemIds()::contains)
            .filter(id -> !"block.aura.".concat(id).equals(TestMinecraftBootstrap.auraItemDescriptionSnapshot().descriptionIds().get(id)))
            .collect(Collectors.toCollection(TreeSet::new));

        assertAll(
            () -> assertTrue(
                leakingTranslationIds.isEmpty(),
                () -> "Aura block items still use non-block translation ids: " + leakingTranslationIds
            ),
            () -> assertEquals(
                "block.aura.monitor",
                TestMinecraftBootstrap.auraItemDescriptionSnapshot().descriptionIds().get("monitor")
            )
        );
    }

    private static void assertCraftingBackedBlock(String id, String expectedName, JsonObject lang) throws IOException {
        assertTrue(Files.exists(DATA_ROOT.resolve("recipe/" + id + ".json")));
        assertClientBlockSurface(id, expectedName, lang);
    }

    private static void assertProcessorBackedBlock(String id, String expectedName, String recipePath, JsonObject lang) throws IOException {
        assertCatalogRecipeBackedBlock(id, expectedName, recipePath, lang);
    }

    private static void assertVortexBackedBlock(String id, String expectedName, String recipePath, JsonObject lang) throws IOException {
        assertCatalogRecipeBackedBlock(id, expectedName, recipePath, lang);
    }

    private static void assertCatalogRecipeBackedBlock(String id, String expectedName, String recipePath, JsonObject lang) throws IOException {
        assertTrue(Files.exists(DATA_ROOT.resolve("recipes/" + recipePath)));
        assertTrue(Files.notExists(DATA_ROOT.resolve("recipe/" + id + ".json")));
        assertClientBlockSurface(id, expectedName, lang);
    }

    private static void assertConversionOnlyBlock(String id, String expectedName, JsonObject lang) throws IOException {
        assertTrue(Files.notExists(DATA_ROOT.resolve("recipe/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("blockstates/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("models/block/" + id + ".json")));
        assertTrue(Files.notExists(ASSET_ROOT.resolve("models/item/" + id + ".json")));
        assertTrue(Files.notExists(ASSET_ROOT.resolve("items/" + id + ".json")));
        assertTrue(Files.exists(DATA_ROOT.resolve("loot_table/blocks/" + id + ".json")));
        assertEquals(expectedName, lang.get("block.aura." + id).getAsString());
    }

    private static void assertCreativeOnlyBlock(String id, String expectedName, JsonObject lang) throws IOException {
        assertTrue(Files.notExists(DATA_ROOT.resolve("recipe/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("blockstates/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("models/block/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("models/item/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("items/" + id + ".json")));
        assertTrue(Files.exists(DATA_ROOT.resolve("loot_table/blocks/" + id + ".json")));
        assertEquals(expectedName, lang.get("block.aura." + id).getAsString());
    }

    private static void assertClientBlockSurface(String id, String expectedName, JsonObject lang) throws IOException {
        assertTrue(Files.exists(ASSET_ROOT.resolve("blockstates/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("models/block/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("models/item/" + id + ".json")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("items/" + id + ".json")));
        assertTrue(Files.exists(DATA_ROOT.resolve("loot_table/blocks/" + id + ".json")));
        assertEquals(expectedName, lang.get("block.aura." + id).getAsString());
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String readGuideEntries() throws IOException {
        Path entries = ASSET_ROOT.resolve("patchouli_books/encyclopedia_aura/en_us/entries");
        StringBuilder guidebook = new StringBuilder();
        try (var paths = Files.list(entries)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                guidebook.append(Files.readString(path, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return guidebook.toString();
    }
}
