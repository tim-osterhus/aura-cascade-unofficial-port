package pixlepix.auracascade.enchantment;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraEnchantments {
    public static final TagKey<Item> KALEIDOSCOPIC_ENCHANTABLE = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, "kaleidoscopic_enchantable")
    );

    private static final EnumMap<AuraColor, ResourceKey<Enchantment>> KALEIDOSCOPIC = new EnumMap<>(AuraColor.class);

    static {
        KALEIDOSCOPIC.put(AuraColor.RED, key("kaleidoscopic_red"));
        KALEIDOSCOPIC.put(AuraColor.ORANGE, key("kaleidoscopic_orange"));
        KALEIDOSCOPIC.put(AuraColor.YELLOW, key("kaleidoscopic_yellow"));
        KALEIDOSCOPIC.put(AuraColor.GREEN, key("kaleidoscopic_green"));
        KALEIDOSCOPIC.put(AuraColor.BLUE, key("kaleidoscopic_blue"));
        KALEIDOSCOPIC.put(AuraColor.VIOLET, key("kaleidoscopic_violet"));
    }

    private AuraEnchantments() {
    }

    public static Optional<ResourceKey<Enchantment>> kaleidoscopic(AuraColor color) {
        return Optional.ofNullable(KALEIDOSCOPIC.get(color));
    }

    public static Map<AuraColor, ResourceKey<Enchantment>> allKaleidoscopic() {
        return Map.copyOf(KALEIDOSCOPIC);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, path));
    }
}
