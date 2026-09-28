package pixlepix.auracascade.data.recipe;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pixlepix.auracascade.AuraCascadeMod;

public final class AuraIngredientTypes {
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, AuraCascadeMod.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<AuraCustomDataIngredient>> CUSTOM_DATA =
        INGREDIENT_TYPES.register("custom_data", () -> new IngredientType<>(AuraCustomDataIngredient.CODEC));

    private AuraIngredientTypes() {
    }

    public static void register(IEventBus modBus) {
        INGREDIENT_TYPES.register(modBus);
    }
}
