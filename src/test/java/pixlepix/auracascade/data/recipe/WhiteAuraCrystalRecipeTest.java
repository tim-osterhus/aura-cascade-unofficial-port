package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WhiteAuraCrystalRecipeTest {
    private static final Path RECIPE_PATH = Path.of("src/main/resources/data/aura/recipe/aura_crystal_white.json");

    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void whiteCrystalRecipeMatchesOnlyTheAmethystAndGoldNuggetShape() throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(RECIPE_PATH, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
        assertEquals("misc", json.get("category").getAsString());

        var decodeResult = ShapedRecipePattern.MAP_CODEC.codec().parse(JsonOps.INSTANCE, json);
        assertTrue(decodeResult.result().isPresent(), () -> "Could not decode the white crystal pattern: " + decodeResult.error());
        ShapedRecipePattern pattern = decodeResult.result().orElseThrow();
        assertEquals(3, pattern.width());
        assertEquals(3, pattern.height());
        assertEquals(9, pattern.ingredients().size());
        assertEquals(8L, matchingIngredients(pattern, new ItemStack(Items.GOLD_NUGGET)));
        assertEquals(1L, matchingIngredients(pattern, new ItemStack(Items.AMETHYST_SHARD)));
        assertEquals(0L, matchingIngredients(pattern, new ItemStack(Items.IRON_INGOT)));

        JsonObject result = json.getAsJsonObject("result");
        assertEquals("aura:aura_crystal_white", result.get("id").getAsString());
        int outputCount = result.get("count").getAsInt();
        assertEquals(2, outputCount);

        // This bootstrap does not register Aura items; live crafting must verify the real output and consumption.
        ShapedRecipe recipe = new ShapedRecipe(
            "",
            CraftingBookCategory.MISC,
            pattern,
            new ItemStack(Items.AMETHYST_SHARD, outputCount),
            true
        );
        CraftingInput validInput = input(3, 3, whiteCrystalGrid(new ItemStack(Items.AMETHYST_SHARD)));
        assertTrue(recipe.matches(validInput, null));
        assertEquals(9, validInput.items().stream().mapToInt(ItemStack::getCount).sum());
        assertEquals(outputCount, recipe.assemble(validInput, RegistryAccess.EMPTY).getCount());

        assertFalse(recipe.matches(input(3, 3, whiteCrystalGrid(new ItemStack(Items.IRON_INGOT))), null));

        List<ItemStack> missingNugget = new ArrayList<>(whiteCrystalGrid(new ItemStack(Items.AMETHYST_SHARD)));
        missingNugget.set(0, ItemStack.EMPTY);
        assertFalse(recipe.matches(input(3, 3, missingNugget), null));

        List<ItemStack> shiftedAmethyst = new ArrayList<>(whiteCrystalGrid(new ItemStack(Items.AMETHYST_SHARD)));
        shiftedAmethyst.set(1, new ItemStack(Items.AMETHYST_SHARD));
        shiftedAmethyst.set(4, new ItemStack(Items.GOLD_NUGGET));
        assertFalse(recipe.matches(input(3, 3, shiftedAmethyst), null));

        CraftingInput inventoryGrid = input(
            2,
            2,
            List.of(
                new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.GOLD_NUGGET),
                new ItemStack(Items.AMETHYST_SHARD)
            )
        );
        assertFalse(recipe.canCraftInDimensions(2, 2));
        assertFalse(recipe.matches(inventoryGrid, null));
    }

    private static long matchingIngredients(ShapedRecipePattern pattern, ItemStack stack) {
        return pattern.ingredients().stream().filter(ingredient -> ingredient.test(stack)).count();
    }

    private static List<ItemStack> whiteCrystalGrid(ItemStack center) {
        return List.of(
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),
            center,
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET),
            new ItemStack(Items.GOLD_NUGGET)
        );
    }

    private static CraftingInput input(int width, int height, List<ItemStack> stacks) {
        return CraftingInput.of(width, height, stacks);
    }
}
