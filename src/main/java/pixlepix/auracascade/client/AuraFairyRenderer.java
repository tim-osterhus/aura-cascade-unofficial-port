package pixlepix.auracascade.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pixlepix.auracascade.fairy.AuraFairyEntity;

public final class AuraFairyRenderer extends EntityRenderer<AuraFairyEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("aura", "textures/item/fairy_plain.png");
    private static final int[] FRAME_ORDER = { 0, 1, 2, 3, 4, 3, 2, 1 };
    private static final float FRAME_COUNT = 5.0F;

    public AuraFairyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
        AuraFairyEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.5F, 0.5F, 0.5F);

        int frame = FRAME_ORDER[Math.floorMod(entity.tickCount, FRAME_ORDER.length)];
        float v0 = frame / FRAME_COUNT;
        float v1 = (frame + 1) / FRAME_COUNT;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 0.0F, v0, packedLight);
        vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 0.0F, v1, packedLight);
        vertex(vertices, pose, 0.5F, -0.5F, 0.0F, 1.0F, v1, packedLight);
        vertex(vertices, pose, 0.5F, 0.5F, 0.0F, 1.0F, v0, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        vertices.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(AuraFairyEntity entity) {
        return TEXTURE;
    }
}
