package pixlepix.auracascade.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import pixlepix.auracascade.fairy.AuraFairyEntityRegistry;

public final class AuraFairyClientRegistry {
    private AuraFairyClientRegistry() {
    }

    public static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(AuraFairyClientRegistry::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AuraFairyEntityRegistry.entityType(), AuraFairyRenderer::new);
    }
}
