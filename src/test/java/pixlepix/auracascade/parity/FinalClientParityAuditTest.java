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
import pixlepix.auracascade.lexicon.EncyclopediaAuraContent;
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
            () -> assertCraftingBackedBlock("aura_node_black", "Aura Manipulator: Black", lang),
            () -> assertCraftingBackedBlock("aura_node_capacitor", "Aura Capacitor", lang),
            () -> assertCraftingBackedBlock("aura_node_conserve", "Conserving Aura Node", lang),
            () -> assertCraftingBackedBlock("aura_node_orange", "Aura Manipulator: Orange", lang),
            () -> assertCraftingBackedBlock("monitor", "Monitor", lang),
            () -> assertCraftingBackedBlock("travelers_bricks", "Traveler's Bricks", lang),
            () -> assertCraftingBackedBlock("rebounding_enigma", "Rebounding Enigma", lang),
            () -> assertCraftingBackedBlock("consumer_block_miner", "Cascading Miner", lang),
            () -> assertCraftingBackedBlock("ritual_end", "Ritual of the End", lang),
            () -> assertWorldRecipeBackedBlock("consumer_block_loot", "Cascading Looter", "vortex/consumer_block_loot.json", lang),
            () -> assertWorldRecipeBackedBlock("consumer_block_spawn", "Cascading Spawner", "vortex/consumer_block_spawn.json", lang),
            () -> assertWorldRecipeBackedBlock("ritual_nether", "Ritual of the Nether", "vortex/ritual_nether.json", lang)
        );
    }

    @Test
    void guidebookAndParityNotesStayTruthfulAboutClosureSliceResiduals() throws IOException {
        TestMinecraftBootstrap.ensureBootstrapped();

        String guidebook = EncyclopediaAuraContent.createBook(null)
            .getPages(false)
            .stream()
            .map(page -> page.getString())
            .collect(Collectors.joining("\n"));
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertTrue(guidebook.contains("monitor reads nearby aura strength")),
            () -> assertTrue(guidebook.contains("Aura creative tab")),
            () -> assertTrue(guidebook.contains("Use a Storage Book on a vanilla bookshelf")),
            () -> assertTrue(guidebook.contains("skips block entities and fluids")),
            () -> assertTrue(guidebook.contains("plain Fairy")),
            () -> assertTrue(guidebook.contains("Red = Silk Touch")),
            () -> assertTrue(guidebook.contains("Yellow + Green harvests crops")),
            () -> assertTrue(guidebook.contains("does not yet restrict explosion damage to dirt, stone, sand, and gravel")),
            () -> assertTrue(portingNotes.contains("### Representative Client-Facing Evidence")),
            () -> assertTrue(portingNotes.contains("creative tab now keeps every registered Aura item form reachable")),
            () -> assertTrue(portingNotes.contains("Storage Bookshelves come from using a storage book on a vanilla bookshelf")),
            () -> assertTrue(portingNotes.contains("fresh live-client evidence for this closure slice is archived under `millrace-agents/runs/run-76955964dbb9474f848a0dfb36ae5bd5/live_client_validation/`")),
            () -> assertTrue(portingNotes.contains("localized `Monitor` replacement text")),
            () -> assertTrue(portingNotes.contains("no Aura-side `Missing textures in model`, `Unable to parse metadata from aura:`, failed-model, missing-model, or `item.aura.monitor` log hits")),
            () -> assertTrue(portingNotes.contains("`Yellow + Green` crop harvest")),
            () -> assertTrue(portingNotes.contains("consumer_block_loot")),
            () -> assertTrue(portingNotes.contains("aura_node_black"))
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

    private static void assertWorldRecipeBackedBlock(String id, String expectedName, String recipePath, JsonObject lang) throws IOException {
        assertTrue(Files.exists(DATA_ROOT.resolve("recipes/" + recipePath)));
        assertClientBlockSurface(id, expectedName, lang);
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
}
