package pixlepix.auracascade.parity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;
import pixlepix.auracascade.client.AuraItemModels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualAssetParityAuditTest {
    private static final Path ASSET_ROOT = Path.of("src/main/resources/assets/aura");
    private static final String STORAGE_BOOKSHELF = "storage_bookshelf";
    private static final Set<String> ALLOWED_MINECRAFT_REFERENCES = Set.of(
        "minecraft:model",
        "minecraft:block/cube_all",
        "minecraft:block/cube_top",
        "minecraft:block/cube_bottom_top",
        "minecraft:block/orientable",
        "minecraft:item/generated",
        "minecraft:item/handheld",
        "minecraft:range_dispatch"
    );

    @Test
    void registeredAuraContentKeepsRestoredClientResources() {
        RegistrationSnapshot registrations = registeredAuraContent();
        Set<String> blockIds = registrations.blockIds();
        Set<String> itemIds = registrations.itemIds();

        assertFalse(blockIds.isEmpty(), "No Aura blocks were registered for the parity audit.");
        assertFalse(itemIds.isEmpty(), "No Aura items were registered for the parity audit.");
        assertTrue(blockIds.contains(STORAGE_BOOKSHELF), "Expected storage_bookshelf to remain part of the Aura block surface.");
        assertFalse(itemIds.contains(STORAGE_BOOKSHELF), "storage_bookshelf intentionally has no standalone Aura item registration.");

        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/blocks/node_top.png")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/items/mirror.png")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/blocks/aura_mob.png.mcmeta")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/block/node_top.png")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/item/mirror.png")));
        assertTrue(Files.exists(ASSET_ROOT.resolve("textures/block/aura_mob.png.mcmeta")));

        for (String blockId : blockIds) {
            assertTrue(Files.exists(ASSET_ROOT.resolve("blockstates/" + blockId + ".json")), () -> "Missing blockstate for " + blockId);
            assertTrue(Files.exists(ASSET_ROOT.resolve("models/block/" + blockId + ".json")), () -> "Missing block model for " + blockId);
        }

        for (String itemId : itemIds) {
            assertTrue(Files.exists(ASSET_ROOT.resolve("items/" + itemId + ".json")), () -> "Missing item definition for " + itemId);
            assertTrue(Files.exists(ASSET_ROOT.resolve("models/item/" + itemId + ".json")), () -> "Missing item model for " + itemId);
        }
    }

    @Test
    void restoredClientJsonUsesOnlyDocumentedMinecraftReferencesAndResolvableAuraTextures() throws IOException {
        List<Path> clientJson = new ArrayList<>();
        clientJson.addAll(listJson(ASSET_ROOT.resolve("blockstates")));
        clientJson.addAll(listJson(ASSET_ROOT.resolve("items")));
        clientJson.addAll(listJson(ASSET_ROOT.resolve("models")));

        for (Path jsonPath : clientJson) {
            JsonElement json = readJson(jsonPath);
            Set<String> minecraftReferences = new TreeSet<>();
            collectPrefixedStrings(json, "minecraft:", minecraftReferences);
            Set<String> unexpectedReferences = minecraftReferences.stream()
                .filter(reference -> !ALLOWED_MINECRAFT_REFERENCES.contains(reference))
                .collect(Collectors.toCollection(TreeSet::new));
            assertTrue(
                unexpectedReferences.isEmpty(),
                () -> "Unexpected minecraft: references in " + jsonPath + ": " + unexpectedReferences
            );

            if (jsonPath.startsWith(ASSET_ROOT.resolve("models")) && json.isJsonObject()) {
                assertAuraTexturesResolve(jsonPath, json.getAsJsonObject());
            }
        }
    }

    @Test
    void angelsteelCursesHaveLocalizedNamesAndOriginalTextureSprites() throws IOException {
        var sources = readJson(Path.of("src/main/resources/assets/minecraft/atlases/mob_effects.json"))
            .getAsJsonObject().getAsJsonArray("sources");
        var language = readJson(ASSET_ROOT.resolve("lang/en_us.json")).getAsJsonObject();
        Set<String> found = new TreeSet<>();
        for (var element : sources) {
            var source = element.getAsJsonObject();
            assertEquals("single", source.get("type").getAsString());
            String sprite = source.get("sprite").getAsString();
            assertTrue(found.add(sprite));
            assertTrue(language.has("effect." + sprite.replace(':', '.')));
            String texture = source.get("resource").getAsString().replace("aura:", "");
            assertTrue(Files.isRegularFile(ASSET_ROOT.resolve("textures/" + texture + ".png")));
        }
        assertEquals(Set.of("aura:angelsteel_curse_red", "aura:angelsteel_curse_orange",
            "aura:angelsteel_curse_yellow", "aura:angelsteel_curse_green",
            "aura:angelsteel_curse_blue", "aura:angelsteel_curse_violet"), found);
    }

    @Test
    void everyAngelsteelDegreeSelectsTheSixOriginalAttunementTextures() throws IOException {
        List<AuraColor> colors = List.of(AuraColor.RED, AuraColor.ORANGE, AuraColor.YELLOW,
            AuraColor.GREEN, AuraColor.BLUE, AuraColor.VIOLET);
        assertEquals(0.0F, AuraItemModels.attunementValue(AuraColor.WHITE));
        assertEquals(0.0F, AuraItemModels.attunementValue(AuraColor.BLACK));
        for (int degree = 1; degree <= 12; degree++) {
            JsonObject dispatch = readJson(ASSET_ROOT.resolve("items/angelsteel_sword_" + degree + ".json"))
                .getAsJsonObject().getAsJsonObject("model");
            assertEquals("minecraft:range_dispatch", dispatch.get("type").getAsString(), "degree " + degree);
            assertEquals("aura:attunement", dispatch.get("property").getAsString(), "degree " + degree);
            var entries = dispatch.getAsJsonArray("entries");
            assertEquals(colors.size(), entries.size(), "degree " + degree);
            for (int index = 0; index < colors.size(); index++) {
                AuraColor color = colors.get(index);
                JsonObject entry = entries.get(index).getAsJsonObject();
                assertEquals(AuraItemModels.attunementValue(color),
                    entry.get("threshold").getAsFloat());
                JsonObject modelDefinition = entry.getAsJsonObject("model");
                assertEquals("minecraft:model", modelDefinition.get("type").getAsString());
                assertEquals("aura:item/angelsteel_sword_" + color.id(), modelDefinition.get("model").getAsString());
                JsonObject model = readJson(ASSET_ROOT.resolve("models/item/angelsteel_sword_" + color.id() + ".json"))
                    .getAsJsonObject();
                assertEquals("aura:item/angel_sword_" + color.id(),
                    model.getAsJsonObject("textures").get("layer0").getAsString());
            }
            JsonObject fallback = dispatch.getAsJsonObject("fallback");
            assertEquals("minecraft:model", fallback.get("type").getAsString());
            assertEquals("aura:item/angelsteel_sword_" + degree, fallback.get("model").getAsString());
        }
    }

    @Test
    void accessoryKeyMappingsKeepTheirRegisteredCategoryTranslation() throws IOException {
        JsonObject language = readJson(ASSET_ROOT.resolve("lang/en_us.json")).getAsJsonObject();
        assertEquals("Aura Cascade", language.get("key.category.aura.aura").getAsString());
    }

    @Test
    void temporaryFairyLightHasAnExplicitInvisibleModel() throws IOException {
        JsonObject state = readJson(ASSET_ROOT.resolve("blockstates/fairy_torch.json")).getAsJsonObject();
        assertEquals("aura:block/fairy_torch",
            state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        JsonObject model = readJson(ASSET_ROOT.resolve("models/block/fairy_torch.json")).getAsJsonObject();
        assertTrue(model.getAsJsonArray("elements").isEmpty());
    }

    @Test
    void restoredAuraTextureMetadataRemainsValidJson() throws IOException {
        for (Path metadataPath : listFiles(ASSET_ROOT.resolve("textures"), ".mcmeta")) {
            readJson(metadataPath);
        }
    }

    private static RegistrationSnapshot registeredAuraContent() {
        TestMinecraftBootstrap.AuraRegistrationSnapshot registrations = TestMinecraftBootstrap.auraRegistrationSnapshot();
        return new RegistrationSnapshot(
            registrations.blockIds(),
            registrations.itemIds()
        );
    }

    private static List<Path> listJson(Path root) throws IOException {
        return listFiles(root, ".json");
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(suffix))
                .sorted()
                .toList();
        }
    }

    private static JsonElement readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void collectPrefixedStrings(JsonElement element, String prefix, Set<String> collector) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                collectPrefixedStrings(entry.getValue(), prefix, collector);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectPrefixedStrings(child, prefix, collector);
            }
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith(prefix)) {
                collector.add(value);
            }
        }
    }

    private static void assertAuraTexturesResolve(Path jsonPath, JsonObject json) throws IOException {
        JsonElement texturesElement = json.get("textures");
        if (texturesElement == null || !texturesElement.isJsonObject()) {
            return;
        }

        for (var entry : texturesElement.getAsJsonObject().entrySet()) {
            String value = entry.getValue().getAsString();
            if (value.startsWith("#") || !value.startsWith("aura:")) {
                continue;
            }

            assertFalse(
                value.startsWith("aura:items/") || value.startsWith("aura:blocks/"),
                () -> "Legacy plural Aura texture reference " + value + " found in " + jsonPath
            );

            Path texturePath = ASSET_ROOT.resolve("textures").resolve(value.substring("aura:".length()) + ".png");
            assertTrue(Files.exists(texturePath), () -> "Missing texture " + value + " referenced by " + jsonPath);
            assertMirroredFromLegacyAuraTexture(value, texturePath, jsonPath);
        }
    }

    private static void assertMirroredFromLegacyAuraTexture(String value, Path mirroredTexturePath, Path jsonPath) throws IOException {
        if (value.startsWith("aura:item/")) {
            assertMirroredFromLegacyAuraTexture(
                value,
                mirroredTexturePath,
                ASSET_ROOT.resolve("textures/items").resolve(value.substring("aura:item/".length()) + ".png"),
                jsonPath
            );
            return;
        }
        if (value.startsWith("aura:block/")) {
            assertMirroredFromLegacyAuraTexture(
                value,
                mirroredTexturePath,
                ASSET_ROOT.resolve("textures/blocks").resolve(value.substring("aura:block/".length()) + ".png"),
                jsonPath
            );
        }
    }

    private static void assertMirroredFromLegacyAuraTexture(
        String value,
        Path mirroredTexturePath,
        Path legacyTexturePath,
        Path jsonPath
    ) throws IOException {
        assertTrue(Files.exists(legacyTexturePath), () -> "Missing legacy Aura texture source " + value + " referenced by " + jsonPath);
        assertEquals(-1L, Files.mismatch(mirroredTexturePath, legacyTexturePath), () -> "Mirrored Aura texture diverged for " + value);

        Path mirroredMetadata = Path.of(mirroredTexturePath + ".mcmeta");
        Path legacyMetadata = Path.of(legacyTexturePath + ".mcmeta");
        assertEquals(
            Files.exists(legacyMetadata),
            Files.exists(mirroredMetadata),
            () -> "Aura metadata mirror mismatch for " + value
        );
        if (Files.exists(legacyMetadata)) {
            assertEquals(-1L, Files.mismatch(mirroredMetadata, legacyMetadata), () -> "Mirrored Aura metadata diverged for " + value);
        }
    }

    private record RegistrationSnapshot(Set<String> blockIds, Set<String> itemIds) {
    }
}
