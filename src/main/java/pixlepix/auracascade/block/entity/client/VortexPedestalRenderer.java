package pixlepix.auracascade.block.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;

public final class VortexPedestalRenderer implements BlockEntityRenderer<
    VortexPedestalBlockEntity,
    VortexPedestalRenderer.PedestalRenderState
> {
    public VortexPedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void bootstrapClient() {
        BlockEntityRendererRegistry.register(AuraContent.VORTEX_PEDESTAL_BLOCK_ENTITY, VortexPedestalRenderer::new);
    }

    @Override
    public PedestalRenderState createRenderState() {
        return new PedestalRenderState();
    }

    @Override
    public void extractRenderState(
        VortexPedestalBlockEntity pedestal,
        PedestalRenderState state,
        float partialTick,
        Vec3 cameraPos,
        CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(pedestal, state, partialTick, cameraPos, breakProgress);
        state.item.clear();
        var level = pedestal.getLevel();
        state.time = level == null ? 0.0D : level.getGameTime() + partialTick;
        ItemStack stack = pedestal.heldItem();
        if (!stack.isEmpty() && level != null) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                state.item, stack, ItemDisplayContext.GROUND, level, null, 0
            );
        }
    }

    @Override
    public void submit(
        PedestalRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.16D + Math.sin(state.time * 0.12D) * 0.04D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) (state.time * 0.05D), 0.0F, 1.0F, 0.0F)));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    public static final class PedestalRenderState extends BlockEntityRenderState {
        private final ItemStackRenderState item = new ItemStackRenderState();
        private double time;
    }
}
