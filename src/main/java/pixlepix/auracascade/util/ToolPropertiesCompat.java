package pixlepix.auracascade.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;

public final class ToolPropertiesCompat {
    private ToolPropertiesCompat() {
    }

    public static Item.Properties sword(Item.Properties properties, Tier tier, int attackDamage, float attackSpeed) {
        return toolProperties(properties, tier, BlockTags.SWORD_EFFICIENT)
            .attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed));
    }

    public static Item.Properties pickaxe(Item.Properties properties, Tier tier, float attackDamage, float attackSpeed) {
        return toolProperties(properties, tier, BlockTags.MINEABLE_WITH_PICKAXE)
            .attributes(DiggerItem.createAttributes(tier, attackDamage, attackSpeed));
    }

    public static Item.Properties axe(Item.Properties properties, Tier tier, float attackDamage, float attackSpeed) {
        return toolProperties(properties, tier, BlockTags.MINEABLE_WITH_AXE)
            .attributes(DiggerItem.createAttributes(tier, attackDamage, attackSpeed));
    }

    public static Item.Properties shovel(Item.Properties properties, Tier tier, float attackDamage, float attackSpeed) {
        return toolProperties(properties, tier, BlockTags.MINEABLE_WITH_SHOVEL)
            .attributes(DiggerItem.createAttributes(tier, attackDamage, attackSpeed));
    }

    private static Item.Properties toolProperties(Item.Properties properties, Tier tier, TagKey<Block> mineableTag) {
        return properties
            .durability(tier.getUses())
            .component(DataComponents.TOOL, tier.createToolProperties(mineableTag));
    }
}
