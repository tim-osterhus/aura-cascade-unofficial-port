package pixlepix.auracascade.qa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public final class PackagedObserverClient implements ClientModInitializer {
    private static final int STABLE_FRAMES = 40;
    private static final int TIMEOUT_TICKS = 2400;
    private static final long TIMEOUT_NANOS = 180_000_000_000L;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path outputDir = Path.of(System.getProperty("aura.qa.observer.dir",
        FabricLoader.getInstance().getGameDir().resolve("qa-observer").toString())).toAbsolutePath().normalize();
    private int ticks;
    private int stableFrames;
    private boolean finished;
    private boolean finishing;
    private final long startedAt = System.nanoTime();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register((graphics, delta) -> onHudRendered(Minecraft.getInstance()));
    }

    private void onTick(Minecraft client) {
        ticks++;
        if (finished) {
            if (Boolean.getBoolean("aura.qa.clientProbe") && !TargetClientProbe.isComplete()) {
                TargetClientProbe.startIfEnabled();
                return;
            }
            if (Boolean.getBoolean("aura.qa.feedbackProbe") && !TargetFeedbackProbe.isComplete()) {
                TargetFeedbackProbe.startIfEnabled();
                return;
            }
            if (Boolean.getBoolean("aura.qa.renderProbe") && !TargetRenderProbe.isComplete()) {
                TargetRenderProbe.startIfEnabled();
                return;
            }
            if (!Boolean.getBoolean("aura.qa.observer.keepOpen")) {
                client.stop();
            }
        } else if (!finishing && (ticks >= TIMEOUT_TICKS || System.nanoTime() - startedAt >= TIMEOUT_NANOS)) {
            finish(client, "world_not_rendered_before_timeout");
        }
    }

    private void onHudRendered(Minecraft client) {
        if (finished || finishing) {
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null
            || client.screen != null) {
            stableFrames = 0;
            return;
        }
        if (++stableFrames >= STABLE_FRAMES) {
            finish(client, null);
        }
    }

    private void finish(Minecraft client, String failure) {
        finishing = true;
        Screenshot.takeScreenshot(client.getMainRenderTarget(), image ->
            client.execute(() -> finishWithScreenshot(client, failure, image)));
    }

    private void finishWithScreenshot(Minecraft client, String failure, NativeImage captured) {
        JsonObject report = new JsonObject();
        report.addProperty("runtimeNamespace", FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace());
        report.addProperty("clientTicks", ticks);
        report.addProperty("stableRenderedFrames", stableFrames);
        report.addProperty("singleplayer", client.getSingleplayerServer() != null);
        report.addProperty("worldLoaded", client.level != null && client.player != null);
        if (client.getSingleplayerServer() != null) {
            report.addProperty("worldName", client.getSingleplayerServer().getWorldData().getLevelName());
        }
        if (client.level != null) {
            report.addProperty("dimension", client.level.dimension().identifier().toString());
        }
        if (client.player != null) {
            report.addProperty("playerUuid", client.player.getUUID().toString());
            report.addProperty("playerX", client.player.getX());
            report.addProperty("playerY", client.player.getY());
            report.addProperty("playerZ", client.player.getZ());
        }

        var aura = FabricLoader.getInstance().getModContainer("aura");
        if (aura.isEmpty()) {
            failure = firstFailure(failure, "aura_mod_not_loaded");
        } else {
            report.addProperty("auraVersion", aura.get().getMetadata().getVersion().getFriendlyString());
            ModOrigin origin = aura.get().getOrigin();
            if (origin.getKind() != ModOrigin.Kind.PATH || origin.getPaths().size() != 1) {
                failure = firstFailure(failure, "aura_origin_is_not_one_path");
            } else {
                Path jar = origin.getPaths().get(0).toAbsolutePath().normalize();
                report.addProperty("auraOrigin", jar.toString());
                if (!Files.isRegularFile(jar) || !jar.getFileName().toString().endsWith(".jar")) {
                    failure = firstFailure(failure, "aura_origin_is_not_packaged_jar");
                } else {
                    try {
                        report.addProperty("auraSha256", sha256(jar));
                        report.addProperty("auraJarBytes", Files.size(jar));
                    } catch (Exception exception) {
                        failure = firstFailure(failure, "aura_jar_hash_failed");
                        report.addProperty("auraHashError", exception.toString());
                    }
                }
            }
        }
        if (!"intermediary".equals(report.get("runtimeNamespace").getAsString())) {
            failure = firstFailure(failure, "runtime_is_not_intermediary");
        }

        try {
            Files.createDirectories(outputDir);
            Path screenshot = outputDir.resolve("screenshot.png");
            try (NativeImage image = captured) {
                if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                    throw new IllegalStateException("Screenshot image is empty");
                }
                image.writeToFile(screenshot);
                long bytes = Files.size(screenshot);
                if (bytes == 0) {
                    throw new IllegalStateException("Screenshot file is empty");
                }
                report.addProperty("screenshot", screenshot.toString());
                report.addProperty("screenshotWidth", image.getWidth());
                report.addProperty("screenshotHeight", image.getHeight());
                report.addProperty("screenshotBytes", bytes);
            }
        } catch (Exception exception) {
            failure = firstFailure(failure, "screenshot_failed");
            report.addProperty("screenshotError", exception.toString());
        }

        JsonObject guideLayout;
        try {
            guideLayout = GuideLayoutAudit.audit(client);
        } catch (Exception | LinkageError exception) {
            guideLayout = new JsonObject();
            guideLayout.addProperty("book", "aura:encyclopedia_aura");
            guideLayout.addProperty("success", false);
            guideLayout.addProperty("failure", exception.toString());
            failure = firstFailure(failure, "guide_layout_audit_failed");
        }
        Path guideLayoutPath = outputDir.resolve("guide-layout.json");
        try {
            Files.writeString(guideLayoutPath, JSON.toJson(guideLayout), StandardCharsets.UTF_8);
            boolean guideLayoutSuccess = guideLayout.get("success").getAsBoolean();
            report.addProperty("guideLayoutReport", guideLayoutPath.toString());
            report.addProperty("guideLayoutSuccess", guideLayoutSuccess);
            report.addProperty("guideLayoutEntryCount", guideLayout.has("entryCount")
                ? guideLayout.get("entryCount").getAsInt() : 0);
            report.addProperty("guideLayoutPageCount", guideLayout.has("pageCount")
                ? guideLayout.get("pageCount").getAsInt() : 0);
            if (!guideLayoutSuccess) {
                failure = firstFailure(failure, "guide_layout_failed");
            }
        } catch (Exception exception) {
            failure = firstFailure(failure, "guide_layout_report_write_failed");
            report.addProperty("guideLayoutSuccess", false);
            report.addProperty("guideLayoutReport", guideLayoutPath.toString());
            report.addProperty("guideLayoutReportError", exception.toString());
        }

        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
            failure = firstFailure(failure, "singleplayer_world_not_loaded");
        }
        report.addProperty("success", failure == null);
        if (failure != null) {
            report.addProperty("failure", failure);
        }
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("manifest.json"), JSON.toJson(report), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            System.err.println("[Aura QA Observer] Could not write manifest: " + exception);
        }
        finished = true;
    }

    private static String firstFailure(String current, String candidate) {
        return current == null ? candidate : current;
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
}
