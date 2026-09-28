package pixlepix.auracascade.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.component.CustomData;

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
            throw new AssertionError("Legacy raw-string ingredient payload is not accepted by the 1.21.1 crafting codec: " + element);
        }
        if (!element.isJsonObject()) {
            throw new AssertionError("Unsupported ingredient payload: " + element);
        }

        JsonObject ingredient = element.getAsJsonObject();
        if (ingredient.has("type")) {
            String type = stringValue(required(ingredient, "type"), "ingredient type");
            if ("aura:custom_data".equals(type)) {
                List<String> itemIds = readItemSet(required(ingredient, "items"));
                var decoded = CustomData.CODEC.parse(JsonOps.INSTANCE, required(ingredient, "nbt"));
                var data = decoded.result().orElseThrow(() ->
                    new AssertionError("Invalid custom-data ingredient NBT: " + decoded.error()));
                if (data.isEmpty()) {
                    throw new AssertionError("Custom-data ingredient NBT cannot be empty");
                }
                return itemIds;
            }
            if (!"neoforge:components".equals(type)) {
                throw new AssertionError("Unsupported custom ingredient type: " + type);
            }
            List<String> itemIds = readItemSet(required(ingredient, "items"));
            JsonElement strict = ingredient.get("strict");
            if (strict != null && (!strict.isJsonPrimitive() || !strict.getAsJsonPrimitive().isBoolean())) {
                throw new AssertionError("Ingredient strict flag must be a boolean: " + ingredient);
            }

            TestMinecraftBootstrap.ensureBootstrapped();
            var ops = RegistryOps.create(JsonOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            var decoded = DataComponentPredicate.CODEC.parse(ops, required(ingredient, "components"));
            var predicate = decoded.result().orElseThrow(() ->
                new AssertionError("Invalid ingredient components: " + decoded.error()));
            if (predicate.alwaysMatches()) {
                throw new AssertionError("Components ingredient must define at least one component");
            }
            return itemIds;
        }
        if (ingredient.has("item")) {
            return List.of(stringValue(ingredient.get("item"), "item"));
        }
        if (ingredient.has("tag")) {
            return List.of("#" + stringValue(ingredient.get("tag"), "tag"));
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

    private static List<String> readItemSet(JsonElement element) {
        if (element.isJsonArray()) {
            if (element.getAsJsonArray().isEmpty()) {
                throw new AssertionError("Component ingredient items cannot be empty");
            }
            ArrayList<String> ids = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                ids.add(itemSetEntry(item));
            }
            return ids;
        }
        return List.of(itemSetEntry(element));
    }

    private static String itemSetEntry(JsonElement element) {
        String id = stringValue(element, "component ingredient item");
        if (id.isBlank()) {
            throw new AssertionError("Component ingredient item cannot be blank");
        }
        return id;
    }

    private static String stringValue(JsonElement element, String field) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new AssertionError("Ingredient field '" + field + "' must be a string: " + element);
        }
        return element.getAsString();
    }
}
