package pixlepix.auracascade.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.common.NeoForge;
import pixlepix.auracascade.aura.AuraInspectionText;
import pixlepix.auracascade.aura.AuraConsumerInspectionText;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpLogic;
import pixlepix.auracascade.block.entity.VortexControllerBlockEntity;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.block.entity.LateGameBlockEntity;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.compat.client.AuraAccessoryClient;
import pixlepix.auracascade.item.AuraDiscoverability;
import pixlepix.auracascade.mixin.client.CreativeModeInventoryScreenAccessor;

public final class AuraCascadeClient {
    private static final int MARGIN = 6;
    private static final int ROW_GAP = 1;
    private static final int ROW_BACKGROUND = 0xA8000000;
    private static final EnumSet<AuraColor> visibleAuraColors = EnumSet.noneOf(AuraColor.class);
    private static AuraNetworkBlockEntity inspectedNetwork;
    private static boolean introducedCreativeTab;

    private AuraCascadeClient() {
    }

    public static void bootstrapClient(IEventBus modBus) {
        AuraItemModels.bootstrapClient(modBus);
        AuraFairyClientRegistry.bootstrapClient(modBus);
        pixlepix.auracascade.block.entity.client.MinerExplosionClient.bootstrapClient(modBus);
        pixlepix.auracascade.block.entity.client.VortexPedestalRenderer.bootstrapClient(modBus);
        AuraAccessoryClient.bootstrapClient(modBus);
        BookshelfCoordinatorClientNetworking.register(modBus);
        NeoForge.EVENT_BUS.addListener(AuraCascadeClient::renderInspectionHud);
        NeoForge.EVENT_BUS.addListener(AuraCascadeClient::selectAuraTabOnFirstOpen);
    }

    private static void selectAuraTabOnFirstOpen(ScreenEvent.Init.Post event) {
        if (introducedCreativeTab || !(event.getScreen() instanceof CreativeModeInventoryScreen creative)) {
            return;
        }

        CreativeModeInventoryScreenAccessor accessor = (CreativeModeInventoryScreenAccessor) (Object) creative;
        CreativeModeTab auraTab = AuraDiscoverability.AURA_TAB;
        for (CreativeTabsScreenPage page : accessor.aura$getPages()) {
            if (page.getVisibleTabs().contains(auraTab)) {
                creative.setCurrentPage(page);
                accessor.aura$selectTab(auraTab);
                introducedCreativeTab = true;
                return;
            }
        }
    }

    private static void renderInspectionHud(RenderGuiEvent.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        DeltaTracker tickCounter = event.getPartialTick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
            || minecraft.level == null
            || minecraft.screen != null
            || minecraft.options.hideGui
            || minecraft.getDebugOverlay().showDebugScreen()) {
            resetInspection();
            return;
        }

        double reach = minecraft.player.blockInteractionRange();
        // Entity targeting can briefly cover a node while its block remains under the crosshair.
        HitResult blockHit = minecraft.player.pick(reach, tickCounter.getGameTimeDeltaPartialTick(true), false);
        if (!(blockHit instanceof BlockHitResult hitResult)
            || hitResult.getType() != HitResult.Type.BLOCK) {
            resetInspection();
            return;
        }

        if (minecraft.player.getEyePosition().distanceToSqr(hitResult.getLocation()) > reach * reach) {
            resetInspection();
            return;
        }

        var blockEntity = minecraft.level.getBlockEntity(hitResult.getBlockPos());
        if (!(blockEntity instanceof AuraNetworkBlockEntity)
            && !(blockEntity instanceof AuraConsumerBlockEntity)
            && !(blockEntity instanceof LateGameBlockEntity)
            && !(blockEntity instanceof VortexControllerBlockEntity)) {
            resetInspection();
            return;
        }

        AuraNetworkBlockEntity networkTarget = blockEntity instanceof AuraNetworkBlockEntity network ? network : null;
        if (networkTarget != inspectedNetwork) {
            resetInspection();
            inspectedNetwork = networkTarget;
        }

        Optional<AuraInspectionText.PumpReadout> pumpReadout = Optional.empty();
        if (blockEntity instanceof AuraPumpBlockEntity pump) {
            AuraPumpLogic.PumpState pumpState = pump.pumpState();
            pumpReadout = Optional.of(new AuraInspectionText.PumpReadout(
                pumpState.power(),
                pumpState.speed(),
                pumpState.active(),
                pump.pumpInhibited()
            ));
        }

