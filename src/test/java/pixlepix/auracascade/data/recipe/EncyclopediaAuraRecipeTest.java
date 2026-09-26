package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EncyclopediaAuraRecipeTest {
    private static final Path RECIPE_PATH = Path.of("src/main/resources/data/aura/recipe/encyclopedia_aura.json");

    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void encyclopediaUsesTheOriginalBookAndWhiteCrystalShape() throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(RECIPE_PATH, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
        assertEquals("misc", json.get("category").getAsString());

        JsonObject key = json.getAsJsonObject("key");
        assertEquals(2, key.size());
        assertEquals("aura:aura_crystal_white", key.getAsJsonObject("C").get("item").getAsString());
        assertEquals("minecraft:book", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals(1, json.getAsJsonArray("pattern").size());
        assertEquals("CB", json.getAsJsonArray("pattern").get(0).getAsString());
        assertFalse(json.toString().contains("arcane_prism"));

        JsonObject result = json.getAsJsonObject("result");
        assertEquals("aura:encyclopedia_aura", result.get("id").getAsString());
        int outputCount = result.get("count").getAsInt();
        assertEquals(1, outputCount);

        // The test bootstrap has vanilla registries only. Keep the Aura item id assertion above,
        // then use amethyst shard as a surrogate to exercise Minecraft's real shaped matcher.
        JsonObject codecJson = json.deepCopy();
        codecJson.getAsJsonObject("key").getAsJsonObject("C").addProperty("item", "minecraft:amethyst_shard");
        var decodeResult = ShapedRecipePattern.MAP_CODEC.codec().parse(JsonOps.INSTANCE, codecJson);
        assertTrue(decodeResult.result().isPresent(), () -> "Could not decode the encyclopedia pattern: " + decodeResult.error());
        ShapedRecipePattern pattern = decodeResult.result().orElseThrow();
        assertEquals(2, pattern.width());
        assertEquals(1, pattern.height());
        assertEquals(2, pattern.ingredients().size());
        assertTrue(pattern.ingredients().get(0).test(new ItemStack(Items.AMETHYST_SHARD)));
        assertTrue(pattern.ingredients().get(1).test(new ItemStack(Items.BOOK)));

        ShapedRecipe recipe = new ShapedRecipe(
            "",
            CraftingBookCategory.MISC,
            pattern,
            new ItemStack(Items.WRITTEN_BOOK, outputCount),
            true
        );
        assertTrue(recipe.canCraftInDimensions(2, 2));

        CraftingInput shiftedInput = input(
            2,
            2,
            List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.BOOK))
        );
        assertTrue(recipe.matches(shiftedInput, null));
        assertEquals(2, shiftedInput.items().stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(outputCount, recipe.assemble(shiftedInput, RegistryAccess.EMPTY).getCount());

        CraftingInput mirroredInput = input(
            2,
            2,
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.AMETHYST_SHARD), ItemStack.EMPTY, ItemStack.EMPTY)
        );
        assertTrue(recipe.matches(mirroredInput, null));
    }

    private static CraftingInput input(int width, int height, List<ItemStack> stacks) {
        return CraftingInput.of(width, height, stacks);
    }
}
