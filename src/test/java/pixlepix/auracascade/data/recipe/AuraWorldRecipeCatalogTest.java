package pixlepix.auracascade.data.recipe;

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

final class AuraWorldRecipeCatalogTest {
    @Test
    void builtInProcessorAndSynthRecipesLoadFromResources() throws IOException {
        List<AuraRecipeShape> recipes;
        try (var pathStream = Files.walk(Path.of("src/main/resources/data/aura/recipes"))) {
            recipes = pathStream
                .filter(path -> path.toString().endsWith(".json"))
                .map(AuraWorldRecipeCatalogTest::readRecipeShape)
                .filter(recipe -> recipe.type().equals("aura:processor") || recipe.type().equals("aura:synthesizer"))
                .toList();
        }

        assertEquals(23, recipes.size());
        assertEquals(8, recipes.stream().filter(AuraRecipeShape::prismaticOnly).count());
        assertEquals(1, recipes.stream().filter(recipe -> recipe.type().equals("aura:synthesizer")).count());
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:arcane_prism")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:fortified_obsidian")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:fortified_planks")));
    }

    private static AuraRecipeShape readRecipeShape(Path path) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            return new AuraRecipeShape(
                json.get("type").getAsString(),
                json.has("prismatic_only") && json.get("prismatic_only").getAsBoolean(),
                json.getAsJsonObject("result").get("item").getAsString()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read recipe resource " + path, exception);
        }
    }

    private record AuraRecipeShape(String type, boolean prismaticOnly, String resultId) {
    }
}
