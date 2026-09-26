package pixlepix.auracascade.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import pixlepix.auracascade.fairy.AuraFairyEntityRegistry;

public final class AuraFairyClientRegistry {
    private AuraFairyClientRegistry() {
    }

    public static void bootstrapClient() {
        EntityRendererRegistry.register(AuraFairyEntityRegistry.entityType(), AuraFairyRenderer::new);
    }
}
