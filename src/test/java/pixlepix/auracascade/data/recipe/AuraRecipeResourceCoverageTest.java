package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.RecipeIngredientReader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraRecipeResourceCoverageTest {
    private static final List<String> AURA_COLORS = List.of("white", "black", "orange", "red", "yellow", "green", "blue", "violet");
    private static final List<String> ANGELSTEEL_TOOL_KINDS = List.of("pickaxe", "axe", "shovel", "sword");
    private static final Set<String> KNOWN_OUTPUTS = knownOutputs();

    @Test
    void vanillaAndCustomProgressionRecipesResolveRegisteredOutputs() throws IOException {
        assertTrue(checkRecipeTree(Path.of("src/main/resources/data/aura/recipe"), false));
        assertTrue(checkRecipeTree(Path.of("src/main/resources/data/aura/recipes"), true));
    }

    private static boolean checkRecipeTree(Path root, boolean customRecipes) throws IOException {
        try (Stream<Path> pathStream = Files.walk(root)) {
            List<Path> jsonFiles = pathStream.filter(path -> path.toString().endsWith(".json")).toList();
            assertFalse(jsonFiles.isEmpty());
            for (Path jsonFile : jsonFiles) {
                JsonObject json = JsonParser.parseString(Files.readString(jsonFile, StandardCharsets.UTF_8)).getAsJsonObject();
                if (customRecipes) {
                    assertResolves(json.getAsJsonObject("result").get("item").getAsString());
                    JsonArray ingredients = json.getAsJsonArray("ingredients");
                    for (JsonElement ingredient : ingredients) {
                        assertIngredient(ingredient.getAsJsonObject().get("item").getAsString());
                    }
                } else {
                    assertResolves(json.getAsJsonObject("result").get("id").getAsString());
                    if (json.has("ingredients")) {
                        JsonArray ingredients = json.getAsJsonArray("ingredients");
                        for (JsonElement ingredient : ingredients) {
                            for (String ingredientId : RecipeIngredientReader.readIngredientIds(ingredient)) {
                                assertIngredient(ingredientId);
                            }
                        }
                    }
                    if (json.has("key")) {
                        JsonObject key = json.getAsJsonObject("key");
                        for (String symbol : key.keySet()) {
                            for (String ingredientId : RecipeIngredientReader.readIngredientIds(key.get(symbol))) {
                                assertIngredient(ingredientId);
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private static void assertIngredient(String id) {
        if (id.startsWith("#")) {
            return;
        }
        assertTrue(id.startsWith("minecraft:") || KNOWN_OUTPUTS.contains(id));
    }

    private static void assertResolves(String id) {
        assertTrue(KNOWN_OUTPUTS.contains(id));
    }

    private static Set<String> knownOutputs() {
        LinkedHashSet<String> outputs = new LinkedHashSet<>();
        for (String color : AURA_COLORS) {
            outputs.add("aura:aura_crystal_" + color);
            outputs.add("aura:arcane_ingot_" + color);
            outputs.add("aura:arcane_gem_" + color);
        }

        outputs.addAll(List.of(
            "minecraft:bone_meal", "minecraft:orange_dye", "minecraft:magenta_dye", "minecraft:light_blue_dye",
            "minecraft:yellow_dye", "minecraft:lime_dye", "minecraft:pink_dye", "minecraft:gray_dye",
            "minecraft:light_gray_dye", "minecraft:cyan_dye", "minecraft:purple_dye", "minecraft:lapis_lazuli",
            "minecraft:cocoa_beans", "minecraft:green_dye", "minecraft:red_dye", "minecraft:ink_sac",
            "aura:arcane_prism",
            "aura:encyclopedia_aura",
            "aura:fairy_charm",
            "aura:ring_of_binding",
            "aura:ring_of_shattered_stone",
            "aura:amulet_of_the_angels_wing",
            "aura:sash_of_the_angels_heels",
            "aura:amulet_of_the_forbidden_fruit",
            "aura:red_protection_amulet",
            "aura:orange_protection_amulet",
            "aura:yellow_protection_amulet",
            "aura:green_protection_amulet",
            "aura:blue_protection_amulet",
            "aura:violet_protection_amulet",
            "aura:mirror_of_the_angel",
            "aura:portable_red_hole",
            "aura:portable_black_hole",
            "aura:prismatic_wand",
            "aura:basic_storage_book",
            "aura:dense_storage_book",
            "aura:very_dense_storage_book",
            "aura:super_dense_storage_book",
            "aura:extremely_dense_storage_book",
            "aura:light_storage_book",
            "aura:very_light_storage_book",
            "aura:super_light_storage_book",
            "aura:extremely_light_storage_book",
            "aura:mineral_storage_book",
            "aura:mob_storage_book",
            "aura:farming_storage_book",
            "aura:mod_storage_book",
            "aura:transmuting_sword",
            "aura:sword_of_the_thief",
            "aura:sword_of_the_barbarian",
            "aura:aura_node",
            "aura:aura_node_black",
            "aura:aura_node_orange",
            "aura:aura_node_capacitor",
            "aura:aura_node_conserve",
            "aura:aura_node_flux",
            "aura:aura_node_pump",
            "aura:aura_node_pump_alt",
            "aura:aura_node_pump_light",
            "aura:aura_node_pump_light_alt",
            "aura:aura_node_pump_fall",
            "aura:aura_node_pump_fall_alt",
            "aura:aura_node_pump_projectile",
            "aura:aura_node_pump_projectile_alt",
            "aura:aura_node_pump_redstone",
            "aura:aura_node_pump_redstone_alt",
            "aura:consumer_block_furnace",
            "aura:consumer_block_plant",
            "aura:consumer_block_fish",
            "aura:consumer_block_potion",
            "aura:consumer_block_dye",
            "aura:consumer_block_ore",
            "aura:consumer_block_ore_adv",
            "aura:consumer_block_angel",
            "aura:consumer_block_enchant",
            "aura:consumer_block_loot",
            "aura:consumer_block_spawn",
            "aura:consumer_block_miner",
            "aura:aura_node_crafting_center",
            "aura:aura_node_crafting_pedestal",
            "aura:bookshelf_coordinator",
            "aura:monitor",
            "aura:travelers_bricks",
            "aura:rebounding_enigma",
            "aura:fortified_cobblestone",
            "aura:fortified_stone",
            "aura:fortified_planks",
            "aura:fortified_glass",
            "aura:fortified_obsidian",
            "aura:fortified_dirt",
            "aura:ritual_nether",
            "aura:ritual_end"
        ));

        for (int degree = 1; degree <= 12; degree++) {
            outputs.add("aura:angelsteel_ingot_" + degree);
            for (String kind : ANGELSTEEL_TOOL_KINDS) {
                outputs.add("aura:angelsteel_" + kind + "_" + degree);
            }
        }

        return Set.copyOf(outputs);
    }
}
