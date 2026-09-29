package pixlepix.auracascade.qa.neoforge.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import pixlepix.auracascade.aura.AuraConsumerInspectionText;
import pixlepix.auracascade.aura.AuraInspectionText;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.item.AuraDiscoverability;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.gui.GuiBookLanding;

final class AuraQaUiClient {
    private static final String ACTION = System.getProperty("aura.qa.ui.action", "").trim().toLowerCase(Locale.ROOT);
    private static final int REQUESTED_GUI_SCALE = Integer.getInteger("aura.qa.ui.guiScale", 0);
    private static final int TIMEOUT_TICKS = 2_400;
    private static final long TIMEOUT_NANOS = 180_000_000_000L;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> ACTIONS = Set.of("creative", "guide", "hud", "placement", "animation");

    private static final JsonObject REPORT = new JsonObject();
    private static final JsonArray PHASES = new JsonArray();
    private static final JsonArray SCREENSHOTS = new JsonArray();
    private static final JsonArray ACTION_PATH = new JsonArray();
    private static final JsonArray ANIMATION_SAMPLES = new JsonArray();

    private static Stage stage = Stage.WAIT_WORLD;
    private static boolean started;
    private static boolean finished;
    private static int clientTicks;
    private static int fixtureReadyTick;
    private static int stableFrames;
    private static CompletableFuture<Fixture> fixturePreparation;
    private static Fixture fixture;
    private static JsonObject activePhase;
    private static String pendingScreenshot;
    private static String pendingScreenshotPhase;
    private static String pendingScreenClass;
    private static Stage stageAfterScreenshot;
    private static int pendingScreenshotFrames;
    private static int screenshotSerial;
    private static BlockPos placementPosition;
    private static boolean placementInteractionAccepted;
    private static int placementStartTick;
    private static int lastWorldCaptureTick = Integer.MIN_VALUE;
    private static boolean animationTriggered;
    private static int animationStartTick;
    private static int lastAnimationCaptureTick = Integer.MIN_VALUE;
    private static int animationCaptureCount;
    private static int lastAnimationSampleTick = Integer.MIN_VALUE;
    private static long initialRawIronCount;
    private static long initialIronIngotCount;
    private static BlockPos animationConsumerPosition;
    private static boolean animationBaselineCaptured;
    private static long startedAtNanos;
    private static Path outputDirectory;

    private AuraQaUiClient() {
    }

