package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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

        assertEquals(44, recipes.size());
        assertEquals(16, recipes.stream().filter(AuraRecipeShape::prismaticOnly).count());
        assertEquals(0, recipes.stream().filter(recipe -> recipe.type().equals("aura:synthesizer")).count());
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:arcane_prism")
            && recipe.type().equals("aura:processor") && !recipe.prismaticOnly()));
        assertTrue(recipes.stream().noneMatch(recipe -> recipe.resultId().startsWith("aura:arcane_gem_")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:fortified_obsidian")));
        assertTrue(recipes.stream().anyMatch(recipe -> recipe.resultId().equals("aura:fortified_planks")));
    }

    @Test
    void everyLegacyWoolColorMakesItsOriginalIngotWithIronAndNoCrystal() throws IOException {
        Map<String, List<String>> woolByAura = Map.of(
            "white", List.of("white"), "black", List.of("black", "brown", "gray", "light_gray"),
            "orange", List.of("orange"), "red", List.of("red"), "yellow", List.of("yellow"),
            "green", List.of("green", "lime"), "blue", List.of("blue", "light_blue", "cyan"),
            "violet", List.of("purple", "magenta", "pink")
        );
        Set<String> actual = new HashSet<>();
        int count = 0;
        for (JsonObject recipe : processorRecipes()) {
            String output = recipe.getAsJsonObject("result").get("item").getAsString();
            if (!output.startsWith("aura:arcane_ingot_")) {
                continue;
            }
            count++;
            assertTrue(!recipe.has("prismatic_only") || !recipe.get("prismatic_only").getAsBoolean());
            assertEquals(2, recipe.getAsJsonArray("ingredients").size());
            List<String> inputs = ingredients(recipe);
            assertTrue(inputs.contains("minecraft:iron_ingot"));
            String wool = inputs.stream().filter(id -> id.endsWith("_wool")).findFirst().orElseThrow();
            actual.add(output + "=" + wool);
        }
        Set<String> expected = new HashSet<>();
        woolByAura.forEach((aura, colors) -> colors.forEach(color ->
            expected.add("aura:arcane_ingot_" + aura + "=minecraft:" + color + "_wool")));
        assertEquals(16, count);
        assertEquals(expected, actual);
    }

    @Test
    void advancedWoolRecipesPreserveLegacyDyeItemsRatherThanModernSubstitutes() throws IOException {
        List<String> colors = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
        Map<String, String> special = Map.of("white", "bone_meal", "blue", "lapis_lazuli",
            "brown", "cocoa_beans", "black", "ink_sac");
        Set<String> actual = new HashSet<>();
        for (JsonObject recipe : processorRecipes()) {
            if (!recipe.has("prismatic_only") || !recipe.get("prismatic_only").getAsBoolean()) {
                continue;
            }
            assertEquals(1, recipe.getAsJsonArray("ingredients").size());
            actual.add(ingredients(recipe).getFirst() + "=" + recipe.getAsJsonObject("result").get("item").getAsString());
        }
        Set<String> expected = new HashSet<>();
        for (String color : colors) {
            expected.add("minecraft:" + color + "_wool=minecraft:" + special.getOrDefault(color, color + "_dye"));
        }
        assertEquals(expected, actual);
    }

    @Test
    void fortificationUsesEndStoneAndAllSixLegacyPlanks() throws IOException {
        Set<String> actual = new HashSet<>();
        int count = 0;
        for (JsonObject recipe : processorRecipes()) {
            String output = recipe.getAsJsonObject("result").get("item").getAsString();
            if (!output.startsWith("aura:fortified_")) {
                continue;
            }
            count++;
            assertTrue(!recipe.has("prismatic_only") || !recipe.get("prismatic_only").getAsBoolean());
            List<String> inputs = ingredients(recipe);
            assertEquals(2, inputs.size());
            assertTrue(inputs.contains("minecraft:end_stone"));
            actual.add(output + "=" + inputs.stream().filter(id -> !id.equals("minecraft:end_stone")).findFirst().orElseThrow());
        }
        Set<String> expected = new HashSet<>();
        for (String base : List.of("dirt", "glass", "cobblestone", "stone", "obsidian")) {
            expected.add("aura:fortified_" + base + "=minecraft:" + base);
        }
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak")) {
            expected.add("aura:fortified_planks=minecraft:" + wood + "_planks");
        }
        assertEquals(11, count);
        assertEquals(expected, actual);
    }

    @Test
    void catalogRegistersEveryProcessorResourceExactlyOnce() throws IOException {
        String catalog = Files.readString(Path.of("src/main/java/pixlepix/auracascade/data/recipe/AuraWorldRecipeCatalog.java"));
        var matcher = java.util.regex.Pattern.compile("data/aura/recipes/processor/[^\\\"]+\\.json").matcher(catalog);
        Set<String> registered = new HashSet<>();
        while (matcher.find()) {
            assertTrue(registered.add(matcher.group()), "Duplicate catalog entry: " + matcher.group());
        }
        try (var files = Files.list(Path.of("src/main/resources/data/aura/recipes/processor"))) {
            Set<String> resources = files.filter(path -> path.toString().endsWith(".json"))
                .map(path -> "data/aura/recipes/processor/" + path.getFileName())
                .collect(java.util.stream.Collectors.toSet());
            assertEquals(resources, registered);
        }
    }

    private static List<JsonObject> processorRecipes() throws IOException {
        try (var files = Files.list(Path.of("src/main/resources/data/aura/recipes/processor"))) {
            return files.filter(path -> path.toString().endsWith(".json")).map(path -> {
                try {
                    JsonObject recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    assertEquals("aura:processor", recipe.get("type").getAsString());
                    assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
                    return recipe;
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            }).toList();
        }
    }

    private static List<String> ingredients(JsonObject recipe) {
        return java.util.stream.StreamSupport.stream(recipe.getAsJsonArray("ingredients").spliterator(), false)
            .map(element -> {
                assertEquals(1, element.getAsJsonObject().get("count").getAsInt());
                return element.getAsJsonObject().get("item").getAsString();
            }).toList();
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
