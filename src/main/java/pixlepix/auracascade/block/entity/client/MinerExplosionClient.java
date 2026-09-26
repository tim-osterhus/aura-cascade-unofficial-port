package pixlepix.auracascade.block.entity.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import pixlepix.auracascade.block.entity.MinerExplosionEntities;

public final class MinerExplosionClient {
    private MinerExplosionClient() {
    }

    public static void bootstrapClient() {
        EntityRendererRegistry.register(MinerExplosionEntities.type(), NoopRenderer::new);
    }
}