    static void bootstrapClient(IEventBus modBus) {
        if (ACTION.isBlank()) {
            return;
        }
        modBus.addListener(AuraQaUiClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            NeoForge.EVENT_BUS.addListener(AuraQaUiClient::onClientTick);
            NeoForge.EVENT_BUS.addListener(AuraQaUiClient::onRenderFrame);
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (finished) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        clientTicks++;
        try {
            if (!started) {
                start(client);
            }
            if (finished) {
                return;
            }
            if (timedOut()) {
                fail(client, "client_action_timeout", "The requested action did not reach its observable state before timeout.");
                return;
            }
            if (stage == Stage.WAIT_WORLD && client.level != null && client.player != null
                    && client.screen == null && client.getSingleplayerServer() != null) {
                beginFixturePreparation(client);
            }
            if (stage == Stage.PREPARING) {
                completePreparation(client);
                return;
            }
            if (client.level == null || client.player == null || client.gameMode == null
                    || client.getSingleplayerServer() == null) {
                return;
            }
            advance(client);
        } catch (Exception | LinkageError exception) {
            fail(client, "client_driver_exception", exception.toString());
        }
    }

    private static void start(Minecraft client) {
        started = true;
        startedAtNanos = System.nanoTime();
        outputDirectory = outputDirectory(client);
        REPORT.addProperty("schemaVersion", 1);
        REPORT.addProperty("modId", AuraQaUiMod.MOD_ID);
        REPORT.addProperty("action", ACTION);
        REPORT.addProperty("fixtureOutcomeInjection", false);
        REPORT.addProperty("status", "running");
        REPORT.addProperty("visualVerdict", "not_evaluated");
        REPORT.addProperty("workflowClaim", "Fixtures establish only player, block, and raw-item preconditions; gameplay outcomes are produced by normal actions and ticks. Screenshots require independent visual review.");
        REPORT.add("phases", PHASES);
        REPORT.add("screenshots", SCREENSHOTS);
        REPORT.add("actionPath", ACTION_PATH);
        REPORT.add("animationSamples", ANIMATION_SAMPLES);

        if (!ACTIONS.contains(ACTION)) {
            fail(client, "unknown_action", "Set aura.qa.ui.action to creative, guide, hud, placement, or animation.");
            return;
        }
        if (ACTION.equals("guide") && REQUESTED_GUI_SCALE != 2 && REQUESTED_GUI_SCALE != 3) {
            fail(client, "guide_gui_scale_required", "Guide action requires aura.qa.ui.guiScale=2 or 3.");
            return;
        }
        if (REQUESTED_GUI_SCALE != 0 && REQUESTED_GUI_SCALE != 2 && REQUESTED_GUI_SCALE != 3) {
            fail(client, "unsupported_gui_scale", "Only guiScale 2 or 3 is supported by this bounded run.");
            return;
        }
        if (REQUESTED_GUI_SCALE != 0) {
            client.options.guiScale().set(REQUESTED_GUI_SCALE);
            client.resizeDisplay();
        }
        REPORT.addProperty("requestedGuiScale", REQUESTED_GUI_SCALE == 0 ? client.options.guiScale().get() : REQUESTED_GUI_SCALE);
        REPORT.addProperty("guiScaleApplied", client.options.guiScale().get());

        String auraJarFailure = auditAuraJar();
        if (auraJarFailure != null) {
            fail(client, auraJarFailure, "Native NeoForge ModList could not verify the loaded Aura JAR.");
            return;
        }
        if (!ModList.get().isLoaded("patchouli")) {
            fail(client, "patchouli_not_loaded", "Patchouli is required for the Encyclopedia action and guide audit.");
            return;
        }

        REPORT.addProperty("fixtureSetup", "bounded integrated-server preconditions only; no Aura storage, power, progress, or output is seeded");
        REPORT.add("fixturePreconditions", new JsonArray());
        stage = Stage.WAIT_WORLD;
        writeManifest();
    }

    private static void beginFixturePreparation(Minecraft client) {
        stage = Stage.PREPARING;
        UUID playerId = client.player.getUUID();
        var server = client.getSingleplayerServer();
        fixturePreparation = new CompletableFuture<>();
        CompletableFuture<Fixture> completion = fixturePreparation;
        server.execute(() -> {
            try {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
                if (serverPlayer == null) {
                    throw new IllegalStateException("The integrated-server player is unavailable");
                }
                completion.complete(prepareFixture(serverPlayer, ACTION));
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
    }

    private static void completePreparation(Minecraft client) {
        if (fixturePreparation == null || !fixturePreparation.isDone()) {
            return;
        }
        try {
            fixture = fixturePreparation.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            fail(client, "fixture_preparation_failed", cause.toString());
            return;
        }
        JsonArray preconditions = REPORT.getAsJsonArray("fixturePreconditions");
        fixture.preconditions().forEach(preconditions::add);
        JsonObject fixtureJson = new JsonObject();
        addPosition(fixtureJson, "hudNode", fixture.hudNode());
        addPosition(fixtureJson, "placementTarget", fixture.placementTarget());
        addPosition(fixtureJson, "consumer", fixture.consumer());
        addPosition(fixtureJson, "energyReceiver", fixture.energyReceiver());
        addPosition(fixtureJson, "energySource", fixture.energySource());
        REPORT.add("fixture", fixtureJson);
        if (fixture.camera() != null && client.player != null) {
            client.player.setYRot(fixture.camera().yaw());
            client.player.setXRot(fixture.camera().pitch());
            client.player.setYHeadRot(fixture.camera().yaw());
        }
        fixtureReadyTick = clientTicks;
        stage = Stage.WAIT_FIXTURE_SYNC;
        writeManifest();
    }

    private static Stage initialStage() {
        return switch (ACTION) {
            case "creative" -> Stage.OPEN_CREATIVE;
            case "guide" -> Stage.OPEN_GUIDE;
            case "hud" -> Stage.CAPTURE_HUD;
            case "placement" -> Stage.PLACE_NODE;
            case "animation" -> Stage.START_ANIMATION;
            default -> Stage.WAIT_WORLD;
        };
    }

    private static void advance(Minecraft client) throws Exception {
        switch (stage) {
            case WAIT_WORLD -> {
            }
            case WAIT_FIXTURE_SYNC -> {
                if (client.screen == null && clientTicks - fixtureReadyTick >= 20) {
                    stage = initialStage();
                }
            }
            case OPEN_CREATIVE -> openCreative(client);
            case WAIT_CREATIVE -> inspectCreative(client);
            case OPEN_GUIDE -> openGuide(client);
            case WAIT_GUIDE_LANDING -> inspectGuideLanding(client);
            case RUN_GUIDE_LAYOUT -> auditGuide(client);
            case CLICK_GUIDE_LINK -> clickGuideLink(client);
            case WAIT_GUIDE_ENTRY -> inspectGuideEntry(client);
            case CLICK_GUIDE_BACK -> clickGuideBack(client);
            case WAIT_GUIDE_BACK -> inspectGuideBack(client);
            case CAPTURE_HUD -> startHudPhase();
            case PLACE_NODE -> placeNode(client);
            case WAIT_PLACEMENT -> inspectPlacement(client);
            case CAPTURE_PLACEMENT -> {
            }
            case START_ANIMATION -> startAnimation(client);
            case WAIT_ANIMATION -> inspectAnimation(client);
            case DONE -> finishCompletedAction(client);
        }
    }

    private static void openCreative(Minecraft client) {
        if (client.screen != null) {
            fail(client, "creative_screen_precondition_failed", "The client must start with no screen open.");
            return;
        }
        if (client.player == null || !client.player.isCreative()) {
            fail(client, "creative_mode_required", "Start in a creative-mode singleplayer world.");
            return;
        }
        activePhase = beginPhase("creative-tab-opening");
        activePhase.addProperty("inputPath", "KeyMapping.click(options.keyInventory.getKey())");
        ACTION_PATH.add("Minecraft inventory key mapping click");
        KeyMapping.click(client.options.keyInventory.getKey());
        stage = Stage.WAIT_CREATIVE;
        writeManifest();
    }

    private static void inspectCreative(Minecraft client) {
        if (!(client.screen instanceof CreativeModeInventoryScreen creative)) {
            return;
        }
        JsonObject state = screenState(client);
        boolean pageContainsAura = creative.getCurrentPage() != null
            && creative.getCurrentPage().getVisibleTabs().contains(AuraDiscoverability.AURA_TAB);
        List<String> expected = AuraDiscoverability.AURA_TAB.getDisplayItems().stream()
            .filter(stack -> !stack.isEmpty())
            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
            .distinct().sorted().toList();
        List<String> actual = creative.getMenu().items.stream()
            .filter(stack -> !stack.isEmpty())
            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
            .distinct().sorted().toList();
        boolean contentMatches = expected.equals(actual);
        JsonObject content = new JsonObject();
        content.addProperty("tabId", "aura:aura");
        content.addProperty("tabTitle", AuraDiscoverability.AURA_TAB.getDisplayName().getString());
        content.addProperty("pageContainsAuraTab", pageContainsAura);
        content.addProperty("expectedItemCount", expected.size());
        content.addProperty("actualMenuItemCount", actual.size());
        content.addProperty("exactItemSetMatch", contentMatches);
        content.add("expectedItemIds", stringArray(expected));
        content.add("actualMenuItemIds", stringArray(actual));
        state.add("creativeTab", content);
        activePhase.add("screen", state);
        activePhase.addProperty("screenOpened", true);
        activePhase.addProperty("openedVia", "inventory_key_mapping");
        activePhase.addProperty("stateCheckPassed", pageContainsAura && contentMatches);
        activePhase.addProperty("status", pageContainsAura && contentMatches ? "pass" : "fail");
        requestScreenshot(client, "creative-aura-tab.png", activePhase, Stage.DONE);
        if (!(pageContainsAura && contentMatches)) {
            REPORT.addProperty("actionFailure", "creative_aura_tab_or_content_mismatch");
        }
    }

    private static void openGuide(Minecraft client) {
        if (client.screen != null) {
            fail(client, "guide_screen_precondition_failed", "The client must start with no screen open.");
            return;
        }
        if (client.player == null || !client.player.getMainHandItem().is(AuraItems.ENCYCLOPEDIA_AURA)) {
            fail(client, "encyclopedia_item_required", "Hold aura:encyclopedia_aura in the main hand before the run.");
            return;
        }
        activePhase = beginPhase("guide-open-via-item-use");
        activePhase.addProperty("item", BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).toString());
        activePhase.addProperty("inputPath", "MultiPlayerGameMode.useItem(player, MAIN_HAND)");
        ACTION_PATH.add("Use Encyclopedia Aura from the main hand through MultiPlayerGameMode.useItem");
        InteractionResult result = client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        activePhase.addProperty("clientInteractionResult", result.name());
        stage = Stage.WAIT_GUIDE_LANDING;
        writeManifest();
    }

    private static void inspectGuideLanding(Minecraft client) {
        if (!(client.screen instanceof GuiBookLanding landing)) {
            return;
        }
        JsonObject state = screenState(client);
        activePhase.add("screen", state);
        activePhase.addProperty("landingOpened", true);
        activePhase.addProperty("status", "pass");
        requestScreenshot(client, "guide-landing.png", activePhase, Stage.RUN_GUIDE_LAYOUT);
    }

    private static void auditGuide(Minecraft client) {
        activePhase = beginPhase("full-guide-font-layout");
        activePhase.addProperty("auditClass", "pixlepix.auracascade.qa.neoforge.ui.GuideLayoutAudit");
        activePhase.addProperty("auditSource", "Adapted from scripts/qa/observer/GuideLayoutAudit.java");
        activePhase.addProperty("runtimeModList", "net.neoforged.fml.ModList");
        try {
            JsonObject audit = GuideLayoutAudit.audit(client);
            Path auditPath = outputDirectory.resolve("guide-layout.json");
            Files.createDirectories(outputDirectory);
            Files.writeString(auditPath, JSON.toJson(audit), StandardCharsets.UTF_8);
            activePhase.addProperty("reportPath", auditPath.toAbsolutePath().toString());
            activePhase.addProperty("fullGuideLayoutAudit", true);
            activePhase.addProperty("entryCount", audit.has("entryCount") ? audit.get("entryCount").getAsInt() : 0);
            activePhase.addProperty("pageCount", audit.has("pageCount") ? audit.get("pageCount").getAsInt() : 0);
            activePhase.addProperty("auditPassed", audit.has("success") && audit.get("success").getAsBoolean());
            activePhase.addProperty("status", activePhase.get("auditPassed").getAsBoolean() ? "pass" : "fail");
            REPORT.addProperty("guideLayoutReport", auditPath.toAbsolutePath().toString());
            REPORT.addProperty("guideLayoutPassed", activePhase.get("auditPassed").getAsBoolean());
            if (!activePhase.get("auditPassed").getAsBoolean()) {
                REPORT.addProperty("actionFailure", "full_guide_font_layout_audit_failed");
            }
        } catch (Exception | LinkageError exception) {
            activePhase.addProperty("fullGuideLayoutAudit", false);
            activePhase.addProperty("status", "fail");
            activePhase.addProperty("failure", exception.toString());
            REPORT.addProperty("actionFailure", "full_guide_font_layout_audit_exception");
        }
        stage = Stage.CLICK_GUIDE_LINK;
        writeManifest();
    }

    private static void clickGuideLink(Minecraft client) throws Exception {
        if (!(client.screen instanceof GuiBookLanding landing)) {
            fail(client, "guide_landing_lost", "The live Patchouli landing screen was not active before link click.");
            return;
        }
        activePhase = beginPhase("guide-hyperlink-navigation");
        activePhase.addProperty("targetEntry", "aura:getting_started");
        activePhase.addProperty("clickPath", "GuiBookLanding.mouseClicked at live rendered hyperlink bounds");
        GuideLayoutAudit.LinkTarget target = GuideLayoutAudit.landingLink(client, landing, "Getting");
        if (target == null) {
            activePhase.addProperty("status", "fail");
            activePhase.addProperty("failure", "The rendered Getting Started link fragment was not found.");
            fail(client, "guide_link_target_missing", "No clickable Getting Started fragment was found on the live landing renderer.");
            return;
        }
        activePhase.add("linkBounds", target.bounds());
        boolean clickAccepted = landing.mouseClicked(target.x(), target.y(), 0);
        activePhase.addProperty("mouseClickAccepted", clickAccepted);
        ACTION_PATH.add("Click the rendered Getting Started link on the Patchouli landing screen");
        stage = Stage.WAIT_GUIDE_ENTRY;
        writeManifest();
    }

    private static void inspectGuideEntry(Minecraft client) {
        if (!(client.screen instanceof GuiBookEntry entryScreen)) {
            return;
        }
        String entryId = entryScreen.getEntry().getId().toString();
        JsonObject state = screenState(client);
        activePhase.add("screen", state);
        activePhase.addProperty("entryId", entryId);
        activePhase.addProperty("expectedEntryId", "aura:getting_started");
        boolean matched = entryId.equals("aura:getting_started");
        activePhase.addProperty("navigationPassed", matched);
        activePhase.addProperty("status", matched ? "pass" : "fail");
        if (!matched) {
            REPORT.addProperty("actionFailure", "guide_link_opened_unexpected_entry");
        }
        requestScreenshot(client, "guide-getting-started.png", activePhase, Stage.CLICK_GUIDE_BACK);
    }

    private static void clickGuideBack(Minecraft client) throws Exception {
        if (!(client.screen instanceof GuiBook gui)) {
            fail(client, "guide_entry_lost", "The entry screen was not active before the back-button click.");
            return;
        }
        activePhase = beginPhase("guide-back-navigation");
        activePhase.addProperty("clickPath", "GuiBook.mouseClicked on its live back widget");
        AbstractWidget back = GuideLayoutAudit.backButton(gui);
        if (back == null) {
            activePhase.addProperty("status", "fail");
            fail(client, "guide_back_button_missing", "The live Patchouli back widget was not found.");
            return;
        }
        GuideLayoutAudit.LinkTarget target = GuideLayoutAudit.widgetCenter(gui, back);
        activePhase.add("backButtonBounds", target.bounds());
        boolean clickAccepted = gui.mouseClicked(target.x(), target.y(), 0);
        activePhase.addProperty("mouseClickAccepted", clickAccepted);
        ACTION_PATH.add("Click Patchouli's visible back widget");
        stage = Stage.WAIT_GUIDE_BACK;
        writeManifest();
    }

    private static void inspectGuideBack(Minecraft client) {
        if (!(client.screen instanceof GuiBookLanding landing)) {
            return;
        }
        activePhase.add("screen", screenState(client));
        activePhase.addProperty("returnedToLanding", true);
        activePhase.addProperty("status", "pass");
        requestScreenshot(client, "guide-back-to-landing.png", activePhase, Stage.DONE);
    }

    private static void placeNode(Minecraft client) {
        if (client.screen != null || client.player == null || client.gameMode == null) {
            fail(client, "placement_screen_precondition_failed", "Node placement requires a live world and no open screen.");
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        if (!held.is(AuraContent.AURA_NODE.asItem())) {
            fail(client, "aura_node_item_required", "Hold an Aura Node block item before the run.");
            return;
        }
        BlockHitResult hit;
        if (client.hitResult instanceof BlockHitResult blockHit) {
            hit = blockHit;
            placementPosition = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else {
            fail(client, "placement_target_required", "Aim at a replaceable supported block before placement.");
            return;
        }
        if (!client.level.getBlockState(placementPosition).canBeReplaced()) {
            fail(client, "placement_target_obstructed", "The client-visible placement target is not replaceable.");
            return;
        }
        if (!placementPosition.equals(fixture.placementTarget())) {
            fail(client, "placement_aim_mismatch", "The crosshair must hit the fixture's straight-link target.");
            return;
        }
        activePhase = beginPhase("aura-node-placement-and-preview");
        activePhase.addProperty("item", "aura:aura_node");
        activePhase.addProperty("actionApi", "MultiPlayerGameMode.useItemOn");
        addPosition(activePhase, "placementTarget", placementPosition);
        addPosition(activePhase, "supportBlock", hit.getBlockPos());
        activePhase.addProperty("face", hit.getDirection().getName());
        InteractionResult result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        placementInteractionAccepted = result.consumesAction();
        placementStartTick = clientTicks;
        activePhase.addProperty("clientInteractionResult", result.name());
        activePhase.addProperty("interactionAccepted", placementInteractionAccepted);
        activePhase.addProperty("outcomeInjected", false);
        ACTION_PATH.add("Place Aura Node with MultiPlayerGameMode.useItemOn; server performs normal BlockItem placement");
        stage = Stage.WAIT_PLACEMENT;
        writeManifest();
    }

    private static void inspectPlacement(Minecraft client) {
        if (client.level == null || placementPosition == null || activePhase == null) {
            return;
        }
        if (client.level.getBlockState(placementPosition).is(AuraContent.AURA_NODE)
                && clientTicks - placementStartTick >= 2) {
            boolean confirmed = placementInteractionAccepted;
            activePhase.addProperty("placedNodeVisibleOnClient", true);
            activePhase.addProperty("previewObservationWindowTicks", Math.max(0, clientTicks - placementStartTick));
            activePhase.addProperty("blockId", "aura:aura_node");
            activePhase.addProperty("status", confirmed ? "outcome_observed" : "interaction_result_uncertain");
            stage = Stage.CAPTURE_PLACEMENT;
            stableFrames = 0;
            return;
        }
        if (clientTicks - placementStartTick > 80) {
            activePhase.addProperty("placedNodeVisibleOnClient", false);
            fail(client, "placement_outcome_not_observed", "The normal use-item-on action did not produce a visible Aura Node at its target.");
        }
    }

    private static void startHudPhase() {
        if (activePhase == null) {
            activePhase = beginPhase("stable-aura-inspection-hud");
            activePhase.addProperty("consecutiveRenderFramesRequired", 120);
            activePhase.addProperty("targetPrecondition", "Crosshair stays on an Aura Node or consumer with HUD enabled.");
        }
    }

    private static void finishCompletedAction(Minecraft client) {
        if (ACTION.equals("guide")) {
            requestGuideActionComplete(client);
        } else if (ACTION.equals("creative")) {
            boolean passed = activePhase != null && activePhase.has("stateCheckPassed")
                && activePhase.get("stateCheckPassed").getAsBoolean()
                && !REPORT.has("actionFailure");
            if (passed) {
                activePhase.addProperty("status", "pass");
                complete(client, "passed", "The inventory key opened the creative screen on the Aura tab and its live menu item set matched the tab contents.");
            } else {
                fail(client, "creative_tab_check_failed", "The creative tab or live menu content check failed.");
            }
        }
    }

    private static void startAnimation(Minecraft client) {
        if (client.screen != null || client.player == null || client.level == null) {
            fail(client, "animation_screen_precondition_failed", "Item-conversion capture requires a live world and no open screen.");
            return;
        }
        activePhase = beginPhase("dropped-item-conversion-reaction");
        activePhase.addProperty("outcomeInjected", false);
        activePhase.addProperty("reactionWindowTicks", 10);
        if (!(client.hitResult instanceof BlockHitResult hit)
                || !(client.level.getBlockEntity(hit.getBlockPos()) instanceof AuraConsumerBlockEntity consumer)
                || !client.level.getBlockState(hit.getBlockPos()).is(AuraContent.CONSUMER_BLOCK_FURNACE)) {
            fail(client, "animation_smelter_required", "Aim at a live aura:consumer_block_furnace Smelter before the run.");
            return;
        }

        animationConsumerPosition = hit.getBlockPos().immutable();
        initialRawIronCount = nearbyItemCount(client, animationConsumerPosition, Items.RAW_IRON);
        initialIronIngotCount = nearbyItemCount(client, animationConsumerPosition, Items.IRON_INGOT);
        if (initialRawIronCount == 0) {
            fail(client, "animation_input_required", "A naturally dropped Raw Iron item must be within three blocks of the aimed Smelter.");
            return;
        }
        activePhase.addProperty("consumerBlock", BuiltInRegistries.BLOCK.getKey(
            client.level.getBlockState(animationConsumerPosition).getBlock()).toString());
        activePhase.addProperty("rawIronCountBefore", initialRawIronCount);
        activePhase.addProperty("ironIngotCountBefore", initialIronIngotCount);
        activePhase.addProperty("consumerProgressBefore", consumer.inspectionState().progress());
        activePhase.addProperty("consumerStoredPowerBefore", consumer.inspectionState().storedPower());
        activePhase.addProperty("observationRadiusBlocks", 3);
        ACTION_PATH.add("Observe live Smelter state and wait for raw-iron removal plus a new iron-ingot output");
        stage = Stage.WAIT_ANIMATION;
        writeManifest();
    }

    private static void inspectAnimation(Minecraft client) {
        if (animationConsumerPosition == null || client.level == null || activePhase == null) {
            return;
        }
        var blockEntity = client.level.getBlockEntity(animationConsumerPosition);
        int progress = blockEntity instanceof AuraConsumerBlockEntity consumer
            ? consumer.inspectionState().progress() : -1;
        int power = blockEntity instanceof AuraConsumerBlockEntity consumer
            ? consumer.inspectionState().storedPower() : -1;
        long rawIron = nearbyItemCount(client, animationConsumerPosition, Items.RAW_IRON);
        long ironIngots = nearbyItemCount(client, animationConsumerPosition, Items.IRON_INGOT);
        if (clientTicks - lastAnimationSampleTick >= 20) {
            lastAnimationSampleTick = clientTicks;
            JsonObject sample = new JsonObject();
            sample.addProperty("clientTick", clientTicks);
            sample.addProperty("consumerProgress", progress);
            sample.addProperty("consumerStoredPower", power);
            sample.addProperty("rawIronCount", rawIron);
            sample.addProperty("ironIngotCount", ironIngots);
            ANIMATION_SAMPLES.add(sample);
        }
        if (!animationTriggered && rawIron < initialRawIronCount && ironIngots > initialIronIngotCount) {
            animationTriggered = true;
            animationStartTick = clientTicks;
            activePhase.addProperty("conversionOutputObserved", true);
            activePhase.addProperty("conversionObservedAtClientTick", clientTicks);
            activePhase.addProperty("rawIronCountAfter", rawIron);
            activePhase.addProperty("ironIngotCountAfter", ironIngots);
            activePhase.addProperty("consumerProgressAtObservation", progress);
            activePhase.addProperty("consumerStoredPowerAtObservation", power);
            activePhase.addProperty("status", "outcome_observed");
            ACTION_PATH.add("Observe naturally ticked Raw Iron-to-Iron Ingot conversion; no input, output, or animation outcome is injected");
            writeManifest();
        }
        if (animationTriggered && animationCaptureCount >= 3) {
            activePhase.addProperty("animationFramesCaptured", animationCaptureCount);
            complete(client, "evidence_captured", "Natural Raw Iron conversion observed and reaction frames captured; inspect screenshots.");
        }
    }

    private static long nearbyItemCount(Minecraft client, BlockPos pos, net.minecraft.world.item.Item item) {
        return client.level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D)).stream()
            .filter(entity -> entity.getItem().is(item))
            .mapToLong(entity -> entity.getItem().getCount())
            .sum();
    }

    private static void onRenderFrame(RenderFrameEvent.Post event) {
        if (finished || !started) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        try {
            if (pendingScreenshot != null) {
                if (client.screen == null || !client.screen.getClass().getName().equals(pendingScreenClass)) {
                    pendingScreenshotFrames = 0;
                    return;
                }
                if (++pendingScreenshotFrames >= 2) {
                    captureScreen(client, pendingScreenshot, pendingScreenshotPhase, client.screen);
                    pendingScreenshot = null;
                    pendingScreenshotPhase = null;
                    pendingScreenClass = null;
                    stage = stageAfterScreenshot;
                    stageAfterScreenshot = null;
                }
                return;
            }
            if (stage == Stage.CAPTURE_HUD) {
                captureHudFrames(client);
            } else if (stage == Stage.CAPTURE_PLACEMENT) {
                capturePlacementFrames(client);
            } else if (stage == Stage.WAIT_ANIMATION) {
                if (animationTriggered) {
                    captureAnimationFrames(client);
                } else {
                    captureAnimationBaseline(client);
                }
            }
        } catch (Exception | LinkageError exception) {
            fail(client, "render_capture_exception", exception.toString());
        }
    }

    private static void captureHudFrames(Minecraft client) {
        startHudPhase();
        JsonObject sample = hudTargetSample(client);
        if (sample == null || !sample.get("targetedAuraBlock").getAsBoolean()) {
            stableFrames = 0;
            if (activePhase != null) {
                activePhase.addProperty("targetValid", false);
            }
            return;
        }
        if (stableFrames == 0) {
            stableFrames = 1;
        } else {
            stableFrames++;
        }
        activePhase.addProperty("targetValid", true);
        activePhase.addProperty("consecutiveRenderFrames", stableFrames);
        if (stableFrames == 5 || stableFrames == 20 || stableFrames == 40 || stableFrames == 80 || stableFrames == 120) {
            captureWorldFrame(client, "hud-frame-" + stableFrames + ".png", "hud", sample);
        }
        if (stableFrames >= 120 && !finished) {
            activePhase.addProperty("status", "outcome_observed");
            complete(client, "evidence_captured", "The Aura HUD remained targeted for 120 consecutive rendered frames with natural crystal/transfer inputs; inspect the captured frames manually.");
        }
    }

    private static void capturePlacementFrames(Minecraft client) {
        if (client.level == null || placementPosition == null
                || !client.level.getBlockState(placementPosition).is(AuraContent.AURA_NODE)) {
            return;
        }
        if (lastWorldCaptureTick != Integer.MIN_VALUE && clientTicks - lastWorldCaptureTick < 2) {
            return;
        }
        lastWorldCaptureTick = clientTicks;
        JsonObject state = new JsonObject();
        addPosition(state, "placedNode", placementPosition);
        state.addProperty("blockId", BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(placementPosition).getBlock()).toString());
        state.addProperty("clientTick", clientTicks);
        state.addProperty("previewWindowTick", clientTicks - placementStartTick);
        state.addProperty("interactionAccepted", placementInteractionAccepted);
        captureWorldFrame(client, "placement-preview-" + (++screenshotSerial) + ".png", "placement", state);
        if (screenshotSerial >= 3) {
            activePhase.addProperty("previewFramesCaptured", screenshotSerial);
            complete(client, "evidence_captured", "Normal Aura Node placement observed; the transient preview is recorded for visual review.");
        }
    }

    private static void captureAnimationFrames(Minecraft client) {
        if (clientTicks - animationStartTick > 10 || (lastAnimationCaptureTick != Integer.MIN_VALUE
                && clientTicks - lastAnimationCaptureTick < 2)) {
            return;
        }
        if (client.level == null) {
            return;
        }
        lastAnimationCaptureTick = clientTicks;
        JsonObject state = new JsonObject();
        addPosition(state, "consumer", animationConsumerPosition);
        state.addProperty("blockId", BuiltInRegistries.BLOCK.getKey(
            client.level.getBlockState(animationConsumerPosition).getBlock()).toString());
        state.addProperty("clientTick", clientTicks);
        state.addProperty("ticksSinceObservedOutput", clientTicks - animationStartTick);
        var blockEntity = client.level.getBlockEntity(animationConsumerPosition);
        if (blockEntity instanceof AuraConsumerBlockEntity consumer) {
            state.addProperty("progress", consumer.inspectionState().progress());
            state.addProperty("storedPower", consumer.inspectionState().storedPower());
        }
        captureWorldFrame(client, "conversion-animation-" + (++animationCaptureCount) + ".png", "animation", state);
    }

    private static void captureAnimationBaseline(Minecraft client) {
        if (animationBaselineCaptured || client.level == null || client.screen != null || animationConsumerPosition == null) {
            return;
        }
        JsonObject state = new JsonObject();
        addPosition(state, "consumer", animationConsumerPosition);
        state.addProperty("blockId", "aura:consumer_block_furnace");
        state.addProperty("clientTick", clientTicks);
        state.addProperty("rawIronCountBefore", initialRawIronCount);
        state.addProperty("ironIngotCountBefore", initialIronIngotCount);
        captureWorldFrame(client, "item-conversion-before.png", "animation-baseline", state);
        animationBaselineCaptured = true;
    }

    private static JsonObject hudTargetSample(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null || client.options.hideGui
                || client.getDebugOverlay().showDebugScreen()) {
            return null;
        }
        HitResult result = client.player.pick(client.player.blockInteractionRange(), 1.0F, false);
        if (!(result instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        var blockEntity = client.level.getBlockEntity(hit.getBlockPos());
        boolean supported = blockEntity instanceof AuraNodeBlockEntity || blockEntity instanceof AuraConsumerBlockEntity;
        JsonObject sample = new JsonObject();
        sample.addProperty("targetedAuraBlock", supported);
        sample.addProperty("blockId", BuiltInRegistries.BLOCK.getKey(
            client.level.getBlockState(hit.getBlockPos()).getBlock()).toString());
        addPosition(sample, "target", hit.getBlockPos());
        sample.addProperty("screenClass", "none");
        sample.addProperty("guiScaledWidth", client.getWindow().getGuiScaledWidth());
        sample.addProperty("guiScaledHeight", client.getWindow().getGuiScaledHeight());
        sample.addProperty("clientTick", clientTicks);
        if (supported) {
            sample.add("hudTextBounds", hudTextBounds(client, blockEntity));
        }
        return sample;
    }

    private static JsonObject hudTextBounds(Minecraft client, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        List<Component> lines = new ArrayList<>();
        lines.add(blockEntity.getBlockState().getBlock().getName());
        if (blockEntity instanceof AuraNetworkBlockEntity network) {
            EnumSet<AuraColor> visibleColors = EnumSet.noneOf(AuraColor.class);
            var inspection = network.inspectionState();
            for (AuraColor color : AuraColor.values()) {
                if (inspection.storage().get(color) > 0) {
                    visibleColors.add(color);
                }
            }
            if (network instanceof AuraNodeBlockEntity node && node.isCapacitor()) {
                lines.add(Component.translatable("text.aura.hud.capacitor.threshold", node.capacitorThreshold()));
            }
            for (AuraInspectionText.Line line : AuraInspectionText.format(inspection, Optional.empty(), visibleColors)) {
                lines.add(Component.translatable(line.translationKey(), line.arguments().toArray()));
            }
        } else if (blockEntity instanceof AuraConsumerBlockEntity consumer) {
            for (AuraConsumerInspectionText.Line line : AuraConsumerInspectionText.format(consumer.inspectionState())) {
                lines.add(Component.translatable(line.translationKey(), line.arguments().toArray()));
            }
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int maxWidth = screenWidth - 12;
        int y = 6;
        int maxRight = 6;
        JsonArray rendered = new JsonArray();
        for (Component line : lines) {
            for (var wrapped : client.font.split(line, maxWidth)) {
                if (y + client.font.lineHeight > screenHeight - 6) {
                    break;
                }
                int width = client.font.width(wrapped);
                JsonObject bounds = new JsonObject();
                bounds.addProperty("text", formattedText(wrapped));
                bounds.addProperty("left", 6);
                bounds.addProperty("top", y);
                bounds.addProperty("right", 6 + width);
                bounds.addProperty("bottom", y + client.font.lineHeight);
                rendered.add(bounds);
                maxRight = Math.max(maxRight, 6 + width);
                y += client.font.lineHeight + 1;
            }
        }
        JsonObject result = new JsonObject();
        result.addProperty("derivedFromProductionHudLayout", true);
        result.addProperty("left", 6);
        result.addProperty("top", 6);
        result.addProperty("right", maxRight);
        result.addProperty("bottom", Math.max(6, y - 1));
        result.add("renderedLines", rendered);
        return result;
    }

    private static String formattedText(FormattedCharSequence sequence) {
        StringBuilder text = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            text.appendCodePoint(codePoint);
            return true;
        });
        return text.toString();
    }

    private static void captureWorldFrame(Minecraft client, String fileName, String phaseName, JsonObject state) {
        try {
            Files.createDirectories(outputDirectory);
            Path file = outputDirectory.resolve(fileName);
            try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                    throw new IllegalStateException("Main render target is empty");
                }
                image.writeToFile(file);
                JsonObject evidence = new JsonObject();
                evidence.addProperty("phase", phaseName);
                evidence.addProperty("path", file.toAbsolutePath().toString());
                evidence.addProperty("width", image.getWidth());
                evidence.addProperty("height", image.getHeight());
                evidence.addProperty("bytes", Files.size(file));
                evidence.add("state", state);
                SCREENSHOTS.add(evidence);
            }
            REPORT.addProperty("visualVerdict", "pending_manual_review");
            writeManifest();
        } catch (Exception exception) {
            REPORT.addProperty("screenshotFailure", exception.toString());
            fail(client, "screenshot_failed", exception.toString());
        }
    }

