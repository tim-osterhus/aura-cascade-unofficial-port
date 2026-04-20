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
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraVortexRecipeCatalog {
    private static final String[] RESOURCE_PATHS = {
        "data/aura/recipes/vortex/consumer_block_loot.json",
        "data/aura/recipes/vortex/consumer_block_spawn.json",
        "data/aura/recipes/vortex/kaleidoscopic_enchanter.json",
        "data/aura/recipes/vortex/fluxing_node.json",
        "data/aura/recipes/vortex/prismatic_processor.json",
        "data/aura/recipes/vortex/ritual_nether.json"
    };

    private static volatile List<AuraVortexRecipe> cachedRecipes;

    private AuraVortexRecipeCatalog() {
    }

    public static List<AuraVortexRecipe> all() {
        List<AuraVortexRecipe> local = cachedRecipes;
        if (local == null) {
            synchronized (AuraVortexRecipeCatalog.class) {
                local = cachedRecipes;
                if (local == null) {
                    local = loadAll();
                    cachedRecipes = local;
                }
            }
        }
        return local;
    }

    private static List<AuraVortexRecipe> loadAll() {
        ArrayList<AuraVortexRecipe> recipes = new ArrayList<>();
        ClassLoader classLoader = AuraVortexRecipeCatalog.class.getClassLoader();
        for (String resourcePath : RESOURCE_PATHS) {
            InputStream stream = classLoader.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IllegalStateException("Missing vortex recipe resource: " + resourcePath);
            }

            try (InputStream inputStream = stream; InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                recipes.add(parse(resourcePath, json));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load vortex recipe resource: " + resourcePath, exception);
            }
        }
        return List.copyOf(recipes);
    }

    private static AuraVortexRecipe parse(String resourcePath, JsonObject json) {
        JsonArray ingredientsJson = json.getAsJsonArray("ingredients");
        ArrayList<AuraVortexRecipe.Component> components = new ArrayList<>();
        for (int index = 0; index < ingredientsJson.size(); index++) {
            JsonObject ingredientJson = ingredientsJson.get(index).getAsJsonObject();
            Item item = itemById(ingredientJson.get("item").getAsString(), resourcePath);
            int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;

            JsonObject auraJson = ingredientJson.getAsJsonObject("aura");
            AuraStorage auraRequirement = AuraStorage.of(
                AuraColor.byId(auraJson.get("color").getAsString()),
                auraJson.get("amount").getAsInt()
            );
            components.add(new AuraVortexRecipe.Component(item, count, auraRequirement));
        }

        JsonObject resultJson = json.getAsJsonObject("result");
        Item resultItem = itemById(resultJson.get("item").getAsString(), resourcePath);
        int resultCount = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;
        int maxProgress = json.has("max_progress") ? json.get("max_progress").getAsInt() : 100;

        return new AuraVortexRecipe(
            resourcePath,
            components,
            new ItemStack(resultItem, resultCount),
            maxProgress
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
