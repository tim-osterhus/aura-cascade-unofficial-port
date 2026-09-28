package pixlepix.auracascade.data.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraCustomDataIngredientTest {
    @Test
    void codecRoundTripPreservesCustomDataSubsetMatchingAndCharmRoleGate() {
        Ingredient ingredient = roundTrip("""
            {"type":"aura:custom_data","items":"aura:fairy_charm","nbt":{"fairyRole":"fairy"}}
            """, "aura:custom_data");

        assertTrue(ingredient.test(charmWithData("fairy", "foreignKey", "preserved")));
        assertFalse(ingredient.test(charmWithData("fighter", "foreignKey", "preserved")));
        assertFalse(ingredient.test(charmWithData(null, "foreignKey", "preserved")));
        assertFalse(ingredient.test(charmWithData(null, null, null)));
        assertFalse(ingredient.test(itemWithData(Items.BRICK, "fairy", "foreignKey", "preserved")));
    }

    @Test
    void codecRejectsAnEmptyCustomDataPredicate() {
        JsonElement emptyPredicate = JsonParser.parseString("""
            {"type":"aura:custom_data","items":"aura:fairy_charm","nbt":{}}
            """);

        assertTrue(Ingredient.CODEC.parse(registryOps(), emptyPredicate).error().isPresent());
    }

    @Test
    void codecRoundTripKeepsPotionIngredientValueExact() {
        Ingredient ingredient = roundTrip("""
            {"type":"neoforge:components","items":"minecraft:potion",
             "components":{"minecraft:potion_contents":{"potion":"minecraft:poison"}},
             "strict":false}
            """, "neoforge:components");

        assertTrue(ingredient.test(PotionContents.createItemStack(Items.POTION, Potions.POISON)));
        assertFalse(ingredient.test(PotionContents.createItemStack(Items.POTION, Potions.LONG_REGENERATION)));
    }

    @Test
    void vanillaIngredientCodecRoundTripStillMatchesOrdinaryItems() {
        Ingredient ingredient = roundTrip("""
            {"item":"minecraft:brick"}
            """, null);

        assertTrue(ingredient.test(new ItemStack(Items.BRICK)));
        assertFalse(ingredient.test(new ItemStack(Items.COBBLESTONE)));
    }

    private static Ingredient roundTrip(String json, String expectedType) {
        RegistryOps<JsonElement> ops = registryOps();
        Ingredient decoded = decode(ops, JsonParser.parseString(json));
        JsonElement encoded = Ingredient.CODEC.encodeStart(ops, decoded).result()
            .orElseThrow(() -> new AssertionError("Failed to encode ingredient: " + json));
        if (expectedType != null) {
            assertTrue(encoded.isJsonObject());
            assertTrue(encoded.getAsJsonObject().has("type"));
            assertEquals(expectedType, encoded.getAsJsonObject().get("type").getAsString());
        }
        return decode(ops, encoded);
    }

    private static Ingredient decode(RegistryOps<JsonElement> ops, JsonElement json) {
        return Ingredient.CODEC.parse(ops, json).result()
            .orElseThrow(() -> new AssertionError("Failed to decode ingredient: " + json));
    }

    private static RegistryOps<JsonElement> registryOps() {
        TestMinecraftBootstrap.ensureBootstrapped();
        return RegistryOps.create(JsonOps.INSTANCE,
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    private static ItemStack charmWithData(String role, String extraKey, String extraValue) {
        Item charm = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("aura", "fairy_charm"));
        return itemWithData(charm, role, extraKey, extraValue);
    }

    private static ItemStack itemWithData(Item item, String role, String extraKey, String extraValue) {
        ItemStack stack = new ItemStack(item);
        CompoundTag data = new CompoundTag();
        if (role != null) {
            data.putString("fairyRole", role);
        }
        if (extraKey != null) {
            data.putString(extraKey, extraValue);
        }
        if (!data.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }
}