        List<Component> lines = new ArrayList<>();
        lines.add(blockEntity.getBlockState().getBlock().getName());
        if (blockEntity instanceof AuraNetworkBlockEntity network) {
            if (network instanceof AuraNodeBlockEntity node && node.isCapacitor()) {
                lines.add(Component.translatable("text.aura.hud.capacitor.threshold", node.capacitorThreshold()));
            }
            var inspection = network.inspectionState();
            for (AuraColor color : AuraColor.values()) {
                if (inspection.storage().get(color) > 0) {
                    visibleAuraColors.add(color);
                }
            }
            for (AuraInspectionText.Line line : AuraInspectionText.format(inspection, pumpReadout, visibleAuraColors)) {
                lines.add(Component.translatable(line.translationKey(), line.arguments().toArray())
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(line.color()))));
            }
        }
        if (blockEntity instanceof AuraConsumerBlockEntity consumer) {
            for (AuraConsumerInspectionText.Line line : AuraConsumerInspectionText.format(consumer.inspectionState())) {
                lines.add(Component.translatable(line.translationKey(), line.arguments().toArray())
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(line.color()))));
            }
        } else if (blockEntity instanceof LateGameBlockEntity consumer) {
            var snapshot = consumer.inspectionSnapshot();
            lines.add(Component.translatable("text.aura.hud.consumer.progress", snapshot.progress(), snapshot.maxProgress()));
            lines.add(Component.translatable("text.aura.hud.consumer.last_power", snapshot.lastPower()));
            lines.add(Component.translatable("text.aura.hud.consumer.stored_power", snapshot.storedPower()));
            if (snapshot.minerCharge() > 0) {
                lines.add(Component.translatable("text.aura.hud.miner.charge", snapshot.minerCharge()));
            }
            if (snapshot.ritualCellsRemaining() > 0) {
                lines.add(Component.translatable("text.aura.hud.ritual.remaining", snapshot.ritualCellsRemaining()));
            }
        } else if (blockEntity instanceof VortexPedestalBlockEntity pedestal) {
            var receipt = pedestal.inspectionSnapshot();
            var item = pedestal.heldItem();
            lines.add(item.isEmpty() ? Component.translatable("text.aura.hud.vortex.empty") : item.getHoverName());
            if (receipt.required() > 0) {
                lines.add(vortexPower(receipt.color(), receipt.received(), receipt.required()));
            } else {
                lines.add(Component.translatable("text.aura.hud.vortex.no_recipe"));
            }
        } else if (blockEntity instanceof VortexControllerBlockEntity controller) {
            var snapshot = controller.inspectionSnapshot(minecraft.level, hitResult.getBlockPos());
            if (snapshot.pedestals().isEmpty()) {
                lines.add(Component.translatable("text.aura.hud.vortex.no_recipe"));
            } else {
                lines.add(Component.translatable(snapshot.resultDescriptionId()));
                lines.add(Component.translatable("text.aura.hud.vortex.total", snapshot.receivedPower(), snapshot.requiredPower()));
                for (var pedestal : snapshot.pedestals()) {
                    lines.add(Component.translatable(pedestal.itemDescriptionId()).append(": ")
                        .append(vortexPower(pedestal.color(), pedestal.received(), pedestal.required())));
                }
            }
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int maxTextWidth = screenWidth - MARGIN * 2;
        if (maxTextWidth <= 0) {
            return;
        }

        int y = MARGIN;
        int bottom = screenHeight - MARGIN;
        for (Component line : lines) {
            for (FormattedCharSequence wrappedLine : minecraft.font.split(line, maxTextWidth)) {
                if (y + minecraft.font.lineHeight > bottom) {
                    return;
                }

                int width = minecraft.font.width(wrappedLine);
                graphics.fill(MARGIN - 2, y - 1, MARGIN + width + 2, y + minecraft.font.lineHeight, ROW_BACKGROUND);
                graphics.drawString(minecraft.font, wrappedLine, MARGIN, y, 0xFFFFFF, true);
                y += minecraft.font.lineHeight + ROW_GAP;
            }
        }
    }

    private static void resetInspection() {
        inspectedNetwork = null;
        visibleAuraColors.clear();
    }

    private static Component vortexPower(AuraColor color, int received, int required) {
        Component colorName = Component.translatable(color == null || color == AuraColor.WHITE
            ? "text.aura.hud.vortex.any_color" : "text.aura.color." + color.id());
        return Component.translatable("text.aura.hud.vortex.power", colorName, received, required);
    }
}
