package pixlepix.auracascade.support;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RecipeIngredientReaderTest {
    @Test
    void customDataIngredientsReadItemSetsAndAcceptObjectOrSnbtNbt() {
        assertEquals(List.of("aura:fairy_charm", "#aura:charms"), read("""
            {"type":"aura:custom_data",
             "items":["aura:fairy_charm","#aura:charms"],
             "nbt":{"fairyRole":"fairy"}}
            """));
        assertEquals(List.of("aura:fairy_charm"), read("""
            {"type":"aura:custom_data","items":"aura:fairy_charm",
             "nbt":"{fairyRole:fairy}"}
            """));
    }

    @Test
    void customDataIngredientsRejectMissingOrMalformedItemsAndNbt() {
        for (String json : List.of(
            """
            {"type":"aura:custom_data","nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"type":"aura:custom_data","items":[],"nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"type":"aura:custom_data","items":["aura:fairy_charm",42],"nbt":{"fairyRole":"fairy"}}
            """,
            """
            {"type":"aura:custom_data","items":"aura:fairy_charm"}
            """,
            """
            {"type":"aura:custom_data","items":"aura:fairy_charm","nbt":{}}
            """,
            """
            {"type":"aura:custom_data","items":"aura:fairy_charm","nbt":[]}
            """,
            """
            {"type":"aura:custom_data","items":"aura:fairy_charm","nbt":"{fairyRole:"}
            """
        )) {
            assertThrows(AssertionError.class, () -> read(json), json);
        }
    }

    @Test
    void componentIngredientsValidateRegisteredPotionContents() {
        assertEquals(List.of("minecraft:potion"), read("""
            {"type":"neoforge:components","items":"minecraft:potion",
             "components":{"minecraft:potion_contents":{"potion":"minecraft:long_regeneration"}},
             "strict":false}
            """));
    }

    @Test
    void componentIngredientsRejectMissingEmptyAndInvalidPredicates() {
        for (String fields : List.of(
            "",
            ",\"components\":{}",
            ",\"components\":[]",
            ",\"components\":{\"minecraft:unknown_component\":{}}",
            ",\"components\":{\"minecraft:potion_contents\":{\"potion\":\"minecraft:unknown_potion\"}}",
            ",\"components\":{\"minecraft:potion_contents\":{\"potion\":42}}",
            ",\"components\":{\"minecraft:potion_contents\":{\"potion\":\"minecraft:poison\"}},\"strict\":\"false\""
        )) {
            String json = "{\"type\":\"neoforge:components\",\"items\":\"minecraft:potion\""
                + fields + "}";
            assertThrows(AssertionError.class, () -> read(json), json);
        }
    }

    @Test
    void unknownIngredientTypesCannotBypassValidationWithVanillaFields() {
        assertThrows(AssertionError.class, () -> read("""
            {"type":"neoforge:unknown","item":"minecraft:potion","items":"minecraft:potion",
             "components":{"minecraft:potion_contents":{"potion":"minecraft:poison"}}}
            """));
        assertThrows(AssertionError.class, () -> read("""
            {"fabric:type":"fabric:custom_data","base":{"item":"aura:fairy_charm"},
             "nbt":{"fairyRole":"fairy"}}
            """));
    }

    private static List<String> read(String json) {
        return RecipeIngredientReader.readIngredientIds(JsonParser.parseString(json));
    }
}
