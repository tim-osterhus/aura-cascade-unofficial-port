package pixlepix.auracascade.support;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RecipeIngredientReaderTest {
    @Test
    void vanillaIngredientsReadItemAndTagStringsAndAlternatives() {
        assertEquals(List.of("minecraft:amethyst_shard"), read("\"minecraft:amethyst_shard\""));
        assertEquals(List.of("#minecraft:planks"), read("\"#minecraft:planks\""));
        assertEquals(List.of("minecraft:iron_ingot", "#minecraft:planks"), read("""
            ["minecraft:iron_ingot", "#minecraft:planks"]
            """));
    }

    @Test
    void vanillaIngredientsRejectNonStringAndLegacyObjectPayloads() {
        for (String json : List.of(
            "42",
            "true",
            "\"\"",
            "\"#\"",
            "{\"item\":\"minecraft:iron_ingot\"}",
            "{\"tag\":\"minecraft:planks\"}"
        )) {
            assertThrows(AssertionError.class, () -> read(json), json);
        }
    }

    @Test
    void customDataReadsEveryNestedBaseAndAcceptsObjectOrSnbtPredicates() {
        assertEquals(List.of("aura:fairy_charm", "#aura:charms"), read("""
            {"fabric:type":"fabric:custom_data",
             "base":["aura:fairy_charm","#aura:charms"],
             "nbt":{"fairyRole":"fairy"}}
            """));
        assertEquals(List.of("aura:fairy_charm"), read("""
            {"fabric:type":"fabric:custom_data",
             "base":{"fabric:type":"fabric:custom_data",
                     "base":"aura:fairy_charm","nbt":{"fairyRole":"fairy"}},
             "nbt":"{fairyRole:fairy}"}
            """));
    }

    @Test
    void customDataRejectsMissingOrMalformedBaseAndNbt() {
        for (String json : List.of(
            """
            {"fabric:type":"fabric:custom_data","nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":[],"nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":{"unknown":"aura:fairy_charm"},"nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":"aura:fairy_charm"}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":"aura:fairy_charm","nbt":{}}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":"aura:fairy_charm","nbt":[]}
            """,
            """
            {"fabric:type":"fabric:custom_data","base":"aura:fairy_charm","nbt":"{fairyRole:"}
            """
        )) {
            assertThrows(AssertionError.class, () -> read(json), json);
        }
    }

    @Test
    void componentsValidateRegisteredPotionContents() {
        assertEquals(List.of("minecraft:potion"), read("""
            {"fabric:type":"fabric:components","base":"minecraft:potion",
             "components":{"minecraft:potion_contents":{"potion":"minecraft:long_regeneration"}}}
            """));
    }

    @Test
    void componentsRejectMissingEmptyAndInvalidPredicates() {
        for (String fields : List.of(
            "",
            ",\"components\":{}",
            ",\"components\":[]",
            ",\"components\":{\"minecraft:unknown_component\":{}}",
            ",\"components\":{\"minecraft:potion_contents\":{\"potion\":\"minecraft:unknown_potion\"}}",
            ",\"components\":{\"minecraft:potion_contents\":{\"potion\":42}}"
        )) {
            String json = "{\"fabric:type\":\"fabric:components\",\"base\":\"minecraft:potion\""
                + fields + "}";
            assertThrows(AssertionError.class, () -> read(json), json);
        }
    }

    @Test
    void unknownCustomTypesCannotBypassValidationWithAnItemField() {
        assertThrows(AssertionError.class, () -> read("""
            {"fabric:type":"fabric:unknown","item":"minecraft:potion","base":"minecraft:potion"}
            """));
    }

    private static List<String> read(String json) {
        return RecipeIngredientReader.readIngredientIds(JsonParser.parseString(json));
    }
}
