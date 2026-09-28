package pixlepix.auracascade.block.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;

public final class VortexPedestalRenderer implements BlockEntityRenderer<VortexPedestalBlockEntity> {
    public VortexPedestalRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(VortexPedestalRenderer::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AuraContent.VORTEX_PEDESTAL_BLOCK_ENTITY, VortexPedestalRenderer::new);
    }

    @Override
    public void render(VortexPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stack = pedestal.heldItem();
        if (stack.isEmpty()) {
            return;
        }
        double time = pedestal.getLevel() == null ? 0.0D : pedestal.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.16D + Math.sin(time * 0.12D) * 0.04D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) (time * 0.05D), 0.0F, 1.0F, 0.0F)));
        poseStack.scale(0.7F, 0.7F, 0.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND,
            packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, pedestal.getLevel(), 0);
        poseStack.popPose();
    }
}
