package pixlepix.auracascade.parity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KaleidoscopicEnchantableTagAuditTest {
    private static final Path ENCHANTMENT_ROOT = Path.of("src/main/resources/data/aura/enchantment");
    private static final Path ENCHANTABLE_TAG = Path.of("src/main/resources/data/aura/tags/item/kaleidoscopic_enchantable.json");
    private static final String SHARED_ENCHANTABLE_TAG = "#aura:kaleidoscopic_enchantable";
    private static final Set<String> ENCHANTMENTS = Set.of(
        "kaleidoscopic_blue",
        "kaleidoscopic_green",
        "kaleidoscopic_orange",
        "kaleidoscopic_red",
        "kaleidoscopic_violet",
        "kaleidoscopic_yellow"
    );

    @Test
    void auraEnchantableTagResolvesAgainstMinecraft1211VanillaItemTags() throws IOException {
        JsonArray values = readJson(ENCHANTABLE_TAG).getAsJsonArray("values");
        Set<String> referencedTags = new HashSet<>();
        Set<String> resolvedItems = new HashSet<>();

        for (JsonElement value : values) {
            String entry = value.getAsString();
            if (entry.startsWith("#")) {
                String tagId = entry.substring(1);
                referencedTags.add(entry);
                resolvedItems.addAll(resolveItemTag(tagId, new HashSet<>()));
            } else {
                resolvedItems.add(entry);
            }
        }

        assertEquals(Set.of("#minecraft:enchantable/mining", "#minecraft:enchantable/weapon"), referencedTags);
        assertTrue(resolvedItems.contains("minecraft:diamond_pickaxe"));
        assertTrue(resolvedItems.contains("minecraft:diamond_sword"));
        assertTrue(resolvedItems.contains("minecraft:diamond_axe"));
        assertTrue(resolvedItems.contains("minecraft:mace"));
        assertTrue(resolvedItems.contains("aura:angelsteel_pickaxe_1"));
    }

    @Test
    void mergedRootIncludesExactlyTheFiftyLegacyEligibleAuraToolsAndSwords() throws IOException {
        Set<String> resolved = resolveItemTag("aura:kaleidoscopic_enchantable", new HashSet<>());
        Set<String> auraItems = resolved.stream().filter(id -> id.startsWith("aura:")).collect(Collectors.toSet());
        Set<String> expected = new HashSet<>();
        for (String kind : new String[] {"pickaxe", "axe", "shovel", "sword"}) {
            expected.addAll(angelsteelIds(kind));
        }
        expected.add("aura:sword_of_the_thief");
        expected.add("aura:sword_of_the_barbarian");
        assertEquals(50, expected.size());
        assertEquals(expected, auraItems);
        assertFalse(resolved.contains("aura:transmuting_sword"), "592 Transmuting Sword extends Item, not ItemSword");
        assertFalse(resolved.contains("aura:ring_of_binding"));
        assertFalse(resolved.contains("aura:arcane_ingot_red"));
        assertFalse(resolved.contains("aura:prismatic_wand"));
        Set<String> registered = TestMinecraftBootstrap.auraRegistrationSnapshot().itemIds();
        for (String id : expected) {
            assertTrue(registered.contains(id.substring("aura:".length())), "Unregistered item " + id);
        }
    }

    @Test
    void categoryAdditionsAreExplicitNonReplacingAndDoNotMixToolKinds() throws IOException {
        for (String kind : new String[] {"pickaxe", "axe", "shovel", "sword"}) {
            JsonObject tag = readJson(Path.of("src/main/resources/data/minecraft/tags/item", kind + "s.json"));
            assertFalse(tag.get("replace").getAsBoolean(), kind);
            Set<String> expected = angelsteelIds(kind);
            if (kind.equals("sword")) {
                expected.add("aura:sword_of_the_thief");
                expected.add("aura:sword_of_the_barbarian");
            }
            Set<String> values = new HashSet<>();
            for (JsonElement entry : tag.getAsJsonArray("values")) {
                assertTrue(values.add(entry.getAsString()), "Duplicate entry in " + kind);
            }
            assertEquals(expected, values, kind);
            assertTrue(resolveItemTag("minecraft:" + kind + "s", new HashSet<>()).contains("minecraft:diamond_" + kind));
        }
    }

    @Test
    void allSixEnchantmentsUseTheSharedTagForPrimaryAndSupportedItems() throws IOException {
        Set<String> actualEnchantments;
        try (Stream<Path> files = Files.list(ENCHANTMENT_ROOT)) {
            actualEnchantments = files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("kaleidoscopic_") && name.endsWith(".json"))
                .map(name -> name.substring(0, name.length() - ".json".length()))
                .collect(Collectors.toSet());
        }
        assertEquals(ENCHANTMENTS, actualEnchantments);

        for (String enchantment : ENCHANTMENTS) {
            JsonObject definition = readJson(ENCHANTMENT_ROOT.resolve(enchantment + ".json"));
            assertEquals(SHARED_ENCHANTABLE_TAG, definition.get("primary_items").getAsString(), enchantment);
            assertEquals(SHARED_ENCHANTABLE_TAG, definition.get("supported_items").getAsString(), enchantment);
        }
    }

    private static Set<String> angelsteelIds(String kind) {
        Set<String> ids = new HashSet<>();
        for (int degree = 1; degree <= 12; degree++) {
            ids.add("aura:angelsteel_" + kind + "_" + degree);
        }
        return ids;
    }

    private static Set<String> resolveItemTag(String tagId, Set<String> resolving) throws IOException {
        if (!resolving.add(tagId)) {
            throw new IOException("Cyclic item tag reference: " + tagId);
        }

        int separator = tagId.indexOf(':');
        if (separator < 1) {
            throw new IOException("Expected a namespaced item tag: " + tagId);
        }
        String resourcePath = "data/" + tagId.substring(0, separator)
            + "/tags/item/" + tagId.substring(separator + 1) + ".json";
        try {
            Set<String> entries = new HashSet<>();
            if (tagId.startsWith("minecraft:")) {
                addEntries(entries, readVanillaTag(resourcePath));
            }
            Path overlay = Path.of("src/main/resources", resourcePath);
            if (Files.exists(overlay)) {
                addEntries(entries, readJson(overlay));
            }
            assertFalse(entries.isEmpty(), "Missing/empty item tag " + tagId);
            Set<String> items = new HashSet<>();
            for (String entry : entries) {
                if (entry.startsWith("#")) {
                    items.addAll(resolveItemTag(entry.substring(1), resolving));
                } else {
                    items.add(entry);
                }
            }
            return items;
        } finally {
            resolving.remove(tagId);
        }
    }

    private static void addEntries(Set<String> entries, JsonObject tag) {
        if (tag.has("replace") && tag.get("replace").getAsBoolean()) {
            entries.clear();
        }
        for (JsonElement value : tag.getAsJsonArray("values")) {
            entries.add(value.getAsString());
        }
    }

    private static JsonObject readVanillaTag(String resourcePath) throws IOException {
        // ModDevGradle places vanilla data in a separate Minecraft resources JAR.
        var resources = Thread.currentThread().getContextClassLoader().getResources(resourcePath);
        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            if (resource.toExternalForm().contains("client-extra-aka-minecraft-resources.jar")) {
                try (InputStream input = resource.openStream()) {
                    return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
        }
        throw new IOException("Missing vanilla Minecraft JAR tag resource " + resourcePath);
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
