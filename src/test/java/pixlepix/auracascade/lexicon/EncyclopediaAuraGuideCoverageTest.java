package pixlepix.auracascade.lexicon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EncyclopediaAuraGuideCoverageTest {
    private static final Path ENTRY_DIRECTORY = Path.of(
        "src/main/resources/assets/aura/patchouli_books/encyclopedia_aura/en_us/entries"
    );
    private static final Path COVERAGE_AUDIT = Path.of("docs/audits/2026-09-23-guide-coverage.md");
    private static final Path PAGE_COVERAGE_AUDIT = Path.of("docs/audits/2026-09-23-guide-page-coverage.md");
    private static final Pattern COVERAGE_ROW = Pattern.compile("(?m)^\\| `(aura\\.entry\\.[^`]+)` \\|");
    private static final Pattern PAGE_COVERAGE_KEY = Pattern.compile("`(aura\\.page\\.[^`]+)`");
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9]+(?:'[A-Za-z0-9]+)?");
    private static final Set<String> EXPANDED_ENTRIES = Set.of(
        "accessory_amulets",
        "accessory_belt",
        "accessory_rings",
        "angelsteel_axes",
        "angelsteel_pickaxes",
        "angelsteel_shovels",
        "angelsteel_swords_blue",
        "angelsteel_swords_green",
        "angelsteel_swords_orange",
        "angelsteel_swords_red",
        "angelsteel_swords_violet",
        "angelsteel_swords_yellow",
        "aura_colors",
        "consumer_fieldwork",
        "consumers_vortex",
        "fairy_charm_basics",
        "fairy_combat_charms",
        "fairy_travel_charms",
        "fairy_work_charms",
        "fairy_world_charms",
        "first_aura_circuit",
        "node_variants",
        "pump_variants",
        "arcane_progression",
        "storage_network",
        "accessory_loadout",
        "gear_utility",
        "fairy_roles",
        "angelsteel_tools",
        "angelsteel_swords",
        "kaleidoscopic_enchantments",
        "enchant_matrix",
        "late_systems",
        "prismatic_wand",
        "pumps_control",
        "quest_progression",
        "rebounding_enigma",
        "travelers_bricks",
        "white_aura_crystal"
    );
    private static final Set<String> EXPANDED_RECIPES = expandedRecipes();

    @Test
    void coverageAuditMapsEveryOriginalEntryKeyExactlyOnce() throws IOException {
        Set<String> originalKeys = originalEntryKeys();
        String audit = Files.readString(COVERAGE_AUDIT, StandardCharsets.UTF_8);
        Matcher matcher = COVERAGE_ROW.matcher(audit);
        List<String> rows = new ArrayList<>();
        while (matcher.find()) {
            rows.add(matcher.group(1));
        }
        Set<String> mappedKeys = new TreeSet<>(rows);

        assertEquals(101, originalKeys.size());
        assertEquals(rows.size(), mappedKeys.size(), "Each original key must have exactly one coverage row");
        assertEquals(originalKeys, mappedKeys, "Coverage rows must match the jar's original entry keys");
    }

    @Test
    void pageCoverageAuditMapsEveryOriginalPageKeyExactlyOnce() throws IOException {
        Set<String> originalKeys = originalPageKeys();
        String audit = Files.readString(PAGE_COVERAGE_AUDIT, StandardCharsets.UTF_8);
        int sectionStart = audit.indexOf("## Page-Key Mapping");
        int sectionEnd = audit.indexOf("## Remaining Instruction Gaps", sectionStart);
        assertTrue(sectionStart >= 0 && sectionEnd > sectionStart, "Page-key map section is missing or not bounded");

        Matcher matcher = PAGE_COVERAGE_KEY.matcher(audit.substring(sectionStart, sectionEnd));
        List<String> rows = new ArrayList<>();
        while (matcher.find()) {
            rows.add(matcher.group(1));
        }
        Set<String> mappedKeys = new TreeSet<>(rows);

        assertEquals(261, originalKeys.size());
        assertEquals(rows.size(), mappedKeys.size(), "Each original page key must appear exactly once");
        assertEquals(originalKeys, mappedKeys, "Page-key mappings must match the jar's original page keys");
    }

    @Test
    void expandedEntriesKeepReadablePagesAndUseRegisteredCraftingRecipes() throws IOException {
        Set<String> foundEntries = new TreeSet<>();
        Set<String> foundRecipes = new TreeSet<>();

        for (String entryId : EXPANDED_ENTRIES) {
            Path entryPath = ENTRY_DIRECTORY.resolve(entryId + ".json");
            JsonObject entry = readJson(entryPath);
            foundEntries.add(entryId);
            assertTrue(entry.get("name").getAsString().length() <= 27, entryId);

            JsonArray pages = entry.getAsJsonArray("pages");
            assertTrue(pages.size() > 0, entryId);
            assertTrue(pages.size() <= 20, entryId + " should remain a focused chapter");
            assertEquals("patchouli:text", pages.get(0).getAsJsonObject().get("type").getAsString(), entryId);
            int textPageIndex = 0;

            for (JsonElement pageElement : pages) {
                JsonObject page = pageElement.getAsJsonObject();
                String type = page.get("type").getAsString();
                assertTrue(page.get("title").getAsString().length() <= 27, entryId + ": " + page.get("title"));

                if ("patchouli:text".equals(type)) {
                    int maximumWords = textPageIndex == 0 ? 40 : 55;
                    assertTrue(wordCount(page.get("text").getAsString()) <= maximumWords,
                        entryId + ": " + page.get("title") + " exceeds " + maximumWords + " words");
                    textPageIndex++;
                } else if ("patchouli:quest".equals(type)) {
                    assertTrue(wordCount(page.get("text").getAsString()) <= 55,
                        entryId + ": " + page.get("title") + " exceeds 55 words");
                } else if ("patchouli:crafting".equals(type)) {
                    if (page.has("text") && !page.get("text").isJsonNull()) {
                        assertTrue(wordCount(page.get("text").getAsString()) <= 30,
                            entryId + ": " + page.get("title") + " crafting text exceeds 30 words");
                    }
                    String recipeId = page.get("recipe").getAsString();
                    foundRecipes.add(recipeId);
                    assertCraftingRecipeExists(recipeId);
                }
            }
        }

        assertEquals(EXPANDED_ENTRIES, foundEntries);
        assertEquals(EXPANDED_RECIPES, foundRecipes);
    }

    @Test
    void everyEntryRespectsPatchouliPageCopyBudgets() throws IOException {
        try (var paths = Files.list(ENTRY_DIRECTORY)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                JsonObject entry = readJson(path);
                String entryName = entry.get("name").getAsString();
                assertTrue(entryName.length() <= 27, path.getFileName() + " entry name");

                int textPageIndex = 0;
                for (JsonElement pageElement : entry.getAsJsonArray("pages")) {
                    JsonObject page = pageElement.getAsJsonObject();
                    String pageTitle = page.get("title").getAsString();
                    assertTrue(pageTitle.length() <= 27, path.getFileName() + ": " + pageTitle);
                    if (!page.has("text") || page.get("text").isJsonNull()) {
                        continue;
                    }

                    int words = wordCount(page.get("text").getAsString());
                    String type = page.get("type").getAsString();
                    if ("patchouli:text".equals(type)) {
                        int limit = textPageIndex == 0 ? 40 : 55;
                        assertTrue(words <= limit,
                            path.getFileName() + ": " + pageTitle + " exceeds " + limit + " words");
                        textPageIndex++;
                    } else if ("patchouli:quest".equals(type)) {
                        assertTrue(words <= 55, path.getFileName() + ": " + pageTitle + " exceeds 55 words");
                    } else if ("patchouli:crafting".equals(type)) {
                        assertTrue(words <= 30, path.getFileName() + ": " + pageTitle + " crafting text exceeds 30 words");
                    }
                }
            }
        }
    }

    @Test
    void questPagesMatchTheParentManifestAndLegacyRewardContract() throws IOException {
        JsonArray pages = readJson(ENTRY_DIRECTORY.resolve("quest_progression.json")).getAsJsonArray("pages");
        JsonArray manifest = JsonParser.parseString(Files.readString(
            Path.of("src/main/resources/data/aura/quests.json"), StandardCharsets.UTF_8
        )).getAsJsonArray();
        Set<String> pageTriggers = new TreeSet<>();
        Set<String> manifestTriggers = new TreeSet<>();
        Map<String, String> pageTexts = new java.util.HashMap<>();
        assertEquals(
            "Carry a discovery and open this encyclopedia to record it. The item is not consumed; each reward can be claimed once.",
            pages.get(0).getAsJsonObject().get("text").getAsString()
        );

        for (JsonElement pageElement : pages) {
            JsonObject page = pageElement.getAsJsonObject();
            if ("patchouli:quest".equals(page.get("type").getAsString())) {
                String trigger = page.get("trigger").getAsString();
                pageTriggers.add(trigger);
                pageTexts.put(trigger, page.get("text").getAsString());
            }
        }
        for (JsonElement questElement : manifest) {
            String id = questElement.getAsJsonObject().get("id").getAsString();
            String trigger = "aura:quest/" + id;
            manifestTriggers.add(trigger);
            assertTrue(Files.exists(Path.of("src/main/resources/data/aura/advancement/quest", id + ".json")), id);
        }

        assertEquals(14, pageTriggers.size());
        assertEquals(manifestTriggers, pageTriggers);
        assertContains(pageTexts.get("aura:quest/crystals"), "Reward: 64 White Aura Crystals");
        assertContains(pageTexts.get("aura:quest/furnace"), "Reward: 64 White Aura Crystals");
        assertContains(pageTexts.get("aura:quest/synthesizer"), "Reward: 64 White Aura Crystals");
        assertContains(pageTexts.get("aura:quest/nodes"), "Reward: 16 Aura Nodes");
        assertContains(pageTexts.get("aura:quest/angelsteel"), "tier-two Angelsteel Ingot. Reward: 15 tier-one Angelsteel Ingots");

        String contract = Files.readString(Path.of("docs/audits/2026-09-23-quest-contract.md"), StandardCharsets.UTF_8);
        assertContains(contract, "| 1 | Nodes | 1 Aura Node | 16 Aura Nodes |");
        assertContains(contract, "aura:angelsteel_ingot_2");
        assertContains(contract, "aura:angelsteel_ingot_1");
        assertContains(contract, "not a consumed item");
    }

    @Test
    void sourceBackedPagesExplainStorageGearAndNodeControls() throws IOException {
        String basics = entryText("aura_basics");
        assertContains(basics, "A nearer link weighs (20 minus distance) squared");
        assertContains(basics, "regular node reserves weight 400");
        assertContains(basics, "differ by over 25");
        assertContains(basics, "Cobblestone blocks their Aura link");
        assertContains(basics, "Wait up to 200 ticks for links to refresh");
        assertContains(basics, "other open links may still transfer it");

        String storage = entryText("storage_network");
        assertContains(storage, "Right-click empty-handed to open the browser");
        assertContains(storage, "scroll the 9-by-3 results");
        assertContains(storage, "choose Max, then press Retrieve");
        assertContains(storage, "Sneak-use it empty-handed to withdraw the first stack");
        assertContains(storage, "incomplete, unloaded, unreachable, or unpowered networks disable actions");

        String accessories = entryText("accessory_loadout");
        assertContains(accessories, "Press B (default)");
        assertContains(accessories, "Amulet, Ring, Ring, and Belt");
        assertContains(accessories, "first empty matching slot");
        assertContains(accessories, "Diamond Block (50,000 Blue)");
        assertContains(accessories, "Redstone Block (50,000 Red)");

        String charms = entryText("fairy_roles");
        assertContains(charms, "Use a typed Fairy Charm while wearing a Ring of Binding");
        assertContains(charms, "a ring holds up to 15");
        assertContains(charms, "Sneak-using the ring releases bound charms rather than cycling roles");

        String nodes = entryText("node_variants");
        assertContains(nodes, "Sneak-right-click empty-handed");
        assertContains(nodes, "1,000, 10,000, 100,000, then 100 Aura, returning to 1,000");
        assertContains(nodes, "incoming Aura is blocked for 110 ticks");
        assertContains(nodes, "Threshold checks can arm during that cooldown");
        assertContains(nodes, "clear all stored Aura");
        assertContains(nodes, "unpowered they stay empty");
        assertContains(nodes, "stored consumer power remain");

        String monitor = entryText("pumps_control");
        assertContains(monitor, "first adjacent supported machine");
        assertContains(monitor, "empty pump or consumer without valid work outputs 15");
        assertContains(monitor, "pump holding power or consumer with valid work outputs 0");
        assertContains(monitor, "Ordinary Aura Nodes are ignored");
        assertContains(monitor, "not an aggregate-storage meter");

        String progression = entryText("arcane_progression");
        assertContains(progression, "one Iron Ingot and one wool block");
        assertContains(progression, "White wool yields White. Lime and Green yield Green");
        assertContains(progression, "Brown, Black, Gray, and Light Gray yield Black");
        assertContains(progression, "Red yields Red; Orange yields Orange; Yellow yields Yellow");
        assertContains(progression, "Blue, Light Blue, and Cyan yield Blue");
        assertContains(progression, "Magenta, Pink, and Purple yield Violet");
        assertContains(progression, "At the Prismatic Processor, one wool block yields one result");
        assertContains(progression, "White wool to Bone Meal; Orange wool to Orange Dye; Magenta wool to Magenta Dye; Light Blue wool to Light Blue Dye");
        assertContains(progression, "Yellow wool to Yellow Dye; Lime wool to Lime Dye; Pink wool to Pink Dye; Gray wool to Gray Dye");
        assertContains(progression, "Light Gray wool to Light Gray Dye; Cyan wool to Cyan Dye; Purple wool to Purple Dye; Blue wool to Lapis Lazuli");
        assertContains(progression, "Brown wool to Cocoa Beans; Green wool to Green Dye; Red wool to Red Dye; Black wool to Ink Sac");
        assertTrue(!progression.contains("Aura Crystal"));
        assertContains(progression, "face-connected group touching the Fluxing Node");
        assertContains(progression, "up to four compatible receivers");
        assertContains(progression, "one stored power per 15 energy inserted");

        String consumers = entryText("consumers_vortex");
        assertContains(consumers, "c:ores/<material>");
        assertContains(consumers, "c:dusts/<material>");
        assertContains(consumers, "one ore yields two dust here or three");
        assertContains(consumers, "without a matching dust tag, the ore stays untouched");

        String colors = entryText("aura_colors");
        assertContains(colors, "within three blocks just before it bursts");
        assertContains(colors, "TNT lends a 200,000 Aura lift budget");
        assertContains(colors, "each Creeper lends 50,000");
        assertContains(colors, "explosion effect is visual only");

        String crystals = entryText("white_aura_crystal");
        assertContains(crystals, "Use a White Aura Crystal directly on a Node or Pump for 1,000 matching Aura");

        String wand = entryText("prismatic_wand");
        assertContains(wand, "right-click two opposite corners");
        assertContains(wand, "source coordinates and offset from you, not a snapshot");
        assertContains(wand, "Containers and machines keep no contents or settings from the copy");
        assertContains(wand, "no fluid resources or buckets are saved");
        assertContains(wand, "same offset from your current position");
        assertContains(wand, "Only air is filled; occupied destinations and source-air cells stay unchanged");
        assertContains(wand, "512 cells total, including air");
        assertContains(wand, "every source and destination cell must be loaded and within build height and the world border");
        assertContains(wand, "one matching Block Item is consumed only after a block is placed successfully");
        assertContains(wand, "There is no undo");

        String travelersBricks = entryText("travelers_bricks");
        assertContains(travelersBricks, "0.8 blocks high and emit full light");
        assertContains(travelersBricks, "motion faster than 0.25 blocks per tick");
        assertContains(travelersBricks, "adds a five-block-per-tick push along its 3D direction to your motion");

        String reboundingEnigma = entryText("rebounding_enigma");
        assertContains(reboundingEnigma, "0.8 blocks high; it emits full light");
        assertContains(reboundingEnigma, "sets upward motion to 10 blocks per tick and preserves horizontal motion");
        assertContains(reboundingEnigma, "No fall-damage protection");
        String lateSystems = entryText("late_systems");
        assertContains(lateSystems, "$(l:aura:travelers_bricks)");
        assertContains(lateSystems, "$(l:aura:rebounding_enigma)");

        String workflows = entryText("consumer_fieldwork");
        assertContains(workflows, "Drop one furnace-smeltable item within three blocks");
        assertContains(workflows, "exactly two blocks above the Grower");
        assertContains(workflows, "4-by-4 water footprint one block below");
        assertContains(workflows, "water, Awkward, a randomly selected base potion");
        assertContains(workflows, "restores its fleece");

        String enchantments = entryText("kaleidoscopic_enchantments");
        assertContains(enchantments, "Orange multiplies harvestable-tool break speed by 1.15 per level");
        assertContains(enchantments, "Violet adds 0.5 per level to player-sourced outgoing damage");
        String matrix = entryText("enchant_matrix");
        assertContains(matrix, "Red + Violet multiplies incoming player damage by 0.9^pair");
        assertContains(matrix, "Yellow + Violet rolls bonus loot from the killed entity's loot table");
        assertContains(matrix, "Violet + Orange multiplies speed by 1.5^pair at hardness 3 or higher");

        String angels = entryText("angelsteel_tools");
        assertContains(angels, "$(l:aura:angelsteel_axes)");
        assertContains(angels, "$(l:aura:angelsteel_pickaxes)");
        assertContains(angels, "$(l:aura:angelsteel_shovels)");

        String swords = entryText("angelsteel_swords");
        for (String color : List.of("blue", "green", "orange", "red", "violet", "yellow")) {
            assertContains(swords, "$(l:aura:angelsteel_swords_" + color + ")");
        }

        String accessoriesHub = entryText("accessory_loadout");
        assertContains(accessoriesHub, "$(l:aura:accessory_amulets)");
        assertContains(accessoriesHub, "$(l:aura:accessory_rings)");
        assertContains(accessoriesHub, "$(l:aura:accessory_belt)");

        String fairyHub = entryText("fairy_roles");
        assertContains(fairyHub, "$(l:aura:fairy_combat_charms)");
        assertContains(fairyHub, "$(l:aura:fairy_work_charms)");
        assertContains(fairyHub, "$(l:aura:fairy_world_charms)");
        assertContains(fairyHub, "$(l:aura:fairy_travel_charms)");

        String combatFairies = entryText("fairy_combat_charms");
        assertContains(combatFairies, "every three ticks for 1.5 damage");
        assertContains(combatFairies, "below five health and a hostile is close");
        String workFairies = entryText("fairy_work_charms");
        assertContains(workFairies, "nearby item drops to you");
        assertContains(workFairies, "nearby player's held item to your feet");
        assertContains(workFairies, "a cow, chicken, pig, or sheep");
        assertContains(workFairies, "within four blocks of itself to you after pickup delay ends");
        assertContains(workFairies, "players within two blocks every 200 ticks");
        assertContains(workFairies, "rolls 1-in-3600 per tick");
        assertContains(workFairies, "rolls 1-in-1,200 per tick");
        assertContains(entryText("fairy_world_charms"), "clears your burning and removes lava at the fairy's position");
    }

    @Test
    void newConsumerUtilityAndFairyPagesExplainSourceBackedPlayerActions() throws IOException {
        String consumers = entryText("consumers_vortex");
        assertContains(consumers, "even when no work can be completed");
        assertContains(consumers, "idle cycles still spend power");
        assertContains(consumers, "Monitor reports readiness; it does not gate progress");
        assertContains(consumers, "one colored Arcane Ingot near the Enchanter");
        assertContains(consumers, "spent on every attempt, whether the enchantment succeeds or fails");
        assertTrue(!consumers.contains("Aura Crystal"));
        assertTrue(!consumers.contains("as it works"));
        assertTrue(!consumers.contains("Work advances"));

        String swords = entryText("angelsteel_swords");
        assertContains(swords, "higher degrees hit harder and extend it");
        for (String effect : List.of(
            "Red kindles fire",
            "Orange lifts its foe",
            "Yellow calls lightning",
            "Green deals and spreads magic harm",
            "Blue strikes foes below half health",
            "Violet swaps places with nearby living beings"
        )) {
            assertContains(swords, effect);
        }

        String tools = entryText("angelsteel_tools");
        assertContains(tools, "Efficiency multiplies speed by 1.3 per level");
        assertContains(tools, "Shatter triples it per level at hardness two or more");
        assertContains(tools, "Disintegrate does so at hardness one or less");
        assertContains(tools, "Fortune lends its buff level only when higher than the tool's");
        assertContains(tools, "Swords gain no mining gifts");

        String heels = entryText("accessory_belt");
        assertContains(heels, "step up to two blocks when the way is clear");
        assertContains(heels, "move clear and it settles at once");
        assertTrue(!heels.contains("modifier"));

        String utility = entryText("gear_utility");
        assertContains(utility, "Land hits five to ninety-nine ticks apart");
        assertContains(utility, "compounds the next strike's force by five percent");
        assertContains(utility, "up to one hundred links");
        assertContains(utility, "explodes about every five seconds");
        assertFalse(utility.contains("erupts in fire"));
        assertContains(utility, "roughly twenty-five minutes of world time");
        assertTrue(!utility.contains("incoming damage"));
        assertTrue(!utility.contains("ItemEntity.age"));

        String combatFairies = entryText("fairy_combat_charms");
        assertContains(combatFairies, "all six maladies");
        assertContains(combatFairies, "each lasts 200 ticks");
        assertContains(combatFairies, "regeneration, resistance, strength, absorption, jump, or speed");
        assertContains(combatFairies, "foes within two blocks with 0.4 force every three ticks");
        assertContains(combatFairies, "below 10 damage gains 10 and turns critical");
        assertContains(combatFairies, "within 50 blocks of a natural spawn point");

        String workFairies = entryText("fairy_work_charms");
        assertContains(workFairies, "first ready adult animal within one block");
        assertContains(workFairies, "With the Binding Ring worn");
        assertContains(workFairies, "compound your breaking speed in 1.08 steps");
        assertContains(workFairies, "up to fifteen steps");

        String worldFairies = entryText("fairy_world_charms");
        assertContains(worldFairies, "empty spot below light level 10");
        assertContains(worldFairies, "hidden light bright enough to guide you");
        assertContains(worldFairies, "The glow fades on its own");

        String travelFairies = entryText("fairy_travel_charms");
        assertContains(travelFairies, "nearby Gliders bound to it halve your fall distance");
        assertContains(travelFairies, "each companion halves it again");
        assertContains(travelFairies, "A long enough fall can still hurt");
    }

    @Test
    void pageCoverageRepairsGiveSourceBackedPlayerInstructions() throws IOException {
        String basics = entryText("aura_basics");
        assertContains(basics, "Falling Aura adds stored power below");
        assertContains(basics, "A longer fall, greater transferred amount, and heavier color yield more");
        assertContains(basics, "Look at a Node, Pump, Consumer, or Vortex block");

        String nodes = entryText("node_variants");
        assertContains(nodes, "Powering a regular Node stops it from sending Aura but not from receiving");
        assertContains(nodes, "Consumers signal stored work power against base step cost, not completed progress");

        String pumps = entryText("pumps_control");
        assertContains(pumps, "they do not pass it sideways to another Pump");
        assertContains(pumps, "each one a linked input branch from a lower node");
        String variants = entryText("pump_variants");
        assertContains(variants, "Glowstone gives 180 eligible seconds at 750 Aura per second");
        assertContains(variants, "Furnace fuel grants its burn time divided by five at 300");
        assertContains(variants, "each following wire adds 40% duration, up to 15");
        assertContains(variants, "20,000 world ticks: 16 minutes 40 seconds at 20 ticks per second");
        assertContains(variants, "containers are consumed too");

        String storage = entryText("storage_network");
        assertContains(storage, "Extremely Dense 1x100,000");
        assertContains(storage, "Mineral: 8 x 100,000; Mob and Farming: 32 x 100,000 each");
        assertContains(storage, "5 power for one shelf, 31 for five, 81 for ten");

        String late = entryText("late_systems");
        assertContains(late, "natural monster for the local biome and structure");
        assertContains(late, "after at least 21 charges, pulse redstone at the Miner");
        assertContains(late, "pair End Stone with Dirt, Cobblestone");
        assertContains(late, "Oak, Spruce, Birch, Jungle, Acacia, or Dark Oak Planks");
        assertTrue(!late.contains("combine one Obsidian with its matching base block"));
        assertContains(late, "within about 150 blocks");
        assertContains(late, "The End rite leaves water unchanged");

        String fieldwork = entryText("consumer_fieldwork");
        assertContains(fieldwork, "within three blocks");
        assertContains(fieldwork, "every 500 ticks");
        assertContains(fieldwork, "regardless of work or power");
        assertTrue(!fieldwork.contains("ordinary item lifetime"));

        String amulets = entryText("accessory_amulets");
        assertContains(amulets, "press Activate Wing (V by default)");
        assertContains(amulets, "Yellow halves projectile damage without healing");
        assertContains(amulets, "each food or drink grants the same gift on every use");
        assertContains(amulets, "potions and milk also qualify");
        assertContains(amulets, "Canceling a meal or drink grants nothing");

        String gear = entryText("gear_utility");
        assertContains(gear, "Wither Skulls are not redirected");
        assertContains(gear, "Cow/Mooshroom, Creeper/Enderman, Sheep/Pig");
        assertContains(gear, "one-in-four chance to take the first item");
        assertContains(gear, "every five seconds it consumes carried Cobblestone");

        assertContains(entryText("angelsteel_tools"), "two random bonus points among Efficiency, Fortune, Shatter, and Disintegrate");
        assertContains(entryText("angelsteel_swords"), "higher degrees hit harder and extend it");
        assertContains(entryText("kaleidoscopic_enchantments"), "three-quarters of the prior chance");
        assertContains(entryText("consumer_fieldwork"), "Nearby dropped items stay fresh within three blocks");
        assertContains(entryText("consumers_vortex"), "two steps cost 3 base units, three 7, four 15");
    }

    private static Set<String> originalEntryKeys() throws IOException {
        try (ZipFile jar = new ZipFile("AuraCascade-592.jar")) {
            ZipEntry language = jar.getEntry("assets/aura/lang/en_US.lang");
            assertNotNull(language, "Original English language file is missing from AuraCascade-592.jar");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(jar.getInputStream(language), StandardCharsets.UTF_8)
            )) {
                return reader.lines()
                    .filter(line -> line.startsWith("aura.entry."))
                    .map(line -> line.substring(0, line.indexOf('=')))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            }
        }
    }

    private static Set<String> originalPageKeys() throws IOException {
        try (ZipFile jar = new ZipFile("AuraCascade-592.jar")) {
            ZipEntry language = jar.getEntry("assets/aura/lang/en_US.lang");
            assertNotNull(language, "Original English language file is missing from AuraCascade-592.jar");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(jar.getInputStream(language), StandardCharsets.UTF_8)
            )) {
                return reader.lines()
                    .filter(line -> line.startsWith("aura.page."))
                    .map(line -> line.substring(0, line.indexOf('=')))
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            }
        }
    }

    private static void assertCraftingRecipeExists(String recipeId) throws IOException {
        String[] parts = recipeId.split(":", 2);
        assertEquals(2, parts.length, recipeId);
        Path recipePath = Path.of("src/main/resources/data", parts[0], "recipe", parts[1] + ".json");
        JsonObject recipe = readJson(recipePath);
        assertTrue(recipe.get("type").getAsString().startsWith("minecraft:crafting_"), recipeId);
    }

    private static String entryText(String entryId) throws IOException {
        JsonArray pages = readJson(ENTRY_DIRECTORY.resolve(entryId + ".json")).getAsJsonArray("pages");
        StringBuilder text = new StringBuilder();
        for (JsonElement page : pages) {
            JsonObject object = page.getAsJsonObject();
            if (object.has("text")) {
                text.append(object.get("text").getAsString()).append('\n');
            }
        }
        return text.toString();
    }

    private static void assertContains(String text, String expected) {
        assertTrue(text.contains(expected), "Guide text is missing: " + expected);
    }

    private static Set<String> expandedRecipes() {
        TreeSet<String> recipes = new TreeSet<>(Set.of(
            "aura:aura_crystal_black",
            "aura:aura_crystal_blue",
            "aura:aura_crystal_green",
            "aura:aura_crystal_orange",
            "aura:aura_crystal_red",
            "aura:aura_crystal_violet",
            "aura:aura_crystal_yellow",
            "aura:aura_crystal_white",
            "aura:aura_node",
            "aura:aura_node_black",
            "aura:aura_node_capacitor",
            "aura:aura_node_conserve",
            "aura:aura_node_orange",
            "aura:aura_node_pump",
            "aura:aura_node_pump_alt",
            "aura:aura_node_pump_fall",
            "aura:aura_node_pump_fall_alt",
            "aura:aura_node_pump_light",
            "aura:aura_node_pump_light_alt",
            "aura:aura_node_pump_projectile",
            "aura:aura_node_pump_projectile_alt",
            "aura:aura_node_pump_redstone",
            "aura:aura_node_pump_redstone_alt",
            "aura:consumer_block_ore",
            "aura:aura_node_crafting_center",
            "aura:aura_node_crafting_pedestal",
            "aura:consumer_block_angel",
            "aura:aura_node_flux",
            "aura:consumer_block_ore_adv",
            "aura:bookshelf_coordinator",
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
            "aura:amulet_of_the_angels_wing",
            "aura:sash_of_the_angels_heels",
            "aura:ring_of_shattered_stone",
            "aura:amulet_of_the_forbidden_fruit",
            "aura:red_protection_amulet",
            "aura:orange_protection_amulet",
            "aura:yellow_protection_amulet",
            "aura:green_protection_amulet",
            "aura:blue_protection_amulet",
            "aura:violet_protection_amulet",
            "aura:prismatic_wand",
            "aura:travelers_bricks",
            "aura:rebounding_enigma",
            "aura:mirror_of_the_angel",
            "aura:transmuting_sword",
            "aura:sword_of_the_barbarian",
            "aura:sword_of_the_thief",
            "aura:portable_red_hole",
            "aura:portable_black_hole",
            "aura:fairy_charm",
            "aura:fairy_charm_baiter",
            "aura:fairy_charm_breeder",
            "aura:fairy_charm_buffer",
            "aura:fairy_charm_debuffer",
            "aura:fairy_charm_digger",
            "aura:fairy_charm_extinguisher",
            "aura:fairy_charm_fetcher",
            "aura:fairy_charm_fighter",
            "aura:fairy_charm_glider",
            "aura:fairy_charm_lighter",
            "aura:fairy_charm_pusher",
            "aura:fairy_charm_savior",
            "aura:fairy_charm_scarer",
            "aura:fairy_charm_shooter",
            "aura:fairy_charm_stealer",
            "aura:fairy_charm_trainer"
        ));
        for (int tier = 1; tier <= 12; tier++) {
            recipes.add("aura:angelsteel_axe_" + tier);
            recipes.add("aura:angelsteel_pickaxe_" + tier);
            recipes.add("aura:angelsteel_shovel_" + tier);
            for (String color : List.of("blue", "green", "orange", "red", "violet", "yellow")) {
                recipes.add("aura:angelsteel_sword_" + tier + "_" + color);
            }
        }
        return recipes;
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static int wordCount(String text) {
        Matcher matcher = WORD.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
