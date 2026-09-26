package pixlepix.auracascade.lexicon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraQuestResourceTest {
    private static final Path DATA = Path.of("src/main/resources/data/aura");

    @Test
    void everyQuestHasOneServerAwardedPersistentAdvancement() throws IOException {
        var quests = JsonParser.parseString(Files.readString(DATA.resolve("quests.json"))).getAsJsonArray();
        var ids = new HashSet<String>();
        assertEquals(14, quests.size());
        for (var element : quests) {
            var quest = element.getAsJsonObject();
            String id = quest.get("id").getAsString();
            assertTrue(ids.add(id), "Duplicate quest: " + id);
            assertTrue(quest.get("reward_count").getAsInt() > 0);
            JsonObject advancement = readAdvancement(id);
            assertEquals("aura:quest/root", advancement.get("parent").getAsString());
            assertEquals(quest.get("goal").getAsString(),
                advancement.getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString());
            assertEquals("minecraft:impossible", advancement.getAsJsonObject("criteria")
                .getAsJsonObject("book_open").get("trigger").getAsString());
            assertFalse(advancement.has("rewards"), "Reward is granted once by the book interaction");
        }
        assertEquals("aura:encyclopedia_aura", readAdvancement("root").getAsJsonObject("display")
            .getAsJsonObject("icon").get("id").getAsString());
    }

    @Test
    void originalRewardQuantitiesAndMetadataMappingsArePreserved() throws IOException {
        Map<String, String> expected = Map.ofEntries(
            Map.entry("crystals", "aura:aura_crystal_white|aura:aura_crystal_white|64"),
            Map.entry("nodes", "aura:aura_node|aura:aura_node|16"),
            Map.entry("pumps", "aura:aura_node_pump|minecraft:stick|64"),
            Map.entry("furnace", "aura:consumer_block_furnace|aura:aura_crystal_white|64"),
            Map.entry("red_crystals", "aura:aura_crystal_red|minecraft:tnt|4"),
            Map.entry("processor", "aura:consumer_block_ore|minecraft:coal|8"),
            Map.entry("dye", "aura:consumer_block_dye|minecraft:white_wool|32"),
            Map.entry("arcane_ingot", "aura:arcane_ingot_white|aura:arcane_ingot_white|15"),
            Map.entry("vortex_infusion", "aura:aura_node_crafting_center|aura:aura_node_crafting_pedestal|4"),
            Map.entry("arcane_gem", "aura:arcane_gem_white|aura:arcane_gem_white|8"),
            Map.entry("arcane_prism", "aura:arcane_prism|aura:arcane_prism|1"),
            Map.entry("synthesizer", "aura:consumer_block_angel|aura:aura_crystal_white|64"),
            Map.entry("angelsteel", "aura:angelsteel_ingot_2|aura:angelsteel_ingot_1|15"),
            Map.entry("fairies", "aura:ring_of_binding|aura:fairy_charm|1")
        );
        for (var element : JsonParser.parseString(Files.readString(DATA.resolve("quests.json"))).getAsJsonArray()) {
            var quest = element.getAsJsonObject();
            assertEquals(expected.get(quest.get("id").getAsString()), quest.get("goal").getAsString()
                + "|" + quest.get("reward").getAsString() + "|" + quest.get("reward_count").getAsInt());
        }
    }

    private static JsonObject readAdvancement(String id) throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve("advancement/quest/" + id + ".json")))
            .getAsJsonObject();
    }
}
