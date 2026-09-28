package pixlepix.auracascade.qa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

/** Bounded integrated-world visual evidence for production Aura feedback triggers. */
public final class TargetFeedbackProbe {
    private static final String PREFIX = "aura.qa.feedbackProbe";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_TOTAL_TICKS = 2_400;
    private static final int MAX_PHASE_TICKS = 240;
    private static final int MAX_WORLD_WAIT_TICKS = 1_900;
    private static final int EVENT_SAMPLE_COUNT = 12;
    private static final int BEFORE_SAMPLE_COUNT = 2;
    private static final int AFTER_SAMPLE_COUNT = EVENT_SAMPLE_COUNT - BEFORE_SAMPLE_COUNT;
    private static final int FLOOR_Y = 180;
    private static final int AREA_MIN_X = 358;
    private static final int AREA_MAX_X = 373;
    private static final int AREA_MIN_Z = 236;
    private static final int AREA_MAX_Z = 248;
    private static final int AREA_MAX_Y = 185;
    private static final BlockPos PREVIEW_ANCHOR = new BlockPos(361, FLOOR_Y + 1, 240);
    private static final BlockPos PREVIEW_NODE = new BlockPos(365, FLOOR_Y + 1, 240);
    private static final BlockPos HUD_NODE = new BlockPos(370, FLOOR_Y + 1, 246);
    private static final BlockPos HUD_POWER = HUD_NODE.east();
    private static final BlockPos ITEM_CENTER = new BlockPos(361, FLOOR_Y + 1, 245);
    private static final Vec3 PREVIEW_CAMERA = new Vec3(363.5D, FLOOR_Y + 1.0D, 237.0D);
    private static final Vec3 ITEM_CAMERA = new Vec3(361.5D, FLOOR_Y + 1.0D, 241.5D);
    private static final Vec3 HUD_CAMERA = new Vec3(370.5D, FLOOR_Y + 1.0D, 242.5D);
    private static final Identifier FRAME_MARKER = Identifier.fromNamespaceAndPath("aura_qa", "feedback_probe_frame");
    private static boolean started;
    private static volatile boolean completed;

    private final Minecraft client;
    private final Path outputDir;
    private final JsonObject report = new JsonObject();
    private final JsonArray samples = new JsonArray();
    private final JsonArray assertions = new JsonArray();
    private final JsonArray failures = new JsonArray();
    private final List<BlockPos> placedBlocks = new ArrayList<>();
    private final List<int[]> forcedChunks = new ArrayList<>();

    private volatile boolean serverCommandDone;
    private volatile String serverCommandFailure;
    private volatile ServerLevel fixtureLevel;
    private volatile boolean fixtureTouched;
    private volatile long fixtureCreatedGameTime;
    private Vec3 expectedCamera;
    private Phase phase = Phase.WAIT_FOR_WORLD;
    private Phase commandNextPhase;
    private Phase sampleNextPhase;
    private String sampleEvent;
    private String sampleSegment;
    private int sampleGoal;
    private int sampleCount;
    private int ticks;
    private int phaseTicks;
    private int renderedFrames;
    private int lastCapturedFrame;
    private boolean capturePending;
    private boolean cleaning;
    private boolean timedOut;
    private boolean originalHideGui;
    private boolean originalHideGuiKnown;
    private boolean waitedForClientProbe;
    private boolean fixtureItemsSeeded;
    private boolean angelOutputObserved;
    private boolean hudPositiveObserved;
    private boolean hudTargetConsistent = true;
    private int hudPositiveSamples;
    private int hudZeroSamples;
    private Vec3 originalPlayerPosition;
    private float originalPlayerYaw;
    private float originalPlayerPitch;
    private String originalPlayerUuid;

    private TargetFeedbackProbe(Minecraft client, Path outputDir) {
        this.client = client;
        this.outputDir = outputDir;
    }

    public static synchronized void startIfEnabled() {
        if (!Boolean.getBoolean(PREFIX) || started) {
            return;
        }
        started = true;
        try {
            String configuredOutput = System.getProperty(PREFIX + ".output", "").trim();
            if (configuredOutput.isEmpty()) {
                throw new IOException("property " + PREFIX + ".output must name a fresh directory");
            }
            Path output = Path.of(configuredOutput).toAbsolutePath().normalize();
            TargetFeedbackProbe probe = new TargetFeedbackProbe(Minecraft.getInstance(), output);
            probe.start();
        } catch (Exception | LinkageError exception) {
            completed = true;
            System.err.println("[Aura QA Feedback Probe] Could not start: " + exception);
        }
    }

    public static boolean isComplete() {
        return completed || !Boolean.getBoolean(PREFIX);
    }

