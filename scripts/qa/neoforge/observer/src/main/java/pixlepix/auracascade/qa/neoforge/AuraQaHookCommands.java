package pixlepix.auracascade.qa.neoforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server-only QA command. Run in a disposable world: the fixture seeds the world's RNG. */
public final class AuraQaHookCommands {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    private AuraQaHookCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aura_qa_hooks")
            .requires(source -> source.hasPermission(4))
            .executes(context -> execute(context.getSource())));
    }

    private static int execute(CommandSourceStack source) {
        JsonObject report = new JsonObject();
        report.addProperty("runId", UUID.randomUUID().toString());
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("status", "RUNNING");
        report.addProperty("success", false);
        JsonObject checks = new JsonObject();
        checks.addProperty("damage", "NOT_RUN");
        checks.addProperty("redHoleLifetime", "NOT_RUN");
        checks.addProperty("thiefDrops", "NOT_RUN");
        checks.addProperty("bonusLootCancellation", "NOT_RUN");
        checks.addProperty("harvestingIsolation", "NOT_RUN");
        report.add("checks", checks);

        Path output;
        try {
            String configured = System.getProperty("aura.qa.observer.dir");
            Path directory = configured == null || configured.isBlank()
                ? source.getServer().getServerDirectory().resolve("qa-observer") : Path.of(configured);
            output = directory.toAbsolutePath().normalize().resolve("hooks-result.json");
            Files.createDirectories(output.getParent());
            // Replace any previous PASS before invoking code that may fail.
            writeReport(output, report);
        } catch (IOException | RuntimeException failure) {
            AuraQaObserverMod.LOGGER.error("[aura_qa_hooks] FAIL: cannot initialize result JSON", failure);
            source.sendFailure(Component.literal("Aura hook QA FAIL: cannot initialize result JSON; see server log."));
            return 0;
        }

        AuraQaObserverMod.LOGGER.info("[aura_qa_hooks] START run={} output={}", report.get("runId").getAsString(), output);
        try {
            ServerLevel level = source.getServer().overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            report.addProperty("dimension", level.dimension().location().toString());
            report.addProperty("gameTime", level.getGameTime());
            if (!level.hasChunkAt(spawn)) {
                retry(report, "Overworld spawn chunk is not loaded. Load it, then retry /aura_qa_hooks.");
            } else if (level.getGameTime() % 100L == 0L) {
                retry(report, "Eruption tick: retry /aura_qa_hooks after game time advances (unfreeze ticks if needed).");
            } else {
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
                int y = Math.min(level.getMaxBuildHeight() - 3, Math.max(spawn.getY(), surface) + 2);
                BlockPos pos = new BlockPos(spawn.getX(), y, spawn.getZ());
                JsonObject position = new JsonObject();
                position.addProperty("x", pos.getX());
                position.addProperty("y", pos.getY());
                position.addProperty("z", pos.getZ());
                report.add("position", position);
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                    retry(report, "No clear two-block test space above overworld spawn. Clear the recorded position and retry.");
                } else {
                    boolean passed = runCheck(report, "damage", () -> NeoForgeHookRuntimeFixture.verifyDamage(level, pos))
                        && runCheck(report, "redHoleLifetime", () -> NeoForgeHookRuntimeFixture.verifyRedHoleLifetime(level, pos))
                        && runCheck(report, "thiefDrops", () -> NeoForgeHookRuntimeFixture.verifyThiefDrops(level, pos))
                        && runCheck(report, "bonusLootCancellation", () -> NeoForgeHookRuntimeFixture.verifyBonusLootCancellation(level, pos))
                        && runCheck(report, "harvestingIsolation", () -> NeoForgeHookRuntimeFixture.verifyHarvestingIsolation(level, pos));
                    report.addProperty("status", passed ? "PASS" : "FAIL");
                    report.addProperty("success", passed);
                }
            }
        } catch (AssertionError | RuntimeException failure) {
            report.addProperty("status", "FAIL");
            report.addProperty("failure", failure.toString());
            AuraQaObserverMod.LOGGER.error("[aura_qa_hooks] FAIL: runtime setup", failure);
        }

        report.addProperty("finishedAt", Instant.now().toString());
        try {
            writeReport(output, report);
        } catch (IOException | RuntimeException failure) {
            AuraQaObserverMod.LOGGER.error("[aura_qa_hooks] FAIL: cannot finish result JSON at {}", output, failure);
            source.sendFailure(Component.literal("Aura hook QA FAIL: cannot finish result JSON at " + output));
            return 0;
        }

        String status = report.get("status").getAsString();
        String message = "Aura hook QA " + status + ": " + output;
        if (report.has("reason")) {
            message += ". " + report.get("reason").getAsString();
        }
        if (report.get("success").getAsBoolean()) {
            AuraQaObserverMod.LOGGER.info("[aura_qa_hooks] PASS run={} output={}", report.get("runId").getAsString(), output);
            String result = message;
            source.sendSuccess(() -> Component.literal(result), true);
            return 1;
        }
        AuraQaObserverMod.LOGGER.warn("[aura_qa_hooks] {} run={} output={} detail={}", status,
            report.get("runId").getAsString(), output, report.has("failure") ? report.get("failure") : report.get("reason"));
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private static boolean runCheck(JsonObject report, String name, Runnable assertion) {
        try {
            assertion.run();
            report.getAsJsonObject("checks").addProperty(name, "PASS");
            AuraQaObserverMod.LOGGER.info("[aura_qa_hooks] {} PASS", name);
            return true;
        } catch (AssertionError | RuntimeException failure) {
            report.getAsJsonObject("checks").addProperty(name, "FAIL");
            report.addProperty("failure", name + ": " + failure);
            AuraQaObserverMod.LOGGER.error("[aura_qa_hooks] {} FAIL", name, failure);
            return false;
        }
    }

    private static void retry(JsonObject report, String reason) {
        report.addProperty("status", "RETRY");
        report.addProperty("reason", reason);
    }

    private static void writeReport(Path output, JsonObject report) throws IOException {
        Files.writeString(output, JSON.toJson(report), StandardCharsets.UTF_8);
    }
}
