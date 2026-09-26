package pixlepix.auracascade.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ToolPropertiesCompat {
    private ToolPropertiesCompat() {
    }

    public static Item.Properties sword(Item.Properties properties, ToolMaterial material, int attackDamage, float attackSpeed) {
        return properties.sword(material, attackDamage, attackSpeed);
    }

    public static Item.Properties pickaxe(Item.Properties properties, ToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.pickaxe(material, attackDamage, attackSpeed);
    }

    public static Item.Properties axe(Item.Properties properties, ToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.axe(material, attackDamage, attackSpeed);
    }

    public static Item.Properties shovel(Item.Properties properties, ToolMaterial material, float attackDamage, float attackSpeed) {
        return properties.shovel(material, attackDamage, attackSpeed);
    }
}
