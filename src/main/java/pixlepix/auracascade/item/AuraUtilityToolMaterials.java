package pixlepix.auracascade.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import pixlepix.auracascade.AuraCascadeMod;

public final class AuraUtilityToolMaterials {
    public static final TagKey<net.minecraft.world.item.Item> ARCANE_INGOTS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, "arcane_ingots")
    );
    public static final Tier ARCANE_SWORD = new AuraTier(
        BlockTags.INCORRECT_FOR_IRON_TOOL,
        12,
        7.0F,
        3.0F,
        12,
        Ingredient.of(ARCANE_INGOTS)
    );

    private AuraUtilityToolMaterials() {
    }

    private record AuraTier(
        TagKey<net.minecraft.world.level.block.Block> incorrectBlocksForDrops,
        int uses,
        float speed,
        float attackDamageBonus,
        int enchantmentValue,
        Ingredient repairIngredient
    ) implements Tier {
        @Override
        public TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return incorrectBlocksForDrops;
        }

        @Override
        public int getUses() {
            return uses;
        }

        @Override
        public float getSpeed() {
            return speed;
        }

        @Override
        public float getAttackDamageBonus() {
            return attackDamageBonus;
        }

        @Override
        public int getEnchantmentValue() {
            return enchantmentValue;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return repairIngredient;
        }
    }
}
