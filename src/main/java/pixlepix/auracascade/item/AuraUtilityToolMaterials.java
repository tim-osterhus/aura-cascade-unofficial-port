package pixlepix.auracascade.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import pixlepix.auracascade.AuraCascadeMod;

public final class AuraUtilityToolMaterials {
    public static final TagKey<net.minecraft.world.item.Item> ARCANE_INGOTS = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, "arcane_ingots")
    );
    public static final ToolMaterial ARCANE_SWORD = new ToolMaterial(
        BlockTags.INCORRECT_FOR_IRON_TOOL,
        12,
        7.0F,
        3.0F,
        12,
        ARCANE_INGOTS
    );

    private AuraUtilityToolMaterials() {
    }
}
