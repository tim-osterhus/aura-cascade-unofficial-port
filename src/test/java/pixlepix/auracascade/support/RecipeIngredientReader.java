package pixlepix.auracascade.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;

public final class RecipeIngredientReader {
    private RecipeIngredientReader() {
    }

    public static List<String> readIngredientIds(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            throw new AssertionError("Missing ingredient payload");
        }
        if (element.isJsonArray()) {
            if (element.getAsJsonArray().isEmpty()) {
                throw new AssertionError("Ingredient alternatives cannot be empty");
            }
            ArrayList<String> ids = new ArrayList<>();
            for (JsonElement option : element.getAsJsonArray()) {
                ids.addAll(readIngredientIds(option));
            }
            return ids;
        }
        if (element.isJsonPrimitive()) {
            if (!element.getAsJsonPrimitive().isString()) {
                throw new AssertionError("Ingredient item and tag IDs must be strings: " + element);
            }
            String id = element.getAsString();
            if (id.isBlank() || id.equals("#")) {
                throw new AssertionError("Ingredient item and tag IDs cannot be empty: " + element);
            }
            return List.of(id);
        }
        if (!element.isJsonObject()) {
            throw new AssertionError("Unsupported ingredient payload: " + element);
        }

        JsonObject ingredient = element.getAsJsonObject();
        if (ingredient.has("fabric:type")) {
            String type = ingredient.get("fabric:type").getAsString();
            List<String> baseIds = readIngredientIds(ingredient.get("base"));
            switch (type) {
                case "fabric:custom_data" -> {
                    var decoded = TagParser.LENIENT_CODEC.parse(JsonOps.INSTANCE, required(ingredient, "nbt"));
                    var nbt = decoded.result().orElseThrow(() ->
                        new AssertionError("Invalid custom_data NBT: " + decoded.error()));
                    if (nbt.isEmpty()) {
                        throw new AssertionError("Custom_data ingredient NBT cannot be empty");
                    }
                }
                case "fabric:components" -> {
                    TestMinecraftBootstrap.ensureBootstrapped();
                    var ops = RegistryOps.create(JsonOps.INSTANCE,
                        RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
                    var decoded = DataComponentPatch.CODEC.parse(ops, required(ingredient, "components"));
                    var components = decoded.result().orElseThrow(() ->
                        new AssertionError("Invalid ingredient components: " + decoded.error()));
                    if (components.isEmpty()) {
                        throw new AssertionError("Components ingredient must define at least one component");
                    }
                }
                default -> throw new AssertionError("Unsupported custom ingredient type: " + type);
            }
            return baseIds;
        }
        throw new AssertionError("Unsupported ingredient payload: " + ingredient);
    }

    private static JsonElement required(JsonObject ingredient, String field) {
        JsonElement value = ingredient.get(field);
        if (value == null || value.isJsonNull()) {
            throw new AssertionError("Missing custom ingredient field '" + field + "': " + ingredient);
        }
        return value;
    }
}
