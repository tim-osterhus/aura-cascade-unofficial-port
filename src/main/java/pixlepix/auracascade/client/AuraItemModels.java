package pixlepix.auracascade.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.item.AngelsteelSwordItem;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraItemModels {
    private static final Identifier ATTUNEMENT = Identifier.fromNamespaceAndPath("aura", "attunement");
    private static final MapCodec<AttunementProperty> ATTUNEMENT_CODEC = MapCodec.unit(new AttunementProperty());

    private AuraItemModels() {
    }

    public static void bootstrapClient() {
        RangeSelectItemModelProperties.ID_MAPPER.put(ATTUNEMENT, ATTUNEMENT_CODEC);
    }

    public static float attunementValue(AuraColor color) {
        // Model predicates are clamped to [0, 1]; binary fractions stay exact.
        return switch (color) {
            case RED -> 0.125F;
            case ORANGE -> 0.25F;
            case YELLOW -> 0.375F;
            case GREEN -> 0.5F;
            case BLUE -> 0.625F;
            case VIOLET -> 0.75F;
            default -> 0.0F;
        };
    }

    private record AttunementProperty() implements RangeSelectItemModelProperty {
        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            Item item = stack.getItem();
            return item instanceof AngelsteelSwordItem sword
                ? sword.swordAura(stack).map(AuraItemModels::attunementValue).orElse(0.0F)
                : 0.0F;
        }

        @Override
        public MapCodec<AttunementProperty> type() {
            return ATTUNEMENT_CODEC;
        }
    }
}
