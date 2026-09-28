package pixlepix.auracascade.qa.neoforge;

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
import java.util.UUID;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import pixlepix.auracascade.block.AuraContent;

final class AuraQaObserverClient {
    private static final int STABLE_FRAMES_REQUIRED = 40;
    private static final int TIMEOUT_TICKS = 2_400;
    private static final long TIMEOUT_NANOS = 180_000_000_000L;
    private static final String FONT_PROBE = "Aura Cascade QA 0123456789";
    private static final boolean SCENE_ENABLED = Boolean.getBoolean("aura.qa.observer.scene");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long STARTED_AT = System.nanoTime();

    private static int clientTicks;
    private static int renderedFrames;
    private static int stableRenderedFrames;
    private static boolean finished;
    private static boolean stopNextTick;
    private static boolean sceneSetupStarted;
    private static boolean scenePreconditionsReady;
    private static CompletableFuture<SceneSetup> sceneSetup;
    private static SceneSetup scene;
    private static String sceneSetupError;

    private AuraQaObserverClient() {
    }

    static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(AuraQaObserverClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AuraQaObserverClient::onClientTick);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AuraQaObserverClient::onRenderFrame);
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (stopNextTick) {
            stopNextTick = false;
            client.stop();
            return;
        }
        if (finished) {
            return;
        }

        clientTicks++;
        if (SCENE_ENABLED) {
            startSceneSetup(client);
            completeSceneSetup(client);
        }
        if (timedOut()) {
            finish(client, "observer_timeout");
        }
    }

    private static void onRenderFrame(RenderFrameEvent.Post event) {
        if (finished) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        renderedFrames++;
        if (timedOut()) {
            finish(client, "observer_timeout");
            return;
        }
        if (!isReady(client)) {
            stableRenderedFrames = 0;
            return;
        }
        if (SCENE_ENABLED && !scenePreconditionsReady) {
            stableRenderedFrames = 0;
            return;
        }
        if (++stableRenderedFrames >= STABLE_FRAMES_REQUIRED) {
            finish(client, null);
        }
    }

    private static boolean isReady(Minecraft client) {
        return client.level != null && client.player != null
            && client.getSingleplayerServer() != null && client.screen == null;
    }

    private static boolean timedOut() {
        return clientTicks >= TIMEOUT_TICKS || System.nanoTime() - STARTED_AT >= TIMEOUT_NANOS;
    }

    private static void finish(Minecraft client, String failure) {
        if (finished) {
            return;
        }
        finished = true;
        stopNextTick = true;

        JsonObject report = new JsonObject();
        report.addProperty("clientTicks", clientTicks);
        report.addProperty("renderedFrames", renderedFrames);
        report.addProperty("stableRenderedFrames", stableRenderedFrames);
        report.addProperty("worldLoaded", client.level != null && client.player != null);
        report.addProperty("singleplayer", client.getSingleplayerServer() != null);
        report.addProperty("auraLoaded", ModList.get().isLoaded("aura"));
        if (!report.get("auraLoaded").getAsBoolean()) {
            failure = firstFailure(failure, "aura_mod_not_loaded");
        }
        failure = firstFailure(failure, auditAuraJar(report));
        if (!isReady(client)) {
            failure = firstFailure(failure, "clean_singleplayer_world_not_ready");
        }
        if (client.getSingleplayerServer() != null) {
            report.addProperty("worldName", client.getSingleplayerServer().getWorldData().getLevelName());
        }
        if (client.level != null) {
            report.addProperty("dimension", client.level.dimension().location().toString());
        }
        if (client.player != null) {
            report.addProperty("playerUuid", client.player.getUUID().toString());
            report.addProperty("playerX", client.player.getX());
            report.addProperty("playerY", client.player.getY());
            report.addProperty("playerZ", client.player.getZ());
        }
        if (SCENE_ENABLED) {
            report.addProperty("scenePreconditionsReady", scenePreconditionsReady);
            report.addProperty("sceneScope", "Asset rendering preconditions only; no energy is injected and no gameplay outcome is asserted.");
            report.addProperty("sceneNodeBlockId", "aura:aura_node");
            JsonArray nodePositions = new JsonArray();
            if (scene != null) {
                nodePositions.add(positionJson(scene.firstNode()));
                nodePositions.add(positionJson(scene.secondNode()));
            }
            report.add("sceneNodePositions", nodePositions);
            if (sceneSetupError != null) {
                report.addProperty("sceneSetupError", sceneSetupError);
            }
        }

        try {
            int fontProbeWidth = client.font.width(Component.literal(FONT_PROBE));
            JsonObject fontAudit = new JsonObject();
            fontAudit.addProperty("probeText", FONT_PROBE);
            fontAudit.addProperty("probeWidth", fontProbeWidth);
            fontAudit.addProperty("scope", "Basic font-width probe only; NOT a full Patchouli guide layout audit.");
            fontAudit.addProperty("fullGuideLayoutAudit", false);
            fontAudit.addProperty("success", fontProbeWidth > 0);
            report.add("fontAudit", fontAudit);
            if (fontProbeWidth <= 0) {
                failure = firstFailure(failure, "font_probe_failed");
            }
        } catch (Exception | LinkageError exception) {
            JsonObject fontAudit = new JsonObject();
            fontAudit.addProperty("scope", "Basic font-width probe only; NOT a full Patchouli guide layout audit.");
            fontAudit.addProperty("fullGuideLayoutAudit", false);
            fontAudit.addProperty("success", false);
            fontAudit.addProperty("failure", exception.toString());
            report.add("fontAudit", fontAudit);
            failure = firstFailure(failure, "font_probe_failed");
        }

        Path outputDir = outputDirectory(client);
        try {
            Files.createDirectories(outputDir);
            Path screenshot = outputDir.resolve("screenshot.png");
            try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                    throw new IllegalStateException("Screenshot image is empty");
                }
                image.writeToFile(screenshot);
                long screenshotBytes = Files.size(screenshot);
                if (screenshotBytes == 0) {
                    throw new IllegalStateException("Screenshot file is empty");
                }
                report.addProperty("screenshot", screenshot.toAbsolutePath().toString());
                report.addProperty("screenshotWidth", image.getWidth());
                report.addProperty("screenshotHeight", image.getHeight());
                report.addProperty("screenshotBytes", screenshotBytes);
            }
        } catch (Exception exception) {
            report.addProperty("screenshotError", exception.toString());
            failure = firstFailure(failure, "screenshot_failed");
        }

        JsonArray unsupportedAudits = new JsonArray();
        unsupportedAudits.add("Full Patchouli encyclopedia page and entry layout/overflow audit");
        unsupportedAudits.add("Fabric intermediary runtime-namespace validation");
        report.add("unsupportedAudits", unsupportedAudits);
        report.addProperty("success", failure == null);
        if (failure != null) {
            report.addProperty("failure", failure);
        }

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("manifest.json"), JSON.toJson(report), StandardCharsets.UTF_8);
            AuraQaObserverMod.LOGGER.info("Observer finished: success={}, ticks={}, frames={}, output={}",
                failure == null, clientTicks, renderedFrames, outputDir.toAbsolutePath());
        } catch (Exception exception) {
            AuraQaObserverMod.LOGGER.error("Could not write observer manifest to {}", outputDir, exception);
        }
    }

    private static Path outputDirectory(Minecraft client) {
        String configured = System.getProperty("aura.qa.observer.dir");
        Path directory = configured == null || configured.isBlank()
            ? client.gameDirectory.toPath().resolve("qa-observer")
            : Path.of(configured);
        return directory.toAbsolutePath().normalize();
    }

    private static void startSceneSetup(Minecraft client) {
        if (sceneSetupStarted || !isReady(client)) {
            return;
        }
        sceneSetupStarted = true;
        sceneSetup = new CompletableFuture<>();
        CompletableFuture<SceneSetup> completion = sceneSetup;
        UUID playerId = client.player.getUUID();
        var server = client.getSingleplayerServer();
        server.execute(() -> {
            try {
                completion.complete(createScene(server.overworld(), server.getPlayerList().getPlayer(playerId)));
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
    }

    private static SceneSetup createScene(ServerLevel level, ServerPlayer player) {
        if (player == null || player.serverLevel() != level) {
            throw new IllegalStateException("The local player is not in the overworld");
        }

        BlockPos spawn = level.getSharedSpawnPos();
        int nodeZ = spawn.getZ() + 2;
        BlockPos firstNode = new BlockPos(spawn.getX() + 2,
            level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX() + 2, nodeZ), nodeZ);
        BlockPos secondNode = new BlockPos(spawn.getX() + 4,
            level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX() + 4, nodeZ), nodeZ);
        if (!level.getBlockState(firstNode).canBeReplaced() || !level.getBlockState(secondNode).canBeReplaced()) {
            throw new IllegalStateException("Spawn-offset scene positions are obstructed");
        }

        BlockState nodeState = AuraContent.AURA_NODE.defaultBlockState();
        boolean firstPlaced = level.setBlock(firstNode, nodeState, 3);
        boolean secondPlaced = level.setBlock(secondNode, nodeState, 3);
        if (!firstPlaced || !secondPlaced) {
            if (firstPlaced) {
                level.removeBlock(firstNode, false);
            }
            if (secondPlaced) {
                level.removeBlock(secondNode, false);
            }
            throw new IllegalStateException("Could not place both Aura scene nodes");
        }

        double playerX = spawn.getX() + 3.5D;
        double playerZ = spawn.getZ() + 8.5D;
        double playerY = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
            BlockPos.containing(playerX, 0.0D, playerZ).getX(),
            BlockPos.containing(playerX, 0.0D, playerZ).getZ());
        double targetX = (firstNode.getX() + secondNode.getX()) / 2.0D + 0.5D;
        double targetY = (firstNode.getY() + secondNode.getY()) / 2.0D + 0.5D;
        double targetZ = nodeZ + 0.5D;
        double dx = targetX - playerX;
        double dy = targetY - (playerY + 1.62D);
        double dz = targetZ - playerZ;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        player.setPos(playerX, playerY, playerZ);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        return new SceneSetup(firstNode, secondNode, playerX, playerY, playerZ, yaw, pitch);
    }

    private static void completeSceneSetup(Minecraft client) {
        if (sceneSetup == null || scenePreconditionsReady || !sceneSetup.isDone()) {
            return;
        }

        try {
            scene = sceneSetup.join();
        } catch (CompletionException exception) {
            sceneSetupError = exception.getCause() == null ? exception.toString() : exception.getCause().toString();
            finish(client, "observer_scene_setup_failed");
            return;
        }

        if (client.level == null || client.player == null
            || !client.level.getBlockState(scene.firstNode()).is(AuraContent.AURA_NODE)
            || !client.level.getBlockState(scene.secondNode()).is(AuraContent.AURA_NODE)) {
            return;
        }

        client.player.setPos(scene.playerX(), scene.playerY(), scene.playerZ());
        client.player.setYRot(scene.yaw());
        client.player.setXRot(scene.pitch());
        client.player.setYHeadRot(scene.yaw());
        scenePreconditionsReady = true;
    }

    private static JsonObject positionJson(BlockPos pos) {
        JsonObject position = new JsonObject();
        position.addProperty("x", pos.getX());
        position.addProperty("y", pos.getY());
        position.addProperty("z", pos.getZ());
        return position;
    }

    private static String auditAuraJar(JsonObject report) {
        try {
            var modFileInfo = ModList.get().getModFileById("aura");
            if (modFileInfo == null) {
                report.addProperty("auraJarProof", "missing_mod_file");
                return "aura_mod_file_missing";
            }

            Path auraJar = modFileInfo.getFile().getFilePath().toAbsolutePath().normalize();
            report.addProperty("auraJarPath", auraJar.toString());
            boolean regularJar = Files.isRegularFile(auraJar)
                && auraJar.getFileName() != null
                && auraJar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
            report.addProperty("auraJarIsRegularFile", regularJar);
            if (!regularJar) {
                report.addProperty("auraJarProof", "not_a_packaged_jar");
                return "aura_not_loaded_from_packaged_jar";
            }

            report.addProperty("auraJarBytes", Files.size(auraJar));
            report.addProperty("auraJarSha256", sha256(auraJar));
            report.addProperty("auraJarProof", "verified");
            return null;
        } catch (Exception exception) {
            report.addProperty("auraJarProof", "failed");
            report.addProperty("auraJarProofError", exception.toString());
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

    private record SceneSetup(
        BlockPos firstNode,
        BlockPos secondNode,
        double playerX,
        double playerY,
        double playerZ,
        float yaw,
        float pitch
    ) {
    }

    private static String firstFailure(String current, String candidate) {
        return current == null ? candidate : current;
    }
}
