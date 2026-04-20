package pixlepix.auracascade.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public enum AngelsteelToolKind {
    PICKAXE("angelsteel_pickaxe", "pickaxe", "minecraft:item/handheld") {
        @Override
        public Item.Properties createProperties(ToolMaterial material) {
            return new Item.Properties().stacksTo(1).pickaxe(material, 1.0F, -2.8F);
        }
    },
    AXE("angelsteel_axe", "axe", "minecraft:item/handheld") {
        @Override
        public Item.Properties createProperties(ToolMaterial material) {
            return new Item.Properties().stacksTo(1).axe(material, 6.0F, -3.0F);
        }
    },
    SHOVEL("angelsteel_shovel", "shovel", "minecraft:item/handheld") {
        @Override
        public Item.Properties createProperties(ToolMaterial material) {
            return new Item.Properties().stacksTo(1).shovel(material, 1.5F, -3.0F);
        }
    },
    SWORD("angelsteel_sword", "sword", "minecraft:item/handheld") {
        @Override
        public Item.Properties createProperties(ToolMaterial material) {
            return new Item.Properties().stacksTo(1).sword(material, 3.0F, -2.4F);
        }
    };

    private final String registryPrefix;
    private final String displaySuffix;
    private final String modelParent;

    AngelsteelToolKind(String registryPrefix, String displaySuffix, String modelParent) {
        this.registryPrefix = registryPrefix;
        this.displaySuffix = displaySuffix;
        this.modelParent = modelParent;
    }

    public String registryPath(int degreeIndex) {
        return registryPrefix + "_" + (degreeIndex + 1);
    }

    public String displaySuffix() {
        return displaySuffix;
    }

    public String modelParent() {
        return modelParent;
    }

    public abstract Item.Properties createProperties(ToolMaterial material);
}
