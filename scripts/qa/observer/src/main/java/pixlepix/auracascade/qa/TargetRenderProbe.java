package pixlepix.auracascade.qa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.client.AuraItemModels;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.fairy.AuraFairyEntity;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.fairy.FairySystem;
import pixlepix.auracascade.item.AngelsteelSwordItem;
import pixlepix.auracascade.item.AngelsteelToolHelper;
import pixlepix.auracascade.item.AngelsteelToolKind;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.RingOfBindingItem;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;

/** Opt-in packaged-render evidence probe. The observer owns calling startIfEnabled() and stopping. */
public final class TargetRenderProbe {
    private static final String ENABLE_PROPERTY = "aura.qa.renderProbe";
    private static final String OUTPUT_PROPERTY = ENABLE_PROPERTY + ".output";
    private static final int MAX_CLIENT_TICKS = 1200;
    private static final int FRAME_COUNT = 6;
    private static final int FRAME_GAP_TICKS = 8;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final BlockPos PEDESTAL_POS = new BlockPos(64, 180, 64);
    private static final BlockPos PEDESTAL_BASE = PEDESTAL_POS.below();
    private static final BlockPos PLAYER_BASE = new BlockPos(67, 180, 72);
    private static final double CAMERA_X = 67.5D;
    private static final double CAMERA_Y = 181.0D;
    private static final double CAMERA_Z = 72.5D;
    private static final float CAMERA_YAW = 180.0F;
    private static final float CAMERA_PITCH = 10.0F;
    private static final List<AuraColor> SWORD_COLORS = List.of(
        AuraColor.RED, AuraColor.ORANGE, AuraColor.YELLOW,
        AuraColor.GREEN, AuraColor.BLUE, AuraColor.VIOLET
    );
    private static boolean started;
    private static volatile boolean completed;

    private final Minecraft client;
    private final Path outputDir;
    private final JsonObject manifest = new JsonObject();
    private final JsonObject assertions = new JsonObject();
    private final JsonArray failures = new JsonArray();
    private final JsonArray frames = new JsonArray();
    private final List<BlockPos> placedBlocks = new ArrayList<>();
    private final List<FairySample> fairySamples = new ArrayList<>();

    private int clientTicks;
    private int frameIndex;
    private int nextFrameTick;
    private boolean capturePending;
    private boolean fixtureRequested;
    private volatile boolean fixtureSeeded;
    private boolean finishing;
    private boolean originalHideGui;
    private CameraType originalCameraType;
    private boolean hideGuiChanged;
    private volatile String fixtureFailure;
    private volatile String cleanupFailure;
    private volatile boolean cleanupFinished;
    private volatile boolean fixtureMutationStarted;
    private volatile UUID fairyUuid;
    private volatile String fixtureDimension;
    private ServerLevel fixtureLevel;
    private ServerPlayer fixturePlayer;
    private Vec3 originalPosition;
    private float originalYaw;
    private float originalPitch;
    private float originalHeadYaw;
    private ItemStack[] originalHotbar;
    private ItemStack originalFirstRing;
    private boolean clientSideAssertionsRecorded;

    private TargetRenderProbe(Minecraft client, Path outputDir) {
        this.client = client;
        this.outputDir = outputDir;
    }

