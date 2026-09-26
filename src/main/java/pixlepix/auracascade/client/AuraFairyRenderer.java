package pixlepix.auracascade.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pixlepix.auracascade.fairy.AuraFairyEntity;

public final class AuraFairyRenderer extends EntityRenderer<AuraFairyEntity, AuraFairyRenderer.FairyRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("aura", "textures/item/fairy_plain.png");
    private static final int[] FRAME_ORDER = { 0, 1, 2, 3, 4, 3, 2, 1 };
    private static final float FRAME_COUNT = 5.0F;

    public AuraFairyRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public FairyRenderState createRenderState() {
        return new FairyRenderState();
    }

    @Override
    public void extractRenderState(AuraFairyEntity entity, FairyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.animationTick = entity.tickCount;
    }

    @Override
    public void submit(FairyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.5F, 0.5F, 0.5F);

        int frame = FRAME_ORDER[Math.floorMod(state.animationTick, FRAME_ORDER.length)];
        float v0 = frame / FRAME_COUNT;
        float v1 = (frame + 1) / FRAME_COUNT;
        int packedLight = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE), (pose, vertices) -> {
            vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 0.0F, v0, packedLight);
            vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 0.0F, v1, packedLight);
            vertex(vertices, pose, 0.5F, -0.5F, 0.0F, 1.0F, v1, packedLight);
            vertex(vertices, pose, 0.5F, 0.5F, 0.0F, 1.0F, v0, packedLight);
        });
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

    public static final class FairyRenderState extends EntityRenderState {
        private int animationTick;
    }
}
