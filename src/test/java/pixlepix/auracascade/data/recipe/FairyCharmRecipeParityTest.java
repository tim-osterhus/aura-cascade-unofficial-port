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
import static org.junit.jupiter.api.Assertions.assertFalse;

final class FairyCharmRecipeParityTest {
    private static final Path CRAFTING_ROOT = Path.of("src/main/resources/data/aura/recipe");
    private static final List<RoleIngredient> ROLE_INGREDIENTS = List.of(
        item("fighter", "minecraft:golden_sword"),
        potion("debuffer", "minecraft:poison"),
        potion("buffer", "minecraft:long_regeneration"),
        item("stealer", "minecraft:fishing_rod"),
        item("pusher", "minecraft:snowball"),
        item("shooter", "minecraft:arrow"),
        item("savior", "minecraft:diamond_sword"),
        item("fetcher", "minecraft:chest"),
        item("baiter", "minecraft:egg"),
        item("breeder", "minecraft:wheat"),
        item("scarer", "minecraft:gunpowder"),
        item("extinguisher", "minecraft:water_bucket"),
        item("digger", "minecraft:golden_pickaxe"),
        item("lighter", "minecraft:white_wool"),
        item("glider", "minecraft:torch"),
        item("trainer", "minecraft:lapis_block")
    );

    @Test
    void allLegacyCharmRolesHaveTheirOwnTypedShapedRecipe() throws IOException {
        JsonObject base = read(CRAFTING_ROOT.resolve("fairy_charm.json"));
        assertEquals("minecraft:crafting_shaped", base.get("type").getAsString());
        assertEquals(List.of("BGB"), strings(base.getAsJsonArray("pattern")));
        assertEquals("minecraft:brick", base.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("aura:arcane_gem_white", base.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertRoleOutput(base, "fairy");

        assertEquals(17, countCraftingCharmRecipes());
        for (RoleIngredient expected : ROLE_INGREDIENTS) {
            JsonObject recipe = read(CRAFTING_ROOT.resolve("fairy_charm_" + expected.role() + ".json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), expected.role());
            assertEquals(List.of(" X ", "XCX", " X "), strings(recipe.getAsJsonArray("pattern")), expected.role());

            JsonObject key = recipe.getAsJsonObject("key");
            JsonObject baseCharm = key.getAsJsonObject("C");
            assertEquals("fabric:custom_data", baseCharm.get("fabric:type").getAsString(), expected.role());
            assertEquals("aura:fairy_charm", baseCharm.getAsJsonObject("base").get("item").getAsString(), expected.role());
            assertEquals("fairy", baseCharm.getAsJsonObject("nbt").get("fairyRole").getAsString(), expected.role());
            assertExpectedIngredient(key.getAsJsonObject("X"), expected);
            assertRoleOutput(recipe, expected.role());
        }
    }

    @Test
    void bindingRingUsesOnlyItsOriginalFourColorVortexRecipe() throws IOException {
        Path oldCraftingRecipe = CRAFTING_ROOT.resolve("ring_of_binding.json");
        assertFalse(Files.exists(oldCraftingRecipe));

        JsonObject recipe = read(Path.of("src/main/resources/data/aura/recipes/vortex/ring_of_binding.json"));
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        List<String> items = List.of(
            "minecraft:diamond_block", "minecraft:obsidian", "minecraft:gold_block", "minecraft:redstone_block"
        );
        List<String> colors = List.of("blue", "white", "yellow", "red");
        assertEquals(4, ingredients.size());
        for (int index = 0; index < ingredients.size(); index++) {
            JsonObject ingredient = ingredients.get(index).getAsJsonObject();
            assertEquals(items.get(index), ingredient.get("item").getAsString());
            assertEquals(colors.get(index), ingredient.getAsJsonObject("aura").get("color").getAsString());
            assertEquals(50000, ingredient.getAsJsonObject("aura").get("amount").getAsInt());
        }
        assertEquals("aura:ring_of_binding", recipe.getAsJsonObject("result").get("item").getAsString());
    }

    private static void assertExpectedIngredient(JsonObject ingredient, RoleIngredient expected) {
        if (expected.potion() == null) {
            assertEquals(expected.item(), ingredient.get("item").getAsString(), expected.role());
            return;
        }

        assertEquals("fabric:components", ingredient.get("fabric:type").getAsString(), expected.role());
        assertEquals("minecraft:potion", ingredient.getAsJsonObject("base").get("item").getAsString(), expected.role());
        assertEquals(
            expected.potion(),
            ingredient.getAsJsonObject("components")
                .getAsJsonObject("minecraft:potion_contents")
                .get("potion")
                .getAsString(),
            expected.role()
        );
    }

    private static void assertRoleOutput(JsonObject recipe, String role) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("aura:fairy_charm", result.get("id").getAsString());
        assertEquals(role, result.getAsJsonObject("components")
            .getAsJsonObject("minecraft:custom_data")
            .get("fairyRole")
            .getAsString());
    }

    private static int countCraftingCharmRecipes() throws IOException {
        try (var paths = Files.list(CRAFTING_ROOT)) {
            return (int) paths
                .filter(path -> path.getFileName().toString().startsWith("fairy_charm"))
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .count();
        }
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(element -> element.getAsString()).toList();
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static RoleIngredient item(String role, String item) {
        return new RoleIngredient(role, item, null);
    }

    private static RoleIngredient potion(String role, String potion) {
        return new RoleIngredient(role, null, potion);
    }

    private record RoleIngredient(String role, String item, String potion) {
    }
}
