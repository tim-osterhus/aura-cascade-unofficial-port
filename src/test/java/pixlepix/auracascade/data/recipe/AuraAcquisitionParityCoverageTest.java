package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraAcquisitionParityCoverageTest {
    private static final Path CRAFTING_ROOT = Path.of("src/main/resources/data/aura/recipe");
    private static final Path WORLD_ROOT = Path.of("src/main/resources/data/aura/recipes");

    @Test
    void restoredCraftingAcquisitionRecipesStayPresentAndStable() throws IOException {
        assertAll(
            () -> assertCraftingRecipe(
                "aura_node_black.json",
                "aura:aura_node_black",
                List.of(" I ", " N ", " I "),
                Set.of("aura:arcane_ingot_black", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "aura_node_capacitor.json",
                "aura:aura_node_capacitor",
                List.of(" Y ", " N ", " G "),
                Set.of("aura:arcane_ingot_yellow", "aura:arcane_ingot_green", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "aura_node_conserve.json",
                "aura:aura_node_conserve",
                List.of(" B ", " N ", " B "),
                Set.of("aura:arcane_ingot_blue", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "aura_node_orange.json",
                "aura:aura_node_orange",
                List.of(" O ", " N ", " O "),
                Set.of("aura:arcane_ingot_orange", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "monitor.json",
                "aura:monitor",
                List.of("RRR", "RNR", "RRR"),
                Set.of("minecraft:redstone", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump.json",
                "aura:aura_node_pump",
                List.of("ILI", "INI", "ILI"),
                Set.of("minecraft:iron_ingot", "minecraft:magma_cream", "aura:aura_node")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_alt.json",
                "aura:aura_node_pump_alt",
                List.of(" E ", "EPE", " E "),
                Set.of("minecraft:redstone", "aura:aura_node_pump")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_light.json",
                "aura:aura_node_pump_light",
                List.of("TTT", "YPY", "Y Y"),
                Set.of("minecraft:torch", "aura:arcane_ingot_yellow", "aura:aura_node_pump")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_light_alt.json",
                "aura:aura_node_pump_light_alt",
                List.of(" E ", "EPE", " E "),
                Set.of("minecraft:redstone", "aura:aura_node_pump_light")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_fall.json",
                "aura:aura_node_pump_fall",
                List.of("FFF", "BPB"),
                Set.of("minecraft:feather", "aura:arcane_ingot_blue", "aura:aura_node_pump")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_fall_alt.json",
                "aura:aura_node_pump_fall_alt",
                List.of(" E ", "EPE", " E "),
                Set.of("minecraft:redstone", "aura:aura_node_pump_fall")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_projectile.json",
                "aura:aura_node_pump_projectile",
                List.of("AAA", "VPV"),
                Set.of("minecraft:arrow", "aura:arcane_ingot_violet", "aura:aura_node_pump")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_projectile_alt.json",
                "aura:aura_node_pump_projectile_alt",
                List.of(" E ", "EPE", " E "),
                Set.of("minecraft:redstone", "aura:aura_node_pump_projectile")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_redstone.json",
                "aura:aura_node_pump_redstone",
                List.of("BBB", "RPR", "R R"),
                Set.of("minecraft:redstone_block", "aura:arcane_ingot_red", "aura:aura_node_pump")
            ),
            () -> assertCraftingRecipe(
                "aura_node_pump_redstone_alt.json",
                "aura:aura_node_pump_redstone_alt",
                List.of(" E ", "EPE", " E "),
                Set.of("minecraft:redstone", "aura:aura_node_pump_redstone")
            ),
            () -> assertCraftingRecipe(
                "travelers_bricks.json",
                "aura:travelers_bricks",
                List.of("BBB", "BIB", "BBB"),
                Set.of("minecraft:nether_bricks", "aura:arcane_ingot_black")
            ),
            () -> assertCraftingRecipe(
                "rebounding_enigma.json",
                "aura:rebounding_enigma",
                List.of(" S ", "SIS", " S "),
                Set.of("minecraft:slime_ball", "aura:arcane_ingot_violet")
            ),
            () -> assertCraftingRecipe(
                "consumer_block_miner.json",
                "aura:consumer_block_miner",
                List.of("PDP", "IRI", "IRI"),
                Set.of("aura:arcane_prism", "minecraft:diamond_pickaxe", "minecraft:iron_ingot", "aura:portable_red_hole")
            ),
            () -> assertCraftingRecipe(
                "ritual_end.json",
                "aura:ritual_end",
                List.of("EPE", "ENE", "EEE"),
                Set.of("minecraft:obsidian", "aura:arcane_prism", "aura:ritual_nether")
            )
        );
    }

    @Test
    void restoredProcessorAndVortexAcquisitionRecipesStayPresentAndStable() throws IOException {
        assertAll(
            () -> assertWorldRecipe(
                "processor/fortified_cobblestone.json",
                "aura:fortified_cobblestone",
                List.of("minecraft:obsidian", "minecraft:cobblestone")
            ),
            () -> assertWorldRecipe(
                "processor/fortified_stone.json",
                "aura:fortified_stone",
                List.of("minecraft:obsidian", "minecraft:stone")
            ),
            () -> assertWorldRecipe(
                "processor/fortified_planks.json",
                "aura:fortified_planks",
                List.of("minecraft:obsidian", "minecraft:oak_planks")
            ),
            () -> assertWorldRecipe(
                "processor/fortified_glass.json",
                "aura:fortified_glass",
                List.of("minecraft:obsidian", "minecraft:glass")
            ),
            () -> assertWorldRecipe(
                "processor/fortified_obsidian.json",
                "aura:fortified_obsidian",
                List.of("minecraft:obsidian", "minecraft:obsidian")
            ),
            () -> assertWorldRecipe(
                "processor/fortified_dirt.json",
                "aura:fortified_dirt",
                List.of("minecraft:obsidian", "minecraft:dirt")
            ),
            () -> assertWorldRecipe(
                "vortex/consumer_block_loot.json",
                "aura:consumer_block_loot",
                List.of("aura:consumer_block_ore_adv", "aura:sword_of_the_thief", "aura:arcane_prism", "aura:arcane_gem_yellow")
            ),
            () -> assertWorldRecipe(
                "vortex/consumer_block_spawn.json",
                "aura:consumer_block_spawn",
                List.of("aura:consumer_block_loot", "aura:mob_storage_book", "aura:arcane_prism", "aura:arcane_gem_violet")
            ),
            () -> assertWorldRecipe(
                "vortex/ritual_nether.json",
                "aura:ritual_nether",
                List.of("aura:consumer_block_spawn", "aura:arcane_prism", "aura:arcane_gem_red", "aura:arcane_gem_orange")
            )
        );
    }

    @Test
    void creativePumpRemainsCreativeOnlyAndPortingNotesDropTheOldGapList() throws IOException {
        assertFalse(Files.exists(CRAFTING_ROOT.resolve("aura_node_pump_creative.json")));

        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);
        assertAll(
            () -> assertTrue(portingNotes.contains("aura_node_pump_creative")),
            () -> assertTrue(portingNotes.contains("creative-only")),
            () -> assertFalse(portingNotes.contains("No direct data-driven acquisition definition is shipped yet")),
            () -> assertFalse(portingNotes.contains("No direct data-driven survival acquisition definition is shipped yet"))
        );
    }

    private static void assertCraftingRecipe(String fileName, String resultId, List<String> pattern, Set<String> ingredientIds) throws IOException {
        JsonObject json = readJson(CRAFTING_ROOT.resolve(fileName));
        assertEquals(resultId, json.getAsJsonObject("result").get("id").getAsString());

        List<String> actualPattern = new ArrayList<>();
        for (JsonElement row : json.getAsJsonArray("pattern")) {
            actualPattern.add(row.getAsString());
        }
        assertEquals(pattern, actualPattern);

        assertEquals(ingredientIds, readCraftingIngredients(json));
    }

    private static void assertWorldRecipe(String relativePath, String resultId, List<String> ingredientIds) throws IOException {
        JsonObject json = readJson(WORLD_ROOT.resolve(relativePath));
        assertEquals(resultId, json.getAsJsonObject("result").get("item").getAsString());
        assertEquals(ingredientIds, readWorldIngredients(json));
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Set<String> readCraftingIngredients(JsonObject json) {
        JsonObject key = json.getAsJsonObject("key");
        java.util.LinkedHashSet<String> ingredients = new java.util.LinkedHashSet<>();
        for (String symbol : key.keySet()) {
            JsonElement value = key.get(symbol);
            if (value.isJsonArray()) {
                for (JsonElement option : value.getAsJsonArray()) {
                    ingredients.add(option.getAsString());
                }
            } else {
                ingredients.add(value.getAsString());
            }
        }
        return Set.copyOf(ingredients);
    }

    private static List<String> readWorldIngredients(JsonObject json) {
        JsonArray ingredients = json.getAsJsonArray("ingredients");
        ArrayList<String> result = new ArrayList<>();
        for (JsonElement ingredient : ingredients) {
            result.add(ingredient.getAsJsonObject().get("item").getAsString());
        }
        return result;
    }
}
