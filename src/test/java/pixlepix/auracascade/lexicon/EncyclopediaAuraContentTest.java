package pixlepix.auracascade.lexicon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EncyclopediaAuraContentTest {
    private static final Path BOOK_DATA = Path.of("src/main/resources/data/aura/patchouli_books/encyclopedia_aura/book.json");
    private static final Path BOOK_ASSETS = Path.of("src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us");
    private static final Pattern INTERNAL_LINK = Pattern.compile("\\$\\(l:([^)]*)\\)");

    @Test
    void patchouliBookRetainsTheAuraItemAndRequiredDependency() throws IOException {
        JsonObject book = readJson(BOOK_DATA);
        JsonObject fabric = readJson(Path.of("src/main/resources/fabric.mod.json"));
        JsonObject dependencies = fabric.getAsJsonObject("depends");

        assertAll(
            () -> assertEquals("item.aura.encyclopedia_aura", book.get("name").getAsString()),
            () -> assertEquals("aura:encyclopedia_aura", book.get("custom_book_item").getAsString()),
            () -> assertEquals("aura", fabric.get("id").getAsString()),
            () -> assertTrue(book.get("dont_generate_book").getAsBoolean()),
            () -> assertTrue(book.get("use_resource_pack").getAsBoolean()),
            () -> assertTrue(dependencies.has("patchouli")),
            () -> assertTrue(Files.exists(Path.of("src/main/resources/data/aura/recipe/encyclopedia_aura.json")))
        );
    }

    @Test
    void indexCategoriesEntriesAndInternalLinksResolve() throws IOException {
        JsonObject book = readJson(BOOK_DATA);
        Map<String, JsonObject> categories = readObjects(BOOK_ASSETS.resolve("categories"));
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));
        Set<String> expectedCategories = Set.of(
            "aura:walkthrough",
            "aura:aura_systems",
            "aura:storage_gear",
            "aura:fairies",
            "aura:enchantments",
            "aura:late_game"
        );
        Set<String> pilotEntries = Set.of(
            "aura:getting_started",
            "aura:white_aura_crystal",
            "aura:first_aura_circuit"
        );

        assertAll(
            () -> assertEquals(expectedCategories, categories.keySet()),
            () -> assertTrue(entries.keySet().containsAll(pilotEntries)),
            () -> assertTrue(book.get("landing_text").getAsString().contains("$(l:aura:walkthrough)")),
            () -> assertTrue(book.get("landing_text").getAsString().contains("$(l:aura:late_game)")),
            () -> assertEquals("aura:walkthrough", entries.get("aura:getting_started").get("category").getAsString()),
            () -> assertEquals("aura:walkthrough", entries.get("aura:white_aura_crystal").get("category").getAsString()),
            () -> assertEquals("aura:walkthrough", entries.get("aura:first_aura_circuit").get("category").getAsString())
        );

        for (Map.Entry<String, JsonObject> entry : entries.entrySet()) {
            assertTrue(categories.containsKey(entry.getValue().get("category").getAsString()), entry.getKey());
            assertTrue(entry.getValue().getAsJsonArray("pages").size() > 0, entry.getKey());
        }

        List<String> resourceStrings = new ArrayList<>();
        collectStrings(book, resourceStrings);
        categories.values().forEach(category -> collectStrings(category, resourceStrings));
        entries.values().forEach(entry -> collectStrings(entry, resourceStrings));
        assertInternalLinksResolve(resourceStrings, categories.keySet(), entries.keySet());
    }

    @Test
    void openingSpreadLinksToAllSixCategoriesAndThreeStarterEntries() throws IOException {
        String landingText = readJson(BOOK_DATA).get("landing_text").getAsString();
        String openingLinks = String.join("$(br)",
            "$(l:aura:walkthrough)Walkthrough$(/l)",
            "$(l:aura:aura_systems)Aura Systems$(/l)",
            "$(l:aura:storage_gear)Storage and Gear$(/l)",
            "$(l:aura:fairies)Fairies$(/l)",
            "$(l:aura:enchantments)Enchantments$(/l)",
            "$(l:aura:late_game)Late Systems$(/l)",
            "$(l:aura:getting_started)Getting Started$(/l)",
            "$(l:aura:white_aura_crystal)White Aura Crystal$(/l)",
            "$(l:aura:first_aura_circuit)First Circuit$(/l)"
        );
        JsonArray gettingStartedPages = readJson(BOOK_ASSETS.resolve("entries/getting_started.json"))
            .getAsJsonArray("pages");
        Matcher openingLinksMatcher = INTERNAL_LINK.matcher(landingText);
        int openingLinkCount = 0;
        while (openingLinksMatcher.find()) {
            openingLinkCount++;
        }

        assertTrue(landingText.startsWith(
            "One crystal opens a world of flowing Aura. $(br2)"
        ));
        assertTrue(landingText.endsWith("$(br2)" + openingLinks));
        assertEquals(9, openingLinkCount);
        assertEquals(
            "Begin with $(l:aura:white_aura_crystal)the White Aura Crystal$(/l), then continue to "
                + "$(l:aura:first_aura_circuit)the first circuit$(/l). The "
                + "$(l:aura:aura_systems)Aura Systems$(/l) section follows power through the network.",
            gettingStartedPages.get(2).getAsJsonObject().get("text").getAsString()
        );
    }

    @Test
    void everyGuideIconAndPageItemResolvesToARegisteredItem() throws IOException {
        TestMinecraftBootstrap.ensureBootstrapped();
        Set<String> auraItemPaths = TestMinecraftBootstrap.auraRegistrationSnapshot().itemIds();
        Map<String, JsonObject> categories = readObjects(BOOK_ASSETS.resolve("categories"));
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));

        for (Map.Entry<String, JsonObject> category : categories.entrySet()) {
            assertRegisteredItemReferences(category.getValue(), category.getKey(), auraItemPaths);
        }
        for (Map.Entry<String, JsonObject> entry : entries.entrySet()) {
            assertRegisteredItemReferences(entry.getValue(), entry.getKey(), auraItemPaths);
        }

        JsonObject book = readJson(BOOK_DATA);
        assertRegisteredItemReference(book.get("custom_book_item"), "book.custom_book_item", auraItemPaths);
    }

    @Test
    void pilotLinksToTheRegisteredCrystalAndEncyclopediaRecipes() throws IOException {
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));
        JsonObject gettingStarted = entries.get("aura:getting_started");
        JsonObject whiteCrystal = entries.get("aura:white_aura_crystal");
        JsonObject firstCircuit = entries.get("aura:first_aura_circuit");
        String gettingStartedText = strings(gettingStarted);
        String crystalText = strings(whiteCrystal);
        String circuitText = strings(firstCircuit);

        assertAll(
            () -> assertTrue(gettingStartedText.contains("$(l:aura:white_aura_crystal)")),
            () -> assertTrue(gettingStartedText.contains("$(l:aura:first_aura_circuit)")),
            () -> assertTrue(crystalText.contains("$(l:aura:first_aura_circuit)")),
            () -> assertTrue(circuitText.contains("$(l:aura:pumps_control)")),
            () -> assertTrue(circuitText.contains("$(l:aura:consumers_vortex)")),
            () -> assertTrue(hasRecipePage(gettingStarted, "aura:encyclopedia_aura")),
            () -> assertTrue(hasRecipePage(whiteCrystal, "aura:aura_crystal_white")),
            () -> assertTrue(Files.exists(Path.of("src/main/resources/data/aura/recipe/aura_crystal_white.json")))
        );

        String lowerCrystalText = crystalText.toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("former recipe", "previous recipe", "old recipe", "replacement", "replaced", "history", "iron", "prism", "new recipe")) {
            assertFalse(lowerCrystalText.contains(forbidden), "White Aura Crystal entry contains: " + forbidden);
        }
    }

    @Test
    void headingsUseTheShortFormsSelectedAfterLiveOverflowFindings() throws IOException {
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));
        assertEquals("Enchanting", entries.get("aura:kaleidoscopic_enchantments").get("name").getAsString());
        JsonArray pages = entries.get("aura:getting_started").getAsJsonArray("pages");
        for (JsonElement page : pages) {
            JsonObject content = page.getAsJsonObject();
            if ("patchouli:crafting".equals(content.get("type").getAsString())) {
                assertEquals("Encyclopedia Aura", content.get("title").getAsString());
            }
        }
    }

    @Test
    void clippedGuideTitlesUseShortDisplayLabelsWithoutChangingRecipeTargets() throws IOException {
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));
        for (String color : List.of("blue", "green", "orange", "violet", "yellow")) {
            assertEquals(color.substring(0, 1).toUpperCase(Locale.ROOT) + color.substring(1) + " Swords",
                entries.get("aura:angelsteel_swords_" + color).get("name").getAsString());
        }

        JsonArray amuletPages = entries.get("aura:accessory_amulets").getAsJsonArray("pages");
        assertEquals("Orange Amulet", amuletPages.get(4).getAsJsonObject().get("title").getAsString());
        assertEquals("aura:orange_protection_amulet", amuletPages.get(4).getAsJsonObject().get("recipe").getAsString());
        assertEquals("Yellow Amulet", amuletPages.get(5).getAsJsonObject().get("title").getAsString());
        assertEquals("aura:yellow_protection_amulet", amuletPages.get(5).getAsJsonObject().get("recipe").getAsString());
        assertEquals("Green Amulet", amuletPages.get(6).getAsJsonObject().get("title").getAsString());
        assertEquals("aura:green_protection_amulet", amuletPages.get(6).getAsJsonObject().get("recipe").getAsString());
        assertEquals("Violet Amulet", amuletPages.get(8).getAsJsonObject().get("title").getAsString());
        assertEquals("aura:violet_protection_amulet", amuletPages.get(8).getAsJsonObject().get("recipe").getAsString());

        assertEquals("Spawner and Nether", entries.get("aura:consumers_vortex").getAsJsonArray("pages")
            .get(12).getAsJsonObject().get("title").getAsString());
        assertEquals("Shattered Stone Ring", entries.get("aura:late_systems").getAsJsonArray("pages")
            .get(1).getAsJsonObject().get("title").getAsString());
        assertEquals("Projectile and Wire", entries.get("aura:pump_variants").getAsJsonArray("pages")
            .get(17).getAsJsonObject().get("title").getAsString());
    }

    @Test
    void migratedEntriesPreserveExistingFactsAndCorrectPowerTerminology() throws IOException {
        Map<String, JsonObject> entries = readObjects(BOOK_ASSETS.resolve("entries"));
        String guide = entries.values().stream().map(EncyclopediaAuraContentTest::strings).reduce("", (left, right) -> left + "\n" + right);

        assertAll(
            () -> assertTrue(guide.contains("Aura Basics")),
            () -> assertTrue(guide.contains("Pumps and Control")),
            () -> assertTrue(guide.contains("Consumers and Vortex")),
            () -> assertTrue(guide.contains("Storage Network")),
            () -> assertTrue(guide.contains("Fairy Charms")),
            () -> assertTrue(guide.contains("Enchant Matrix")),
            () -> assertTrue(guide.contains("Late Systems")),
            () -> assertTrue(guide.contains("Use a typed Fairy Charm while wearing a Ring of Binding")),
            () -> assertTrue(guide.contains("source coordinates and offset from you, not a snapshot")),
            () -> assertTrue(guide.contains("512 cells total, including air")),
            () -> assertTrue(guide.contains("Containers and machines keep no contents or settings from the copy")),
            () -> assertTrue(guide.contains("no fluid resources or buckets are saved")),
            () -> assertFalse(guide.contains("source fluids, and supported block entity data")),
            () -> assertTrue(guide.contains("Red = temporary Silk Touch mining override")),
            () -> assertTrue(guide.contains("Yellow + Green breaks up to 25 x pair-strength connected growable blocks")),
            () -> assertTrue(guide.contains("Red + Violet multiplies incoming player damage by 0.9^pair")),
            () -> assertFalse(guide.contains("harvests mature crops in a 3x3 footprint")),
            () -> assertFalse(guide.contains("heals back part of nonfatal incoming damage")),
            () -> assertFalse(guide.contains("Violet = hard-material haste window")),
            () -> assertTrue(guide.contains("Colored Aura storage and stored power are separate pools")),
            () -> assertTrue(guide.contains("Consumers draw that separate pool from adjacent blocks even when no work can be completed")),
            () -> assertTrue(guide.contains("first adjacent supported machine")),
            () -> assertTrue(guide.contains("Everything with an item form stays in the Aura creative tab")),
            () -> assertTrue(guide.contains("Use any Storage Book on a vanilla bookshelf")),
            () -> assertTrue(guide.contains("it spares ores and other nonterrain blocks")),
            () -> assertTrue(guide.contains("It does not protect its wearer from damage")),
            () -> assertFalse(guide.contains("current runtime expresses them as")),
            () -> assertFalse(guide.contains("[ ] Carry an Aura Crystal"))
        );
    }

    private static Map<String, JsonObject> readObjects(Path directory) throws IOException {
        Map<String, JsonObject> objects = new TreeMap<>();
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                String fileName = path.getFileName().toString();
                objects.put("aura:" + fileName.substring(0, fileName.length() - ".json".length()), readJson(path));
            }
        }
        return objects;
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String strings(JsonElement element) {
        List<String> values = new ArrayList<>();
        collectStrings(element, values);
        return String.join("\n", values);
    }

    private static void collectStrings(JsonElement element, List<String> values) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            values.add(element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectStrings(child, values);
            }
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> child : element.getAsJsonObject().entrySet()) {
                collectStrings(child.getValue(), values);
            }
        }
    }

    private static boolean hasRecipePage(JsonObject entry, String recipeId) {
        JsonArray pages = entry.getAsJsonArray("pages");
        for (JsonElement page : pages) {
            JsonObject pageObject = page.getAsJsonObject();
            if ("patchouli:crafting".equals(pageObject.get("type").getAsString())
                && recipeId.equals(pageObject.get("recipe").getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static void assertInternalLinksResolve(List<String> strings, Set<String> categories, Set<String> entries) {
        for (String text : strings) {
            Matcher matcher = INTERNAL_LINK.matcher(text);
            while (matcher.find()) {
                String destination = matcher.group(1).split("#", 2)[0];
                assertTrue(categories.contains(destination) || entries.contains(destination), "Unresolved Patchouli link: " + destination);
            }
        }
    }

    private static void assertRegisteredItemReferences(JsonElement element, String location, Set<String> auraItemPaths) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
                String fieldLocation = location + "." + field.getKey();
                if ("icon".equals(field.getKey()) || "item".equals(field.getKey())) {
                    assertRegisteredItemReference(field.getValue(), fieldLocation, auraItemPaths);
                }
                assertRegisteredItemReferences(field.getValue(), fieldLocation, auraItemPaths);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                assertRegisteredItemReferences(array.get(index), location + "[" + index + "]", auraItemPaths);
            }
        }
    }

    private static void assertRegisteredItemReference(JsonElement element, String location, Set<String> auraItemPaths) {
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(), location + " must be an item-id string");
        String itemId = element.getAsString();
        int componentSuffix = itemId.indexOf('[');
        if (componentSuffix >= 0) {
            itemId = itemId.substring(0, componentSuffix);
        }

        ResourceLocation id = ResourceLocation.parse(itemId);
        boolean registered = "aura".equals(id.getNamespace())
            ? auraItemPaths.contains(id.getPath())
            : BuiltInRegistries.ITEM.containsKey(id);
        assertTrue(registered, location + " references unregistered item " + id);
    }
}