    private void start() throws IOException {
        Files.createDirectories(outputDir);
        try (var existing = Files.list(outputDir)) {
            if (existing.findAny().isPresent()) {
                throw new IOException("output directory is not fresh: " + outputDir);
            }
        }

        report.addProperty("probe", "target-feedback-v1");
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("outputDirectory", outputDir.toString());
        report.addProperty("runtimeNamespace", FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace());
        report.addProperty("maxTotalClientTicks", MAX_TOTAL_TICKS);
        report.addProperty("maxPhaseClientTicks", MAX_PHASE_TICKS);
        report.addProperty("maxWorldAndPriorUiWaitClientTicks", MAX_WORLD_WAIT_TICKS);
        report.addProperty("samplesPerEvent", EVENT_SAMPLE_COUNT);
        report.addProperty("samplesBeforeEachEvent", BEFORE_SAMPLE_COUNT);
        report.addProperty("samplesAfterEachEvent", AFTER_SAMPLE_COUNT);
        report.addProperty("screenshotTiming", "HUD render marker, then Screenshot.takeScreenshot on the following END_CLIENT_TICK");
        report.addProperty("visualHelpersInvoked", false);
        report.addProperty("visualReviewRequired", true);
        report.add("samples", samples);
        report.add("assertions", assertions);
        report.add("failures", failures);
        recordFixturePlan();
        writeManifest();

        HudElementRegistry.addLast(FRAME_MARKER, (graphics, tickCounter) -> markRenderedFrame());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void recordFixturePlan() {
        JsonObject fixture = new JsonObject();
        fixture.addProperty("dimensionRequired", Level.OVERWORLD.identifier().toString());
        fixture.addProperty("floorY", FLOOR_Y);
        fixture.addProperty("clearsOnlyNewlyPlacedBlocks", true);
        fixture.addProperty("requiresInitiallyAirFixtureVolume", true);
        fixture.addProperty("previewRawAuraCrystals", 0);
        fixture.addProperty("angelsteelInputs", "two degree-1 ingots, then a third as grounded ItemEntity instances");
        fixture.addProperty("hudTransition", "orange manipulator powered by an adjacent seeded redstone block, then unpowered");
        fixture.add("previewAnchor", posJson(PREVIEW_ANCHOR));
        fixture.add("previewNode", posJson(PREVIEW_NODE));
        fixture.add("angelsteelDropCenter", posJson(ITEM_CENTER));
        fixture.add("hudNode", posJson(HUD_NODE));
        fixture.add("hudRedstoneBlock", posJson(HUD_POWER));
        report.add("fixturePlan", fixture);
    }

    private void markRenderedFrame() {
        if (!finished() && client.level != null && client.player != null && client.screen == null) {
            renderedFrames++;
        }
    }

    private void tick(Minecraft minecraft) {
        if (finished()) {
            return;
        }
        ticks++;
        phaseTicks++;
        if (ticks >= MAX_TOTAL_TICKS) {
            timedOut = true;
            fail("total_timeout: exceeded " + MAX_TOTAL_TICKS + " client ticks");
            if (phase == Phase.WAIT_CLEANUP && serverCommandDone) finishCleanupCommand(minecraft);
            else if (phase != Phase.WAIT_CLEANUP) finish(minecraft);
            return;
        }
        int phaseLimit = phase == Phase.WAIT_FOR_WORLD ? MAX_WORLD_WAIT_TICKS : MAX_PHASE_TICKS;
        if (cleaning) phaseLimit = MAX_TOTAL_TICKS;
        if (phaseTicks >= phaseLimit) {
            timedOut = true;
            fail("phase_timeout:" + phase + " after " + phaseLimit + " client ticks");
            finish(minecraft);
            return;
        }

        try {
            if (phase == Phase.WAIT_SERVER_COMMAND) {
                pollServerCommand(minecraft);
                return;
            }
            if (phase == Phase.WAIT_CLEANUP) {
                if (serverCommandDone) finishCleanupCommand(minecraft);
                return;
            }
            if (phase == Phase.SAMPLE) {
                sampleTick(minecraft);
                return;
            }
            if (phase == Phase.WAIT_HUD_POWERED) {
                aimClient(minecraft, center(HUD_NODE));
                int amount = hudOrange(minecraft);
                if (isHudTarget(minecraft) && amount > 0) {
                    hudPositiveObserved = true;
                    report.addProperty("hudPoweredOrange", amount);
                    beginSamples("auraHudRowTransition", "powered-before-removal", BEFORE_SAMPLE_COUNT, Phase.REMOVE_HUD_POWER);
                }
                return;
            }
            if (phase == Phase.WAIT_HUD_ZERO) {
                aimClient(minecraft, center(HUD_NODE));
                if (isHudTarget(minecraft) && hudOrange(minecraft) == 0) {
                    assertion("hudSameTargetReachedZero", hudPositiveObserved,
                        "same orange manipulator retained its previously seen color row at zero after power removal");
                    beginSamples("auraHudRowTransition", "zero-after-removal", AFTER_SAMPLE_COUNT, Phase.CLEANUP);
                }
                return;
            }

            switch (phase) {
                case WAIT_FOR_WORLD -> waitForWorld(minecraft);
                case PLACE_PREVIEW_NODE -> placePreviewNode(minecraft);
                case WAIT_ANCHOR_PREVIEW_EXPIRED -> waitForAnchorPreview(minecraft);
                case VERIFY_PREVIEW -> verifyPreview(minecraft);
                case SEED_TWO_INGOTS -> seedTwoIngots(minecraft);
                case WAIT_TWO_INGOTS -> waitForTwoIngots(minecraft);
                case DROP_THIRD_INGOT -> dropThirdIngot(minecraft);
                case VERIFY_ANGEL_RESULT -> verifyAngelResult(minecraft);
                case REMOVE_HUD_POWER -> removeHudPower(minecraft);
                case CLEANUP -> beginCleanup(minecraft);
                case WAIT_SERVER_COMMAND, SAMPLE, WAIT_HUD_POWERED, WAIT_HUD_ZERO, WAIT_CLEANUP, DONE -> { }
            }
        } catch (Exception | AssertionError exception) {
            fail("probe_exception:" + exception);
            finish(minecraft);
        }
    }

    private void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (minecraft.screen != null) {
            return;
        }
        if (Boolean.getBoolean("aura.qa.clientProbe") && !TargetClientProbe.isComplete()) {
            waitedForClientProbe = true;
            return;
        }
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            assertion("integratedWorld", false, "the loaded client world has no integrated server");
            finish(minecraft);
            return;
        }
        if (!Level.OVERWORLD.equals(minecraft.level.dimension())) {
            assertion("overworld", false, "feedback fixture requires the Overworld, got " + minecraft.level.dimension().identifier());
            finish(minecraft);
            return;
        }
        originalHideGui = minecraft.options.hideGui;
        originalHideGuiKnown = true;
        minecraft.options.hideGui = false;
        report.addProperty("worldName", server.getWorldData().getLevelName());
        report.addProperty("dimension", minecraft.level.dimension().identifier().toString());
        report.addProperty("playerUuid", minecraft.player.getUUID().toString());
        report.addProperty("waitedForTargetClientProbe", waitedForClientProbe);
        assertion("integratedWorld", true, "loaded singleplayer Overworld and integrated server");
        java.util.UUID playerUuid = minecraft.player.getUUID();
        expectedCamera = PREVIEW_CAMERA;
        submitServerCommand(minecraft, Phase.WAIT_ANCHOR_PREVIEW_EXPIRED, () -> createFixture(server, playerUuid));
    }

    private void createFixture(net.minecraft.server.MinecraftServer server, java.util.UUID playerUuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        require(player != null, "fixturePlayer: integrated server player is absent");
        ServerLevel level = player.level();
        require(Level.OVERWORLD.equals(level.dimension()), "fixtureDimension: expected Overworld");
        fixtureLevel = level;
        originalPlayerPosition = player.position();
        originalPlayerYaw = player.getYRot();
        originalPlayerPitch = player.getXRot();
        originalPlayerUuid = player.getUUID().toString();
        fixtureTouched = true;

        for (int chunkX : new int[] {Math.floorDiv(AREA_MIN_X, 16), Math.floorDiv(AREA_MAX_X, 16)}) {
            for (int chunkZ : new int[] {Math.floorDiv(AREA_MIN_Z, 16), Math.floorDiv(AREA_MAX_Z, 16)}) {
                level.setChunkForced(chunkX, chunkZ, true);
                forcedChunks.add(new int[] {chunkX, chunkZ});
            }
        }
        requireClearFixtureVolume(level);
        for (int x = AREA_MIN_X; x <= AREA_MAX_X; x++) {
            for (int z = AREA_MIN_Z; z <= AREA_MAX_Z; z++) {
                BlockPos floor = new BlockPos(x, FLOOR_Y, z);
                require(level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState()), "floor: could not place " + floor);
                placedBlocks.add(floor);
            }
        }

        placeAuraNode(level, PREVIEW_ANCHOR, AuraContent.AURA_NODE.defaultBlockState());
        placeAuraNode(level, HUD_NODE, AuraContent.AURA_NODE_ORANGE.defaultBlockState());
        require(level.setBlockAndUpdate(HUD_POWER, Blocks.REDSTONE_BLOCK.defaultBlockState()), "hudPower: could not place redstone block");
        placedBlocks.add(HUD_POWER);
        fixtureCreatedGameTime = level.getGameTime();
        aimServerPlayer(player, PREVIEW_CAMERA, center(PREVIEW_NODE).add(-1.0D, 0.0D, 0.0D));
    }

    private void requireClearFixtureVolume(ServerLevel level) {
        for (int x = AREA_MIN_X; x <= AREA_MAX_X; x++) {
            for (int z = AREA_MIN_Z; z <= AREA_MAX_Z; z++) {
                for (int y = FLOOR_Y; y <= AREA_MAX_Y; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    require(level.isEmptyBlock(pos), "fixtureVolumeOccupied: " + pos + " contains " + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
                }
            }
        }
        AABB bounds = fixtureBounds();
        require(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(), "fixtureVolumeOccupied: item entities already exist in " + bounds);
    }

    private void placeAuraNode(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        require(level.setBlockAndUpdate(pos, state), "nodePlacement: could not place " + pos);
        placedBlocks.add(pos);
        require(level.getBlockEntity(pos) instanceof AuraNetworkBlockEntity, "nodeBlockEntity: missing at " + pos);
    }

    private void waitForAnchorPreview(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.level.getGameTime() - fixtureCreatedGameTime < 16L) {
            return;
        }
        if (networkAt(minecraft, PREVIEW_ANCHOR) == null
            || !minecraft.levelRenderer.isSectionCompiledAndVisible(PREVIEW_ANCHOR)) {
            return;
        }
        assertion("previewAnchorEmpty", auraAt(minecraft, PREVIEW_ANCHOR) == 0,
            "existing node remained empty during the new-node placement preview");
        beginSamples("nodePlacementPreview", "before-placement", BEFORE_SAMPLE_COUNT, Phase.PLACE_PREVIEW_NODE);
    }

    private void placePreviewNode(Minecraft minecraft) {
        sampleEvent = "nodePlacementPreview";
        sampleSegment = "after-placement";
        sampleGoal = AFTER_SAMPLE_COUNT;
        sampleCount = 0;
        sampleNextPhase = Phase.VERIFY_PREVIEW;
        submitServerCommand(minecraft, Phase.SAMPLE, () -> {
            ServerLevel level = fixtureLevel;
            placeAuraNode(level, PREVIEW_NODE, AuraContent.AURA_NODE.defaultBlockState());
            report.addProperty("previewPlacedAtGameTime", level.getGameTime());
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(originalPlayerUuid));
            require(player != null, "previewCamera: integrated server player is absent");
            aimServerPlayer(player, PREVIEW_CAMERA, center(PREVIEW_NODE).add(-1.0D, 0.0D, 0.0D));
        });
    }

    private void verifyPreview(Minecraft minecraft) {
        AuraNetworkBlockEntity node = networkAt(minecraft, PREVIEW_NODE);
        AuraNetworkBlockEntity anchor = networkAt(minecraft, PREVIEW_ANCHOR);
        boolean valid = node != null && anchor != null && node.inspectionState().totalAura() == 0
            && node.inspectionState().linkedNodeCount() >= 1;
        assertion("emptyNodePlacementPreview", valid,
            "new node is empty and reports at least one scanned peer; actual particles remain subject to PNG review");
        if (node != null) {
            report.addProperty("previewNodeAuraAfterSamples", node.inspectionState().totalAura());
            report.addProperty("previewNodeLinkedCountAfterSamples", node.inspectionState().linkedNodeCount());
        }
        expectedCamera = ITEM_CAMERA;
        movePlayer(minecraft, ITEM_CAMERA,
            new Vec3(ITEM_CENTER.getX() + 0.5D, FLOOR_Y + 1.2D, ITEM_CENTER.getZ() + 0.5D), Phase.SEED_TWO_INGOTS);
    }

    private void seedTwoIngots(Minecraft minecraft) {
        submitServerCommand(minecraft, Phase.WAIT_TWO_INGOTS, () -> {
            ServerLevel level = fixtureLevel;
            addDroppedIngot(level, 361.20D, 181.25D, 245.15D);
            addDroppedIngot(level, 361.80D, 181.25D, 245.15D);
            fixtureItemsSeeded = true;
            report.addProperty("angelsteelTwoInputSeededAtGameTime", level.getGameTime());
        });
    }

    private void waitForTwoIngots(Minecraft minecraft) {
        ItemCounts counts = itemCounts(minecraft);
        if (counts.groundedBase() == 2 && counts.higher() == 0) {
            report.addProperty("angelsteelTwoInputControl", "2 grounded base ingots, no upgraded output");
            beginSamples("angelsteelGroundCraft", "two-input-control", BEFORE_SAMPLE_COUNT, Phase.DROP_THIRD_INGOT);
        }
    }

    private void dropThirdIngot(Minecraft minecraft) {
        ItemCounts counts = itemCounts(minecraft);
        assertion("angelsteelTwoInputNegativeControl", counts.base() == 2 && counts.higher() == 0,
            "two grounded base ingots did not transform before the third dropped input");
        submitServerCommand(minecraft, Phase.SAMPLE, () -> {
            addDroppedIngot(fixtureLevel, 361.50D, 181.25D, 245.55D);
            report.addProperty("angelsteelThirdInputSeededAtGameTime", fixtureLevel.getGameTime());
        });
        commandNextPhase = Phase.SAMPLE;
        sampleEvent = "angelsteelGroundCraft";
        sampleSegment = "third-input-and-transformation";
        sampleGoal = AFTER_SAMPLE_COUNT;
        sampleCount = 0;
        sampleNextPhase = Phase.VERIFY_ANGEL_RESULT;
    }

    private void addDroppedIngot(ServerLevel level, double x, double y, double z) {
        ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(AuraItems.angelsteelIngot(0)));
        require(level.addFreshEntity(entity), "angelsteelDrop: could not add grounded-item input");
    }

    private void verifyAngelResult(Minecraft minecraft) {
        observeAngelsteel(minecraft);
        if (angelOutputObserved) {
            assertion("angelsteelTransformation", true, "three grounded same-degree ingots became one next-degree ingot");
            movePlayer(minecraft, HUD_CAMERA, center(HUD_NODE), Phase.WAIT_HUD_POWERED);
        } else if (phaseTicks >= MAX_PHASE_TICKS - 1) {
            assertion("angelsteelTransformation", false, "no next-degree dropped ingot appeared before the phase timeout");
            finish(minecraft);
        }
    }

    private void observeAngelsteel(Minecraft minecraft) {
        ItemCounts counts = itemCounts(minecraft);
        if (counts.higher() > 0) {
            angelOutputObserved = true;
            report.addProperty("angelsteelBaseInputCountAfterCraft", counts.base());
            report.addProperty("angelsteelNextDegreeOutputCount", counts.higher());
            report.addProperty("angelsteelGroundedOutputCount", counts.groundedHigher());
        }
    }

    private void removeHudPower(Minecraft minecraft) {
        submitServerCommand(minecraft, Phase.WAIT_HUD_ZERO, () -> {
            require(fixtureLevel.setBlockAndUpdate(HUD_POWER, Blocks.AIR.defaultBlockState()), "hudPowerRemoval: failed");
            placedBlocks.remove(HUD_POWER);
            report.addProperty("hudPowerRemovedAtGameTime", fixtureLevel.getGameTime());
        });
    }

    private void beginSamples(String event, String segment, int amount, Phase next) {
        sampleEvent = event;
        sampleSegment = segment;
        sampleGoal = amount;
        sampleCount = 0;
        sampleNextPhase = next;
        phase = Phase.SAMPLE;
        phaseTicks = 0;
    }

    private void sampleTick(Minecraft minecraft) {
        if (capturePending) {
            return;
        }
        if (sampleCount >= sampleGoal) {
            phase = sampleNextPhase;
            phaseTicks = 0;
            return;
        }
        if (renderedFrames <= lastCapturedFrame || minecraft.screen != null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if ("nodePlacementPreview".equals(sampleEvent)) {
            aimClient(minecraft, center(PREVIEW_NODE).add(-1.0D, 0.0D, 0.0D));
        } else if ("angelsteelGroundCraft".equals(sampleEvent)) {
            aimClient(minecraft, new Vec3(ITEM_CENTER.getX() + 0.5D, FLOOR_Y + 1.2D, ITEM_CENTER.getZ() + 0.5D));
        } else {
            aimClient(minecraft, center(HUD_NODE));
        }
        takeSample(minecraft, renderedFrames);
    }

    private void takeSample(Minecraft minecraft, int frameSerial) {
        capturePending = true;
        int index = samplesForEvent(sampleEvent);
        String filename = sampleEvent + "-" + String.format("%02d", index) + ".png";
        JsonObject sample = new JsonObject();
        sample.addProperty("event", sampleEvent);
        sample.addProperty("segment", sampleSegment);
        sample.addProperty("eventSample", index);
        sample.addProperty("frameSerial", frameSerial);
        sample.addProperty("clientTick", ticks);
        sample.addProperty("gameTime", minecraft.level.getGameTime());
        sample.addProperty("dimension", minecraft.level.dimension().identifier().toString());
        sample.addProperty("file", filename);
        sample.addProperty("guiScale", minecraft.options.guiScale().get());
        sample.add("camera", vectorJson(minecraft.player.position()));
        addVisualState(sample, minecraft);
        Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image ->
            minecraft.execute(() -> finishSample(minecraft, sample, filename, frameSerial, image)));
    }

    private void addVisualState(JsonObject sample, Minecraft minecraft) {
        if ("nodePlacementPreview".equals(sampleEvent)) {
            AuraNetworkBlockEntity node = networkAt(minecraft, PREVIEW_NODE);
            sample.addProperty("previewNodePresent", node != null);
            sample.addProperty("previewNodeAura", node == null ? -1 : node.inspectionState().totalAura());
            sample.addProperty("previewNodeLinkedCount", node == null ? -1 : node.inspectionState().linkedNodeCount());
        } else if ("angelsteelGroundCraft".equals(sampleEvent)) {
            ItemCounts counts = itemCounts(minecraft);
            sample.addProperty("groundedBaseCount", counts.groundedBase());
            sample.addProperty("baseCount", counts.base());
            sample.addProperty("nextDegreeCount", counts.higher());
            if (counts.higher() > 0) {
                angelOutputObserved = true;
            }
        } else {
            sample.addProperty("hudTargeted", isHudTarget(minecraft));
            sample.addProperty("hudOrange", hudOrange(minecraft));
        }
    }

    private void finishSample(Minecraft minecraft, JsonObject sample, String filename, int frameSerial, NativeImage image) {
        try {
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new IOException("Minecraft returned an empty framebuffer image");
            }
            Path destination = outputDir.resolve(filename).normalize();
            if (!destination.getParent().equals(outputDir)) {
                throw new IOException("invalid screenshot path: " + filename);
            }
            try (NativeImage captured = image) {
                captured.writeToFile(destination.toFile());
                sample.addProperty("width", captured.getWidth());
                sample.addProperty("height", captured.getHeight());
            }
            sample.addProperty("bytes", Files.size(destination));
            samples.add(sample);
            sampleCount++;
            lastCapturedFrame = frameSerial;
            if ("auraHudRowTransition".equals(sampleEvent)) {
                boolean targeted = sample.get("hudTargeted").getAsBoolean();
                int amount = sample.get("hudOrange").getAsInt();
                hudTargetConsistent &= targeted;
                if ("powered-before-removal".equals(sampleSegment)) {
                    if (amount > 0 && targeted) hudPositiveSamples++;
                    else fail("hud_powered_sample_not_observed_on_target:" + sampleCount);
                } else if (targeted && amount == 0) {
                    hudZeroSamples++;
                } else {
                    fail("hud_zero_sample_not_observed_on_same_target:" + sampleCount);
                }
            }
            writeManifest();
        } catch (Exception exception) {
            fail("framebuffer_capture_failed:" + filename + ":" + exception);
            finish(minecraft);
            return;
        } finally {
            capturePending = false;
        }
        if ("angelsteelGroundCraft".equals(sampleEvent) && angelOutputObserved) {
            report.addProperty("angelsteelOutputObservedDuringSamples", true);
        }
    }

    private int samplesForEvent(String event) {
        int count = 0;
        for (var element : samples) {
            if (element.isJsonObject() && event.equals(element.getAsJsonObject().get("event").getAsString())) {
                count++;
            }
        }
        return count;
    }

    private void submitServerCommand(Minecraft minecraft, Phase next, Runnable action) {
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            fail("integrated_server_disappeared");
            finish(minecraft);
            return;
        }
        serverCommandDone = false;
        serverCommandFailure = null;
        commandNextPhase = next;
        phase = Phase.WAIT_SERVER_COMMAND;
        phaseTicks = 0;
        server.execute(() -> {
            try {
                action.run();
            } catch (Exception | AssertionError exception) {
                serverCommandFailure = exception.toString();
            } finally {
                serverCommandDone = true;
            }
        });
    }

    private void pollServerCommand(Minecraft minecraft) {
        if (!serverCommandDone) {
            return;
        }
        if (serverCommandFailure != null) {
            fail("server_fixture_failed:" + serverCommandFailure);
            finish(minecraft);
            return;
        }
        phase = commandNextPhase;
        phaseTicks = 0;
    }

    private void movePlayer(Minecraft minecraft, Vec3 camera, Vec3 target, Phase next) {
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            fail("integrated_server_disappeared_during_camera_move");
            finish(minecraft);
            return;
        }
        java.util.UUID uuid = minecraft.player.getUUID();
        submitServerCommand(minecraft, next, () -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            require(player != null, "cameraPlayer: integrated server player is absent");
            aimServerPlayer(player, camera, target);
        });
    }

    private void aimClient(Minecraft minecraft, Vec3 target) {
        if (minecraft.player == null) return;
        Vec3 eye = minecraft.player.getEyePosition();
        float[] rotation = rotation(eye, target);
        minecraft.player.setYRot(rotation[0]);
        minecraft.player.setXRot(rotation[1]);
        minecraft.player.setYHeadRot(rotation[0]);
    }

    private static void aimServerPlayer(ServerPlayer player, Vec3 camera, Vec3 target) {
        player.teleportTo(camera.x, camera.y, camera.z);
        float[] rotation = rotation(camera.add(0.0D, player.getEyeHeight(), 0.0D), target);
        player.setYRot(rotation[0]);
        player.setXRot(rotation[1]);
        player.setYHeadRot(rotation[0]);
    }

    private static float[] rotation(Vec3 eye, Vec3 target) {
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[] {yaw, pitch};
    }

    private void beginCleanup(Minecraft minecraft) {
        if (cleaning) return;
        cleaning = true;
        if (!fixtureTouched || fixtureLevel == null) {
            finishReport(minecraft);
            return;
        }
        submitServerCommand(minecraft, Phase.WAIT_CLEANUP, () -> cleanupFixture());
    }

    private void cleanupFixture() {
        ServerLevel level = fixtureLevel;
        if (level == null) return;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, fixtureBounds())) {
            entity.discard();
        }
        for (int index = placedBlocks.size() - 1; index >= 0; index--) {
            level.setBlockAndUpdate(placedBlocks.get(index), Blocks.AIR.defaultBlockState());
        }
        placedBlocks.clear();
        for (int[] chunk : forcedChunks) {
            level.setChunkForced(chunk[0], chunk[1], false);
        }
        forcedChunks.clear();
        if (originalPlayerUuid != null && originalPlayerPosition != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(originalPlayerUuid));
            if (player != null) {
                player.teleportTo(originalPlayerPosition.x, originalPlayerPosition.y, originalPlayerPosition.z);
                player.setYRot(originalPlayerYaw);
                player.setXRot(originalPlayerPitch);
                player.setYHeadRot(originalPlayerYaw);
            }
        }
    }

    private void finish(Minecraft minecraft) {
        if (finished() || cleaning) return;
        beginCleanup(minecraft);
    }

    private void finishReport(Minecraft minecraft) {
        if (originalHideGuiKnown) minecraft.options.hideGui = originalHideGui;
        if (!samples.isEmpty()) {
            assertion("twelveFramebufferSamplesPerEvent", sampleCountFor("nodePlacementPreview") == EVENT_SAMPLE_COUNT
                && sampleCountFor("angelsteelGroundCraft") == EVENT_SAMPLE_COUNT
                && sampleCountFor("auraHudRowTransition") == EVENT_SAMPLE_COUNT,
                "each primary event has two before and ten after framebuffer samples");
        }
        assertion("hudRowsCaptured", hudPositiveSamples == BEFORE_SAMPLE_COUNT && hudZeroSamples == AFTER_SAMPLE_COUNT
            && hudTargetConsistent, "same HUD target captured powered and retained zero-orange rows");
        report.addProperty("clientTicks", ticks);
        report.addProperty("finalPhase", phase.toString());
        report.addProperty("timedOut", timedOut);
        report.addProperty("completedAt", Instant.now().toString());
        report.addProperty("visualReviewRequired", true);
        report.addProperty("visualReviewScope", "Confirm the native placement particles, Angelsteel burst particles, and retained Aura HUD row in the saved PNGs.");
        report.addProperty("success", failures.isEmpty());
        report.addProperty("status", failures.isEmpty() ? "CAPTURED_REVIEW_REQUIRED" : timedOut ? "TIMEOUT" : "FAIL");
        report.addProperty("complete", true);
        writeManifest();
        phase = Phase.DONE;
        completed = true;
    }

    private void finishCleanupCommand(Minecraft minecraft) {
        assertion("fixtureCleanup", serverCommandFailure == null, "temporary blocks, dropped items, forced chunks and camera were restored");
        if (serverCommandFailure != null) fail("fixture_cleanup_failed:" + serverCommandFailure);
        fixtureTouched = false;
        finishReport(minecraft);
    }

    private void fail(String reason) {
        failures.add(reason);
        writeManifest();
    }

    private void assertion(String id, boolean passed, String detail) {
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        result.addProperty("passed", passed);
        result.addProperty("detail", detail);
        assertions.add(result);
        if (!passed) failures.add(id + ": " + detail);
        writeManifest();
    }

    private void writeManifest() {
        if (!Files.isDirectory(outputDir)) return;
        try {
            Path temporary = outputDir.resolve("manifest.json.tmp");
            Path destination = outputDir.resolve("manifest.json");
            Files.writeString(temporary, JSON.toJson(report), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Aura QA Feedback Probe] Could not write manifest: " + exception);
        }
    }

    private boolean finished() {
        return completed || phase == Phase.DONE;
    }

    private int sampleCountFor(String event) {
        int count = 0;
        for (var element : samples) {
            if (event.equals(element.getAsJsonObject().get("event").getAsString())) count++;
        }
        return count;
    }

    private AuraNetworkBlockEntity networkAt(Minecraft minecraft, BlockPos pos) {
        if (minecraft.level == null) return null;
        return minecraft.level.getBlockEntity(pos) instanceof AuraNetworkBlockEntity network ? network : null;
    }

    private int auraAt(Minecraft minecraft, BlockPos pos) {
        AuraNetworkBlockEntity network = networkAt(minecraft, pos);
        return network == null ? -1 : network.inspectionState().totalAura();
    }

    private int hudOrange(Minecraft minecraft) {
        AuraNetworkBlockEntity network = networkAt(minecraft, HUD_NODE);
        return network == null ? -1 : network.inspectionState().storage().get(AuraColor.ORANGE);
    }

    private boolean isHudTarget(Minecraft minecraft) {
        if (minecraft.player == null) return false;
        HitResult hit = minecraft.player.pick(minecraft.player.blockInteractionRange(), 1.0F, false);
        return hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK
            && HUD_NODE.equals(blockHit.getBlockPos());
    }

    private ItemCounts itemCounts(Minecraft minecraft) {
        if (minecraft.level == null) return new ItemCounts(0, 0, 0, 0);
        int base = 0;
        int groundedBase = 0;
        int higher = 0;
        int groundedHigher = 0;
        for (ItemEntity entity : minecraft.level.getEntitiesOfClass(ItemEntity.class, itemBounds())) {
            OptionalInt degree = AuraItems.angelsteelDegree(entity.getItem());
            if (degree.isEmpty()) continue;
            if (degree.getAsInt() == 0) {
                base += entity.getItem().getCount();
                if (entity.onGround()) groundedBase += entity.getItem().getCount();
            } else if (degree.getAsInt() == 1) {
                higher += entity.getItem().getCount();
                if (entity.onGround()) groundedHigher += entity.getItem().getCount();
            }
        }
        return new ItemCounts(base, groundedBase, higher, groundedHigher);
    }

    private static AABB itemBounds() {
        return new AABB(ITEM_CENTER.getX() - 2.0D, FLOOR_Y, ITEM_CENTER.getZ() - 1.0D,
            ITEM_CENTER.getX() + 3.0D, FLOOR_Y + 4.0D, ITEM_CENTER.getZ() + 2.0D);
    }

    private static AABB fixtureBounds() {
        return new AABB(AREA_MIN_X, FLOOR_Y, AREA_MIN_Z, AREA_MAX_X + 1.0D, AREA_MAX_Y + 1.0D, AREA_MAX_Z + 1.0D);
    }

    private static Vec3 center(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    private static JsonObject posJson(BlockPos pos) {
        JsonObject object = new JsonObject();
        object.addProperty("x", pos.getX());
        object.addProperty("y", pos.getY());
        object.addProperty("z", pos.getZ());
        return object;
    }

    private static JsonObject vectorJson(Vec3 vector) {
        JsonObject object = new JsonObject();
        object.addProperty("x", vector.x);
        object.addProperty("y", vector.y);
        object.addProperty("z", vector.z);
        return object;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private enum Phase {
        WAIT_FOR_WORLD,
        WAIT_SERVER_COMMAND,
        WAIT_ANCHOR_PREVIEW_EXPIRED,
        PLACE_PREVIEW_NODE,
        VERIFY_PREVIEW,
        SEED_TWO_INGOTS,
        WAIT_TWO_INGOTS,
        DROP_THIRD_INGOT,
        VERIFY_ANGEL_RESULT,
        WAIT_ANGEL_RESULT,
        WAIT_HUD_POWERED,
        REMOVE_HUD_POWER,
        WAIT_HUD_ZERO,
        SAMPLE,
        CLEANUP,
        WAIT_CLEANUP,
        DONE
    }

    private record ItemCounts(int base, int groundedBase, int higher, int groundedHigher) { }
}
