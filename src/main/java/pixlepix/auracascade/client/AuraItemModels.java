package pixlepix.auracascade.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import pixlepix.auracascade.item.AngelsteelSwordItem;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraItemModels {
    private static final ResourceLocation ATTUNEMENT = ResourceLocation.fromNamespaceAndPath("aura", "attunement");

    private AuraItemModels() {
    }

    public static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(AuraItemModels::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AuraItemModels::registerPredicates);
    }

    private static void registerPredicates() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof AngelsteelSwordItem sword) {
                ItemProperties.register(item, ATTUNEMENT, (stack, level, entity, seed) ->
                    sword.swordAura(stack).map(AuraItemModels::attunementValue).orElse(0.0F));
            }
        }
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
}