    public static synchronized void startIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY) || started) {
            return;
        }
        started = true;
        try {
            Minecraft client = Minecraft.getInstance();
            Path defaultOutput = FabricLoader.getInstance().getGameDir().resolve("qa-render-probe");
            Path output = Path.of(System.getProperty(OUTPUT_PROPERTY, defaultOutput.toString()))
                .toAbsolutePath()
                .normalize();
            new TargetRenderProbe(client, output).start();
        } catch (Exception | LinkageError error) {
            completed = true;
            System.err.println("[Aura QA Render Probe] Could not start: " + error);
        }
    }

    public static boolean isComplete() {
        return completed || !Boolean.getBoolean(ENABLE_PROPERTY);
    }

    private void start() throws IOException {
        Files.createDirectories(outputDir);
        try (var entries = Files.list(outputDir)) {
            if (entries.findAny().isPresent()) {
                throw new IOException("Probe output directory is not fresh: " + outputDir);
            }
        }

        manifest.addProperty("probe", "target-render-v1");
        manifest.addProperty("startedAt", Instant.now().toString());
        manifest.addProperty("runtimeNamespace", FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace());
        manifest.addProperty("outputDirectory", outputDir.toString());
        manifest.addProperty("captureApi", "Screenshot.takeScreenshot(Minecraft.getMainRenderTarget(), callback)");
        manifest.addProperty("captureTiming", "END_CLIENT_TICK captures the framebuffer rendered by the preceding client frame");
        manifest.addProperty("renderPath", "native packaged item, block-entity, and entity renderers; the observer draws no substitute visuals");
        manifest.addProperty("successScope", "logical fixture state and native framebuffer capture; visual acceptance remains external");
        manifest.add("logicalAssertions", assertions);
        manifest.add("failures", failures);
        manifest.add("frames", frames);
        JsonObject visualReview = new JsonObject();
        visualReview.addProperty("required", true);
        visualReview.addProperty("status", "PENDING_INDEPENDENT_ASTRA_REVIEW");
        manifest.add("visualReview", visualReview);
        manifest.addProperty("complete", false);
        manifest.addProperty("success", false);
        manifest.addProperty("status", "RUNNING");
        writeManifest();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft minecraft) {
        if (completed || finishing) {
            return;
        }
        clientTicks++;
        if (clientTicks >= MAX_CLIENT_TICKS) {
            fail("client_tick_timeout:" + MAX_CLIENT_TICKS);
            return;
        }

        try {
            if (!fixtureRequested) {
                requestFixtureWhenReady();
                return;
            }
            if (fixtureFailure != null) {
                fail("server_fixture:" + fixtureFailure);
                return;
            }
            if (fixtureSeeded && client.player != null) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                client.player.setYRot(CAMERA_YAW);
                client.player.setXRot(CAMERA_PITCH);
                client.player.setYHeadRot(CAMERA_YAW);
            }
            if (!fixtureSeeded || !clientFixtureReady()) {
                return;
            }
            if (!clientSideAssertionsRecorded) {
                recordClientSideAssertions();
                clientSideAssertionsRecorded = true;
                nextFrameTick = clientTicks + 20;
                writeManifest();
            }
            if (!capturePending && clientTicks >= nextFrameTick) {
                requestFrame();
            }
        } catch (Exception | LinkageError error) {
            fail("probe_exception:" + error);
        }
    }

    private void requestFixtureWhenReady() {
        if (client.level == null || client.player == null || client.player.connection == null) {
            return;
        }
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            fail("integrated_singleplayer_world_required");
            return;
        }
        if (client.screen != null) {
            return;
        }

        originalHideGui = client.options.hideGui;
        originalCameraType = client.options.getCameraType();
        hideGuiChanged = true;
        client.options.hideGui = false;
        fixtureRequested = true;
        UUID playerId = client.player.getUUID();
        server.execute(() -> seedFixture(server, playerId));
        manifest.addProperty("fixtureSeedRequested", true);
        manifest.addProperty("fixtureSeeded", false);
        manifest.addProperty("progressionEarned", false);
        writeManifest();
    }

    private void seedFixture(MinecraftServer server, UUID playerId) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            require(player != null, "integrated server could not find the connected player");
            ServerLevel level = (ServerLevel) player.level();
            fixturePlayer = player;
            fixtureLevel = level;
            originalPosition = player.position();
            originalYaw = player.getYRot();
            originalPitch = player.getXRot();
            originalHeadYaw = player.getYHeadRot();
            originalHotbar = new ItemStack[6];
            for (int slot = 0; slot < originalHotbar.length; slot++) {
                originalHotbar[slot] = player.getInventory().getItem(slot).copy();
            }
            originalFirstRing = AuraAccessoryInventory.get(player, AuraAccessoryInventory.FIRST_RING).copy();

            require(level.getBlockState(PEDESTAL_POS).isAir(), "fixtureCollision: pedestal position is occupied at " + PEDESTAL_POS);
            require(level.getBlockState(PEDESTAL_BASE).isAir(), "fixtureCollision: pedestal base is occupied at " + PEDESTAL_BASE);
            require(level.getBlockState(PLAYER_BASE).isAir(), "fixtureCollision: camera floor position is occupied at " + PLAYER_BASE);
            require(level.getBlockState(PEDESTAL_POS.above()).isAir(), "fixtureCollision: pedestal display space is occupied");
            require(level.getBlockState(PLAYER_BASE.above()).isAir()
                    && level.getBlockState(PLAYER_BASE.above(2)).isAir(),
                "fixtureCollision: camera player space is occupied");

            fixtureMutationStarted = true;
            place(level, PEDESTAL_BASE, Blocks.SMOOTH_STONE.defaultBlockState());
            place(level, PLAYER_BASE, Blocks.SMOOTH_STONE.defaultBlockState());
            place(level, PEDESTAL_POS, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
            BlockEntity blockEntity = level.getBlockEntity(PEDESTAL_POS);
            require(blockEntity instanceof VortexPedestalBlockEntity, "fixturePlacement: vortex pedestal block entity is missing");
            VortexPedestalBlockEntity pedestal = (VortexPedestalBlockEntity) blockEntity;
            pedestal.exchangeHeldItem(new ItemStack(AuraItems.arcaneGem(AuraColor.WHITE)));
            level.sendBlockUpdated(PEDESTAL_POS, level.getBlockState(PEDESTAL_POS), level.getBlockState(PEDESTAL_POS), 3);

            for (int slot = 0; slot < SWORD_COLORS.size(); slot++) {
                player.getInventory().setItem(slot, attunedSword(SWORD_COLORS.get(slot)));
            }
            player.getInventory().setChanged();

            ItemStack ring = new ItemStack(AuraItems.RING_OF_BINDING);
            require(RingOfBindingItem.bindCharm(ring, FairyRole.BASIC), "fixtureFairy: could not bind BASIC role to seeded ring");
            AuraAccessoryInventory.set(player, AuraAccessoryInventory.FIRST_RING, ring);

            player.teleportTo(CAMERA_X, CAMERA_Y, CAMERA_Z);
            player.setYRot(CAMERA_YAW);
            player.setXRot(CAMERA_PITCH);
            player.setYHeadRot(CAMERA_YAW);
            FairySystem.syncPlayerFairies(player);
            AuraFairyEntity fairy = level.getEntitiesOfClass(AuraFairyEntity.class, player.getBoundingBox().inflate(32.0D),
                    entity -> playerId.equals(entity.ownerId()) && entity.role() == FairyRole.BASIC)
                .stream()
                .filter(entity -> FairySystem.isAuthorizedForRole(entity, player))
                .findFirst()
                .orElse(null);
            require(fairy != null, "fixtureFairy: production bound-fairy reconciliation did not create an authorized BASIC fairy");
            fairyUuid = fairy.getUUID();
            fixtureDimension = level.dimension().identifier().toString();
            fixtureSeeded = true;
        } catch (Exception | LinkageError error) {
            fixtureFailure = error.toString();
        }
    }

    private void place(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        require(level.setBlockAndUpdate(pos, state), "fixturePlacement: failed at " + pos);
        placedBlocks.add(pos.immutable());
    }

    private boolean clientFixtureReady() {
        if (client.level == null || client.player == null || client.screen != null || client.options.hideGui) {
            return false;
        }
        if (!client.levelRenderer.isSectionCompiledAndVisible(PEDESTAL_POS)) {
            return false;
        }
        BlockEntity blockEntity = client.level.getBlockEntity(PEDESTAL_POS);
        if (!(blockEntity instanceof VortexPedestalBlockEntity pedestal)
            || !pedestal.heldItem().is(AuraItems.arcaneGem(AuraColor.WHITE))) {
            return false;
        }
        AuraFairyEntity fairy = clientFairy();
        return fairy != null && fairy.role() == FairyRole.BASIC && fairy.slot() == 0;
    }

    private void recordClientSideAssertions() {
        boolean hotbarVisible = client.screen == null && !client.options.hideGui;
        assertion("visibleHotbar", hotbarVisible, "six attuned stacks occupy visible hotbar slots 0-5");

        JsonArray swordEvidence = new JsonArray();
        boolean swordsValid = true;
        for (int slot = 0; slot < SWORD_COLORS.size(); slot++) {
            AuraColor expected = SWORD_COLORS.get(slot);
            ItemStack stack = client.player.getInventory().getItem(slot);
            boolean valid = stack.getItem() instanceof AngelsteelSwordItem sword
                && sword.swordAura(stack).orElse(null) == expected
                && AngelsteelToolHelper.getBuffs(stack).length == 4
                && AuraItemModels.attunementValue(expected) > 0.0F;
            swordsValid &= valid;
            JsonObject evidence = new JsonObject();
            evidence.addProperty("slot", slot);
            evidence.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            evidence.addProperty("expectedColor", expected.id());
            evidence.addProperty("realAuraHelperKey", AngelsteelToolHelper.NBT_AURA_NAME);
            CompoundTag customTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            evidence.addProperty("customDataAura", NbtCompat.getStringOr(customTag, AngelsteelToolHelper.NBT_AURA_NAME, ""));
            evidence.addProperty("realBuffHelperKey", AngelsteelToolHelper.NBT_BUFF_ARRAY_NAME);
            evidence.addProperty("modelPropertyValue", AuraItemModels.attunementValue(expected));
            evidence.addProperty("logicalPass", valid);
            swordEvidence.add(evidence);
        }
        assertion("sixAttunedAngelsteelSwords", swordsValid, "client inventory stacks resolve through swordAura and the registered attunement values");
        manifest.add("swordStacks", swordEvidence);

        JsonObject fixture = new JsonObject();
        fixture.addProperty("source", "QA-seeded integrated server fixture");
        fixture.addProperty("seeded", true);
        fixture.addProperty("earnedProgression", false);
        fixture.addProperty("progressionClaimed", false);
        fixture.addProperty("dimension", fixtureDimension);
        JsonObject coordinates = new JsonObject();
        coordinates.addProperty("x", PEDESTAL_POS.getX());
        coordinates.addProperty("y", PEDESTAL_POS.getY());
        coordinates.addProperty("z", PEDESTAL_POS.getZ());
        fixture.add("vortexPedestal", coordinates);
        fixture.addProperty("heldItem", "aura:arcane_gem_white");
        fixture.addProperty("swordStacks", SWORD_COLORS.size());
        fixture.addProperty("boundFairyRole", FairyRole.BASIC.id());
        fixture.addProperty("boundFairyUuid", fairyUuid.toString());
        fixture.addProperty("cameraX", CAMERA_X);
        fixture.addProperty("cameraY", CAMERA_Y);
        fixture.addProperty("cameraZ", CAMERA_Z);
        manifest.addProperty("fixtureSeeded", true);
        manifest.addProperty("progressionEarned", false);
        manifest.add("fixture", fixture);

        BlockEntity blockEntity = client.level.getBlockEntity(PEDESTAL_POS);
        boolean pedestalValid = blockEntity instanceof VortexPedestalBlockEntity pedestal
            && pedestal.heldItem().is(AuraItems.arcaneGem(AuraColor.WHITE));
        assertion("seededVortexPedestalState", pedestalValid, "client received the production pedestal block entity and seeded held item");
        assertion("authorizedBoundFairyOnServer", fairyUuid != null, "integrated server fixture uses the normal bound-ring reconciliation path");
        assertion("clientBoundFairyPresent", clientFairy() != null, "client received the server-bound BASIC fairy entity");
    }

    private void requestFrame() {
        if (frameIndex >= FRAME_COUNT || finishing) {
            return;
        }
        AuraFairyEntity fairy = clientFairy();
        if (fairy == null) {
            fail("client_bound_fairy_missing_before_frame_" + (frameIndex + 1));
            return;
        }
        JsonObject evidence = frameState(fairy, frameIndex);
        String filename = "render-frame-" + (frameIndex + 1) + ".png";
        evidence.addProperty("screenshot", outputDir.resolve(filename).toString());
        evidence.addProperty("framebufferCapture", "REQUESTED_FROM_END_CLIENT_TICK");
        capturePending = true;
        Screenshot.takeScreenshot(client.getMainRenderTarget(), image ->
            client.execute(() -> completeFrame(filename, evidence, image)));
        frameIndex++;
    }

    private JsonObject frameState(AuraFairyEntity fairy, int index) {
        JsonObject frame = new JsonObject();
        frame.addProperty("index", index + 1);
        frame.addProperty("requestedAtClientTick", clientTicks);
        frame.addProperty("clientGameTime", client.level.getGameTime());
        frame.addProperty("fairyUuid", fairy.getUUID().toString());
        frame.addProperty("fairyRole", fairy.role().id());
        frame.addProperty("fairySlot", fairy.slot());
        frame.addProperty("fairyTickCount", fairy.tickCount);
        frame.addProperty("fairyX", fairy.getX());
        frame.addProperty("fairyY", fairy.getY());
        frame.addProperty("fairyZ", fairy.getZ());
        frame.addProperty("playerX", client.player.getX());
        frame.addProperty("playerY", client.player.getY());
        frame.addProperty("playerZ", client.player.getZ());
        frame.addProperty("playerYaw", client.player.getYRot());
        frame.addProperty("playerPitch", client.player.getXRot());
        frame.addProperty("cameraType", client.options.getCameraType().name());
        BlockEntity blockEntity = client.level.getBlockEntity(PEDESTAL_POS);
        if (blockEntity instanceof VortexPedestalBlockEntity pedestal) {
            frame.addProperty("pedestalHeldItem", net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(pedestal.heldItem().getItem()).toString());
            frame.addProperty("pedestalHeldItemCount", pedestal.heldItem().getCount());
        }
        JsonArray swordModelValues = new JsonArray();
        for (int slot = 0; slot < SWORD_COLORS.size(); slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            AuraColor color = SWORD_COLORS.get(slot);
            JsonObject sword = new JsonObject();
            sword.addProperty("slot", slot);
            sword.addProperty("attunement", color.id());
            sword.addProperty("modelPropertyValue", AuraItemModels.attunementValue(color));
            sword.addProperty("item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            swordModelValues.add(sword);
        }
        frame.add("visibleHotbarModelState", swordModelValues);
        fairySamples.add(new FairySample(fairy.getX(), fairy.getY(), fairy.getZ(), fairy.tickCount));
        return frame;
    }

    private void completeFrame(String filename, JsonObject evidence, NativeImage image) {
        if (finishing) {
            if (image != null) {
                image.close();
            }
            return;
        }
        try {
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new IOException("Minecraft framebuffer returned an empty image");
            }
            Path screenshot = outputDir.resolve(filename);
            try (NativeImage captured = image) {
                captured.writeToFile(screenshot.toFile());
                evidence.addProperty("framebufferWidth", captured.getWidth());
                evidence.addProperty("framebufferHeight", captured.getHeight());
            }
            long bytes = Files.size(screenshot);
            if (bytes < 1L) {
                throw new IOException("Minecraft framebuffer image was empty on disk");
            }
            evidence.addProperty("screenshotBytes", bytes);
            evidence.addProperty("sha256", sha256(screenshot));
            evidence.addProperty("framebufferCapture", "CAPTURED; independent visual review required");
            frames.add(evidence);
            capturePending = false;
            nextFrameTick = clientTicks + FRAME_GAP_TICKS;
            writeManifest();
            if (frames.size() == FRAME_COUNT) {
                recordTemporalAssertions();
                beginCleanup(null);
            }
        } catch (Exception | LinkageError error) {
            if (image != null) {
                image.close();
            }
            fail("framebuffer_capture_failed:" + filename + ":" + error);
        }
    }

    private void recordTemporalAssertions() {
        if (fairySamples.size() < FRAME_COUNT) {
            assertion("fairyMovementAcrossFrames", false, "not enough live client fairy samples");
            assertion("fairyAnimationClockAdvanced", false, "not enough live client fairy samples");
            return;
        }
        FairySample first = fairySamples.get(0);
        FairySample last = fairySamples.get(fairySamples.size() - 1);
        double moved = Math.sqrt(square(last.x() - first.x()) + square(last.y() - first.y()) + square(last.z() - first.z()));
        int tickAdvance = last.tickCount() - first.tickCount();
        assertion("fairyMovementAcrossFrames", moved >= 0.15D,
            "client entity displacement across captured frame requests: " + moved);
        assertion("fairyAnimationClockAdvanced", tickAdvance >= FRAME_GAP_TICKS * (FRAME_COUNT - 1),
            "production renderer animation tick advanced by " + tickAdvance + " across captures");
        assertion("threeNativeFramebufferCaptures", frames.size() == FRAME_COUNT,
            "captured framebuffer count: " + frames.size());
    }

    private AuraFairyEntity clientFairy() {
        if (client.level == null || fairyUuid == null) {
            return null;
        }
        return client.level.getEntitiesOfClass(AuraFairyEntity.class, client.player.getBoundingBox().inflate(32.0D),
                entity -> fairyUuid.equals(entity.getUUID()))
            .stream()
            .findFirst()
            .orElse(null);
    }

    private void assertion(String name, boolean passed, String detail) {
        JsonObject result = new JsonObject();
        result.addProperty("passed", passed);
        result.addProperty("detail", detail);
        assertions.add(name, result);
    }

    private void fail(String message) {
        if (finishing || completed) {
            return;
        }
        failures.add(message);
        beginCleanup(message);
    }

    private void beginCleanup(String reason) {
        if (finishing) {
            return;
        }
        finishing = true;
        manifest.addProperty("cleanupStarted", true);
        if (reason != null && (failures.size() == 0 || !failures.get(failures.size() - 1).getAsString().equals(reason))) {
            failures.add(reason);
        }
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            cleanupFinished = true;
            finishAfterCleanup();
            return;
        }
        server.execute(() -> {
            try {
                cleanupFixture();
            } catch (Exception | LinkageError error) {
                cleanupFailure = error.toString();
            }
            cleanupFinished = true;
            client.execute(this::finishAfterCleanup);
        });
    }

    private void cleanupFixture() {
        if (fixtureMutationStarted && fixtureLevel != null) {
            if (fixturePlayer != null && fixturePlayer.isAlive()) {
                AuraAccessoryInventory.set(fixturePlayer, AuraAccessoryInventory.FIRST_RING,
                    originalFirstRing == null ? ItemStack.EMPTY : originalFirstRing);
                FairySystem.syncPlayerFairies(fixturePlayer);
                if (originalHotbar != null) {
                    for (int slot = 0; slot < originalHotbar.length; slot++) {
                        fixturePlayer.getInventory().setItem(slot, originalHotbar[slot]);
                    }
                    fixturePlayer.getInventory().setChanged();
                }
                if (originalPosition != null) {
                    fixturePlayer.teleportTo(originalPosition.x, originalPosition.y, originalPosition.z);
                    fixturePlayer.setYHeadRot(originalHeadYaw);
                    fixturePlayer.setYRot(originalYaw);
                    fixturePlayer.setXRot(originalPitch);
                }
            }
            BlockEntity blockEntity = fixtureLevel.getBlockEntity(PEDESTAL_POS);
            if (blockEntity instanceof VortexPedestalBlockEntity pedestal) {
                pedestal.exchangeHeldItem(ItemStack.EMPTY);
                fixtureLevel.sendBlockUpdated(PEDESTAL_POS, fixtureLevel.getBlockState(PEDESTAL_POS),
                    fixtureLevel.getBlockState(PEDESTAL_POS), 3);
            }
            for (int index = placedBlocks.size() - 1; index >= 0; index--) {
                BlockPos pos = placedBlocks.get(index);
                if (fixtureLevel.getBlockState(pos).is(AuraContent.VORTEX_PEDESTAL)
                    || fixtureLevel.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
                    fixtureLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private void finishAfterCleanup() {
        if (!cleanupFinished || completed) {
            return;
        }
        if (cleanupFailure != null) {
            failures.add("cleanup:" + cleanupFailure);
        }
        if (hideGuiChanged) {
            client.options.hideGui = originalHideGui;
            client.options.setCameraType(originalCameraType);
        }
        boolean passed = failures.size() == 0 && allAssertionsPassed() && frames.size() == FRAME_COUNT;
        manifest.addProperty("finishedAt", Instant.now().toString());
        manifest.addProperty("clientTicks", clientTicks);
        manifest.addProperty("cleanup", cleanupFailure == null ? "PASS" : "FAIL: " + cleanupFailure);
        manifest.addProperty("complete", true);
        manifest.addProperty("success", passed);
        manifest.addProperty("status", passed ? "PASS_LOGICAL_AND_CAPTURE" : "FAIL");
        manifest.addProperty("visualReviewPending", true);
        writeManifest();
        completed = true;
    }

    private boolean allAssertionsPassed() {
        for (String name : assertions.keySet()) {
            if (!assertions.getAsJsonObject(name).get("passed").getAsBoolean()) {
                return false;
            }
        }
        return !assertions.isEmpty();
    }

    private void writeManifest() {
        Path temporary = outputDir.resolve("manifest.json.tmp");
        Path destination = outputDir.resolve("manifest.json");
        try {
            Files.writeString(temporary, JSON.toJson(manifest), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            System.err.println("[Aura QA Render Probe] Could not write manifest: " + error);
        }
    }

    private static ItemStack attunedSword(AuraColor color) {
        ItemStack stack = new ItemStack(AuraItems.angelsteelTool(AngelsteelToolKind.SWORD, 0));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag tag) -> {
            tag.putString(AngelsteelToolHelper.NBT_AURA_NAME, color.id());
            tag.putIntArray(AngelsteelToolHelper.NBT_BUFF_ARRAY_NAME, new int[4]);
        });
        return stack;
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static double square(double value) {
        return value * value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record FairySample(double x, double y, double z, int tickCount) {
    }
}
