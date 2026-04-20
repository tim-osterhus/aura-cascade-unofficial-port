package pixlepix.auracascade.data.recipe;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record AuraWorldRecipe(
    String id,
    RecipeKind kind,
    boolean prismaticOnly,
    List<Ingredient> ingredients,
    ItemStack result
) {
    public AuraWorldRecipe {
        ingredients = List.copyOf(ingredients);
        result = result.copy();
    }

    public enum RecipeKind {
        PROCESSOR,
        SYNTHESIZER
    }

    public record Ingredient(Item item, int count) {
    }
}