    private static void captureScreen(Minecraft client, String fileName, String phaseName, Screen screen) {
        try {
            Files.createDirectories(outputDirectory);
            Path file = outputDirectory.resolve(fileName);
            try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                    throw new IllegalStateException("Main render target is empty");
                }
                image.writeToFile(file);
                JsonObject evidence = new JsonObject();
                evidence.addProperty("phase", phaseName);
                evidence.addProperty("path", file.toAbsolutePath().toString());
                evidence.addProperty("width", image.getWidth());
                evidence.addProperty("height", image.getHeight());
                evidence.addProperty("bytes", Files.size(file));
                evidence.add("screen", screenState(client));
                SCREENSHOTS.add(evidence);
            }
            REPORT.addProperty("visualVerdict", "pending_manual_review");
            writeManifest();
        } catch (Exception exception) {
            if (activePhase != null) {
                activePhase.addProperty("screenshotFailure", exception.toString());
            }
            fail(client, "screenshot_failed", exception.toString());
        }
    }

    private static void requestScreenshot(Minecraft client, String fileName, JsonObject phase, Stage next) {
        if (client.screen == null) {
            fail(client, "screen_missing_for_capture", "The expected screen closed before its screenshot.");
            return;
        }
        pendingScreenshot = fileName;
        pendingScreenshotPhase = phase.has("name") ? phase.get("name").getAsString() : ACTION;
        pendingScreenClass = client.screen.getClass().getName();
        pendingScreenshotFrames = 0;
        stageAfterScreenshot = next;
        phase.addProperty("screenshotRequested", fileName);
        writeManifest();
    }

    private static JsonObject screenState(Minecraft client) {
        JsonObject state = new JsonObject();
        Screen screen = client.screen;
        state.addProperty("screenClass", screen == null ? "none" : screen.getClass().getName());
        state.addProperty("screenWidth", screen == null ? 0 : screen.width);
        state.addProperty("screenHeight", screen == null ? 0 : screen.height);
        state.addProperty("windowWidthPixels", client.getWindow().getWidth());
        state.addProperty("windowHeightPixels", client.getWindow().getHeight());
        state.addProperty("guiScaleOption", client.options.guiScale().get());
        state.addProperty("dimension", client.level == null ? "unavailable" : client.level.dimension().location().toString());
        if (screen instanceof CreativeModeInventoryScreen creative) {
            state.addProperty("currentPageTabCount", creative.getCurrentPage() == null ? 0 : creative.getCurrentPage().getVisibleTabs().size());
            state.addProperty("inventoryMenuItemCount", creative.getMenu().items.size());
        }
        if (screen instanceof GuiBook bookGui) {
            JsonObject bounds = new JsonObject();
            bounds.addProperty("left", bookGui.bookLeft);
            bounds.addProperty("top", bookGui.bookTop);
            bounds.addProperty("width", GuiBook.FULL_WIDTH);
            bounds.addProperty("height", GuiBook.FULL_HEIGHT);
            try {
                bounds.addProperty("rendererScale", GuideLayoutAudit.guiScaleFactor(bookGui));
            } catch (ReflectiveOperationException exception) {
                bounds.addProperty("rendererScaleError", exception.toString());
            }
            state.add("bookBounds", bounds);
        }
        if (screen instanceof GuiBookEntry entry) {
            state.addProperty("entryId", entry.getEntry().getId().toString());
            state.addProperty("spread", entry.getSpread());
        }
        return state;
    }

    private static void requestGuideActionComplete(Minecraft client) {
        boolean phasesPassed = PHASES.asList().stream().allMatch(element -> !element.isJsonObject()
            || !element.getAsJsonObject().has("status")
            || Set.of("pass", "outcome_observed").contains(element.getAsJsonObject().get("status").getAsString()));
        if (phasesPassed) {
            complete(client, "passed", "The book opened from item use, its rendered hyperlink navigated, the back widget returned, and the full-font audit passed.");
        } else {
            fail(client, "guide_phase_failed", "One or more guide navigation/layout phases failed; see each phase report.");
        }
    }

    private static JsonObject beginPhase(String name) {
        JsonObject phase = new JsonObject();
        phase.addProperty("name", name);
        phase.addProperty("status", "running");
        phase.addProperty("clientTick", clientTicks);
        PHASES.add(phase);
        writeManifest();
        return phase;
    }

    private static void complete(Minecraft client, String status, String claim) {
        if (finished) {
            return;
        }
        finished = true;
        stage = Stage.DONE;
        REPORT.addProperty("status", status);
        REPORT.addProperty("claim", claim);
        REPORT.addProperty("clientTicks", clientTicks);
        REPORT.addProperty("wallSeconds", (System.nanoTime() - startedAtNanos) / 1_000_000_000.0D);
        if (SCREENSHOTS.size() > 0) {
            REPORT.addProperty("visualVerdict", "pending_manual_review");
        }
        addRuntimeSnapshot(client);
        writeManifest();
        client.stop();
    }

    private static void fail(Minecraft client, String code, String detail) {
        if (finished) {
            return;
        }
        finished = true;
        stage = Stage.DONE;
        REPORT.addProperty("status", "failed");
        REPORT.addProperty("failure", code);
        REPORT.addProperty("failureDetail", detail);
        REPORT.addProperty("clientTicks", clientTicks);
        REPORT.addProperty("wallSeconds", (System.nanoTime() - startedAtNanos) / 1_000_000_000.0D);
        if (SCREENSHOTS.size() > 0) {
            REPORT.addProperty("visualVerdict", "pending_manual_review");
        }
        if (activePhase != null && activePhase.has("status") && activePhase.get("status").getAsString().equals("running")) {
            activePhase.addProperty("status", "fail");
        }
        addRuntimeSnapshot(client);
        writeManifest();
        client.stop();
    }

    private static void writeManifest() {
        if (!finished) {
            return;
        }
        try {
            if (outputDirectory == null) {
                return;
            }
            Files.createDirectories(outputDirectory);
            Files.writeString(outputDirectory.resolve("manifest.json"), JSON.toJson(REPORT), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            System.err.println("[Aura QA UI] Could not write manifest: " + exception);
        }
    }

    private static void addRuntimeSnapshot(Minecraft client) {
        JsonObject runtime = new JsonObject();
        runtime.addProperty("singleplayer", client.getSingleplayerServer() != null);
        runtime.addProperty("worldLoaded", client.level != null && client.player != null);
        runtime.addProperty("guiScaleOption", client.options.guiScale().get());
        runtime.addProperty("windowWidthPixels", client.getWindow().getWidth());
        runtime.addProperty("windowHeightPixels", client.getWindow().getHeight());
        if (client.level != null) {
            runtime.addProperty("dimension", client.level.dimension().location().toString());
            runtime.addProperty("gameTime", client.level.getGameTime());
        }
        if (client.player != null) {
            runtime.addProperty("playerX", client.player.getX());
            runtime.addProperty("playerY", client.player.getY());
            runtime.addProperty("playerZ", client.player.getZ());
        }
        REPORT.add("runtime", runtime);
    }

    private static String auditAuraJar() {
        try {
            if (!ModList.get().isLoaded("aura")) {
                REPORT.addProperty("auraLoaded", false);
                return "aura_mod_not_loaded";
            }
            var modFileInfo = ModList.get().getModFileById("aura");
            if (modFileInfo == null) {
                REPORT.addProperty("auraJarProof", "missing_mod_file");
                return "aura_mod_file_missing";
            }
            Path jar = modFileInfo.getFile().getFilePath().toAbsolutePath().normalize();
            boolean packagedJar = Files.isRegularFile(jar) && jar.getFileName() != null
                && jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
            REPORT.addProperty("auraLoaded", true);
            REPORT.addProperty("auraJarPath", jar.toString());
            REPORT.addProperty("auraJarIsRegularFile", packagedJar);
            if (!packagedJar) {
                return "aura_not_loaded_from_packaged_jar";
            }
            REPORT.addProperty("auraJarBytes", Files.size(jar));
            REPORT.addProperty("auraJarSha256", sha256(jar));
            REPORT.addProperty("auraJarProof", "verified_by_neoforge_modlist");
            return null;
        } catch (Exception exception) {
            REPORT.addProperty("auraJarProof", "failed");
            REPORT.addProperty("auraJarProofError", exception.toString());
            return "aura_jar_proof_failed";
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8_192];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Fixture prepareFixture(ServerPlayer player, String action) {
        ServerLevel level = player.serverLevel();
        List<String> preconditions = new ArrayList<>();
        player.setGameMode(GameType.CREATIVE);
        preconditions.add("Set the isolated player to Creative mode for bounded QA actions.");
        if (action.equals("creative")) {
            return new Fixture(null, null, null, null, null, null, preconditions);
        }
        if (action.equals("guide")) {
            player.getInventory().setItem(0, new ItemStack(AuraItems.ENCYCLOPEDIA_AURA));
            player.getInventory().selected = 0;
            preconditions.add("Granted aura:encyclopedia_aura in hotbar slot 1; the client opens it through normal item use.");
            return new Fixture(null, null, null, null, null, null, preconditions);
        }

        BlockPos spawn = level.getSharedSpawnPos();
        if (action.equals("animation")) {
            int x = spawn.getX() + 10;
            int z = spawn.getZ() + 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos consumer = new BlockPos(x, y, z);
            BlockPos receiver = consumer.west();
            BlockPos source = receiver.above(3);
            requireReplaceable(level, consumer);
            requireReplaceable(level, receiver);
            requireReplaceable(level, source);
            placeFixtureBlock(level, consumer, AuraContent.CONSUMER_BLOCK_FURNACE.defaultBlockState());
            placeFixtureBlock(level, receiver, AuraContent.AURA_NODE.defaultBlockState());
            placeFixtureBlock(level, source, AuraContent.AURA_NODE.defaultBlockState());
            ItemEntity crystals = new ItemEntity(level, source.getX() + 0.5D, source.getY() + 1.1D,
                source.getZ() + 0.5D, new ItemStack(AuraItems.crystal(AuraColor.WHITE), 4));
            ItemEntity rawIron = new ItemEntity(level, consumer.getX() + 1.5D, consumer.getY() + 0.2D,
                consumer.getZ() + 0.5D, new ItemStack(Items.RAW_IRON));
            level.addFreshEntity(crystals);
            level.addFreshEntity(rawIron);
            preconditions.add("Placed an empty Smelter and two empty Aura Nodes as a natural power-transfer fixture.");
            preconditions.add("Spawned four raw White Aura Crystals and one dropped Raw Iron input; ordinary ticks must absorb, transfer, and convert.");
            preconditions.add("No Aura storage, stored power, consumer progress, iron output, or reaction animation was seeded.");
            Camera camera = cameraFor(consumer,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + 3, z + 1), x + 3.0D, z + 1.5D);
            player.teleportTo(level, camera.x(), camera.y(), camera.z(), camera.yaw(), camera.pitch());
            return new Fixture(null, null, consumer, receiver, source, camera, preconditions);
        }

        int x = spawn.getX() + 2;
        int z = spawn.getZ() + 2;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos node = new BlockPos(x, y, z);
        BlockPos target = node.east();
        requireReplaceable(level, node);
        placeFixtureBlock(level, node, AuraContent.AURA_NODE.defaultBlockState());
        preconditions.add("Placed one empty aura:aura_node as the isolated HUD/placement precondition.");
        if (action.equals("placement")) {
            if (!level.getBlockState(target.below()).canOcclude()) {
                throw new IllegalStateException("The placement preview target has no solid support");
            }
            requireReplaceable(level, target);
            player.getInventory().setItem(0, new ItemStack(AuraContent.AURA_NODE));
            player.getInventory().selected = 0;
            preconditions.add("Granted one Aura Node block item in hotbar slot 1; the client places it with normal use-item-on.");
            Camera camera = cameraFor(target,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + 2, z + 3), x + 2.5D, z + 4.0D, 0.0D);
            player.teleportTo(level, camera.x(), camera.y(), camera.z(), camera.yaw(), camera.pitch());
            return new Fixture(node, target, null, null, null, camera, preconditions);
        }

        BlockPos feeder = node.above(3);
        requireReplaceable(level, feeder);
        placeFixtureBlock(level, feeder, AuraContent.AURA_NODE.defaultBlockState());
        level.addFreshEntity(new ItemEntity(level, feeder.getX() + 0.5D, feeder.getY() + 1.1D,
            feeder.getZ() + 0.5D, new ItemStack(AuraItems.crystal(AuraColor.WHITE), 4)));
        preconditions.add("An empty node above the HUD target receives four raw White crystals through normal absorption/transfer ticks.");
        Camera camera = cameraFor(node,
            level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + 2, z + 3), x + 2.5D, z + 4.0D);
        player.teleportTo(level, camera.x(), camera.y(), camera.z(), camera.yaw(), camera.pitch());
        return new Fixture(node, null, null, null, null, camera, preconditions);
    }

    private static Camera cameraFor(BlockPos target, int groundY, double x, double z) {
        return cameraFor(target, groundY, x, z, 0.5D);
    }

    private static Camera cameraFor(BlockPos target, int groundY, double x, double z, double targetYOffset) {
        double eyeY = groundY + 1.62D;
        double dx = target.getX() + 0.5D - x;
        double dy = target.getY() + targetYOffset - eyeY;
        double dz = target.getZ() + 0.5D - z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new Camera(x, groundY, z, yaw, pitch);
    }

    private static void requireReplaceable(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            throw new IllegalStateException("Fixture position is obstructed: " + pos);
        }
    }

    private static void placeFixtureBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.setBlock(pos, state, 3)) {
            throw new IllegalStateException("Could not place fixture block at " + pos);
        }
    }

    private static void addPosition(JsonObject target, String name, BlockPos pos) {
        if (pos == null) {
            return;
        }
        JsonObject value = new JsonObject();
        value.addProperty("x", pos.getX());
        value.addProperty("y", pos.getY());
        value.addProperty("z", pos.getZ());
        target.add(name, value);
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray result = new JsonArray();
        values.forEach(result::add);
        return result;
    }

    private static Path outputDirectory(Minecraft client) {
        String configured = System.getProperty("aura.qa.ui.dir");
        Path directory = configured == null || configured.isBlank()
            ? client.gameDirectory.toPath().resolve("qa-ui") : Path.of(configured);
        return directory.toAbsolutePath().normalize();
    }

    private static boolean timedOut() {
        return clientTicks >= TIMEOUT_TICKS || System.nanoTime() - startedAtNanos >= TIMEOUT_NANOS;
    }

    private enum Stage {
        WAIT_WORLD,
        PREPARING,
        WAIT_FIXTURE_SYNC,
        OPEN_CREATIVE,
        WAIT_CREATIVE,
        OPEN_GUIDE,
        WAIT_GUIDE_LANDING,
        RUN_GUIDE_LAYOUT,
        CLICK_GUIDE_LINK,
        WAIT_GUIDE_ENTRY,
        CLICK_GUIDE_BACK,
        WAIT_GUIDE_BACK,
        CAPTURE_HUD,
        PLACE_NODE,
        WAIT_PLACEMENT,
        CAPTURE_PLACEMENT,
        START_ANIMATION,
        WAIT_ANIMATION,
        DONE
    }

    private record Camera(double x, double y, double z, float yaw, float pitch) {
    }

    private record Fixture(
        BlockPos hudNode,
        BlockPos placementTarget,
        BlockPos consumer,
        BlockPos energyReceiver,
        BlockPos energySource,
        Camera camera,
        List<String> preconditions
    ) {
    }
}
