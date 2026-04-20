package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AuraWorldRecipeCatalog {
    private static final String[] RESOURCE_PATHS = {
        "data/aura/recipes/processor/arcane_ingot_white.json",
        "data/aura/recipes/processor/arcane_ingot_black.json",
        "data/aura/recipes/processor/arcane_ingot_orange.json",
        "data/aura/recipes/processor/arcane_ingot_red.json",
        "data/aura/recipes/processor/arcane_ingot_yellow.json",
        "data/aura/recipes/processor/arcane_ingot_green.json",
        "data/aura/recipes/processor/arcane_ingot_blue.json",
        "data/aura/recipes/processor/arcane_ingot_violet.json",
        "data/aura/recipes/processor/arcane_gem_white.json",
        "data/aura/recipes/processor/arcane_gem_black.json",
        "data/aura/recipes/processor/arcane_gem_orange.json",
        "data/aura/recipes/processor/arcane_gem_red.json",
        "data/aura/recipes/processor/arcane_gem_yellow.json",
        "data/aura/recipes/processor/arcane_gem_green.json",
        "data/aura/recipes/processor/arcane_gem_blue.json",
        "data/aura/recipes/processor/arcane_gem_violet.json",
        "data/aura/recipes/processor/fortified_cobblestone.json",
        "data/aura/recipes/processor/fortified_stone.json",
        "data/aura/recipes/processor/fortified_planks.json",
        "data/aura/recipes/processor/fortified_glass.json",
        "data/aura/recipes/processor/fortified_obsidian.json",
        "data/aura/recipes/processor/fortified_dirt.json",
        "data/aura/recipes/synthesizer/arcane_prism.json"
    };

    private static volatile List<AuraWorldRecipe> cachedRecipes;

    private AuraWorldRecipeCatalog() {
    }

    public static List<AuraWorldRecipe> all() {
        List<AuraWorldRecipe> local = cachedRecipes;
        if (local == null) {
            synchronized (AuraWorldRecipeCatalog.class) {
                local = cachedRecipes;
                if (local == null) {
                    local = loadAll();
                    cachedRecipes = local;
                }
            }
        }
        return local;
    }

    private static List<AuraWorldRecipe> loadAll() {
        ArrayList<AuraWorldRecipe> recipes = new ArrayList<>();
        ClassLoader classLoader = AuraWorldRecipeCatalog.class.getClassLoader();
        for (String resourcePath : RESOURCE_PATHS) {
            InputStream stream = classLoader.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalStateException("Missing aura recipe resource: " + resourcePath);
            }

            try (InputStream inputStream = stream; InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                recipes.add(parse(resourcePath, json));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load aura recipe resource: " + resourcePath, exception);
            }
        }
        return List.copyOf(recipes);
    }

    private static AuraWorldRecipe parse(String resourcePath, JsonObject json) {
        String type = json.get("type").getAsString();
        AuraWorldRecipe.RecipeKind recipeKind = switch (type) {
            case "aura:processor" -> AuraWorldRecipe.RecipeKind.PROCESSOR;
            case "aura:synthesizer" -> AuraWorldRecipe.RecipeKind.SYNTHESIZER;
            default -> throw new IllegalStateException("Unsupported aura recipe type in " + resourcePath + ": " + type);
        };

        JsonArray ingredientsJson = json.getAsJsonArray("ingredients");
        ArrayList<AuraWorldRecipe.Ingredient> ingredients = new ArrayList<>();
        for (int index = 0; index < ingredientsJson.size(); index++) {
            JsonObject ingredientJson = ingredientsJson.get(index).getAsJsonObject();
            Item item = itemById(ingredientJson.get("item").getAsString(), resourcePath);
            int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;
            ingredients.add(new AuraWorldRecipe.Ingredient(item, count));
        }

        JsonObject resultJson = json.getAsJsonObject("result");
        Item resultItem = itemById(resultJson.get("item").getAsString(), resourcePath);
        int resultCount = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;
        boolean prismaticOnly = json.has("prismatic_only") && json.get("prismatic_only").getAsBoolean();

        return new AuraWorldRecipe(
            resourcePath,
            recipeKind,
            prismaticOnly,
            ingredients,
            new ItemStack(resultItem, resultCount)
        );
    }

    private static Item itemById(String id, String resourcePath) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Unknown item " + id + " in " + resourcePath);
        }
        return item;
    }
}
