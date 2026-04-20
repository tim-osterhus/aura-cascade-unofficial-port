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
        "minecraft:item/handheld"
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
