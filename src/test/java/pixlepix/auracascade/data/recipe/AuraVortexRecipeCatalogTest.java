package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

        assertEquals(6, recipes.size());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.ingredientCount() == 4));
        assertTrue(recipes.stream().allMatch(recipe -> recipe.maxProgress() > 0));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_enchant")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:aura_node_flux")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_ore_adv")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_loot")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:consumer_block_spawn")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:ritual_nether")));
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
