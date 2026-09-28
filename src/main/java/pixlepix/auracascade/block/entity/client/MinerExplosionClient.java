package pixlepix.auracascade.block.entity.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import pixlepix.auracascade.block.entity.MinerExplosionEntities;

public final class MinerExplosionClient {
    private MinerExplosionClient() {
    }

    public static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(MinerExplosionClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MinerExplosionEntities.type(), NoopRenderer::new);
    }
}
