package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraVortexRecipeCatalogTest {
    @Test
    void builtInVortexRecipesCoverTheCoreLateGameUnlocks() throws IOException {
        List<VortexRecipeShape> recipes;
        try (var pathStream = Files.walk(Path.of("src/main/resources/data/aura/recipes/vortex"))) {
            recipes = pathStream
                .filter(path -> path.toString().endsWith(".json"))
                .map(AuraVortexRecipeCatalogTest::readRecipeShape)
                .toList();
        }

        assertEquals(13, recipes.size());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.ingredientCount() == 4));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.maxProgress() > 0));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_enchant")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_loot")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_spawn")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:ritual_nether")));
        assertEquals(Set.of(
            "aura:consumer_block_enchant", "aura:consumer_block_loot", "aura:consumer_block_spawn", "aura:ritual_nether",
            "aura:arcane_gem_white", "aura:arcane_gem_black", "aura:arcane_gem_orange", "aura:arcane_gem_red",
            "aura:arcane_gem_yellow", "aura:arcane_gem_green", "aura:arcane_gem_blue", "aura:arcane_gem_violet",
            "aura:ring_of_binding"
        ), recipes.stream().map(VortexRecipeShape::resultId).collect(java.util.stream.Collectors.toSet()));
        for (String color : List.of("white", "black", "orange", "red", "yellow", "green", "blue", "violet")) {
            assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:arcane_gem_" + color)));
            assertGemRecipe(color);
        }
        assertRepeatedGemRecipe("kaleidoscopic_enchanter", "aura:arcane_gem_black", "white", 250000);
        assertRepeatedGemRecipe("consumer_block_loot", "aura:arcane_gem_yellow", "yellow", 100000);
        assertRepeatedGemRecipe("consumer_block_spawn", "aura:arcane_gem_violet", "violet", 100000);
        assertRepeatedGemRecipe("ritual_nether", "aura:arcane_gem_red", "red", 100000);
    }

    private static void assertRepeatedGemRecipe(String name, String ingredientId, String color, int power) throws IOException {
        Path path = Path.of("src/main/resources/data/aura/recipes/vortex/" + name + ".json");
        JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray ingredients = json.getAsJsonArray("ingredients");
        assertEquals(4, ingredients.size());
        for (int index = 0; index < 4; index++) {
            JsonObject ingredient = ingredients.get(index).getAsJsonObject();
            assertEquals(ingredientId, ingredient.get("item").getAsString());
            assertEquals(color, ingredient.getAsJsonObject("aura").get("color").getAsString());
            assertEquals(power, ingredient.getAsJsonObject("aura").get("amount").getAsInt());
        }
    }

    private static void assertGemRecipe(String color) throws IOException {
        Path path = Path.of("src/main/resources/data/aura/recipes/vortex/arcane_gem_" + color + ".json");
        JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray ingredients = json.getAsJsonArray("ingredients");
        assertEquals("minecraft:diamond", ingredients.get(0).getAsJsonObject().get("item").getAsString());
        assertEquals(60000, ingredients.get(0).getAsJsonObject().getAsJsonObject("aura").get("amount").getAsInt());
        for (int index = 1; index < 4; index++) {
            JsonObject ingredient = ingredients.get(index).getAsJsonObject();
            assertEquals("aura:arcane_ingot_" + color, ingredient.get("item").getAsString());
            assertEquals("white", ingredient.getAsJsonObject("aura").get("color").getAsString());
            assertEquals(20000, ingredient.getAsJsonObject("aura").get("amount").getAsInt());
        }
    }

    private static VortexRecipeShape readRecipeShape(Path path) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            return new VortexRecipeShape(
                json.getAsJsonObject("result").get("item").getAsString(),
                ingredients.size(),
                json.has("max_progress") ? json.get("max_progress").getAsInt() : 100
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read vortex recipe resource " + path, exception);
        }
    }

    private record VortexRecipeShape(String resultId, int ingredientCount, int maxProgress) {
    }
}
